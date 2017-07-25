package com.tschuchort.readerforcommitstrip

import android.os.Bundle
import android.support.annotation.CallSuper
import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.mironov.smuggler.AutoParcelable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import org.funktionale.option.toOption
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
		private var attachedForFirstTime = true

		/**
		 * emits state changes, subcribed by view
		 */
		val stateUpdates by lazy { BehaviorRelay.createDefault(initialState)!! }

		/**
		 * emits sideEffects that cause side effects, may be subscribed by view and others
		 */
		val sideEffects by lazy { PublishRelay.create<C>()!! }

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
			outBundle!!.putParcelable(BUNDLE_KEY, stateUpdates.value
												  ?: throw IllegalStateException("stateUpdates relay is empty"))
		}

		fun onRestoreInstanceState(savedInstanceBundle: Bundle?) {
			val lastState = savedInstanceBundle?.getParcelable<S>(BUNDLE_KEY)
		}

		/**
		 * called by view when created to connect to presenter
		 */
		@CallSuper
		open fun attechView(view: V) {
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
						.map { event -> reduce(stateUpdates.value!!, event) }
						.subscribe { (state, cmd) ->
							stateUpdates.accept(state)

							if(cmd != null)
								sideEffects.accept(cmd)
						}

				// log state updates
				stateUpdates
						.subscribeOn(compScheduler)
						.distinctUntilChanged()
						.subscribe(this::logState)

				// log side effects
				sideEffects
						.subscribeOn(compScheduler)
						.subscribe(this::logSideEffect)

				attachedForFirstTime = false
			}

			// now the view can subscribe to the relays in separate streams, so we can terminate them
			// without terminating other streams that only live in the presenter/data layer
			viewDisposables += stateUpdates.observeOn(uiScheduler).subscribe(view::render)
			viewDisposables += sideEffects.observeOn(uiScheduler).subscribe(view::doSideEffect)
		}

		/**
		 * called by view when destroyed to disconnect from presenter
		 */
		@CallSuper
		open fun detachView() {
			viewDisposables
					.onEach(Disposable::dispose)
					.clear()
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