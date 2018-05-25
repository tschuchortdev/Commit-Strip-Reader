package com.tschuchort.readerforcommitstrip

import android.graphics.Bitmap
import com.tschuchort.readerforcommitstrip.Contract.*
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

@Suppress("UNCHECKED_CAST")
fun <S : Contract.State, V : Contract.View<S>>
        logViewEvents(log: (String) -> Any?,
                      bindViewSignal: (V.() -> Observable<Any>) -> Observable<Any>,
                      vararg signals: V.() -> Observable<*>)
        = Observable.merge<Any>(
        (signals as Array<Contract.View<S>.() -> Observable<Any>>).map(bindViewSignal)
).subscribe { ev -> log(ev.javaClass.simpleName + ev.toString()) }


fun <S : Contract.State, V : Contract.View<S>>
        handleInternetConnectivity(systemManager: SystemManager,
                                   setInternetConnected: S.(Boolean) -> Any?)
        = systemManager.observeInternetConnectivity()
        .distinctUntilChanged()
        .map { status -> StateUpdate<S,V> { setInternetConnected(status) } }


fun <S : Contract.State, V : Contract.View<S>, T>
        handleRefreshing(loadNewItems: () -> Single<List<T>>,
                         refreshSignal: Observable<*>,
                         getList: S.() -> List<T>,
                         setList: S.(List<T>) -> Any?,
                         setRefreshing: S.(Boolean) -> Any?,
                         showRefreshFailed: V.() -> Any?,
                         scrollToTop: V.() -> Any?) = refreshSignal.switchMap {
    Observable.just(StateUpdate<S, V> { setRefreshing(true) })
            .then(loadNewItems().retryDelayed(delay = 1000, times = 5)
                          .map { newItems ->
                              ProgramUpdate<S, V>({ setList(newItems + getList()) }, scrollToTop)
                          }
                          .onErrorReturn(ViewAction<S, V>(showRefreshFailed))
            )
            .then(StateUpdate<S, V> { setRefreshing(false) })
}


fun <S : Contract.State, V : Contract.View<S>, T>
        handlePagination(loadNextPage: () -> Single<List<T>>,
                         endReachedSignal: Observable<*>,
                         getCurrentState: () -> S,
                         getList: S.() -> List<T>,
                         setList: S.(List<T>) -> Any?,
                         setLoading: S.(Boolean) -> Any?,
                         showPagingFailed: V.() -> Any?): Observable<ProgramUpdate<S,V>> {

    fun handleLoading() = Observable.just(StateUpdate<S, V> { setLoading(true) })
            .then(loadNextPage().retryDelayed(delay = 1000, times = 5)
                          .map { nextPageItems ->
                              StateUpdate<S, V> { setList(getList() + nextPageItems) }
                          }
                          .onErrorReturn(ViewAction<S, V>(showPagingFailed))
            )
            .then(StateUpdate<S, V> { setLoading(false) })

    return Observable.merge(
            endReachedSignal.dropMap { handleLoading() },
            if (getCurrentState().getList().isEmpty())
                handleLoading()
            else
                Observable.never()
    )
}


fun <S : Contract.State, V : Contract.View<S>>
        handleSaveComic(storage: LocalStorage,
                        comicRepo: ComicRepository,
                        saveSignal: Observable<*>,
                        getComic: () -> Comic,
                        showSuccess: V.() -> Any?,
                        showFail: V.() -> Any?) = saveSignal.flatMapSingle {
    val comic = getComic()

    comicRepo.loadBitmap(comic.imageUrl)
            .flatMapCompletable { bmp ->
                storage.saveImageToGallery(bmp, comic.title, "Commit Strips")
            }
            .onCompleteReturn(ViewAction<S,V>(showSuccess))
            .onErrorReturn {
                Timber.e(it)
                ViewAction<S,V>(showFail)
            }
}


fun <S : Contract.State, V : Contract.View<S>>
        handleShareComic(comicRepo: ComicRepository,
                         shareSignal: Observable<*>,
                         getComic: () -> Comic,
                         share: V.(Bitmap, String) -> Any?,
                         showDownloadFailed: V.() -> Any?) = shareSignal.flatMapSingle {
    val selectedComic = getComic()

    comicRepo.loadBitmap(selectedComic.imageUrl)
            .map { bmp ->
                ViewAction<S,V>{ share(bmp, selectedComic.title) }
            }
            .onErrorReturn {
                Timber.e(it)
                ViewAction(showDownloadFailed)
            }
}


fun <S : Contract.State, V : Contract.View<S>>
        handleShareSaveDialog(comicLongClickSignal: Observable<Comic>,
                              dialogCancelSignal: Observable<*>,
                              dialogOptionSelectedSignal: Observable<*>,
                              setSelectedComic: S.(Comic?) -> Any?) = Observable.merge(
        Observable.merge(dialogCancelSignal, dialogOptionSelectedSignal)
                .map(StateUpdate {
                    setSelectedComic(null)
                }),
        comicLongClickSignal.map { clickedComic ->
            StateUpdate<S, V> { setSelectedComic(clickedComic) }
        }
)