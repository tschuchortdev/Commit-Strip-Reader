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
		object SaveClicked : Event()
		object UpClicked : Event()
		object SaveSuccessful : Event()
		data class SaveFailed(val t: Throwable) : Event()
	}

	sealed class SideEffect : Contract.SideEffect {
		object NavigateUp : SideEffect()
		data class SaveComic(val comic: Comic) : SideEffect()
	}

	sealed class ViewEffect : Contract.ViewEffect {
		data class ShareComic(val comic: Comic) : ViewEffect()
		object ShowSaveSuccesful : ViewEffect()
		data class ShowSaveFailed(val t: Throwable) : ViewEffect()
	}

	abstract class Presenter(uiScheduler: Scheduler)
		: Contract.Presenter<State, Event, View, SideEffect, ViewEffect>(uiScheduler) {

		interface Factory {
			fun create(selectedComic: Comic): Presenter
		}
	}

	interface View : Contract.View<State, Event, ViewEffect>
}