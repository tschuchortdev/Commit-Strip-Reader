package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.AllOpen
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.PerActivity
import dagger.Module
import dagger.Provides

@AllOpen
@Module
class ZoomModule(val selectedComic: Comic) {

	@Provides
	@PerActivity
	fun provideSelectedComic() = selectedComic

	@Provides
	@PerActivity
	fun providePresenter(presenter: ZoomPresenter): ZoomContract.Presenter = presenter
}