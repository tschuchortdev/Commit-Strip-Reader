package com.tschuchort.readerforcommitstrip.zoom

import com.google.firebase.analytics.FirebaseAnalytics
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.PerActivity
import com.tschuchort.readerforcommitstrip.UiScheduler
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.*
import io.reactivex.Scheduler
import javax.inject.Inject

@PerActivity
class ZoomPresenter
	@Inject constructor(comic: Comic,
						@UiScheduler uiScheduler: Scheduler,
						private val analytics: FirebaseAnalytics)
		: ZoomContract.Presenter(uiScheduler) {

	override val initialState = State(comic)

	override fun logEvent(event: Event) {
		super.logEvent(event)
		analytics.logEvent(event.javaClass.simpleName, null)
	}

	override fun reduce(oldState: State, event: Event) = when(event) {
		is Event.ShareClicked -> Pair(oldState, Command.Share(oldState.comic))
		is Event.UpClicked -> Pair(oldState, Command.NavigateUp)
	}
}