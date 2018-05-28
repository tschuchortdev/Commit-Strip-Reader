package com.tschuchort.readerforcommitstrip

import android.graphics.Bitmap
import com.tschuchort.readerforcommitstrip.Contract.*
import io.reactivex.Observable
import io.reactivex.Observable.*
import io.reactivex.Single
import timber.log.Timber


fun <S : State, V : View<S>> handleInternetConnectivity(systemManager: SystemManager,
                                                        setInternetConnected: S.(Boolean) -> Any?)
        = systemManager.observeInternetConnectivity()
        .distinctUntilChanged()
        .map { status ->
            StateChange<S, V> {
                setInternetConnected(status)
            }
        }

fun <S: State, V : View<S>, T> handlePagingFeed(loadNewItems: () -> Single<List<T>>,
                                                loadNextPage: () -> Single<List<T>>,
                                                refreshSignal: Observable<*>,
                                                endReachedSignal: Observable<*>,
                                                getCurrentState: () -> S,
                                                getList: S.() -> List<T>,
                                                setList: S.(List<T>) -> Any?,
                                                setLoading: S.(Boolean) -> Any?,
                                                setRefreshing: S.(Boolean) -> Any?,
                                                showRefreshFailed: V.() -> Any?,
                                                showPagingFailed: V.() -> Any?,
                                                scrollToTop: V.() -> Any?)
    = merge(handleRefreshing(loadNewItems, refreshSignal, getList, setList, setRefreshing,
                             showRefreshFailed, scrollToTop),
            handlePagination(loadNextPage,endReachedSignal, getCurrentState, getList, setList,
                             setLoading, showPagingFailed)
)

fun <S : State, V : View<S>, T> handleRefreshing(loadNewItems: () -> Single<List<T>>,
                                                 refreshSignal: Observable<*>,
                                                 getList: S.() -> List<T>,
                                                 setList: S.(List<T>) -> Any?,
                                                 setRefreshing: S.(Boolean) -> Any?,
                                                 showRefreshFailed: V.() -> Any?,
                                                 scrollToTop: V.() -> Any?)
        = refreshSignal.switchMap {
    just(StateChange<S, V> { setRefreshing(true) })
            .then(loadNewItems().retryDelayed(delay = 1000, times = 5)
                          .map { newItems ->
                              ProgramUpdate({ setList(newItems + getList()) }, scrollToTop)
                          }
                          .onErrorReturn {
                              Timber.e(it)
                              ViewAction(showRefreshFailed)
                          }
            )
            .then(StateChange<S, V> { setRefreshing(false) })
}


fun <S : State, V : View<S>, T> handlePagination(loadNextPage: () -> Single<List<T>>,
                                                 endReachedSignal: Observable<*>,
                                                 getCurrentState: () -> S,
                                                 getList: S.() -> List<T>,
                                                 setList: S.(List<T>) -> Any?,
                                                 setLoading: S.(Boolean) -> Any?,
                                                 showPagingFailed: V.() -> Any?)
        : Observable<ProgramUpdate<S, V>> {

    fun load() = just(StateChange<S, V> { setLoading(true) })
            .then(loadNextPage().retryDelayed(delay = 1000, times = 5)
                          .map<ProgramUpdate<S,V>> { nextPageItems ->
                              StateChange {
                                  setList(getList() + nextPageItems)
                              }
                          }
                          .onErrorReturn {
                              Timber.e(it)
                              ViewAction(showPagingFailed)
                          }
            )
            .then(StateChange<S, V> { setLoading(false) })

    fun initList() =
            if (getCurrentState().getList().isEmpty())
                load()
            else
                never()

    return merge(endReachedSignal.dropMap { load() }, initList())
}


fun <S : State, V : View<S>> handleSaveComic(storage: LocalStorage,
                                             comicRepo: ComicRepository,
                                             saveSignal: Observable<*>,
                                             getComic: () -> Comic,
                                             showSuccess: V.() -> Any?,
                                             showFail: V.() -> Any?)
        = saveSignal.flatMapSingle {
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


fun <S : State, V : View<S>> handleShareComic(comicRepo: ComicRepository,
                                              shareSignal: Observable<*>,
                                              getComic: () -> Comic,
                                              share: V.(Bitmap, String) -> Any?,
                                              showDownloadFailed: V.() -> Any?)
        = shareSignal.flatMapSingle {
    val selectedComic = getComic()

    comicRepo.loadBitmap(selectedComic.imageUrl)
            .map { bmp ->
                ViewAction<S, V> { share(bmp, selectedComic.title) }
            }
            .onErrorReturn {
                Timber.e(it)
                ViewAction(showDownloadFailed)
            }
}


