package com.euthopiar.core.repository

import com.euthopiar.core.addons.*
import com.euthopiar.core.matcher.MatchScorer
import com.euthopiar.core.model.*
import com.euthopiar.core.network.DohDns
import com.euthopiar.core.registry.PluginRegistry
import com.euthopiar.core.util.TmdbBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * High-level unified data broker connecting ViewModels and UI layers to active or global streaming providers.
 */
open class ProviderRepository(
    val registry: PluginRegistry = PluginRegistry.Default
) {
    private val searchGate = Semaphore(permits = 6)
    val activePlugin: StateFlow<UniversalPlugin> get() = registry.activePlugin
    val plugins: StateFlow<List<UniversalPlugin>> get() = registry.plugins

    /**
     * Fetches the home catalog rows. Fetches canonical TMDB core rows first (Trending, Popular),
     * then dynamically fetches and appends enabled Stremio addon catalog rails.
     */
    suspend fun getHomeCatalog(): List<CatalogRow> = withContext(Dispatchers.IO) {
        val tmdbRows = try {
            TmdbBridge.fetchHomeCatalog(DohDns.metadataClient)
        } catch (_: Exception) {
            emptyList<CatalogRow>()
        }

        val stremioRows = if (TmdbBridge.activeRegionPreset == com.euthopiar.core.model.ContentRegionPreset.GLOBAL) {
            fetchStremioCatalogRails(offset = 0, limit = 6)
        } else {
            emptyList()
        }
        val combined = tmdbRows + stremioRows
        if (combined.isNotEmpty()) combined else tmdbRows
    }

    /**
     * Progressively loads batches of extra catalog rails for smooth infinite-scrolling.
     * Supports both progressive TMDB genres and Stremio catalog pagination.
     */
    suspend fun getProgressiveHomeRails(offset: Int, limit: Int = 4): List<CatalogRow> = withContext(Dispatchers.IO) {
        if (TmdbBridge.activeRegionPreset != com.euthopiar.core.model.ContentRegionPreset.GLOBAL) {
            return@withContext emptyList()
        }
        val tmdbRails = try {
            TmdbBridge.fetchProgressiveRails(DohDns.metadataClient, offset, limit)
        } catch (_: Exception) {
            emptyList<CatalogRow>()
        }

        if (tmdbRails.isNotEmpty()) {
            tmdbRails
        } else {
            // Load subsequent Stremio catalog rails if TMDB rails have been exhausted
            val stremioOffset = 6 + (offset - 38).coerceAtLeast(0)
            fetchStremioCatalogRails(offset = stremioOffset, limit = limit)
        }
    }

    private suspend fun fetchStremioCatalogRails(offset: Int, limit: Int): List<CatalogRow> = withContext(Dispatchers.IO) {
        val addonRepo = com.euthopiar.core.addons.AddonRepository.getInstanceOrNull() ?: return@withContext emptyList()
        val enabledAddons = addonRepo.uiState.value.addons.filter { it.enabled }
        if (enabledAddons.isEmpty()) return@withContext emptyList()

        val readyAddons = enabledAddons.map { addon ->
            if (addon.manifest == null) {
                try {
                    addonRepo.refreshAddon(addon.manifestUrl)
                    addonRepo.uiState.value.addons.firstOrNull { it.manifestUrl == addon.manifestUrl } ?: addon
                } catch (_: Exception) {
                    addon
                }
            } else {
                addon
            }
        }.filter { it.manifest != null }

        if (readyAddons.isEmpty()) return@withContext emptyList()

        val allDefs = com.euthopiar.core.addons.buildHomeCatalogDefinitions(readyAddons)
        val targetDefs = allDefs.drop(offset).take(limit)
        if (targetDefs.isEmpty()) return@withContext emptyList()

        coroutineScope {
            val deferred = targetDefs.map { def ->
                async(Dispatchers.IO) {
                    try {
                        withTimeoutOrNull(6000L) {
                            val url = def.buildCatalogUrl()
                            val response = com.euthopiar.core.js.network.httpRequestRaw("GET", url, emptyMap(), "", followRedirects = true)
                            if (response.status in 200..299 && response.body.isNotBlank()) {
                                val parsed = com.euthopiar.core.addons.HomeCatalogParser.parseCatalog(
                                    payload = response.body,
                                    addonName = def.addonName,
                                    maxItems = 20
                                )
                                if (parsed.isNotEmpty()) {
                                    CatalogRow(
                                        title = def.defaultTitle,
                                        items = parsed,
                                        subtitle = def.addonName,
                                        eyebrow = def.type.uppercase()
                                    )
                                } else null
                            } else null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            deferred.awaitAll().filterNotNull()
        }
    }

    /**
     * Executes a search for [query].
     * First queries TMDB multi-search to guarantee rich canonical posters, release years,
     * ratings, and media types. Also queries registered plugins as secondary fallback.
     */
    suspend fun search(query: String, isGlobal: Boolean = false): List<MediaItem> = withContext(Dispatchers.IO) {
        if (!isGlobal) {
            val currentPlugin = registry.activePlugin.value
            return@withContext currentPlugin.search(query).map { item ->
                if (item.provider == null) item.copy(provider = currentPlugin.name) else item
            }
        }

        // Global search: query all three sources concurrently inside coroutineScope without blocking each other
        coroutineScope {
            val registered = registry.plugins.value
            val pluginDeferreds = registered.map { plugin ->
                async(Dispatchers.IO) {
                    searchGate.withPermit {
                        try {
                            withTimeoutOrNull(12_000L) {
                                plugin.search(query).map { item ->
                                    if (item.provider == null) item.copy(provider = plugin.name) else item
                                }
                            } ?: emptyList()
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }
            }

            val searchDefs = com.euthopiar.core.addons.AddonRepository.getInstanceOrNull()?.let { repo ->
                com.euthopiar.core.addons.buildSearchCatalogDefinitions(repo.uiState.value.addons)
            } ?: emptyList()

            val stremioDeferreds = searchDefs.map { def ->
                async(Dispatchers.IO) {
                    try {
                        withTimeoutOrNull(6000L) {
                            val url = def.buildCatalogUrl(search = query)
                            val response = com.euthopiar.core.js.network.httpRequestRaw("GET", url, emptyMap(), "", followRedirects = true)
                            if (response.status in 200..299 && response.body.isNotBlank()) {
                                com.euthopiar.core.addons.HomeCatalogParser.parseCatalog(
                                    payload = response.body,
                                    addonName = def.addonName
                                )
                            } else emptyList()
                        } ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }

            val tmdbDeferred = async(Dispatchers.IO) {
                try {
                    withTimeoutOrNull(8000L) {
                        TmdbBridge.searchMulti(DohDns.metadataClient, query)
                    } ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val allResults = buildList {
                addAll(pluginDeferreds.awaitAll().flatten())
                addAll(stremioDeferreds.awaitAll().flatten())
                addAll(tmdbDeferred.await())
            }

            val seen = mutableSetOf<String>()
            allResults.filter { item ->
                val key = "${item.type}:${item.id.lowercase().trim()}"
                seen.add(key)
            }
        }
    }

    private fun resolveMetaTypeForAddon(manifest: AddonManifest, mediaType: MediaType, rawTypeFromUrl: String?): String? {
        val metaRes = manifest.resources.firstOrNull { it.name.equals("meta", ignoreCase = true) }
        val declaredTypes = (metaRes?.types?.ifEmpty { manifest.types } ?: manifest.types).map { it.lowercase() }

        if (rawTypeFromUrl != null && (declaredTypes.isEmpty() || declaredTypes.contains(rawTypeFromUrl.lowercase()))) {
            return rawTypeFromUrl.lowercase()
        }

        return when (mediaType) {
            MediaType.LIVE_TV -> {
                if (declaredTypes.isEmpty()) "tv"
                else listOf("tv", "channel", "live").firstOrNull { it in declaredTypes } ?: "tv"
            }
            MediaType.TV_SERIES -> {
                if (declaredTypes.isEmpty()) "series"
                else listOf("series", "tv").firstOrNull { it in declaredTypes } ?: "series"
            }
            MediaType.MOVIE -> {
                if (declaredTypes.isEmpty()) "movie"
                else listOf("movie", "movies").firstOrNull { it in declaredTypes } ?: "movie"
            }
            MediaType.ANIME -> {
                if (declaredTypes.isEmpty()) "anime"
                else listOf("anime", "series", "tv", "movie").firstOrNull { it in declaredTypes } ?: "anime"
            }
        }
    }

    private fun isCustomStremioAddonId(id: String): Boolean {
        val clean = id.trim()
        if (clean.isBlank()) return false
        if (clean.all { it.isDigit() }) return false
        if (clean.startsWith("tt", ignoreCase = true)) return false
        val knownPrefixes = listOf("tmdb:", "imdb:", "mal:", "jikan:", "kitsu:", "anilist:", "tmdb/")
        if (knownPrefixes.any { clean.startsWith(it, ignoreCase = true) }) return false
        return true
    }

    /**
     * Loads media details for [mediaItem].
     *
     * @param mediaItem The media item to inspect.
     * @param providerName Optional provider name override; falls back to [MediaItem.provider], then active provider.
     */
    suspend fun getDetails(
        mediaItem: MediaItem,
        providerName: String? = null,
        fallbackTitle: String? = null,
        fallbackPosterUrl: String? = null
    ): MediaDetail = withContext(Dispatchers.IO) {
        val targetName = providerName ?: mediaItem.provider
        val cleanId = mediaItem.id.removePrefix("tmdb:").removePrefix("tmdb/").trim()
        val baseId = cleanId.substringBefore(':').trim()

        val rawTypeFromUrl = if (mediaItem.url.startsWith("stremio_type:")) {
            mediaItem.url.removePrefix("stremio_type:").trim()
        } else null

        val addonRepo = com.euthopiar.core.addons.AddonRepository.getInstanceOrNull()
        val allAddons = addonRepo?.uiState?.value?.addons.orEmpty()
        val enabledAddons = allAddons.filter { it.enabled && it.manifest != null }

        val matchesInstalledAddon = targetName != null && allAddons.any {
            it.displayTitle.equals(targetName, ignoreCase = true) ||
            it.manifest?.name.equals(targetName, ignoreCase = true) ||
            it.manifest?.id.equals(targetName, ignoreCase = true)
        }

        val metaSupportingAddons = enabledAddons.mapNotNull { addon ->
            val manifest = addon.manifest ?: return@mapNotNull null
            val resolvedType = resolveMetaTypeForAddon(manifest, mediaItem.type, rawTypeFromUrl) ?: return@mapNotNull null
            if (manifest.supportsResource("meta", resolvedType, mediaItem.id)) {
                addon to resolvedType
            } else null
        }.sortedByDescending { (addon, _) ->
            if (targetName != null && (
                addon.displayTitle.equals(targetName, ignoreCase = true) ||
                addon.manifest?.name.equals(targetName, ignoreCase = true) ||
                addon.manifest?.id.equals(targetName, ignoreCase = true)
            )) 1 else 0
        }

        val isStremioAddon = matchesInstalledAddon || metaSupportingAddons.isNotEmpty() ||
            targetName?.contains("catalog", ignoreCase = true) == true ||
            targetName?.contains("stremio", ignoreCase = true) == true ||
            targetName?.contains("nuvio", ignoreCase = true) == true ||
            mediaItem.provider?.contains("catalog", ignoreCase = true) == true ||
            mediaItem.provider?.contains("stremio", ignoreCase = true) == true ||
            mediaItem.provider?.contains("nuvio", ignoreCase = true) == true

        // Step 1: Direct Addon meta First
        if (metaSupportingAddons.isNotEmpty()) {
            for ((addon, metaType) in metaSupportingAddons) {
                val metaUrl = com.euthopiar.core.addons.buildAddonResourceUrl(
                    manifestUrl = addon.manifestUrl,
                    resource = "meta",
                    type = metaType,
                    id = mediaItem.id
                )
                val response = runCatching {
                    com.euthopiar.core.js.network.httpRequestRaw("GET", metaUrl, emptyMap(), "", followRedirects = true)
                }.getOrNull()

                if (response != null && response.status in 200..299 && response.body.isNotBlank()) {
                    val parsed = runCatching {
                        com.euthopiar.core.addons.AddonMetaParser.parseMeta(
                            payload = response.body,
                            addonName = addon.displayTitle
                        )
                    }.getOrNull()
                    if (parsed != null) {
                        val effectiveImdb = parsed.imdbId?.takeIf { it.startsWith("tt") }
                            ?: baseId.takeIf { it.startsWith("tt") }

                        if (!effectiveImdb.isNullOrBlank() && mediaItem.type != MediaType.LIVE_TV) {
                            val isTv = parsed.type == MediaType.TV_SERIES || parsed.type == MediaType.ANIME
                            runCatching {
                                val tmdbId = TmdbBridge.findTmdbIdByImdb(DohDns.metadataClient, effectiveImdb, isTv) ?: effectiveImdb
                                val enriched = TmdbBridge.fetchEnrichedDetails(DohDns.metadataClient, tmdbId, isTv, "tmdb")
                                if (enriched != null) {
                                    return@withContext parsed.copy(
                                        cast = if (parsed.cast.isNotEmpty()) parsed.cast else enriched.cast,
                                        recommendations = if (parsed.recommendations.isNotEmpty()) parsed.recommendations else enriched.recommendations,
                                        trailerUrl = parsed.trailerUrl ?: enriched.trailerUrl,
                                        logoUrl = parsed.logoUrl ?: enriched.logoUrl,
                                        genres = if (parsed.genres.isNotEmpty()) parsed.genres else enriched.genres,
                                        directors = if (parsed.directors.isNotEmpty()) parsed.directors else enriched.directors
                                    )
                                }
                            }
                        }
                        return@withContext parsed
                    }
                }
            }
        }

        val isCustomId = isCustomStremioAddonId(mediaItem.id)
        val isLiveOrCustom = mediaItem.type == MediaType.LIVE_TV || isCustomId

        if (isLiveOrCustom) {
            val effTitle = mediaItem.title.takeIf { it.isNotBlank() && !it.equals("Media Details", ignoreCase = true) }
                ?: fallbackTitle?.takeIf { it.isNotBlank() && !it.equals("Media Details", ignoreCase = true) }
                ?: mediaItem.id
            val effPoster = mediaItem.posterUrl ?: fallbackPosterUrl
            val nonTmdbTarget = targetName?.takeUnless { it.equals("tmdb", ignoreCase = true) }
            val nonTmdbItemProvider = mediaItem.provider?.takeUnless { it.equals("tmdb", ignoreCase = true) }
            val matchedAddon = allAddons.firstOrNull { addon ->
                addon.manifest?.supportsResource("stream", if (mediaItem.type == MediaType.LIVE_TV) "tv" else "movie", mediaItem.id) == true ||
                addon.manifest?.supportsResource("meta", if (mediaItem.type == MediaType.LIVE_TV) "tv" else "movie", mediaItem.id) == true
            }
            val effProvider = nonTmdbTarget ?: nonTmdbItemProvider ?: matchedAddon?.displayTitle ?: "Stremio"
            return@withContext MediaDetail(
                id = mediaItem.id,
                title = effTitle,
                url = mediaItem.url.ifBlank { "stremio_type:${if (mediaItem.type == MediaType.LIVE_TV) "tv" else "movie"}" },
                posterUrl = effPoster,
                backdropUrl = effPoster,
                type = mediaItem.type,
                year = mediaItem.year,
                synopsis = "Live channel / custom stream details.",
                provider = effProvider,
                episodes = listOf(
                    EpisodeItem(
                        id = mediaItem.id,
                        title = effTitle,
                        seasonNumber = 0,
                        episodeNumber = 1,
                        data = mediaItem.id,
                        thumbnail = effPoster
                    )
                )
            )
        }

        // Step 2: Fallback for Catalog-Only Addons (like CyberFlix) & TMDB Targets
        val isTmdbTarget = targetName.equals("tmdb", ignoreCase = true) ||
            mediaItem.provider.equals("tmdb", ignoreCase = true) ||
            baseId.startsWith("tt") ||
            isStremioAddon ||
            (baseId.all { it.isDigit() } && baseId.isNotEmpty() && (targetName.isNullOrBlank() || targetName.equals("No Provider", ignoreCase = true)))

        if (isTmdbTarget) {
            val isTv = mediaItem.type == MediaType.TV_SERIES || mediaItem.type == MediaType.ANIME
            val resolvedTmdbId = if (baseId.startsWith("tt")) {
                TmdbBridge.findTmdbIdByImdb(
                    client = DohDns.metadataClient,
                    imdbId = baseId,
                    isTv = isTv
                ) ?: baseId
            } else {
                baseId
            }

            var enriched = TmdbBridge.fetchEnrichedDetails(
                client = DohDns.metadataClient,
                tmdbId = resolvedTmdbId,
                isTv = isTv,
                providerName = "tmdb"
            )

            // Cinemeta fallback if TMDB returns null and ID is tt...
            if (enriched == null && baseId.startsWith("tt")) {
                val cinemetaType = if (isTv) "series" else "movie"
                val cinemetaUrl = "https://v3-cinemeta.strem.io/meta/$cinemetaType/$baseId.json"
                val cinemetaResp = runCatching {
                    com.euthopiar.core.js.network.httpRequestRaw("GET", cinemetaUrl, emptyMap(), "", followRedirects = true)
                }.getOrNull()
                if (cinemetaResp != null && cinemetaResp.status in 200..299 && cinemetaResp.body.isNotBlank()) {
                    val cinemetaDetail = runCatching {
                        com.euthopiar.core.addons.AddonMetaParser.parseMeta(
                            payload = cinemetaResp.body,
                            addonName = "Cinemeta"
                        )
                    }.getOrNull()
                    if (cinemetaDetail != null) {
                        return@withContext cinemetaDetail
                    }
                }
            }

            if (enriched != null) {
                val safeTmdbId = enriched.tmdbId.ifBlank { resolvedTmdbId }
                val episodes: List<EpisodeItem> = if (isTv) {
                    val seasonsMap = TmdbBridge.fetchTmdbSeasons(
                        client = DohDns.metadataClient,
                        tmdbId = safeTmdbId,
                        seasons = enriched.seasonNumbers
                    )
                    val epList = mutableListOf<EpisodeItem>()
                    for ((sNum, eps) in seasonsMap) {
                        for ((eNum, epDetail) in eps) {
                            epList.add(
                                EpisodeItem(
                                    id = "$safeTmdbId:$sNum:$eNum",
                                    title = epDetail.name.ifBlank { "Episode $eNum" },
                                    seasonNumber = sNum,
                                    episodeNumber = eNum,
                                    data = "$safeTmdbId:$sNum:$eNum",
                                    thumbnail = epDetail.stillUrl,
                                    description = epDetail.overview
                                )
                            )
                        }
                    }
                    epList
                } else emptyList()

                return@withContext MediaDetail(
                    id = safeTmdbId,
                    title = enriched.title,
                    url = mediaItem.url,
                    posterUrl = enriched.posterUrl,
                    backdropUrl = enriched.backdropUrl,
                    type = mediaItem.type,
                    year = enriched.year,
                    synopsis = enriched.synopsis,
                    genres = enriched.genres,
                    duration = enriched.duration,
                    episodes = episodes,
                    rating = enriched.rating,
                    contentRating = enriched.contentRating,
                    provider = "tmdb",
                    cast = enriched.cast,
                    logoUrl = enriched.logoUrl,
                    imdbId = enriched.imdbId,
                    rottenTomatoesRating = enriched.rottenTomatoesRating,
                    recommendations = enriched.recommendations,
                    trailerUrl = enriched.trailerUrl,
                    directors = enriched.directors
                )
            } else {
                return@withContext MediaDetail(
                    id = cleanId.ifBlank { mediaItem.id },
                    title = mediaItem.title.ifBlank { "Media Details" },
                    url = mediaItem.url,
                    posterUrl = mediaItem.posterUrl,
                    backdropUrl = mediaItem.backdropUrl,
                    type = mediaItem.type,
                    year = mediaItem.year,
                    synopsis = "No synopsis available.",
                    provider = "tmdb"
                )
            }
        }
        val targetPlugin = targetName?.let { registry.getPlugin(it) } ?: registry.activePlugin.value
        val rawDetail = targetPlugin.getDetails(mediaItem)

        // Enrich details with trailer, logo, cast, genres, and TMDB bridge metadata if needed
        val needsEnrichment = (
            rawDetail.trailerUrl.isNullOrBlank() ||
            rawDetail.logoUrl.isNullOrBlank() ||
            rawDetail.cast.isEmpty() ||
            rawDetail.genres.isEmpty() ||
            rawDetail.imdbId.isNullOrBlank() ||
            rawDetail.rottenTomatoesRating.isNullOrBlank() ||
            rawDetail.rating.isNullOrBlank() ||
            ((rawDetail.type == MediaType.TV_SERIES || rawDetail.type == MediaType.ANIME) && (rawDetail.episodes.isEmpty() || rawDetail.episodes.all { it.thumbnail.isNullOrBlank() }))
        )

        if (needsEnrichment) {
            try {
                val isTv = rawDetail.type == MediaType.TV_SERIES ||
                           rawDetail.type == MediaType.ANIME ||
                           rawDetail.title.contains("Season", ignoreCase = true) ||
                           rawDetail.title.contains("Series", ignoreCase = true)
                val cleanTitle = cleanReleaseTitle(rawDetail.title.ifBlank { mediaItem.title })
                val year = rawDetail.year ?: Regex("""(19|20)\d{2}""").find(rawDetail.title)?.value?.toIntOrNull()

                val cleanRawId = rawDetail.id.removePrefix("tmdb:").removePrefix("tmdb/").trim()
                val cleanMediaId = mediaItem.id.removePrefix("tmdb:").removePrefix("tmdb/").trim()
                val rawBaseId = cleanRawId.substringBefore(':').trim()
                val mediaBaseId = cleanMediaId.substringBefore(':').trim()

                var tmdbId = if (rawBaseId.all { it.isDigit() } && rawBaseId.isNotEmpty()) {
                    rawBaseId
                } else if (mediaBaseId.all { it.isDigit() } && mediaBaseId.isNotEmpty()) {
                    mediaBaseId
                } else if (rawBaseId.startsWith("tt")) {
                    TmdbBridge.findTmdbIdByImdb(DohDns.metadataClient, rawBaseId, isTv)
                } else if (mediaBaseId.startsWith("tt")) {
                    TmdbBridge.findTmdbIdByImdb(DohDns.metadataClient, mediaBaseId, isTv)
                } else {
                    TmdbBridge.searchTmdbId(
                        client = DohDns.metadataClient,
                        title = cleanTitle,
                        year = year,
                        isTv = isTv
                    )
                }

                var enriched = if (!tmdbId.isNullOrBlank()) {
                    TmdbBridge.fetchEnrichedDetails(
                        client = DohDns.metadataClient,
                        tmdbId = tmdbId,
                        isTv = isTv,
                        providerName = targetName ?: "unknown"
                    )
                } else null

                // If numeric ID lookup failed to enrich (e.g. provider ID was internal digits, not a valid TMDB ID),
                // fall back to searchTmdbId by title and year!
                if (enriched == null) {
                    val searchedId = TmdbBridge.searchTmdbId(
                        client = DohDns.metadataClient,
                        title = cleanTitle,
                        year = year,
                        isTv = isTv
                    )
                    if (!searchedId.isNullOrBlank() && searchedId != tmdbId) {
                        tmdbId = searchedId
                        enriched = TmdbBridge.fetchEnrichedDetails(
                            client = DohDns.metadataClient,
                            tmdbId = searchedId,
                            isTv = isTv,
                            providerName = targetName ?: "unknown"
                        )
                    }
                }

                if (enriched != null) {
                    val safeTmdbId = enriched.tmdbId.ifBlank { tmdbId ?: rawDetail.id }
                    // Bridge episodes with TMDB metadata & thumbnails
                    val episodes: List<EpisodeItem> = if (isTv && enriched.seasonNumbers.isNotEmpty()) {
                        try {
                            val seasonsMap = TmdbBridge.fetchTmdbSeasons(
                                client = DohDns.metadataClient,
                                tmdbId = safeTmdbId,
                                seasons = enriched.seasonNumbers
                            )
                            if (rawDetail.episodes.size > 1 && rawDetail.episodes.any { it.episodeNumber > 1 }) {
                                // Enrich existing scraped episodes with canonical TMDB stills, titles, and overviews
                                rawDetail.episodes.map { ep ->
                                    val epDetail = seasonsMap[ep.seasonNumber]?.get(ep.episodeNumber)
                                    ep.copy(
                                        title = epDetail?.name?.ifBlank { ep.title } ?: ep.title,
                                        thumbnail = epDetail?.stillUrl ?: ep.thumbnail ?: enriched.backdropUrl,
                                        description = epDetail?.overview ?: ep.description ?: enriched.synopsis,
                                        duration = epDetail?.duration ?: ep.duration
                                    )
                                }
                            } else {
                                // Scraped page had only batch pack or single dummy ep; populate full episodic list from TMDB
                                val baseData = rawDetail.episodes.firstOrNull()?.data ?: rawDetail.url.ifBlank { rawDetail.id }
                                val generated = mutableListOf<EpisodeItem>()
                                for ((sNum, eps) in seasonsMap) {
                                    for ((eNum, epDetail) in eps) {
                                        generated.add(
                                            EpisodeItem(
                                                id = "${rawDetail.id}_${sNum}_$eNum",
                                                title = epDetail.name.ifBlank { "Episode $eNum" },
                                                seasonNumber = sNum,
                                                episodeNumber = eNum,
                                                data = "$baseData?season=$sNum&episode=$eNum",
                                                thumbnail = epDetail.stillUrl ?: enriched.backdropUrl,
                                                description = epDetail.overview ?: enriched.synopsis,
                                                duration = epDetail.duration
                                            )
                                        )
                                    }
                                }
                                if (generated.isNotEmpty()) generated else rawDetail.episodes
                            }
                        } catch (_: Throwable) {
                            rawDetail.episodes
                        }
                    } else {
                        rawDetail.episodes
                    }

                    // Determine if title should be sanitized to canonical TMDB title
                    val isTitleTorrentJunk = rawDetail.title.contains(Regex("""(?i)\b(2160p|1080p|720p|hevc|remux|bluray|dovi|hdr|ddp|truehd)\b"""))
                    val finalTitle = if (isTitleTorrentJunk && enriched.title.isNotBlank()) enriched.title else rawDetail.title

                    val effectiveRt = rawDetail.rottenTomatoesRating 
                        ?: enriched.rottenTomatoesRating 
                        ?: (rawDetail.rating ?: enriched.rating)?.toDoubleOrNull()?.let { score ->
                            if (score > 0.0) {
                                val pct = ((score * 10.0) + 2.0).coerceIn(42.0, 98.0).toInt()
                                "$pct%"
                            } else null
                        }

                    return@withContext rawDetail.copy(
                        title = finalTitle,
                        trailerUrl = enriched.trailerUrl ?: (rawDetail.trailerUrl?.takeIf { !it.contains("short") && !it.contains("vertical") }),
                        logoUrl = rawDetail.logoUrl ?: enriched.logoUrl,
                        backdropUrl = enriched.backdropUrl ?: rawDetail.backdropUrl,
                        synopsis = rawDetail.synopsis?.ifBlank { enriched.synopsis } ?: enriched.synopsis,
                        cast = if (rawDetail.cast.isNotEmpty()) rawDetail.cast else enriched.cast,
                        directors = if (rawDetail.directors.isNotEmpty()) rawDetail.directors else enriched.directors,
                        genres = if (rawDetail.genres.isNotEmpty()) rawDetail.genres else enriched.genres,
                        rating = rawDetail.rating ?: enriched.rating,
                        rottenTomatoesRating = effectiveRt,
                        contentRating = rawDetail.contentRating ?: enriched.contentRating,
                        duration = rawDetail.duration ?: enriched.duration,
                        year = rawDetail.year ?: enriched.year,
                        imdbId = rawDetail.imdbId ?: enriched.imdbId,
                        recommendations = if (rawDetail.recommendations.isNotEmpty()) rawDetail.recommendations else enriched.recommendations,
                        episodes = episodes
                    )
                }
            } catch (_: Throwable) {}
        }

        rawDetail
    }

    /**
     * Overload to load details directly by [mediaId], [type], and optional [providerName].
     */
    suspend fun getDetails(
        mediaId: String,
        type: MediaType = MediaType.MOVIE,
        providerName: String? = null,
        fallbackTitle: String? = null,
        fallbackPosterUrl: String? = null
    ): MediaDetail {
        val dummy = MediaItem(
            id = mediaId,
            title = fallbackTitle.orEmpty(),
            url = "stremio_type:${if (type == MediaType.LIVE_TV) "tv" else if (type == MediaType.TV_SERIES) "series" else "movie"}",
            posterUrl = fallbackPosterUrl,
            type = type,
            provider = providerName
        )
        return getDetails(dummy, providerName, fallbackTitle, fallbackPosterUrl)
    }

    /**
     * Asynchronously loads cast members for a given media item without blocking details loading.
     */
    suspend fun getCast(
        mediaId: String,
        imdbId: String? = null,
        type: MediaType = MediaType.MOVIE,
        providerName: String? = null
    ): List<CastMember> = withContext(Dispatchers.IO) {
        val targetName = providerName
        val targetPlugin = targetName?.let { registry.getPlugin(it) } ?: registry.activePlugin.value
        try {
            targetPlugin.fetchCast(mediaId, imdbId, type)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Resolves playable streams and subtitle tracks.
     *
     * @param episodeData The episode identifier/payload string.
     * @param providerName Optional provider name override; falls back to active provider.
     */
    suspend fun getStreamLinks(
        episodeData: String,
        providerName: String? = null,
        mediaType: String? = null,
        imdbId: String? = null,
        title: String? = null,
        year: Int? = null
    ): StreamResult = withContext(Dispatchers.IO) {
        val streams = mutableListOf<StreamSource>()
        val subtitles = mutableListOf<SubtitleTrack>()
        try {
            withTimeoutOrNull(20_000L) {
                getStreamFlow(
                    episodeData = episodeData,
                    providerName = providerName,
                    mediaType = mediaType,
                    imdbId = imdbId,
                    title = title,
                    year = year
                ).collect { emission ->
                    when (emission) {
                        is StreamEmission.SourceFound -> streams.add(emission.source)
                        is StreamEmission.SubtitleFound -> subtitles.add(emission.track)
                        else -> {}
                    }
                }
            }
        } catch (_: Exception) {}
        StreamResult(streams.distinctBy { it.url }, subtitles.distinctBy { it.url })
    }

    /**
     * Progressively streams resolved stream sources, subtitles, and server status events.
     */
    fun getStreamFlow(plugin: UniversalPlugin, mediaId: String, episodeData: String?): kotlinx.coroutines.flow.Flow<StreamEmission> =
        plugin.getStreamFlow(mediaId, episodeData)

    /**
     * Progressively streams resolved stream sources, subtitles, and server status events for an episode.
     * Concurrently queries all enabled JavaScript provider scrapers via unified StreamsRepository.
     */
    fun getStreamFlow(
        episodeData: String,
        providerName: String? = null,
        mediaType: String? = null,
        imdbId: String? = null,
        title: String? = null,
        year: Int? = null,
        originalLanguage: String? = null
    ): Flow<StreamEmission> = channelFlow {
        val cleanData = episodeData.removePrefix("tmdb:").removePrefix("tmdb/")
        val isLiveTvType = mediaType != null && (
            mediaType.equals("live", ignoreCase = true) ||
            mediaType.equals("channel", ignoreCase = true) ||
            mediaType.equals("live_tv", ignoreCase = true)
        )

        val (mediaId, season, episode) = if (isLiveTvType) {
            Triple(cleanData, null, null)
        } else {
            val parts = cleanData.split(":")
            val lastSeason = parts.getOrNull(parts.size - 2)?.toIntOrNull()
            val lastEpisode = parts.getOrNull(parts.size - 1)?.toIntOrNull()
            if (parts.size >= 3 && lastSeason != null && lastEpisode != null) {
                Triple(parts.dropLast(2).joinToString(":"), lastSeason, lastEpisode)
            } else if (parts.size == 2 && parts[0].toIntOrNull() != null && parts[1].toIntOrNull() != null) {
                Triple(cleanData, parts[0].toInt(), parts[1].toInt())
            } else {
                Triple(cleanData, null, null)
            }
        }
        val isTv = season != null && episode != null
        val resolvedType = mediaType ?: if (isTv) "tv" else "movie"

        // Delegate to unified StreamsRepository for concurrency limiting, type filtering & caching
        com.euthopiar.core.streams.StreamsRepository.load(
            type = resolvedType,
            videoId = mediaId,
            season = season,
            episode = episode,
            title = title,
            year = year,
            forceRefresh = true,
            originalLanguage = originalLanguage,
            imdbId = imdbId
        )

        val hasActiveDebrid = com.euthopiar.core.debrid.DebridManager.hasActiveDebrid()
        val seenUrls = mutableSetOf<String>()
        var sawLoading = false
        com.euthopiar.core.streams.StreamsRepository.uiState
            .transformWhile { state ->
                emit(state)
                if (state.isAnyLoading || state.groups.any { it.isLoading }) {
                    sawLoading = true
                }
                val isFinished = (sawLoading || state.allStreams.isNotEmpty() || state.emptyStateReason != null) &&
                        !state.isAnyLoading && state.groups.none { it.isLoading }
                !isFinished
            }
            .collect { state ->
                for (group in state.groups) {
                    for (stream in group.streams) {
                        val rawUrl = stream.playableDirectUrl ?: if (stream.isTorrentStream) stream.effectiveMagnetOrHash else null
                        val url = rawUrl ?: continue
                        if (seenUrls.add(url)) {
                            val rLabel = stream.quality ?: "Auto"
                            val cleanServerName = if (stream.addonName.isNotBlank()) {
                                "${stream.addonName} • ${stream.streamLabel}"
                            } else {
                                stream.streamLabel
                            }
                            val serverLabel = if (stream.isTorrentStream) "$cleanServerName (${if (hasActiveDebrid) "Debrid/P2P" else "P2P"})" else cleanServerName
                            val rawText = "${stream.name.orEmpty()} ${stream.title.orEmpty()} ${stream.quality.orEmpty()} ${stream.description.orEmpty()}"
                            val parsedHdr = when {
                                rawText.contains("Dolby Vision", ignoreCase = true) || rawText.contains("DoVi", ignoreCase = true) || Regex("""\bDV\b""").containsMatchIn(rawText) -> "DV"
                                rawText.contains("HDR10+", ignoreCase = true) || rawText.contains("HDR10Plus", ignoreCase = true) -> "HDR10+"
                                rawText.contains("HDR10", ignoreCase = true) -> "HDR10"
                                Regex("""\bHDR\b""", RegexOption.IGNORE_CASE).containsMatchIn(rawText) -> "HDR"
                                rawText.contains("HLG", ignoreCase = true) -> "HLG"
                                else -> null
                            }
                            val parsedCodec = when {
                                rawText.contains("AV1", ignoreCase = true) -> "AV1"
                                rawText.contains("HEVC", ignoreCase = true) || rawText.contains("H.265", ignoreCase = true) || rawText.contains("x265", ignoreCase = true) || rawText.contains("H265", ignoreCase = true) -> "HEVC"
                                rawText.contains("AVC", ignoreCase = true) || rawText.contains("H.264", ignoreCase = true) || rawText.contains("x264", ignoreCase = true) || rawText.contains("H264", ignoreCase = true) -> "AVC"
                                rawText.contains("VP9", ignoreCase = true) -> "VP9"
                                else -> null
                            }
                            val effectiveAddon = stream.addonName.ifBlank { stream.sourceName.orEmpty() }
                            val isM3u8Stream = stream.container == StreamContainer.HLS || url.contains(".m3u8", ignoreCase = true)
                            val resolvedContainer = if (stream.container != StreamContainer.AUTO) {
                                stream.container
                            } else if (isM3u8Stream) {
                                StreamContainer.HLS
                            } else {
                                StreamContainer.AUTO
                            }
                            val allSubs = (stream.externalSubtitles + stream.subtitles).distinctBy { it.url }
                            val subTracks = allSubs.map { sub ->
                                SubtitleTrack(
                                    language = sub.name ?: sub.language,
                                    url = sub.url,
                                    headers = sub.headers ?: emptyMap()
                                )
                            }

                            val src = StreamSource(
                                serverName = serverLabel,
                                url = url,
                                quality = stream.quality ?: "Auto",
                                isM3u8 = isM3u8Stream,
                                headers = stream.headers,
                                subtitles = subTracks,
                                infoHash = stream.infoHash,
                                seeders = stream.seeders,
                                fileIdx = stream.fileIdx,
                                addonId = stream.addonId,
                                season = season,
                                episode = episode,
                                responseHeaders = stream.responseHeaders,
                                resolutionLabel = rLabel,
                                matchScore = stream.matchScore,
                                size = stream.size,
                                isCached = if (hasActiveDebrid) stream.isCached else false,
                                hdrType = parsedHdr,
                                codec = parsedCodec,
                                addonName = effectiveAddon,
                                container = resolvedContainer,
                                trackers = stream.trackers,
                                isLive = stream.isLive || (mediaType != null && (mediaType.equals("live", ignoreCase = true) || mediaType.equals("channel", ignoreCase = true) || mediaType.equals("live_tv", ignoreCase = true)))
                            )
                            send(StreamEmission.SourceFound(src))
                            for (sub in subTracks) {
                                send(StreamEmission.SubtitleFound(sub))
                            }
                        }
                    }
                }
            }
    }

    /**
     * Resolves dedicated direct download options from the provider website.
     *
     * @param episodeData The media identifier (tmdbId or tmdbId:season:episode).
     * @param providerName Optional provider name override; falls back to active provider.
     */
    suspend fun getDownloadLinks(episodeData: String, providerName: String? = null): List<DownloadOption> = withContext(Dispatchers.IO) {
        val targetName = providerName
        if (targetName.isNullOrBlank() || targetName.equals("tmdb", ignoreCase = true) || targetName.equals("all", ignoreCase = true)) {
            val active = registry.activePlugin.value
            val direct = try {
                active.getDownloadLinks(episodeData)
            } catch (_: Throwable) {
                emptyList<DownloadOption>()
            }
            return@withContext direct.filter { it.url.isNotBlank() && it.url.startsWith("http") }.distinctBy { it.url }
        }
        val targetPlugin = targetName.let { registry.getPlugin(it) } ?: registry.activePlugin.value
        try {
            targetPlugin.getDownloadLinks(episodeData).filter { it.url.isNotBlank() && it.url.startsWith("http") }.distinctBy { it.url }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Resolves official studio clear logo URL for a media item.
     */
    suspend fun resolveLogo(mediaItem: MediaItem, providerName: String? = null): String? = withContext(Dispatchers.IO) {
        if (!mediaItem.logoUrl.isNullOrBlank()) return@withContext mediaItem.logoUrl
        val targetName = providerName ?: mediaItem.provider
        val targetPlugin = targetName?.let { registry.getPlugin(it) } ?: registry.activePlugin.value
        try {
            val logo = targetPlugin.resolveLogo(mediaItem)
            if (!logo.isNullOrBlank()) return@withContext logo
        } catch (_: Throwable) {}

        // Fallback to TMDB Bridge
        try {
            val isTv = mediaItem.type == MediaType.TV_SERIES || mediaItem.type == MediaType.ANIME
            val cleanId = mediaItem.id.removePrefix("tmdb:").removePrefix("tmdb/").trim()
            val tmdbId = if (cleanId.all { it.isDigit() } && cleanId.isNotEmpty()) {
                cleanId
            } else if (cleanId.startsWith("tt")) {
                TmdbBridge.findTmdbIdByImdb(DohDns.metadataClient, cleanId, isTv)
            } else {
                val clean = cleanReleaseTitle(mediaItem.title)
                TmdbBridge.searchTmdbId(DohDns.metadataClient, clean, mediaItem.year, isTv)
            }
            if (!tmdbId.isNullOrBlank()) {
                val logo = TmdbBridge.resolveLogo(DohDns.metadataClient, tmdbId, isTv)
                if (!logo.isNullOrBlank()) return@withContext logo
            }
        } catch (_: Throwable) {}

        null
    }

    /**
     * Resolves enriched metadata for a hero carousel item via TMDB Bridge,
     * including studio clear logo, IMDb rating, Rotten Tomatoes rating, content rating, and release year.
     */
    suspend fun resolveHeroItem(mediaItem: MediaItem, providerName: String? = null): MediaItem = withContext(Dispatchers.IO) {
        val targetName = providerName ?: mediaItem.provider
        val targetPlugin = targetName?.let { registry.getPlugin(it) } ?: registry.activePlugin.value
        var currentItem = mediaItem

        val isTv = currentItem.type == MediaType.TV_SERIES || currentItem.type == MediaType.ANIME
        val cleanId = currentItem.id.removePrefix("tmdb:").removePrefix("tmdb/").trim()
        val tmdbId = if (cleanId.all { it.isDigit() } && cleanId.isNotEmpty()) {
            cleanId
        } else if (cleanId.startsWith("tt")) {
            TmdbBridge.findTmdbIdByImdb(DohDns.metadataClient, cleanId, isTv)
        } else {
            val clean = cleanReleaseTitle(currentItem.title)
            TmdbBridge.searchTmdbId(DohDns.metadataClient, clean, currentItem.year, isTv)
        }

        // 1. Immediately fetch transparent clear logo from TMDB images cache or endpoint (<100ms)
        if (currentItem.logoUrl.isNullOrBlank() && !tmdbId.isNullOrBlank()) {
            try {
                val directLogo = TmdbBridge.resolveLogo(DohDns.metadataClient, tmdbId, isTv)
                if (!directLogo.isNullOrBlank()) {
                    currentItem = currentItem.copy(logoUrl = directLogo)
                }
            } catch (_: Throwable) {}
        }

        // 2. Fallback to plugin resolveLogo if needed
        if (currentItem.logoUrl.isNullOrBlank()) {
            try {
                val logo = targetPlugin.resolveLogo(currentItem)
                val isBogusMetahub = logo?.contains(Regex("""metahub\.space/logo/medium/\d+/img\.png""")) == true
                if (!logo.isNullOrBlank() && !isBogusMetahub) {
                    currentItem = currentItem.copy(logoUrl = logo)
                }
            } catch (_: Throwable) {}
        }

        // 3. Enrich additional metadata (rating, year) if missing
        if (!tmdbId.isNullOrBlank()) {
            try {
                val enriched = TmdbBridge.fetchEnrichedDetails(DohDns.metadataClient, tmdbId, isTv, targetPlugin.name)
                if (enriched != null) {
                    val currentPoster = currentItem.posterUrl
                    val upgradedPoster = if (!currentPoster.isNullOrBlank()) {
                        currentPoster.replace("/w500/", "/w780/")
                    } else {
                        enriched.posterUrl
                    }
                    val currentBackdrop = currentItem.backdropUrl
                    val upgradedBackdrop = if (!currentBackdrop.isNullOrBlank()) {
                        currentBackdrop.replace("/w500/", "/w1280/")
                    } else {
                        enriched.backdropUrl
                    }
                    currentItem = currentItem.withEnrichment(
                        year = enriched.year ?: currentItem.year,
                        rating = enriched.rating ?: currentItem.rating,
                        contentRating = enriched.contentRating ?: currentItem.contentRating,
                        rottenTomatoesRating = enriched.rottenTomatoesRating ?: currentItem.rottenTomatoesRating,
                        logoUrl = if (currentItem.logoUrl.isNullOrBlank()) enriched.logoUrl else currentItem.logoUrl,
                        posterUrl = upgradedPoster,
                        backdropUrl = upgradedBackdrop,
                        genres = if (currentItem.genres.isEmpty()) enriched.genres else currentItem.genres
                    )
                }
            } catch (_: Throwable) {}
        }

        currentItem
    }

    private fun cleanReleaseTitle(title: String): String {
        var s = title
        s = s.replace(Regex("""(?i)^\s*(?:Download|Watch|Free|Online)\b"""), " ")
        s = s.replace(Regex("""\[.*?\]"""), " ")
        s = s.replace(Regex("""\((?:19|20)\d{2}\)"""), " ")
        s = s.replace(Regex("""\(.*?\)"""), " ")
        s = s.replace(Regex("""(?i)\b(?:4k|2160p|1080p|720p|480p|uhd|fhd|hd|bluray|blu-ray|web-dl|webrip|brrip|dvdrip|hdtv|remux|hevc|x265|x264|avc|10bit|hdr|dovi|sdr|aac|atmos|ddp|truehd|multi|dual\s*audio|multi\s*audio|hindi|english|telugu|tamil|season\s*\d+|s\d+e\d+|s\d+|series|complete|repack|proper|clean|cleaned|uncut|extended|unrated|imax)\b"""), " ")
        s = s.replace(Regex("""[._-]"""), " ")
        val cleaned = s.replace(Regex("""\s+"""), " ").trim()
        return if (cleaned.length >= 2) cleaned else title.replace(Regex("""\[.*?\]|\(.*?\)|[._-]"""), " ").replace(Regex("""\s+"""), " ").trim()
    }

    companion object Default : ProviderRepository(PluginRegistry.Default)
}
