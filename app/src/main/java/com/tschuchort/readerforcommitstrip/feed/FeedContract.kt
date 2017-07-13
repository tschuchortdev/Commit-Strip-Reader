package com.tschuchort.readerforcommitstrip.feed


import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Contract


interface FeedContract : Contract {
	enum class Orientation { VERTICAL, HORIZONTAL }

	sealed class State(
			val comics: List<Comic> = emptyList(),
			val feedOrientation: Orientation = Orientation.VERTICAL,
			val isLoading: Boolean = false,
			val noInternet: Boolean = false
	) : Contract.State {

		data class ShareDialog(val comic: Comic): State()
	}

	sealed class Event : Contract.Event {
		class EndReached : Event()
		class SettingsClicked : Event()
		class PulledToRefresh : Event()
		class OrientationChanged : Event()
		data class ComicClicked(val comic: Comic): Event()
		data class ComicLongClicked(val comic: Comic): Event()
		data class ComicDoubleClicked(val comic: Comic): Event()
	}

	abstract class Presenter : Contract.Presenter<State>()

	interface View : Contract.View<State> {
		fun showSettings()
		fun showDetails(comic: Comic)
		fun showEnlarged(comic: Comic)
	}
}
