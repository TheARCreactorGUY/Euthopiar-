package com.euthopiar.core.js

import android.os.Process
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.quickJs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.concurrent.Volatile
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

const val MAX_CONCURRENT_PLUGINS = 10
const val PLUGIN_TIMEOUT_MS = 60_000L

val pluginDispatcher: CoroutineDispatcher =
    Executors.newFixedThreadPool(MAX_CONCURRENT_PLUGINS) { runnable ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }, "plugin-worker").apply {
            isDaemon = true
        }
    }.asCoroutineDispatcher()

class JsRuntime {
    suspend fun <T> use(block: suspend QuickJs.() -> T): T {
        val dispatcher = (coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher)
            ?: pluginDispatcher
        return quickJs(dispatcher) {
            evaluationTimeoutMillis = PLUGIN_TIMEOUT_MS
            block()
        }
    }

    companion object {
        @Volatile
        private var cachedPolyfillBytecode: ByteArray? = null

        @Volatile
        private var cachedCallBytecode: ByteArray? = null

        @Volatile
        private var cachedSettingsCallBytecode: ByteArray? = null

        fun polyfillBytecode(runtime: QuickJs): ByteArray =
            cachedPolyfillBytecode ?: runtime.compile(JsBindings.staticPolyfillCode, "polyfill.js", false).also {
                cachedPolyfillBytecode = it
            }

        fun callBytecode(runtime: QuickJs): ByteArray =
            cachedCallBytecode ?: runtime.compile(JsBindings.staticCallCode, "call.js", false).also {
                cachedCallBytecode = it
            }

        fun settingsCallBytecode(runtime: QuickJs): ByteArray =
            cachedSettingsCallBytecode
                ?: runtime.compile(JsBindings.staticSettingsCallCode, "settings-call.js", false).also {
                    cachedSettingsCallBytecode = it
                }
    }
}
