package com.tschuchort.readerforcommitstrip

import android.os.Bundle
import android.support.annotation.CallSuper
import io.mironov.smuggler.AutoParcelable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.io.Serializable

interface Contract {
	interface State : AutoParcelable
	interface Event : Serializable

	abstract class Presenter<S : State> {
		private val BUNDLE_KEY = "presenter last state"
		abstract val stateChanges: BehaviorSubject<S>
		val eventDisposables = ArrayList<Disposable>()

		protected abstract val initialState: S

		fun onSaveInstanceState(outState: Bundle?) {
			outState!!.putParcelable(BUNDLE_KEY, stateChanges.blockingLast(initialState))
		}

		fun onRestoreInstanceState(savedInstanceState: Bundle?) {
			val lastState = savedInstanceState?.getParcelable<S>(BUNDLE_KEY)
		}

		@CallSuper
		open fun attechView(view: View<S>) {
			eventDisposables += view.uiEvents
					.scan(initialState, this::reduce)
					.subscribeWith(stateChanges)
					.distinctUntilChanged()
					.subscribe { newState: S -> view.render(newState) }
		}

		@CallSuper
		open fun detachView() {
			eventDisposables.forEach(Disposable::dispose)
			eventDisposables.clear()
		}
	}

	interface View<in S : State> {
		fun render(state: S)
	}
}