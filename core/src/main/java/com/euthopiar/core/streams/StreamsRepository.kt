package com.euthopiar.core.streams

import com.euthopiar.core.addons.isCustomStremioAddonId
import com.euthopiar.core.js.JsPluginStorage
import com.euthopiar.core.js.PluginRuntime
import com.euthopiar.core.js.normalizePluginType
import com.euthopiar.core.js.pluginContentId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object StreamsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(StreamsUiState())
    val uiState: StateFlow<StreamsUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var activeRequestKey: String? = null
    private var lastLoadedKey: String? = null
    private var lastLoadedHadImdb: Boolean = false
    private var lastLoadedHadTitle: Boolean = false

    fun requestToken(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        title: String? = null,
        year: Int? = null,
        imdbId: String? = null
    ): String = "$type::$videoId::$season::$episode::$title::$year::$imdbId"

    fun load(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        title: String? = null,
        year: Int? = null,
        forceRefresh: Boolean = false,
        originalLanguage: String? = null,
        imdbId: String? = null
    ) {
        val requestToken = requestToken(type, videoId, season, episode, title, year, imdbId)
        val currentState = _uiState.value

        if (!forceRefresh &&
            lastLoadedKey == requestToken &&
            currentState.allStreams.isNotEmpty() &&
            (imdbId == null || lastLoadedHadImdb) &&
            (title == null || lastLoadedHadTitle)
        ) {
            return
        }

        lastLoadedKey = requestToken
        lastLoadedHadImdb = !imdbId.isNullOrBlank()
        lastLoadedHadTitle = !title.isNullOrBlank()
        activeRequestKey = requestToken
        activeJob?.cancel()

        val normType = normalizePluginType(type)
        val isCustomId = isCustomStremioAddonId(videoId)
        val isLiveTv = normType == "live" || normType == "channel" || normType == "live_tv" || (normType == "tv" && isCustomId)
        val cleanTmdbId = if (isLiveTv) videoId.trim() else pluginContentId(videoId, season, episode)
        val effectiveOriginalLanguage = originalLanguage ?: if (normType == "anime") "ja" else null

        val storage = JsPluginStorage.getInstanceOrNull()
        val scrapers = if (isLiveTv) emptyList() else (storage?.getEnabledScrapersForType(normType) ?: emptyList())
        val addonRepo = com.euthopiar.core.addons.AddonRepository.getInstanceOrNull()
        val targetAddonType = if (isLiveTv) "live" else normType
        val addons = addonRepo?.getEnabledAddonsForType(targetAddonType, cleanTmdbId) ?: emptyList()

        if (scrapers.isEmpty() && addons.isEmpty()) {
            _uiState.value = StreamsUiState(
                requestToken = requestToken,
                isAnyLoading = false,
                emptyStateReason = StreamsEmptyStateReason.NoCompatibleAddons
            )
            return
        }

        // Initialize placeholder loading groups for scrapers and addons
        val scraperGroups = scrapers.map { scraper ->
            AddonStreamGroup(
                addonName = scraper.name,
                addonId = "plugin:${scraper.id}",
                streams = emptyList(),
                isLoading = true
            )
        }
        val addonGroups = addons.map { addon ->
            AddonStreamGroup(
                addonName = addon.displayTitle,
                addonId = addon.streamAddonInstanceId(addon.manifest?.id ?: addon.displayTitle),
                streams = emptyList(),
                isLoading = true
            )
        }
        val initialGroups = scraperGroups + addonGroups

        _uiState.value = StreamsUiState(
            requestToken = requestToken,
            groups = initialGroups,
            isAnyLoading = true,
            emptyStateReason = null
        )

        activeJob = scope.launch {
            val totalTasks = scrapers.size + addons.size
            val completions = Channel<StreamLoadCompletion>(capacity = Channel.BUFFERED)

            val baseId = if (isLiveTv || isCustomId) cleanTmdbId else cleanTmdbId.substringBefore(':')
            val isTv = !isLiveTv && (normType == "series" || normType == "tv")

            val canonicalTmdbId: String
            val effectiveImdbId: String?
            val effectiveTitle: String?
            val effectiveYear: Int?
            var cachedEnriched: com.euthopiar.core.util.TmdbBridge.EnrichedMetadata? = null

            if (isLiveTv || isCustomId) {
                canonicalTmdbId = cleanTmdbId
                effectiveImdbId = null
                effectiveTitle = title
                effectiveYear = year
            } else {
                var resolvedTmdbId = if (baseId.startsWith("tt")) {
                    com.euthopiar.core.util.TmdbBridge.findTmdbIdByImdb(
                        client = com.euthopiar.core.network.DohDns.metadataClient,
                        imdbId = baseId,
                        isTv = isTv
                    ) ?: baseId
                } else {
                    baseId
                }

                if (resolvedTmdbId.startsWith("mal:", ignoreCase = true) ||
                    resolvedTmdbId.startsWith("jikan:", ignoreCase = true) ||
                    (!resolvedTmdbId.all { it.isDigit() } && !resolvedTmdbId.startsWith("tt"))
                ) {
                    if (!title.isNullOrBlank()) {
                        val searched = com.euthopiar.core.util.TmdbBridge.searchTmdbId(
                            client = com.euthopiar.core.network.DohDns.metadataClient,
                            title = title,
                            year = year,
                            isTv = isTv
                        )
                        if (searched != null) {
                            resolvedTmdbId = searched
                        }
                    }
                }

                canonicalTmdbId = resolvedTmdbId

                val canonicalImdbId = when {
                    !imdbId.isNullOrBlank() && imdbId.startsWith("tt") -> imdbId
                    baseId.startsWith("tt") -> baseId
                    canonicalTmdbId.all { it.isDigit() } -> com.euthopiar.core.util.TmdbBridge.resolveImdbId(
                        tmdbId = canonicalTmdbId,
                        isTv = isTv
                    )
                    else -> null
                }

                cachedEnriched = com.euthopiar.core.util.TmdbBridge.getCachedEnrichedDetails(canonicalTmdbId, isTv = isTv)
                if (cachedEnriched == null && (canonicalTmdbId.all { it.isDigit() } || canonicalTmdbId.startsWith("tt"))) {
                    cachedEnriched = com.euthopiar.core.util.TmdbBridge.fetchEnrichedDetails(
                        client = com.euthopiar.core.network.DohDns.metadataClient,
                        tmdbId = canonicalTmdbId,
                        isTv = isTv,
                        providerName = "tmdb"
                    )
                }

                effectiveImdbId = canonicalImdbId?.takeIf { it.startsWith("tt") }
                    ?: cachedEnriched?.imdbId?.takeIf { it.startsWith("tt") }
                    ?: if (canonicalTmdbId.all { it.isDigit() }) {
                        com.euthopiar.core.util.TmdbBridge.resolveImdbId(
                            tmdbId = canonicalTmdbId,
                            isTv = isTv
                        )
                    } else null
                effectiveTitle = title ?: cachedEnriched?.title
                effectiveYear = year ?: cachedEnriched?.year
            }

            val isAnime = !isLiveTv && (normType == "anime" || effectiveOriginalLanguage.equals("ja", ignoreCase = true))
            val absoluteEpisode = if (!isLiveTv && season != null && episode != null) {
                val shouldCalculate = isAnime || (cachedEnriched?.isAnime == true)
                if (shouldCalculate) {
                    com.euthopiar.core.util.TmdbBridge.resolveAnimeAbsoluteEpisode(
                        tmdbId = canonicalTmdbId,
                        imdbId = effectiveImdbId,
                        season = season,
                        episode = episode,
                        seasonEpisodeCounts = cachedEnriched?.seasonEpisodeCounts ?: emptyMap()
                    )
                } else null
            } else null

            val effectiveAddonId = effectiveImdbId ?: canonicalTmdbId
            val addonStreamId = if (!isLiveTv && season != null && episode != null && season > 0) "$effectiveAddonId:$season:$episode" else effectiveAddonId

            val streamHints = cachedEnriched?.toMatchHints(
                isTv = isTv,
                season = season,
                episode = episode,
                absoluteEpisode = absoluteEpisode
            ) ?: com.euthopiar.core.model.MatchHints(
                tmdbId = canonicalTmdbId.toIntOrNull() ?: 0,
                imdbId = effectiveImdbId,
                type = if (isAnime) com.euthopiar.core.model.ContentType.ANIME else if (isTv) com.euthopiar.core.model.ContentType.TV_SERIES else com.euthopiar.core.model.ContentType.MOVIE,
                titles = setOfNotNull(effectiveTitle).filter { it.isNotBlank() }.toSet(),
                year = effectiveYear,
                season = season,
                episode = episode,
                absoluteEpisode = absoluteEpisode
            )

            // Concurrently query each enabled JS scraper
            scrapers.forEach { scraper ->
                val addonId = "plugin:${scraper.id}"
                val jsMediaType = when {
                    normType.equals("movie", ignoreCase = true) && (season == null || season <= 0) -> "movie"
                    scraper.supportedTypes.any { it.equals("tv", ignoreCase = true) } -> "tv"
                    scraper.supportedTypes.any { it.equals("series", ignoreCase = true) } -> "tv"
                    else -> normType.lowercase()
                }
                launch(Dispatchers.IO) {
                    try {
                        val results = withTimeoutOrNull(60_000L) {
                            PluginRuntime.executePlugin(
                                code = scraper.code,
                                tmdbId = canonicalTmdbId,
                                mediaType = jsMediaType,
                                season = season,
                                episode = episode,
                                scraperId = scraper.id,
                                imdbId = effectiveImdbId,
                                absoluteEpisode = absoluteEpisode,
                                title = effectiveTitle,
                                year = effectiveYear,
                                repoId = scraper.canonicalRepoId,
                                originalTitle = cachedEnriched?.originalTitle,
                                alternativeTitles = cachedEnriched?.alternativeTitles ?: emptySet(),
                                tvdbId = cachedEnriched?.tvdbId,
                                seasonYear = season?.let { cachedEnriched?.seasonYears?.get(it) },
                                seasonName = season?.let { cachedEnriched?.seasonNames?.get(it) },
                                malId = cachedEnriched?.malId,
                                anilistId = cachedEnriched?.anilistId,
                                romajiTitle = cachedEnriched?.romajiTitle,
                                englishTitle = cachedEnriched?.englishTitle,
                                seasonSpecificTitle = season?.let { cachedEnriched?.seasonSpecificTitles?.get(it)?.firstOrNull() }
                            )
                        } ?: emptyList()

                        val streamItems = results.map { res ->
                            res.toStreamItem(
                                scraper = scraper,
                                addonName = scraper.name,
                                addonId = addonId,
                                originalLanguage = effectiveOriginalLanguage
                            )
                        }
                        val verifiedStreams = com.euthopiar.core.matcher.MatchScorer.filterAndRankStreams(streamHints, streamItems)

                        completions.trySend(
                            StreamLoadCompletion.PluginScraper(
                                addonId = addonId,
                                addonName = scraper.name,
                                streams = verifiedStreams,
                                error = null
                            )
                        )
                    } catch (t: Throwable) {
                        if (t is kotlinx.coroutines.CancellationException) throw t
                        completions.trySend(
                            StreamLoadCompletion.PluginScraper(
                                addonId = addonId,
                                addonName = scraper.name,
                                streams = emptyList(),
                                error = t.localizedMessage ?: "Failed to resolve streams"
                            )
                        )
                    }
                }
            }

            // Concurrently query each enabled Stremio addon
            addons.forEach { addon ->
                val addonId = addon.streamAddonInstanceId(addon.manifest?.id ?: addon.displayTitle)
                val streamResource = addon.manifest?.resources?.firstOrNull { it.name.equals("stream", ignoreCase = true) }
                val streamTypes = streamResource?.types?.takeIf { it.isNotEmpty() } ?: addon.manifest?.types.orEmpty()
                val declaredTypes = streamTypes.map { it.lowercase() }
                val effectivePrefixes = if (!streamResource?.idPrefixes.isNullOrEmpty()) {
                    streamResource!!.idPrefixes
                } else {
                    addon.manifest?.idPrefixes.orEmpty()
                }
                val addonSupportsTt = effectivePrefixes.isEmpty() || effectivePrefixes.any { it.startsWith("tt", ignoreCase = true) }
                val matchesPrefixes = effectivePrefixes.any { prefix ->
                    cleanTmdbId.startsWith(prefix, ignoreCase = true) ||
                    (prefix.endsWith(":") && cleanTmdbId.startsWith(prefix.removeSuffix(":"), ignoreCase = true))
                }
                val isSeries = !isLiveTv && (normType == "tv" || normType == "series" || (season != null && episode != null && season > 0))

                val perAddonStreamId = when {
                    isLiveTv -> cleanTmdbId
                    matchesPrefixes || (isCustomId && !addonSupportsTt) -> {
                        if (isSeries && season != null && episode != null && season > 0 && !cleanTmdbId.endsWith(":$season:$episode")) {
                            "$cleanTmdbId:$season:$episode"
                        } else {
                            cleanTmdbId
                        }
                    }
                    !effectiveImdbId.isNullOrBlank() && effectiveImdbId.startsWith("tt") && addonSupportsTt -> {
                        if (isSeries && season != null && episode != null && season > 0 && !effectiveImdbId.endsWith(":$season:$episode")) {
                            "$effectiveImdbId:$season:$episode"
                        } else {
                            effectiveImdbId
                        }
                    }
                    isCustomId -> {
                        if (isSeries && season != null && episode != null && season > 0 && !cleanTmdbId.endsWith(":$season:$episode")) {
                            "$cleanTmdbId:$season:$episode"
                        } else {
                            cleanTmdbId
                        }
                    }
                    else -> addonStreamId
                }

                val targetType = when {
                    isLiveTv -> {
                        listOf("tv", "channel", "live", "live_tv").firstOrNull { it in declaredTypes } ?: "tv"
                    }
                    normType == "anime" && ("anime" in declaredTypes) -> {
                        "anime"
                    }
                    isSeries -> {
                        if ("series" in declaredTypes) "series" else if ("tv" in declaredTypes) "tv" else "series"
                    }
                    normType == "movie" -> {
                        if ("movie" in declaredTypes) "movie" else if ("movies" in declaredTypes) "movies" else "movie"
                    }
                    else -> {
                        listOf(normType, "movie").firstOrNull { it in declaredTypes } ?: normType
                    }
                }

                launch(Dispatchers.IO) {
                    try {
                        val url = com.euthopiar.core.addons.buildAddonResourceUrl(
                            manifestUrl = addon.manifestUrl,
                            resource = "stream",
                            type = targetType,
                            id = perAddonStreamId,
                            extraPathSegment = null
                        )
                        val response = com.euthopiar.core.js.network.httpRequestRaw("GET", url, emptyMap(), "", followRedirects = true)
                        val streamItems = if (response.status in 200..299 && response.body.isNotBlank()) {
                            StreamParser.parse(
                                payload = response.body,
                                addonName = addon.displayTitle,
                                addonId = addonId,
                                addonLogo = addon.manifest?.logoUrl,
                                originalLanguage = effectiveOriginalLanguage
                            )
                        } else emptyList()

                        val shouldBypassMatchScorer = isLiveTv ||
                            isCustomId ||
                            !perAddonStreamId.startsWith("tt", ignoreCase = true) ||
                            streamItems.any { it.isLive }

                        val verifiedStreams = if (shouldBypassMatchScorer) {
                            streamItems.map { item ->
                                if (item.matchScore <= 0.0) {
                                    item.copy(matchScore = 1.0, isLive = item.isLive || isLiveTv)
                                } else {
                                    item.copy(isLive = item.isLive || isLiveTv)
                                }
                            }
                        } else {
                            com.euthopiar.core.matcher.MatchScorer.filterAndRankStreams(streamHints, streamItems)
                        }

                        completions.trySend(
                            StreamLoadCompletion.PluginScraper(
                                addonId = addonId,
                                addonName = addon.displayTitle,
                                streams = verifiedStreams,
                                error = null
                            )
                        )
                    } catch (t: Throwable) {
                        if (t is kotlinx.coroutines.CancellationException) throw t
                        completions.trySend(
                            StreamLoadCompletion.PluginScraper(
                                addonId = addonId,
                                addonName = addon.displayTitle,
                                streams = emptyList(),
                                error = t.localizedMessage ?: "Failed to resolve addon streams"
                            )
                        )
                    }
                }
            }

            repeat(totalTasks) {
                val completion = completions.receive()
                if (completion is StreamLoadCompletion.PluginScraper) {
                    _uiState.update { current ->
                        val updatedGroups = current.groups.map { group ->
                            if (group.addonId != completion.addonId) {
                                group
                            } else {
                                val mergedStreams = if (isLiveTv || isCustomId) {
                                    (group.streams + completion.streams)
                                } else {
                                    (group.streams + completion.streams).sortedForGroupedDisplay()
                                }
                                group.copy(
                                    streams = mergedStreams,
                                    isLoading = false,
                                    error = if (mergedStreams.isEmpty()) completion.error else null
                                )
                            }
                        }

                        val stillLoading = updatedGroups.any { it.isLoading }
                        val allAvailable = updatedGroups.flatMap { it.streams }
                        val bestAutoPlay = current.autoPlayStream ?: allAvailable.firstOrNull { it.playableDirectUrl != null }

                        current.copy(
                            groups = updatedGroups,
                            isAnyLoading = stillLoading,
                            autoPlayStream = bestAutoPlay,
                            emptyStateReason = if (!stillLoading && allAvailable.isEmpty()) {
                                StreamsEmptyStateReason.NoStreamsFound
                            } else null
                        )
                    }
                }
            }
        }
    }

    fun reload(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        title: String? = null,
        year: Int? = null,
        imdbId: String? = null
    ) {
        load(type, videoId, season, episode, title, year, forceRefresh = true, imdbId = imdbId)
    }

    fun clear() {
        activeJob?.cancel()
        activeJob = null
        activeRequestKey = null
        _uiState.value = StreamsUiState()
    }
}
