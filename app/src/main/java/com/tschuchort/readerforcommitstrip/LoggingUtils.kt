package com.tschuchort.readerforcommitstrip

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import timber.log.Timber


class CrashlyticsTimberTree : Timber.Tree() {

	override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
		when(priority) {
			Log.VERBOSE, Log.DEBUG -> return
			Log.INFO -> {
				if(message != null)
					Answers.getInstance().logCustom(
							CustomEvent("Info").putCustomAttribute(
									"message", message
					))
			}
			else -> {
				if(message != null)
					Crashlytics.log(message)

				if (t != null)
					Crashlytics.logException(t)
			}
		}
	}
}