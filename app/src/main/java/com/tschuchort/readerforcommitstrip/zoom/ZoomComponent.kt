package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.ActivityModule
import com.tschuchort.readerforcommitstrip.AppComponent
import com.tschuchort.readerforcommitstrip.PerActivity
import dagger.Component


@PerActivity
@Component(
		modules = arrayOf(ZoomModule::class, ActivityModule::class),
		dependencies = arrayOf(AppComponent::class))
interface ZoomComponent : AppComponent {
	fun exposePresenter(): ZoomContract.Presenter

	fun inject(activity: ZoomActivity)
}