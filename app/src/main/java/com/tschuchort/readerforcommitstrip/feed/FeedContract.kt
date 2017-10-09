package com.tschuchort.readerforcommitstrip.feed


import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract
import com.tschuchort.readerforcommitstrip.Logger
import io.reactivex.Scheduler

interface FeedContract : Contract {
	enum class Orientation { VERTICAL, HORIZONTAL }

	sealed class State: Contract.State {
		open var comics: List<Comic> = emptyList()
		open var feedOrientation: Orientation = Orientation.VERTICAL
		abstract var internetConnected: Boolean

		data class Default(
				override var comics: List<Comic>,
				override var feedOrientation: Orientation,
				override var internetConnected: Boolean) : State()

		data class LoadingMore(
				override var comics: List<Comic>,
				override var feedOrientation: Orientation,
				override var internetConnected: Boolean) : State()

		data class Refreshing(
				override var comics: List<Comic>,
				override var feedOrientation: Orientation,
				override var internetConnected: Boolean) : State()
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
		data class NetworkStatusChanged(val connected: Boolean): Event()
	}

	sealed class Command : Contract.Command {
		data class LoadMore(val lastComic: Comic? = null, val lastIndex: Int = 0) : Command()
		data class RefreshNewest(val newestComic: Comic? = null) : Command()
		data class ShowEnlarged(val selectedComic: Comic) : Command()
		data class Share(val selectedComic: Comic) : Command()
		object StartSettings : Command()
		object ShowLoadingFailed : Command()
		object ShowNoMoreComics : Command()
		object ScrollToTop : Command()
	}

	abstract class Presenter(uiScheduler: Scheduler, compScheduler: Scheduler, logger: Logger)
		: Contract.Presenter<State, Event, View, Command>(uiScheduler, compScheduler, logger)

	interface View : Contract.View<State, Event, Command>
}
