package com.tschuchort.readerforcommitstrip

import android.app.Activity
import androidx.test.espresso.intent.rule.IntentsTestRule
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.Semaphore

abstract class BaseActivityTest<out T : Activity>(activityClass: Class<out T>) {
    @get:Rule
    val daggermockRule = EspressoDaggerMockRule()

    @get:Rule
    val intentsRule = IntentsTestRule(activityClass, false, false)

    protected val activity by lazy { intentsRule.activity!! }

    protected fun doOnUi(task: () -> Unit) {
        val mutex = Semaphore(1)
        mutex.acquire()

        activity.runOnUiThread {
            task()
            mutex.release()
        }

        // block until the mutex is released by runOnUiThread
        mutex.acquire()
    }

    protected fun launchActivity() = intentsRule.launchActivity(null)

    @Before
    fun before() {
        // we need to launch the activity ourselves, otherwise DaggerMock won't
        // be fast enough to swap out the components in time before the first test
        launchActivity()
    }
}