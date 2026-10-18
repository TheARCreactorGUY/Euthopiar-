package com.euthopiar.core.model

sealed interface StreamEmission {
    data class SourceFound(val source: StreamSource) : StreamEmission
    data class SubtitleFound(val track: SubtitleTrack) : StreamEmission
    data class StatusUpdate(val serverName: String, val message: String) : StreamEmission
}
