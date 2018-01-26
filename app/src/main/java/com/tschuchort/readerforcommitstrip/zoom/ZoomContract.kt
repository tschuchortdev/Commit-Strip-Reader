package com.tschuchort.readerforcommitstrip.zoom

import android.annotation.SuppressLint
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract
import io.reactivex.Scheduler

interface ZoomContract : Contract {

	@SuppressLint("ParcelCreator")
	data class State(val comic: Comic) : Contract.State

	sealed class Event : Contract.Event {
		object ShareClicked : Event()
		object UpClicked : Event()
	}

	sealed class SideEffect : Contract.SideEffect {
		object NavigateUp : SideEffect()
	}

	sealed class ViewEffect : Contract.ViewEffect {
		data class Share(val comic: Comic) : ViewEffect()
	}

	abstract class Presenter(uiScheduler: Scheduler)
		: Contract.Presenter<State, Event, View, SideEffect, ViewEffect>(uiScheduler) {

		interface Factory {
			fun create(selectedComic: Comic): Presenter
		}
	}

	interface View : Contract.View<State, Event, ViewEffect>
}