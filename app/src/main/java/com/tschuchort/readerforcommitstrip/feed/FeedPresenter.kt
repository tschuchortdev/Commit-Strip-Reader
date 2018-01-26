package com.tschuchort.readerforcommitstrip.feed

import com.google.firebase.analytics.FirebaseAnalytics
import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.feed.FeedContract.*
import com.tschuchort.readerforcommitstrip.feed.FeedContract.Orientation.HORIZONTAL
import com.tschuchort.readerforcommitstrip.feed.FeedContract.Orientation.VERTICAL
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.ofType
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@PerActivity
class FeedPresenter
		@Inject constructor(
				private val comicRepository: ComicRepository,
				@UiScheduler uiScheduler: Scheduler,
				systemManager: SystemManager,
				navigator: Navigator,
				private val analytics: FirebaseAnalytics)
		: Presenter(uiScheduler) {

	private val ioScheduler = Schedulers.io()

	override val initialState = State(
			comics = emptyList(),
			feedOrientation = VERTICAL,
			internetConnected = true,
			selectedComic = null,
			loading = false,
			refreshing = true
	)

	override fun restoreState(savedState: State) = savedState.copy(
			loading = false,
			refreshing = false
	)

	override val initCommand = SideEffect.RefreshNewest()

	init {
		sideEffects.ofType<SideEffect.ShowEnlarged>()
				.subscribe { navigator.showZoomedScreen(it.selectedComic) }

		sideEffects.ofType<SideEffect.StartSettings>()
				.subscribe { navigator.showSettings() }
	}

	override val events = Observable.mergeArray(

			sideEffects.ofType<SideEffect.LoadMore>()
					.dropMapSingle { (lastComic, lastIndex) ->
						(if(lastComic != null)
							comicRepository.getComicsAfter(lastComic, lastIndex)
						else
							comicRepository.getNewestComics()
						)
								.subscribeOn(ioScheduler)
								.retryDelayed(delay = 1000, times = 5)
								.map<Event>(Event::ComicsLoaded)
								.onErrorReturn(Event::LoadingFailed)
					},

			sideEffects.ofType<SideEffect.RefreshNewest>()
					.switchMapSingle { (newestComic) ->
						(if(newestComic != null)
							comicRepository.getComicsBefore(newestComic)
						else
							comicRepository.getNewestComics()
						)
								.subscribeOn(ioScheduler)
								.retryDelayed(delay = 1000, times = 5)
								.map<Event>(Event::DataRefreshed)
								.onErrorReturn(Event::RefreshFailed)
					},

			systemManager.observeInternetConnectivity()
					.subscribeOn(ioScheduler)
					.distinctUntilChanged()
					.map(Event::NetworkStatusChanged),

			sideEffects.ofType<SideEffect.DownloadImageForSharing>()
					.flatMapSingle { cmd ->
						comicRepository.loadBitmap(cmd.url)
								.subscribeOn(ioScheduler)
								.map { bmp -> Pair(bmp, cmd.title) }
					}
					.map { (bmp, title) -> Event.ImageDownloaded(bmp, title) }
	)!!

	override fun logEvent(event: Event) {
		super.logEvent(event)
		analytics.logEvent(event.javaClass.simpleName, null)
	}

	override fun reduce(oldState: State, event: Event) = when (event) {
		is Event.OrientationChanged -> StateUpdate(
				oldState.copy(
						feedOrientation =
								if (oldState.feedOrientation == VERTICAL)
									HORIZONTAL
								else
									VERTICAL
				)
		)

		is Event.Refresh            -> StateUpdate(
				oldState.copy(refreshing = true),
				SideEffect.RefreshNewest(oldState.comics.firstOrNull()))

		is Event.DataRefreshed   -> StateUpdate(
				oldState.copy(
						comics = event.latestComics + oldState.comics,
						refreshing = false
				),
				ViewEffect.ScrollToTop)

		is Event.RefreshFailed   -> StateUpdate(
				oldState.copy(refreshing = false),
				ViewEffect.ShowLoadingFailed)

		is Event.EndReached         ->
			if(oldState.loading)
				StateUpdate(oldState) // do nothing essentially
			else
				StateUpdate(
						oldState.copy(loading = true),
						SideEffect.LoadMore(oldState.comics.lastOrNull(), oldState.comics.lastIndex))

		is Event.LoadingFailed   -> StateUpdate(oldState.copy(loading = false), ViewEffect.ShowLoadingFailed)

		is Event.ComicsLoaded    ->
			if(event.newComics.isNotEmpty())
				StateUpdate(
						oldState.copy(
								comics = oldState.comics + event.newComics,
								loading = false
						)
				)
			else
				StateUpdate(oldState, ViewEffect.ShowNoMoreComics)

		is Event.SettingsClicked    -> StateUpdate(oldState, SideEffect.StartSettings)

		is Event.ComicClicked       -> StateUpdate(oldState, SideEffect.ShowEnlarged(event.selectedComic))

		is Event.ComicLongClicked   -> StateUpdate(oldState.copy(selectedComic = event.selectedComic))

		is Event.NetworkStatusChanged -> StateUpdate(oldState.copy(internetConnected = event.connected))

		is Event.ImageDownloaded -> StateUpdate(oldState, ViewEffect.Share(event.image, event.title))

		is Event.FailedToDownloadImage -> StateUpdate(oldState, ViewEffect.ShowFailedToShare)

		is Event.DialogCanceled -> StateUpdate(oldState.copy(selectedComic = null))

		is Event.SaveClicked -> {
			val comic = oldState.selectedComic!!

			StateUpdate(oldState.copy(selectedComic = null), SideEffect.SaveComic(comic))
		}

		is Event.ShareClicked -> {
			val url = oldState.selectedComic!!.imageUrl
			val title = oldState.selectedComic.title

			StateUpdate(oldState.copy(selectedComic = null), SideEffect.DownloadImageForSharing(url, title))
		}
	}
}