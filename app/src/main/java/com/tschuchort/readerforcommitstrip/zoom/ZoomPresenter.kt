package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.*
import io.reactivex.Scheduler
import javax.inject.Inject

@PerActivity
class ZoomPresenter
	@Inject constructor(comic: Comic,
					@UiScheduler uiScheduler: Scheduler,
					@ComputationScheduler compScheduler: Scheduler,
					logger: Logger)
		: ZoomContract.Presenter(uiScheduler, compScheduler, logger) {

	override val initialState = State(comic)

	override fun reduce(oldState: State, event: Event) = when(event) {
		is Event.ShareClicked -> Pair(oldState, Command.Share(oldState.comic))
		is Event.UpClicked -> Pair(oldState, Command.NavigateUp)
	}
}