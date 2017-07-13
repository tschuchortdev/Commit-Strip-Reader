package com.tschuchort.readerforcommitstrip.feed

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import io.apptik.multiview.layoutmanagers.ViewPagerLayoutManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

fun RecyclerView.addOnEndReachedListener(visibleItemThreshhold: Int = 0, onEndReached: (RecyclerView) -> Unit): RecyclerView.OnScrollListener {
	val scrollListener = object : RecyclerView.OnScrollListener() {
		override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
			val layoutManager = rv.layoutManager
			val visibleItemCount = layoutManager.childCount
			val totalItemCount = layoutManager.itemCount

			val firstVisibleItemPositions: List<Int> = when (layoutManager) {
				is LinearLayoutManager        -> listOf(layoutManager.findFirstVisibleItemPosition())
				is ViewPagerLayoutManager     -> listOf(layoutManager.findFirstVisibleItemPosition())
				is GridLayoutManager          -> listOf(layoutManager.findFirstVisibleItemPosition())
				is StaggeredGridLayoutManager -> layoutManager.findFirstVisibleItemPositions(null).toList()
				else                          -> throw IllegalStateException(
						"can't get firstVisibleItemPosition for unknown layoutManager in RecyclerView scroll onEndReached")
			}

			if (firstVisibleItemPositions.isNotEmpty()
				&& (totalItemCount - visibleItemCount) <= (firstVisibleItemPositions[0] + visibleItemThreshhold)) {

				onEndReached(rv)
			}
		}
	}

	addOnScrollListener(scrollListener)
	return scrollListener
}

fun RecyclerView.onEndReachedEvents() = Flowable.create<Unit>({ emitter ->
	val listener = addOnEndReachedListener(3, {
		emitter.onNext(Unit)
	})

	emitter.setCancellable {
		emitter.onComplete()
		removeOnScrollListener(listener)
	}

}, BackpressureStrategy.LATEST)!!
