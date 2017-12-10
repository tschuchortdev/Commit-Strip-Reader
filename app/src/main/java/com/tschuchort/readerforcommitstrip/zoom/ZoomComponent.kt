package com.tschuchort.readerforcommitstrip.zoom

import com.tschuchort.readerforcommitstrip.ActivityModule
import com.tschuchort.readerforcommitstrip.PerActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [ZoomModule::class, ActivityModule::class])
interface ZoomComponent {
	fun exposePresenter(): ZoomContract.Presenter

	fun inject(activity: ZoomActivity)
}