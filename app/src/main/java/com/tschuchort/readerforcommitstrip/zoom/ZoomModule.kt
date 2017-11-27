package com.tschuchort.readerforcommitstrip.zoom

import com.google.firebase.analytics.FirebaseAnalytics
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.Navigator
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
						 navigator: Navigator,
						 analytics: FirebaseAnalytics)
			: ZoomContract.Presenter
			= ZoomPresenter(selectedComic, uiScheduler, navigator, analytics)
}