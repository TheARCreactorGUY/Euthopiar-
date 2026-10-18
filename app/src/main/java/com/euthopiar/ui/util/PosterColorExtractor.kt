package com.euthopiar.ui.util

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import coil3.transform.Transformation
import com.euthopiar.ui.theme.SurfaceHigh

data class PosterGlowColors(
    val color1: Color,
    val color2: Color,
    val color3: Color
)

object PosterColorCache : ComponentCallbacks2 {
    private val memoryCache = LruCache<String, PosterGlowColors>(256)
    @Volatile
    private var isRegistered = false

    fun init(context: Context) {
        if (!isRegistered) {
            synchronized(this) {
                if (!isRegistered) {
                    try {
                        context.applicationContext.registerComponentCallbacks(this)
                        isRegistered = true
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun get(key: String?): PosterGlowColors? {
        if (key.isNullOrBlank()) return null
        synchronized(memoryCache) {
            return memoryCache.get(key)
        }
    }

    fun put(key: String?, colors: PosterGlowColors) {
        if (key.isNullOrBlank()) return
        synchronized(memoryCache) {
            memoryCache.put(key, colors)
        }
    }

    fun clear() {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            clear()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {
        clear()
    }

    /**
     * Initial placeholder before extraction completes.
     * Uses transparent so no incorrect or random palettes are ever flashed.
     */
    fun getOrInitial(posterUrl: String?): PosterGlowColors {
        if (!posterUrl.isNullOrBlank()) {
            get(posterUrl)?.let { return it }
        }
        val transparent = Color.Transparent
        return PosterGlowColors(transparent, transparent, transparent)
    }
}

object PosterColorExtractor {
    /**
     * Safe, lightweight color extraction request requesting a tiny 64x64 software bitmap
     * with allowHardware(false) and CachePolicy.DISABLED.
     */
    suspend fun extractPosterColors(context: Context, posterUrl: String?): PosterGlowColors {
        if (posterUrl.isNullOrBlank()) return PosterGlowColors(SurfaceHigh, SurfaceHigh, SurfaceHigh)
        PosterColorCache.init(context)
        PosterColorCache.get(posterUrl)?.let { return it }

        return try {
            val request = ImageRequest.Builder(context)
                .data(posterUrl)
                .size(64, 64)
                .allowHardware(false)
                .memoryCachePolicy(coil3.request.CachePolicy.DISABLED)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.image.toBitmap()
                val colors = extractArtworkColors(bitmap)
                PosterColorCache.put(posterUrl, colors)
                colors
            } else {
                PosterGlowColors(SurfaceHigh, SurfaceHigh, SurfaceHigh)
            }
        } catch (_: Exception) {
            PosterGlowColors(SurfaceHigh, SurfaceHigh, SurfaceHigh)
        }
    }

    fun extractArtworkColors(bitmap: Bitmap): PosterGlowColors {
        val safeBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            try {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } catch (_: Exception) {
                bitmap
            }
        } else {
            bitmap
        }

        return try {
            val w = safeBitmap.width
            val h = safeBitmap.height

            // Top 45% (Title, sky, upper lighting)
            val c1 = sampleRegionArtworkColor(safeBitmap, 0, 0, w, (h * 0.45f).toInt())

            // Middle-to-Bottom Right (Main subject highlights, side rim lighting)
            val c2 = sampleRegionArtworkColor(safeBitmap, (w * 0.30f).toInt(), (h * 0.30f).toInt(), w, (h * 0.85f).toInt())

            // Bottom 45% (Base ground, neon reflections, bottom artwork)
            val c3 = sampleRegionArtworkColor(safeBitmap, 0, (h * 0.55f).toInt(), w, h)

            PosterGlowColors(c1, c2, c3)
        } finally {
            if (safeBitmap !== bitmap) {
                safeBitmap.recycle()
            }
        }
    }

    private fun sampleRegionArtworkColor(
        bitmap: Bitmap,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int
    ): Color {
        val safeStartX = startX.coerceIn(0, bitmap.width - 1)
        val safeStartY = startY.coerceIn(0, bitmap.height - 1)
        val safeEndX = endX.coerceIn(safeStartX + 1, bitmap.width)
        val safeEndY = endY.coerceIn(safeStartY + 1, bitmap.height)

        val stepX = ((safeEndX - safeStartX) / 8).coerceAtLeast(1)
        val stepY = ((safeEndY - safeStartY) / 8).coerceAtLeast(1)

        var bestColor: Color? = null
        var maxScore = -1f

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0

        val hsv = FloatArray(3)

        for (y in safeStartY until safeEndY step stepY) {
            for (x in safeStartX until safeEndX step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val a = (pixel ushr 24) and 0xFF
                if (a < 50) continue

                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF

                sumR += r
                sumG += g
                sumB += b
                count++

                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val value = hsv[2]

                // Score pixels that have genuine artwork color and are not pitch black
                if (value > 0.15f && sat > 0.15f) {
                    val score = sat * 3.0f + value
                    if (score > maxScore) {
                        maxScore = score
                        bestColor = Color(r, g, b)
                    }
                }
            }
        }

        // If we found a vibrant colored pixel in the artwork, tune its glow luminosity
        if (bestColor != null && maxScore > 0.6f) {
            return tuneGlowColor(bestColor)
        }

        // Otherwise use the average color of the artwork region, preserving its exact hue/tone
        if (count > 0) {
            val avgR = (sumR / count).toInt().coerceIn(0, 255)
            val avgG = (sumG / count).toInt().coerceIn(0, 255)
            val avgB = (sumB / count).toInt().coerceIn(0, 255)
            return tuneGlowColor(Color(avgR, avgG, avgB))
        }

        return SurfaceHigh
    }

    private fun tuneGlowColor(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            hsv
        )
        // Enhance vibrancy while preserving the true artwork hue
        hsv[1] = (hsv[1] * 1.35f).coerceIn(0.40f, 1.0f)
        hsv[2] = hsv[2].coerceIn(0.70f, 0.95f)
        val rgb = android.graphics.Color.HSVToColor(hsv)
        return Color(rgb)
    }
}

/**
 * Coil 3 Transformation that extracts authentic dominant and vibrant colors
 * directly from the actual poster artwork Bitmap with zero hardcoded random palettes.
 */
class PosterColorExtractionTransformation(
    private val posterUrl: String?,
    private val onColorsExtracted: (PosterGlowColors) -> Unit
) : Transformation() {

    override val cacheKey: String = "ArtworkColorExtraction_${posterUrl ?: ""}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (posterUrl.isNullOrBlank() || input.width <= 0 || input.height <= 0) {
            return input
        }

        PosterColorCache.get(posterUrl)?.let {
            onColorsExtracted(it)
            return input
        }

        val colors = PosterColorExtractor.extractArtworkColors(input)
        PosterColorCache.put(posterUrl, colors)
        onColorsExtracted(colors)

        return input
    }
}
