package com.tschuchort.readerforcommitstrip.feed

import com.tschuchort.readerforcommitstrip.*
import dagger.Component

@PerActivity
@Component(
		modules = arrayOf(FeedModule::class),
		dependencies = arrayOf(AppComponent::class))
interface FeedComponent : AppComponent {
	fun exposePresenter(): FeedContract.Presenter

	fun inject(activity: FeedActivity)
}