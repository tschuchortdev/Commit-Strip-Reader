package com.tschuchort.readerforcommitstrip

import io.reactivex.Single

interface ComicRepository {
	fun getComicsAfter(lastComic: Comic, lastIndex: Int): Single<List<Comic>>

	fun getComicsBefore(firstComic: Comic): Single<List<Comic>>

	fun getNewestComics(): Single<List<Comic>>
}