package com.tschuchort.readerforcommitstrip

import android.content.Context
import android.view.View
import androidx.annotation.CallSuper
import com.airbnb.epoxy.EpoxyHolder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


/**
 * Creating a base holder class allows us to leverage ButterKnife's view binding for all subclasses.
 * This makes subclasses much cleaner, and is a highly recommended pattern.
 */
abstract class BaseEpoxyHolder : EpoxyHolder() {
	lateinit var context: Context
	private var itemView: View? = null

	@CallSuper
	final override fun bindView(itemView: View) {
		context = itemView.context
		this.itemView = itemView

		onViewCreated(context, itemView)
	}

	open fun onViewCreated(context: Context, view: View) {}

	fun <V : View> bindView(id: Int): ReadOnlyProperty<BaseEpoxyHolder, V>
		= Lazy { holder: BaseEpoxyHolder, _ -> holder.itemView!!.findViewById<V>(id) }
}

// Like Kotlin's lazy delegate but the initializer gets the target and metadata passed to it
private class Lazy<in T, out V>(private val initializer: (T, KProperty<*>) -> V) : ReadOnlyProperty<T, V> {
	private object EMPTY
	private var value: Any? = EMPTY

	override fun getValue(thisRef: T, property: KProperty<*>): V {
		if (value == EMPTY) {
			value = initializer(thisRef, property)
		}
		@Suppress("UNCHECKED_CAST")
		return value as V
	}
}