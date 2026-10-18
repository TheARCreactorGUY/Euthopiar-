package com.euthopiar.ui.longpress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.euthopiar.ui.navigation.LocalNavbarVisible

/**
 * Unified entry point for long-press content card sheets.
 * Renders the chosen [style] cleanly while content callers interact with a single interface.
 */
@Composable
fun LongPressHost(
    item: SheetItem,
    style: LongPressSheetStyle,
    actions: SheetActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navbarVisible = LocalNavbarVisible.current
    DisposableEffect(Unit) {
        navbarVisible.value = false
        onDispose {
            navbarVisible.value = true
        }
    }

    when (style) {
        LongPressSheetStyle.HERO_PEEK -> {
            HeroPeekSheet(
                item = item,
                actions = actions,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
        LongPressSheetStyle.GLASS_MENU -> {
            GlassMenuSheet(
                item = item,
                actions = actions,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
        LongPressSheetStyle.TINTED_RESUME -> {
            TintedResumeSheet(
                item = item,
                actions = actions,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
        LongPressSheetStyle.TRAILER_CARD -> {
            TrailerCardSheet(
                item = item,
                actions = actions,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
        LongPressSheetStyle.PILL_ACTIONS -> {
            PillActionsSheet(
                item = item,
                actions = actions,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
        LongPressSheetStyle.REACTION_LIST -> {
            ReactionListSheet(
                item = item,
                actions = actions,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
    }
}
