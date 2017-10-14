package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.ComputationScheduler
import com.tschuchort.readerforcommitstrip.PerActivity
import com.tschuchort.readerforcommitstrip.UiScheduler
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.*
import io.reactivex.Scheduler
import javax.inject.Inject

@PerActivity
class ZoomPresenter
	@Inject constructor(comic: Comic,
					@UiScheduler uiScheduler: Scheduler,
					@ComputationScheduler compScheduler: Scheduler)
		: ZoomContract.Presenter(uiScheduler, compScheduler) {

	override val initialState = State(comic)

	override fun reduce(oldState: State, event: Event) = when(event) {
		is Event.ShareClicked -> Pair(oldState, Command.Share(oldState.comic))
		is Event.UpClicked -> Pair(oldState, Command.NavigateUp)
	}
}