package com.tschuchort.readerforcommitstrip

import android.content.SharedPreferences
import android.content.res.Resources
import android.webkit.URLUtil.isValidUrl
import com.firebase.jobdispatcher.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicRepositoryImpl
		@Inject constructor(private val prefs: SharedPreferences,
							private val res: Resources,
							jobDispatcher: FirebaseJobDispatcher,
							private val webService: CommitStripWebService)
	: ComicRepository {

	private var latestComicDateCache: String?
		get() = prefs.getString(res.getString(R.string.pref_key_latest_comic_date), null)
		set(value) {
			prefs.edit().putString(res.getString(R.string.pref_key_latest_comic_date), value).apply()
		}

	private val latestComicDownloadJob = jobDispatcher.newJobBuilder()
				.setService(DownloadLatestComicService::class.java)
				.setTag("latest_comic_download_service")
				.setLifetime(Lifetime.FOREVER)
				.setConstraints(Constraint.ON_ANY_NETWORK)
				.setReplaceCurrent(true)
				.setRecurring(true)
				// let the job run every 6-9h in a time window of 3h
				.setTrigger(Trigger.executionWindow(hoursToSec(6f), hoursToSec(9f)))
				.build()

	private val latestComicCallbacks = ArrayList<(Comic) -> Completable>()


	init {
		jobDispatcher.mustSchedule(latestComicDownloadJob)
	}

	override fun subscribeLatestComics(callback: (Comic) -> Completable): Disposable {
		if(latestComicCallbacks.contains(callback)) {
			throw IllegalArgumentException("callback already registered")
		}

		latestComicCallbacks += callback

		return object : Disposable {
			var disposed = false

			override fun dispose() {
				latestComicCallbacks.remove(callback)
				disposed = true
			}

			override fun isDisposed() = disposed
		}
	}

	fun onServiceDownloadedLatestComic(latestComic: Comic): Completable {
		if(latestComicDateCache != latestComic.date && latestComicDateCache != null) {
			latestComicDateCache = latestComic.date

			val completables = latestComicCallbacks.fold(ArrayList<Completable>()) { acc, it ->
				 acc += it(latestComic)
				 return@fold acc
			}

			return Completable.merge(completables)
		}
		else {
			latestComicDateCache = latestComic.date

		    return Completable.complete()
		}
	}

	override fun getNewestComic() = getNewestComics().map(List<Comic>::first)!!

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
				//.onErrorReturn(emptyList())
				// append another page to return a list large enough
				.zipWith(getComicsForPage(currentPage + 1)
						.onErrorReturn(emptyList()))
				{ firstPage, secondPage ->
					firstPage + secondPage
				}
				.onErrorReturn { error ->
					if(error is HttpException && error.code() == 404)
						emptyList() // 404 means we are at the end of the list
					else
						throw error
				}
	}

	override fun getComicsBefore(firstComic: Comic): Single<List<Comic>> =
			Observable.range(0, Int.MAX_VALUE)
					.concatMap { getComicsForPage(it).toObservable() }
					// retry until we have found the page that contained the last downloaded comic
					.takeUntil { comics -> comics.contains(firstComic) }
					.reduce { accumulatedList, newList -> accumulatedList + newList }
					.toSingle()
					.map { comics -> comics.takeWhile { it != firstComic }}

	private fun getComicsForPage(page: Int)
			= webService.getRssFeed(page)
			.subscribeOn(Schedulers.io())
			.map(::createComicsFromRss)
}

class DownloadLatestComicService : JobService() {
	@Inject
	protected lateinit var comicRepo: ComicRepository

	private var networkRequest: Disposable? = null

	override fun onCreate() {
		super.onCreate()
		(application as App).component.inject(this)
	}

	override fun onStartJob(jobParams: JobParameters): Boolean {
		networkRequest = comicRepo.getNewestComic()
				.flatMapCompletable { comic ->
					(comicRepo as ComicRepositoryImpl).onServiceDownloadedLatestComic(comic)
				}
				.subscribe({
					jobFinished(jobParams, false)
				}, { error ->
					Timber.e(error, "failed to download latest comic in background service")
					jobFinished(jobParams, true)
				})

		// job hasn't finished yet. Work is still going on in the background
		return true
	}

	override fun onStopJob(params: JobParameters?): Boolean {
		networkRequest?.dispose()
		// job was cancelled before jobFinished could be called.
		// The job should be retried if possible
		return true
	}
}

private const val PAGE_SIZE = 10

private fun getPageForIndex(index: Int) = (index / PAGE_SIZE) + 1 // pages are 1-indexed

private fun createComicsFromRss(rss: Rss): List<Comic> =
		rss.channel.itemList.map {
			val imageUrl = Regex("https?://www\\.commitstrip\\.com/wp-content/uploads/\\d+/\\d+/.*?\\.(jpe?g|png|gif)")
								   .findAll(it.content!!).map(MatchResult::value)
								   .firstOrNull(::isValidUrl) // if there actually happen to be multiple images, take the first one
						   ?: throw RuntimeException("could not parse content url from RSS: " + it.content!!)

			Comic(it.title!!, it.pubDate!!, it.description, it.link, imageUrl, it.categories ?: emptyList())
		}

private fun hoursToSec(hours: Float) = Math.round(hours * 60 * 60)

