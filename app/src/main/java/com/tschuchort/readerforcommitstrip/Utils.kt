package com.tschuchort.readerforcommitstrip

import android.content.res.Resources
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import io.apptik.multiview.layoutmanagers.ViewPagerLayoutManager
import android.util.DisplayMetrics
import java.security.AccessController.getContext


fun <T> Iterable<T>.dropUntilAfter(predicate: (T) -> Boolean) = dropWhile { !predicate(it) }.drop(1)

fun <R> List<R>.addIf(condition: Boolean, element: R) = if(condition) this + element else this

fun CharSequence.substringStartsWith(prefix: String, startIndex: Int = 0) = substring(indexOf(prefix, startIndex))

fun CharSequence.indexOf(regex: Regex, startIndex: Int = 0) = regex.find(this, startIndex)

fun <T> nonNull(value: T) = value != null
fun <T> isNull(value: T) = value == null

val Int.dp: Int
	get() = pxToDp(this)

fun dpToPx(dp: Int)
		= Math.round(dp * (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun dpToPx(dp: Float)
		= Math.round(dp * (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun pxToDp(px: Int)
		= Math.round(px / (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun RecyclerView.LayoutManager.findFirstVisibleItemPosition() = when(this) {
	is LinearLayoutManager                    -> findFirstVisibleItemPosition()
	is ViewPagerLayoutManager     -> findFirstVisibleItemPosition()
	is GridLayoutManager          -> findFirstVisibleItemPosition()
	is StaggeredGridLayoutManager -> findFirstVisibleItemPositions(null).first()
	else                          -> throw IllegalArgumentException(
			"can't get first visible item position for unknown recycler view layout manager")
}

fun RecyclerView.LayoutManager.findLastVisibleItemPosition() = when(this) {
	is LinearLayoutManager                    -> findLastVisibleItemPosition()
	is ViewPagerLayoutManager     -> findLastVisibleItemPosition()
	is GridLayoutManager          -> findLastVisibleItemPosition()
	is StaggeredGridLayoutManager -> findLastVisibleItemPositions(null).first()
	else                          -> throw IllegalArgumentException(
			"can't get last visible item position for unknown recycler view layout manager")
}