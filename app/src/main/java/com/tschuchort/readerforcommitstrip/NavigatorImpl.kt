package com.tschuchort.readerforcommitstrip

import android.app.Activity
import android.content.Intent
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import com.tschuchort.readerforcommitstrip.settings.SettingsActivity
import com.tschuchort.readerforcommitstrip.zoom.ZoomActivity

class NavigatorImpl(private val sourceActivity: Activity) : Navigator {
	override fun showSettings() =
			sourceActivity.startActivity(
					sourceActivity.makeIntent<SettingsActivity>(), null)

	override fun showZoomedScreen(comic: Comic) =
			sourceActivity.startActivity(
					sourceActivity.makeIntent<ZoomActivity>().apply {
						putExtra(sourceActivity.getString(R.string.extra_selected_comic), comic)
					},
					null)

	override fun navigateUp() = sourceActivity.navigateUp()
}

fun Activity.navigateUp() {
	val upIntent = NavUtils.getParentActivityIntent(this)

	// if this task is the root, the parent cant be in the back stack anymore, so we need to
	// recreate it anyway. Otherwise it will just navigate back to the home screen
	if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot) {
		TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities()
	}
	// parent activity still exists in back stack, so we set flags to navigate and restore state
	else {
		upIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
		NavUtils.navigateUpTo(this, upIntent)
	}
}