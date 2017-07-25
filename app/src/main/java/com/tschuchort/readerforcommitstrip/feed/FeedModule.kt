package com.tschuchort.readerforcommitstrip.feed

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
						 @ComputationScheduler compScheduler: Scheduler,
						 @IoScheduler ioScheduler: Scheduler,
						 logger: Logger): FeedContract.Presenter
			= FeedPresenter(repository, uiScheduler, compScheduler, ioScheduler, logger)
}