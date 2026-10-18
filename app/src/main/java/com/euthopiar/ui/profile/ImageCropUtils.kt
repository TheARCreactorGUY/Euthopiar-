package com.euthopiar.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import kotlin.math.min

/**
 * Utility functions for loading, normalizing, and circular-cropping profile avatars.
 */
object ImageCropUtils {

    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        var tempFile: File? = null
        return try {
            val contentResolver = context.contentResolver
            tempFile = File.createTempFile("crop_source_${System.currentTimeMillis()}_", ".tmp", context.cacheDir)
            val bytesCopied = contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: 0L

            if (bytesCopied <= 0L || !tempFile.exists() || tempFile.length() <= 0L) {
                return null
            }

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tempFile.absolutePath, boundsOptions)

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            while ((origWidth / sampleSize) > maxDimension || (origHeight / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOptions) ?: return null

            val rotationDegrees = getExifRotationFromFile(tempFile)
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } else {
                decoded
            }
        } catch (e: Throwable) {
            android.util.Log.e("ImageCropUtils", "loadBitmapFromUri failed", e)
            null
        } finally {
            try {
                tempFile?.delete()
            } catch (_: Exception) {}
        }
    }

    private fun getExifRotationFromFile(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    fun cropCircularBitmap(
        sourceBitmap: Bitmap,
        cropCenterX: Float,
        cropCenterY: Float,
        cropRadius: Float,
        targetOutputSize: Int = 512,
    ): Bitmap {
        val safeRadius = kotlin.math.max(10f, min(cropRadius, min(sourceBitmap.width, sourceBitmap.height) / 2f))
        val safeCenterX = cropCenterX.coerceIn(safeRadius, sourceBitmap.width - safeRadius)
        val safeCenterY = cropCenterY.coerceIn(safeRadius, sourceBitmap.height - safeRadius)

        val srcLeft = (safeCenterX - safeRadius).toInt().coerceAtLeast(0)
        val srcTop = (safeCenterY - safeRadius).toInt().coerceAtLeast(0)
        val srcRight = (safeCenterX + safeRadius).toInt().coerceAtMost(sourceBitmap.width)
        val srcBottom = (safeCenterY + safeRadius).toInt().coerceAtMost(sourceBitmap.height)

        val srcRect = Rect(srcLeft, srcTop, srcRight, srcBottom)
        val dstRect = Rect(0, 0, targetOutputSize, targetOutputSize)

        val output = Bitmap.createBitmap(targetOutputSize, targetOutputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val half = targetOutputSize / 2f

        canvas.drawCircle(half, half, half, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(sourceBitmap, srcRect, dstRect, paint)

        return output
    }
}
