package com.tschuchort.readerforcommitstrip

import android.app.Application
import android.app.NotificationManager
import android.content.res.Resources
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.google.firebase.analytics.FirebaseAnalytics
import com.tschuchort.readerforcommitstrip.feed.FeedComponent
import com.tschuchort.readerforcommitstrip.zoom.ZoomComponent
import com.tschuchort.readerforcommitstrip.zoom.ZoomModule
import dagger.Component
import io.reactivex.Scheduler
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, NetworkModule::class])
interface AppComponent {

	@AppContext fun exposeAppContext(): Application
	fun exposeResources(): Resources

	fun exposeComicRepo(): ComicRepository

	fun exposeFirebaseJobDispatcher(): FirebaseJobDispatcher

	fun exposeNotificationManager(): NotificationManager

	@UiScheduler fun exposeUiScheduler(): Scheduler
	@ComputationScheduler fun exposeCompScheduler(): Scheduler
	@IoScheduler fun exposeIoScheduler(): Scheduler

	fun exposePreferences(): SettingsRepository

	fun exposeSystemManager(): SystemManager

	fun exposeFirebaseAnalytics(): FirebaseAnalytics

	fun newFeedComponent(activityModule: ActivityModule): FeedComponent
	fun newZoomComponent(activityModule: ActivityModule, zoomModule: ZoomModule): ZoomComponent

	fun inject(app: App)
	fun inject(notifService: DownloadLatestComicService)
}