package com.tschuchort.readerforcommitstrip

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.pm.LabeledIntent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.bumptech.glide.GenericRequestBuilder
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import de.mrapp.android.bottomsheet.BottomSheet
import io.apptik.multiview.layoutmanagers.ViewPagerLayoutManager
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.lang.Exception
import java.lang.UnsupportedOperationException
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> Iterable<T>.dropUntilAfter(predicate: (T) -> Boolean) = dropWhile { !predicate(it) }.drop(1)

fun <R> List<R>.addIf(condition: Boolean, element: R) = if(condition) this + element else this

fun CharSequence.substringStartsWith(prefix: String, startIndex: Int = 0) = substring(indexOf(prefix, startIndex))

fun CharSequence.indexOf(regex: Regex, startIndex: Int = 0) = regex.find(this, startIndex)

fun <T> nonNull(value: T) = value != null
fun <T> isNull(value: T) = value == null

val Int.dp: Int
	get() = dpToPx(this)

fun dpToPx(dp: Int)
		= Math.round(dp * (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun dpToPx(dp: Float)
		= Math.round(dp * (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun pxToDp(px: Int)
		= Math.round(px / (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun getScreenSize(context: Context): Point {
	val size = Point()
	(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(size)
	return size
}

fun getScreenWidth(context: Context) = getScreenSize(context).x

fun getScreenHeight(context: Context) = getScreenSize(context).y

fun View.setPaddingLeft(left: Int) = setPadding(left, paddingTop, paddingRight, paddingBottom)
fun View.setPaddingStart(start: Int) = setPaddingRelative(start, paddingTop, paddingEnd, paddingBottom)
fun View.setPaddingRight(right: Int) = setPadding(paddingLeft, paddingTop, right, paddingBottom)
fun View.setPaddingEnd(end: Int) = setPaddingRelative(paddingStart, paddingTop, end, paddingBottom)
fun View.setPaddingTop(top: Int) = setPaddingRelative(paddingStart, top, paddingEnd, paddingBottom)
fun View.setPaddingBottom(bottom: Int) = setPaddingRelative(paddingStart, paddingTop, paddingEnd, bottom)
fun View.setPaddingVertical(vertical: Int) = setPaddingRelative(paddingStart, vertical, paddingEnd, vertical)
fun View.setPaddingHorizontal(horizontal: Int) = setPaddingRelative(horizontal, paddingTop, horizontal, paddingBottom)

var View.marginLeft: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				leftMargin = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginStart: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				marginStart = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginTop: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				topMargin = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginRight: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.rightMargin ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				rightMargin = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginEnd: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.marginEnd ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				marginEnd = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

var View.marginBottom: Int
	get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
	set(value) {
		try {
			layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
				bottomMargin = value
			}
		}
		catch(e: ClassCastException) {
			throw UnsupportedOperationException("can not set margin because view does not have MarginLayoutParams")
		}
	}

fun RecyclerView.LayoutManager.findFirstVisibleItemPosition() = when(this) {
	is LinearLayoutManager                    -> findFirstVisibleItemPosition()
	is ViewPagerLayoutManager     -> findFirstVisibleItemPosition()
	is GridLayoutManager          -> findFirstVisibleItemPosition()
	is StaggeredGridLayoutManager -> findFirstVisibleItemPositions(null).first()
	else                          -> throw IllegalArgumentException(
			"can't get first visible item position for unknown recycler view layout manager")
}

fun RecyclerView.LayoutManager.findLastVisibleItemPosition() = when(this) {
	is LinearLayoutManager                    -> findLastVisibleItemPosition()
	is ViewPagerLayoutManager     -> findLastVisibleItemPosition()
	is GridLayoutManager          -> findLastVisibleItemPosition()
	is StaggeredGridLayoutManager -> findLastVisibleItemPositions(null).first()
	else                          -> throw IllegalArgumentException(
			"can't get last visible item position for unknown recycler view layout manager")
}

inline fun <reified LM : RecyclerView.LayoutManager> RecyclerView.swapLayoutManager(layoutManager: LM) {
	val oldLayoutManager = this.layoutManager

	// this check is important because
	// the RV scrolls back to the top every time layout manager is changed
	if(oldLayoutManager is LM && oldLayoutManager::class == layoutManager::class)
		return

	val firstVisibleItemPos =
			try { oldLayoutManager.findFirstVisibleItemPosition() }
			catch (e: IllegalArgumentException) { RecyclerView.NO_POSITION }

	this.layoutManager = layoutManager

	if(firstVisibleItemPos != 0 && firstVisibleItemPos != RecyclerView.NO_POSITION) {
		scrollToPosition(firstVisibleItemPos)
	}
}

fun Activity.shareText(text: String, callToAction: String? = null) {
	val sendIntent = Intent().apply {
		action = Intent.ACTION_SEND
		type = "text/plain"
		putExtra(Intent.EXTRA_TEXT, text)
	}

	shareIntent(this, sendIntent, callToAction)
}

@SuppressLint("SetWorldReadable")
fun Activity.shareImage(image: Bitmap, text: String? = null, callToAction: String? = null) {
	val file = File(
			applicationContext.cacheDir,
			"shared_image${Random().nextInt()}.png")

    val fOut = FileOutputStream(file)
    image.compress(Bitmap.CompressFormat.PNG, 100, fOut)
    fOut.flush()
    fOut.close()

    file.setReadable(true, false)

	val sendIntent = Intent().apply {
		action = Intent.ACTION_SEND
		type = "image/*"
		putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))

		if(text != null)
			putExtra(Intent.EXTRA_TEXT, text)
	}

	shareIntent(this, sendIntent, callToAction)
}

fun shareIntent(activity: Activity, intent: Intent, callToAction: String? = null) {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		activity.startActivity(
				if(callToAction != null)
					Intent.createChooser(intent, callToAction)
				else
					intent)
	}
	else {
		BottomSheet.Builder(activity)
				.setTitle(callToAction)
				.setIntent(activity, intent)
				.show()
	}
}

fun saveImageToGallery(context: Context, image: Bitmap, name: String,
					   folderName: String, quality: Int = 100) {
	val storageDir = File(
    	Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
    	folderName)

	// may throw IOException, but no idea how to sensibly handle this here
	val imageFile = File(storageDir.absolutePath + name)
	imageFile.createNewFile()
	val ostream = FileOutputStream(imageFile)
	image.compress(Bitmap.CompressFormat.JPEG, quality, ostream)
	ostream.close()

	makeImageAvailableToGallery(context, imageFile.absolutePath)
}

/**
 * sends a broadcast so other apps like the gallery can find the saved image
 */
fun makeImageAvailableToGallery(context: Context, savedImagePath: String) {
	val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
	val imageFile = File(savedImagePath)
	val contentUri = Uri.fromFile(imageFile)
	mediaScanIntent.data = contentUri
	context.sendBroadcast(mediaScanIntent)
}

/**
 * gets the native share bottom sheet intent and adds own intent to it
 */
fun getNativeShareIntent(context: Context, sendIntent: Intent): Intent {
  	val pm = context.packageManager

  	val intentList = pm.queryIntentActivities (sendIntent, 0)
			.map {
				val packageName = it.activityInfo.packageName

				val intent = Intent().apply {
					component = ComponentName(packageName, it.activityInfo.name)
					`package` = packageName
					action = ACTION_SEND
					putExtras(sendIntent.extras)
					type = sendIntent.type
				}

				LabeledIntent(intent, packageName, it.loadLabel(pm), it.iconResource)
			}
			.toMutableList()

  	val chooserIntent = Intent.createChooser(intentList.removeAt(0), "Share")

	chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toTypedArray())
  	return chooserIntent
}

inline fun <reified Target : Activity> Context.makeIntent()
		= Intent(this, Target::class.java)

fun <A, B, C, D> GenericRequestBuilder<A, B, C, D>.progressView(progressView: View) =
	listener(object : RequestListener<A,D> {
		override fun onResourceReady(resource: D, model: A, target: Target<D>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
			progressView.visibility = View.GONE
			return false
		}

		override fun onException(e: Exception?, model: A, target: Target<D>?, isFirstResource: Boolean): Boolean {
			progressView.visibility = View.GONE
			return false
		}
	})

enum class MutableLazyThreadSafetyMode { SYNCHRONIZED, NONE }

fun <T> mutableLazy(
			mode: MutableLazyThreadSafetyMode = MutableLazyThreadSafetyMode.SYNCHRONIZED,
			initialize: () -> T)
		: ReadWriteProperty<Any?, T>
		= when(mode) {
			MutableLazyThreadSafetyMode.SYNCHRONIZED -> SynchronizedMutableLazy(initialize)
			MutableLazyThreadSafetyMode.NONE -> UnsafeMutableLazy(initialize)
		}

fun <T> mutableLazy(lock: Any?, initialize: () -> T)
		: ReadWriteProperty<Any?, T>
		= SynchronizedMutableLazy(initialize, lock)

private object UNINITIALIZED_VALUE

private class SynchronizedMutableLazy<T>(private val initialize: () -> T, lock: Any? = null)
	: ReadWriteProperty<Any?, T>, Serializable {

	@Volatile
	private var value: Any? = UNINITIALIZED_VALUE

	private val lock = lock ?: this

	override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		synchronized(lock) {
			if (!isInitialized()) {
				value = initialize()
			}

			@Suppress("UNCHECKED_CAST")
			return value as T
		}
	}

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		synchronized(lock) {
			this.value = value
		}
	}

	override fun toString(): String =
			if (isInitialized()) value.toString()
			else "Lazy value not initialized yet."

	@Suppress("NOTHING_TO_INLINE")
	inline fun isInitialized() = (value !== UNINITIALIZED_VALUE)
}

private class UnsafeMutableLazy<T>(private val initialize: () -> T)
	: ReadWriteProperty<Any?, T>, Serializable {

	@Volatile
	private var value: Any? = UNINITIALIZED_VALUE

	override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		if (!isInitialized()) {
			value = initialize()
		}

		@Suppress("UNCHECKED_CAST")
		return value as T
	}

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		this.value = value
	}

	override fun toString(): String =
			if (isInitialized()) value.toString()
			else "Lazy value not initialized yet."

	@Suppress("NOTHING_TO_INLINE")
	inline fun isInitialized() = (value !== UNINITIALIZED_VALUE)
}

fun <T> Queue<T>.pollIterator() = object : Iterator<T> {
	override fun hasNext() = (peek() != null)

	override fun next() = poll()
}
