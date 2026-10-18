package com.euthopiar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.euthopiar.R
import com.euthopiar.ui.theme.AppTypography
import com.euthopiar.ui.theme.Dimens
import com.euthopiar.ui.theme.Error
import com.euthopiar.ui.theme.TextPrimary
import com.euthopiar.ui.theme.TextSecondary
import com.euthopiar.ui.util.UserError
import com.euthopiar.ui.util.getHint
import com.euthopiar.ui.util.getTitle

/**
 * Standard calm Error State view.
 * Displays friendly consumer message and retry action instead of technical exception traces.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.error_generic_title),
    icon: ImageVector = Icons.Outlined.WifiOff,
    retryLabel: String = stringResource(R.string.action_retry),
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.spacing32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decorative - error state title and message describe error
            tint = Error,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(Dimens.spacing16))

        Text(
            text = title,
            style = AppTypography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(Dimens.spacing8))

        Text(
            text = message,
            style = AppTypography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Dimens.spacing24))

        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                PrimaryButton(
                    text = retryLabel,
                    onClick = onRetry
                )
                SecondaryButton(
                    text = secondaryActionLabel,
                    onClick = onSecondaryAction
                )
            }
        } else {
            PrimaryButton(
                text = retryLabel,
                onClick = onRetry
            )
        }
    }
}

/**
 * Overload for ErrorState directly accepting a [UserError] model.
 */
@Composable
fun ErrorState(
    error: UserError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = when (error) {
        is UserError.Offline -> Icons.Outlined.WifiOff
        else -> Icons.Outlined.ErrorOutline
    },
    retryLabel: String = stringResource(R.string.action_retry),
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val titleText = remember(error) { error.getTitle(context) }
    val hintText = remember(error) { error.getHint(context) }
    ErrorState(
        title = titleText,
        message = hintText,
        onRetry = onRetry,
        modifier = modifier,
        icon = icon,
        retryLabel = retryLabel,
        secondaryActionLabel = secondaryActionLabel,
        onSecondaryAction = onSecondaryAction
    )
}

/**
 * Inline compact variant of ErrorState.
 * Used for per-row error states so that a failing row does not blank the entire screen.
 */
@Composable
fun CompactErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = stringResource(R.string.action_retry)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null, // decorative - error message conveys context
                tint = Error,
                modifier = Modifier.size(Dimens.iconM)
            )
            Text(
                text = message,
                style = AppTypography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.size(Dimens.spacing8))
        SecondaryButton(
            text = retryLabel,
            onClick = onRetry
        )
    }
}
