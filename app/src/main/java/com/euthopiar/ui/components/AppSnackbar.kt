package com.euthopiar.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.euthopiar.ui.theme.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * Visual model for snackbar messages.
 */
data class AppSnackbarData(
    val message: String,
    val actionLabel: String? = null,
    val durationMillis: Long = 3000L,
    private val continuation: CancellableContinuation<AppSnackbarResult>? = null
) {
    fun performAction() {
        if (continuation?.isActive == true) {
            continuation.resume(AppSnackbarResult.ActionPerformed)
        }
    }

    fun dismiss() {
        if (continuation?.isActive == true) {
            continuation.resume(AppSnackbarResult.Dismissed)
        }
    }
}

enum class AppSnackbarResult {
    Dismissed,
    ActionPerformed
}

/**
 * Controller/host state managing queue and presentation of snackbars.
 */
@Stable
class AppSnackbarHostState {
    private val mutex = Mutex()

    var currentSnackbarData by mutableStateOf<AppSnackbarData?>(null)
        private set

    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        durationMillis: Long = 3000L
    ): AppSnackbarResult {
        currentSnackbarData?.dismiss()
        return mutex.withLock {
            try {
                suspendCancellableCoroutine { continuation ->
                    currentSnackbarData = AppSnackbarData(
                        message = message,
                        actionLabel = actionLabel,
                        durationMillis = durationMillis,
                        continuation = continuation
                    )
                }
            } finally {
                currentSnackbarData = null
            }
        }
    }
}

/**
 * Controller interface for triggering snackbars across the app composition tree.
 */
interface SnackbarController {
    fun show(
        message: String,
        actionLabel: String? = null,
        durationMillis: Long = 3000L,
        onAction: (() -> Unit)? = null
    )
}

@Stable
class AppSnackbarController(
    val hostState: AppSnackbarHostState,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
) : SnackbarController {
    override fun show(
        message: String,
        actionLabel: String?,
        durationMillis: Long,
        onAction: (() -> Unit)?
    ) {
        coroutineScope.launch {
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                durationMillis = durationMillis
            )
            if (result == AppSnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }
}

val LocalSnackbarController = staticCompositionLocalOf<SnackbarController> {
    object : SnackbarController {
        override fun show(
            message: String,
            actionLabel: String?,
            durationMillis: Long,
            onAction: (() -> Unit)?
        ) {
            // Fallback for previews and unattached hierarchies
        }
    }
}

/**
 * Standard token-based Snackbar presentation component (SurfaceMid, ShapeM, hairline border).
 */
@Composable
fun AppSnackbar(
    snackbarData: AppSnackbarData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeM)
            .background(SurfaceMid)
            .border(HairlineWidth, Outline, ShapeM)
            .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing12)
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = snackbarData.message,
            style = AppTypography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        if (snackbarData.actionLabel != null) {
            Spacer(modifier = Modifier.width(Dimens.spacing12))
            Box(
                modifier = Modifier
                    .clip(ShapeS)
                    .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                    .pressable(
                        role = Role.Button,
                        onClickLabel = snackbarData.actionLabel
                    ) {
                        snackbarData.performAction()
                    }
                    .tvFocusable(
                        shape = ShapeS,
                        focusedScale = 1.0f,
                        onClick = { snackbarData.performAction() }
                    )
                    .padding(horizontal = Dimens.spacing12, vertical = Dimens.spacing8),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = snackbarData.actionLabel,
                    style = AppTypography.labelLarge,
                    color = Accent
                )
            }
        }
    }
}

/**
 * Host container for AppSnackbar that respects navigation bar insets and bottom navigation bar height.
 */
@Composable
fun AppSnackbarHost(
    hostState: AppSnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = Dimens.spacing16
) {
    val currentData = hostState.currentSnackbarData
    var displayedData by remember { mutableStateOf<AppSnackbarData?>(null) }
    if (currentData != null) {
        displayedData = currentData
    }

    LaunchedEffect(currentData) {
        if (currentData != null) {
            delay(currentData.durationMillis)
            currentData.dismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = bottomPadding, start = Dimens.spacing16, end = Dimens.spacing16),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = currentData != null,
            enter = fadeIn(tween(Motion.DurationStandard)) + slideInVertically(
                animationSpec = tween(Motion.DurationStandard),
                initialOffsetY = { it / 2 }
            ),
            exit = fadeOut(tween(Motion.DurationStandard)) + slideOutVertically(
                animationSpec = tween(Motion.DurationStandard),
                targetOffsetY = { it / 2 }
            )
        ) {
            displayedData?.let { data ->
                AppSnackbar(snackbarData = data)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun AppSnackbarPreview() {
    AppSnackbar(
        snackbarData = AppSnackbarData(
            message = "Removed from Watchlist",
            actionLabel = "Undo"
        )
    )
}
