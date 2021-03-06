package com.tschuchort.readerforcommitstrip.zoom

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView
import com.jakewharton.rxbinding2.support.v7.widget.RxToolbar
import com.jakewharton.rxbinding2.view.RxMenuItem
import com.tschuchort.readerforcommitstrip.*
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.*
import com.tschuchort.retainedproperties.retained
import io.reactivex.Observable
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

	override fun onSaveInstanceState(outState: Bundle) {
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

	override fun doSideEffect(effect: ViewEffect) = when(effect) {
		is ViewEffect.ShareComic        -> shareText(effect.comic.link, getString(R.string.share_call_to_action))
		is ViewEffect.ShowSaveSuccesful -> toast(getString(R.string.toast_saved_comic))
		is ViewEffect.ShowSaveFailed    -> toast(getString(R.string.toast_failed_to_save_comic))
	}

	override val events by lazy {
		Observable.merge(
				RxMenuItem.clicks(actionBar.menu.findItem(R.id.action_share))
						.map<Event> { Event.ShareClicked },
				RxMenuItem.clicks(actionBar.menu.findItem(R.id.action_save))
						.map<Event> { Event.SaveClicked },
				RxToolbar.navigationClicks(actionBar)
						.map<Event> { Event.UpClicked })!!
	}
}