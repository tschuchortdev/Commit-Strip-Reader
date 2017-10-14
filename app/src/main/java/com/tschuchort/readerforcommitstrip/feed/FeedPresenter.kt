package com.tschuchort.readerforcommitstrip.feed

import com.google.firebase.analytics.FirebaseAnalytics
import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.feed.FeedContract.*
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.ofType
import javax.inject.Inject

@PerActivity
class FeedPresenter
		@Inject constructor(
				private val comicRepository: ComicRepository,
				@UiScheduler uiScheduler: Scheduler,
				@ComputationScheduler compScheduler: Scheduler,
				@IoScheduler val ioScheduler: Scheduler,
				systemManager: SystemManager,
				private val analytics: FirebaseAnalytics)
		: Presenter(uiScheduler, compScheduler) {

	override val initialState = State.Refreshing(emptyList(), Orientation.VERTICAL, true)

	override val initCommand = Command.RefreshNewest()

	override val events = Observable.merge(
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
								.onErrorReturn(Event.LoadingFailed)
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
								.onErrorReturn(Event.RefreshFailed)
					},
			systemManager.observeInternetConnectivity()
					.subscribeOn(ioScheduler)
					.distinctUntilChanged()
					.map(Event::NetworkStatusChanged))!!

	override fun logEvent(event: Event) {
		super.logEvent(event)
		analytics.logEvent(event.javaClass.simpleName, null)
	}

	override fun reduce(oldState: State, event: Event) = when (event) {
		is Event.OrientationChanged -> Pair(
				State.Default(
						comics = oldState.comics,
						feedOrientation =
								if (oldState.feedOrientation == Orientation.VERTICAL)
									Orientation.HORIZONTAL
								else
									Orientation.VERTICAL,
						internetConnected = oldState.internetConnected),
				null)

		is Event.Refresh            -> Pair(
				State.Refreshing(oldState.comics, oldState.feedOrientation, oldState.internetConnected),
				Command.RefreshNewest(oldState.comics.firstOrNull()))

		is Event.DataRefreshed      -> Pair(
				State.Default((event.latestComics + oldState.comics), oldState.feedOrientation, oldState.internetConnected),
				Command.ScrollToTop)

		is Event.RefreshFailed      -> Pair(
				State.Default(oldState.comics, oldState.feedOrientation, oldState.internetConnected),
				Command.ShowLoadingFailed)

		is Event.EndReached         ->
			if(oldState is State.LoadingMore)
				Pair(oldState, null)
			else
				Pair(
					State.LoadingMore(oldState.comics, oldState.feedOrientation, oldState.internetConnected),
					Command.LoadMore(oldState.comics.lastOrNull(), oldState.comics.lastIndex))

		is Event.LoadingFailed      ->
			Pair(State.Default(oldState.comics, oldState.feedOrientation, oldState.internetConnected), Command.ShowLoadingFailed)

		is Event.ComicsLoaded       ->
			if(event.newComics.isNotEmpty())
				Pair(State.Default(
						(oldState.comics + event.newComics), oldState.feedOrientation, oldState.internetConnected),
						null)
			else
				Pair(oldState, Command.ShowNoMoreComics)

		is Event.SettingsClicked    -> Pair(oldState, Command.StartSettings)

		is Event.ComicClicked       -> Pair(oldState, Command.ShowEnlarged(event.selectedComic))

		is Event.ComicLongClicked   -> Pair(oldState, Command.Share(event.selectedComic))

		is Event.NetworkStatusChanged -> Pair(
				oldState.apply { internetConnected = event.connected },
				null)
	}
}