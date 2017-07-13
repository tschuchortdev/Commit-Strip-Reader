package com.tschuchort.readerforcommitstrip

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface CommitStripWebService {
	@GET("en/feed")
	fun getRssFeed(@Query("paged") page: Int): Single<Rss>
}