package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract
import com.tschuchort.readerforcommitstrip.Logger
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

	abstract class Presenter(uiScheduler: Scheduler, compScheduler: Scheduler, logger: Logger)
		: Contract.Presenter<State, Event, View, Command>(uiScheduler, compScheduler, logger)

	interface View : Contract.View<State, Event, Command>
}