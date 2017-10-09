package com.tschuchort.readerforcommitstrip

import android.app.Application
import android.app.NotificationManager
import android.content.res.Resources
import com.firebase.jobdispatcher.FirebaseJobDispatcher
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

	fun exposeFirebaseJobDispatcher(): FirebaseJobDispatcher

	fun exposeNotificationManager(): NotificationManager

	@UiScheduler fun exposeUiScheduler(): Scheduler
	@ComputationScheduler fun exposeCompScheduler(): Scheduler
	@IoScheduler fun exposeIoScheduler(): Scheduler

	fun exposePreferences(): SettingsRepository

	fun exposeLogger(): Logger

	fun exposeSystemManager(): SystemManager

	fun inject(app: App)
	fun inject(notifService: DownloadLatestComicService)
}