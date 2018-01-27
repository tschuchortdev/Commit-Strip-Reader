package com.tschuchort.readerforcommitstrip

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import io.reactivex.Single
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class LocalStorage
    @Inject constructor(@AppContext private val context: Context) {

    /**
     * saves the bitmap on the device at the specified absolute path
     *
     * @param image the bitmap
     * @param absolutePath absolute path to the save location, including file name (but not file
     * type suffix!)
     * @param quality compression quality
     * @param format saved file format (jpg/png/webp)
     */
    fun saveImage(image: Bitmap, absolutePath: String, quality: Float = 1f,
                  format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): Single<File>
            = Single.fromCallable {

        require(absolutePath.isNotEmpty())
        require(quality in 0..1)

        val fileSuffix = when(format) {
            Bitmap.CompressFormat.JPEG -> ".jpg"
            Bitmap.CompressFormat.PNG -> ".png"
            Bitmap.CompressFormat.WEBP -> ".webp"
        }

        val pathFileSuffix = Regex("\\.(jpe?g|png|gif|webp|bmp|tiff?|raw)")
                .find(absolutePath)?.value

        if(pathFileSuffix == fileSuffix) {
            Timber.w("absolutePath for bitmap to be saved contains a file suffix " +
                             "that is the same as the image format. Remove the suffix from the path")
        }
        else if(pathFileSuffix != null) {
            throw IllegalArgumentException("absolutePath contains file suffix that doesn't align " +
                                                   "with the specified image format")
        }

        // may throw IOException, but no idea how to sensibly handle this here
        val imageFile = File(absolutePath + if(pathFileSuffix == null) fileSuffix else "")
        imageFile.createNewFile()
        val ostream = FileOutputStream(imageFile)
        image.compress(format, (quality * 100).roundToInt(), ostream)
        ostream.close()

        return@fromCallable imageFile
    }

    /**
     * saves the bitmap in the device storage directory, in the specified folder with the given name
     *
     * @param image the bitmap
     * @param name name of the saved file (excluding the file type suffix!)
     * @param folderName folder in the device storage directory where the bitmap should be saved
     * @param quality compression quality
     * @param format saved file format (jpg/png/webp)
     */
    fun saveImage(image: Bitmap, name: String, folderName: String, quality: Float = 1f,
                  format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): Single<File> {

        require(name.isNotEmpty())
        require(folderName.isNotEmpty())

        val storageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                folderName)

        return saveImage(image, storageDir.absolutePath + "/" + name, quality, format)
    }

    /**
     * saves the bitmap in the device storage directory in the specified folder with the given name
     * and makes it available to the gallery
     *
     * @param image the bitmap
     * @param name name of the saved file (excluding the file type suffix!)
     * @param folderName folder in the device storage directory where the bitmap should be saved
     * @param quality compression quality
     * @param format saved file format (jpg/png/webp)
     */
    fun saveImageToGallery(image: Bitmap, name: String,
                           folderName: String, quality: Float = 1f,
                           format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG)
            = saveImage(image, name, folderName, quality, format)
            .doOnSuccess { imageFile -> makeImageAvailableToGallery(imageFile.absolutePath) }
            .toCompletable()

    /**
     * sends a broadcast so other apps like the gallery, so they can find the saved image
     *
     * @param absolutePath absolute path to the image file
     */
    fun makeImageAvailableToGallery(absolutePath: String) {
        require(absolutePath.isNotEmpty())

        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val imageFile = File(absolutePath)
        val contentUri = Uri.fromFile(imageFile)
        mediaScanIntent.data = contentUri
        context.sendBroadcast(mediaScanIntent)
    }
}