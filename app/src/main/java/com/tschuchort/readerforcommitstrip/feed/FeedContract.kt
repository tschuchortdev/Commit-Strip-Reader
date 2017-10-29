package com.tschuchort.readerforcommitstrip.feed


import android.graphics.Bitmap
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract
import io.reactivex.Scheduler

interface FeedContract : Contract {
	enum class Orientation { VERTICAL, HORIZONTAL }

	data class State(
			val comics: List<Comic>,
			val feedOrientation: Orientation,
			val internetConnected: Boolean,
			val selectedComic: Comic?,
			val loading: Boolean,
			val refreshing: Boolean)
		: Contract.State

	sealed class Event : Contract.Event {
		object EndReached : Event()
		object SettingsClicked : Event()
		object Refresh : Event()
		object OrientationChanged : Event()
		data class ComicsLoaded(val newComics: List<Comic>) : Event()
		data class LoadingFailed(val t: Throwable) : Event()
		data class DataRefreshed(val latestComics: List<Comic>) : Event()
		data class RefreshFailed(val t: Throwable) : Event()
		data class ComicClicked(val selectedComic: Comic) : Event()
		data class ComicLongClicked(val selectedComic: Comic) : Event()
		data class NetworkStatusChanged(val connected: Boolean) : Event()
		data class ImageDownloaded(val image: Bitmap, val title: String) : Event()
		data class FailedToDownloadImage(val t: Throwable) : Event()
		object ShareClicked : Event()
		object SaveClicked : Event()
		object DialogCanceled : Event()
	}

	sealed class Command : Contract.Command {
		data class LoadMore(val lastComic: Comic? = null, val lastIndex: Int = 0) : Command()
		data class RefreshNewest(val newestComic: Comic? = null) : Command()
		data class ShowEnlarged(val selectedComic: Comic) : Command()
		data class Share(val image: Bitmap, val title: String) : Command()
		object StartSettings : Command()
		object ShowLoadingFailed : Command()
		object ShowNoMoreComics : Command()
		object ScrollToTop : Command()
		data class DownloadImageForSharing(val url: String, val title: String) : Command()
		data class SaveComic(val comic: Comic) : Command()
		object ShowFailedToShare : Command()
		object ShowFailedToSave : Command()
	}

	abstract class Presenter(uiScheduler: Scheduler)
		: Contract.Presenter<State, Event, View, Command>(uiScheduler)

	interface View : Contract.View<State, Event, Command>
}
