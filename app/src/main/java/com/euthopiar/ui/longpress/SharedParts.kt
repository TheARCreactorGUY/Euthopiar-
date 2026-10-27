package com.euthopiar.ui.longpress

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import com.euthopiar.core.model.MediaType
import com.euthopiar.ui.components.AppAsyncImage
import com.euthopiar.ui.theme.Accent
import com.euthopiar.ui.theme.ShapeFull
import com.euthopiar.ui.theme.pressable

import com.euthopiar.ui.theme.GlassDefaults
import androidx.compose.ui.graphics.Shape

/**
 * CompositionLocal holding the active HazeState covering the screen content behind the sheet.
 */
val LocalLongPressHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Computes readable on-accent color from the accent's luminance (Rule 1.2).
 * Returns white for dark or saturated accents (like red), black for light accents (white, yellow).
 */
fun getOnAccentColor(accentColor: Color = Accent): Color {
    return if (accentColor.luminance() < 0.48f) Color.White else Color.Black
}

// Shared Design Tokens (Section 3)
val LongPressSheetBackground = Color(0xFF18181C)
val LongPressPrimaryText = Color.White
val LongPressSecondaryText = Color.White.copy(alpha = 0.65f)
val LongPressDivider = Color.White.copy(alpha = 0.14f)
val LongPressSoftButtonFill = Color.White.copy(alpha = 0.12f)
val LongPressDestructive = Color(0xFFFF7A7A)
val LongPressPlaceholder = Color(0xFF26262B)
val LongPressSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

val LongPressSheetBorder = Modifier.border(
    width = 0.5.dp,
    color = Color.White.copy(alpha = 0.10f),
    shape = LongPressSheetShape
)

/**
 * Frosted-glass modifier matching the FloatingNavbar and "More Details" hero button.
 * Uses Haze backdrop blur (24dp), 10% white milky tint, 0.03 noise grain, and 1dp crystal gradient rim border.
 */
fun Modifier.frostedGlassSurface(
    shape: Shape = ShapeFull,
    hazeState: HazeState? = null,
    fallbackColor: Color = GlassDefaults.PillFallbackColor,
    customTint: Color = GlassDefaults.TintColor
): Modifier {
    return this
        .clip(shape)
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
                Modifier.hazeEffect(state = hazeState) {
                    blurRadius = GlassDefaults.BlurRadius
                    tints = listOf(HazeTint(customTint))
                    fallbackTint = HazeTint(fallbackColor)
                    noiseFactor = GlassDefaults.NoiseFactor
                }
            } else {
                Modifier.background(fallbackColor, shape)
            }
        )
        .border(width = 1.dp, brush = GlassDefaults.BorderBrush, shape = shape)
}

/**
 * Standard drag handle: 36 x 4 dp, white @ 60% (Rule 1.3), 8 dp from the top.
 */
@Composable
fun SheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 8.dp, bottom = 12.dp)
            .size(width = 36.dp, height = 4.dp)
            .clip(ShapeFull)
            .background(Color.White.copy(alpha = 0.60f))
    )
}

/**
 * 40 dp gradient scrim at the very top of sheets that start with an image (A and D),
 * black at 35% fading to transparent, so the handle never vanishes into the picture (Rule 1.3).
 */
@Composable
fun ImageTopScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Primary action pill (Play / Resume):
 * 48 dp (or 52 dp) tall, full-width or custom width, pure solid White background,
 * with pure Black label and play icon for optimal contrast and clean streaming-app aesthetic.
 */
@Composable
fun PrimaryPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    icon: ImageVector = Icons.Rounded.PlayArrow
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(ShapeFull)
            .background(Color.White)
            .pressable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.Black,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

/**
 * Circular secondary action button (Watchlist, Like, Share, Download):
 * 52 dp circle with soft button fill (12% white), 22 dp icon, 11 sp label underneath.
 * When active, circle background becomes Accent and icon becomes on-accent (Rule 1.2).
 */
@Composable
fun CircleAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    contentDescription: String = label
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val onAccent = getOnAccentColor(Accent)
        val bg = if (isActive) Accent else LongPressSoftButtonFill
        val tint = if (isActive) onAccent else Color.White

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = LongPressSecondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Metadata line: Year, Type, Certificate chip (1dp outline white @ 40%), HD chip.
 */
@Composable
fun MetaRow(
    item: SheetItem,
    modifier: Modifier = Modifier,
    extraMeta: String? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val parts = mutableListOf<String>()
        if (item.year != null && item.year > 0) parts.add(item.year.toString())
        parts.add(if (item.type == MediaType.TV_SERIES) "Series" else "Movie")
        if (!extraMeta.isNullOrBlank()) parts.add(extraMeta)

        Text(
            text = parts.joinToString(" • "),
            fontSize = 12.sp,
            color = LongPressSecondaryText
        )

        val cert = item.certificate
        if (!cert.isNullOrBlank()) {
            MetaChip(text = cert)
        }

        if (item.isHd) {
            MetaChip(text = "HD")
        }
    }
}

/**
 * 1 dp outline meta chip: 4 dp corners, 10 sp text.
 */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.40f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

/**
 * Thin progress bar: 3 dp tall, white @ 20% track, Accent fill.
 */
@Composable
fun ThinProgress(
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    val fraction = (progressPercent / 100f).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(ShapeFull)
            .background(Color.White.copy(alpha = 0.20f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .clip(ShapeFull)
                .background(Accent)
        )
    }
}

/**
 * Animated Watchlist icon: Plus <-> Check with 180ms rotation + crossfade.
 */
@Composable
fun WatchlistIcon(
    inWatchlist: Boolean,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (inWatchlist) 90f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "WatchlistIconRotate"
    )

    AnimatedContent(
        targetState = inWatchlist,
        transitionSpec = {
            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "WatchlistIconCrossfade",
        modifier = modifier.rotate(rotation)
    ) { active ->
        Icon(
            imageVector = if (active) Icons.Rounded.Check else Icons.Rounded.Add,
            contentDescription = if (active) "Remove from watchlist" else "Add to watchlist",
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Shared full-screen background scrim (Rule 1.1):
 * - Draws full-screen Haze surface over the home screen container:
 *   blur 16 dp, animated from 0 over 280 ms on open and back to 0 over 200 ms on close.
 * - Scrim opacity: 70% black for styles A, C, D, F; 78% black for styles B and E.
 * - On older Android versions / when blur is not supported: 85% solid black scrim, no blur.
 * - Tapping the scrim dismisses the sheet.
 */
@Composable
fun SharedScrim(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.76f,
    progress: Float = 1f,
    hazeState: HazeState? = LocalLongPressHazeState.current
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val isBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = clampedProgress
            }
            // Ambient dark underlay so transparent/negative space blurs seamlessly without patchy halos
            .background(Color.Black.copy(alpha = 0.55f))
            .then(
                if (isBlurSupported) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = 26.dp
                        tints = listOf(HazeTint(Color.Black.copy(alpha = scrimAlpha)))
                        fallbackTint = HazeTint(Color.Black.copy(alpha = 0.85f))
                    }
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.85f))
                }
            )
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            }
    )
}

