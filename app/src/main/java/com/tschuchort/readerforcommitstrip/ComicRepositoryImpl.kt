package com.tschuchort.readerforcommitstrip

import android.webkit.URLUtil
import android.webkit.URLUtil.isValidUrl
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicRepositoryImpl
		@Inject constructor(private val webService: CommitStripWebService)
	: ComicRepository {

	override fun getNewestComics(): Single<List<Comic>> = getComicsForPage(getPageForIndex(0))

	override fun getComicsAfter(lastComic: Comic, lastIndex: Int): Single<List<Comic>> {
		var currentPage = getPageForIndex(lastIndex)

		return getComicsForPage(currentPage)
				.toObservable()
				.doOnNext { currentPage++ }
				// retry until we have found the page that contained the last downloaded comic
				.takeUntil { comics -> comics.contains(lastComic) }
				.lastOrError()
				// drop elements that are before the last comic in the list we already have
				// so we don't have duplicates in the end. This should be much more
				// performant than distinct()
				.map { comics -> comics.dropUntilAfter { it == lastComic } }
				// append another page to return a list large enough
				.zipWith(getComicsForPage(currentPage + 1)
						.onErrorReturn(emptyList()))
				{ firstPage, secondPage ->
					firstPage + secondPage
				}
	}

	override fun getComicsBefore(firstComic: Comic): Single<List<Comic>> =
			Observable.range(0, Int.MAX_VALUE)
					.flatMap { getComicsForPage(it).toObservable() }
					.scan { accumulatedList, newList -> accumulatedList + newList }
					// retry until we have found the page that contained the last downloaded comic
					.takeUntil { comics -> comics.contains(firstComic) }
					.lastOrError()
					.map { comics -> comics.takeWhile { it != firstComic }}


	private fun getComicsForPage(page: Int)
			= webService.getRssFeed(page)
			.map(::createComicsFromRss)
}

private const val page_size = 10

private fun getPageForIndex(index: Int) = (index / page_size) + 1 // pages are 1-indexed

private fun createComicsFromRss(rss: Rss): List<Comic> =
		rss.channel.itemList.map {
			val imageUrl = Regex("https?://www\\.commitstrip\\.com/wp-content/uploads/\\d+/\\d+/.*?\\.(jpe?g|png|gif)")
								   .findAll(it.content!!).map { it.value }
								   .firstOrNull { isValidUrl(it) } // if there actually happen to be multiple images, take the first one
						   ?: throw RuntimeException("could not parse content url from RSS: " + it.content!!)

			Comic(it.title!!, it.pubDate!!, it.description, it.link, imageUrl, it.categories ?: emptyList())
		}

