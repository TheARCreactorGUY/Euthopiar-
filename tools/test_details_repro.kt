package com.euthopiar.core.provider

import com.euthopiar.core.network.DohDns
import com.euthopiar.core.model.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = DohDns.createOkHttpClient(15, 25)
    val plugin = MovyPlugin(client)
    
    println("--- TEST MOVIE (Dune 2: 693134) ---")
    val dummyMovie = MediaItem(id = "693134", title = "", url = "", posterUrl = null, type = MediaType.MOVIE, provider = "Movy")
    try {
        val dMovie = plugin.getDetails(dummyMovie)
        println("Movie success: id=${dMovie.id}, title='${dMovie.title}', eps=${dMovie.episodes.size}")
    } catch (e: Throwable) {
        println("Movie FAILED: $e")
        e.printStackTrace()
    }

    println("--- TEST TV (Breaking Bad: 1396) ---")
    val dummyTv = MediaItem(id = "1396", title = "", url = "", posterUrl = null, type = MediaType.TV_SERIES, provider = "Movy")
    try {
        val dTv = plugin.getDetails(dummyTv)
        println("TV success: id=${dTv.id}, title='${dTv.title}', eps=${dTv.episodes.size}")
    } catch (e: Throwable) {
        println("TV FAILED: $e")
        e.printStackTrace()
    }
}
