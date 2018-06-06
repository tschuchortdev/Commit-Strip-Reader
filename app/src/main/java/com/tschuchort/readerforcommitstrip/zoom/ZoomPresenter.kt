package com.tschuchort.readerforcommitstrip.zoom

import com.google.firebase.analytics.FirebaseAnalytics
import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.*
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.ofType
import javax.inject.Inject

@PerActivity
class ZoomPresenter
	@Inject constructor(val comic: Comic,
						@UiScheduler uiScheduler: Scheduler,
						private val navigator: Navigator,
						private val analytics: FirebaseAnalytics,
						private val comicRepo: ComicRepository,
						private val storage: LocalStorage)
		: ZoomContract.Presenter(uiScheduler) {

	override val initialState = State(comic)

	init {
		sideEffects.ofType<SideEffect.NavigateUp>()
				.subscribe { navigator.navigateUp() }
	}

	override val events = Observable.mergeArray(
			sideEffects.ofType<SideEffect.SaveComic>()
					.flatMapSingle { (comic) ->
						comicRepo.loadBitmap(comic.imageUrl)
								.flatMapCompletable { bmp ->
									storage.saveImageToGallery(bmp, comic.title, "Commit Strips")
								}
								.onCompleteReturn<Event>(Event.SaveSuccessful)
					}
					.onErrorReturn(Event::SaveFailed)
	)!!

	override fun logEvent(event: Event) {
		super.logEvent(event)
		analytics.logEvent(event.javaClass.simpleName, null)
	}

	override fun reduce(oldState: State, event: Event) = when(event) {
		is Event.ShareClicked -> StateUpdate(oldState, ViewEffect.ShareComic(oldState.comic))
		is Event.SaveClicked -> StateUpdate(oldState, SideEffect.SaveComic(oldState.comic))
		is Event.UpClicked -> StateUpdate(oldState, SideEffect.NavigateUp)
		is Event.SaveSuccessful -> StateUpdate(oldState, ViewEffect.ShowSaveSuccesful)
		is Event.SaveFailed -> StateUpdate(oldState, ViewEffect.ShowSaveFailed(event.t))
	}
}