package com.tschuchort.readerforcommitstrip

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.support.multidex.MultiDexApplication
import android.support.v4.app.NotificationCompat
import com.akaita.java.rxjava2debug.RxJava2Debug
import com.crashlytics.android.Crashlytics
import com.facebook.stetho.Stetho
import com.squareup.leakcanary.LeakCanary
import com.tschuchort.readerforcommitstrip.zoom.ZoomActivity
import io.fabric.sdk.android.Fabric
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class App : MultiDexApplication() {

	@Inject
	protected lateinit var settings: SettingsRepository

	@Inject
	protected lateinit var notificationManager: NotificationManager

	@Inject
	protected lateinit var comicRepo: ComicRepository

	protected var newComicsSubscription: Disposable? = null

	var component by mutableLazy {
		DaggerAppComponent.builder()
				.appModule(AppModule(this))
				.build()!!
	}

	override fun onCreate() {
		super.onCreate()

		if (LeakCanary.isInAnalyzerProcess(this)) {
			// This process is dedicated to LeakCanary for heap analysis.
			// You should not init your app in this process.
			return
		}

		component.inject(this)

		if(BuildConfig.DEBUG) {
			Stetho.initializeWithDefaults(this)
			LeakCanary.install(this)
			Timber.plant(Timber.DebugTree())
		}
		else {
			Fabric.with(this, Crashlytics()) // enable crashlytics
			Timber.plant(CrashlyticsTimberTree()) // integrate Timber logging with crashlytics
		}

		//this has to be done AFTER initializing crash reporting (firebase, crashlytics etc)
		RxJava2Debug.enableRxJava2AssemblyTracking(arrayOf("com.tschuchort.readerforcommitstrip"))

		// start or stop notification service when settings change
		settings.notifyAboutNewComics.observe().subscribe { notifySetting ->
			if (notifySetting)
				newComicsSubscription = comicRepo.subscribeLatestComics(this::sendNewComicNotification)
			else
				newComicsSubscription?.dispose()
		}

		@TargetApi(Build.VERSION_CODES.O)
		// create a NotificationChannel if we are on Oreo
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val notifChannel = NotificationChannel(
					"default_notification_channel",
					getString(R.string.notif_channel_title_new_comics),
					NotificationManager.IMPORTANCE_LOW)

			notifChannel.apply {
				enableLights(true)
				enableVibration(false)
				lockscreenVisibility = Notification.VISIBILITY_PUBLIC
			}

			notificationManager.createNotificationChannel(notifChannel)
		}
	}

	private fun sendNewComicNotification(comic: Comic): Completable =
		comicRepo.loadBitmap(comic.imageUrl)
				.retryDelayed(10, TimeUnit.SECONDS, 6)
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSuccess { bitmap ->

					val showComicIntent = PendingIntent.getActivity(
							this, Random().nextInt(),
							Intent(this, ZoomActivity::class.java).apply {
								putExtra(getString(R.string.extra_selected_comic), comic)
							},
							FLAG_ONE_SHOT, null)

					val notification = NotificationCompat.Builder(this, "default_notification_channel")
							.setContentTitle(getString(R.string.notification_title_new_comic))
							.setContentText(comic.title)
							.setAutoCancel(true)
							.setContentIntent(showComicIntent)
							.setSmallIcon(R.drawable.ic_notif_commitstrip)
							.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round))
							.setStyle(NotificationCompat.BigPictureStyle()
									.bigPicture(bitmap)
									.setSummaryText(comic.title))
							.build()

					// notification ID is always different, so new notifications are send
					// instead of updating the old one. We don't need to hold on to the ID
					// because we never touch the notification again after sending it
					notificationManager.notify(Random().nextInt(), notification)
				}
				.doOnError { error ->
					Timber.e(error, "failed to show notification")
				}
				.toCompletable()
}

