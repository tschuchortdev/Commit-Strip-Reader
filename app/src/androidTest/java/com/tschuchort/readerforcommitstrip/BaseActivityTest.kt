package com.tschuchort.readerforcommitstrip

import android.support.test.espresso.intent.rule.IntentsTestRule
import com.tschuchort.readerforcommitstrip.feed.FeedActivity
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.Semaphore

abstract class BaseActivityTest {
    @get:Rule
    val daggermockRule = EspressoDaggerMockRule()

    @get:Rule
    val intentsRule = IntentsTestRule(FeedActivity::class.java, false, false)

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