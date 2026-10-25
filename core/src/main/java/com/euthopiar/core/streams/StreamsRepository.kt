package com.euthopiar.core.streams

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
        val cleanTmdbId = pluginContentId(videoId, season, episode)
        val effectiveOriginalLanguage = originalLanguage ?: if (normType == "anime") "ja" else null

        val storage = JsPluginStorage.getInstanceOrNull()
        val scrapers = storage?.getEnabledScrapersForType(normType) ?: emptyList()
        val addonRepo = com.euthopiar.core.addons.AddonRepository.getInstanceOrNull()
        val addons = addonRepo?.getEnabledAddonsForType(normType, cleanTmdbId) ?: emptyList()

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

            val baseId = cleanTmdbId.substringBefore(':')
            val isTv = normType == "series" || normType == "tv"

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

            val canonicalTmdbId = resolvedTmdbId

            val canonicalImdbId = when {
                !imdbId.isNullOrBlank() && imdbId.startsWith("tt") -> imdbId
                baseId.startsWith("tt") -> baseId
                canonicalTmdbId.all { it.isDigit() } -> com.euthopiar.core.util.TmdbBridge.resolveImdbId(
                    tmdbId = canonicalTmdbId,
                    isTv = isTv
                )
                else -> null
            }

            var cachedEnriched = com.euthopiar.core.util.TmdbBridge.getCachedEnrichedDetails(canonicalTmdbId, isTv = isTv)
            if (cachedEnriched == null && (canonicalTmdbId.all { it.isDigit() } || canonicalTmdbId.startsWith("tt"))) {
                cachedEnriched = com.euthopiar.core.util.TmdbBridge.fetchEnrichedDetails(
                    client = com.euthopiar.core.network.DohDns.metadataClient,
                    tmdbId = canonicalTmdbId,
                    isTv = isTv,
                    providerName = "tmdb"
                )
            }

            val effectiveImdbId = canonicalImdbId?.takeIf { it.startsWith("tt") }
                ?: cachedEnriched?.imdbId?.takeIf { it.startsWith("tt") }
                ?: if (canonicalTmdbId.all { it.isDigit() }) {
                    com.euthopiar.core.util.TmdbBridge.resolveImdbId(
                        tmdbId = canonicalTmdbId,
                        isTv = isTv
                    )
                } else null
            val effectiveTitle = title ?: cachedEnriched?.title
            val effectiveYear = year ?: cachedEnriched?.year

            val isAnime = normType == "anime" || effectiveOriginalLanguage.equals("ja", ignoreCase = true)
            val absoluteEpisode = if (season != null && episode != null) {
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
            val addonStreamId = if (season != null && episode != null) "$effectiveAddonId:$season:$episode" else effectiveAddonId

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
            val stremioAddonStreamId = if (!effectiveImdbId.isNullOrBlank() && effectiveImdbId.startsWith("tt")) {
                if (season != null && episode != null) "$effectiveImdbId:$season:$episode" else effectiveImdbId
            } else if (effectiveImdbId == null && (
                videoId.startsWith("kitsu:", ignoreCase = true) ||
                videoId.startsWith("ustv:", ignoreCase = true) ||
                videoId.startsWith("yt_id:", ignoreCase = true) ||
                videoId.contains(":") ||
                !videoId.all { it.isDigit() }
            )) {
                videoId
            } else {
                addonStreamId
            }

            addons.forEach { addon ->
                val addonId = addon.streamAddonInstanceId(addon.manifest?.id ?: addon.displayTitle)
                val streamResource = addon.manifest?.resources?.firstOrNull { it.name.equals("stream", ignoreCase = true) }
                val streamTypes = streamResource?.types ?: addon.manifest?.types.orEmpty()
                val exclusivelyAnime = streamTypes.isNotEmpty() && streamTypes.all { it.equals("anime", ignoreCase = true) }

                val targetType = if (stremioAddonStreamId.startsWith("tt", ignoreCase = true)) {
                    if (exclusivelyAnime) {
                        "anime"
                    } else {
                        if (season != null && episode != null) "series" else "movie"
                    }
                } else if (effectiveImdbId == null) {
                    val supportedMatchingType = streamTypes.firstOrNull { st ->
                        st.equals(normType, ignoreCase = true) ||
                        (normType == "tv" && st.equals("series", ignoreCase = true)) ||
                        (normType == "anime" && (st.equals("anime", ignoreCase = true) || st.equals("series", ignoreCase = true)))
                    }
                    supportedMatchingType ?: if (normType == "tv") "series" else normType
                } else {
                    if (normType == "tv") "series" else normType
                }

                launch(Dispatchers.IO) {
                    try {
                        val url = com.euthopiar.core.addons.buildAddonResourceUrl(
                            manifestUrl = addon.manifestUrl,
                            resource = "stream",
                            type = targetType,
                            id = stremioAddonStreamId,
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
                        val verifiedStreams = com.euthopiar.core.matcher.MatchScorer.filterAndRankStreams(streamHints, streamItems)

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
                                val mergedStreams = (group.streams + completion.streams).sortedForGroupedDisplay()
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
