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
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Qualifier
import javax.inject.Singleton


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
	@Singleton
	fun provideFirebaseJobDispatcher(@AppContext ctx: Context) = FirebaseJobDispatcher(GooglePlayDriver(ctx))

	@Provides
	@Singleton
	fun provideNotificationManager(@AppContext ctx: Context) = ctx.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

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
	fun provideSharedPreferences(@AppContext ctx: Context) = PreferenceManager.getDefaultSharedPreferences(ctx)!!

	@Provides
	@Singleton
	fun provideRxSharedPreferences(prefs: SharedPreferences) = RxSharedPreferences.create(prefs)

	@Provides
	@Singleton
	fun provideSettings(@AppContext ctx: Context, rxPrefs: RxSharedPreferences, res: Resources): SettingsRepository
			= SettingsRepositoryImpl(ctx, rxPrefs, res)

	@Provides
	@Singleton
	fun provideSystemManager(@AppContext ctx: Context): SystemManager = object : SystemManager {
		override fun observeInternetConnectivity()
				= ReactiveNetwork.observeNetworkConnectivity(ctx).map { it.isAvailable }

		override val isInternetConnected
				= ReactiveNetwork.checkInternetConnectivity()
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







