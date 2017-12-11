package com.tschuchort.readerforcommitstrip.feed

import com.tschuchort.readerforcommitstrip.AllOpen
import com.tschuchort.readerforcommitstrip.PerActivity
import dagger.Module
import dagger.Provides

@AllOpen
@Module
class FeedModule {

	@Provides
	@PerActivity
	fun providePresenter(presenter: FeedPresenter): FeedContract.Presenter = presenter
}