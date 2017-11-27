package com.tschuchort.readerforcommitstrip.feed

import com.google.firebase.analytics.FirebaseAnalytics
import com.tschuchort.readerforcommitstrip.*
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
						 navigator: Navigator,
						 analytics: FirebaseAnalytics): FeedContract.Presenter
			= FeedPresenter(repository, uiScheduler, systemManager, navigator, analytics)
}