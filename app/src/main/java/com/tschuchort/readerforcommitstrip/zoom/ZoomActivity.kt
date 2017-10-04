package com.tschuchort.readerforcommitstrip.zoom

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
import com.tschuchort.readerforcommitstrip.zoom.ZoomContract.*
import io.reactivex.Observable
import kotterknife.bindView
import javax.inject.Inject

class ZoomActivity : AppCompatActivity(), ZoomContract.View {
	private val photoView: PhotoView by bindView(R.id.photo_view)
	private val actionBar: Toolbar by bindView(R.id.action_bar)

	@Inject
	protected lateinit var presenter: ZoomContract.Presenter

	val component by lazy {
		val selectedComic: Comic = intent!!.getParcelableExtra(getString(R.string.extra_selected_comic))!!

		DaggerZoomComponent.builder()
				.appComponent((application as App).component)
				.zoomModule(ZoomModule(selectedComic))
				.build()!!
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
		super.onStop()
		presenter.detachView(isFinishing)
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

	override fun doSideEffect(command: Command) {
		when(command) {
			is Command.Share      -> shareText(command.comic.link, getString(R.string.share_call_to_action))
			is Command.NavigateUp -> navigateUp()
		}
	}

	override val events by lazy {
		Observable.merge(
				RxMenuItem.clicks(actionBar.menu.findItem(R.id.action_share))
						.map<Event> { Event.ShareClicked },
				RxToolbar.navigationClicks(actionBar)
						.map<Event> { Event.UpClicked })!!
	}
}