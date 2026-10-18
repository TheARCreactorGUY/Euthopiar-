package com.nuvio.app.features.trakt

import kotlinx.serialization.json.JsonObject

internal expect object TraktCommentsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
    fun exportToSyncPayload(): JsonObject
    fun replaceFromSyncPayload(payload: JsonObject)
}
