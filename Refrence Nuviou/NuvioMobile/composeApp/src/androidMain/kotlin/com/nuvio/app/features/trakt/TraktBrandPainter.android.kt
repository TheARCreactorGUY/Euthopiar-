package com.nuvio.app.features.trakt

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.nuvio.app.R

@Composable
actual fun traktBrandPainter(asset: TraktBrandAsset): Painter =
    painterResource(
        id = when (asset) {
            TraktBrandAsset.Glyph -> R.drawable.trakt_tv_favicon
            TraktBrandAsset.Wordmark -> R.drawable.trakt_logo_wordmark
        },
    )
