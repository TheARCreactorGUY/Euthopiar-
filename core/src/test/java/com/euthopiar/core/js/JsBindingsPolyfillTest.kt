package com.euthopiar.core.js

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class JsBindingsPolyfillTest {

    private fun evaluateJs(jsSnippet: String): String {
        val harness = """
            globalThis.__get_scraper_id = function() { return 'test-plugin'; };
            globalThis.__get_scraper_settings = function() { return '{}'; };
            globalThis.__get_call_args = function() { return '{}'; };
            globalThis.__get_tmdb_api_key = function() { return 'dummy_key'; };
            globalThis.__plugin_sleep = async function() { return null; };
            globalThis.__host_browser_resolve = async function() { return JSON.stringify({}); };
            globalThis.__native_fetch = async function() { return JSON.stringify({ ok: true, status: 200, statusText: 'OK', url: '', headers: {}, body: '', bodyBase64: '' }); };

            ${JsBindings.staticPolyfillCode}

            (async function() {
                try {
                    $jsSnippet
                } catch(e) {
                    console.error('Test script error:', e);
                    process.exit(1);
                }
            })();
        """.trimIndent()

        val tempFile = File.createTempFile("js_bindings_test_", ".js")
        try {
            tempFile.writeText(harness)
            val process = ProcessBuilder("node", tempFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                fail("Node evaluation failed with exit code $exitCode:\n$output")
            }
            return output
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testBufferPolyfillBase64Utf8RoundTrip() {
        val script = """
            const original = "Hello Euthopiar Engine!";
            const b64 = Buffer.from(original).toString("base64");
            const roundTrip = Buffer.from(b64, "base64").toString("utf-8");
            const isBuf = Buffer.isBuffer(Buffer.from("foo"));
            console.log(JSON.stringify({ b64, roundTrip, isBuf }));
        """
        val output = evaluateJs(script)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(output)
        val obj = json as kotlinx.serialization.json.JsonObject
        assertEquals("SGVsbG8gRXV0aG9waWFyIEVuZ2luZSE=", (obj["b64"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("Hello Euthopiar Engine!", (obj["roundTrip"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(true, (obj["isBuf"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
    }

    @Test
    fun testAbortSignalTimeout() {
        val script = """
            const sig = AbortSignal.timeout(50);
            console.log(JSON.stringify({ hasSignal: !!sig, isAborted: sig.aborted }));
        """
        val output = evaluateJs(script)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(output)
        val obj = json as kotlinx.serialization.json.JsonObject
        assertEquals(true, (obj["hasSignal"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals(false, (obj["isAborted"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
    }

    @Test
    fun testRequireWhitelistExpansion() {
        val script = """
            const bufModule = require('buffer');
            const nodeBufModule = require('node:buffer');
            const cryptoModule = require('crypto');
            const nodeCryptoModule = require('node:crypto');
            const axiosModule = require('axios');

            const hasBuffer = typeof bufModule.Buffer.from === 'function';
            const hasNodeBuffer = typeof nodeBufModule.Buffer.from === 'function';
            const hasCreateHash = typeof cryptoModule.createHash === 'function';
            const hasNodeCreateHash = typeof nodeCryptoModule.createHash === 'function';
            const hasAxios = typeof axiosModule === 'function' && typeof axiosModule.get === 'function';

            console.log(JSON.stringify({
                hasBuffer,
                hasNodeBuffer,
                hasCreateHash,
                hasNodeCreateHash,
                hasAxios
            }));
        """
        val output = evaluateJs(script)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(output)
        val obj = json as kotlinx.serialization.json.JsonObject
        assertEquals(true, (obj["hasBuffer"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals(true, (obj["hasNodeBuffer"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals(true, (obj["hasCreateHash"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals(true, (obj["hasNodeCreateHash"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals(true, (obj["hasAxios"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
    }

    @Test
    fun testAxiosAdvancedFeatures() {
        val script = """
            const axios = require('axios');
            const instance = axios.create({ baseURL: 'https://api.example.com', headers: { 'X-Custom': '1' } });
            const hasDefault = axios.default === axios;
            const hasCreate = typeof instance.get === 'function';
            const hasDefaults = typeof axios.defaults === 'object';
            const isAxiosErr = axios.isAxiosError({ response: { status: 404 } });
            const notAxiosErr = axios.isAxiosError(new Error('general'));

            console.log(JSON.stringify({
                hasDefault,
                hasCreate,
                hasDefaults,
                isAxiosErr,
                notAxiosErr
            }));
        """
        val output = evaluateJs(script)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(output)
        val obj = json as kotlinx.serialization.json.JsonObject
        assertEquals(true, (obj["hasDefault"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals(true, (obj["hasCreate"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals(true, (obj["hasDefaults"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals(true, (obj["isAxiosErr"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
        assertEquals(false, (obj["notAxiosErr"] as kotlinx.serialization.json.JsonPrimitive).content.toBoolean())
    }
}
