package com.tschuchort.readerforcommitstrip

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import io.apptik.multiview.layoutmanagers.ViewPagerLayoutManager
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import java.lang.UnsupportedOperationException


fun <T> Iterable<T>.dropUntilAfter(predicate: (T) -> Boolean) = dropWhile { !predicate(it) }.drop(1)

fun <R> List<R>.addIf(condition: Boolean, element: R) = if(condition) this + element else this

fun CharSequence.substringStartsWith(prefix: String, startIndex: Int = 0) = substring(indexOf(prefix, startIndex))

fun CharSequence.indexOf(regex: Regex, startIndex: Int = 0) = regex.find(this, startIndex)

fun <T> nonNull(value: T) = value != null
fun <T> isNull(value: T) = value == null

val Int.dp: Int
	get() = dpToPx(this)

fun dpToPx(dp: Int)
		= Math.round(dp * (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun dpToPx(dp: Float)
		= Math.round(dp * (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun pxToDp(px: Int)
		= Math.round(px / (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun getScreenSize(context: Context): Point {
	val size = Point()
	(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(size)
	return size
}

fun getScreenWidth(context: Context) = getScreenSize(context).x

fun getScreenHeight(context: Context) = getScreenSize(context).y

fun View.setPaddingLeft(left: Int) = setPadding(left, paddingTop, paddingRight, paddingBottom)
fun View.setPaddingStart(start: Int) = setPaddingRelative(start, paddingTop, paddingEnd, paddingBottom)
fun View.setPaddingRight(right: Int) = setPadding(paddingLeft, paddingTop, right, paddingBottom)
fun View.setPaddingEnd(end: Int) = setPaddingRelative(paddingStart, paddingTop, end, paddingBottom)
fun View.setPaddingTop(top: Int) = setPaddingRelative(paddingStart, top, paddingEnd, paddingBottom)
fun View.setPaddingBottom(bottom: Int) = setPaddingRelative(paddingStart, paddingTop, paddingEnd, bottom)

var View.marginLeft: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				leftMargin = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginStart: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				marginStart = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginTop: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				topMargin = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginRight: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.rightMargin ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				rightMargin = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginEnd: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.marginEnd ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				marginEnd = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginBottom: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				bottomMargin = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

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

inline fun <reified LM : RecyclerView.LayoutManager> RecyclerView.swapLayoutManager(layoutManager: LM) {
	val oldLayoutManager = this.layoutManager

	// this check is important because
	// the RV scrolls back to the top every time layout manager is changed
	if(oldLayoutManager is LM && oldLayoutManager::class == layoutManager::class)
		return

	val firstVisibleItemPos =
			try { oldLayoutManager.findFirstVisibleItemPosition() }
			catch (e: IllegalArgumentException) { RecyclerView.NO_POSITION }

	this.layoutManager = layoutManager

	if(firstVisibleItemPos != 0 && firstVisibleItemPos != RecyclerView.NO_POSITION) {
		scrollToPosition(firstVisibleItemPos)
	}
}