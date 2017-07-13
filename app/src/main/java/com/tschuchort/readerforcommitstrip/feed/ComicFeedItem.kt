package com.tschuchort.readerforcommitstrip.feed

import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bumptech.glide.Glide
import com.tschuchort.readerforcommitstrip.BaseEpoxyHolder
import com.tschuchort.readerforcommitstrip.Comic
import com.tschuchort.readerforcommitstrip.R

class ComicFeedItem(
		@EpoxyAttribute val comic: Comic) : EpoxyModelWithHolder<ComicFeedItem.Holder>() {

	override fun getDefaultLayout() = R.layout.feed_card

	override fun bind(holder: Holder) {
		super.bind(holder)

		holder.titleView.text = comic.title

		Glide.with(holder.context)
				.load(comic.imageUrl)
				.crossFade()
				.into(holder.comicView)
	}

	override fun unbind(holder: Holder) {
		super.unbind(holder)
		Glide.clear(holder.comicView)
	}

	override fun id(): Long {
		//TODO
	}
	override fun createNewHolder() = Holder()

	class Holder : BaseEpoxyHolder() {
		@BindView(R.id.title) lateinit var titleView: TextView
		@BindView(R.id.comic) lateinit var comicView: ImageView
	}
}