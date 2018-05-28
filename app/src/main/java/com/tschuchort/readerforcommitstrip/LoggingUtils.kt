package com.tschuchort.readerforcommitstrip

import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import io.reactivex.Observable
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

fun <T> Observable<T>.andLogEvent(eventName: String, logContent: Boolean = false) = doOnNext {
	val content = if(logContent) ": $it" else ""
	Timber.i("Event: $eventName$content")
}