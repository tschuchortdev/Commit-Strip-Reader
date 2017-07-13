package com.tschuchort.readerforcommitstrip.feed

import com.tschuchort.readerforcommitstrip.feed.FeedContract.*
import javax.inject.Inject

class FeedPresenter @Inject constructor(val comicRepository: ) : Presenter() {
	override val initialState = State()

	fun updateState(state: State, event: Event, view: View) = when(event) {
		is Event.OrientationChanged -> state.copy(
				feedOrientation = if(state.feedOrientation == Orientation.VERTICAL)
					Orientation.HORIZONTAL
				else
					Orientation.VERTICAL)

		is Event.SettingsClicked -> {
			view.showSettings()
			state
		}
		is Event.ComicClicked -> {
			view.showDetails(event.comic)
			state
		}
		is Event.ComicLongClicked ->

		is Event.ComicDoubleClicked -> {
			view.showEnlarged(event.comic)
			state
		}



	}

	fun log(state: State, event: Event) {
		// TODO
	}
}

