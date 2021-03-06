package com.tschuchort.readerforcommitstrip.feed

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tschuchort.readerforcommitstrip.*
import timber.log.Timber

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

		holder.comicView.setOnClickListener { view ->
			if(view == null)
				Timber.w("onClick listener invocation dropped due to view being null")
			else
				onClick(comic)
		}

		holder.comicView.setOnLongClickListener { view ->
			if (view == null) {
				Timber.w("onClick listener invocation dropped due to view being null")
				false
			} else {
				onLongClick(comic)
				true
			}
		}


		Glide.with(holder.context)
				.load(comic.imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.SOURCE)
				.crossFade()
				.into(holder.comicView)
	}

	override fun unbind(holder: Holder) {
		super.unbind(holder)
		Glide.clear(holder.comicView)

		holder.comicView.setOnClickListener(null)
		holder.comicView.setOnLongClickListener(null)
	}

	override fun createNewHolder() = Holder()

	class Holder : BaseEpoxyHolder() {
		val titleView: TextView by bindView(R.id.title)
		val comicView: ImageView by bindView(R.id.comic)
		val cardView: CardView by bindView(R.id.card_view)

		override fun onViewCreated(context: Context, view: View) {
			val screenWidth = getScreenWidth(context)

			if(context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
			   		|| screenWidth  > 720.dp) {

				// can't set padding on the CardView directly
				cardView.apply {
					marginStart = screenWidth / 6
					marginEnd = screenWidth / 6
				}
			}
		}
	}
}