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

	override val initCommand = Command.RefreshNewest()

	init {
		sideEffects.ofType<Command.ShowEnlarged>()
				.subscribe { navigator.showZoomedScreen(it.selectedComic) }

		sideEffects.ofType<Command.StartSettings>()
				.subscribe { navigator.showSettings() }
	}

	override val events = Observable.mergeArray(

			sideEffects.ofType<Command.LoadMore>()
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

			sideEffects.ofType<Command.RefreshNewest>()
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

			sideEffects.ofType<Command.DownloadImageForSharing>()
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
		is Event.OrientationChanged -> Pair(
				oldState.copy(
						feedOrientation =
								if (oldState.feedOrientation == VERTICAL)
									HORIZONTAL
								else
									VERTICAL
				),
				null)

		is Event.Refresh            -> Pair(
				oldState.copy(refreshing = true),
				Command.RefreshNewest(oldState.comics.firstOrNull()))

		is Event.DataRefreshed      -> Pair(
				oldState.copy(
						comics = event.latestComics + oldState.comics,
						refreshing = false
				),
				Command.ScrollToTop)

		is Event.RefreshFailed      -> Pair(
				oldState.copy(refreshing = false),
				Command.ShowLoadingFailed)

		is Event.EndReached         ->
			if(oldState.loading)
				Pair(oldState, null) // do nothing essentially
			else
				Pair(
					oldState.copy(loading = true),
					Command.LoadMore(oldState.comics.lastOrNull(), oldState.comics.lastIndex))

		is Event.LoadingFailed      -> Pair(oldState.copy(loading = false), Command.ShowLoadingFailed)

		is Event.ComicsLoaded       ->
			if(event.newComics.isNotEmpty())
				Pair(
						oldState.copy(
								comics = oldState.comics + event.newComics,
								loading = false
						),
						null)
			else
				Pair(oldState, Command.ShowNoMoreComics)

		is Event.SettingsClicked    -> Pair(oldState, Command.StartSettings)

		is Event.ComicClicked       -> Pair(oldState, Command.ShowEnlarged(event.selectedComic))

		is Event.ComicLongClicked   -> Pair(oldState.copy(selectedComic = event.selectedComic), null)

		is Event.NetworkStatusChanged -> Pair(oldState.copy(internetConnected = event.connected), null)

		is Event.ImageDownloaded -> Pair(oldState, Command.Share(event.image, event.title))

		is Event.FailedToDownloadImage -> Pair(oldState, Command.ShowFailedToShare)

		is Event.DialogCanceled -> Pair(oldState.copy(selectedComic = null), null)

		is Event.SaveClicked -> {
			val comic = oldState.selectedComic!!

			Pair(oldState.copy(selectedComic = null), Command.SaveComic(comic))
		}

		is Event.ShareClicked -> {
			val url = oldState.selectedComic!!.imageUrl
			val title = oldState.selectedComic!!.title

			Pair(oldState.copy(selectedComic = null), Command.DownloadImageForSharing(url, title))
		}
	}
}