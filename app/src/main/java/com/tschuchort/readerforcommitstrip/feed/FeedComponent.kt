package com.tschuchort.readerforcommitstrip.feed

import com.tschuchort.readerforcommitstrip.ActivityModule
import com.tschuchort.readerforcommitstrip.PerActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [FeedModule::class, ActivityModule::class])
interface FeedComponent {
	fun exposePresenter(): FeedContract.Presenter

	fun inject(activity: FeedActivity)
}