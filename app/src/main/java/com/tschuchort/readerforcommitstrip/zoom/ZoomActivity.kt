package com.tschuchort.readerforcommitstrip.zoom

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.R

class ZoomActivity : AppCompatActivity() {
	private lateinit var selectedComic: Comic
	private val photoView: PhotoView by bindView(R.id.photo_view)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_zoom)

		selectedComic = intent!!.getParcelableExtra(getString(R.string.extra_selected_comic))!!

		Glide.with(this)
				.load(selectedComic.imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.SOURCE)
				.dontTransform()
				.crossFade()
				.into(photoView)
	}

}