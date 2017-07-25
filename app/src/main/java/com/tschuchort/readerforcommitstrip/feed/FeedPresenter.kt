package com.tschuchort.readerforcommitstrip.feed

import android.util.Log
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

	override val initialState = State.Default(emptyList(), Orientation.VERTICAL)

	override val events = Observable.merge(
			sideEffects.ofType<Command.LoadMore>()
					.dropMapSingle { (lastComic, lastIndex) ->
						(if(lastComic != null)
							comicRepository.getComicsAfter(lastComic, lastIndex)
						else
							comicRepository.getNewestComics()
						)
								.subscribeOn(ioScheduler)
								.map<Event> { Event.ComicsLoaded(it) }
								.doOnError { Log.e("Error", it.message) }
								//.onErrorReturn(Event.LoadingFailed)
					},
			sideEffects.ofType<Command.RefreshNewest>()
					.switchMapSingle { (newestComic) ->
						(if(newestComic != null)
							comicRepository.getComicsBefore(newestComic)
						else
							comicRepository.getNewestComics()
						)
								.subscribeOn(ioScheduler)
								.map<Event> { Event.DataRefreshed(it) }
								.doOnError { Log.e("Error", it.message) }
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
				null)

		is Event.RefreshFailed      -> Pair(
				State.Default(oldState.comics, oldState.feedOrientation),
				Command.ShowRefreshFailed)

		is Event.EndReached         ->
			if(oldState is State.LoadingMore)
				Pair(oldState, null)
			else
				Pair(
					State.LoadingMore(oldState.comics, oldState.feedOrientation),
					Command.LoadMore(oldState.comics.lastOrNull(), oldState.comics.lastIndex))

		is Event.LoadingFailed      ->
			// TODO retry?
			Pair(State.Default(oldState.comics, oldState.feedOrientation), null)

		is Event.ComicsLoaded       -> Pair(State.Default((oldState.comics + event.newComics), oldState.feedOrientation), null)

		is Event.SettingsClicked    -> Pair(oldState, Command.StartSettings)

		is Event.ComicClicked       -> Pair(oldState, Command.ShowEnlarged(event.selectedComic))

		is Event.ComicLongClicked   -> Pair(State.ShareDialog(event.selectedComic, oldState.comics, oldState.feedOrientation), null)
	}
}
