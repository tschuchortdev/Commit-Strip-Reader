package com.tschuchort.readerforcommitstrip

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Comic(
		val title: String,
		val date: String,
		val description: String?,
		val link: String,
		val imageUrl: String,
		val categories: List<String> = emptyList()
) : Parcelable