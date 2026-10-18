package com.euthopiar.ui.profile

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalAmoledMode
import kotlin.math.min

/**
 * Interactive Circular Crop Dialog ported from Detroit with Euthopiar minimal glass design.
 */
@Composable
fun DetroitCircleCropDialog(
    sourceBitmap: Bitmap,
    onDismissRequest: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit,
) {
    val isAmoled = LocalAmoledMode.current
    val dialogBgColor = if (isAmoled) ProfileGlassTokens.AmoledBackground else ProfileGlassTokens.BaseDarkBackground
    val sourceImageBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }

    var zoomFactor by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Reusable Path instance to avoid per-frame heap allocations inside draw
    val maskPath = remember { Path().apply { fillType = PathFillType.EvenOdd } }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(dialogBgColor)
                    .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(20.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Crop Avatar",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 20.sp,
                                    lineHeight = 24.sp,
                                    color = Color.White,
                                ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Drag image to frame • Adjust zoom below",
                                style = TextStyle(
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = ProfileGlassTokens.TextMuted50,
                                ),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ProfileGlassTokens.GlassFill)
                                .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, CircleShape)
                                .clickable { onDismissRequest() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val srcW = sourceBitmap.width.toFloat()
                    val srcH = sourceBitmap.height.toFloat()
                    val dynamicAspectRatio = remember(srcW, srcH) {
                        if (srcH > 0) (srcW / srcH).coerceIn(0.68f, 1.48f) else 1f
                    }

                    var viewportSizePx by remember { mutableStateOf(Size.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(dynamicAspectRatio)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161822))
                            .border(ProfileGlassTokens.HairlineWidthDp, ProfileGlassTokens.HairlineBorder, RoundedCornerShape(14.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    panOffsetX += dragAmount.x
                                    panOffsetY += dragAmount.y
                                }
                            },
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            viewportSizePx = size
                            val vw = size.width
                            val vh = size.height

                            val circleRadius = min(vw, vh) * 0.39f
                            val circleCenterX = vw / 2f
                            val circleCenterY = vh / 2f

                            val baseScale = maxOf(vw / srcW, vh / srcH)
                            val currentScale = baseScale * zoomFactor
                            val drawW = srcW * currentScale
                            val drawH = srcH * currentScale

                            val maxPanX = maxOf(0f, (drawW / 2f) - circleRadius)
                            val maxPanY = maxOf(0f, (drawH / 2f) - circleRadius)

                            val clampedPanX = panOffsetX.coerceIn(-maxPanX, maxPanX)
                            val clampedPanY = panOffsetY.coerceIn(-maxPanY, maxPanY)

                            val imgLeft = circleCenterX - (drawW / 2f) + clampedPanX
                            val imgTop = circleCenterY - (drawH / 2f) + clampedPanY

                            // 1. Draw image full brightness
                            drawImage(
                                image = sourceImageBitmap,
                                dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt()),
                                dstSize = IntSize(drawW.toInt(), drawH.toInt()),
                            )

                            // 2. Translucent dark vignette mask outside circle (reusing maskPath)
                            maskPath.reset()
                            maskPath.addRect(Rect(0f, 0f, vw, vh))
                            maskPath.addOval(
                                Rect(
                                    circleCenterX - circleRadius,
                                    circleCenterY - circleRadius,
                                    circleCenterX + circleRadius,
                                    circleCenterY + circleRadius,
                                )
                            )
                            drawPath(maskPath, color = Color(0x99000000))

                            // 3. Crisp white circular border (No red!)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.80f),
                                radius = circleRadius,
                                center = Offset(circleCenterX, circleCenterY),
                                style = Stroke(width = 1.5.dp.toPx()),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Zoom Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Zoom Level",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = ProfileGlassTokens.TextMuted50
                            )
                        )
                        Text(
                            text = "${(zoomFactor * 100).toInt()}%",
                            style = TextStyle(
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        )
                    }

                    Slider(
                        value = zoomFactor,
                        onValueChange = { zoomFactor = it },
                        valueRange = 1.0f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Actions: Reset & Apply
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileGlassPill(
                            text = "Reset",
                            onClick = {
                                panOffsetX = 0f
                                panOffsetY = 0f
                                zoomFactor = 1.0f
                            },
                            visualHeight = 44.dp,
                            fontSize = 14,
                            modifier = Modifier.weight(1f)
                        )

                        ProfileWhitePillButton(
                            text = "Apply Crop",
                            onClick = {
                                val vw = if (viewportSizePx.width > 0) viewportSizePx.width else 512f
                                val vh = if (viewportSizePx.height > 0) viewportSizePx.height else 512f
                                val circleRadius = min(vw, vh) * 0.39f
                                val circleCenterX = vw / 2f
                                val circleCenterY = vh / 2f

                                val baseScale = maxOf(vw / srcW, vh / srcH)
                                val currentScale = baseScale * zoomFactor
                                val drawW = srcW * currentScale
                                val drawH = srcH * currentScale

                                val maxPanX = maxOf(0f, (drawW / 2f) - circleRadius)
                                val maxPanY = maxOf(0f, (drawH / 2f) - circleRadius)

                                val clampedPanX = panOffsetX.coerceIn(-maxPanX, maxPanX)
                                val clampedPanY = panOffsetY.coerceIn(-maxPanY, maxPanY)

                                val imgLeft = circleCenterX - (drawW / 2f) + clampedPanX
                                val imgTop = circleCenterY - (drawH / 2f) + clampedPanY

                                val srcCenterX = (circleCenterX - imgLeft) / currentScale
                                val srcCenterY = (circleCenterY - imgTop) / currentScale
                                val srcRadius = circleRadius / currentScale

                                val cropped = ImageCropUtils.cropCircularBitmap(
                                    sourceBitmap = sourceBitmap,
                                    cropCenterX = srcCenterX,
                                    cropCenterY = srcCenterY,
                                    cropRadius = srcRadius,
                                    targetOutputSize = 512,
                                )
                                onCropConfirmed(cropped)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
