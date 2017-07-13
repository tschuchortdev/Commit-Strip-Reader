package com.tschuchort.readerforcommitstrip

import android.app.Application
import android.content.res.Resources
import com.squareup.leakcanary.RefWatcher
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * provides app wide instances
 */
@Module
class AppModule(val app: Application) {

	@Provides
	@Singleton
	private fun provideAppContext(): Application = app

	@Provides
	@Singleton
	private fun provideResources(): Resources = app.resources
}