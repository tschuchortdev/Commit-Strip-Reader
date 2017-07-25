package com.tschuchort.readerforcommitstrip.feed

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import butterknife.bindView
import com.airbnb.epoxy.SimpleEpoxyController
import com.jakewharton.rxbinding2.support.v4.widget.RxSwipeRefreshLayout
import com.jakewharton.rxbinding2.view.RxMenuItem
import com.jakewharton.rxrelay2.PublishRelay
import com.tschuchort.readerforcommitstrip.*
import io.apptik.multiview.layoutmanagers.ViewPagerLayoutManager
import javax.inject.Inject
import com.tschuchort.readerforcommitstrip.feed.FeedContract.*
import io.reactivex.Observable

open class FeedActivity : AppCompatActivity(), FeedContract.View {
	private lateinit var feedOrientationMenuItem: MenuItem
	private lateinit var settingsMenuItem: MenuItem
	private val comicClicks = PublishRelay.create<Comic>()
	private val comicLongClicks = PublishRelay.create<Comic>()
	private val settingsClicks = PublishRelay.create<Unit>()
	private val changeOriantationClicks = PublishRelay.create<Unit>()

	private val feedRecycler: RecyclerView by bindView(R.id.feed_recycler)
	private val swipeRefreshLayout: SwipeRefreshLayout by bindView(R.id.swipe_refresh_layout)
	private val noInternetWarningView: TextView by bindView(R.id.no_internet_warning)
	private val feedController = SimpleEpoxyController()

	val component by lazy {
		DaggerFeedComponent.builder()
				.appComponent((application as App).component)
				.build()!!
	}

	@Inject
	internal lateinit var presenter: Presenter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_feed)
		title = resources.getString(R.string.feed_title)

		component.inject(this)

		feedRecycler.layoutManager = LinearLayoutManager(this)
		feedRecycler.adapter = feedController.adapter
		feedRecycler.setHasFixedSize(true)
		feedRecycler.itemAnimator = DefaultItemAnimator()

		if(savedInstanceState != null) {
			feedController.onRestoreInstanceState(savedInstanceState)
			//TODO presenter.onRestoreInstanceState(savedInstanceState)
		}
	}

	override fun onStop() {
		super.onStop()
		presenter.detachView()
	}

	override fun onSaveInstanceState(outState: Bundle?) {
		super.onSaveInstanceState(outState)
		feedController.onSaveInstanceState(outState)
		presenter.onSaveInstanceState(outState)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)

		menuInflater.inflate(R.menu.actionbar_feed, menu)

		feedOrientationMenuItem = menu.findItem(R.id.action_display_style)
		settingsMenuItem = menu.findItem(R.id.action_settings)

		RxMenuItem.clicks(feedOrientationMenuItem).subscribe { changeOriantationClicks.accept(Unit) }
		RxMenuItem.clicks(settingsMenuItem).subscribe { settingsClicks.accept(Unit) }

		presenter.attechView(this)
		return true
	}

	override fun render(state: State) {
		when(state.feedOrientation) {
			Orientation.VERTICAL -> {
				feedRecycler.swapLayoutManager(LinearLayoutManager(this))
				feedOrientationMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_toolbar_horizontal_feed)
			}
			Orientation.HORIZONTAL -> {
				feedRecycler.swapLayoutManager(ViewPagerLayoutManager(this, ViewPagerLayoutManager.HORIZONTAL, false))
				feedOrientationMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_toolbar_vertical_feed)
			}
		}

		noInternetWarningView.visibility = if(state is State.NoInternet) View.VISIBLE else View.GONE

		swipeRefreshLayout.isRefreshing = state is State.Refreshing

		feedController.setModels(
				state.comics
						.map {
							if(state.feedOrientation == Orientation.VERTICAL)
								ComicItem(
										comic = it,
										onClick = comicClicks::accept,
										onLongClick = comicLongClicks::accept)
							else
								ScrollableComicItem(
										comic = it,
										onClick = comicClicks::accept,
										onLongClick = comicLongClicks::accept)
						}
						.addIf(state is State.LoadingMore, LoadingSpinnerItem()))
	}

	override fun doSideEffect(command: Command) {
		when(command) {
			is Command.ShowRefreshFailed -> Toast
					.makeText(this, getString(R.string.toast_failed_to_refresh_feed), Toast.LENGTH_SHORT)
					.show()
		}
	}

	override fun events() = Observable.merge(
			settingsClicks
					.map { Event.SettingsClicked },
			changeOriantationClicks
					.map { Event.OrientationChanged },
			feedRecycler.onEndReachedEvents()
					.map { Event.EndReached },
			RxSwipeRefreshLayout.refreshes(swipeRefreshLayout)
					.map { Event.Refresh }/*,
					TODO
			comicClicks
					.map(Event::ComicClicked),
			comicLongClicks
					.map(Event::ComicLongClicked)*/)!!
}

inline fun <reified LM : RecyclerView.LayoutManager> RecyclerView.swapLayoutManager(layoutManager: LM) {
	val oldLayoutManager = this.layoutManager

	// this check is important because
	// the RV scrolls back to the top everytime layout manager is changed
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