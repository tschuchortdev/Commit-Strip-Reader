package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.ComputationScheduler
import com.tschuchort.readerforcommitstrip.PerActivity
import com.tschuchort.readerforcommitstrip.UiScheduler
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler

@Module
class ZoomModule(val selectedComic: Comic) {

	@Provides
	@PerActivity
	fun providePresenter(@UiScheduler uiScheduler: Scheduler,
						 @ComputationScheduler compScheduler: Scheduler)
			: ZoomContract.Presenter
			= ZoomPresenter(selectedComic, uiScheduler, compScheduler)
}