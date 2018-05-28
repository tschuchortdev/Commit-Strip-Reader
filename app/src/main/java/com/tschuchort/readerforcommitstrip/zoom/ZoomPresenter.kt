package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.State
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.View
import io.reactivex.Observable
import io.reactivex.Scheduler
import javax.inject.Inject

private typealias ProgramUpdate = Contract.ProgramUpdate<State, View>
private typealias StateChange = Contract.StateChange<State, View>
private typealias ViewAction = Contract.ViewAction<State, View>

@PerActivity
class ZoomPresenter
	@Inject constructor(val comic: Comic,
						@UiScheduler uiScheduler: Scheduler,
						private val navigator: Navigator,
						private val comicRepo: ComicRepository,
						private val storage: LocalStorage)
		: ZoomContract.Presenter(uiScheduler) {

	override val initialState = State(comic)

	init {
		bindSignal(View::upClicked).subscribe { navigator.navigateUp() }
	}

	override fun update() = Observable.mergeArray<ProgramUpdate>(
			handleSaveComic(storage = storage,
							comicRepo = comicRepo,
							saveSignal = bindSignal(View::saveClicked),
							getComic = latestState::comic,
							showSuccess = View::showSaveSuccessful,
							showFail = View::showSaveFailed),
			handleShareComic(comicRepo = comicRepo,
							 shareSignal = bindSignal(View::shareClicked),
							 getComic = latestState::comic,
							 share = View::share,
							 showDownloadFailed = View::showDownloadFailed)
	)
}
