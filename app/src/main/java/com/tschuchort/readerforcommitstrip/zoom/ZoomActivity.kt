package com.tschuchort.readerforcommitstrip.zoom

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView
import com.jakewharton.rxbinding2.support.v7.widget.RxToolbar
import com.jakewharton.rxbinding2.view.RxMenuItem
import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.Presenter
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.State
import com.tschuchort.retainedproperties.retained
import kotterknife.bindView
import javax.inject.Inject

class ZoomActivity : AppCompatActivity(), ZoomContract.View {
	private val photoView: PhotoView by bindView(R.id.photo_view)
	private val actionBar: Toolbar by bindView(R.id.action_bar)

	private val component by lazy {
		(application as App).component.newActivityComponent(ActivityModule(this))
	}

	@Inject
	protected lateinit var presenterFactory: Presenter.Factory

	private val presenter by retained {
		val selectedComic: Comic = intent!!.getParcelableExtra(getString(R.string.extra_selected_comic))!!
		presenterFactory.create(selectedComic)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_zoom)

		component.inject(this)

		/*
		 actionbar menu has to be inflated manually in onCreate because the render
		 function is called immediately after presenter.attachView in onStart
		 and at that point the menu items we access in render wouldn't be initialized
		 since onCreateOptionsMenu is called so late in the lifecycle
		 */
		actionBar.inflateMenu(R.menu.actionbar_zoom)
		actionBar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_toolbar_back)

		if(savedInstanceState != null) {
			presenter.onRestoreInstanceState(savedInstanceState)
		}
	}

	override fun onStart() {
		super.onStart()
		presenter.attachView(this)
	}

	override fun onStop() {
		presenter.detachView(isFinishing)
		super.onStop()
	}

	override fun onSaveInstanceState(outState: Bundle?) {
		super.onSaveInstanceState(outState)
		presenter.onSaveInstanceState(outState)
	}

	override fun render(state: State) {
		actionBar.title = state.comic.title

		Glide.with(this)
				.load(state.comic.imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.SOURCE)
				.dontTransform()
				.crossFade()
				.into(photoView)
	}

	// signals

	override val shareClicked by lazy {
		RxMenuItem.clicks(actionBar.menu.findItem(R.id.action_share)).share().toUnit()
				.andLogEvent("ShareClicked")
	}

	override val saveClicked by lazy {
		RxMenuItem.clicks(actionBar.menu.findItem(R.id.action_save)).share().toUnit()
				.andLogEvent("SaveClicked")
	}

	override val upClicked by lazy {
		RxToolbar.navigationClicks(actionBar).share().toUnit().andLogEvent("UpClicked")
	}

	// effects

	override fun share(image: Bitmap, title: String)
			= shareImage(image, title, getString(R.string.share_call_to_action))

	override fun showSaveSuccessful() = toast(getString(R.string.toast_saved_comic))

	override fun showSaveFailed() = toast(getString(R.string.toast_failed_to_save_comic))

	override fun showDownloadFailed() = toast(getString(R.string.toast_download_failed))

}