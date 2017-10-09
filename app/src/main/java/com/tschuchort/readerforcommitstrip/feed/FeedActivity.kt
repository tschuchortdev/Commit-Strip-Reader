package com.tschuchort.readerforcommitstrip.feed

import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import com.airbnb.epoxy.SimpleEpoxyController
import com.jakewharton.rxbinding2.support.v4.widget.RxSwipeRefreshLayout
import com.jakewharton.rxbinding2.view.RxMenuItem
import com.jakewharton.rxrelay2.PublishRelay
import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.feed.FeedContract.*
import com.tschuchort.readerforcommitstrip.settings.SettingsActivity
import com.tschuchort.readerforcommitstrip.zoom.ZoomActivity
import io.apptik.multiview.layoutmanagers.ViewPagerLayoutManager
import io.reactivex.Observable
import kotterknife.bindView
import javax.inject.Inject

open class FeedActivity : AppCompatActivity(), FeedContract.View {
	private val actionBar: Toolbar by bindView(R.id.action_bar)
	private val feedOrientationMenuItem by lazy { actionBar.menu.findItem(R.id.action_display_style) }
	private val settingsMenuItem by lazy { actionBar.menu.findItem(R.id.action_settings) }
	private val feedRecycler: RecyclerView by bindView(R.id.feed_recycler)
	private val swipeRefreshLayout: SwipeRefreshLayout by bindView(R.id.swipe_refresh_layout)
	private val noInternetWarningView: TextView by bindView(R.id.no_internet_warning)
	private val feedController = SimpleEpoxyController()

	private val comicClicks = PublishRelay.create<Comic>()
	private val comicLongClicks = PublishRelay.create<Comic>()

	val component by lazy {
		DaggerFeedComponent.builder()
				.appComponent((application as App).component)
				.build()!!
	}

	@Inject
	lateinit var presenter: Presenter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

        component.inject(this)

		setContentView(R.layout.activity_feed)
        title = resources.getString(R.string.feed_title)

		/*
		 actionbar menu has to be inflated manually in onCreate because the render
		 function is called immediately after presenter.attachView in onStart
		 and at that point the menu items we access in render wouldn't be initialized
		 since onCreateOptionsMenu is called so late in the lifecycle
		 */
		actionBar.inflateMenu(R.menu.actionbar_feed)

        feedRecycler.layoutManager = LinearLayoutManager(this)
		feedRecycler.adapter = feedController.adapter
		feedRecycler.itemAnimator = DefaultItemAnimator()
        feedRecycler.setHasFixedSize(true)

		if(savedInstanceState != null) {
			feedController.onRestoreInstanceState(savedInstanceState)
			presenter.onRestoreInstanceState(savedInstanceState)
		}
    }

    override fun onStart() {
        super.onStart()
		presenter.attachView(this)
    }

	override fun onStop() {
		super.onStop()
		presenter.detachView(isFinishing)
	}

	override fun onSaveInstanceState(outState: Bundle?) {
		super.onSaveInstanceState(outState)
		feedController.onSaveInstanceState(outState)
		presenter.onSaveInstanceState(outState)
	}

	override fun setTitle(title: CharSequence?) = actionBar.setTitle(title)

	override fun setTitle(@StringRes titleId: Int) = actionBar.setTitle(titleId)

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

		noInternetWarningView.visibility = if(state.internetConnected) GONE else VISIBLE

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
			is Command.ShowLoadingFailed ->
				Toast.makeText(this, getString(R.string.toast_failed_to_load_comics), Toast.LENGTH_SHORT)
						.show()

			is Command.ShowNoMoreComics ->
				Toast.makeText(this, getString(R.string.toast_no_more_comics_to_load), Toast.LENGTH_SHORT)
						.show()

			is Command.ShowEnlarged -> {
				startActivity(Intent(this, ZoomActivity::class.java).apply {
					putExtra(getString(R.string.extra_selected_comic), command.selectedComic)
				})
			}

			is Command.Share -> shareText(command.selectedComic.link, getString(R.string.share_call_to_action))

			is Command.ScrollToTop -> feedRecycler.smoothScrollToPosition(0)

			is Command.StartSettings -> startActivity(Intent(this, SettingsActivity::class.java))
		}
	}

	override val events by lazy {
		Observable.mergeArray(
				RxMenuItem.clicks(settingsMenuItem)
						.map { Event.SettingsClicked },
				RxMenuItem.clicks(feedOrientationMenuItem)
						.map { Event.OrientationChanged },
				feedRecycler.onEndReachedEvents()
						.map { Event.EndReached },
				RxSwipeRefreshLayout.refreshes(swipeRefreshLayout)
						.map { Event.Refresh },
				comicClicks
						.map(Event::ComicClicked),
				comicLongClicks
						.map(Event::ComicLongClicked))!!
	}
}