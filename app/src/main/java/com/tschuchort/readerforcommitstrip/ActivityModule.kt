package com.tschuchort.readerforcommitstrip

import android.app.Activity
import dagger.Module
import dagger.Provides

/**
 * Module for stuff that depends on the activity
 */
@Module
class ActivityModule(private val activity: Activity) {

    @Provides
    @PerActivity
    fun provideNavigator(): Navigator = NavigatorImpl(activity)
}