package com.tschuchort.readerforcommitstrip

import android.app.Application
import com.squareup.leakcanary.LeakCanary

class App : Application() {

	val component by lazy {
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

		LeakCanary.install(this)
	}
}