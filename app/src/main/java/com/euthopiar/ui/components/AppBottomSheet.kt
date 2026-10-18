package com.euthopiar.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.euthopiar.ui.theme.OverlayScrim
import com.euthopiar.ui.theme.ShapeL
import com.euthopiar.ui.theme.SurfaceMid
import com.euthopiar.ui.theme.TextSecondary

/**
 * Standard Modal Bottom Sheet wrapping Material3 ModalBottomSheet with ShapeL and SurfaceMid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onDismissRequest)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = ShapeL,
        containerColor = SurfaceMid,
        scrimColor = OverlayScrim,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = TextSecondary.copy(alpha = 0.4f)
            )
        },
        content = content
    )
}
