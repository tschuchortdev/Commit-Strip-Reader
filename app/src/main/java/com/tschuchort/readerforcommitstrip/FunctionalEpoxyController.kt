package com.tschuchort.readerforcommitstrip

import com.airbnb.epoxy.EpoxyController
import com.squareup.haha.guava.collect.ImmutableList
import io.reactivex.Observable

class FunctionalEpoxyController : EpoxyController() {
	private var items

	fun setItems() {}

	override fun buildModels() {
		for(item in items)
	}

	override fun onExceptionSwallowed(exception: RuntimeException) {
		// Best practice is to throw in debug so you are aware of any issues that Epoxy notices.
		// Otherwise Epoxy does its best to swallow these exceptions and continue gracefully
		if(BuildConfig.DEBUG)
			throw exception
	}
}