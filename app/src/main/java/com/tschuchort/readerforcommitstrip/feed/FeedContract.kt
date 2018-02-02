package com.tschuchort.readerforcommitstrip.feed


import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract
import io.reactivex.Scheduler

interface FeedContract : Contract {
	enum class Orientation { VERTICAL, HORIZONTAL }

	@SuppressLint("ParcelCreator")
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
		object SaveSuccessful : Event()
		data class SaveFailed(val t: Throwable) : Event()
	}

	sealed class SideEffect : Contract.SideEffect {
		data class LoadMore(val lastComic: Comic? = null, val lastIndex: Int = 0) : SideEffect()
		data class RefreshNewest(val newestComic: Comic? = null) : SideEffect()
		data class DownloadImageForSharing(val url: String, val title: String) : SideEffect()
		data class SaveComic(val comic: Comic) : SideEffect()
		data class ShowEnlarged(val selectedComic: Comic) : SideEffect()
		object StartSettings : SideEffect()
	}

	sealed class ViewEffect : Contract.ViewEffect {
		data class Share(val image: Bitmap, val title: String) : ViewEffect()
		object ShowLoadingFailed : ViewEffect()
		object ShowNoMoreComics : ViewEffect()
		object ScrollToTop : ViewEffect()
		object ShowShareFailed : ViewEffect()
		object ShowSaveSuccesful : ViewEffect()
		object ShowSaveFailed : ViewEffect()
	}

	abstract class Presenter(uiScheduler: Scheduler)
		: Contract.Presenter<State, Event, View, SideEffect, ViewEffect>(uiScheduler)

	interface View : Contract.View<State, Event, ViewEffect>
}
