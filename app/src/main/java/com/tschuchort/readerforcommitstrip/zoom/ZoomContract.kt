package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract
import io.reactivex.Scheduler

interface ZoomContract : Contract {

	data class State(val comic: Comic) : Contract.State

	sealed class Event : Contract.Event {
		object ShareClicked : Event()
		object UpClicked : Event()
	}

	sealed class Command : Contract.Command {
		data class Share(val comic: Comic) : Command()
		object NavigateUp : Command()
	}

	abstract class Presenter(uiScheduler: Scheduler, compScheduler: Scheduler)
		: Contract.Presenter<State, Event, View, Command>(uiScheduler, compScheduler)

	interface View : Contract.View<State, Event, Command>
}