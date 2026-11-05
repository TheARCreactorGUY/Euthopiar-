package com.euthopiar.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Lucide Design System Iconography for Euthopiar.
 * Standard 2.0px stroke width with round line caps & joins.
 * Perfectly sized and optically centered on a 24x24 grid.
 * Filled action icons (Play, Pause, FastForward, BookmarkFilled) have solid interior fill
 * so tinting (e.g. Color.Black, Color.White) fills completely solid.
 */
object LucideIcons {

    private fun strokedIcon(name: String, vararg paths: String, strokeWidth: Float = 2.0f): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        for (p in paths) {
            builder.addPath(
                pathData = addPathNodes(p),
                stroke = SolidColor(Color.White),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    private fun filledIcon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        for (p in paths) {
            builder.addPath(
                pathData = addPathNodes(p),
                fill = SolidColor(Color.White)
            )
        }
        return builder.build()
    }

    private fun compositeIcon(
        name: String,
        strokedPaths: List<String> = emptyList(),
        filledPaths: List<String> = emptyList(),
        strokeWidth: Float = 2.0f
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        for (p in strokedPaths) {
            builder.addPath(
                pathData = addPathNodes(p),
                stroke = SolidColor(Color.White),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        for (p in filledPaths) {
            builder.addPath(
                pathData = addPathNodes(p),
                fill = SolidColor(Color.White)
            )
        }
        return builder.build()
    }

    /**
     * Solid filled Play arrow with rounded corners, sized optically (10x12) to match
     * standard play buttons perfectly without oversized bloating.
     */
    val Play: ImageVector by lazy {
        filledIcon(
            "Play",
            "M 8.5 7.2 a 1.4 1.4 0 0 1 2.1 -1.2 l 7.4 4.3 a 1.4 1.4 0 0 1 0 2.4 l -7.4 4.3 a 1.4 1.4 0 0 1 -2.1 -1.2 z"
        )
    }

    val PlayFilled: ImageVector by lazy { Play }

    /**
     * Solid filled Pause bars (two vertical rounded pills) matching Play's optical height.
     */
    val Pause: ImageVector by lazy {
        filledIcon(
            "Pause",
            "M 8.0 6.0 a 1.5 1.5 0 0 1 1.5 1.5 v 9.0 a 1.5 1.5 0 0 1 -3.0 0 v -9.0 a 1.5 1.5 0 0 1 1.5 -1.5 z",
            "M 16.0 6.0 a 1.5 1.5 0 0 1 1.5 1.5 v 9.0 a 1.5 1.5 0 0 1 -3.0 0 v -9.0 a 1.5 1.5 0 0 1 1.5 -1.5 z"
        )
    }

    /**
     * Solid filled FastForward (two rounded triangles).
     */
    val FastForward: ImageVector by lazy {
        filledIcon(
            "FastForward",
            "M 5.5 7.8 a 1.2 1.2 0 0 1 1.8 -1.0 l 5.2 3.8 a 1.2 1.2 0 0 1 0 2.0 l -5.2 3.8 a 1.2 1.2 0 0 1 -1.8 -1.0 z",
            "M 12.5 7.8 a 1.2 1.2 0 0 1 1.8 -1.0 l 5.2 3.8 a 1.2 1.2 0 0 1 0 2.0 l -5.2 3.8 a 1.2 1.2 0 0 1 -1.8 -1.0 z"
        )
    }

    val VolumeUp: ImageVector by lazy {
        strokedIcon(
            "VolumeUp",
            "M 11 5.5 a 0.8 0.8 0 0 0 -1.3 -0.6 l -3.2 2.6 H 4.5 a 1 1 0 0 0 -1 1 v 7 a 1 1 0 0 0 1 1 h 2.0 l 3.2 2.6 a 0.8 0.8 0 0 0 1.3 -0.6 z",
            "M 14.5 9.5 a 3.5 3.5 0 0 1 0 5",
            "M 17.5 6.5 a 7.5 7.5 0 0 1 0 11"
        )
    }

    val VolumeDown: ImageVector by lazy {
        strokedIcon(
            "VolumeDown",
            "M 11 5.5 a 0.8 0.8 0 0 0 -1.3 -0.6 l -3.2 2.6 H 4.5 a 1 1 0 0 0 -1 1 v 7 a 1 1 0 0 0 1 1 h 2.0 l 3.2 2.6 a 0.8 0.8 0 0 0 1.3 -0.6 z",
            "M 14.5 9.5 a 3.5 3.5 0 0 1 0 5"
        )
    }

    val VolumeMute: ImageVector by lazy {
        strokedIcon(
            "VolumeMute",
            "M 11 5.5 a 0.8 0.8 0 0 0 -1.3 -0.6 l -3.2 2.6 H 4.5 a 1 1 0 0 0 -1 1 v 7 a 1 1 0 0 0 1 1 h 2.0 l 3.2 2.6 a 0.8 0.8 0 0 0 1.3 -0.6 z",
            "m 15 9.5 l 4.5 4.5",
            "m 19.5 9.5 l -4.5 4.5"
        )
    }

    val Fullscreen: ImageVector by lazy {
        strokedIcon(
            "Fullscreen",
            "M 8 4 H 5 a 1 1 0 0 0 -1 1 v 3",
            "M 16 4 h 3 a 1 1 0 0 1 1 1 v 3",
            "M 4 16 v 3 a 1 1 0 0 0 1 1 h 3",
            "M 20 16 v 3 a 1 1 0 0 1 -1 1 h -3"
        )
    }

    val FullscreenExit: ImageVector by lazy {
        strokedIcon(
            "FullscreenExit",
            "M 4 8 h 3 a 1 1 0 0 0 1 -1 V 4",
            "M 20 8 h -3 a 1 1 0 0 1 -1 -1 V 4",
            "M 4 16 h 3 a 1 1 0 0 1 1 1 v 3",
            "M 20 16 h -3 a 1 1 0 0 0 -1 1 v 3"
        )
    }

    val Bookmark: ImageVector by lazy {
        strokedIcon(
            "Bookmark",
            "M 7 4 a 2 2 0 0 1 2 -2 h 6 a 2 2 0 0 1 2 2 v 16.5 a 0.8 0.8 0 0 1 -1.2 0.7 L 12 18.5 l -3.8 2.7 A 0.8 0.8 0 0 1 7 20.5 z"
        )
    }

    val BookmarkFilled: ImageVector by lazy {
        filledIcon(
            "BookmarkFilled",
            "M 7 4 a 2 2 0 0 1 2 -2 h 6 a 2 2 0 0 1 2 2 v 16.5 a 0.8 0.8 0 0 1 -1.2 0.7 L 12 18.5 l -3.8 2.7 A 0.8 0.8 0 0 1 7 20.5 z"
        )
    }

    val BookmarkAdd: ImageVector by lazy {
        strokedIcon(
            "BookmarkAdd",
            "M 7 4 a 2 2 0 0 1 2 -2 h 6 a 2 2 0 0 1 2 2 v 16.5 a 0.8 0.8 0 0 1 -1.2 0.7 L 12 18.5 l -3.8 2.7 A 0.8 0.8 0 0 1 7 20.5 z",
            "M 12 7 v 4",
            "M 10 9 h 4"
        )
    }

    val Cast: ImageVector by lazy {
        compositeIcon(
            name = "Cast",
            strokedPaths = listOf(
                "M3,8V6.5A2.5,2.5 0 0 1 5.5,4H18.5A2.5,2.5 0 0 1 21,6.5V17.5A2.5,2.5 0 0 1 18.5,20H14",
                "M3.5,15.5A4.5,4.5 0 0 1 8,20M3.5,11.5A8.5,8.5 0 0 1 12,20"
            ),
            filledPaths = listOf(
                "M2.5,19.7a1.3,1.3 0 1,0 2.6,0a1.3,1.3 0 1,0 -2.6,0z"
            )
        )
    }

    val CastConnected: ImageVector by lazy { Cast }

    val Lock: ImageVector by lazy {
        compositeIcon(
            name = "Lock",
            strokedPaths = listOf("M8,10.5V7.5A4,4 0 0 1 16,7.5V10.5"),
            filledPaths = listOf(
                "M7.5,10.5H16.5a3,3 0 0 1 3,3V18.5a3,3 0 0 1 -3,3H7.5a3,3 0 0 1 -3,-3V13.5a3,3 0 0 1 3,-3zM10.5,15a1.5,1.5 0 1,0 3,0a1.5,1.5 0 1,0 -3,0zM12,16.6H12a0.8,0.8 0 0 1 0.8,0.8V18.4a0.8,0.8 0 0 1 -0.8,0.8H12a0.8,0.8 0 0 1 -0.8,-0.8V17.4a0.8,0.8 0 0 1 0.8,-0.8z"
            )
        )
    }

    val Unlock: ImageVector by lazy {
        compositeIcon(
            name = "Unlock",
            strokedPaths = listOf("M8,10.5V6.5A4,4 0 0 1 16,6.5V7.5"),
            filledPaths = listOf(
                "M7.5,10.5H16.5a3,3 0 0 1 3,3V18.5a3,3 0 0 1 -3,3H7.5a3,3 0 0 1 -3,-3V13.5a3,3 0 0 1 3,-3zM10.5,15a1.5,1.5 0 1,0 3,0a1.5,1.5 0 1,0 -3,0zM12,16.6H12a0.8,0.8 0 0 1 0.8,0.8V18.4a0.8,0.8 0 0 1 -0.8,0.8H12a0.8,0.8 0 0 1 -0.8,-0.8V17.4a0.8,0.8 0 0 1 0.8,-0.8z"
            )
        )
    }

    val Pip: ImageVector by lazy {
        compositeIcon(
            name = "Pip",
            strokedPaths = listOf("M6,4.5H18a3.5,3.5 0 0 1 3.5,3.5V16a3.5,3.5 0 0 1 -3.5,3.5H6a3.5,3.5 0 0 1 -3.5,-3.5V8a3.5,3.5 0 0 1 3.5,-3.5z"),
            filledPaths = listOf("M13.5,11.5H18a1.5,1.5 0 0 1 1.5,1.5V15.5a1.5,1.5 0 0 1 -1.5,1.5H13.5a1.5,1.5 0 0 1 -1.5,-1.5V13a1.5,1.5 0 0 1 1.5,-1.5z")
        )
    }

    val Speed: ImageVector by lazy {
        compositeIcon(
            name = "Speed",
            strokedPaths = listOf(
                "M5.64,19.36A9,9 0 1 1 18.36,19.36",
                "M12,6.3V7.7M4.6,13H6M18,13H19.4M6.8,7.8L7.8,8.8",
                "M12,13L16.4,8.8"
            ),
            filledPaths = listOf(
                "M10.2,13a1.8,1.8 0 1,0 3.6,0a1.8,1.8 0 1,0 -3.6,0z"
            )
        )
    }

    val Tune: ImageVector by lazy {
        strokedIcon(
            "Tune",
            "M3,6H7M11,6H21M3,12H13M17,12H21M3,18H8M12,18H21",
            "M9,4A2,2 0,1 1,9 8A2,2 0,1 1,9 4ZM15,10A2,2 0,1 1,15 14A2,2 0,1 1,15 10ZM10,16A2,2 0,1 1,10 20A2,2 0,1 1,10 16Z"
        )
    }

    val AspectRatio: ImageVector by lazy {
        strokedIcon(
            "AspectRatio",
            "M6,5H18a3.5,3.5 0 0 1 3.5,3.5V15.5a3.5,3.5 0 0 1 -3.5,3.5H6a3.5,3.5 0 0 1 -3.5,-3.5V8.5a3.5,3.5 0 0 1 3.5,-3.5z",
            "M7,10.5V8H9.5M17,13.5V16H14.5"
        )
    }

    val Episodes: ImageVector by lazy {
        compositeIcon(
            name = "Episodes",
            strokedPaths = listOf(
                "M5.5,9H18.5a2.5,2.5 0 0 1 2.5,2.5V18.5a2.5,2.5 0 0 1 -2.5,2.5H5.5a2.5,2.5 0 0 1 -2.5,-2.5V11.5a2.5,2.5 0 0 1 2.5,-2.5z",
                "M3,9L2.5,5L20.5,2.6L21,9",
                "M7.5,9L10,4.1M13,9L15.5,3.4"
            ),
            filledPaths = listOf(
                "M10,12.5L15,15L10,17.5Z"
            )
        )
    }

    val NightMode: ImageVector by lazy {
        strokedIcon(
            "NightMode",
            "M14.5,4.05A8.5,8.5 0,1 0,14.5 19.95A8.5,8.5 0,0 1,14.5 4.05Z",
            "M13,12C14.3,9.25 15.7,9.25 17,12C18.3,14.75 19.7,14.75 21,12"
        )
    }

    val External: ImageVector by lazy {
        strokedIcon(
            "External",
            "M18,13.5V18A2.5,2.5 0 0 1 15.5,20.5H6A2.5,2.5 0 0 1 3.5,18V8.5A2.5,2.5 0 0 1 6,6H10.5",
            "M14,3.5H20.5V10M20.5,3.5L11,13"
        )
    }
}
