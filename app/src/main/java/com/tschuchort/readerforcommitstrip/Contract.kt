package com.tschuchort.readerforcommitstrip

import android.os.Bundle
import android.support.annotation.CallSuper
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.mironov.smuggler.AutoParcelable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import java.io.Serializable

interface Contract {
	interface State : AutoParcelable
	interface Event : Serializable
	interface Command : Serializable

	abstract class Presenter<S : State, E : Event, in V : View<S,E,C>, C : Command>(
			protected val uiScheduler: Scheduler,
			protected val compScheduler: Scheduler,
			protected val logger: Logger) {

		companion object {
			private const val BUNDLE_KEY = "presenter last state"
		}

		private val viewDisposables = ArrayList<Disposable>()
		private val viewEvents = PublishRelay.create<E>()
		private val stateRelay by lazy { BehaviorRelay.createDefault(initialState)!! }
		private val commandRelay by lazy { PublishRelay.create<C>()!! }

		private var attachedForFirstTime = true
		private var viewIsAttached = false

		/**
		 * emits state changes, subcribed by view
		 */
		val stateUpdates: Observable<S> by lazy { stateRelay }


		/**
		 * emits commands that cause side effects, may be subscribed by view and others
		 */
		val sideEffects: Observable<C> by lazy { commandRelay }

		/**
		 * initial state of the view
		 */
		protected abstract val initialState: S

		/**
		 * stream of other (non-ui) events that are fed into the reducer
		 */
		open val events: Observable<E> = Observable.never()

		/*
		 * reduces a new oldState from the previous oldState and event that occured
		 */
		protected abstract fun reduce(oldState: S, event: E): Pair<S,C?>

		/**
		 * called by view to save the last view state
		 */
		fun onSaveInstanceState(outBundle: Bundle?) {
			outBundle!!.putParcelable(BUNDLE_KEY, stateRelay.value
												  ?: throw IllegalStateException("stateRelay relay is empty"))
		}

		fun onRestoreInstanceState(savedInstanceBundle: Bundle?) {
			val lastState = savedInstanceBundle?.getParcelable<S>(BUNDLE_KEY)
		}

		/**
		 * called by view when created to connect to presenter
		 */
		@CallSuper
		open fun attechView(view: V) {
			if(viewIsAttached)
				throw IllegalStateException("view is already attached")

			// subscribe to the view's events via a relay so we can unsubscribe
			// from those events separately
			viewDisposables += view.events().subscribe(viewEvents::accept)

			// only setup the stream inside the presenter once, so it is persistent
			// and parts of the stream that don't depend on the view can continue
			// running when the view is destroyed
			if(attachedForFirstTime) {
				// no direct references to the view in this persistent stream,
				// so we don't get activity memory leaks
				// instead everyone subscribes to the respective relay
				Observable.merge(viewEvents, this.events)
						.doOnNext(this::logEvent)
						.subscribeOn(compScheduler)
						.map { event -> reduce(stateRelay.value!!, event) }
						.subscribe { (state, cmd) ->
							stateRelay.accept(state)

							if(cmd != null)
								commandRelay.accept(cmd)
						}

				// log state updates
				stateRelay
						.subscribeOn(compScheduler)
						.distinctUntilChanged()
						.subscribe(this::logState)

				// log side effects
				commandRelay
						.subscribeOn(compScheduler)
						.subscribe(this::logSideEffect)

				attachedForFirstTime = false
			}

			// now the view can subscribe to the relays in separate streams, so we can terminate them
			// without terminating other streams that only live in the presenter/data layer
			viewDisposables += stateRelay.observeOn(uiScheduler).subscribe(view::render)
			viewDisposables += commandRelay.observeOn(uiScheduler).subscribe(view::doSideEffect)

			viewIsAttached = true
		}

		/**
		 * called by view when destroyed to disconnect from presenter
		 */
		@CallSuper
		open fun detachView() {
			if(!viewIsAttached)
				throw IllegalStateException("view is already detached or was never attached")

			viewDisposables
					.onEach(Disposable::dispose)
					.clear()

			viewIsAttached = false
		}

		fun logEvent(event: E) {
			logger.d("Event", event::class.simpleName!!)
		}

		fun logState(state: S) {
			logger.d("State", state.toString())
		}

		fun logSideEffect(command: C) {
			logger.d("Command", command.toString())
		}
	}

	interface View<in S : State, E: Event, in C : Command> {

		/**
		 * updates the view to display the current state.
		 */
		fun render(state: S)

		/**
		 * execute a UI side-effect that doesn't really have a lasting effect
		 * on the view's internal state, for example toasts
		 */
		fun doSideEffect(command: C) {}

		/**
		 * provides the view's events that the presenter subscribes to
		 *
		 * called immediatley after the presenter is attached
		 */
		fun events(): Observable<E>
	}
}