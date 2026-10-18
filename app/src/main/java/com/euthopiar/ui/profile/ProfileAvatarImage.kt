package com.euthopiar.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.euthopiar.data.profile.AvatarPreset
import com.euthopiar.data.profile.CustomAvatarRepository
import com.euthopiar.ui.theme.SurfaceLow

/**
 * Universal circular profile avatar component for Euthopiar.
 * Seamlessly resolves 8 preset avatars from assets/profiles/, custom cropped user uploads,
 * legacy preset drawables, and URI schemes.
 */
@Composable
fun ProfileAvatarImage(
    avatarId: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    borderColor: Color? = null,
    borderWidth: Dp = 2.dp,
    contentDescription: String? = "Profile Avatar",
) {
    val context = LocalContext.current

    val legacyPresetRes = remember(avatarId) {
        val matchingLegacy = AvatarPreset.entries.find { it.id.equals(avatarId, ignoreCase = true) }
        matchingLegacy?.drawableRes
    }

    val imageModel: Any? = remember(avatarId, legacyPresetRes) {
        if (legacyPresetRes != null) {
            null
        } else when {
            avatarId.startsWith("custom:") -> {
                val fileName = avatarId.removePrefix("custom:")
                val file = CustomAvatarRepository.getAvatarFile(context, fileName)
                if (file.exists()) file else "file:///android_asset/profiles/avatar_1.png"
            }
            avatarId.startsWith("file://") || avatarId.startsWith("content://") -> {
                avatarId
            }
            avatarId.startsWith("/") -> {
                val file = java.io.File(avatarId)
                if (file.exists()) file else "file:///android_asset/profiles/avatar_1.png"
            }
            else -> {
                val normalizedId = avatarId.removeSuffix(".png").trim()
                "file:///android_asset/profiles/$normalizedId.png"
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                if (borderColor != null && borderWidth > 0.dp) {
                    Modifier.border(borderWidth, borderColor, CircleShape)
                } else {
                    Modifier
                }
            )
            .background(SurfaceLow),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarId.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = contentDescription,
                    tint = Color.White.copy(alpha = 0.50f),
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        } else if (legacyPresetRes != null) {
            Image(
                painter = painterResource(id = legacyPresetRes),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (imageModel != null) {
            var isLoadError by remember(imageModel) { mutableStateOf(false) }
            if (isLoadError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = contentDescription,
                        tint = Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(size * 0.5f)
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    onError = { isLoadError = true },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = contentDescription,
                    tint = Color.White.copy(alpha = 0.50f),
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}
