package com.euthopiar.core.debrid

import com.euthopiar.core.model.StreamSource

/**
 * High-level broker managing Debrid services across Euthopiar.
 */
class DebridManager(
    private val realDebridClient: RealDebridClient = RealDebridClient(),
    private val allDebridClient: AllDebridClient = AllDebridClient(),
    private val torBoxClient: TorBoxClient = TorBoxClient()
) {

    suspend fun verifyAccount(providerType: DebridProviderType, apiKey: String): Result<DebridAccountInfo> {
        if (apiKey.isBlank()) return Result.failure(IllegalArgumentException("API key cannot be empty"))
        return when (providerType) {
            DebridProviderType.REAL_DEBRID -> {
                realDebridClient.getUserInfo(apiKey).map { user ->
                    DebridAccountInfo(
                        provider = "Real-Debrid",
                        username = user.username,
                        isPremium = user.isPremium,
                        expirationDate = user.expiration,
                        points = user.points
                    )
                }
            }
            DebridProviderType.ALL_DEBRID -> {
                allDebridClient.getUserInfo(apiKey).map { user ->
                    DebridAccountInfo(
                        provider = "AllDebrid",
                        username = user.username,
                        isPremium = user.isPremium,
                        expirationDate = null
                    )
                }
            }
            DebridProviderType.TORBOX -> {
                torBoxClient.getUserInfo(apiKey).map { user ->
                    DebridAccountInfo(
                        provider = "TorBox",
                        username = user.email,
                        isPremium = user.plan > 0,
                        expirationDate = null
                    )
                }
            }
            DebridProviderType.NONE -> Result.failure(IllegalArgumentException("Debrid is disabled"))
        }
    }

    suspend fun resolveStream(providerType: DebridProviderType, apiKey: String, linkOrMagnet: String): Result<StreamSource> {
        if (apiKey.isBlank()) return Result.failure(IllegalArgumentException("API key is blank"))
        return when (providerType) {
            DebridProviderType.REAL_DEBRID -> {
                val isMagnet = linkOrMagnet.startsWith("magnet:", ignoreCase = true) ||
                        linkOrMagnet.startsWith("torrent://", ignoreCase = true) ||
                        (linkOrMagnet.length in listOf(32, 40) && linkOrMagnet.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' })

                val effectiveMagnet = if (!linkOrMagnet.startsWith("magnet:", ignoreCase = true) && !linkOrMagnet.contains("http")) {
                    "magnet:?xt=urn:btih:$linkOrMagnet"
                } else {
                    linkOrMagnet
                }

                val unrestrictResult = if (isMagnet) {
                    realDebridClient.unrestrictMagnet(apiKey, effectiveMagnet)
                } else {
                    realDebridClient.unrestrictLink(apiKey, linkOrMagnet)
                }

                unrestrictResult.map { resp ->
                    StreamSource(
                        url = resp.download.ifBlank { resp.link },
                        serverName = "Real-Debrid (${resp.host.ifBlank { "High-Speed CDN" }})",
                        resolutionLabel = "1080p FHD",
                        quality = "1080p",
                        isM3u8 = resp.download.contains(".m3u8"),
                        headers = mapOf("User-Agent" to "Euthopiar/1.0")
                    )
                }
            }
            else -> Result.failure(UnsupportedOperationException("Stream resolving for $providerType not yet available"))
        }
    }

    companion object {
        @Volatile
        private var instance: DebridManager? = null

        fun getInstance(): DebridManager {
            return instance ?: synchronized(this) {
                instance ?: DebridManager().also { instance = it }
            }
        }
    }
}
