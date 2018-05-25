package com.tschuchort.readerforcommitstrip.zoom

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract
import io.reactivex.Observable
import io.reactivex.Scheduler

interface ZoomContract : Contract {

	@SuppressLint("ParcelCreator")
	data class State(val comic: Comic) : Contract.State

	abstract class Presenter(uiScheduler: Scheduler)
		: Contract.Presenter<State, View>(uiScheduler) {

		interface Factory {
			fun create(selectedComic: Comic): Presenter
		}
	}

	interface View : Contract.View<State> {
		// signals
		val shareClicked: Observable<Unit>
		val saveClicked: Observable<Unit>
		val upClicked: Observable<Unit>

		// effects
		fun share(image: Bitmap, title: String)
		fun showSaveSuccessful()
		fun showSaveFailed()
		fun showDownloadFailed()
	}
}