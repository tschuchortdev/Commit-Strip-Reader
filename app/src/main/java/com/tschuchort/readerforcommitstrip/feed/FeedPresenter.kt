package com.tschuchort.readerforcommitstrip.feed

import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.feed.FeedContract.*
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.ofType
import javax.inject.Inject

@PerActivity
class FeedPresenter
		@Inject constructor(
				val comicRepository: ComicRepository,
				@UiScheduler uiScheduler: Scheduler,
				@ComputationScheduler compScheduler: Scheduler,
				@IoScheduler val ioScheduler: Scheduler,
				logger: Logger)
		: Presenter(uiScheduler, compScheduler, logger) {

	private val TAG = "FeedPresenter"

	override val initialState = State.Refreshing(emptyList(), Orientation.VERTICAL)

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
								.doOnSubscribe { logger.d(TAG, "loading more comics") }
								.retryDelayed(delay = 1000, times = 5)
								.map<Event>(Event::ComicsLoaded)
								.doOnError { logger.e(TAG, it.message) }
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
								.doOnSubscribe { logger.d(TAG, "refreshing comics") }
								.retryDelayed(delay = 1000, times = 5)
								.map<Event>(Event::DataRefreshed)
								.doOnError { logger.e(TAG, it.message) }
								.onErrorReturn(Event.RefreshFailed)
					})!!

	override fun reduce(oldState: State, event: Event) = when (event) {
		is Event.OrientationChanged -> Pair(
				State.Default(
						comics = oldState.comics,
						feedOrientation =
								if (oldState.feedOrientation == Orientation.VERTICAL)
									Orientation.HORIZONTAL
								else
									Orientation.VERTICAL),
				null)

		is Event.Refresh            -> Pair(
				State.Refreshing(oldState.comics, oldState.feedOrientation),
				Command.RefreshNewest(oldState.comics.firstOrNull()))

		is Event.DataRefreshed      -> Pair(
				State.Default((event.latestComics + oldState.comics), oldState.feedOrientation),
				Command.ScrollToTop)

		is Event.RefreshFailed      ->
			Pair(State.Default(oldState.comics, oldState.feedOrientation), Command.ShowLoadingFailed)

		is Event.EndReached         ->
			if(oldState is State.LoadingMore)
				Pair(oldState, null)
			else
				Pair(
					State.LoadingMore(oldState.comics, oldState.feedOrientation),
					Command.LoadMore(oldState.comics.lastOrNull(), oldState.comics.lastIndex))

		is Event.LoadingFailed      ->
			Pair(State.Default(oldState.comics, oldState.feedOrientation), Command.ShowLoadingFailed)

		is Event.ComicsLoaded       ->
			if(event.newComics.isNotEmpty())
				Pair(State.Default((oldState.comics + event.newComics), oldState.feedOrientation), null)
			else
				Pair(oldState, Command.ShowNoMoreComics)

		is Event.SettingsClicked    -> Pair(oldState, Command.StartSettings)

		is Event.ComicClicked       -> Pair(oldState, Command.ShowEnlarged(event.selectedComic))

		is Event.ComicLongClicked   -> Pair(oldState, Command.Share(event.selectedComic))
	}
}