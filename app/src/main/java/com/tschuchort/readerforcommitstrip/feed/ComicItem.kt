package com.tschuchort.readerforcommitstrip.feed

import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tschuchort.readerforcommitstrip.BaseEpoxyHolder
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.R

open class ComicItem(
		@EpoxyAttribute @JvmField val comic: Comic,
		val onClick: (Comic) -> Unit = {},
		val onLongClick: (Comic) -> Unit = {})
	: EpoxyModelWithHolder<ComicItem.Holder>() {

	init {
		id(comic.title)
	}

	override fun getDefaultLayout() = R.layout.feed_card

	override fun bind(holder: Holder) {
		super.bind(holder)

		holder.titleView.text = comic.title
		holder.comicView.setOnClickListener { onClick(comic) }
		holder.comicView.setOnLongClickListener { onLongClick(comic); true }

		Glide.with(holder.context)
				.load(comic.imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.RESULT)
				.crossFade()
				.into(holder.comicView)
	}

	override fun unbind(holder: Holder) {
		super.unbind(holder)
		Glide.clear(holder.comicView)
	}

	override fun createNewHolder() = Holder()

	class Holder : BaseEpoxyHolder() {
		val titleView: TextView by bindView(R.id.title)
		val comicView: ImageView by bindView(R.id.comic)
	}
}