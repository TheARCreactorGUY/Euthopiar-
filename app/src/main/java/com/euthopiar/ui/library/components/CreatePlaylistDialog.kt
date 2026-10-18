package com.euthopiar.ui.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euthopiar.data.repository.Playlist
import com.euthopiar.ui.theme.AppFontFamily
import com.euthopiar.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Frosted Glass Bottom Sheet for creating a new playlist:
 * - 28dp top rounded frosted glass panel with 4x36dp handle
 * - Large 26sp headline "New playlist"
 * - 46dp glass text field with 0.5px hairline border (no crimson/red)
 * - Auto-focus with keyboard
 * - Case-insensitive duplicate check & 40-char limit with live counter
 * - Full-width white pill button "Create playlist"
 * - Subtle horizontal shake animation on validation error
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    existingPlaylists: List<Playlist> = emptyList()
) {
    var title by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reduceMotion = LocalReduceMotion.current
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(120)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val trimmed = title.trim()
    val isDuplicate = remember(trimmed, existingPlaylists) {
        trimmed.isNotEmpty() && existingPlaylists.any { it.title.equals(trimmed, ignoreCase = true) }
    }

    fun triggerShake() {
        if (!reduceMotion) {
            scope.launch {
                shakeOffset.animateTo(4f, tween(40))
                shakeOffset.animateTo(-4f, tween(40))
                shakeOffset.animateTo(3f, tween(40))
                shakeOffset.animateTo(-3f, tween(40))
                shakeOffset.animateTo(0f, tween(40))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        containerColor = Color(0xFF0E0F17).copy(alpha = 0.94f),
        scrimColor = Color.Black.copy(alpha = 0.58f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "New playlist",
                style = TextStyle(
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 26.sp,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.02).sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Glass Text Field (46dp tall, 14dp radius, 0.5px hairline border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(
                        width = 0.5.dp,
                        color = if (errorMessage != null) Color.White.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = "Playlist name",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.40f)
                        )
                    )
                }

                BasicTextField(
                    value = title,
                    onValueChange = {
                        if (it.length <= 40) {
                            title = it
                            errorMessage = null
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }

            // Inline error message and live character counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.70f)
                        )
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (title.length >= 30) {
                    Text(
                        text = "${title.length}/40",
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.50f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // White Pill Button "Create playlist"
            val isButtonActive = trimmed.isNotEmpty() && !isDuplicate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isButtonActive) Color.White else Color.White.copy(alpha = 0.30f))
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            if (trimmed.isEmpty()) {
                                errorMessage = "Enter a name"
                                triggerShake()
                            } else if (isDuplicate) {
                                errorMessage = "You already have a playlist with that name"
                                triggerShake()
                            } else {
                                onConfirm(trimmed)
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create playlist",
                    style = TextStyle(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = if (isButtonActive) Color(0xFF0B0B0F) else Color(0xFF0B0B0F).copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}
