package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.*
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler

@Module
class ZoomModule(val selectedComic: Comic) {

	@Provides
	@PerActivity
	fun providePresenter(@UiScheduler uiScheduler: Scheduler,
						 @ComputationScheduler compScheduler: Scheduler,
						 logger: Logger): ZoomContract.Presenter
			= ZoomPresenter(selectedComic, uiScheduler, compScheduler, logger)
}