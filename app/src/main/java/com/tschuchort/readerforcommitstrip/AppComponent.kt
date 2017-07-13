package com.tschuchort.readerforcommitstrip

import android.app.Application
import android.content.res.Resources
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {
	fun provideAppContext(): Application
	fun provideResources(): Resources

	fun inject(app: App)
}