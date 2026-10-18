package com.nuvio.app.features.watchprogress

internal expect object ResumePromptStorage {
    fun loadWasInPlayer(): Boolean
    fun saveWasInPlayer(value: Boolean)
    fun loadLastPlayerVideoId(): String?
    fun saveLastPlayerVideoId(videoId: String?)
}
