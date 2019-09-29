package com.tschuchort.readerforcommitstrip

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import arrow.core.Either
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.Serializable

interface Contract {
	interface State : Parcelable

	interface Event : Serializable

	interface Effect : Serializable
	interface SideEffect : Effect
	interface ViewEffect : Effect
	private enum class EffectType { Side, View }

	abstract class Presenter<
			St : State,              // state super type
			Ev : Event,              // event super type
			in Vi : View<St,Ev,VEf>,  // view type
			SEf : SideEffect,         // type for commands that are side effects in the presenter
			VEf : ViewEffect       // type for commands that are side effects in the view
			>
		constructor(private val uiScheduler: Scheduler) {

		companion object {
			private const val BUNDLE_KEY = "presenter last state"
		}

		private val viewDisposables = ArrayList<Disposable>()
		private var backgroundStreamDisposable: Disposable? = null
		private val viewEvents = PublishRelay.create<Ev>()!!
		private val stateRelay = BehaviorRelay.create<St>()!!
		private val sideEffectRelay = PublishRelay.create<SEf>()!!
		private val viewEffectRelay = QueueRelay<VEf>()

		private var restoredState = false
		private var attachedForFirstTime = true

		var viewIsAttached = false
			private set

		/**
		 * a class to represent the result of the `reduce` function. `reduce` actually returns
		 * somewhat of a `Pair<St, Either<SEf,VEf>>` but this class uses constructor overloading
		 * to get rid of the ugly `Left()` or `Right()` calls, as well as saving the type of Effect
		 * so they can be pattern matched against (since the actual type information of the generics
		 * is erased)
		 */
		protected inner class StateUpdate
			private constructor(
					val newState: St,
					private val taggedEffect: Pair<Effect, EffectType>?){

			constructor(newState: St, effect: SEf)
					: this(newState, Pair(effect, EffectType.Side))

			constructor(newState: St, effect: VEf)
					: this(newState, Pair(effect, EffectType.View))

			constructor(newState: St) : this(newState, null)

			operator fun component1() = newState
			operator fun component2() = effect

			@Suppress("UNCHECKED_CAST")
			val effect: Either<SEf, VEf>? get() = when(taggedEffect?.second) {
				EffectType.Side -> Either.Left(taggedEffect.first as SEf)
				EffectType.View -> Either.Right(taggedEffect.first as VEf)
				null -> null
			}
		}

		/**
		 * emits state changes, subcribed by view
		 */
		val stateUpdates: Observable<St> by lazy { stateRelay }


		/**
		 * emits commands that cause side effects, may be subscribed by view and others
		 */
		val sideEffects: Observable<SEf> = sideEffectRelay

		/**
		 * initial state of the view
		 */
		protected abstract val initialState: St

		/**
		 * function that restores the state after the process has died completely
		 *
		 * this can be useful to reset flags for loading, refeshing and so on,
		 * depending on a background task running in the presenter to toggle them, which
		 * died with the process
		 *
		 * since the presenter is retained through config changes, restoring the state
		 * won't be neccessary then
		 */
		protected open fun restoreState(savedState: St): St = savedState

		/**
		 * command that will be executed once to start an initial side effect
		 * like loading
		 */
		protected open val initCommand: SEf? = null

		/**
		 * stream of other (non-ui) events that are fed into the reducer
		 */
		open val events: Observable<Ev> = Observable.never()

		/*
		 * reduces a new oldState from the previous oldState and event that occured
		 */
		protected abstract fun reduce(oldState: St, event: Ev): StateUpdate

		/**
		 * called by view to save the last view state
		 */
		@CallSuper
		@MainThread
		open fun onSaveInstanceState(outBundle: Bundle?) {
			outBundle!!.putParcelable(BUNDLE_KEY,
					stateRelay.value ?: throw IllegalStateException("stateRelay relay is empty"))
		}

		@CallSuper
		@MainThread
		open fun onRestoreInstanceState(savedInstanceBundle: Bundle?) {
			val savedState = savedInstanceBundle?.getParcelable<St>(BUNDLE_KEY)

			if(savedState != null && attachedForFirstTime) {
				stateRelay.accept(restoreState(savedState))
				restoredState = true
			}
		}

		/**
		 * called by view when created to connect to presenter
		 */
		@CallSuper
		@MainThread
		open fun attachView(view: Vi) {
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
            viewDisposables += viewEffectRelay.subscribe(view::doSideEffect)

			// subscribe to the view's events via a relay so we can unsubscribe
			// from those events separately
			viewDisposables += view.events.subscribeOn(uiScheduler).subscribe(viewEvents::accept)

			// only setup the stream inside the presenter once, so it is persistent
			// and parts of the stream that don't depend on the view can continue
			// running when the view is destroyed
			if(attachedForFirstTime) {
				// again build stream bottom up so no emissions get lost

				// state has to be initialized before the backgroundStream is
				// started, or stateRealy.value is not available when some event
				// in the presenter immediately upon subscribtion
				if(!restoredState)
					stateRelay.accept(initialState)

                // log state updates
                stateRelay
                        .distinctUntilChanged()
                        .subscribe(this::logState)

                // log side effects
                viewEffectRelay.subscribe(this::logSideEffect)
                sideEffectRelay.subscribe(this::logSideEffect)

				// no direct references to the view in this persistent stream,
				// so we don't get activity memory leaks
				// instead everyone subscribes to the respective relay
				backgroundStreamDisposable = Observable.merge(viewEvents, this.events)
						.observeOn(Schedulers.computation())
						.doOnNext(this::logEvent)
						.map { event -> reduce(getCurrentState(), event) }
						.observeOn(uiScheduler) // VERY IMPORTANT! stateRelay.accept() needs to be called on UI thread always!
						.subscribe { (state, cmd) ->
							stateRelay.accept(state)
							dispatchEffect(cmd)
						}

				// command has to be send after the backgroundStream is setup
				if(!restoredState && initCommand != null)
					dispatchEffect(Either.left(initCommand!!))

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
				Timber.d("presenter is finishing")
				backgroundStreamDisposable?.dispose()
				attachedForFirstTime = false
			}
		}

		protected open fun logEvent(event: Ev) {
			Timber.i("Event: $event")
		}

		protected open fun logState(state: St) {
			Timber.i("State: $state")
		}

		protected open fun logSideEffect(effect: Effect) {
			Timber.i("Effect: $effect")
		}

		private fun dispatchEffect(effect: Either<SEf,VEf>?) = when(effect) {
			is Either.Left -> sideEffectRelay.accept(effect.a)
			is Either.Right -> viewEffectRelay.accept(effect.b)
			null -> Unit
		}

		private fun getCurrentState()
				= stateRelay.value
				?: throw IllegalStateException("could not get current state from relay. " +
													   "This should never happen. " +
													   "Was the State not initialized?")
	}

	@MainThread
	interface View<in St : State, Ev: Event, in Ef : ViewEffect> {

		/**
		 * updates the view to display the current state.
		 */
		fun render(state: St)

		/**
		 * execute a UI side-effect that doesn't really have a lasting effect
		 * on the view's internal state, for example toasts
		 */
		fun doSideEffect(effect: Ef) {}

		/**
		 * provides the view's events that the presenter subscribes to
		 *
		 * called immediatley after the presenter is attached
		 */
		val events: Observable<Ev>
	}
}