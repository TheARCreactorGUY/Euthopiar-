package com.euthopiar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.euthopiar.ui.theme.*

/**
 * Standard Velvet AsyncImage component ensuring:
 * 1. SurfaceLow placeholder background
 * 2. Crossfade 220ms
 * 3. Graceful fallback displaying title initial when image fails or is absent
 */
@Composable
fun AppAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    titleForFallback: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center
) {
    val context = LocalContext.current
    val isBlankModel = model == null || (model is String && model.isBlank())
    var isError by remember(model) { mutableStateOf(isBlankModel) }
    var retryCount by remember(model) { mutableIntStateOf(0) }

    LaunchedEffect(isError, retryCount, model) {
        if (isError && !isBlankModel && retryCount < 2) {
            kotlinx.coroutines.delay(700L * (retryCount + 1))
            retryCount += 1
            isError = false
        }
    }

    Box(
        modifier = modifier.background(SurfaceLow),
        contentAlignment = Alignment.Center
    ) {
        if (!isError && !isBlankModel) {
            val imageRequest = remember(model, retryCount, context) {
                if (model is ImageRequest) {
                    model
                } else {
                    ImageRequest.Builder(context)
                        .data(model)
                        .crossfade(220)
                        .build()
                }
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                onError = { isError = true },
                onSuccess = { isError = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        if ((isError || isBlankModel) && !titleForFallback.isNullOrBlank()) {
            val initial = titleForFallback.trim().take(1).uppercase()
            Text(
                text = initial,
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}
