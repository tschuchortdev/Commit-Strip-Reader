package com.tschuchort.readerforcommitstrip

import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import timber.log.Timber


class FirebaseCrashReportTree : Timber.Tree() {
	override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
		if (priority == Log.VERBOSE || priority == Log.DEBUG) {
			return
		}

		if(message != null) {
			FirebaseCrash.log(message)
		}

		if (t != null) {
			FirebaseCrash.report(t)
		}
	}
}