package com.tschuchort.readerforcommitstrip

import android.content.Context
import butterknife.ButterKnife
import android.support.annotation.CallSuper
import android.view.View
import com.airbnb.epoxy.EpoxyHolder



/**
 * Creating a base holder class allows us to leverage ButterKnife's view binding for all subclasses.
 * This makes subclasses much cleaner, and is a highly recommended pattern.
 */
abstract class BaseEpoxyHolder : EpoxyHolder() {
	lateinit var context: Context

	@CallSuper
	final override fun bindView(itemView: View) {
		context = itemView.context
		ButterKnife.bind(this, itemView)
	}
}