package com.euthopiar.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.data.profile.ProfileType
import com.euthopiar.data.profile.UserProfile

/**
 * Legacy edit profile dialog forwarding to [ProfileStudioSheet].
 */
@Composable
fun ManageProfileDialog(
    profile: UserProfile,
    onDismissRequest: () -> Unit,
    onProfileUpdated: (UserProfile) -> Unit,
    onProfileDeleted: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileManager = remember { ProfileManager.getInstance(context) }
    val profiles by profileManager.profiles.collectAsState()
    val canDelete = profile.type != ProfileType.ADMIN && profiles.size > 1

    ProfileStudioSheet(
        existingProfile = profile,
        canDelete = canDelete,
        onDismiss = onDismissRequest,
        onSave = { name, avatarId, role, isKids, pin, pinHash ->
            val updated = profile.copy(
                name = name,
                avatarId = avatarId,
                type = role,
                isKids = isKids,
                pin = pin,
                pinHash = pinHash
            )
            profileManager.updateProfile(updated)
            onProfileUpdated(updated)
        },
        onDelete = {
            profileManager.deleteProfile(profile.id)
            onProfileDeleted(profile)
        },
        modifier = modifier
    )
}
