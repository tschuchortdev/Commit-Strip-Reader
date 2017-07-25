package com.tschuchort.readerforcommitstrip.feed

import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.R

open class ScrollableComicItem(
		comic: Comic,
		onClick: (Comic) -> Unit = {},
		onLongClick: (Comic) -> Unit = {})
	: ComicItem(comic, onClick, onLongClick) {

	override fun getDefaultLayout() = R.layout.feed_scrollable_card
}