package com.tschuchort.readerforcommitstrip

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.MainThread
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
		private var backgroundStreamDisposable: Disposable? = null
		private val viewEvents = PublishRelay.create<E>()
		private val stateRelay by lazy { BehaviorRelay.createDefault(initialState)!! }
		private val commandRelay by lazy { PublishRelay.create<C>()!! }


		private var restoredState = false
		private var attachedForFirstTime = true

		var viewIsAttached = false
			private set

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

		protected open val initCommand: C? = null

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
		@CallSuper
		@MainThread
		open fun onSaveInstanceState(outBundle: Bundle?) {
			outBundle!!.putParcelable(BUNDLE_KEY, stateRelay.value
												  ?: throw IllegalStateException("stateRelay relay is empty"))
		}

		@CallSuper
		@MainThread
		open fun onRestoreInstanceState(savedInstanceBundle: Bundle?) {
			val lastState = savedInstanceBundle?.getParcelable<S>(BUNDLE_KEY)

			if(lastState != null) {
				stateRelay.accept(lastState)
				restoredState = true
			}
		}

		/**
		 * called by view when created to connect to presenter
		 */
		@CallSuper
		@MainThread
		open fun attachView(view: V) {
			if(viewIsAttached)
				throw IllegalStateException("view is already attached")

            /* the view can subscribe to the relays in separate streams, so we can terminate them
               without terminating other streams that only live in the presenter/data layer

			   consumer needs to be subscribed first (build the stream bottom up), so no events get lost

			   common sense suggests to use observeOn(uiScheduler) here, because views can only be touched
			   on the UI thread. But this doesn't work because the first emission needs to be observed
			   immediately in a blocking fashion (instead of posting to the main thread looper) so
			   that the render method is called before the first layout pass and recyclerview has his
			   data set in time and can restore its scroll position after an orientation change.
			   All subsequent emissions will be observed on the thread where accept() was called
			   on the relay, so we need to ensure it's always called on the UI thread.
			   */
            viewDisposables += stateRelay.subscribe(view::render)
            viewDisposables += commandRelay.subscribe(view::doSideEffect)

			// subscribe to the view's events via a relay so we can unsubscribe
			// from those events separately
			viewDisposables += view.events.subscribeOn(uiScheduler).subscribe(viewEvents::accept)

			// only setup the stream inside the presenter once, so it is persistent
			// and parts of the stream that don't depend on the view can continue
			// running when the view is destroyed
			if(attachedForFirstTime) {
				// again build stream bottom up so no emissions get lost

                // log state updates
                stateRelay
                        .distinctUntilChanged()
                        .subscribe(this::logState)

                // log side effects
                commandRelay
                        .subscribe(this::logSideEffect)

				// no direct references to the view in this persistent stream,
				// so we don't get activity memory leaks
				// instead everyone subscribes to the respective relay
				backgroundStreamDisposable = Observable.merge(viewEvents, this.events)
						.observeOn(compScheduler)
						.doOnNext(this::logEvent)
						.map { event -> reduce(stateRelay.value!!, event) }
						.observeOn(uiScheduler) // VERY IMPORTANT! stateRelay.accept() needs to be called on UI thread always!
						.subscribe { (state, cmd) ->
							stateRelay.accept(state)

							if(cmd != null)
								commandRelay.accept(cmd)
						}

				if(!restoredState && initCommand != null)
					commandRelay.accept(initCommand!!)

				attachedForFirstTime = false
			}


			viewIsAttached = true
		}

		/**
		 * called by view when stopped to disconnect from presenter streams to avoid memory leaks
		 *
		 * @param isFinishing whether or not the activity/fragment is finishing, i.e the same view
		 * will not attach again (presenters should never be reused though)
		 */
		@CallSuper
		@MainThread
		open fun detachView(isFinishing: Boolean) {
			if(!viewIsAttached)
				throw IllegalStateException("view is already detached or was never attached")

			viewDisposables
					.onEach(Disposable::dispose)
					.clear()

			viewIsAttached = false

			if(isFinishing) {
				logger.d("Presenter", "presenter is finishing")
				backgroundStreamDisposable?.dispose()
				attachedForFirstTime = false
			}
		}

		protected open fun logEvent(event: E) {
			logger.i("Event", event::class.simpleName!!)
		}

		protected open fun logState(state: S) {
			logger.i("State", state.toString())
		}

		protected open fun logSideEffect(command: C) {
			logger.i("Command", command.toString())
		}
	}

	@MainThread
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
		val events: Observable<E>
	}
}