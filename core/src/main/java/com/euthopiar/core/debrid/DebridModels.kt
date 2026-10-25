package com.euthopiar.core.debrid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DebridProviderType(val displayName: String) {
    NONE("Disabled"),
    REAL_DEBRID("Real-Debrid"),
    ALL_DEBRID("AllDebrid"),
    TORBOX("TorBox");

    companion object {
        fun fromId(id: String?): DebridProviderType {
            return entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: NONE
        }
    }
}

@Serializable
data class DebridAccountInfo(
    val provider: String,
    val username: String = "",
    val isPremium: Boolean = false,
    val expirationDate: String? = null,
    val points: Long = 0L
)

@Serializable
data class RealDebridUser(
    val id: Long = 0L,
    val username: String = "",
    val email: String = "",
    val points: Long = 0L,
    val type: String = "free", // "premium" or "free"
    val expiration: String? = null
) {
    val isPremium: Boolean get() = type.equals("premium", ignoreCase = true)
}

@Serializable
data class RealDebridUnrestrictResponse(
    val id: String = "",
    val filename: String = "",
    val filesize: Long = 0L,
    val link: String = "",
    val host: String = "",
    val chunks: Int = 1,
    val download: String = "",
    val streamable: Int = 0
)

@Serializable
data class RealDebridAddMagnetResponse(
    val id: String = "",
    val uri: String = ""
)

@Serializable
data class RealDebridTorrentFile(
    val id: Int = 0,
    val path: String = "",
    val bytes: Long = 0L,
    val selected: Int = 0
)

@Serializable
data class RealDebridTorrentInfo(
    val id: String = "",
    val filename: String = "",
    val status: String = "",
    val files: List<RealDebridTorrentFile> = emptyList(),
    val links: List<String> = emptyList()
)


@Serializable
data class AllDebridUserResponse(
    val status: String = "",
    val data: AllDebridUserData? = null
)

@Serializable
data class AllDebridUserData(
    val user: AllDebridUserInfo? = null
)

@Serializable
data class AllDebridUserInfo(
    val username: String = "",
    val email: String = "",
    val isPremium: Boolean = false,
    val premiumUntil: Long = 0L
)

@Serializable
data class TorBoxUserResponse(
    val success: Boolean = false,
    val data: TorBoxUserData? = null
)

@Serializable
data class TorBoxUserData(
    val email: String = "",
    val plan: Int = 0,
    val customer: String? = null
)

data class DebridResolvedStream(
    val url: String,
    val filename: String,
    val quality: String = "1080p",
    val providerName: String
)
