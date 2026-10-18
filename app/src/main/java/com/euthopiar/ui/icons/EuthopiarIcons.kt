package com.euthopiar.ui.icons

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Custom navbar icons for Euthopiar. 24x24 viewport, rounded 1.75 stroke for the
 * unselected (outlined) state, solid/bolder for the selected (filled) state.
 *
 * Colors are placeholders (black); the real color comes from Icon(tint = ...).
 */
data class NavIconSet(val outlined: ImageVector, val filled: ImageVector) {
    fun forState(selected: Boolean) = if (selected) filled else outlined
}

object EuthopiarIcons {

    val Home: NavIconSet by lazy { NavIconSet(homeOutlined, homeFilled) }
    val Search: NavIconSet by lazy { NavIconSet(searchOutlined, searchFilled) }
    val Library: NavIconSet by lazy { NavIconSet(libraryOutlined, libraryFilled) }
    val Profile: NavIconSet by lazy { NavIconSet(profileOutlined, profileFilled) }

    // ---------------------------------------------------------------- Home

    private fun PathBuilder.housePath() {
        moveTo(4f, 10.5f)
        lineTo(12f, 4f)
        lineTo(20f, 10.5f)
        verticalLineTo(19f)
        arcTo(1.5f, 1.5f, 0f, false, true, 18.5f, 20.5f)
        horizontalLineTo(15f)
        verticalLineTo(14.5f)
        arcTo(1f, 1f, 0f, false, false, 14f, 13.5f)
        horizontalLineToRelative(-4f)
        arcTo(1f, 1f, 0f, false, false, 9f, 14.5f)
        verticalLineTo(20.5f)
        horizontalLineTo(5.5f)
        arcTo(1.5f, 1.5f, 0f, false, true, 4f, 19f)
        close()
    }

    private val homeOutlined by lazy {
        icon("HomeOutlined") { stroked { housePath() } }
    }

    private val homeFilled by lazy {
        icon("HomeFilled") { solid { housePath() } }
    }

    // -------------------------------------------------------------- Search

    private fun PathBuilder.lensPath() = circle(10.5f, 10.5f, 6.5f)

    private val searchOutlined by lazy {
        icon("SearchOutlined") {
            stroked {
                lensPath()
                moveTo(15.4f, 15.4f)
                lineTo(20f, 20f)
            }
        }
    }

    private val searchFilled by lazy {
        icon("SearchFilled") {
            // soft glass fill inside the lens
            path(fill = SolidColor(Color.Black), fillAlpha = 0.25f) { lensPath() }
            stroked(width = 2.4f) {
                lensPath()
                moveTo(15.4f, 15.4f)
                lineTo(20f, 20f)
            }
        }
    }

    // ------------------------------------------------------------- Library
    // Shelf lines on top + a rounded "media card" with a play triangle.

    private fun PathBuilder.cardPath() {
        moveTo(5.5f, 10.5f)
        horizontalLineTo(18.5f)
        arcTo(2f, 2f, 0f, false, true, 20.5f, 12.5f)
        verticalLineTo(18.5f)
        arcTo(2f, 2f, 0f, false, true, 18.5f, 20.5f)
        horizontalLineTo(5.5f)
        arcTo(2f, 2f, 0f, false, true, 3.5f, 18.5f)
        verticalLineTo(12.5f)
        arcTo(2f, 2f, 0f, false, true, 5.5f, 10.5f)
        close()
    }

    private fun PathBuilder.playPath() {
        moveTo(10.5f, 12.9f)
        lineTo(15.2f, 15.5f)
        lineTo(10.5f, 18.1f)
        close()
    }

    private fun PathBuilder.shelfLines() {
        moveTo(7f, 4.5f)
        horizontalLineToRelative(10f)
        moveTo(5.5f, 7.5f)
        horizontalLineToRelative(13f)
    }

    private val libraryOutlined by lazy {
        icon("LibraryOutlined") {
            stroked {
                shelfLines()
                cardPath()
                playPath()
            }
        }
    }

    private val libraryFilled by lazy {
        icon("LibraryFilled") {
            stroked { shelfLines() }
            // card with the play triangle punched out (even-odd)
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                cardPath()
                playPath()
            }
        }
    }

    // ------------------------------------------------------------- Profile

    private fun PathBuilder.headPath() = circle(12f, 8.5f, 3.5f)

    private fun PathBuilder.shouldersPath() {
        moveTo(5f, 20f)
        curveToRelative(0f, -3.6f, 3.1f, -6f, 7f, -6f)
        reflectiveCurveToRelative(7f, 2.4f, 7f, 6f)
    }

    private val profileOutlined by lazy {
        icon("ProfileOutlined") {
            stroked {
                headPath()
                shouldersPath()
            }
        }
    }

    private val profileFilled by lazy {
        icon("ProfileFilled") {
            solid {
                headPath()
                shouldersPath()
                close()
            }
        }
    }

    // ------------------------------------------------------------- helpers

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    private fun ImageVector.Builder.stroked(
        width: Float = 2.2f,
        block: PathBuilder.() -> Unit,
    ) = path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )

    /** Filled shape with a bold rounded stroke so corners stay soft. */
    private fun ImageVector.Builder.solid(block: PathBuilder.() -> Unit) = path(
        fill = SolidColor(Color.Black),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcToRelative(r, r, 0f, false, true, 2 * r, 0f)
        arcToRelative(r, r, 0f, false, true, -2 * r, 0f)
        close()
    }
}

// ------------------------------------------------------------------ Preview

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun EuthopiarIconsPreview() {
    val sets = listOf(
        EuthopiarIcons.Home,
        EuthopiarIcons.Search,
        EuthopiarIcons.Library,
        EuthopiarIcons.Profile,
    )
    Row {
        sets.forEach { Icon(it.outlined, null, Modifier.padding(12.dp), tint = Color(0xFFB8BCC6)) }
    }
    Row {
        sets.forEach { Icon(it.filled, null, Modifier.padding(12.dp), tint = Color.White) }
    }
}
