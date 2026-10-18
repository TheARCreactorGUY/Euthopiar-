package com.euthopiar.ui.util

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Custom Coil 3 transformation that crops out all fully transparent border margins
 * from a PNG bitmap, allowing studio Clear Logos to align flush with padding boundaries
 * and scale properly to their visual content.
 */
class TrimTransparentTransformation : Transformation() {
    override val cacheKey: String = "TrimTransparentTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height

        if (width <= 0 || height <= 0) return input

        val softwareBitmap = if (input.config == Bitmap.Config.HARDWARE) {
            input.copy(Bitmap.Config.ARGB_8888, false) ?: return input
        } else {
            input
        }

        val pixels = IntArray(width * height)
        softwareBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                val alpha = (pixels[offset + x] ushr 24) and 0xFF
                if (alpha > 15) { // Visible non-transparent pixel
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // If completely transparent or invalid, return original
        if (maxX < minX || maxY < minY) return input

        val trimmedW = maxX - minX + 1
        val trimmedH = maxY - minY + 1

        if (trimmedW == width && trimmedH == height) return input

        return Bitmap.createBitmap(softwareBitmap, minX, minY, trimmedW, trimmedH)
    }
}
