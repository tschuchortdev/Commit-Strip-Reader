package com.tschuchort.readerforcommitstrip

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable

interface ComicRepository {
	fun getComicsAfter(lastComic: Comic, lastIndex: Int): Single<List<Comic>>

	fun getComicsBefore(firstComic: Comic): Single<List<Comic>>

	fun getNewestComics(): Single<List<Comic>>

	fun getNewestComic(): Single<Comic>

	fun subscribeLatestComics(callback: (Comic) -> Completable): Disposable
}