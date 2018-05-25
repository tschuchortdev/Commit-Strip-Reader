package com.tschuchort.readerforcommitstrip.feed

import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.feed.FeedContract.*
import com.tschuchort.readerforcommitstrip.feed.FeedContract.Orientation.VERTICAL
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import javax.inject.Inject

private typealias ProgramUpdate = Contract.ProgramUpdate<State, View>
private typealias StateUpdate = Contract.StateUpdate<State, View>
private typealias ViewAction = Contract.ViewAction<State, View>

@PerActivity
class FeedPresenter
		@Inject constructor(
				private val comicRepo: ComicRepository,
				private val systemManager: SystemManager,
				navigator: Navigator,
				private val storage: LocalStorage,
				@UiScheduler uiScheduler: Scheduler)
		: Presenter(uiScheduler) {

	override val initialState = State(
			comics = emptyList(),
			feedOrientation = VERTICAL,
			internetConnected = true,
			selectedComic = null,
			loading = false,
			refreshing = false
	)

	init {
		bindSignal(View::settingsClicked).subscribe { navigator.showSettings() }

		bindSignal(View::comicClicked).subscribe { comic -> navigator.showZoomedScreen(comic) }
	}


	override fun restoreState(savedState: State) = savedState.copy(
			loading = false,
			refreshing = false
	)

	override fun update() = Observable.mergeArray<ProgramUpdate>(
			handleInternetConnectivity(systemManager, State::internetConnected::set),
			handleRefreshing(loadNewItems = ::loadNewComis,
							 refreshSignal = bindSignal(View::refresh),
							 getList = State::comics::get,
							 setList = State::comics::set,
							 setRefreshing = State::refreshing::set,
							 showRefreshFailed = View::showRefreshFailed,
							 scrollToTop = View::scrollToTop),
			handlePagination(loadNextPage = ::loadNextPage,
							 endReachedSignal = bindSignal(View::endReached),
							 getCurrentState = ::latestState,
							 getList = State::comics::get,
							 setList = State::comics::set,
							 setLoading = State::loading::set,
							 showPagingFailed = View::showNoMoreComics),
			handleListOrientationChange(bindSignal(View::changeFeedLayoutClicked), ::latestState),
			handleSaveComic(storage = storage,
							comicRepo = comicRepo,
							saveSignal = bindSignal(View::saveClicked),
							getComic = { latestState.selectedComic!! },
							showSuccess = View::showSaveSuccesful,
							showFail = View::showSaveFailed),
			handleShareComic(comicRepo = comicRepo,
							 shareSignal = bindSignal(View::shareClicked),
							 getComic = { latestState.selectedComic!! },
							 share = View::share,
							 showDownloadFailed = View::showDownloadFailed),
			handleShareSaveDialog(comicLongClickSignal = bindSignal(View::comicLongClicked),
								  dialogCancelSignal = bindSignal(View::dialogCanceled),
								  dialogOptionSelectedSignal = Observable.merge(
										  bindSignal(View::shareClicked),
										  bindSignal(View::saveClicked)),
								  setSelectedComic = State::selectedComic::set)
	)!!

	fun loadNextPage(): Single<List<Comic>> {
		val lastComic = latestState.comics.lastOrNull()

		return if (lastComic != null)
				comicRepo.getComicsAfter(lastComic, latestState.comics.lastIndex)
			else
				comicRepo.getNewestComics()
	}

	fun loadNewComis(): Single<List<Comic>> {
		val newestComic = latestState.comics.firstOrNull()

		return if (newestComic != null)
				comicRepo.getComicsBefore(newestComic)
			else
				comicRepo.getNewestComics()
	}
}

private inline fun handleListOrientationChange(changeOrientationSignal: Observable<*>,
											   crossinline getState: () -> State)
		= changeOrientationSignal.map(StateUpdate {
		feedOrientation =
				if (getState().feedOrientation == VERTICAL)
					Orientation.HORIZONTAL
				else
					Orientation.VERTICAL
		Unit
	})