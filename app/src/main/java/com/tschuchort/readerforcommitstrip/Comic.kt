package com.tschuchort.readerforcommitstrip

import android.graphics.drawable.Drawable
import android.os.Parcelable
import io.mironov.smuggler.AutoParcelable
import java.util.*

data class Comic(
		val title: String,
		val date: String,
		val description: String?,
		val link: String,
		val imageUrl: String,
		val categories: List<String> = emptyList()
) : AutoParcelable