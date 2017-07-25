package com.tschuchort.readerforcommitstrip

import android.app.Application
import android.content.res.Resources
import dagger.Component
import io.reactivex.Scheduler
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class, NetworkModule::class))
interface AppComponent {

	@AppContext fun exposeAppContext(): Application
	fun exposeResources(): Resources

	fun exposeWebService(): CommitStripWebService

	fun exposeComicRepo(): ComicRepository

	@UiScheduler fun exposeUiScheduler(): Scheduler
	@ComputationScheduler fun exposeCompScheduler(): Scheduler
	@IoScheduler fun exposeIoScheduler(): Scheduler

	fun exposeLogger(): Logger

	fun inject(app: App)
}