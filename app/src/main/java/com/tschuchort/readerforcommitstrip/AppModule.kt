package com.tschuchort.readerforcommitstrip

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.Log
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Singleton
import javax.inject.Qualifier


/**
 * provides app wide dependencies
 */
@Module
open class AppModule(val app: Application) {

	@Provides
	@Singleton
	@AppContext
	fun provideAppContext(): Context = app

	@Provides
	@Singleton
	@AppContext
	fun provideApp(): Application = app

	@Provides
	@Singleton
	fun provideResources(): Resources = app.resources

	@Provides
	@UiScheduler
	fun provideUiScheduler(): Scheduler = AndroidSchedulers.mainThread()

	@Provides
	@ComputationScheduler
	fun provideCompScheduler(): Scheduler = Schedulers.computation()

	@Provides
	@IoScheduler
	fun provideIoScheduler(): Scheduler = Schedulers.io()

	@Provides
	@Singleton
	fun provideLogger(): Logger = object : Logger {
		override fun v(tag: String, msg: String?) { Log.v(tag, msg ?: "") }
		override fun d(tag: String, msg: String?) { Log.d(tag, msg ?: "") }
		override fun i(tag: String, msg: String?) { Log.i(tag, msg ?: "") }
		override fun w(tag: String, msg: String?) { Log.v(tag, msg ?: "") }
		override fun e(tag: String, msg: String?) { Log.e(tag, msg ?: "") }
	}
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AppContext

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UiScheduler

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ComputationScheduler

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoScheduler







