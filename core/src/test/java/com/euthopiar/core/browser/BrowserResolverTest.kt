package com.euthopiar.core.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class BrowserResolverTest {

    private fun evaluateJs(jsSnippet: String): String {
        val harness = """
            globalThis.window = globalThis;
            globalThis.location = { href: 'https://vidstuck.cc/watch/movie/550' };
            globalThis.mediaPosts = [];
            globalThis.subtitlePosts = [];
            globalThis.__hostResolverBridge = {
                postMedia: function(url, mime, isSub) {
                    globalThis.mediaPosts.push({ url: url, mime: mime, isSub: !!isSub });
                },
                postSubtitle: function(url, lang, name) {
                    globalThis.subtitlePosts.push({ url: url, lang: lang, name: name });
                }
            };

            globalThis.HTMLMediaElement = function() {};
            var _internalMediaSrc = '';
            Object.defineProperty(globalThis.HTMLMediaElement.prototype, 'src', {
                get: function() { return _internalMediaSrc; },
                set: function(v) { _internalMediaSrc = v; },
                configurable: true,
                enumerable: true
            });

            globalThis.HTMLSourceElement = function() {};
            var _internalSourceSrc = '';
            Object.defineProperty(globalThis.HTMLSourceElement.prototype, 'src', {
                get: function() { return _internalSourceSrc; },
                set: function(v) { _internalSourceSrc = v; },
                configurable: true,
                enumerable: true
            });

            globalThis.MediaSource = function() {};

            $UNIVERSAL_SNIFFER_INTERCEPTOR_JS

            (async function() {
                try {
                    $jsSnippet
                } catch(e) {
                    console.error('Test JS error:', e);
                    process.exit(1);
                }
            })();
        """.trimIndent()

        val tempFile = File.createTempFile("browser_resolver_test_", ".js")
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
    fun testBrowserResolveRequestAndResultModels() {
        val request = BrowserResolveRequest(
            url = "https://freekz.to/watch/movie/550",
            autoClickServers = true
        )
        assertTrue(request.autoClickServers)
        assertEquals("https://freekz.to/watch/movie/550", request.url)
        assertFalse(request.stopOnFirstMediaMatch)

        val defaultRequest = BrowserResolveRequest(url = "https://example.com")
        assertFalse(defaultRequest.autoClickServers)

        val sub = SniffedSubtitleResource(
            url = "https://vidstuck.cc/subs/en.vtt",
            lang = "English",
            name = "English [SDH]"
        )
        assertEquals("https://vidstuck.cc/subs/en.vtt", sub.url)
        assertEquals("English", sub.lang)
        assertEquals("English [SDH]", sub.name)

        val media = SniffedMediaResource(
            url = "https://vidstuck.cc/backend/database/atlas/1080.m3u8",
            mimeType = "application/x-mpegURL"
        )

        val result = BrowserResolveResult(
            finalUrl = "https://freekz.to/watch/movie/550",
            html = "<html></html>",
            cookies = mapOf("token" to "xyz"),
            cookieString = "token=xyz",
            userAgent = "test-ua",
            sniffedUrls = listOf(media.url, sub.url),
            sniffedMedia = listOf(media),
            sniffedSubtitles = listOf(sub)
        )

        assertEquals(1, result.sniffedMedia.size)
        assertEquals(1, result.sniffedSubtitles.size)
        assertEquals("https://vidstuck.cc/subs/en.vtt", result.sniffedSubtitles[0].url)
        assertEquals("English", result.sniffedSubtitles[0].lang)
        assertEquals(2, result.sniffedUrls.size)
    }

    @Test
    fun testUniversalSnifferTextDecoderAndAtobInterceptors() {
        val script = """
            // 1. TextDecoder decode with m3u8
            var decoder = new TextDecoder('utf-8');
            var uint8 = new Uint8Array([104, 116, 116, 112, 115, 58, 47, 47, 99, 100, 110, 46, 101, 120, 97, 109, 112, 108, 101, 46, 99, 111, 109, 47, 104, 108, 115, 47, 109, 97, 115, 116, 101, 114, 46, 109, 51, 117, 56]);
            var decoded = decoder.decode(uint8); // "https://cdn.example.com/hls/master.m3u8"

            // 2. atob with relative backend database URL
            var b64 = Buffer.from('/backend/database/ursa/stream-token').toString('base64');
            var atobRes = window.atob(b64);

            console.log(JSON.stringify({
                decoded: decoded,
                atobRes: atobRes,
                mediaPosts: globalThis.mediaPosts
            }));
        """
        val output = evaluateJs(script)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(output) as kotlinx.serialization.json.JsonObject
        val posts = json["mediaPosts"] as kotlinx.serialization.json.JsonArray
        assertTrue("Expected media posts from TextDecoder and atob, got: $posts", posts.size >= 2)

        val urls = posts.map { (it as kotlinx.serialization.json.JsonObject)["url"].toString() }
        assertTrue("Should contain m3u8 URL", urls.any { it.contains("master.m3u8") })
        assertTrue("Should contain resolved vidstuck backend URL", urls.any { it.contains("/backend/database/ursa") })
    }

    @Test
    fun testUniversalSnifferJsonParseCaptionsAndSources() {
        val script = """
            var payload = JSON.stringify({
                sources: [
                    { file: 'https://vidstuck.cc/backend/database/daedalus/index.m3u8', type: 'hls' }
                ],
                links: [
                    { link: 'https://vidstuck.cc/backend/database/andromeda/manifest.mpd', type: 'dash' }
                ],
                captions: [
                    { file: '/subs/french.vtt', display: 'French', lang: 'fr' },
                    { file: 'https://cdn.example.com/spanish.srt', label: 'Spanish' }
                ]
            });

            var parsed = JSON.parse(payload);

            console.log(JSON.stringify({
                hasParsed: !!parsed,
                mediaPosts: globalThis.mediaPosts,
                subtitlePosts: globalThis.subtitlePosts
            }));
        """
        val output = evaluateJs(script)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(output) as kotlinx.serialization.json.JsonObject
        val mediaPosts = json["mediaPosts"] as kotlinx.serialization.json.JsonArray
        val subPosts = json["subtitlePosts"] as kotlinx.serialization.json.JsonArray

        assertTrue("Expected media posts from JSON.parse sources/links", mediaPosts.size >= 2)
        assertTrue("Expected subtitle posts from JSON.parse captions", subPosts.size >= 2)

        val subLangs = subPosts.map { (it as kotlinx.serialization.json.JsonObject)["lang"].toString().replace("\"", "") }
        assertTrue("Should contain French subtitle", subLangs.contains("French"))
        assertTrue("Should contain Spanish subtitle", subLangs.contains("Spanish"))
    }

    @Test
    fun testUniversalSnifferMediaElementAndHlsLoadSource() {
        val script = """
            var videoEl = Object.create(globalThis.HTMLMediaElement.prototype);
            videoEl.src = 'https://cdn.example.com/video-player-stream.mp4';

            // Simulate Hls.js initialization by website
            globalThis.Hls = function() {};
            globalThis.Hls.prototype.loadSource = function(src) { this.source = src; };

            var hlsInstance = new globalThis.Hls();
            hlsInstance.loadSource('https://cdn.example.com/hls/live.m3u8');

            console.log(JSON.stringify({
                mediaPosts: globalThis.mediaPosts
            }));
        """
        val output = evaluateJs(script)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(output) as kotlinx.serialization.json.JsonObject
        val mediaPosts = json["mediaPosts"] as kotlinx.serialization.json.JsonArray
        val urls = mediaPosts.map { (it as kotlinx.serialization.json.JsonObject)["url"].toString() }

        assertTrue("Should contain videoEl.src", urls.any { it.contains("video-player-stream.mp4") })
        assertTrue("Should contain hls.loadSource URL", urls.any { it.contains("live.m3u8") })
    }

    @Test
    fun testFreekzPluginFallbackIntegration() {
        val candidates = listOf(
            File("scratch/TheARCreactorGUY-Plugins/providers/freekz.js"),
            File("../scratch/TheARCreactorGUY-Plugins/providers/freekz.js"),
            File("dist_apk/providers/freekz.js"),
            File("../dist_apk/providers/freekz.js")
        )
        val pluginFile = candidates.firstOrNull { it.exists() } ?: File("scratch/TheARCreactorGUY-Plugins/providers/freekz.js")
        val pluginPath = pluginFile.canonicalFile.absolutePath.replace("\\", "/")
        val harness = """
            globalThis.crypto = require('crypto');
            try {
                globalThis.CryptoJS = require('crypto-js');
            } catch(e) {
                globalThis.CryptoJS = {
                    AES: {
                        decrypt: function(cipher, pass) {
                            return {
                                toString: function(enc) { return ''; }
                            };
                        }
                    },
                    enc: { Utf8: {} }
                };
            }

            var resolvedFallback = false;
            globalThis.Browser = {
                resolve: async function(options) {
                    resolvedFallback = true;
                    return {
                        finalUrl: options.url,
                        html: '<html></html>',
                        cookies: {},
                        cookieString: '',
                        userAgent: 'test-agent',
                        sniffedUrls: ['https://vidstuck.cc/backend/database/atlas/master.m3u8'],
                        sniffedMedia: [
                            {
                                url: 'https://vidstuck.cc/backend/database/atlas/master.m3u8',
                                headers: {},
                                mimeType: 'application/x-mpegURL',
                                isSubtitles: false
                            }
                        ],
                        sniffedSubtitles: [
                            {
                                url: 'https://vidstuck.cc/subs/en.vtt',
                                lang: 'English',
                                name: 'English'
                            }
                        ]
                    };
                }
            };

            var freekz = require('$pluginPath');

            (async function() {
                try {
                    // Check module.exports does not expose deprecated MiniAes
                    var hasMiniAes = !!freekz.MiniAes;
                    var hasGetStreams = typeof freekz.getStreams === 'function';

                    // Mock fetch to simulate 0 streams returned by direct API (e.g. key rotation)
                    globalThis.fetch = async function(url) {
                        return {
                            ok: false,
                            status: 500,
                            json: async function() { return null; },
                            text: async function() { return ''; }
                        };
                    };

                    var streams = await freekz.getStreams(550, 'movie', 1, 1, { title: 'Fight Club', year: '1999' });

                    console.log(JSON.stringify({
                        hasMiniAes: hasMiniAes,
                        hasGetStreams: hasGetStreams,
                        resolvedFallback: resolvedFallback,
                        streamCount: streams.length,
                        firstStreamTitle: streams.length > 0 ? streams[0].title : '',
                        subtitlesCount: (streams.length > 0 && streams[0].subtitles) ? streams[0].subtitles.length : 0
                    }));
                } catch(e) {
                    console.error('Freekz test error:', e);
                    process.exit(1);
                }
            })();
        """.trimIndent()

        val tempFile = File.createTempFile("freekz_test_", ".js")
        try {
            tempFile.writeText(harness)
            val process = ProcessBuilder("node", tempFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                fail("Node evaluation of freekz fallback failed with exit code $exitCode:\n$output")
            }
            val json = kotlinx.serialization.json.Json.parseToJsonElement(output) as kotlinx.serialization.json.JsonObject
            assertFalse("MiniAes should be removed from module.exports", json["hasMiniAes"].toString().toBoolean())
            assertTrue("getStreams should be exported", json["hasGetStreams"].toString().toBoolean())
            assertTrue("Browser.resolve fallback should be invoked when direct API returns 0 streams", json["resolvedFallback"].toString().toBoolean())
            val streamCount = json["streamCount"].toString().toInt()
            assertTrue("Fallback should generate streams from sniffedMedia", streamCount > 0)
            val subCount = json["subtitlesCount"].toString().toInt()
            assertTrue("Fallback should map sniffedSubtitles into output stream", subCount > 0)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testMediaSourceAndCreateObjectURLInterceptors() {
        val script = """
            var ms = new globalThis.MediaSource();
            ms.src = 'https://vidstuck.cc/backend/database/ursa/stream.m3u8';

            // Also test URL.createObjectURL with an object carrying manifestUri
            var objUrl = URL.createObjectURL({ _manifestUri: 'https://vidstuck.cc/backend/database/andromeda/manifest.mpd' });

            console.log(JSON.stringify({
                mediaPosts: globalThis.mediaPosts
            }));
        """
        val output = evaluateJs(script)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(output) as kotlinx.serialization.json.JsonObject
        val posts = json["mediaPosts"] as kotlinx.serialization.json.JsonArray
        val urls = posts.map { (it as kotlinx.serialization.json.JsonObject)["url"].toString() }

        assertTrue("Should intercept MediaSource.src", urls.any { it.contains("stream.m3u8") })
        assertTrue("Should intercept URL.createObjectURL", urls.any { it.contains("manifest.mpd") })
    }

    @Test
    fun testNonMediaUrlsIgnoredByInterceptors() {
        val script = """
            var decoder = new TextDecoder('utf-8');
            var code = "var ga = 'https://www.google-analytics.com/analytics.js'; var icon = 'https://example.com/logo.png'; var schema = 'https://schema.org';";
            var bytes = Buffer.from(code, 'utf8');
            decoder.decode(bytes);

            console.log(JSON.stringify({
                mediaPosts: globalThis.mediaPosts
            }));
        """
        val output = evaluateJs(script)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(output) as kotlinx.serialization.json.JsonObject
        val posts = json["mediaPosts"] as kotlinx.serialization.json.JsonArray
        assertEquals("Non-media URLs in decoded text should not be posted as media streams", 0, posts.size)
    }

    @Test
    fun testAutoClickServersFiltersVisibleElements() {
        val harness = """
            globalThis.window = globalThis;
            var clicked = [];
            var elements = [
                {
                    className: 'server-item',
                    offsetWidth: 100,
                    offsetHeight: 30,
                    getBoundingClientRect: function() { return { width: 100, height: 30 }; },
                    scrollIntoView: function() {},
                    click: function() { clicked.push('server1'); },
                    dispatchEvent: function() {},
                    contains: function() { return false; }
                },
                {
                    className: 'server-item',
                    offsetWidth: 0,
                    offsetHeight: 0,
                    getBoundingClientRect: function() { return { width: 0, height: 0 }; },
                    scrollIntoView: function() {},
                    click: function() { clicked.push('hidden-server'); },
                    dispatchEvent: function() {},
                    contains: function() { return false; }
                }
            ];

            globalThis.document = {
                querySelectorAll: function(sel) {
                    return elements;
                },
                querySelector: function() { return null; }
            };

            var count = $AUTO_CLICK_SERVERS_JS;
            console.log(JSON.stringify({ count: count }));
        """.trimIndent()

        val tempFile = File.createTempFile("autoclick_test_", ".js")
        try {
            tempFile.writeText(harness)
            val process = ProcessBuilder("node", tempFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                fail("Node evaluation of autoclick failed with exit code $exitCode:\n$output")
            }
            val json = kotlinx.serialization.json.Json.parseToJsonElement(output) as kotlinx.serialization.json.JsonObject
            val count = json["count"].toString().toInt()
            assertEquals("Should only click visible server element", 1, count)
        } finally {
            tempFile.delete()
        }
    }
}
