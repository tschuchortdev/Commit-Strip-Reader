package com.tschuchort.readerforcommitstrip

import android.app.Application
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Component
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Qualifier
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

	fun newActivityComponent(activityModule: ActivityModule): ActivityComponent

	fun inject(app: App)
	fun inject(notifService: DownloadLatestComicService)
}

/**
 * provides app wide dependencies
 */
@AllOpen
@Module
class AppModule(val app: Application) {

	@Provides @Singleton
	@AppContext fun provideAppContext(): Context = app

	@Provides @Singleton
	@AppContext fun provideApp(): Application = app

	@Provides @Singleton
	fun provideResources(): Resources = app.resources

	@Provides @Singleton
	fun provideFirebaseJobDispatcher(@AppContext ctx: Context)
			= FirebaseJobDispatcher(GooglePlayDriver(ctx))

	@Provides @Singleton
	fun provideNotificationManager(@AppContext ctx: Context)
			= ctx.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

	@Provides
	@UiScheduler fun provideUiScheduler(): Scheduler = AndroidSchedulers.mainThread()

	@Provides
	@ComputationScheduler fun provideCompScheduler(): Scheduler = Schedulers.computation()

	@Provides
	@IoScheduler fun provideIoScheduler(): Scheduler = Schedulers.io()

	@Provides
	@Singleton
	fun provideSharedPreferences(@AppContext ctx: Context)
			= PreferenceManager.getDefaultSharedPreferences(ctx)!!

	@Provides @Singleton
	fun provideRxSharedPreferences(prefs: SharedPreferences) = RxSharedPreferences.create(prefs)

	@Provides @Singleton
	fun provideSettings(settingsRepositoryImpl: SettingsRepositoryImpl): SettingsRepository
			= settingsRepositoryImpl

	@Provides @Singleton
	fun provideSystemManager(@AppContext ctx: Context): SystemManager = object : SystemManager {
		override fun observeInternetConnectivity()
				= ReactiveNetwork.observeNetworkConnectivity(ctx).map { it.isAvailable }

		override val isInternetConnected
				= ReactiveNetwork.checkInternetConnectivity()
	}

	@Provides @Singleton
	fun provideAnalytics(@AppContext ctx: Context) = FirebaseAnalytics.getInstance(ctx)!!
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
