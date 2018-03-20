package com.tschuchort.readerforcommitstrip.feed

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import com.tschuchort.readerforcommitstrip.findFirstVisibleItemPosition
import io.apptik.multiview.layoutmanagers.ViewPagerLayoutManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import timber.log.Timber

fun RecyclerView.addOnEndReachedListener(visibleItemThreshhold: Int = 0, onEndReached: (RecyclerView) -> Unit): RecyclerView.OnScrollListener {
	val scrollListener = object : RecyclerView.OnScrollListener() {
		private var previousScrollWasEnd = false

		override fun onScrolled(rv: RecyclerView?, dx: Int, dy: Int) {
			// might be null due to asynchronous execution of listeneres and view.requestLayout
			if(rv == null) {
				Timber.w("RecyclerView.onScroll listener invocation dropped due to recyclerView argument being null")
				return
			}

			val layoutManager = rv.layoutManager
			val visibleItemCount = layoutManager.childCount
			val totalItemCount = layoutManager.itemCount

			val firstVisibleItemPosition =
					try { layoutManager.findFirstVisibleItemPosition() }
					catch(e: IllegalArgumentException){
						throw IllegalArgumentException(
								"can not set addOnEndReachedListener for this recyclerview because " +
								"the layout manager doesn't support findFirstVisibleItemPosition")
					}

			if (adapter.itemCount > 0 && scrollState != RecyclerView.SCROLL_STATE_IDLE && !previousScrollWasEnd
				&& (totalItemCount - visibleItemCount) <= (firstVisibleItemPosition + visibleItemThreshhold)) {

				onEndReached(rv)
				previousScrollWasEnd = true
			}
			else {
				previousScrollWasEnd = false
			}
		}
	}

	addOnScrollListener(scrollListener)
	return scrollListener
}

fun RecyclerView.onEndReachedEvents(visibleItemThreshhold: Int = 2) = Observable.create<Unit> { emitter ->
	val listener = addOnEndReachedListener(visibleItemThreshhold) {
		emitter.onNext(Unit)
	}

	emitter.setCancellable {
		emitter.onComplete()
		removeOnScrollListener(listener)
	}
}!!
