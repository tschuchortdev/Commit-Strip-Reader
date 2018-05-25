package com.tschuchort.readerforcommitstrip

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.MainThread
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import io.mironov.smuggler.AutoParcelable
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

interface Contract {
	interface State : AutoParcelable

	open class ProgramUpdate<St : Contract.State, Vi : Contract.View<St>>(
			val stateChange: (St.() -> Any?)? = null,
			val viewAction: (Vi.() -> Any?)? = null) {

		operator fun component1() = stateChange
		operator fun component2() = viewAction
	}

	class StateUpdate<St : Contract.State, Vi : Contract.View<St>>(stateChange: St.() -> Any?)
		: ProgramUpdate<St,Vi>(stateChange, null)

	class ViewAction<St : Contract.State, Vi : Contract.View<St>>(viewAction: Vi.() -> Any?)
		: ProgramUpdate<St,Vi>(null, viewAction)


	abstract class Presenter<St : State, Vi : View<St>>(private val uiScheduler: Scheduler) {

		companion object {
			private const val BUNDLE_KEY = "presenter last state"
		}

		private val viewDisposables = ArrayList<Disposable>()
		private var combinedStreamDisposable: Disposable? = null
		private val stateRelay = BehaviorRelay.create<St>()!!
		private val viewActionRelay = QueueRelay<Vi.() -> Any?>()

		private val viewSignalBinders = ArrayList<ViewEventBinderPair<*, Vi>>()

		private var restoredState = false
		private var attachedForFirstTime = true

		private var attachedView: Vi? = null
		fun isViewAttached() = attachedView != null

		/**
		 * emits state changes, subcribed by view
		 */
		val stateUpdates: Flowable<St> by lazy { stateRelay.toFlowable(BackpressureStrategy.LATEST) }

		/**
		 * initial state of the view
		 */
		protected abstract val initialState: St

		/**
		 * the current state
		 */
		val latestState: St
			get() = stateRelay.value ?: throw IllegalStateException(
					"could not get current state from relay. This should never happen. " +
							"Was the State not initialized?")

		/**
		 * a function to access signals from the view
		 *
		 * we can not subscribe view signals directly because they have to be unsubscribed/resubscribed
		 * with the activity lifecycle
		 */
		@Synchronized
		protected fun <T> bindSignal(getEvent: Vi.() -> Observable<T>): Observable<T> {
			val relay = PublishRelay.create<T>()
			val binder = ViewEventBinderPair(relay, getEvent)

			viewSignalBinders += binder

			/*  bindSignal may be called after `attachView` (in a lazy delegate maybe) so it would
				never be bound. We can bind it here immediately if the view is already attached
			 */
			if(attachedView != null)
				viewDisposables += binder.bind(attachedView!!)

			return relay
		}

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

		protected abstract fun update(): Observable<ProgramUpdate<St,Vi>>

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
		@Synchronized
		open fun attachView(view: Vi) {
			if(isViewAttached())
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
            viewDisposables += stateUpdates.subscribe(view::render)
            viewDisposables += viewActionRelay.subscribe { f -> view.f() }

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

				// no direct references to the view in this persistent stream,
				// so we don't get activity memory leaks
				// instead everyone subscribes to the respective relay
				combinedStreamDisposable = update()
						.subscribeOn(Schedulers.computation())
						.observeOn(uiScheduler) // VERY IMPORTANT! stateRelay.accept() needs to be called on UI thread always!
						.subscribe { (stateUpdate, viewAction) ->
							if(stateUpdate != null)
								stateRelay.accept(latestState.apply { stateUpdate() })

							if(viewAction != null)
								viewActionRelay.accept(viewAction)
						}

				attachedForFirstTime = false
			}

			// subscribe to the view's events via a relay so we can unsubscribe
			// from those events separately
			// each relay is bound to the corresponding view observable and later unbound when
			// the view detaches again
			viewSignalBinders.filter { !it.isBound }.forEach {
				viewDisposables += it.bind(view)
			}

			attachedView = view
		}

		/**
		 * called by view when stopped to disconnect from presenter streams to avoid memory leaks
		 *
		 * @param isFinishing whether or not the activity/fragment is finishing, i.e the same view
		 * will not attach again (presenters should never be reused though)
		 */
		@CallSuper
		@MainThread
		@Synchronized
		open fun detachView(isFinishing: Boolean) {
			if(attachedView == null)
				throw IllegalStateException("view is already detached or was never attached")

			viewDisposables.onEach(Disposable::dispose).clear()

			attachedView = null

			if(isFinishing) {
				Timber.d("presenter is finishing")
				combinedStreamDisposable?.dispose()
				attachedForFirstTime = false
			}
		}

		private fun getCurrentState()
				= stateRelay.value
				?: throw IllegalStateException("could not get current state from relay. " +
													   "This should never happen. " +
													   "Was the State not initialized?")
	}

	@MainThread
	interface View<in St : State> {

		/**
		 * updates the view to display the current state.
		 */
		fun render(state: St)
	}
}

private data class ViewEventBinderPair<T, in V>(
		private val relay: Relay<T>,
		private val getObservable: V.() -> Observable<T>) {

	var isBound = false
		private set

	fun bind(view: V): Disposable {
		val disposable = view.getObservable().subscribe(relay)!!

		isBound = true

		return DisposableWithListener(disposable, { isBound = false })
	}
}


