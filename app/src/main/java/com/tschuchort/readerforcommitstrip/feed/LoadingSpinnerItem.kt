package com.tschuchort.readerforcommitstrip.feed

import android.view.ViewGroup
import android.view.ViewGroup.*
import android.view.ViewGroup.LayoutParams.*
import android.widget.ProgressBar
import com.airbnb.epoxy.EpoxyModelWithView
import com.tschuchort.readerforcommitstrip.dp


class LoadingSpinnerItem : EpoxyModelWithView<ProgressBar>() {

	init {
		id("loading spinner")
	}

	override fun buildView(parent: ViewGroup) = ProgressBar(parent.context).apply {
		layoutParams = MarginLayoutParams(LayoutParams(MATCH_PARENT, WRAP_CONTENT))
				.apply {
					topMargin = 8.dp
					bottomMargin = 16.dp // 8dp extra to account for margin on the bottom of card above
				}

		isIndeterminate = true
	}

}