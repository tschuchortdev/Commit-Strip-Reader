package com.tschuchort.readerforcommitstrip

import android.app.Activity
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import it.cosenonjaviste.daggermock.DaggerMockRule
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher


inline fun <reified A : Activity> checkActivityLaunched() {
	intended(hasComponent(ComponentName(getTargetContext(), A::class.java)))
}

/**
 * checks whether the item is a refreshing SwipeRefreshLayout
 */
fun isRefreshing() = object : TypeSafeMatcher<View>() {
	override fun describeTo(description: Description) {
		description.appendText("is a refreshing SwipeRefreshLayout")
	}

	override fun matchesSafely(item: View?)
			= (item as? SwipeRefreshLayout)?.isRefreshing ?: false
}

enum class ScreenOrientation { LANDSCAPE, PORTRAIT }

fun Activity.rotateOrientation(orientation: ScreenOrientation) {
	requestedOrientation = when (orientation) {
		ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
		ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
	}
}

fun getApp() = InstrumentationRegistry
		.getInstrumentation()
		.targetContext.applicationContext as App

fun getResources() = InstrumentationRegistry
		.getInstrumentation()
		.targetContext.resources

class EspressoDaggerMockRule : DaggerMockRule<AppComponent>(AppComponent::class.java, AppModule(getApp())) {
	init {
		set { getApp().component = it }
	}
}

fun recyclerScrollToEnd(recyclerId: Int, parent: Activity) {
	val recycler = parent.findViewById<RecyclerView>(recyclerId)

	onView(withId(recyclerId))
			.perform(scrollToPosition<RecyclerView.ViewHolder>(recycler.adapter.itemCount - 1))
}