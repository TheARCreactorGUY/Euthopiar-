package com.nuvio.app.features.streams

data class StreamContext(
    val pauseDescription: String? = null,
)

object StreamContextStore {
    private var nextContextId = 1L
    private val contexts = mutableMapOf<Long, StreamContext>()

    fun put(context: StreamContext): Long {
        val contextId = nextContextId++
        contexts[contextId] = context
        return contextId
    }

    fun get(contextId: Long): StreamContext? = contexts[contextId]

    fun remove(contextId: Long) {
        contexts.remove(contextId)
    }

    fun clear() {
        nextContextId = 1L
        contexts.clear()
    }
}
