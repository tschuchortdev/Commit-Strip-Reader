package com.tschuchort.readerforcommitstrip.feed

import com.google.firebase.analytics.FirebaseAnalytics
import com.tschuchort.readerforcommitstrip.ComicRepository
import com.tschuchort.readerforcommitstrip.PerActivity
import com.tschuchort.readerforcommitstrip.SystemManager
import com.tschuchort.readerforcommitstrip.UiScheduler
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler

@Module
class FeedModule {

	@Provides
	@PerActivity
	fun providePresenter(repository: ComicRepository,
						 @UiScheduler uiScheduler: Scheduler,
						 systemManager: SystemManager,
						 analytics: FirebaseAnalytics): FeedContract.Presenter
			= FeedPresenter(repository, uiScheduler, systemManager, analytics)
}