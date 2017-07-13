package com.tschuchort.readerforcommitstrip.feed

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import butterknife.bindView
import com.airbnb.epoxy.SimpleEpoxyController
import com.jakewharton.rxbinding2.support.v4.widget.RxSwipeRefreshLayout
import com.jakewharton.rxbinding2.view.RxMenuItem
import com.tschuchort.readerforcommitstrip.R
import io.apptik.multiview.layoutmanagers.ViewPagerLayoutManager
import io.reactivex.Observable
import javax.inject.Inject
import com.tschuchort.readerforcommitstrip.feed.FeedContract.*

class FeedActivity : AppCompatActivity(), FeedContract.View {
	private lateinit var feedOrientationMenuItem: MenuItem
	private lateinit var settingsMenuItem: MenuItem

	private val feedRecycler: RecyclerView by bindView(R.id.feed_recycler)
	private val swipeRefreshLayout: SwipeRefreshLayout by bindView(R.id.swipe_refresh_layout)
	private val noInternetWarningView: TextView by bindView(R.id.no_internet_warning)
	private val feedController = SimpleEpoxyController()

	@Inject
	private lateinit var presenter: Presenter

	override val uiEvents: Observable<Event> = Observable.merge(
			onSettingsClicked()
	)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_feed)
		title = resources.getString(R.string.feed_title)

		feedRecycler.layoutManager = LinearLayoutManager(this)
		feedRecycler.adapter = feedController.adapter
		presenter.attechView(this)
	}

	override fun onStop() {
		super.onStop()
		presenter.detachView()
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
		super.onRestoreInstanceState(savedInstanceState)
		feedController.onRestoreInstanceState(savedInstanceState)
		presenter.onRestoreInstanceState(savedInstanceState)
	}

	override fun onSaveInstanceState(outState: Bundle?) {
		super.onSaveInstanceState(outState)
		feedController.onSaveInstanceState(outState)
		presenter.onSaveInstanceState(outState)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.actionbar_feed, menu)
		feedOrientationMenuItem = menu.findItem(R.id.action_display_style)
		settingsMenuItem = menu.findItem(R.id.action_settings)

		return true
	}

	override fun render(state: State) {
		when(state.feedOrientation) {
			Orientation.VERTICAL -> {
				feedRecycler.layoutManager = LinearLayoutManager(this)
				feedOrientationMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_toolbar_horizontal_feed)
			}
			Orientation.HORIZONTAL -> {
				feedRecycler.layoutManager = ViewPagerLayoutManager(this)
				feedOrientationMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_toolbar_vertical_feed)
			}
		}

		noInternetWarningView.visibility = if(state.noInternet) View.VISIBLE else View.GONE

		swipeRefreshLayout.isRefreshing = state.isLoading

		feedController.setModels(state.comics.map { ComicFeedItem(it) })
	}

	private val onSettingsClicked = RxMenuItem.clicks(settingsMenuItem)

	private val onFeedOrientationChanged = RxMenuItem.clicks(feedOrientationMenuItem)

	private val onEndReached = feedRecycler.onEndReachedEvents()

	private val onRefresh = RxSwipeRefreshLayout.refreshes(swipeRefreshLayout)
}