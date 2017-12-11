package com.tschuchort.readerforcommitstrip

import android.app.Activity
import dagger.Module
import dagger.Provides

/**
 * Module for stuff that depends on the activity
 */
@AllOpen
@Module
class ActivityModule(private val activity: Activity) {

    @Provides
    @PerActivity
    fun provideActivity() = activity

    @Provides
    @PerActivity
    fun provideNavigator(navigatorImpl: NavigatorImpl): Navigator = navigatorImpl
}