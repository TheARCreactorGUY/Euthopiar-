with open('test_eups_repo/sources/CinejoyPlugin.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add mediaTypeCache
if 'private val mediaTypeCache' not in content:
    content = content.replace(
        'private val logoCache = ConcurrentHashMap<String, String>()',
        'private val logoCache = ConcurrentHashMap<String, String>()\n    private val mediaTypeCache = ConcurrentHashMap<String, MediaType>()'
    )

# 2. Add extractTmdbId helper before parseQualityHeight
if 'private fun extractTmdbId' not in content:
    extract_fn = '''    private fun extractTmdbId(input: String): String {
        val withoutParams = input.substringBefore("?").substringBefore("#")
        val segment = withoutParams.substringAfterLast("/").substringBefore(":")
        val numeric = segment.substringBefore("-").trim()
        val result = if (numeric.all { it.isDigit() } && numeric.isNotEmpty()) numeric else segment.filter { it.isDigit() }
        return result.ifBlank { input.filter { it.isDigit() } }
    }

'''
    content = content.replace(
        '    private fun parseQualityHeight(label: String): Int = when {',
        extract_fn + '    private fun parseQualityHeight(label: String): Int = when {'
    )

# 3. Clear mediaTypeCache in destroy()
if 'mediaTypeCache.clear()' not in content:
    content = content.replace(
        'logoCache.clear()',
        'logoCache.clear()\n        mediaTypeCache.clear()'
    )

# 4. Replace getStreamFlow header
old_stream_flow = '''    override fun getStreamFlow(episodeData: String): Flow<StreamEmission> = channelFlow {
        val tmdbId: String
        val isTv: Boolean
        val season: Int?
        val episode: Int?

        if (episodeData.contains(":")) {
            val parts = episodeData.split(":")
            tmdbId = parts[0]
            season = parts.getOrNull(1)?.toIntOrNull()
            episode = parts.getOrNull(2)?.toIntOrNull()
            isTv = true
        } else {
            tmdbId = episodeData
            season = null
            episode = null
            isTv = false
        }'''

new_stream_flow = '''    override fun getStreamFlow(episodeData: String): Flow<StreamEmission> = channelFlow {
        val rawInput = episodeData.trim()
        val tmdbId = extractTmdbId(rawInput)

        val hasColon = rawInput.contains(":")
        val isTvHint = rawInput.contains("/tv/") || rawInput.contains("tv", ignoreCase = true) || rawInput.contains("series", ignoreCase = true)

        val season: Int?
        val episode: Int?
        val isTv: Boolean

        if (hasColon) {
            val parts = rawInput.split(":")
            season = parts.getOrNull(1)?.toIntOrNull() ?: 1
            episode = parts.getOrNull(2)?.toIntOrNull() ?: 1
            isTv = true
        } else if (isTvHint || mediaTypeCache[tmdbId] == MediaType.TV_SERIES || mediaTypeCache[rawInput] == MediaType.TV_SERIES) {
            season = 1
            episode = 1
            isTv = true
        } else {
            season = null
            episode = null
            isTv = false
        }'''

content = content.replace(old_stream_flow, new_stream_flow)

# 5. Replace whole Helios launch block
old_helios_block = '''        // 2. Helios Engine (Multi-Quality HLS via AES-GCM)
        launch {
            try {
                val heliosUrl = if (isTv && season != null && episode != null) {
                    "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=tv&seasonId=$season&episodeId=$episode"
                } else {
                    "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=movie"
                }

                val req = Request.Builder()
                    .url(heliosUrl)
                    .header("Referer", "https://cinejoy.pk/")
                    .header("Origin", "https://cinejoy.pk")
                    .header("User-Agent", defaultHeaders["User-Agent"]!!)
                    .build()

                http.meta.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val root = json.parseToJsonElement(body).jsonObject
                        val sources = root["sources"]?.jsonObject ?: return@use

                        for ((serverName, serverVal) in sources) {
                            val sObj = serverVal.jsonObject
                            val encUrl = sObj["url"]?.jsonPrimitive?.contentOrNull ?: continue
                            val decryptedUrl = decryptHeliosUrl(encUrl) ?: continue
                            if (!decryptedUrl.startsWith("http")) continue

                            val sourceId = "cinejoy:helios:$tmdbId:$serverName"
                            val virtualUrl = "eup://cinejoy/$sourceId/master.m3u8"

                            // Cache for TokenStore refresh()
                            val eupSource = EupStreamSource(
                                id = sourceId,
                                serverId = "helios_$serverName",
                                serverLabel = "Helios ($serverName Auto)",
                                url = decryptedUrl,
                                kind = StreamKind.HLS,
                                headers = HeaderPolicy(
                                    sticky = mapOf(
                                        "Referer" to "https://stream.hls.lol/",
                                        "Origin" to "https://stream.hls.lol",
                                        "User-Agent" to defaultHeaders["User-Agent"]!!
                                    )
                                ),
                                video = VideoInfo(height = 1080),
                                expiresAtEpochMs = System.currentTimeMillis() + (20 * 60_000L),
                                refreshHandle = "v1|1|$serverName"
                            )
                            sourceEntries[sourceId] = CachedSourceEntry(
                                tmdbId = tmdbId,
                                isTv = isTv,
                                season = season,
                                episode = episode,
                                serverId = serverName,
                                gen = 1,
                                realUrl = decryptedUrl,
                                realSource = eupSource,
                                expiresAtMs = System.currentTimeMillis() + (20 * 60_000L)
                            )

                            // Emit Direct Quality Variants from ?q= parameter
                            val qParam = decryptedUrl.substringAfter("?q=", "").substringBefore("&")
                            if (qParam.isNotBlank()) {
                                try {
                                    val decodedBytes = safeDecodeBase64(qParam)
                                    val decodedJsonStr = String(decodedBytes, Charsets.UTF_8)
                                    val qRoot = json.parseToJsonElement(decodedJsonStr).jsonObject
                                    val vArr = qRoot["v"]?.jsonArray
                                    val customHeadersObj = qRoot["headers"]?.jsonObject
                                    val streamHeaders = mutableMapOf<String, String>()
                                    customHeadersObj?.forEach { (k, v) ->
                                        v.jsonPrimitive.contentOrNull?.let { streamHeaders[k] = it }
                                    }
                                    val reqHeaders = mapOf(
                                        "Referer" to (streamHeaders["referer"] ?: "https://www.movy.sx/"),
                                        "Origin" to (streamHeaders["origin"] ?: "https://www.movy.sx"),
                                        "User-Agent" to defaultHeaders["User-Agent"]!!
                                    )

                                    if (vArr != null) {
                                        for (vElem in vArr) {
                                            val vObj = vElem.jsonObject
                                            val vUrl = vObj["u"]?.jsonPrimitive?.contentOrNull ?: continue
                                            val vQuality = vObj["q"]?.jsonPrimitive?.contentOrNull ?: "1080p"
                                            val qualityLabel = when {
                                                vQuality.contains("1080") -> "1080p FHD"
                                                vQuality.contains("720") -> "720p HD"
                                                vQuality.contains("480") -> "480p SD"
                                                else -> vQuality
                                            }

                                            val src = CoreStreamSource(
                                                url = vUrl,
                                                serverName = "Helios ($qualityLabel)",
                                                resolutionLabel = qualityLabel,
                                                quality = "Cinejoy Helios ($qualityLabel HLS)",
                                                isM3u8 = true,
                                                releaseType = AudioReleaseType.ORIGINAL,
                                                headers = reqHeaders
                                            )
                                            val streamKey = "${src.serverName}:${src.resolutionLabel}:${src.url}"
                                            if (emittedStreamKeys.add(streamKey)) {
                                                send(StreamEmission.SourceFound(src))
                                            }
                                        }
                                    }
                                } catch (_: Throwable) {}
                            }

                            // Emit Master HLS stream
                            val heliosMasterHeaders = mapOf(
                                "Referer" to "https://stream.hls.lol/",
                                "Origin" to "https://stream.hls.lol",
                                "User-Agent" to defaultHeaders["User-Agent"]!!
                            )
                            val masterSrc = CoreStreamSource(
                                url = decryptedUrl,
                                serverName = "Helios ($serverName Auto)",
                                resolutionLabel = "Auto",
                                quality = "Cinejoy Helios ($serverName Auto HLS)",
                                isM3u8 = true,
                                releaseType = AudioReleaseType.ORIGINAL,
                                headers = heliosMasterHeaders
                            )
                            val masterKey = "${masterSrc.serverName}:${masterSrc.resolutionLabel}:${masterSrc.url}"
                            if (emittedStreamKeys.add(masterKey)) {
                                send(StreamEmission.SourceFound(masterSrc))
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}
        }'''

new_helios_block = '''        // 2. Helios Engine (Multi-Quality HLS via AES-GCM)
        launch {
            try {
                fun fetchHeliosSources(forTv: Boolean, s: Int?, e: Int?): Map<String, JsonElement> {
                    val hUrl = if (forTv && s != null && e != null) {
                        "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=tv&seasonId=$s&episodeId=$e"
                    } else {
                        "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=movie"
                    }
                    val hReq = Request.Builder()
                        .url(hUrl)
                        .header("Referer", "https://cinejoy.pk/")
                        .header("Origin", "https://cinejoy.pk")
                        .header("User-Agent", defaultHeaders["User-Agent"]!!)
                        .build()
                    return try {
                        http.meta.newCall(hReq).execute().use { hResp ->
                            if (!hResp.isSuccessful) return emptyMap()
                            val hBody = hResp.body?.string().orEmpty()
                            val hRoot = json.parseToJsonElement(hBody).jsonObject
                            hRoot["sources"]?.jsonObject?.toMap() ?: emptyMap()
                        }
                    } catch (_: Throwable) {
                        emptyMap()
                    }
                }

                var sources = fetchHeliosSources(isTv, season, episode)
                if (sources.isEmpty() && !isTv) {
                    sources = fetchHeliosSources(true, 1, 1)
                } else if (sources.isEmpty() && isTv) {
                    sources = fetchHeliosSources(false, null, null)
                }

                for ((serverName, serverVal) in sources) {
                    val sObj = serverVal.jsonObject
                    val encUrl = sObj["url"]?.jsonPrimitive?.contentOrNull ?: continue
                    val decryptedUrl = decryptHeliosUrl(encUrl) ?: continue
                    if (!decryptedUrl.startsWith("http")) continue

                    val sourceId = "cinejoy:helios:$tmdbId:$serverName"
                    val virtualUrl = "eup://cinejoy/$sourceId/master.m3u8"

                    // Cache for TokenStore refresh()
                    val eupSource = EupStreamSource(
                        id = sourceId,
                        serverId = "helios_$serverName",
                        serverLabel = "Helios ($serverName Auto)",
                        url = decryptedUrl,
                        kind = StreamKind.HLS,
                        headers = HeaderPolicy(
                            sticky = mapOf(
                                "Referer" to "https://stream.hls.lol/",
                                "Origin" to "https://stream.hls.lol",
                                "User-Agent" to defaultHeaders["User-Agent"]!!
                            )
                        ),
                        video = VideoInfo(height = 1080),
                        expiresAtEpochMs = System.currentTimeMillis() + (20 * 60_000L),
                        refreshHandle = "v1|1|$serverName"
                    )
                    sourceEntries[sourceId] = CachedSourceEntry(
                        tmdbId = tmdbId,
                        isTv = isTv,
                        season = season,
                        episode = episode,
                        serverId = serverName,
                        gen = 1,
                        realUrl = decryptedUrl,
                        realSource = eupSource,
                        expiresAtMs = System.currentTimeMillis() + (20 * 60_000L)
                    )

                    // Emit Direct Quality Variants from ?q= parameter
                    val qParam = decryptedUrl.substringAfter("?q=", "").substringBefore("&")
                    if (qParam.isNotBlank()) {
                        try {
                            val decodedBytes = safeDecodeBase64(qParam)
                            val decodedJsonStr = String(decodedBytes, Charsets.UTF_8)
                            val qRoot = json.parseToJsonElement(decodedJsonStr).jsonObject
                            val vArr = qRoot["v"]?.jsonArray
                            val customHeadersObj = qRoot["headers"]?.jsonObject
                            val streamHeaders = mutableMapOf<String, String>()
                            customHeadersObj?.forEach { (k, v) ->
                                v.jsonPrimitive.contentOrNull?.let { streamHeaders[k] = it }
                            }
                            val reqHeaders = mapOf(
                                "Referer" to (streamHeaders["referer"] ?: "https://www.movy.sx/"),
                                "Origin" to (streamHeaders["origin"] ?: "https://www.movy.sx"),
                                "User-Agent" to defaultHeaders["User-Agent"]!!
                            )

                            if (vArr != null) {
                                for (vElem in vArr) {
                                    val vObj = vElem.jsonObject
                                    val vUrl = vObj["u"]?.jsonPrimitive?.contentOrNull ?: continue
                                    val vQuality = vObj["q"]?.jsonPrimitive?.contentOrNull ?: "1080p"
                                    val qualityLabel = when {
                                        vQuality.contains("1080") -> "1080p FHD"
                                        vQuality.contains("720") -> "720p HD"
                                        vQuality.contains("480") -> "480p SD"
                                        else -> vQuality
                                    }

                                    val src = CoreStreamSource(
                                        url = vUrl,
                                        serverName = "Helios ($qualityLabel)",
                                        resolutionLabel = qualityLabel,
                                        quality = "Cinejoy Helios ($qualityLabel HLS)",
                                        isM3u8 = true,
                                        releaseType = AudioReleaseType.ORIGINAL,
                                        headers = reqHeaders
                                    )
                                    val streamKey = "${src.serverName}:${src.resolutionLabel}:${src.url}"
                                    if (emittedStreamKeys.add(streamKey)) {
                                        send(StreamEmission.SourceFound(src))
                                    }
                                }
                            }
                        } catch (_: Throwable) {}
                    }

                    // Emit Master HLS stream
                    val heliosMasterHeaders = mapOf(
                        "Referer" to "https://stream.hls.lol/",
                        "Origin" to "https://stream.hls.lol",
                        "User-Agent" to defaultHeaders["User-Agent"]!!
                    )
                    val masterSrc = CoreStreamSource(
                        url = decryptedUrl,
                        serverName = "Helios ($serverName Auto)",
                        resolutionLabel = "Auto",
                        quality = "Cinejoy Helios ($serverName Auto HLS)",
                        isM3u8 = true,
                        releaseType = AudioReleaseType.ORIGINAL,
                        headers = heliosMasterHeaders
                    )
                    val masterKey = "${masterSrc.serverName}:${masterSrc.resolutionLabel}:${masterSrc.url}"
                    if (emittedStreamKeys.add(masterKey)) {
                        send(StreamEmission.SourceFound(masterSrc))
                    }
                }
            } catch (_: Throwable) {}
        }'''

content = content.replace(old_helios_block, new_helios_block)

# 6. Replace Lisbon requestStream with fallback
old_lisbon = '''        // 3. Cinejoy Native Lisbon Engine (HTTP/1.1 CDN Verified & Virtual eup:// URL)
        launch {
            try {
                val lisbonJson = withTimeoutOrNull(15000L) {
                    CinejoyWasmEngine.requestStream(
                        client = http.cdn,
                        server = "Lisbon",
                        type = if (isTv) "tv" else "movie",
                        tmdbId = tmdbId,
                        season = season,
                        episode = episode
                    )
                }'''

new_lisbon = '''        // 3. Cinejoy Native Lisbon Engine (HTTP/1.1 CDN Verified & Virtual eup:// URL)
        launch {
            try {
                var lisbonJson = withTimeoutOrNull(15000L) {
                    CinejoyWasmEngine.requestStream(
                        client = http.cdn,
                        server = "Lisbon",
                        type = if (isTv) "tv" else "movie",
                        tmdbId = tmdbId,
                        season = season,
                        episode = episode
                    )
                }
                if ((lisbonJson == null || lisbonJson.contains("error")) && !isTv) {
                    lisbonJson = withTimeoutOrNull(15000L) {
                        CinejoyWasmEngine.requestStream(
                            client = http.cdn,
                            server = "Lisbon",
                            type = "tv",
                            tmdbId = tmdbId,
                            season = 1,
                            episode = 1
                        )
                    }
                } else if ((lisbonJson == null || lisbonJson.contains("error")) && isTv) {
                    lisbonJson = withTimeoutOrNull(15000L) {
                        CinejoyWasmEngine.requestStream(
                            client = http.cdn,
                            server = "Lisbon",
                            type = "movie",
                            tmdbId = tmdbId
                        )
                    }
                }'''

content = content.replace(old_lisbon, new_lisbon)

# 7. Replace Nebula requestStream with fallback
old_nebula = '''        // 4. Cinejoy Native Nebula Engine (1080p Master & Adaptive Variants via Wasm)
        launch {
            try {
                val nebulaJson = withTimeoutOrNull(15000L) {
                    CinejoyWasmEngine.requestStream(
                        client = http.cdn,
                        server = "Nebula",
                        type = if (isTv) "tv" else "movie",
                        tmdbId = tmdbId,
                        season = season,
                        episode = episode
                    )
                }'''

new_nebula = '''        // 4. Cinejoy Native Nebula Engine (1080p Master & Adaptive Variants via Wasm)
        launch {
            try {
                var nebulaJson = withTimeoutOrNull(15000L) {
                    CinejoyWasmEngine.requestStream(
                        client = http.cdn,
                        server = "Nebula",
                        type = if (isTv) "tv" else "movie",
                        tmdbId = tmdbId,
                        season = season,
                        episode = episode
                    )
                }
                if ((nebulaJson == null || nebulaJson.contains("error")) && !isTv) {
                    nebulaJson = withTimeoutOrNull(15000L) {
                        CinejoyWasmEngine.requestStream(
                            client = http.cdn,
                            server = "Nebula",
                            type = "tv",
                            tmdbId = tmdbId,
                            season = 1,
                            episode = 1
                        )
                    }
                } else if ((nebulaJson == null || nebulaJson.contains("error")) && isTv) {
                    nebulaJson = withTimeoutOrNull(15000L) {
                        CinejoyWasmEngine.requestStream(
                            client = http.cdn,
                            server = "Nebula",
                            type = "movie",
                            tmdbId = tmdbId
                        )
                    }
                }'''

content = content.replace(old_nebula, new_nebula)

# 8. Replace whole getDownloadLinks block
old_dl_block = '''    override suspend fun getDownloadLinks(episodeData: String): List<DownloadOption> = withContext(Dispatchers.IO) {
        val tmdbId: String
        val isTv: Boolean
        val season: Int?
        val episode: Int?

        if (episodeData.contains(":")) {
            val parts = episodeData.split(":")
            tmdbId = parts[0]
            season = parts.getOrNull(1)?.toIntOrNull()
            episode = parts.getOrNull(2)?.toIntOrNull()
            isTv = true
        } else {
            tmdbId = episodeData
            season = null
            episode = null
            isTv = false
        }

        val options = mutableListOf<DownloadOption>()
        try {
            val heliosUrl = if (isTv && season != null && episode != null) {
                "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=tv&seasonId=$season&episodeId=$episode"
            } else {
                "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=movie"
            }

            val req = Request.Builder().url(heliosUrl).header("User-Agent", defaultHeaders["User-Agent"]!!).build()
            http.meta.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val root = json.parseToJsonElement(body).jsonObject
                    val sources = root["sources"]?.jsonObject
                    if (sources != null) {
                        val masterVal = sources["Moscow"]?.jsonObject ?: sources.values.firstOrNull()?.jsonObject
                        val encUrl = masterVal?.get("url")?.jsonPrimitive?.contentOrNull
                        if (encUrl != null) {
                            val dec = decryptHeliosUrl(encUrl)
                            if (dec != null && dec.startsWith("http")) {
                                val qParam = dec.substringAfter("?q=", "").substringBefore("&")
                                if (qParam.isNotBlank()) {
                                    val decodedBytes = safeDecodeBase64(qParam)
                                    val decodedJsonStr = String(decodedBytes, Charsets.UTF_8)
                                    val qRoot = json.parseToJsonElement(decodedJsonStr).jsonObject
                                    val vArr = qRoot["v"]?.jsonArray
                                    val customHeadersObj = qRoot["headers"]?.jsonObject
                                    val streamHeaders = mutableMapOf<String, String>()
                                    customHeadersObj?.forEach { (k, v) ->
                                        v.jsonPrimitive.contentOrNull?.let { streamHeaders[k] = it }
                                    }
                                    val reqHeaders = mapOf(
                                        "Referer" to (streamHeaders["referer"] ?: "https://www.movy.sx/"),
                                        "Origin" to (streamHeaders["origin"] ?: "https://www.movy.sx"),
                                        "User-Agent" to defaultHeaders["User-Agent"]!!
                                    )

                                    if (vArr != null) {
                                        for (vElem in vArr) {
                                            val vObj = vElem.jsonObject
                                            val vUrl = vObj["u"]?.jsonPrimitive?.contentOrNull ?: continue
                                            val vQuality = vObj["q"]?.jsonPrimitive?.contentOrNull ?: "1080p"
                                            val (qualityLabel, estimatedSize) = when {
                                                vQuality.contains("1080") -> "1080p FHD" to "2.4 GB"
                                                vQuality.contains("720") -> "720p HD" to "1.2 GB"
                                                vQuality.contains("480") -> "480p SD" to "650 MB"
                                                else -> vQuality to "1.0 GB"
                                            }
                                            options.add(
                                                DownloadOption(
                                                    title = "Cinejoy Direct ($qualityLabel)",
                                                    quality = qualityLabel,
                                                    size = estimatedSize,
                                                    url = vUrl,
                                                    source = "Cinejoy Helios CDN",
                                                    provider = name,
                                                    headers = reqHeaders
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        options.distinctBy { it.url }
    }'''

new_dl_block = '''    override suspend fun getDownloadLinks(episodeData: String): List<DownloadOption> = withContext(Dispatchers.IO) {
        val rawInput = episodeData.trim()
        val tmdbId = extractTmdbId(rawInput)

        val hasColon = rawInput.contains(":")
        val isTvHint = rawInput.contains("/tv/") || rawInput.contains("tv", ignoreCase = true) || rawInput.contains("series", ignoreCase = true)

        val season: Int?
        val episode: Int?
        val isTv: Boolean

        if (hasColon) {
            val parts = rawInput.split(":")
            season = parts.getOrNull(1)?.toIntOrNull() ?: 1
            episode = parts.getOrNull(2)?.toIntOrNull() ?: 1
            isTv = true
        } else if (isTvHint || mediaTypeCache[tmdbId] == MediaType.TV_SERIES || mediaTypeCache[rawInput] == MediaType.TV_SERIES) {
            season = 1
            episode = 1
            isTv = true
        } else {
            season = null
            episode = null
            isTv = false
        }

        val options = mutableListOf<DownloadOption>()
        try {
            val heliosUrl = if (isTv && season != null && episode != null) {
                "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=tv&seasonId=$season&episodeId=$episode"
            } else {
                "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=movie"
            }

            val req = Request.Builder().url(heliosUrl).header("User-Agent", defaultHeaders["User-Agent"]!!).build()
            http.meta.newCall(req).execute().use { resp ->
                val rootObj = if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    json.parseToJsonElement(body).jsonObject
                } else null

                var sources = rootObj?.get("sources")?.jsonObject
                if (sources == null || sources.isEmpty()) {
                    val altUrl = if (isTv) {
                        "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=movie"
                    } else {
                        "https://stream.hls.lol/helios?tmdbId=$tmdbId&type=tv&seasonId=1&episodeId=1"
                    }
                    val altReq = Request.Builder().url(altUrl).header("User-Agent", defaultHeaders["User-Agent"]!!).build()
                    try {
                        http.meta.newCall(altReq).execute().use { altResp ->
                            if (altResp.isSuccessful) {
                                val altBody = altResp.body?.string().orEmpty()
                                sources = json.parseToJsonElement(altBody).jsonObject["sources"]?.jsonObject
                            }
                        }
                    } catch (_: Throwable) {}
                }

                val sMap = sources
                if (sMap != null) {
                    val masterVal = sMap["Moscow"]?.jsonObject ?: sMap.values.firstOrNull()?.jsonObject
                    val encUrl = masterVal?.get("url")?.jsonPrimitive?.contentOrNull
                    if (encUrl != null) {
                        val dec = decryptHeliosUrl(encUrl)
                        if (dec != null && dec.startsWith("http")) {
                            val qParam = dec.substringAfter("?q=", "").substringBefore("&")
                            if (qParam.isNotBlank()) {
                                val decodedBytes = safeDecodeBase64(qParam)
                                val decodedJsonStr = String(decodedBytes, Charsets.UTF_8)
                                val qRoot = json.parseToJsonElement(decodedJsonStr).jsonObject
                                val vArr = qRoot["v"]?.jsonArray
                                val customHeadersObj = qRoot["headers"]?.jsonObject
                                val streamHeaders = mutableMapOf<String, String>()
                                customHeadersObj?.forEach { (k, v) ->
                                    v.jsonPrimitive.contentOrNull?.let { streamHeaders[k] = it }
                                }
                                val reqHeaders = mapOf(
                                    "Referer" to (streamHeaders["referer"] ?: "https://www.movy.sx/"),
                                    "Origin" to (streamHeaders["origin"] ?: "https://www.movy.sx"),
                                    "User-Agent" to defaultHeaders["User-Agent"]!!
                                )

                                if (vArr != null) {
                                    for (vElem in vArr) {
                                        val vObj = vElem.jsonObject
                                        val vUrl = vObj["u"]?.jsonPrimitive?.contentOrNull ?: continue
                                        val vQuality = vObj["q"]?.jsonPrimitive?.contentOrNull ?: "1080p"
                                        val (qualityLabel, estimatedSize) = when {
                                            vQuality.contains("1080") -> "1080p FHD" to "2.4 GB"
                                            vQuality.contains("720") -> "720p HD" to "1.2 GB"
                                            vQuality.contains("480") -> "480p SD" to "650 MB"
                                            else -> vQuality to "1.0 GB"
                                        }
                                        options.add(
                                            DownloadOption(
                                                title = "Cinejoy Direct ($qualityLabel)",
                                                quality = qualityLabel,
                                                size = estimatedSize,
                                                url = vUrl,
                                                source = "Cinejoy Helios CDN",
                                                provider = name,
                                                headers = reqHeaders
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        options.distinctBy { it.url }
    }'''

content = content.replace(old_dl_block, new_dl_block)

# 9. getDetails
old_details_head = '''    override suspend fun getDetails(mediaItem: MediaItem): MediaDetail = withContext(Dispatchers.IO) {
        val tmdbId = mediaItem.id
        val isTv = mediaItem.type == MediaType.TV_SERIES
        val endpoint = if (isTv) "tv" else "movie"
        val url = "https://api.themoviedb.org/3/$endpoint/$tmdbId?api_key=$tmdbApiKey&append_to_response=credits,recommendations,similar"
        val req = Request.Builder().url(url).build()

        var title = mediaItem.title
        var overview: String? = null
        var posterUrl = mediaItem.posterUrl
        var backdropUrl = mediaItem.backdropUrl
        var year: Int? = null
        var genres = emptyList<String>()
        var rating: String? = null
        var duration: String? = null
        val castMembers = mutableListOf<CastMember>()
        val allEpisodes = mutableListOf<EpisodeItem>()
        val recsList = mutableListOf<MediaItem>()

        try {
            http.meta.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()'''

new_details_head = '''    override suspend fun getDetails(mediaItem: MediaItem): MediaDetail = withContext(Dispatchers.IO) {
        val tmdbId = extractTmdbId(mediaItem.id).ifBlank { extractTmdbId(mediaItem.url) }
        var isTv = mediaItem.type == MediaType.TV_SERIES ||
            mediaItem.url.contains("/tv/") ||
            mediaItem.id.contains("tv", ignoreCase = true)
        var endpoint = if (isTv) "tv" else "movie"
        var url = "https://api.themoviedb.org/3/$endpoint/$tmdbId?api_key=$tmdbApiKey&append_to_response=credits,recommendations,similar"
        var req = Request.Builder().url(url).build()

        var title = mediaItem.title
        var overview: String? = null
        var posterUrl = mediaItem.posterUrl
        var backdropUrl = mediaItem.backdropUrl
        var year: Int? = null
        var genres = emptyList<String>()
        var rating: String? = null
        var duration: String? = null
        val castMembers = mutableListOf<CastMember>()
        val allEpisodes = mutableListOf<EpisodeItem>()
        val recsList = mutableListOf<MediaItem>()

        try {
            http.meta.newCall(req).execute().use { resp ->
                val body: String? = if (resp.isSuccessful) {
                    resp.body?.string().orEmpty()
                } else if (resp.code == 404) {
                    val altEndpoint = if (isTv) "movie" else "tv"
                    val altUrl = "https://api.themoviedb.org/3/$altEndpoint/$tmdbId?api_key=$tmdbApiKey&append_to_response=credits,recommendations,similar"
                    val altReq = Request.Builder().url(altUrl).build()
                    try {
                        http.meta.newCall(altReq).execute().use { altResp ->
                            if (altResp.isSuccessful) {
                                isTv = altEndpoint == "tv"
                                endpoint = altEndpoint
                                altResp.body?.string().orEmpty()
                            } else null
                        }
                    } catch (_: Throwable) { null }
                } else null

                if (body != null) {
                    mediaTypeCache[tmdbId] = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE
                    mediaTypeCache[mediaItem.id] = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE'''

content = content.replace(old_details_head, new_details_head)

# 10. Episode synthesis and MediaDetail
old_ep_synth = '''        if (!isTv && allEpisodes.isEmpty()) {
            val movieEp = EpisodeItem(
                id = tmdbId,
                title = title,
                seasonNumber = 1,
                episodeNumber = 1,
                data = tmdbId,
                thumbnail = backdropUrl
            )
            allEpisodes.add(movieEp)
        }

        MediaDetail(
            id = tmdbId,
            title = title,
            url = mediaItem.url,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            type = mediaItem.type,'''

new_ep_synth = '''        if (isTv && allEpisodes.isEmpty()) {
            val tvEp = EpisodeItem(
                id = "$tmdbId:1:1",
                title = "$title S1 E1",
                seasonNumber = 1,
                episodeNumber = 1,
                data = "$tmdbId:1:1",
                thumbnail = backdropUrl ?: posterUrl
            )
            allEpisodes.add(tvEp)
        } else if (!isTv && allEpisodes.isEmpty()) {
            val movieEp = EpisodeItem(
                id = tmdbId,
                title = title,
                seasonNumber = 1,
                episodeNumber = 1,
                data = tmdbId,
                thumbnail = backdropUrl ?: posterUrl
            )
            allEpisodes.add(movieEp)
        }

        MediaDetail(
            id = tmdbId,
            title = title,
            url = mediaItem.url,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            type = if (isTv) MediaType.TV_SERIES else MediaType.MOVIE,'''

content = content.replace(old_ep_synth, new_ep_synth)

# 11. resolveLogo
old_logo = '''    override suspend fun resolveLogo(mediaItem: MediaItem): String? = withContext(Dispatchers.IO) {
        val tmdbId = mediaItem.id'''

new_logo = '''    override suspend fun resolveLogo(mediaItem: MediaItem): String? = withContext(Dispatchers.IO) {
        val tmdbId = extractTmdbId(mediaItem.id).ifBlank { extractTmdbId(mediaItem.url) }'''

content = content.replace(old_logo, new_logo)

with open('test_eups_repo/sources/CinejoyPlugin.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Patch applied cleanly!")
