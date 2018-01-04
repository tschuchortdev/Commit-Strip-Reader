package com.tschuchort.readerforcommitstrip

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * property delegate that retains a property of an Activity in an arch ViewModel
 *
 * the creation function is executed lazily or never (if assigned to before the first read)
 */
fun <T> FragmentActivity.retained(initialize: () -> T): ReadWriteProperty<FragmentActivity, T>
        = object : RetainedProperty<FragmentActivity,T>(initialize) {

    // ViewModel needs to be provided lazily instead of ctor arg
    // because it's only available after onCreate
    override val retainer by lazy {
        ViewModelProviders.of(this@retained).get(RetainerViewModel::class.java)
    }
}

/**
 * property delegate that retains a property of a Fragment in an arch ViewModel
 *
 * the creation function is executed lazily or never (if assigned to before the first read)
 */
fun <T> Fragment.retained(initialize: () -> T): ReadWriteProperty<Fragment, T>
        = object : RetainedProperty<Fragment,T>(initialize) {

    // ViewModel needs to be provided lazily instead of ctor arg
    // because it's only available after onCreate
    override val retainer by lazy {
        ViewModelProviders.of(this@retained).get(RetainerViewModel::class.java)
    }
}

private abstract class RetainedProperty<in O,T>(
        private val initialize: () -> T) : ReadWriteProperty<O,T> {

    protected abstract val retainer: RetainerViewModel

    // hold on to the set entry so we don't have to search the set with every access
    // to the delegated property
    private var retainerEntry: Entry<String, T>? = null

    override fun setValue(thisRef: O, property: KProperty<*>, value: T) {
        if(retainerEntry != null) {
            retainerEntry!!.value = value
        }
        else {
            val propName = property.name

            val entry = retainer.properties.firstOrNull { it.key == propName }

            if(entry != null) {
                @Suppress("UNCHECKED_CAST")
                retainerEntry = entry as Entry<String, T>
                retainerEntry!!.value = value
            }
            else {
                retainerEntry = Entry(propName, value)
                retainer.properties.add(retainerEntry!!)
            }
        }
    }

    override fun getValue(thisRef: O, property: KProperty<*>): T {
        if(retainerEntry == null) {
            val propName = property.name

            val entry = retainer.properties.firstOrNull { it.key == propName }

            if(entry != null) {
                @Suppress("UNCHECKED_CAST")
                retainerEntry = entry as Entry<String, T>
            }
            else {
                retainerEntry = Entry(propName, initialize())
                retainer.properties.add(retainerEntry!!)
            }
        }

        return retainerEntry!!.value
    }
}

private data class Entry<out K,V>(val key: K, var value: V) {
    override fun equals(other: Any?) = other is Entry<*,*> && other.key?.equals(key) ?: false
    override fun hashCode() = key?.hashCode() ?: 0
}

private class RetainerViewModel : ViewModel() {
    /* the properties are stored in a set because entries of a HashMap are not
       stable, so we can't hold on to them in order to avoid having to access the HashMap
       with every access to the delegated property
       */
    val properties = mutableSetOf<Entry<String, *>>()
}