package com.tschuchort.readerforcommitstrip.feed


import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract
import com.tschuchort.readerforcommitstrip.Logger
import io.reactivex.Scheduler

interface FeedContract : Contract {
	enum class Orientation { VERTICAL, HORIZONTAL }

	sealed class State: Contract.State {
		open val comics: List<Comic> = emptyList()
		open val feedOrientation: Orientation = Orientation.VERTICAL

		data class Default(
				override val comics: List<Comic>,
				override val feedOrientation: Orientation) : State()

		data class ShareDialog(
				val selectedComic: Comic,
				override val comics: List<Comic>,
				override val feedOrientation: Orientation) : State()

		data class NoInternet(
				override val comics: List<Comic>,
				override val feedOrientation: Orientation) : State()

		data class LoadingMore(
				override val comics: List<Comic>,
				override val feedOrientation: Orientation) : State()

		data class Refreshing(
				override val comics: List<Comic>,
				override val feedOrientation: Orientation) : State()
	}

	sealed class Event : Contract.Event {
		object EndReached : Event()
		object SettingsClicked : Event()
		object Refresh : Event()
		object OrientationChanged : Event()
		data class ComicsLoaded(val newComics: List<Comic>) : Event()
		object LoadingFailed : Event()
		data class DataRefreshed(val latestComics: List<Comic>) : Event()
		object RefreshFailed : Event()
		data class ComicClicked(val selectedComic: Comic): Event()
		data class ComicLongClicked(val selectedComic: Comic): Event()
	}

	sealed class Command : Contract.Command {
		data class LoadMore(val lastComic: Comic?, val lastIndex: Int) : Command()
		data class RefreshNewest(val newestComic: Comic?) : Command()
		data class ShowEnlarged(val selectedComic: Comic) : Command()
		object StartSettings : Command()
		object ShowLoadingFailed : Command()
		object ShowNoMoreComics : Command()
	}

	abstract class Presenter(uiScheduler: Scheduler, compScheduler: Scheduler, logger: Logger)
		: Contract.Presenter<State, Event, View, Command>(uiScheduler, compScheduler, logger)

	interface View : Contract.View<State, Event, Command>
}
