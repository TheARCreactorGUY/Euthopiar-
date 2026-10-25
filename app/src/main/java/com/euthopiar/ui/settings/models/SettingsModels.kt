package com.euthopiar.ui.settings.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.euthopiar.data.profile.UserProfile

enum class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    Account(
        title = "Account & Profile",
        icon = androidx.compose.material.icons.Icons.Outlined.AccountCircle,
        description = "Switch active profile and manage account details"
    ),
    General(
        title = "General",
        icon = androidx.compose.material.icons.Icons.Outlined.Settings,
        description = "Playback controls, subtitles, and appearance"
    ),
    Ecosystem(
        title = "Ecosystem & Plugins",
        icon = androidx.compose.material.icons.Icons.Outlined.Extension,
        description = "External scrapers, stream addons, and sync integrations"
    ),
    Advanced(
        title = "Advanced",
        icon = androidx.compose.material.icons.Icons.Outlined.Storage,
        description = "Storage management, cache clearance, and backup"
    ),
    About(
        title = "About",
        icon = androidx.compose.material.icons.Icons.Outlined.Info,
        description = "Version, licenses, GitHub repository, and attributions"
    )
}

enum class SettingsPage(
    val route: String,
    val title: String,
    val subtitle: String,
    val category: SettingsCategory,
    val icon: ImageVector
) {
    Playback(
        route = "settings/playback",
        title = "Playback",
        subtitle = "Quality, auto-skip, buffer and captions",
        category = SettingsCategory.General,
        icon = androidx.compose.material.icons.Icons.Outlined.PlayArrow
    ),
    Appearance(
        route = "settings/appearance",
        title = "Appearance",
        subtitle = "Card scale, top 10 look, long-press sheets",
        category = SettingsCategory.General,
        icon = androidx.compose.material.icons.Icons.Outlined.Tune
    ),
    Integrations(
        route = "settings/integrations",
        title = "Integrations",
        subtitle = "Trakt, Simkl, Real-Debrid, AllDebrid",
        category = SettingsCategory.Ecosystem,
        icon = androidx.compose.material.icons.Icons.Outlined.Cable
    ),
    Plugins(
        route = "settings/plugins",
        title = "Plugins & Scrapers",
        subtitle = "Repositories, sources, live tester",
        category = SettingsCategory.Ecosystem,
        icon = androidx.compose.material.icons.Icons.Outlined.Extension
    ),
    Storage(
        route = "settings/storage",
        title = "Storage & cache",
        subtitle = "Caches, downloads and config backup",
        category = SettingsCategory.Advanced,
        icon = androidx.compose.material.icons.Icons.Outlined.Storage
    ),
    Updates(
        route = "settings/updates",
        title = "Application Updates",
        subtitle = "Check for updates, changelog and release channel",
        category = SettingsCategory.About,
        icon = androidx.compose.material.icons.Icons.Outlined.SystemUpdateAlt
    ),
    About(
        route = "settings/about",
        title = "About Euthopiar",
        subtitle = "Build info, licenses and project links",
        category = SettingsCategory.About,
        icon = androidx.compose.material.icons.Icons.Outlined.Info
    );

    companion object {
        fun fromRoute(route: String?): SettingsPage? = entries.firstOrNull { it.route == route }
    }
}

fun SettingsPage.isVisibleFor(profile: UserProfile): Boolean {
    if (profile.isKids) {
        return this != SettingsPage.Integrations &&
               this != SettingsPage.Plugins
    }
    return true
}

data class SettingsSearchEntry(
    val id: String,
    val title: String,
    val description: String,
    val category: SettingsCategory,
    val page: SettingsPage,
    val keywords: List<String>,
    val icon: ImageVector
) {
    val searchableText: String = (listOf(title, description, category.title, page.title) + keywords)
        .joinToString(" ")
        .lowercase()
}
