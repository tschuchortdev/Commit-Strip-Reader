package com.tschuchort.readerforcommitstrip

import android.graphics.drawable.Drawable
import android.os.Parcelable
import java.util.*

data class Comic(
		val title: String,
		val date: String,
		val commentsCount: Int,
		val sponsoredText: String,
		val imageUrl: Drawable
)