package com.euthopiar.core.js

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EmbedExtractorTest {

    private fun evaluateJs(jsSnippet: String, callArgsJson: String = "{}"): String {
        val harness = """
            globalThis.__get_scraper_id = function() { return 'test-plugin'; };
            globalThis.__get_scraper_settings = function() { return '{}'; };
            globalThis.__CALL_ARGS_INPUT__ = $callArgsJson;
            globalThis.__get_call_args = function() { return JSON.stringify(globalThis.__CALL_ARGS_INPUT__); };
            globalThis.__get_tmdb_api_key = function() { return 'dummy_tmdb_key'; };
            globalThis.__CAPTURED_RESULT__ = '[]';
            globalThis.__capture_result = function(res) { globalThis.__CAPTURED_RESULT__ = res; };
            globalThis.__plugin_sleep = async function() { return null; };
            globalThis.__host_browser_resolve = async function() { return JSON.stringify({ finalUrl: '', html: '', cookies: {}, cookieString: '', userAgent: '', sniffedUrls: [], sniffedMedia: [] }); };
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

        val tempFile = File.createTempFile("quickjs_test_", ".js")
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
    fun testUnpackerIsPackedAndUnpack() {
        val script = """
            var packed = "eval(function(p,a,c,k,e,d){while(c--)if(k[c])p=p.replace(new RegExp('\\\\b'+e(c)+'\\\\b','g'),k[c]);return p;}('0 1=2;',3,3,'var|videoUrl|https://cdn.example.com/master.m3u8'.split('|')))";
            var isPacked = Unpacker.isPacked(packed);
            var unpacked = Unpacker.unpack(packed);
            console.log(JSON.stringify({ isPacked: isPacked, unpacked: unpacked }));
        """.trimIndent()

        val output = evaluateJs(script)
        val json = Json.parseToJsonElement(output).jsonObject
        assertTrue("Unpacker.isPacked should detect packed script", json["isPacked"]!!.jsonPrimitive.boolean)
        val unpacked = json["unpacked"]!!.jsonPrimitive.content
        assertTrue("Unpacked should contain master.m3u8", unpacked.contains("https://cdn.example.com/master.m3u8"))
        assertTrue("Unpacked should contain var videoUrl", unpacked.contains("var videoUrl="))
    }

    @Test
    fun testUnpackerExtractMediaUrls() {
        val script = """
            var snippet = 'var player = jwplayer("vplayer").setup({ sources: [ { file: "https://cdn.example.com/hls/master.m3u8", label: "Auto" } ], tracks: [ { file: "https://cdn.example.com/subs/en.vtt", label: "English", kind: "captions" } ] });';
            var extracted = Unpacker.extractMediaUrls(snippet, 'https://cdn.example.com');
            console.log(JSON.stringify(extracted));
        """.trimIndent()

        val output = evaluateJs(script)
        val json = Json.parseToJsonElement(output).jsonObject
        val sources = json["sources"]!!.jsonArray
        val subtitles = json["subtitles"]!!.jsonArray

        assertEquals(1, sources.size)
        assertEquals("https://cdn.example.com/hls/master.m3u8", sources[0].jsonObject["url"]!!.jsonPrimitive.content)

        assertEquals(1, subtitles.size)
        assertEquals("https://cdn.example.com/subs/en.vtt", subtitles[0].jsonObject["url"]!!.jsonPrimitive.content)
        assertEquals("English", subtitles[0].jsonObject["language"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorCanHandleSupportedHosts() {
        val script = """
            var urls = [
                "https://filemoon.sx/e/abc123filemoon",
                "https://streamwish.to/e/swish789",
                "https://pixeldrain.com/u/pd123456",
                "https://hubcloud.club/drive/hub456",
                "https://gdflix.top/file/gdf123",
                "https://doodstream.com/e/dood999",
                "https://voe.sx/e/voe444",
                "https://streamtape.com/v/stape555",
                "https://gofile.io/d/gofile888",
                "https://vidmoly.to/w/vidmoly111",
                "https://mixdrop.co/e/mixdrop222",
                "https://cdn.example.com/video.m3u8",
                "https://cdn.example.com/movie.mp4"
            ];
            var results = urls.map(function(u) { return { url: u, canHandle: EmbedExtractor.canHandle(u) }; });
            var googleHandled = EmbedExtractor.canHandle("https://www.google.com/search?q=test");
            console.log(JSON.stringify({ results: results, googleHandled: googleHandled }));
        """.trimIndent()

        val output = evaluateJs(script)
        val json = Json.parseToJsonElement(output).jsonObject
        val results = json["results"]!!.jsonArray
        for (elem in results) {
            val item = elem.jsonObject
            assertTrue("EmbedExtractor.canHandle should return true for ${item["url"]!!.jsonPrimitive.content}", item["canHandle"]!!.jsonPrimitive.boolean)
        }
        assertFalse("EmbedExtractor.canHandle should return false for google", json["googleHandled"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun testEmbedExtractorResolvePixelDrain() {
        val script = """
            var res = await EmbedExtractor.resolve('https://pixeldrain.com/u/abc12345', { name: 'PixelDrain Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertTrue("Should return at least one stream", arr.isNotEmpty())

        val stream = arr[0].jsonObject
        assertEquals("https://pixeldrain.com/api/file/abc12345", stream["url"]!!.jsonPrimitive.content)
        assertEquals("1080p", stream["quality"]!!.jsonPrimitive.content)

        val headers = stream["headers"]!!.jsonObject
        assertEquals("https://pixeldrain.com/", headers["Referer"]!!.jsonPrimitive.content)
    }

    @Test
    fun testStaticCallCodeAutoUnwrapsEmbedUrls() {
        val script = """
            globalThis.getStreams = async function(tmdbId, mediaType) {
                return [
                    { name: "Raw PixelDrain Stream", url: "https://pixeldrain.com/u/xyz987", quality: "1080p" },
                    { name: "Direct HLS Stream", url: "https://cdn.example.com/playlist.m3u8", quality: "Auto" }
                ];
            };

            var capturePromise = new Promise(function(resolve) {
                globalThis.__capture_result = function(res) {
                    globalThis.__CAPTURED_RESULT__ = res;
                    resolve(res);
                };
            });

            ${JsBindings.staticCallCode}

            var captured = await capturePromise;
            console.log(captured);
        """.trimIndent()

        val output = evaluateJs(script, callArgsJson = """{"tmdbId":"550","mediaType":"movie"}""")
        assertFalse("Captured result should not be empty", output.isBlank())

        val streams = Json.parseToJsonElement(output).jsonArray
        assertEquals(2, streams.size)

        val unwrappedPixelDrain = streams[0].jsonObject
        assertEquals("https://pixeldrain.com/api/file/xyz987", unwrappedPixelDrain["url"]!!.jsonPrimitive.content)
        assertEquals("Raw PixelDrain Stream", unwrappedPixelDrain["name"]!!.jsonPrimitive.content)
        assertEquals("https://pixeldrain.com/", unwrappedPixelDrain["headers"]!!.jsonObject["Referer"]!!.jsonPrimitive.content)

        val directStream = streams[1].jsonObject
        assertEquals("https://cdn.example.com/playlist.m3u8", directStream["url"]!!.jsonPrimitive.content)
        assertEquals("Direct HLS Stream", directStream["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorCanHandleNewHostsAndRejectsUnrelated() {
        val script = """
            var urls = [
                "https://www.mp4upload.com/embed-test12345.html",
                "https://vidoza.net/embed-abcde12345.html",
                "https://vidoza.org/embed-abcde12345.html",
                "https://streamsb.net/e/sb123",
                "https://sbembed.com/e/sb456",
                "https://sbplay.org/e/sb789",
                "https://sbfull.com/e/sb111",
                "https://watchsb.com/e/sb222",
                "https://lvturbo.com/e/sb333",
                "https://uqload.to/embed-uq123.html",
                "https://upstream.to/embed-up456.html",
                "https://streamlare.com/e/sl123",
                "https://streamhub.ink/e/sh456",
                "https://supervideo.tv/e/sv789",
                "https://dropload.io/e/dl123",
                "https://vtbe.to/e/vt456",
                "https://vtube.network/e/vt789",
                "https://filelions.to/v/fl123",
                "https://vidhidepro.com/v/vh456",
                "https://mycloud.to/v/mc789",
                "https://lulustream.com/e/ls123",
                "https://vidguard.to/v/vg123",
                "https://listeamed.net/v/ls456",
                "https://bembed.net/v/bm789",
                "https://vgfplay.com/v/vp123",
                "https://dood.watch/d/dw123",
                "https://dood.pm/d/dp456",
                "https://dood.to/e/dt789",
                "https://dood.so/e/ds123",
                "https://dood.cx/e/dc456",
                "https://dood.la/e/dl789",
                "https://dood.ws/e/dw999",
                "https://dood.sh/e/dsh111",
                "https://dood.re/e/dr222",
                "https://dood.yt/e/dy333",
                "https://dood.stream/e/dst444",
                "https://www.mediafire.com/file/mf12345/video.mp4/file",
                "https://krakenfiles.com/view/kf12345/file.html",
                "https://kwik.cx/e/kw12345",
                "https://gogoplay4.com/streaming.php?id=gg123",
                "https://gogohd.net/streaming.php?id=gh456",
                "https://goload.io/streaming.php?id=gl789",
                "https://vidstreaming.io/streaming.php?id=vs111",
                "https://anitaku.to/embed/ak222",
                "https://playgo1.cc/embed.php?id=pg333"
            ];
            var results = urls.map(function(u) { return { url: u, canHandle: EmbedExtractor.canHandle(u) }; });
            var unrelated = [
                "https://www.bbc.com/news/world",
                "https://en.wikipedia.org/wiki/Kotlin",
                "https://news.ycombinator.com",
                "https://github.com/google"
            ].map(function(u) { return { url: u, canHandle: EmbedExtractor.canHandle(u) }; });
            console.log(JSON.stringify({ results: results, unrelated: unrelated }));
        """.trimIndent()

        val output = evaluateJs(script)
        val json = Json.parseToJsonElement(output).jsonObject
        val results = json["results"]!!.jsonArray
        for (elem in results) {
            val item = elem.jsonObject
            assertTrue("EmbedExtractor.canHandle should return true for ${item["url"]!!.jsonPrimitive.content}", item["canHandle"]!!.jsonPrimitive.boolean)
        }
        val unrelated = json["unrelated"]!!.jsonArray
        for (elem in unrelated) {
            val item = elem.jsonObject
            assertFalse("EmbedExtractor.canHandle should return false for unrelated url ${item["url"]!!.jsonPrimitive.content}", item["canHandle"]!!.jsonPrimitive.boolean)
        }
    }

    @Test
    fun testEmbedExtractorResolveMp4Upload() {
        val script = """
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: url,
                    headers: {},
                    body: '<script>player.src("https://www.mp4upload.com/files/1080p/video.mp4");</script>',
                    bodyBase64: ''
                });
            };
            var res = await EmbedExtractor.resolve('https://www.mp4upload.com/embed-test12345.html', { name: 'Mp4Upload Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertTrue("Should return at least one stream", arr.isNotEmpty())

        val stream = arr[0].jsonObject
        assertEquals("https://www.mp4upload.com/files/1080p/video.mp4", stream["url"]!!.jsonPrimitive.content)
        assertEquals("1080p", stream["quality"]!!.jsonPrimitive.content)
        assertEquals("https://www.mp4upload.com/", stream["headers"]!!.jsonObject["Referer"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorResolveVidoza() {
        val script = """
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: url,
                    headers: {},
                    body: 'var sourcesCode = [ { src: "https://vidoza.net/files/720p/movie.mp4", res: "720p" } ];',
                    bodyBase64: ''
                });
            };
            var res = await EmbedExtractor.resolve('https://vidoza.net/embed-abcde12345.html', { name: 'Vidoza Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertTrue("Should return at least one stream", arr.isNotEmpty())

        val stream = arr[0].jsonObject
        assertEquals("https://vidoza.net/files/720p/movie.mp4", stream["url"]!!.jsonPrimitive.content)
        assertEquals("720p", stream["quality"]!!.jsonPrimitive.content)
        assertEquals("https://vidoza.net/", stream["headers"]!!.jsonObject["Referer"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorResolveUqload() {
        val script = """
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: url,
                    headers: {},
                    body: 'sources: ["https://uqload.to/files/1080p/stream.mp4"]',
                    bodyBase64: ''
                });
            };
            var res = await EmbedExtractor.resolve('https://uqload.to/embed-uq123.html', { name: 'Uqload Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertTrue("Should return at least one stream", arr.isNotEmpty())

        val stream = arr[0].jsonObject
        assertEquals("https://uqload.to/files/1080p/stream.mp4", stream["url"]!!.jsonPrimitive.content)
        assertEquals("1080p", stream["quality"]!!.jsonPrimitive.content)
        assertEquals("https://uqload.to/", stream["headers"]!!.jsonObject["Referer"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorResolveStreamhub() {
        val script = """
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: url,
                    headers: {},
                    body: 'sources: [ { file: "https://streamhub.ink/files/720p/video.m3u8", label: "720p" } ]',
                    bodyBase64: ''
                });
            };
            var res = await EmbedExtractor.resolve('https://streamhub.ink/e/sh456', { name: 'Streamhub Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertTrue("Should return at least one stream", arr.isNotEmpty())

        val stream = arr[0].jsonObject
        assertEquals("https://streamhub.ink/files/720p/video.m3u8", stream["url"]!!.jsonPrimitive.content)
        assertEquals("720p", stream["quality"]!!.jsonPrimitive.content)
        assertEquals("https://streamhub.ink/", stream["headers"]!!.jsonObject["Referer"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorResolveMediafire() {
        val script = """
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: url,
                    headers: {},
                    body: '<a class="input popsok" id="downloadButton" href="https://download1234.mediafire.com/xyz/video_1080p.mp4">Download</a>',
                    bodyBase64: ''
                });
            };
            var res = await EmbedExtractor.resolve('https://www.mediafire.com/file/mf12345/video.mp4/file', { name: 'Mediafire Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertTrue("Should return at least one stream", arr.isNotEmpty())

        val stream = arr[0].jsonObject
        assertEquals("https://download1234.mediafire.com/xyz/video_1080p.mp4", stream["url"]!!.jsonPrimitive.content)
        assertEquals("1080p", stream["quality"]!!.jsonPrimitive.content)
        assertEquals("https://www.mediafire.com/", stream["headers"]!!.jsonObject["Referer"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorResolveKrakenfiles() {
        val script = """
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: url,
                    headers: {},
                    body: '<video id="player-video" src="https://krakenfiles.com/video/stream_720p.mp4"></video>',
                    bodyBase64: ''
                });
            };
            var res = await EmbedExtractor.resolve('https://krakenfiles.com/view/kf12345/file.html', { name: 'Krakenfiles Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertTrue("Should return at least one stream", arr.isNotEmpty())

        val stream = arr[0].jsonObject
        assertEquals("https://krakenfiles.com/video/stream_720p.mp4", stream["url"]!!.jsonPrimitive.content)
        assertEquals("720p", stream["quality"]!!.jsonPrimitive.content)
        assertEquals("https://krakenfiles.com/", stream["headers"]!!.jsonObject["Referer"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorResolveKwikPackedScript() {
        val script = """
            var packed = "eval(function(p,a,c,k,e,d){while(c--)if(k[c])p=p.replace(new RegExp('\\\\b'+e(c)+'\\\\b','g'),k[c]);return p;}('0 1=\"2\";',3,3,'var|source|https://kwik.cx/hls/1080p/index.m3u8'.split('|')))";
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: url,
                    headers: {},
                    body: '<script>' + packed + '</script>',
                    bodyBase64: ''
                });
            };
            var res = await EmbedExtractor.resolve('https://kwik.cx/e/kw12345', { name: 'Kwik Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertTrue("Should return at least one stream", arr.isNotEmpty())

        val stream = arr[0].jsonObject
        assertEquals("https://kwik.cx/hls/1080p/index.m3u8", stream["url"]!!.jsonPrimitive.content)
        assertEquals("1080p", stream["quality"]!!.jsonPrimitive.content)
        assertEquals("https://kwik.cx/e/kw12345", stream["headers"]!!.jsonObject["Referer"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorFallbackToSniffOnFetchFailure() {
        val script = """
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: false,
                    status: 500,
                    statusText: 'Internal Server Error',
                    url: url,
                    headers: {},
                    body: 'Error page',
                    bodyBase64: ''
                });
            };
            globalThis.__host_browser_resolve = async function() {
                return JSON.stringify({
                    finalUrl: 'https://www.mp4upload.com/embed-test.html',
                    html: '',
                    cookies: {},
                    cookieString: '',
                    userAgent: '',
                    sniffedUrls: ['https://cdn.fallback.com/sniffed_1080p.m3u8'],
                    sniffedMedia: []
                });
            };
            var res = await EmbedExtractor.resolve('https://www.mp4upload.com/embed-test.html', { name: 'Fallback Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertTrue("Fallback should return at least one stream", arr.isNotEmpty())

        val stream = arr[0].jsonObject
        assertEquals("https://cdn.fallback.com/sniffed_1080p.m3u8", stream["url"]!!.jsonPrimitive.content)
        assertEquals("1080p", stream["quality"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorFiltersOutJunkUrlsAndDuplicates() {
        val script = """
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: url,
                    headers: {},
                    body: 'sources: [ { file: "https://cdn.example.com/video.mp4" }, { file: "https://cdn.example.com/video.mp4" }, { file: "https://cdn.example.com/script.js" }, { file: "https://cdn.example.com/image.png" }, { file: "https://cdn.example.com/style.css" }, { file: "https://cdn.example.com/font.woff" } ]',
                    bodyBase64: ''
                });
            };
            var res = await EmbedExtractor.resolve('https://streamhub.ink/e/junktest', { name: 'Sanity Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertEquals("Should filter out duplicates and junk urls, leaving exactly 1 stream", 1, arr.size)

        val stream = arr[0].jsonObject
        assertEquals("https://cdn.example.com/video.mp4", stream["url"]!!.jsonPrimitive.content)
    }

    @Test
    fun testEmbedExtractorGenericQualityLabelling() {
        val script = """
            globalThis.__native_fetch = async function(url) {
                return JSON.stringify({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: url,
                    headers: {},
                    body: 'sources: [' +
                          '  { file: "https://cdn.example.com/4k/video.m3u8" },' +
                          '  { file: "https://cdn.example.com/1080p/video.mp4" },' +
                          '  { file: "https://cdn.example.com/720p/video.mp4" },' +
                          '  { file: "https://cdn.example.com/480p/video.mp4" },' +
                          '  { file: "https://cdn.example.com/360p/video.mp4" },' +
                          '  { file: "https://cdn.example.com/master.m3u8" }' +
                          ']',
                    bodyBase64: ''
                });
            };
            var res = await EmbedExtractor.resolve('https://vidmoly.to/w/testqual', { name: 'Quality Test' });
            console.log(JSON.stringify(res));
        """.trimIndent()

        val output = evaluateJs(script)
        val arr = Json.parseToJsonElement(output).jsonArray
        assertEquals(6, arr.size)

        assertEquals("4K", arr[0].jsonObject["quality"]!!.jsonPrimitive.content)
        assertEquals("1080p", arr[1].jsonObject["quality"]!!.jsonPrimitive.content)
        assertEquals("720p", arr[2].jsonObject["quality"]!!.jsonPrimitive.content)
        assertEquals("480p", arr[3].jsonObject["quality"]!!.jsonPrimitive.content)
        assertEquals("360p", arr[4].jsonObject["quality"]!!.jsonPrimitive.content)
        assertEquals("Auto", arr[5].jsonObject["quality"]!!.jsonPrimitive.content)
    }
}
