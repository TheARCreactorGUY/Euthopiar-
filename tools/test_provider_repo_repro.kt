package com.euthopiar.core.provider

import com.euthopiar.core.network.DohDns
import com.euthopiar.core.model.*
import com.euthopiar.core.registry.PluginRegistry
import com.euthopiar.core.repository.ProviderRepository
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = DohDns.createOkHttpClient(15, 25)
    val plugin = MovyPlugin(client)
    val registry = PluginRegistry(listOf(plugin))
    val repository = ProviderRepository(registry)

    println("--- TESTING REPOSITORY GETDETAILS FOR TV (Breaking Bad: 1396) ---")
    try {
        val detail = repository.getDetails("1396", MediaType.TV_SERIES, "Movy")
        println("Success! title=${detail.title}, type=${detail.type}, episodes count=${detail.episodes.size}")
        detail.episodes.take(5).forEach { ep ->
            println("  Ep S${ep.seasonNumber}E${ep.episodeNumber}: id=${ep.id}, data=${ep.data}")
        }
    } catch (e: Throwable) {
        println("Repository getDetails FAILED: $e")
        e.printStackTrace()
    }

    println("\n--- TESTING REPOSITORY GETDETAILS FOR MOVIE (693134) ---")
    try {
        val detail = repository.getDetails("693134", MediaType.MOVIE, "Movy")
        println("Success! title=${detail.title}, type=${detail.type}, episodes count=${detail.episodes.size}")
    } catch (e: Throwable) {
        println("Repository getDetails FAILED: $e")
        e.printStackTrace()
    }
}
