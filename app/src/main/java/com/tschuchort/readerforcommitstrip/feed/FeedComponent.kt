package com.tschuchort.readerforcommitstrip.feed

import com.tschuchort.readerforcommitstrip.ActivityModule
import com.tschuchort.readerforcommitstrip.AppComponent
import com.tschuchort.readerforcommitstrip.PerActivity
import dagger.Component

@PerActivity
@Component(
		modules = arrayOf(FeedModule::class, ActivityModule::class),
		dependencies = arrayOf(AppComponent::class))
interface FeedComponent : AppComponent {
	fun exposePresenter(): FeedContract.Presenter

	fun inject(activity: FeedActivity)
}