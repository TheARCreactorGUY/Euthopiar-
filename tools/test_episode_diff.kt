package com.euthopiar.core.provider

import com.euthopiar.core.network.DohDns
import com.euthopiar.core.model.MediaItem
import com.euthopiar.core.model.MediaType
import com.euthopiar.eup.api.MediaCard
import com.euthopiar.eup.api.ContentType
import com.euthopiar.eup.api.PlayableTarget
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = DohDns.createOkHttpClient(15, 25)
    val plugin = MovyPlugin(client)

    println("=== TEST 1: Details on TV Show with different inputs ===")
    
    // A. Regular TV MediaItem
    val tvItem = MediaItem(id = "1396", title = "Breaking Bad", url = "https://www.movy.sx/tv/1396", posterUrl = null, type = MediaType.TV_SERIES)
    val d1 = plugin.getDetails(tvItem)
    println("d1 (type=TV_SERIES): episodes count = ${d1.episodes.size}, first = ${d1.episodes.firstOrNull()?.title}, last = ${d1.episodes.lastOrNull()?.title}")

    // B. MediaItem with type=MOVIE or unspecified (e.g. from search or library)
    val tvItemAsMovie = MediaItem(id = "1396", title = "Breaking Bad", url = "", posterUrl = null, type = MediaType.MOVIE)
    val d2 = plugin.getDetails(tvItemAsMovie)
    println("d2 (id='1396', url='', type=MOVIE): episodes count = ${d2.episodes.size}, first = ${d2.episodes.firstOrNull()?.title}")

    // C. MediaCard for EUP v2 details(card)
    val card = MediaCard(id = "1396", title = "Breaking Bad", type = ContentType.TV_SERIES)
    val d3 = plugin.details(card)
    println("d3 (MediaCard ContentType.TV_SERIES): seasons = ${d3.seasons.size}, total eps = ${d3.seasons.sumOf { it.episodes.size }}")
    d3.seasons.firstOrNull()?.episodes?.take(3)?.forEach { ep ->
        println("   S${ep.seasonNumber}E${ep.episodeNumber}: target = ${ep.target}")
    }

    println("\n=== TEST 2: Streams for S1E1 vs S1E2 ===")
    val ep1Links = plugin.getStreamLinks("1396:1:1")
    println("S1E1 stream count = ${ep1Links.streams.size}")
    val ep1Miami = ep1Links.streams.firstOrNull { it.serverName.contains("Miami") && it.resolutionLabel.contains("1080p") }
    println("S1E1 Miami 1080p URL: ${ep1Miami?.url}")

    val ep2Links = plugin.getStreamLinks("1396:1:2")
    println("S1E2 stream count = ${ep2Links.streams.size}")
    val ep2Miami = ep2Links.streams.firstOrNull { it.serverName.contains("Miami") && it.resolutionLabel.contains("1080p") }
    println("S1E2 Miami 1080p URL: ${ep2Miami?.url}")

    if (ep1Miami?.url == ep2Miami?.url) {
        println("CRITICAL BUG: S1E1 and S1E2 RETURN THE EXACT SAME STREAM URL!")
    } else {
        println("OK: S1E1 and S1E2 have different URLs.")
    }

    println("\n=== TEST 3: Streams with query params (e.g. 1396?season=1&episode=2 or 1396:1:1?season=1&episode=2) ===")
    val queryLinks = plugin.getStreamLinks("1396:1:1?season=1&episode=2")
    val queryMiami = queryLinks.streams.firstOrNull { it.serverName.contains("Miami") && it.resolutionLabel.contains("1080p") }
    println("Query 1396:1:1?season=1&episode=2 Miami 1080p URL: ${queryMiami?.url}")
    if (queryMiami?.url == ep1Miami?.url) {
        println("CRITICAL BUG: Query string '1396:1:1?season=1&episode=2' parsed as Episode 1!")
    }

    val queryLinks2 = plugin.getStreamLinks("1396?season=1&episode=2")
    val queryMiami2 = queryLinks2.streams.firstOrNull { it.serverName.contains("Miami") && it.resolutionLabel.contains("1080p") }
    println("Query 1396?season=1&episode=2 Miami 1080p URL: ${queryMiami2?.url}")
    if (queryMiami2?.url == ep1Miami?.url) {
        println("CRITICAL BUG: Query string '1396?season=1&episode=2' parsed as Episode 1!")
    }
}
