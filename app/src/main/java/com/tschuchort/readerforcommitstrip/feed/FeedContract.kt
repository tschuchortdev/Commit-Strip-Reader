package com.tschuchort.readerforcommitstrip.feed


import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract
import io.reactivex.Observable
import io.reactivex.Scheduler

interface FeedContract : Contract {
	enum class Orientation { VERTICAL, HORIZONTAL }

	@SuppressLint("ParcelCreator")
	data class State(
			val comics: List<Comic>,
			val internetConnected: Boolean,
			val feedOrientation: Orientation,
			val selectedComic: Comic?,
			val loading: Boolean,
			val refreshing: Boolean)
		: Contract.State

	abstract class Presenter(uiScheduler: Scheduler)
		: Contract.Presenter<State, View>(uiScheduler)

	interface View : Contract.View<State> {
		// signals
		val endReached: Observable<Unit>
		val settingsClicked: Observable<Unit>
		val refresh: Observable<Unit>
		val changeFeedLayoutClicked: Observable<Unit>
		val shareClicked: Observable<Unit>
		val saveClicked: Observable<Unit>
		val comicClicked: Observable<Comic>
		val comicLongClicked: Observable<Comic>
		val dialogCanceled: Observable<Unit>

		// effects
		fun share(image: Bitmap, title: String)
		fun showRefreshFailed()
		fun showNoMoreComics()
		fun scrollToTop()
		fun showShareFailed()
		fun showSaveSuccesful()
		fun showSaveFailed()
		fun showDownloadFailed()
	}
}
