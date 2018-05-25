package com.tschuchort.readerforcommitstrip.feed

import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.epoxy.SimpleEpoxyController
import com.jakewharton.rxbinding2.support.v4.widget.RxSwipeRefreshLayout
import com.jakewharton.rxbinding2.view.RxMenuItem
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxrelay2.PublishRelay
import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.feed.FeedContract.Orientation
import com.tschuchort.readerforcommitstrip.feed.FeedContract.State
import com.tschuchort.retainedproperties.retained
import io.apptik.multiview.layoutmanagers.ViewPagerLayoutManager
import kotterknife.bindView

class FeedActivity : AppCompatActivity(), FeedContract.View {
	private val actionBar: Toolbar by bindView(R.id.action_bar)
	private val feedOrientationMenuItem by lazy { actionBar.menu.findItem(R.id.action_display_style) }
	private val settingsMenuItem by lazy { actionBar.menu.findItem(R.id.action_settings) }
	private val feedRecycler: RecyclerView by bindView(R.id.feed_recycler)
	private val swipeRefreshLayout: SwipeRefreshLayout by bindView(R.id.swipe_refresh_layout)
	private val noInternetWarningView: TextView by bindView(R.id.no_internet_warning)
	private val feedController = SimpleEpoxyController()
	private lateinit var dialog: BottomSheetDialog
	private val shareButton by lazy { dialog.findViewById<LinearLayout>(R.id.bottomsheet_share)!! }
	private val saveButton by lazy { dialog.findViewById<LinearLayout>(R.id.bottomsheet_save)!! }

	private val comicClickRelay = PublishRelay.create<Comic>()
	private val comicLongClickRelay = PublishRelay.create<Comic>()
	private val dialogCanceledRelay = PublishRelay.create<Unit>()

	private val component by lazy {
		(application as App).component.newActivityComponent(ActivityModule(this))
	}

	private val presenter by retained { component.exposeFeedPresenter() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

        component.inject(this)

		setContentView(R.layout.activity_feed)
        actionBar.title = resources.getString(R.string.feed_title)

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

		dialog = createBottomSheet(
                layoutInflater.inflate(R.layout.download_share_dialog_sheet, null)
        )

		if(savedInstanceState != null) {
			feedController.onRestoreInstanceState(savedInstanceState)
			presenter.onRestoreInstanceState(savedInstanceState)
		}
    }

    override fun onStart() {
        super.onStart()
		dialog.setOnCancelListener { dialogCanceledRelay.accept(Unit) }
		presenter.attachView(this)
    }

	override fun onStop() {
		presenter.detachView(isFinishing)
		dialog.setOnCancelListener(null)
		super.onStop()
	}

	override fun onSaveInstanceState(outState: Bundle?) {
		super.onSaveInstanceState(outState)
		feedController.onSaveInstanceState(outState)
		presenter.onSaveInstanceState(outState)
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

		noInternetWarningView.visibility = if(state.internetConnected) GONE else VISIBLE

		swipeRefreshLayout.isRefreshing = state.refreshing

		feedController.setModels(
				state.comics
						.map {
							if(state.feedOrientation == Orientation.VERTICAL)
								ComicItem(
										comic = it,
										onClick = comicClickRelay::accept,
										onLongClick = comicLongClickRelay::accept)
							else
								ScrollableComicItem(
										comic = it,
										onClick = comicClickRelay::accept,
										onLongClick = comicLongClickRelay::accept)
						}
						.addIf(state.loading, LoadingSpinnerItem()))

		if(state.selectedComic != null)
			dialog.show()
		else
			dialog.hide()
	}

	// effects

	override fun showRefreshFailed() = toast(getString(R.string.toast_failed_to_load_comics))

	override fun showNoMoreComics() = toast(getString(R.string.toast_no_more_comics_to_load))

	override fun showSaveFailed() = toast(getString(R.string.toast_failed_to_save_comic))

	override fun share(image: Bitmap, title: String)
			= shareImage(image, title, getString(R.string.share_call_to_action))

	override fun showShareFailed() = toast(getString(R.string.toast_failed_to_share))

	override fun scrollToTop() = feedRecycler.smoothScrollToPosition(0)

	override fun showSaveSuccesful() = toast(getString(R.string.toast_saved_comic))

	override fun showDownloadFailed() = toast(getString(R.string.toast_download_failed))

	// signals

	override val settingsClicked by lazy { RxMenuItem.clicks(settingsMenuItem).share().toUnit() }

	override val changeFeedLayoutClicked by lazy {
		RxMenuItem.clicks(feedOrientationMenuItem).toUnit().toUnit()
	}

	override val endReached by lazy { feedRecycler.onEndReachedEvents().toUnit() }

	override val refresh by lazy {
		RxSwipeRefreshLayout.refreshes(swipeRefreshLayout).share().toUnit()
	}

	override val comicClicked = comicClickRelay!!

	override val comicLongClicked = comicLongClickRelay!!

	override val dialogCanceled = dialogCanceledRelay!!

	override val saveClicked by lazy { RxView.clicks(saveButton).share().toUnit() }

	override val shareClicked by lazy { RxView.clicks(shareButton).share().toUnit() }

	// other

    private fun createBottomSheet(contentView: View): BottomSheetDialog {
        val sheet = BottomSheetDialog(this)
        sheet.setContentView(contentView)
        return sheet
    }
}
