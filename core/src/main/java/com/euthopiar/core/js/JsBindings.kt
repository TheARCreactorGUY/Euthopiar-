package com.euthopiar.core.js

object JsBindings {
    val staticPolyfillCode: String = """
            globalThis.SCRAPER_ID = __get_scraper_id();
            globalThis.SCRAPER_SETTINGS = JSON.parse(__get_scraper_settings());
            try { globalThis.__CALL_ARGS__ = JSON.parse(__get_call_args()); } catch(e) { globalThis.__CALL_ARGS__ = {}; }
            if (typeof TMDB_API_KEY === 'undefined') {
                globalThis.TMDB_API_KEY = __get_tmdb_api_key();
            }
            if (typeof globalThis.global === 'undefined') globalThis.global = globalThis;
            if (typeof globalThis.window === 'undefined') globalThis.window = globalThis;
            if (typeof globalThis.self === 'undefined') globalThis.self = globalThis;

            ${storagePolyfill()}
            ${timerPolyfill()}
            ${fetchPolyfill()}
            ${abortControllerPolyfill()}
            ${base64Polyfill()}
            ${bufferPolyfill()}
            ${urlPolyfill()}
            ${cryptoPolyfill()}
            ${textEncoderPolyfill()}
            ${cheerioPolyfill()}
            ${requirePolyfill()}
            ${arrayPolyfill()}
            ${objectPolyfill()}
            ${stringPolyfill()}
            ${browserPolyfill()}
            ${extractorSdkPolyfill()}
        """.trimIndent()

    val staticCallCode: String = """
            (async function() {
                try {
                    var getStreams = module.exports.getStreams || globalThis.getStreams;
                    if (!getStreams) {
                        console.error("getStreams function not found on module.exports or globalThis");
                        __capture_result(JSON.stringify([]));
                        return;
                    }
                    var args = globalThis.__CALL_ARGS__ || JSON.parse(__get_call_args());
                    var season = args.season == null ? undefined : args.season;
                    var episode = args.episode == null ? undefined : args.episode;
                    var effectiveTmdbId = (args.tmdbId && /^\d+${'$'}/.test(args.tmdbId)) ? args.tmdbId : ((args.id && /^\d+${'$'}/.test(args.id)) ? args.id : (args.tmdbId || args.id || args.imdbId));
                    var effectiveType = args.mediaType;
                    var result;
                    try {
                        result = await getStreams(effectiveTmdbId, effectiveType, season, episode);
                    } catch (firstErr) {
                        if (effectiveType === 'tv' || effectiveType === 'anime') {
                            var fallbackType = (effectiveType === 'tv') ? 'anime' : 'tv';
                            try {
                                result = await getStreams(effectiveTmdbId, fallbackType, season, episode);
                            } catch (_fallbackErr) {
                                throw firstErr;
                            }
                        } else {
                            throw firstErr;
                        }
                    }
                    if ((!result || (Array.isArray(result) && result.length === 0)) && (effectiveType === 'tv' || effectiveType === 'anime')) {
                        var fallbackType = (effectiveType === 'tv') ? 'anime' : 'tv';
                        try {
                            var altResult = await getStreams(effectiveTmdbId, fallbackType, season, episode);
                            if (altResult && Array.isArray(altResult) && altResult.length > 0) {
                                result = altResult;
                            }
                        } catch (_fallbackErr) {}
                    }

                    if (Array.isArray(result)) {
                        var unwrapped = [];
                        for (var i = 0; i < result.length; i++) {
                            var item = result[i];
                            var itemUrl = (item && (item.url || item.externalUrl || '')) + '';
                            var cleanItemUrl = itemUrl.split(/[?#]/)[0].toLowerCase();
                            var isDirectMedia = cleanItemUrl.endsWith('.m3u8') || cleanItemUrl.endsWith('.mpd') ||
                                                cleanItemUrl.endsWith('.mp4') || cleanItemUrl.endsWith('.mkv') ||
                                                cleanItemUrl.endsWith('.webm') || cleanItemUrl.indexOf('/hls/') >= 0 ||
                                                cleanItemUrl.indexOf('master.m3u8') >= 0 ||
                                                itemUrl.toLowerCase().startsWith('magnet:') || itemUrl.toLowerCase().startsWith('torrent:');

                            if (!isDirectMedia && typeof EmbedExtractor !== 'undefined' && EmbedExtractor.canHandle(itemUrl)) {
                                try {
                                    var extractedList = await EmbedExtractor.resolve(itemUrl, {
                                        name: item.name || item.title,
                                        title: item.title || item.name,
                                        quality: item.quality,
                                        headers: item.headers
                                    });
                                    if (extractedList && extractedList.length > 0) {
                                        for (var j = 0; j < extractedList.length; j++) {
                                            var ex = extractedList[j];
                                            unwrapped.push({
                                                name: ex.name || item.name,
                                                title: ex.title || item.title || ex.name || item.name,
                                                url: ex.url,
                                                quality: ex.quality || item.quality || 'Auto',
                                                headers: ex.headers || item.headers || {},
                                                subtitles: (item.subtitles || []).concat(ex.subtitles || [])
                                            });
                                        }
                                        continue;
                                    }
                                } catch (unErr) {
                                    console.error('[staticCallCode] Embed unwrap error:', unErr);
                                }
                            }
                            unwrapped.push(item);
                        }
                        result = unwrapped;
                    }

                    __capture_result(JSON.stringify(result || []));
                } catch (e) {
                    console.error("getStreams error:", e && e.message ? e.message : e, e && e.stack ? e.stack : "");
                    __capture_result(JSON.stringify([]));
                }
            })();
        """.trimIndent()

    val staticSettingsCallCode: String = """
            (async function() {
                try {
                    var onSettings = (typeof module !== 'undefined' && module.exports && module.exports.onSettings) || globalThis.onSettings;
                    if (typeof onSettings === 'function') {
                        var layout = await onSettings();
                        __capture_result(JSON.stringify(layout || []));
                    } else {
                        __capture_result("[]");
                    }
                } catch (e) {
                    console.error("onSettings error:", e);
                    __capture_result("[]");
                }
            })();
        """.trimIndent()

    private fun storagePolyfill() = """
        (function() {
            function createStorage() {
                var data = Object.create(null);
                return {
                    getItem: function(key) {
                        var k = String(key);
                        return Object.prototype.hasOwnProperty.call(data, k) ? data[k] : null;
                    },
                    setItem: function(key, value) {
                        data[String(key)] = String(value);
                    },
                    removeItem: function(key) {
                        delete data[String(key)];
                    },
                    clear: function() {
                        data = Object.create(null);
                    },
                    key: function(index) {
                        var keys = Object.keys(data);
                        return index >= 0 && index < keys.length ? keys[index] : null;
                    },
                    get length() {
                        return Object.keys(data).length;
                    }
                };
            }
            if (typeof globalThis.localStorage === 'undefined') {
                globalThis.localStorage = createStorage();
            }
            if (typeof globalThis.sessionStorage === 'undefined') {
                globalThis.sessionStorage = createStorage();
            }
            if (typeof window !== 'undefined') {
                if (!window.localStorage) window.localStorage = globalThis.localStorage;
                if (!window.sessionStorage) window.sessionStorage = globalThis.sessionStorage;
            }
        })();
    """.trimIndent()

    private fun timerPolyfill() = """
        // QuickJS has no event loop or native timers. Back timers with a
        // coroutine delay so callbacks run asynchronously without blocking a worker.
        var __plugin_timer_id = 0;
        var __plugin_timers = Object.create(null);
        function __plugin_delay_value(value, interval) {
            var duration = Number(value);
            if (!isFinite(duration) || duration < 0) duration = 0;
            duration = Math.min(Math.floor(duration), 60000);
            return interval && duration < 1 ? 1 : duration;
        }
        function __plugin_report_timer_error(error) {
            try { console.error('[PluginRuntime] Timer callback failed:', error); } catch (e) {}
        }
        if (typeof globalThis.setTimeout === 'undefined') {
            globalThis.setTimeout = function(callback, delay) {
                if (typeof callback !== 'function') throw new TypeError('Timer callback must be a function');
                var id = ++__plugin_timer_id;
                var args = Array.prototype.slice.call(arguments, 2);
                __plugin_timers[id] = true;
                __plugin_sleep(__plugin_delay_value(delay, false)).then(function() {
                    if (!__plugin_timers[id]) return;
                    delete __plugin_timers[id];
                    try { callback.apply(globalThis, args); } catch (error) { __plugin_report_timer_error(error); }
                }, function(error) {
                    delete __plugin_timers[id];
                    __plugin_report_timer_error(error);
                });
                return id;
            };
        }
        if (typeof globalThis.clearTimeout === 'undefined') {
            globalThis.clearTimeout = function(id) { delete __plugin_timers[id]; };
        }
        if (typeof globalThis.setInterval === 'undefined') {
            globalThis.setInterval = function(callback, delay) {
                if (typeof callback !== 'function') throw new TypeError('Timer callback must be a function');
                var id = ++__plugin_timer_id;
                var duration = __plugin_delay_value(delay, true);
                var args = Array.prototype.slice.call(arguments, 2);
                __plugin_timers[id] = true;
                function tick() {
                    __plugin_sleep(duration).then(function() {
                        if (!__plugin_timers[id]) return;
                        try { callback.apply(globalThis, args); } catch (error) { __plugin_report_timer_error(error); }
                        if (__plugin_timers[id]) tick();
                    }, function(error) {
                        delete __plugin_timers[id];
                        __plugin_report_timer_error(error);
                    });
                }
                tick();
                return id;
            };
        }
        if (typeof globalThis.clearInterval === 'undefined') {
            globalThis.clearInterval = function(id) { delete __plugin_timers[id]; };
        }
    """.trimIndent()

    private fun fetchPolyfill() = """
        function __normalize_fetch_headers(headers) {
            var out = {};
            if (!headers) return out;
            if (typeof headers.forEach === 'function') {
                headers.forEach(function(value, key) { out[key] = String(value); });
                return out;
            }
            if (Array.isArray(headers)) {
                headers.forEach(function(pair) {
                    if (pair && pair.length >= 2) out[pair[0]] = String(pair[1]);
                });
                return out;
            }
            Object.keys(headers).forEach(function(key) { out[key] = String(headers[key]); });
            return out;
        }

        function __fetch_bytes_to_base64(bytes) {
            var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
            var out = '';
            for (var i = 0; i < bytes.length; i += 3) {
                var a = bytes[i];
                var hasB = i + 1 < bytes.length;
                var hasC = i + 2 < bytes.length;
                var b = hasB ? bytes[i + 1] : 0;
                var c = hasC ? bytes[i + 2] : 0;
                out += chars.charAt(a >> 2);
                out += chars.charAt(((a & 3) << 4) | (b >> 4));
                out += hasB ? chars.charAt(((b & 15) << 2) | (c >> 6)) : '=';
                out += hasC ? chars.charAt(c & 63) : '=';
            }
            return out;
        }

        function __fetch_base64_to_bytes(value) {
            var binary = atob(value || '');
            var bytes = new Uint8Array(binary.length);
            for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
            return bytes;
        }

        function __normalize_fetch_body(body) {
            if (body === undefined || body === null) return { kind: 'none', value: '' };
            if (typeof body === 'string') return { kind: 'text', value: body };

            var bytes = null;
            if (typeof ArrayBuffer !== 'undefined' && body instanceof ArrayBuffer) {
                bytes = new Uint8Array(body);
            } else if (typeof ArrayBuffer !== 'undefined' &&
                       typeof ArrayBuffer.isView === 'function' && ArrayBuffer.isView(body)) {
                bytes = new Uint8Array(body.buffer, body.byteOffset, body.byteLength);
            }
            if (bytes !== null) {
                return { kind: 'base64', value: __fetch_bytes_to_base64(bytes) };
            }
            return { kind: 'text', value: String(body) };
        }

        var fetch = async function(url, options) {
            options = options || {};
            var method = (options.method || 'GET').toUpperCase();
            var headers = __normalize_fetch_headers(options.headers);
            var body = __normalize_fetch_body(options.body);
            var followRedirects = options.redirect !== 'manual';
            var result = await __native_fetch(url, method, JSON.stringify(headers), body.kind, body.value, followRedirects);
            var parsed = JSON.parse(result);
            var responseBytes = __fetch_base64_to_bytes(parsed.bodyBase64);
            return {
                ok: parsed.ok,
                status: parsed.status,
                statusText: parsed.statusText,
                url: parsed.url,
                headers: {
                    get: function(name) {
                        return parsed.headers[name.toLowerCase()] || null;
                    }
                },
                arrayBuffer: function() {
                    var copy = new Uint8Array(responseBytes.length);
                    copy.set(responseBytes);
                    return Promise.resolve(copy.buffer);
                },
                text: function() { return Promise.resolve(parsed.body); },
                json: function() {
                    try {
                        if (parsed.body === null || parsed.body === undefined || parsed.body === '') {
                            return Promise.resolve(null);
                        }
                        return Promise.resolve(JSON.parse(parsed.body));
                    } catch (e) {
                        return Promise.resolve(null);
                    }
                }
            };
        };
    """.trimIndent()

    private fun abortControllerPolyfill() = """
        if (typeof AbortSignal === 'undefined') {
            var AbortSignal = function() { this.aborted = false; this.reason = undefined; this._listeners = []; };
            AbortSignal.prototype.addEventListener = function(type, listener) {
                if (type !== 'abort' || typeof listener !== 'function') return;
                this._listeners.push(listener);
            };
            AbortSignal.prototype.removeEventListener = function(type, listener) {
                if (type !== 'abort') return;
                this._listeners = this._listeners.filter(function(l) { return l !== listener; });
            };
            AbortSignal.prototype.dispatchEvent = function(event) {
                if (!event || event.type !== 'abort') return true;
                for (var i = 0; i < this._listeners.length; i++) {
                    try { this._listeners[i].call(this, event); } catch (e) {}
                }
                return true;
            };
            globalThis.AbortSignal = AbortSignal;
        }

        if (typeof AbortController === 'undefined') {
            var AbortController = function() { this.signal = new AbortSignal(); };
            AbortController.prototype.abort = function(reason) {
                if (this.signal.aborted) return;
                this.signal.aborted = true;
                this.signal.reason = reason;
                this.signal.dispatchEvent({ type: 'abort' });
            };
            globalThis.AbortController = AbortController;
        }

        if (typeof globalThis.AbortSignal !== 'undefined' && !globalThis.AbortSignal.timeout) {
            globalThis.AbortSignal.timeout = function(ms) {
                var controller = new globalThis.AbortController();
                setTimeout(function() {
                    controller.abort(new Error("TimeoutError: The operation timed out"));
                }, ms);
                return controller.signal;
            };
        }
    """.trimIndent()

    private fun base64Polyfill() = """
        if (typeof atob === 'undefined') {
            globalThis.atob = function(input) {
                var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
                var str = String(input).replace(/=+$/, '');
                if (str.length % 4 === 1) throw new Error('InvalidCharacterError');
                var output = '';
                var bc = 0, bs, buffer, idx = 0;
                while ((buffer = str.charAt(idx++))) {
                    buffer = chars.indexOf(buffer);
                    if (buffer === -1) continue;
                    bs = bc % 4 ? bs * 64 + buffer : buffer;
                    if (bc++ % 4) output += String.fromCharCode(255 & (bs >> ((-2 * bc) & 6)));
                }
                return output;
            };
        }

        if (typeof btoa === 'undefined') {
            globalThis.btoa = function(input) {
                var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
                var str = String(input);
                var output = '';
                for (var block, charCode, idx = 0, map = chars;
                     str.charAt(idx | 0) || (map = '=', idx % 1);
                     output += map.charAt(63 & (block >> (8 - (idx % 1) * 8)))) {
                    charCode = str.charCodeAt(idx += 3 / 4);
                    if (charCode > 0xFF) throw new Error('InvalidCharacterError');
                    block = (block << 8) | charCode;
                }
                return output;
            };
        }
    """.trimIndent()

    private fun bufferPolyfill() = """
        (function() {
            if (typeof globalThis.Buffer !== 'undefined') return;

            function SimpleBuffer(arg, encodingOrOffset, length) {
                var u8;
                if (typeof arg === 'number') {
                    u8 = new Uint8Array(arg);
                } else if (typeof arg === 'string') {
                    u8 = SimpleBuffer._fromString(arg, encodingOrOffset);
                } else if (ArrayBuffer.isView(arg)) {
                    u8 = new Uint8Array(arg.buffer, arg.byteOffset, arg.byteLength);
                } else if (arg instanceof ArrayBuffer) {
                    u8 = new Uint8Array(arg, encodingOrOffset || 0, length !== undefined ? length : (arg.byteLength - (encodingOrOffset || 0)));
                } else if (Array.isArray(arg)) {
                    u8 = new Uint8Array(arg);
                } else {
                    u8 = new Uint8Array(0);
                }
                Object.setPrototypeOf(u8, SimpleBuffer.prototype);
                return u8;
            }
            Object.setPrototypeOf(SimpleBuffer.prototype, Uint8Array.prototype);

            SimpleBuffer.isBuffer = function(obj) {
                return obj instanceof Uint8Array && (obj._isBuffer === true || Object.getPrototypeOf(obj) === SimpleBuffer.prototype);
            };

            SimpleBuffer.prototype._isBuffer = true;

            SimpleBuffer._fromString = function(str, encoding) {
                var enc = (encoding || 'utf8').toLowerCase().replace(/[^a-z0-9]/g, '');
                if (enc === 'hex') {
                    var clean = str.replace(/[^0-9a-fA-F]/g, '');
                    var len = Math.floor(clean.length / 2);
                    var arr = new Uint8Array(len);
                    for (var i = 0; i < len; i++) {
                        arr[i] = parseInt(clean.substr(i * 2, 2), 16);
                    }
                    return arr;
                } else if (enc === 'base64' || enc === 'base64url') {
                    var b64 = str.replace(/-/g, '+').replace(/_/g, '/');
                    while (b64.length % 4) b64 += '=';
                    var bin = globalThis.atob(b64);
                    var arr = new Uint8Array(bin.length);
                    for (var i = 0; i < bin.length; i++) {
                        arr[i] = bin.charCodeAt(i);
                    }
                    return arr;
                } else if (enc === 'latin1' || enc === 'binary') {
                    var arr = new Uint8Array(str.length);
                    for (var i = 0; i < str.length; i++) {
                        arr[i] = str.charCodeAt(i) & 0xff;
                    }
                    return arr;
                } else {
                    if (typeof TextEncoder !== 'undefined') {
                        return new TextEncoder().encode(str);
                    }
                    var codePoints = [];
                    for (var i = 0; i < str.length; i++) {
                        var c = str.charCodeAt(i);
                        if (c < 0x80) {
                            codePoints.push(c);
                        } else if (c < 0x800) {
                            codePoints.push(0xc0 | (c >> 6), 0x80 | (c & 0x3f));
                        } else if (c < 0xd800 || c >= 0xe000) {
                            codePoints.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f));
                        } else {
                            i++;
                            c = 0x10000 + (((c & 0x3ff) << 10) | (str.charCodeAt(i) & 0x3ff));
                            codePoints.push(0xf0 | (c >> 18), 0x80 | ((c >> 12) & 0x3f), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f));
                        }
                    }
                    return new Uint8Array(codePoints);
                }
            };

            SimpleBuffer.alloc = function(size, fill) {
                var buf = SimpleBuffer(size);
                if (fill !== undefined) {
                    buf.fill(fill);
                }
                return buf;
            };

            SimpleBuffer.from = function(value, encodingOrOffset, length) {
                return SimpleBuffer(value, encodingOrOffset, length);
            };

            SimpleBuffer.concat = function(list, totalLength) {
                if (!Array.isArray(list)) throw new TypeError('"list" argument must be an Array');
                if (list.length === 0) return SimpleBuffer.alloc(0);
                var calcLength = 0;
                for (var i = 0; i < list.length; i++) {
                    calcLength += list[i].length;
                }
                var targetLen = totalLength !== undefined ? totalLength : calcLength;
                var result = SimpleBuffer.alloc(targetLen);
                var offset = 0;
                for (var i = 0; i < list.length; i++) {
                    var item = list[i];
                    for (var j = 0; j < item.length && offset < targetLen; j++) {
                        result[offset++] = item[j];
                    }
                }
                return result;
            };

            SimpleBuffer.prototype.toString = function(encoding, start, end) {
                var enc = (encoding || 'utf8').toLowerCase().replace(/[^a-z0-9]/g, '');
                var s = start || 0;
                var e = end !== undefined ? end : this.length;
                var slice = this.subarray(s, e);
                if (enc === 'hex') {
                    var hex = '';
                    for (var i = 0; i < slice.length; i++) {
                        var h = slice[i].toString(16);
                        hex += (h.length === 1 ? '0' : '') + h;
                    }
                    return hex;
                } else if (enc === 'base64' || enc === 'base64url') {
                    var bin = '';
                    for (var i = 0; i < slice.length; i++) {
                        bin += String.fromCharCode(slice[i]);
                    }
                    var b64 = globalThis.btoa(bin);
                    if (enc === 'base64url') {
                        return b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+${'$'}/, '');
                    }
                    return b64;
                } else if (enc === 'latin1' || enc === 'binary') {
                    var res = '';
                    for (var i = 0; i < slice.length; i++) {
                        res += String.fromCharCode(slice[i]);
                    }
                    return res;
                } else {
                    if (typeof TextDecoder !== 'undefined') {
                        return new TextDecoder().decode(slice);
                    }
                    var res = '';
                    for (var i = 0; i < slice.length; i++) {
                        res += String.fromCharCode(slice[i]);
                    }
                    return decodeURIComponent(escape(res));
                }
            };

            SimpleBuffer.prototype.slice = function(start, end) {
                var sub = this.subarray(start, end);
                Object.setPrototypeOf(sub, SimpleBuffer.prototype);
                return sub;
            };

            globalThis.Buffer = SimpleBuffer;
        })();
    """.trimIndent()

    private fun urlPolyfill() = """
        var __native_parse_url = typeof __parse_url !== 'undefined' ? __parse_url : function(u) { return JSON.stringify({ protocol: '', host: '', hostname: '', port: '', pathname: '/', search: '', hash: '' }); };
        var URL = function(urlString, base) {
            var fullUrl = urlString;
            if (base && !/^https?:\/\//i.test(urlString)) {
                var b = typeof base === 'string' ? base : base.href;
                if (urlString.charAt(0) === '/') {
                    var m = b.match(/^(https?:\/\/[^\/]+)/);
                    fullUrl = m ? m[1] + urlString : urlString;
                } else {
                    fullUrl = b.replace(/\/[^\/]*$/, '/') + urlString;
                }
            }
            var parsed = __native_parse_url(fullUrl);
            var data = JSON.parse(parsed);
            this.href = fullUrl;
            this.protocol = data.protocol;
            this.host = data.host;
            this.hostname = data.hostname;
            this.port = data.port;
            this.pathname = data.pathname;
            this.search = data.search;
            this.hash = data.hash;
            this.origin = data.protocol + '//' + data.host;
            this.searchParams = new URLSearchParams(data.search || '');
        };
        URL.prototype.toString = function() { return this.href; };

        var URLSearchParams = function(init) {
            this._params = {};
            var self = this;
            if (init && typeof init === 'object' && !Array.isArray(init)) {
                Object.keys(init).forEach(function(key) { self._params[key] = String(init[key]); });
            } else if (typeof init === 'string') {
                init.replace(/^\?/, '').split('&').forEach(function(pair) {
                    var parts = pair.split('=');
                    if (parts[0]) self._params[decodeURIComponent(parts[0])] = decodeURIComponent(parts[1] || '');
                });
            }
        };
        URLSearchParams.prototype.toString = function() {
            var self = this;
            return Object.keys(this._params).map(function(key) {
                return encodeURIComponent(key) + '=' + encodeURIComponent(self._params[key]);
            }).join('&');
        };
        URLSearchParams.prototype.get = function(key) { return this._params.hasOwnProperty(key) ? this._params[key] : null; };
        URLSearchParams.prototype.set = function(key, value) { this._params[key] = String(value); };
        URLSearchParams.prototype.append = function(key, value) { this._params[key] = String(value); };
        URLSearchParams.prototype.has = function(key) { return this._params.hasOwnProperty(key); };
        URLSearchParams.prototype.delete = function(key) { delete this._params[key]; };
        URLSearchParams.prototype.keys = function() { return Object.keys(this._params); };
        URLSearchParams.prototype.values = function() {
            var self = this;
            return Object.keys(this._params).map(function(k) { return self._params[k]; });
        };
        URLSearchParams.prototype.entries = function() {
            var self = this;
            return Object.keys(this._params).map(function(k) { return [k, self._params[k]]; });
        };
        URLSearchParams.prototype.forEach = function(callback) {
            var self = this;
            Object.keys(this._params).forEach(function(key) { callback(self._params[key], key, self); });
        };
        URLSearchParams.prototype.getAll = function(key) {
            return this._params.hasOwnProperty(key) ? [this._params[key]] : [];
        };
        URLSearchParams.prototype.sort = function() {
            var sorted = {};
            var self = this;
            Object.keys(this._params).sort().forEach(function(k) { sorted[k] = self._params[k]; });
            this._params = sorted;
        };
    """.trimIndent()

    private fun cryptoPolyfill() = """
        var WordArray = {
            init: function(words, sigBytes) {
                this.words = words || [];
                this.sigBytes = sigBytes != undefined ? sigBytes : this.words.length * 4;
            },
            toString: function(encoder) {
                return (encoder || CryptoJS.enc.Hex).stringify(this);
            },
            concat: function(wordArray) {
                var thisWords = this.words;
                var thatWords = wordArray.words;
                var thisSigBytes = this.sigBytes;
                var thatSigBytes = wordArray.sigBytes;

                this.clamp();

                for (var i = 0; i < thatSigBytes; i++) {
                    var thatByte = (thatWords[i >>> 2] >>> (24 - (i % 4) * 8)) & 0xff;
                    thisWords[(thisSigBytes + i) >>> 2] |= thatByte << (24 - ((thisSigBytes + i) % 4) * 8);
                }
                this.sigBytes += thatSigBytes;
                return this;
            },
            clamp: function() {
                var words = this.words;
                var sigBytes = this.sigBytes;
                if (sigBytes % 4) {
                    words[sigBytes >>> 2] &= 0xffffffff << (32 - (sigBytes % 4) * 8);
                }
                words.length = Math.ceil(sigBytes / 4);
                return this;
            },
            clone: function() {
                return __wordArrayCreate(this.words.slice(0), this.sigBytes);
            }
        };

        function __wordArrayCreate(words, sigBytes) {
            var wa = Object.create(WordArray);
            wa.init(words, sigBytes);
            return wa;
        }

        function __isWordArray(value) {
            return value && typeof value === 'object' && Array.isArray(value.words) && typeof value.sigBytes === 'number';
        }

        function __copyUint8Array(bytes) {
            bytes = __toUint8Array(bytes);
            var copy = new Uint8Array(bytes.length);
            copy.set(bytes);
            return copy;
        }

        function __toUint8Array(data) {
            if (!data) return new Uint8Array(0);
            if (data instanceof Uint8Array) return data;
            if (data instanceof ArrayBuffer) return new Uint8Array(data);
            if (typeof ArrayBuffer !== 'undefined' && ArrayBuffer.isView && ArrayBuffer.isView(data)) {
                return new Uint8Array(data.buffer, data.byteOffset || 0, data.byteLength);
            }
            if (Array.isArray(data)) return new Uint8Array(data);
            if (typeof data.length === 'number') return new Uint8Array(Array.prototype.slice.call(data));
            return new Uint8Array(0);
        }

        function __bytesToArrayBuffer(bytes) {
            return __copyUint8Array(bytes).buffer;
        }

        function __wordArrayToBytes(wordArray) {
            if (!__isWordArray(wordArray)) return typeof wordArray === 'string' ? new TextEncoder().encode(wordArray) : __toUint8Array(wordArray);
            var bytes = new Uint8Array(wordArray.sigBytes);
            for (var i = 0; i < wordArray.sigBytes; i++) {
                bytes[i] = (wordArray.words[i >>> 2] >>> (24 - (i % 4) * 8)) & 0xff;
            }
            return bytes;
        }

        function __bytesToWordArray(bytes) {
            bytes = __toUint8Array(bytes);
            var words = [];
            for (var i = 0; i < bytes.length; i++) {
                words[i >>> 2] |= (bytes[i] & 0xff) << (24 - (i % 4) * 8);
            }
            return __wordArrayCreate(words, bytes.length);
        }

        function __normalizeWordArrayInput(value) {
            if (__isWordArray(value)) return __wordArrayToBytes(value);
            if (typeof value === 'string') return new TextEncoder().encode(value);
            return __toUint8Array(value);
        }

        function __bytesToHex(bytes) {
            bytes = __toUint8Array(bytes);
            var out = [];
            for (var i = 0; i < bytes.length; i++) {
                var hex = bytes[i].toString(16);
                out.push(hex.length < 2 ? '0' + hex : hex);
            }
            return out.join('');
        }

        function __hexToBytes(hex) {
            hex = String(hex || '').replace(/[^0-9a-fA-F]/g, '');
            if (hex.length % 2) hex = '0' + hex;
            var bytes = new Uint8Array(hex.length / 2);
            for (var i = 0; i < hex.length; i += 2) {
                bytes[i / 2] = parseInt(hex.substr(i, 2), 16) & 0xff;
            }
            return bytes;
        }

        function __concatBytes() {
            var total = 0;
            var parts = [];
            for (var i = 0; i < arguments.length; i++) {
                var part = __toUint8Array(arguments[i]);
                parts.push(part);
                total += part.length;
            }
            var out = new Uint8Array(total);
            var offset = 0;
            for (var j = 0; j < parts.length; j++) {
                out.set(parts[j], offset);
                offset += parts[j].length;
            }
            return out;
        }

        function __normalizeHashName(hash) {
            var name = hash && hash.name ? hash.name : hash;
            name = String(name || 'SHA-256').toUpperCase().replace(/[^A-Z0-9]/g, '');
            if (name === 'SHA1' || name === 'SHA256' || name === 'SHA384' || name === 'SHA512' || name === 'MD5') return name;
            throw new Error('Unsupported hash algorithm: ' + name);
        }

        function __normalizeAlgorithmName(algo) {
            var name = algo && algo.name ? algo.name : algo;
            name = String(name || '').toUpperCase();
            if (name.indexOf('AES-GCM') >= 0) return 'AES-GCM';
            if (name.indexOf('AES-CBC') >= 0) return 'AES-CBC';
            if (name.indexOf('AES-ECB') >= 0 || name === 'ECB') return 'AES-ECB';
            if (name.indexOf('PBKDF2') >= 0) return 'PBKDF2';
            if (name.indexOf('HMAC') >= 0) return 'HMAC';
            if (name.indexOf('RSASSA-PKCS1') >= 0) return 'RSASSA-PKCS1-V1_5';
            if (name.indexOf('ECDSA') >= 0) return 'ECDSA';
            return name;
        }

        function __aesModeName(mode, padding) {
            var normalized = __normalizeAlgorithmName(mode || 'AES-CBC');
            if (padding === CryptoJS.pad.NoPadding || padding === 'NoPadding') normalized += '-NoPadding';
            return normalized;
        }

        function __nativeDigestBytes(hash, dataBytes) {
            if (typeof __crypto_digest_hex_raw === 'undefined') throw new Error('Native digest bridge is unavailable');
            return __hexToBytes(__crypto_digest_hex_raw(__normalizeHashName(hash), __bytesToHex(dataBytes)));
        }

        function __nativeHmacBytes(hash, keyBytes, dataBytes) {
            if (typeof __crypto_hmac_hex_raw === 'undefined') throw new Error('Native HMAC bridge is unavailable');
            return __hexToBytes(__crypto_hmac_hex_raw(__normalizeHashName(hash), __bytesToHex(keyBytes), __bytesToHex(dataBytes)));
        }

        function __nativePbkdf2Bytes(passwordBytes, saltBytes, iterations, keySizeBits, hash) {
            if (typeof __crypto_pbkdf2_hex === 'undefined') throw new Error('Native PBKDF2 bridge is unavailable');
            return __hexToBytes(__crypto_pbkdf2_hex(__bytesToHex(passwordBytes), __bytesToHex(saltBytes), iterations, keySizeBits, __normalizeHashName(hash)));
        }

        function __nativeAesBytes(encrypt, mode, keyBytes, ivBytes, dataBytes) {
            var fn = encrypt ? __crypto_aes_encrypt_hex : __crypto_aes_decrypt_hex;
            if (typeof fn === 'undefined') throw new Error('Native AES bridge is unavailable');
            return __hexToBytes(fn(mode, __bytesToHex(keyBytes), __bytesToHex(ivBytes), __bytesToHex(dataBytes)));
        }

        function __evpKdf(passwordBytes, saltBytes, keySizeBytes, ivSizeBytes) {
            var targetSize = keySizeBytes + ivSizeBytes;
            var derived = new Uint8Array(targetSize);
            var block = new Uint8Array(0);
            var offset = 0;
            while (offset < targetSize) {
                block = __nativeDigestBytes('MD5', __concatBytes(block, passwordBytes, saltBytes || new Uint8Array(0)));
                var take = Math.min(block.length, targetSize - offset);
                derived.set(block.subarray(0, take), offset);
                offset += take;
            }
            return {
                key: derived.subarray(0, keySizeBytes),
                iv: derived.subarray(keySizeBytes, keySizeBytes + ivSizeBytes)
            };
        }

        function __opensslSaltHeader() {
            return new Uint8Array([83, 97, 108, 116, 101, 100, 95, 95]);
        }

        function __hasOpenSslSaltHeader(bytes) {
            var header = __opensslSaltHeader();
            if (!bytes || bytes.length < 16) return false;
            for (var i = 0; i < header.length; i++) {
                if (bytes[i] !== header[i]) return false;
            }
            return true;
        }

        function __makeCipherParams(ciphertext, key, iv, salt, mode) {
            return {
                ciphertext: __bytesToWordArray(ciphertext),
                key: key ? __bytesToWordArray(key) : undefined,
                iv: iv ? __bytesToWordArray(iv) : undefined,
                salt: salt ? __bytesToWordArray(salt) : undefined,
                mode: mode,
                toString: function(formatter) {
                    return (formatter || CryptoJS.format.OpenSSL).stringify(this);
                }
            };
        }

        var CryptoJS = {
            enc: {
                Hex: {
                    stringify: function(wordArray) {
                        return __bytesToHex(__wordArrayToBytes(wordArray));
                    },
                    parse: function(hexStr) {
                        return __bytesToWordArray(__hexToBytes(hexStr));
                    }
                },
                Utf8: {
                    stringify: function(wordArray) {
                        return new TextDecoder('utf-8').decode(__wordArrayToBytes(wordArray));
                    },
                    parse: function(utf8Str) {
                        return __bytesToWordArray(new TextEncoder().encode(String(utf8Str)));
                    }
                },
                Latin1: {
                    stringify: function(wordArray) {
                        var bytes = __wordArrayToBytes(wordArray);
                        var out = '';
                        for (var i = 0; i < bytes.length; i++) out += String.fromCharCode(bytes[i]);
                        return out;
                    },
                    parse: function(str) {
                        str = String(str || '');
                        var bytes = new Uint8Array(str.length);
                        for (var i = 0; i < str.length; i++) bytes[i] = str.charCodeAt(i) & 0xff;
                        return __bytesToWordArray(bytes);
                    }
                },
                Base64: {
                    stringify: function(wordArray) {
                        var bytes = __wordArrayToBytes(wordArray);
                        var binaryStr = '';
                        for (var j = 0; j < bytes.length; j++) binaryStr += String.fromCharCode(bytes[j]);
                        return btoa(binaryStr);
                    },
                    parse: function(base64Str) {
                        var binaryStr = atob(String(base64Str || ''));
                        var bytes = new Uint8Array(binaryStr.length);
                        for (var i = 0; i < binaryStr.length; i++) bytes[i] = binaryStr.charCodeAt(i) & 0xff;
                        return __bytesToWordArray(bytes);
                    }
                },
                Base64url: {
                    stringify: function(wordArray) {
                        return CryptoJS.enc.Base64.stringify(wordArray).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
                    },
                    parse: function(str) {
                        str = String(str || '').replace(/-/g, '+').replace(/_/g, '/');
                        while (str.length % 4) str += '=';
                        return CryptoJS.enc.Base64.parse(str);
                    }
                }
            },
            lib: {
                WordArray: {
                    create: function(words, sigBytes) {
                        if (words == null) return __wordArrayCreate([], sigBytes || 0);
                        if (__isWordArray(words)) return words.clone();
                        if (typeof words === 'string') return CryptoJS.enc.Utf8.parse(words);
                        if (words instanceof ArrayBuffer || (typeof ArrayBuffer !== 'undefined' && ArrayBuffer.isView && ArrayBuffer.isView(words))) {
                            var bytes = __toUint8Array(words);
                            return __bytesToWordArray(sigBytes != undefined ? bytes.subarray(0, sigBytes) : bytes);
                        }
                        return __wordArrayCreate(words, sigBytes);
                    },
                    random: function(nBytes) {
                        var bytes = new Uint8Array(nBytes || 0);
                        globalThis.crypto.getRandomValues(bytes);
                        return __bytesToWordArray(bytes);
                    }
                },
                CipherParams: {
                    create: function(params) {
                        params = params || {};
                        params.toString = params.toString || function(formatter) {
                            return (formatter || CryptoJS.format.OpenSSL).stringify(this);
                        };
                        return params;
                    }
                }
            },
            format: {
                OpenSSL: {
                    stringify: function(cipherParams) {
                        var cipherBytes = __wordArrayToBytes(cipherParams.ciphertext);
                        var out = cipherParams.salt
                            ? __concatBytes(__opensslSaltHeader(), __wordArrayToBytes(cipherParams.salt), cipherBytes)
                            : cipherBytes;
                        return CryptoJS.enc.Base64.stringify(__bytesToWordArray(out));
                    },
                    parse: function(str) {
                        var bytes = __wordArrayToBytes(CryptoJS.enc.Base64.parse(str));
                        if (__hasOpenSslSaltHeader(bytes)) {
                            return CryptoJS.lib.CipherParams.create({
                                salt: __bytesToWordArray(bytes.subarray(8, 16)),
                                ciphertext: __bytesToWordArray(bytes.subarray(16))
                            });
                        }
                        return CryptoJS.lib.CipherParams.create({ ciphertext: __bytesToWordArray(bytes) });
                    }
                }
            },
            mode: { CBC: 'AES-CBC', GCM: 'AES-GCM', ECB: 'AES-ECB' },
            pad: { Pkcs7: 'Pkcs7', NoPadding: 'NoPadding' },
            algo: { MD5: 'MD5', SHA1: 'SHA1', SHA256: 'SHA256', SHA384: 'SHA384', SHA512: 'SHA512', AES: 'AES' },
            MD5: function(m) { return __bytesToWordArray(__nativeDigestBytes('MD5', __normalizeWordArrayInput(m))); },
            SHA1: function(m) { return __bytesToWordArray(__nativeDigestBytes('SHA1', __normalizeWordArrayInput(m))); },
            SHA256: function(m) { return __bytesToWordArray(__nativeDigestBytes('SHA256', __normalizeWordArrayInput(m))); },
            SHA384: function(m) { return __bytesToWordArray(__nativeDigestBytes('SHA384', __normalizeWordArrayInput(m))); },
            SHA512: function(m) { return __bytesToWordArray(__nativeDigestBytes('SHA512', __normalizeWordArrayInput(m))); },
            HmacMD5: function(m, k) { return __bytesToWordArray(__nativeHmacBytes('MD5', __normalizeWordArrayInput(k), __normalizeWordArrayInput(m))); },
            HmacSHA1: function(m, k) { return __bytesToWordArray(__nativeHmacBytes('SHA1', __normalizeWordArrayInput(k), __normalizeWordArrayInput(m))); },
            HmacSHA256: function(m, k) { return __bytesToWordArray(__nativeHmacBytes('SHA256', __normalizeWordArrayInput(k), __normalizeWordArrayInput(m))); },
            HmacSHA384: function(m, k) { return __bytesToWordArray(__nativeHmacBytes('SHA384', __normalizeWordArrayInput(k), __normalizeWordArrayInput(m))); },
            HmacSHA512: function(m, k) { return __bytesToWordArray(__nativeHmacBytes('SHA512', __normalizeWordArrayInput(k), __normalizeWordArrayInput(m))); },
            PBKDF2: function(pass, salt, options) {
                options = options || {};
                var pBytes = __normalizeWordArrayInput(pass);
                var sBytes = __normalizeWordArrayInput(salt);
                var iter = options.iterations || 1000;
                var kSize = options.keySize || 8;
                var algo = options.hasher || 'SHA1';
                return __bytesToWordArray(__nativePbkdf2Bytes(pBytes, sBytes, iter, kSize * 32, algo));
            },
            AES: {
                encrypt: function(message, key, options) {
                    options = options || {};
                    var data = __normalizeWordArrayInput(message);
                    var kBytes;
                    var ivBytes;
                    var saltBytes;
                    var isPassphrase = typeof key === 'string';
                    if (isPassphrase) {
                        saltBytes = options.salt ? __wordArrayToBytes(options.salt) : __wordArrayToBytes(CryptoJS.lib.WordArray.random(8));
                        var derived = __evpKdf(new TextEncoder().encode(key), saltBytes, 32, 16);
                        kBytes = derived.key;
                        ivBytes = options.iv ? __wordArrayToBytes(options.iv) : derived.iv;
                    } else {
                        kBytes = __wordArrayToBytes(key);
                        ivBytes = options.iv ? __wordArrayToBytes(options.iv) : new Uint8Array(0);
                    }
                    var mode = __aesModeName(options.mode || 'AES-CBC', options.padding);
                    var resBytes = __nativeAesBytes(true, mode, kBytes, ivBytes, data);
                    return __makeCipherParams(resBytes, kBytes, ivBytes, saltBytes, mode);
                },
                decrypt: function(cipher, key, options) {
                    options = options || {};
                    var cipherParams = typeof cipher === 'string' ? CryptoJS.format.OpenSSL.parse(cipher) : cipher;
                    var data = cipherParams.ciphertext ? __wordArrayToBytes(cipherParams.ciphertext) : __toUint8Array(cipherParams);
                    var kBytes;
                    var ivBytes;
                    var isPassphrase = typeof key === 'string';
                    if (isPassphrase) {
                        var saltBytes = options.salt ? __wordArrayToBytes(options.salt) : (cipherParams.salt ? __wordArrayToBytes(cipherParams.salt) : new Uint8Array(0));
                        var derived = __evpKdf(new TextEncoder().encode(key), saltBytes, 32, 16);
                        kBytes = derived.key;
                        ivBytes = options.iv ? __wordArrayToBytes(options.iv) : derived.iv;
                    } else {
                        kBytes = __wordArrayToBytes(key);
                        ivBytes = options.iv ? __wordArrayToBytes(options.iv) : new Uint8Array(0);
                    }
                    var mode = __aesModeName(options.mode || 'AES-CBC', options.padding);
                    return __bytesToWordArray(__nativeAesBytes(false, mode, kBytes, ivBytes, data));
                }
            }
        };
        globalThis.CryptoJS = CryptoJS;

        function __makeCryptoKey(type, algorithm, extractable, usages, rawBytes) {
            return {
                type: type,
                extractable: !!extractable,
                algorithm: algorithm,
                usages: usages || [],
                _raw: __copyUint8Array(rawBytes)
            };
        }

        function __webCryptoAlgorithm(algo) {
            var name = __normalizeAlgorithmName(algo);
            var out = { name: name };
            if (algo && typeof algo === 'object' && algo.length) out.length = algo.length;
            if (algo && typeof algo === 'object' && algo.hash) out.hash = { name: __normalizeHashName(algo.hash) };
            return out;
        }

        function __signatureAlgorithmName(algo, key) {
            var name = __normalizeAlgorithmName(algo || (key && key.algorithm));
            var hash = algo && algo.hash ? __normalizeHashName(algo.hash) : (key && key.algorithm && key.algorithm.hash ? key.algorithm.hash.name : 'SHA256');
            if (name === 'RSASSA-PKCS1-V1_5') return 'RSASSA-PKCS1-V1_5-' + hash;
            if (name === 'ECDSA') return 'ECDSA-' + hash;
            return name;
        }

        globalThis.crypto = {
            subtle: {
                digest: async function(algo, data) {
                    return __bytesToArrayBuffer(__nativeDigestBytes(algo, __toUint8Array(data)));
                },
                importKey: async function(fmt, data, algo, extractable, usages) {
                    fmt = String(fmt || 'raw').toLowerCase();
                    if (fmt !== 'raw' && fmt !== 'pkcs8' && fmt !== 'spki') throw new Error('Unsupported key format: ' + fmt);
                    var algorithm = __webCryptoAlgorithm(algo || {});
                    var type = fmt === 'spki' ? 'public' : (fmt === 'pkcs8' ? 'private' : 'secret');
                    return __makeCryptoKey(type, algorithm, extractable, usages || [], __toUint8Array(data));
                },
                exportKey: async function(fmt, key) {
                    fmt = String(fmt || 'raw').toLowerCase();
                    if (fmt !== 'raw' && fmt !== 'pkcs8' && fmt !== 'spki') throw new Error('Unsupported key format: ' + fmt);
                    return __bytesToArrayBuffer(key._raw);
                },
                generateKey: async function(algo, extractable, usages) {
                    var algorithm = __webCryptoAlgorithm(algo || {});
                    if (algorithm.name !== 'AES-CBC' && algorithm.name !== 'AES-GCM' && algorithm.name !== 'HMAC') {
                        throw new Error('Unsupported generateKey algorithm: ' + algorithm.name);
                    }
                    var length = algorithm.length || 256;
                    var bytes = new Uint8Array(length / 8);
                    globalThis.crypto.getRandomValues(bytes);
                    return __makeCryptoKey('secret', algorithm, extractable, usages || [], bytes);
                },
                deriveBits: async function(params, key, len) {
                    if (__normalizeAlgorithmName(params) !== 'PBKDF2') throw new Error('Only PBKDF2 deriveBits is supported');
                    var pBytes = __toUint8Array(key._raw);
                    var sBytes = __toUint8Array(params.salt);
                    var hash = params.hash || 'SHA-256';
                    return __bytesToArrayBuffer(__nativePbkdf2Bytes(pBytes, sBytes, params.iterations || 1000, len, hash));
                },
                deriveKey: async function(params, key, derivedKeyAlgo, extractable, usages) {
                    var algorithm = __webCryptoAlgorithm(derivedKeyAlgo || {});
                    var length = algorithm.length || 256;
                    var raw = await globalThis.crypto.subtle.deriveBits(params, key, length);
                    return __makeCryptoKey('secret', algorithm, extractable, usages || [], new Uint8Array(raw));
                },
                encrypt: async function(params, key, data) {
                    var mode = __normalizeAlgorithmName(params);
                    if (mode !== 'AES-CBC' && mode !== 'AES-GCM') throw new Error('Unsupported encrypt algorithm: ' + mode);
                    if (mode === 'AES-GCM' && params.tagLength && params.tagLength !== 128) throw new Error('Only 128-bit AES-GCM tags are supported');
                    if (mode === 'AES-GCM' && params.additionalData) throw new Error('AES-GCM additionalData is not supported');
                    var ivBytes = __toUint8Array(params.iv || new Uint8Array(0));
                    return __bytesToArrayBuffer(__nativeAesBytes(true, mode, __toUint8Array(key._raw), ivBytes, __toUint8Array(data)));
                },
                decrypt: async function(params, key, data) {
                    var mode = __normalizeAlgorithmName(params);
                    if (mode !== 'AES-CBC' && mode !== 'AES-GCM') throw new Error('Unsupported decrypt algorithm: ' + mode);
                    if (mode === 'AES-GCM' && params.tagLength && params.tagLength !== 128) throw new Error('Only 128-bit AES-GCM tags are supported');
                    if (mode === 'AES-GCM' && params.additionalData) throw new Error('AES-GCM additionalData is not supported');
                    var ivBytes = __toUint8Array(params.iv || new Uint8Array(0));
                    return __bytesToArrayBuffer(__nativeAesBytes(false, mode, __toUint8Array(key._raw), ivBytes, __toUint8Array(data)));
                },
                sign: async function(algo, key, data) {
                    if (__normalizeAlgorithmName(algo || key.algorithm) === 'HMAC' || key.algorithm.name === 'HMAC') {
                        var hash = (algo && algo.hash) || (key.algorithm && key.algorithm.hash) || 'SHA-256';
                        return __bytesToArrayBuffer(__nativeHmacBytes(hash, __toUint8Array(key._raw), __toUint8Array(data)));
                    }
                    if (typeof __crypto_sign_hex === 'undefined') throw new Error('Native signature bridge is unavailable');
                    var sigHex = __crypto_sign_hex(__signatureAlgorithmName(algo, key), __bytesToHex(key._raw), __bytesToHex(__toUint8Array(data)));
                    return __bytesToArrayBuffer(__hexToBytes(sigHex));
                },
                verify: async function(algo, key, sig, data) {
                    if (__normalizeAlgorithmName(algo || key.algorithm) === 'HMAC' || key.algorithm.name === 'HMAC') {
                        var expected = __nativeHmacBytes((algo && algo.hash) || (key.algorithm && key.algorithm.hash) || 'SHA-256', __toUint8Array(key._raw), __toUint8Array(data));
                        var actual = __toUint8Array(sig);
                        if (expected.length !== actual.length) return false;
                        var diff = 0;
                        for (var i = 0; i < expected.length; i++) diff |= expected[i] ^ actual[i];
                        return diff === 0;
                    }
                    if (typeof __crypto_verify_hex === 'undefined') throw new Error('Native signature bridge is unavailable');
                    return __crypto_verify_hex(__signatureAlgorithmName(algo, key), __bytesToHex(key._raw), __bytesToHex(__toUint8Array(sig)), __bytesToHex(__toUint8Array(data)));
                }
            },
            getRandomValues: function(arr) {
                if (!arr) return arr;
                var byteLength = arr.byteLength != undefined ? arr.byteLength : arr.length;
                if (!byteLength) return arr;
                if (typeof __crypto_get_random_values_hex === 'undefined') throw new Error('Native random bridge is unavailable');
                var random = __hexToBytes(__crypto_get_random_values_hex(byteLength));
                if (arr.buffer && arr.byteLength != undefined) {
                    new Uint8Array(arr.buffer, arr.byteOffset || 0, arr.byteLength).set(random);
                } else {
                    for (var i = 0; i < arr.length; i++) arr[i] = random[i] || 0;
                }
                return arr;
            },
            randomUUID: function() {
                var b = new Uint8Array(16);
                globalThis.crypto.getRandomValues(b);
                b[6] = (b[6] & 0x0f) | 0x40;
                b[8] = (b[8] & 0x3f) | 0x80;
                var h = __bytesToHex(b);
                return h.substr(0, 8) + '-' + h.substr(8, 4) + '-' + h.substr(12, 4) + '-' + h.substr(16, 4) + '-' + h.substr(20);
            }
        };

        // WebAssembly placeholder
        globalThis.WebAssembly = {
            instantiate: async function(bufferSource, importObject) {
                console.warn("WebAssembly.instantiate called (placeholder)");
                return { instance: { exports: {} }, module: {} };
            }
        };
    """.trimIndent()

    private fun textEncoderPolyfill() = """
        if (typeof TextEncoder === 'undefined') {
            globalThis.TextEncoder = function() {};
            TextEncoder.prototype.encode = function(str) {
                var hex = __crypto_utf8_to_hex(str);
                var bytes = new Uint8Array(hex.length / 2);
                for (var i = 0; i < hex.length; i += 2) {
                    bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
                }
                return bytes;
            };
        }
        if (typeof TextDecoder === 'undefined') {
            globalThis.TextDecoder = function() {};
            TextDecoder.prototype.decode = function(data) {
                var bytes = data;
                if (data instanceof ArrayBuffer) bytes = new Uint8Array(data);
                var hex = '';
                for (var i = 0; i < bytes.length; i++) {
                    hex += bytes[i].toString(16).padStart(2, '0');
                }
                return __crypto_hex_to_utf8(hex);
            };
        }
    """.trimIndent()

    private fun cheerioPolyfill() = """
        var cheerio = {
            load: function(html) {
                var docId = __cheerio_load(html);
                var $ = function(selector, context) {
                    if (selector && selector._elementIds) return selector;
                    if (context && context._elementIds && context._elementIds.length > 0) {
                        var allIds = [];
                        for (var i = 0; i < context._elementIds.length; i++) {
                            var childIdsJson = __cheerio_find(docId, context._elementIds[i], selector);
                            var childIds = JSON.parse(childIdsJson);
                            allIds = allIds.concat(childIds);
                        }
                        return createCheerioWrapperFromIds(docId, allIds);
                    }
                    return createCheerioWrapper(docId, selector);
                };
                $.html = function(el) {
                    if (el && el._elementIds && el._elementIds.length > 0) {
                        return __cheerio_html(docId, el._elementIds[0]);
                    }
                    return __cheerio_html(docId, '');
                };
                return $;
            }
        };

        function createCheerioWrapper(docId, selector) {
            var elementIds;
            if (typeof selector === 'string') {
                var idsJson = __cheerio_select(docId, selector);
                elementIds = JSON.parse(idsJson);
            } else {
                elementIds = [];
            }
            return createCheerioWrapperFromIds(docId, elementIds);
        }

        function createCheerioWrapperFromIds(docId, ids) {
            var wrapper = {
                _docId: docId,
                _elementIds: ids,
                length: ids.length,
                each: function(callback) {
                    for (var i = 0; i < ids.length; i++) {
                        var elWrapper = createCheerioWrapperFromIds(docId, [ids[i]]);
                        callback.call(elWrapper, i, elWrapper);
                    }
                    return wrapper;
                },
                find: function(sel) {
                    var allIds = [];
                    for (var i = 0; i < ids.length; i++) {
                        var childIdsJson = __cheerio_find(docId, ids[i], sel);
                        var childIds = JSON.parse(childIdsJson);
                        allIds = allIds.concat(childIds);
                    }
                    return createCheerioWrapperFromIds(docId, allIds);
                },
                text: function() {
                    if (ids.length === 0) return '';
                    return __cheerio_text(docId, ids.join(','));
                },
                html: function() {
                    if (ids.length === 0) return '';
                    return __cheerio_inner_html(docId, ids[0]);
                },
                attr: function(name) {
                    if (ids.length === 0) return undefined;
                    var val = __cheerio_attr(docId, ids[0], name);
                    return val === '__UNDEFINED__' ? undefined : val;
                },
                first: function() { return createCheerioWrapperFromIds(docId, ids.length > 0 ? [ids[0]] : []); },
                last: function() { return createCheerioWrapperFromIds(docId, ids.length > 0 ? [ids[ids.length - 1]] : []); },
                next: function() {
                    var nextIds = [];
                    for (var i = 0; i < ids.length; i++) {
                        var nextId = __cheerio_next(docId, ids[i]);
                        if (nextId && nextId !== '__NONE__') nextIds.push(nextId);
                    }
                    return createCheerioWrapperFromIds(docId, nextIds);
                },
                prev: function() {
                    var prevIds = [];
                    for (var i = 0; i < ids.length; i++) {
                        var prevId = __cheerio_prev(docId, ids[i]);
                        if (prevId && prevId !== '__NONE__') prevIds.push(prevId);
                    }
                    return createCheerioWrapperFromIds(docId, prevIds);
                },
                eq: function(index) {
                    if (index >= 0 && index < ids.length) return createCheerioWrapperFromIds(docId, [ids[index]]);
                    return createCheerioWrapperFromIds(docId, []);
                },
                get: function(index) {
                    if (typeof index === 'number') {
                        if (index >= 0 && index < ids.length) return createCheerioWrapperFromIds(docId, [ids[index]]);
                        return undefined;
                    }
                    return ids.map(function(id) { return createCheerioWrapperFromIds(docId, [id]); });
                },
                map: function(callback) {
                    var results = [];
                    for (var i = 0; i < ids.length; i++) {
                        var elWrapper = createCheerioWrapperFromIds(docId, [ids[i]]);
                        var result = callback.call(elWrapper, i, elWrapper);
                        if (result !== undefined && result !== null) results.push(result);
                    }
                    return {
                        length: results.length,
                        get: function(index) { return typeof index === 'number' ? results[index] : results; },
                        toArray: function() { return results; }
                    };
                },
                filter: function(selectorOrCallback) {
                    if (typeof selectorOrCallback === 'function') {
                        var filteredIds = [];
                        for (var i = 0; i < ids.length; i++) {
                            var elWrapper = createCheerioWrapperFromIds(docId, [ids[i]]);
                            var result = selectorOrCallback.call(elWrapper, i, elWrapper);
                            if (result) filteredIds.push(ids[i]);
                        }
                        return createCheerioWrapperFromIds(docId, filteredIds);
                    }
                    return wrapper;
                },
                children: function(sel) { return this.find(sel || '*'); },
                parent: function() { return createCheerioWrapperFromIds(docId, []); },
                toArray: function() { return ids.map(function(id) { return createCheerioWrapperFromIds(docId, [id]); }); }
            };
            return wrapper;
        }
    """.trimIndent()

    private fun requirePolyfill() = """
        var require = function(moduleName) {
            if (moduleName === 'cheerio' || moduleName === 'cheerio-without-node-native' || moduleName === 'react-native-cheerio') {
                return cheerio;
            }
            if (moduleName === 'crypto-js') {
                return CryptoJS;
            }
            if (moduleName === 'embed-extractor') {
                return EmbedExtractor;
            }
            if (moduleName === 'buffer' || moduleName === 'node:buffer') {
                return { Buffer: globalThis.Buffer };
            }
            if (moduleName === 'crypto' || moduleName === 'node:crypto') {
                return {
                    createHash: function(algo) {
                        var _data = '';
                        return {
                            update: function(d) {
                                if (typeof d === 'string') _data += d;
                                else if (d && d.toString) _data += d.toString('binary');
                                return this;
                            },
                            digest: function(enc) {
                                var wa = CryptoJS.enc.Latin1.parse(_data);
                                var hash;
                                var a = (algo || 'sha256').toLowerCase().replace(/[^a-z0-9]/g, '');
                                if (a === 'md5') hash = CryptoJS.MD5(wa);
                                else if (a === 'sha1') hash = CryptoJS.SHA1(wa);
                                else if (a === 'sha512') hash = CryptoJS.SHA512(wa);
                                else hash = CryptoJS.SHA256(wa);
                                if (enc === 'hex') return hash.toString(CryptoJS.enc.Hex);
                                if (enc === 'base64') return hash.toString(CryptoJS.enc.Base64);
                                if (enc === 'latin1' || enc === 'binary') return hash.toString(CryptoJS.enc.Latin1);
                                if (globalThis.Buffer) return globalThis.Buffer.from(hash.toString(CryptoJS.enc.Hex), 'hex');
                                return hash.toString(CryptoJS.enc.Hex);
                            }
                        };
                    },
                    createHmac: function(algo, key) {
                        var _data = '';
                        var k = typeof key === 'string' ? key : (key && key.toString ? key.toString('utf8') : '');
                        return {
                            update: function(d) {
                                if (typeof d === 'string') _data += d;
                                else if (d && d.toString) _data += d.toString('binary');
                                return this;
                            },
                            digest: function(enc) {
                                var wa = CryptoJS.enc.Latin1.parse(_data);
                                var hmac;
                                var a = (algo || 'sha256').toLowerCase().replace(/[^a-z0-9]/g, '');
                                if (a === 'sha1') hmac = CryptoJS.HmacSHA1(wa, k);
                                else if (a === 'md5') hmac = CryptoJS.HmacMD5(wa, k);
                                else if (a === 'sha512') hmac = CryptoJS.HmacSHA512(wa, k);
                                else hmac = CryptoJS.HmacSHA256(wa, k);
                                if (enc === 'hex') return hmac.toString(CryptoJS.enc.Hex);
                                if (enc === 'base64') return hmac.toString(CryptoJS.enc.Base64);
                                if (enc === 'latin1' || enc === 'binary') return hmac.toString(CryptoJS.enc.Latin1);
                                if (globalThis.Buffer) return globalThis.Buffer.from(hmac.toString(CryptoJS.enc.Hex), 'hex');
                                return hmac.toString(CryptoJS.enc.Hex);
                            }
                        };
                    },
                    randomBytes: function(size) {
                        var arr = new Uint8Array(size);
                        if (globalThis.crypto && globalThis.crypto.getRandomValues) {
                            globalThis.crypto.getRandomValues(arr);
                        } else {
                            for (var i = 0; i < size; i++) arr[i] = Math.floor(Math.random() * 256);
                        }
                        return globalThis.Buffer ? globalThis.Buffer.from(arr) : arr;
                    }
                };
            }
            if (moduleName === 'axios') {
                var createAxios = function(baseConfig) {
                    var fn = function(configOrUrl, maybeConfig) {
                        var config = Object.assign({}, baseConfig || {});
                        if (typeof configOrUrl === 'string') {
                            Object.assign(config, maybeConfig || {});
                            config.url = configOrUrl;
                        } else if (configOrUrl) {
                            Object.assign(config, configOrUrl);
                        }
                        if (baseConfig && baseConfig.headers) {
                            config.headers = Object.assign({}, baseConfig.headers, config.headers || {});
                        }
                        var url = config.url || '';
                        if (config.baseURL && !/^https?:\/\//i.test(url)) {
                            url = config.baseURL.replace(/\/+${'$'}/, '') + '/' + url.replace(/^\/+/, '');
                        }
                        var method = (config.method || 'GET').toUpperCase();
                        var options = {
                            method: method,
                            headers: config.headers || {}
                        };
                        if (config.data !== undefined && method !== 'GET' && method !== 'HEAD') {
                            options.body = typeof config.data === 'string' ? config.data : JSON.stringify(config.data);
                        }
                        return fetch(url, options).then(function(res) {
                            return res.text().then(function(textData) {
                                var parsedData = textData;
                                try { parsedData = JSON.parse(textData); } catch(e) {}
                                var resHeaders = {};
                                if (res.headers && res.headers.forEach) {
                                    res.headers.forEach(function(v, k) { resHeaders[k.toLowerCase()] = v; });
                                }
                                var axiosResponse = {
                                    data: parsedData,
                                    status: res.status,
                                    statusText: res.statusText || '',
                                    headers: resHeaders,
                                    config: config
                                };
                                if (!res.ok && (config.validateStatus ? !config.validateStatus(res.status) : res.status >= 400)) {
                                    var err = new Error('Request failed with status code ' + res.status);
                                    err.response = axiosResponse;
                                    err.isAxiosError = true;
                                    throw err;
                                }
                                return axiosResponse;
                            });
                        });
                    };
                    fn.get = function(url, config) { var cfg = Object.assign({}, config || {}); cfg.method = 'GET'; cfg.url = url; return fn(cfg); };
                    fn.post = function(url, data, config) { var cfg = Object.assign({}, config || {}); cfg.method = 'POST'; cfg.url = url; cfg.data = data; return fn(cfg); };
                    fn.put = function(url, data, config) { var cfg = Object.assign({}, config || {}); cfg.method = 'PUT'; cfg.url = url; cfg.data = data; return fn(cfg); };
                    fn.delete = function(url, config) { var cfg = Object.assign({}, config || {}); cfg.method = 'DELETE'; cfg.url = url; return fn(cfg); };
                    fn.head = function(url, config) { var cfg = Object.assign({}, config || {}); cfg.method = 'HEAD'; cfg.url = url; return fn(cfg); };
                    fn.patch = function(url, data, config) { var cfg = Object.assign({}, config || {}); cfg.method = 'PATCH'; cfg.url = url; cfg.data = data; return fn(cfg); };
                    fn.create = function(cfg) { return createAxios(Object.assign({}, baseConfig || {}, cfg || {})); };
                    fn.defaults = Object.assign({ headers: { common: {} } }, baseConfig || {});
                    fn.default = fn;
                    fn.isAxiosError = function(err) { return !!(err && (err.isAxiosError || err.response)); };
                    fn.all = Promise.all.bind(Promise);
                    fn.spread = function(callback) { return function(arr) { return callback.apply(null, arr); }; };
                    return fn;
                };
                return createAxios({});
            }
            throw new Error("Module '" + moduleName + "' is not available");
        };
    """.trimIndent()

    private fun arrayPolyfill() = """
        if (!Array.prototype.flat) {
            Array.prototype.flat = function(depth) {
                depth = depth === undefined ? 1 : Math.floor(depth);
                if (depth < 1) return Array.prototype.slice.call(this);
                return (function flatten(arr, d) {
                    return d > 0
                        ? arr.reduce(function(acc, val) { return acc.concat(Array.isArray(val) ? flatten(val, d - 1) : val); }, [])
                        : arr.slice();
                })(this, depth);
            };
        }

        if (!Array.prototype.flatMap) {
            Array.prototype.flatMap = function(callback, thisArg) { return this.map(callback, thisArg).flat(); };
        }
    """.trimIndent()

    private fun objectPolyfill() = """
        if (!Object.entries) {
            Object.entries = function(obj) {
                var result = [];
                for (var key in obj) {
                    if (obj.hasOwnProperty(key)) result.push([key, obj[key]]);
                }
                return result;
            };
        }

        if (!Object.fromEntries) {
            Object.fromEntries = function(entries) {
                var result = {};
                for (var i = 0; i < entries.length; i++) {
                    result[entries[i][0]] = entries[i][1];
                }
                return result;
            };
        }
    """.trimIndent()

    private fun stringPolyfill() = """
        if (!String.prototype.replaceAll) {
            String.prototype.replaceAll = function(search, replace) {
                if (search instanceof RegExp) {
                    if (!search.global) throw new TypeError('replaceAll must be called with a global RegExp');
                    return this.replace(search, replace);
                }
                return this.split(search).join(replace);
            };
        }
    """.trimIndent()

    private fun browserPolyfill() = """
        if (typeof globalThis.Browser === 'undefined') {
            globalThis.Browser = {
                resolve: async function(urlOrOptions, maybeOptions) {
                    try {
                        var options = (typeof urlOrOptions === 'object' && urlOrOptions !== null) ? urlOrOptions : (maybeOptions || {});
                        var targetUrl = (typeof urlOrOptions === 'string') ? urlOrOptions : options.url;
                        var payload = JSON.stringify({
                            url: targetUrl || '',
                            headers: options.headers || {},
                            userAgent: options.userAgent || null,
                            timeoutMs: options.timeoutMs || 20000,
                            waitForSelector: options.waitForSelector || options.waitSelector || null,
                            waitForText: options.waitForText || null,
                            evaluateJsAfterLoad: options.evaluateJsAfterLoad || options.evaluateJs || null,
                            urlSniffRegex: options.urlSniffRegex || null,
                            stopOnFirstMediaMatch: typeof options.stopOnFirstMediaMatch === 'boolean' ? options.stopOnFirstMediaMatch : false,
                            extraDelayAfterLoadMs: options.extraDelayAfterLoadMs || 0,
                            autoClickPlaySelectors: typeof options.autoClickPlaySelectors === 'boolean' ? options.autoClickPlaySelectors : true,
                            autoClickServers: typeof options.autoClickServers === 'boolean' ? options.autoClickServers : false
                        });
                        var raw = await __host_browser_resolve(payload);
                        if (typeof raw === 'string') {
                            try {
                                return JSON.parse(raw);
                            } catch (e) {
                                return { html: raw, finalUrl: targetUrl, cookies: {}, cookieString: '', sniffedUrls: [], sniffedMedia: [], sniffedSubtitles: [] };
                            }
                        }
                        return raw;
                    } catch (err) {
                        console.error('[Browser.resolve] error:', err);
                        return { error: err ? (err.message || String(err)) : 'unknown error', html: '', sniffedUrls: [], sniffedMedia: [], sniffedSubtitles: [] };
                    }
                },

                sniffMedia: async function(url, options) {
                    options = options || {};
                    var sniffRegex = (options && options.urlSniffRegex) || "\\.(?:m3u8|mpd|mp4|mkv)(?:\\?|${'$'}|#)|(?:master|playlist|index)\\.(?:m3u8|txt)|/hls/";
                    var stopEarly = options && typeof options.stopOnFirstMediaMatch === 'boolean' ? options.stopOnFirstMediaMatch : true;
                    var extraDelay = (options && options.extraDelayAfterLoadMs) || 4000;
                    var timeoutMs = (options && options.timeoutMs) || 15000;

                    var res = await globalThis.Browser.resolve({
                        url: url,
                        headers: options.headers || {},
                        userAgent: options.userAgent || null,
                        urlSniffRegex: sniffRegex,
                        stopOnFirstMediaMatch: stopEarly,
                        extraDelayAfterLoadMs: extraDelay,
                        timeoutMs: timeoutMs,
                        autoClickPlaySelectors: options && typeof options.autoClickPlaySelectors === 'boolean' ? options.autoClickPlaySelectors : true,
                        autoClickServers: options && typeof options.autoClickServers === 'boolean' ? options.autoClickServers : false,
                        waitForSelector: options.waitForSelector || null,
                        waitForText: options.waitForText || null
                    });

                    var sniffedMedia = (res && res.sniffedMedia) || [];
                    var subs = [];
                    var videos = [];

                    for (var i = 0; i < sniffedMedia.length; i++) {
                        var item = sniffedMedia[i];
                        if (item.isSubtitles) {
                            subs.push({
                                url: item.url,
                                language: 'Subtitles ' + (subs.length + 1),
                                headers: item.headers || {}
                            });
                        } else {
                            videos.push(item);
                        }
                    }

                    if (res && res.sniffedSubtitles && Array.isArray(res.sniffedSubtitles)) {
                        for (var s = 0; s < res.sniffedSubtitles.length; s++) {
                            var subItem = res.sniffedSubtitles[s];
                            if (subItem && subItem.url && !subs.some(function(sub) { return sub.url === subItem.url; })) {
                                subs.push({
                                    url: subItem.url,
                                    language: subItem.lang || subItem.name || ('Subtitles ' + (subs.length + 1)),
                                    headers: (options && options.headers) || {}
                                });
                            }
                        }
                    }

                    if (videos.length === 0 && res && res.sniffedUrls && res.sniffedUrls.length > 0) {
                        for (var j = 0; j < res.sniffedUrls.length; j++) {
                            var sniffUrl = res.sniffedUrls[j];
                            var cleanSniff = sniffUrl.split(/[?#]/)[0].toLowerCase();
                            if (cleanSniff.endsWith('.vtt') || cleanSniff.endsWith('.srt') || cleanSniff.endsWith('.ass')) {
                                subs.push({ url: sniffUrl, language: 'Subtitles ' + (subs.length + 1), headers: res.headers || {} });
                            } else {
                                videos.push({ url: sniffUrl, headers: (res.headers || {}) });
                            }
                        }
                    }

                    var defaultName = (options && (options.name || options.title)) || 'Sniffed Stream';
                    var defaultQuality = (options && options.quality) || 'auto';

                    return videos.map(function(v, idx) {
                        var q = defaultQuality;
                        var vLower = v.url.toLowerCase();
                        if (vLower.indexOf('1080') >= 0) q = '1080p';
                        else if (vLower.indexOf('720') >= 0) q = '720p';
                        else if (vLower.indexOf('2160') >= 0 || vLower.indexOf('4k') >= 0) q = '4K';

                        return {
                            name: videos.length > 1 ? (defaultName + ' #' + (idx + 1)) : defaultName,
                            title: (options && options.title) || defaultName,
                            url: v.url,
                            quality: q,
                            headers: v.headers || {},
                            subtitles: subs
                        };
                    });
                }
            };
        }

        if (typeof globalThis.Cloudflare === 'undefined') {
            globalThis.Cloudflare = {
                bypass: function(urlOrOptions, options) {
                    var targetUrl = typeof urlOrOptions === 'string' ? urlOrOptions : (urlOrOptions && urlOrOptions.url ? urlOrOptions.url : '');
                    var opts = (typeof urlOrOptions === 'object' && urlOrOptions !== null) ? urlOrOptions : (options || {});
                    return globalThis.Browser.resolve(targetUrl, {
                        headers: opts.headers || {},
                        timeoutMs: opts.timeout || opts.timeoutMs || 20000,
                        waitForSelector: opts.waitForSelector || opts.waitSelector || null,
                        waitForText: opts.waitForText || null,
                        evaluateJsAfterLoad: opts.evaluateJsAfterLoad || opts.evaluateJs || null
                    }).then(function(res) {
                        if (!res) return { ok: false, status: 503, text: function() { return Promise.resolve(''); }, json: function() { return Promise.resolve({}); }, body: '', headers: {}, cookies: '' };
                        var isOk = typeof res.ok !== 'undefined' ? !!res.ok : (!res.error && (res.html ? res.html.length > 0 : true));
                        var statusCode = res.status || (isOk ? 200 : 503);
                        var bodyText = res.html || '';
                        var headers = res.headers || {};
                        var cookieStr = (headers && (headers['Cookie'] || headers['cookie'])) || res.cookieString || '';
                        return {
                            ok: isOk,
                            status: statusCode,
                            url: res.finalUrl || targetUrl,
                            headers: headers,
                            cookies: cookieStr,
                            body: bodyText,
                            text: function() { return Promise.resolve(bodyText); },
                            json: function() {
                                try { return Promise.resolve(JSON.parse(bodyText || '{}')); }
                                catch (e) { return Promise.reject(e); }
                            }
                        };
                    });
                }
            };
        }
    """.trimIndent()

    private fun extractorSdkPolyfill() = """
        if (typeof globalThis.Unpacker === 'undefined') {
            globalThis.Unpacker = {
                isPacked: function(htmlOrScript) {
                    if (!htmlOrScript || typeof htmlOrScript !== 'string') return false;
                    return /eval\(function\(p,a,c,k,e,[rd]/i.test(htmlOrScript);
                },
                unpack: function(htmlOrScript) {
                    if (!htmlOrScript || typeof htmlOrScript !== 'string') return '';
                    var packedRegex = /eval\(function\(p,a,c,k,e,[rd]\)\s*\{.*?\}\s*\(\s*['"]((?:[^'\\]|\\.)*)['"]\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*['"]((?:[^'\\]|\\.)*)['"]\s*\.split\(\s*['"]\|['"]\s*\)/g;
                    var unpackedTotal = '';
                    var match;
                    var hasMatch = false;

                    function parseRadix(word, radix) {
                        if (radix >= 2 && radix <= 36) {
                            var n = parseInt(word, radix);
                            return isNaN(n) ? null : n;
                        }
                        if (radix > 36 && radix <= 62) {
                            var res = 0;
                            for (var ci = 0; ci < word.length; ci++) {
                                var ch = word.charCodeAt(ci);
                                var digit = null;
                                if (ch >= 48 && ch <= 57) digit = ch - 48;
                                else if (ch >= 97 && ch <= 122) digit = ch - 97 + 10;
                                else if (ch >= 65 && ch <= 90) digit = ch - 65 + 36;
                                if (digit === null || digit >= radix) return null;
                                res = res * radix + digit;
                            }
                            return res;
                        }
                        return null;
                    }

                    while ((match = packedRegex.exec(htmlOrScript)) !== null) {
                        hasMatch = true;
                        try {
                            var payload = match[1].replace(/\\'/g, "'").replace(/\\\\/g, "\\");
                            var radix = parseInt(match[2], 10);
                            var count = parseInt(match[3], 10);
                            var symtab = match[4].split('|');

                            var unpacked = payload.replace(/\b\w+\b/g, function(token) {
                                var idx = parseRadix(token, radix);
                                if (idx !== null && idx >= 0 && idx < symtab.length && symtab[idx] !== '') {
                                    return symtab[idx];
                                }
                                return token;
                            });
                            unpackedTotal += (unpackedTotal ? '\n' : '') + unpacked;
                        } catch (err) {
                            console.error('[Unpacker.unpack] block error:', err);
                        }
                    }

                    return hasMatch ? unpackedTotal : (Unpacker.isPacked(htmlOrScript) ? htmlOrScript : '');
                },
                extractMediaUrls: function(text, baseUrl) {
                    if (!text || typeof text !== 'string') return { sources: [], subtitles: [] };
                    var sources = [];
                    var subtitles = [];
                    var seenUrls = {};

                    function addSource(u, label) {
                        if (!u || typeof u !== 'string') return;
                        var fullUrl = u.trim();
                        if (fullUrl.startsWith('//')) fullUrl = 'https:' + fullUrl;
                        else if (fullUrl.startsWith('/') && baseUrl) {
                            var m = baseUrl.match(/^(https?:\/\/[^\/]+)/);
                            if (m) fullUrl = m[1] + fullUrl;
                        }
                        if (!seenUrls[fullUrl] && /^https?:\/\//i.test(fullUrl)) {
                            seenUrls[fullUrl] = true;
                            sources.push({ url: fullUrl, label: label || 'Auto' });
                        }
                    }

                    function addSub(u, lang) {
                        if (!u || typeof u !== 'string') return;
                        var fullUrl = u.trim();
                        if (fullUrl.startsWith('//')) fullUrl = 'https:' + fullUrl;
                        else if (fullUrl.startsWith('/') && baseUrl) {
                            var m = baseUrl.match(/^(https?:\/\/[^\/]+)/);
                            if (m) fullUrl = m[1] + fullUrl;
                        }
                        if (!seenUrls[fullUrl] && /^https?:\/\//i.test(fullUrl)) {
                            seenUrls[fullUrl] = true;
                            subtitles.push({ url: fullUrl, language: lang || 'Subtitles' });
                        }
                    }

                    // 1. JWPlayer / VideoJS sources: [{ file: "..." }]
                    var jwRegex = /sources\s*:\s*\[([^\]]+)\]/gi;
                    var jwMatch;
                    while ((jwMatch = jwRegex.exec(text)) !== null) {
                        var inner = jwMatch[1];
                        var singleSrcRegex = /\{([^}]+)\}/g;
                        var sm;
                        while ((sm = singleSrcRegex.exec(inner)) !== null) {
                            var sObj = sm[1];
                            var sfMatch = sObj.match(/(?:file|src)\s*:\s*["']([^"']+)["']/i);
                            var slMatch = sObj.match(/label\s*:\s*["']([^"']+)["']/i);
                            if (sfMatch) {
                                addSource(sfMatch[1], slMatch ? slMatch[1] : 'Auto');
                            }
                        }
                    }

                    // 2. tracks: [{ file: "...", label: "..." }]
                    var trkRegex = /tracks\s*:\s*\[([^\]]+)\]/gi;
                    var trkMatch;
                    while ((trkMatch = trkRegex.exec(text)) !== null) {
                        var tInner = trkMatch[1];
                        var singleTrkRegex = /\{([^}]+)\}/g;
                        var tm;
                        while ((tm = singleTrkRegex.exec(tInner)) !== null) {
                            var objStr = tm[1];
                            var fMatch = objStr.match(/(?:file|src)\s*:\s*["']([^"']+)["']/i);
                            var lMatch = objStr.match(/label\s*:\s*["']([^"']+)["']/i);
                            if (fMatch) {
                                addSub(fMatch[1], lMatch ? lMatch[1] : 'Subtitles');
                            }
                        }
                    }

                    // 3. hls.loadSource("...")
                    var hlsRegex = /loadSource\s*\(\s*["']([^"']+)["']\s*\)/gi;
                    var hMatch;
                    while ((hMatch = hlsRegex.exec(text)) !== null) {
                        addSource(hMatch[1], 'HLS');
                    }

                    // 4. file: "https://...m3u8"
                    var fileRegex = /(?:file|src|url)\s*:\s*["'](https?:\/\/[^"']+\.(?:m3u8|mpd|mp4|mkv|webm)[^"']*)["']/gi;
                    var fileMatch;
                    while ((fileMatch = fileRegex.exec(text)) !== null) {
                        addSource(fileMatch[1], 'Direct');
                    }

                    // 5. Raw media URLs
                    var rawMediaRegex = /(https?:\/\/[^\s"'<>]+\.(?:m3u8|mpd|mp4|mkv|webm)(?:\?[^\s"'<>]*)?)/gi;
                    var rmMatch;
                    while ((rmMatch = rawMediaRegex.exec(text)) !== null) {
                        addSource(rmMatch[1], 'Direct');
                    }

                    // 6. Raw subtitles
                    var rawSubRegex = /(https?:\/\/[^\s"'<>]+\.(?:vtt|srt|ass)(?:\?[^\s"'<>]*)?)/gi;
                    var rsMatch;
                    while ((rsMatch = rawSubRegex.exec(text)) !== null) {
                        addSub(rsMatch[1], 'Subtitles');
                    }

                    return { sources: sources, subtitles: subtitles };
                }
            };
        }

        if (typeof globalThis.EmbedExtractor === 'undefined') {
            var EmbedExtractor = {
                canHandle: function(url) {
                    if (!url || typeof url !== 'string') return false;
                    var u = url.trim().toLowerCase();
                    var clean = u.split(/[?#]/)[0];
                    if (clean.endsWith('.m3u8') || clean.endsWith('.mpd') || clean.endsWith('.mp4') || clean.endsWith('.mkv') || clean.endsWith('.webm') || u.startsWith('magnet:') || u.startsWith('torrent:')) return true;
                    return u.indexOf('pixeldrain.com/u/') >= 0 || u.indexOf('pixeldrain.com/l/') >= 0 ||
                           /hubcloud\.|vcloud\.|fastcloud\.|gamerxyt\.|cryptoinsights\./i.test(u) ||
                           /gdflix\.|driveeed\.|filepress\./i.test(u) ||
                           u.indexOf('gofile.io/d/') >= 0 ||
                           /filemoon\.|streamwish\.|sfastwish\.|strwish\.|awish\.|dwish\.|hlswish\.|luluvdo\.|vidhide\.|filelions\.|vidhidepro\.|mycloud\.|lulustream\./i.test(u) ||
                           /voe\.sx|voe-network\.|repack\./i.test(u) ||
                           /dood(?:stream)?\.|d000d\.|dooood\.|ds2play\.|dood\.(?:watch|pm|to|so|cx|la|ws|sh|re|yt|stream)/i.test(u) ||
                           /streamtape\.|strtape\./i.test(u) ||
                           /mp4upload\./i.test(u) ||
                           /vidoza\./i.test(u) ||
                           /streamsb\.|sbembed\.|sbplay\.|sbfull\.|watchsb\.|lvturbo\./i.test(u) ||
                           /uqload\.|upstream\./i.test(u) ||
                           /streamlare\.|streamhub\.|supervideo\.|dropload\.|vtbe\.|vtube\./i.test(u) ||
                           /vidguard\.|listeamed\.|bembed\.|vgfplay\./i.test(u) ||
                           /mediafire\.com/i.test(u) ||
                           /krakenfiles\.com/i.test(u) ||
                           /kwik\./i.test(u) ||
                           /gogoplay\d*\.|gogohd\d*\.|goload\d*\.|vidstreaming\.|anitaku\.|playgo\d*\./i.test(u) ||
                           /mixdrop\.|mdfx9dc8n\./i.test(u) ||
                           /vidmoly\./i.test(u) ||
                           /vidsrc\.|vidlink\.|megacloud\.|upcloud\./i.test(u);
                },

                resolve: async function(url, options) {
                    options = options || {};
                    var cleanUrl = (url || '').trim();
                    if (!cleanUrl) return [];

                    var origin = '';
                    var originMatch = cleanUrl.match(/^(https?:\/\/[^\/]+)/i);
                    if (originMatch) {
                        origin = originMatch[1];
                    } else {
                        try {
                            var uObj = new URL(cleanUrl);
                            if (uObj.protocol && uObj.host) origin = uObj.protocol + '//' + uObj.host;
                        } catch(e) {}
                    }

                    var cleanPath = cleanUrl.split(/[?#]/)[0].toLowerCase();

                    function detectQuality(urlStr, fallback) {
                        if (fallback && fallback !== 'Auto' && fallback !== 'Direct' && fallback !== 'Stream') return fallback;
                        if (!urlStr || typeof urlStr !== 'string') return fallback || 'Auto';
                        if (/master\.m3u8/i.test(urlStr)) return 'Auto';
                        if (/(?:^|[^a-z0-9])(?:2160p?|4k)(?:[^a-z0-9]|$)/i.test(urlStr)) return '4K';
                        if (/(?:^|[^0-9])1080(?:[^0-9]|$)/.test(urlStr)) return '1080p';
                        if (/(?:^|[^0-9])720(?:[^0-9]|$)/.test(urlStr)) return '720p';
                        if (/(?:^|[^0-9])480(?:[^0-9]|$)/.test(urlStr)) return '480p';
                        if (/(?:^|[^0-9])360(?:[^0-9]|$)/.test(urlStr)) return '360p';
                        return fallback || 'Auto';
                    }

                    function sanitizeStreams(streams) {
                        if (!Array.isArray(streams)) return [];
                        var seen = Object.create(null);
                        var out = [];
                        var junkExts = ['.js', '.css', '.png', '.jpg', '.jpeg', '.svg', '.gif', '.woff', '.woff2', '.ico', '.bmp', '.webp'];
                        for (var i = 0; i < streams.length; i++) {
                            var s = streams[i];
                            if (!s || typeof s !== 'object') continue;
                            var sUrl = (s.url || '').trim();
                            if (!sUrl) continue;
                            var isHttp = /^https?:\/\//i.test(sUrl);
                            var isTorrentOrMagnet = sUrl.startsWith('magnet:') || sUrl.startsWith('torrent:');
                            if (!isHttp && !isTorrentOrMagnet) continue;

                            var cleanSUrl = sUrl.split(/[?#]/)[0].toLowerCase();
                            var isJunk = false;
                            for (var k = 0; k < junkExts.length; k++) {
                                if (cleanSUrl.endsWith(junkExts[k])) {
                                    isJunk = true;
                                    break;
                                }
                            }
                            if (isJunk) continue;
                            if (seen[sUrl]) continue;
                            seen[sUrl] = true;

                            if (!s.headers || typeof s.headers !== 'object') {
                                s.headers = {};
                            }
                            var subRefererDefault = s.headers['Referer'] || (origin ? (origin + '/') : '');

                            var cleanSubs = [];
                            if (Array.isArray(s.subtitles)) {
                                var seenSub = Object.create(null);
                                for (var j = 0; j < s.subtitles.length; j++) {
                                    var sub = s.subtitles[j];
                                    if (!sub || typeof sub !== 'object') continue;
                                    var subUrl = (sub.url || '').trim();
                                    if (!subUrl || !/^https?:\/\//i.test(subUrl)) continue;
                                    if (seenSub[subUrl]) continue;
                                    seenSub[subUrl] = true;
                                    if (!sub.headers || typeof sub.headers !== 'object') {
                                        sub.headers = {};
                                    }
                                    if (!sub.headers['Referer'] && subRefererDefault) {
                                        sub.headers['Referer'] = subRefererDefault;
                                    }
                                    cleanSubs.push({
                                        url: subUrl,
                                        language: sub.language || 'Subtitles',
                                        headers: sub.headers
                                    });
                                }
                            }
                            s.subtitles = cleanSubs;
                            out.push(s);
                        }
                        return out;
                    }

                    try {
                        var rawResult = await (async function() {
                            // 1. Direct Pass-Through
                    if (cleanPath.endsWith('.m3u8') || cleanPath.endsWith('.mpd') || cleanPath.endsWith('.mp4') ||
                        cleanPath.endsWith('.mkv') || cleanPath.endsWith('.webm') || cleanPath.indexOf('/hls/') >= 0 ||
                        cleanPath.indexOf('master.m3u8') >= 0 || cleanUrl.startsWith('magnet:') || cleanUrl.startsWith('torrent:')) {
                        return [{
                            name: options.name || 'Direct Stream',
                            title: options.title || options.name || 'Direct',
                            url: cleanUrl,
                            quality: options.quality || 'Auto',
                            headers: Object.assign({}, options.headers || {}),
                            subtitles: []
                        }];
                    }

                    // 2. PixelDrain (pixeldrain.com/u/{id} or /l/{id})
                    if (/pixeldrain\.com\/[ul]\/([a-zA-Z0-9_-]+)/i.test(cleanUrl)) {
                        var idMatch = cleanUrl.match(/pixeldrain\.com\/[ul]\/([a-zA-Z0-9_-]+)/i);
                        var fileId = idMatch[1];
                        var directUrl = 'https://pixeldrain.com/api/file/' + fileId;
                        return [{
                            name: options.name || 'PixelDrain',
                            title: options.title || options.name || 'PixelDrain',
                            url: directUrl,
                            quality: options.quality || '1080p',
                            headers: Object.assign({ 'Referer': 'https://pixeldrain.com/' }, options.headers || {}),
                            subtitles: []
                        }];
                    }

                    // 3. GoFile (gofile.io/d/{id})
                    if (/gofile\.io\/d\/([a-zA-Z0-9_-]+)/i.test(cleanUrl)) {
                        var gfMatch = cleanUrl.match(/gofile\.io\/d\/([a-zA-Z0-9_-]+)/i);
                        var folderId = gfMatch[1];
                        var token = globalThis.__GOFILE_TOKEN__;
                        if (!token) {
                            try {
                                var acctResp = await fetch('https://api.gofile.io/accounts', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: '{}'
                                });
                                var acctJson = await acctResp.json();
                                if (acctJson && acctJson.data && acctJson.data.token) {
                                    token = acctJson.data.token;
                                    globalThis.__GOFILE_TOKEN__ = token;
                                }
                            } catch(gfe) {
                                console.error('[GoFile] token error:', gfe);
                            }
                        }
                        var gfHeaders = {
                            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
                        };
                        if (token) {
                            gfHeaders['Authorization'] = 'Bearer ' + token;
                            gfHeaders['Cookie'] = 'accountToken=' + token;
                        }
                        try {
                            var metaResp = await fetch('https://api.gofile.io/contents/' + folderId + '?wt=4fd6sg89d7s6', {
                                headers: gfHeaders
                            });
                            var metaJson = await metaResp.json();
                            if (metaJson && metaJson.data && metaJson.data.children) {
                                var outStreams = [];
                                var children = metaJson.data.children;
                                var keys = Object.keys(children);
                                for (var ki = 0; ki < keys.length; ki++) {
                                    var cItem = children[keys[ki]];
                                    if (cItem && cItem.link) {
                                        outStreams.push({
                                            name: options.name || cItem.name || 'GoFile',
                                            title: cItem.name || options.title || 'GoFile',
                                            url: cItem.link,
                                            quality: options.quality || 'Direct',
                                            headers: Object.assign({}, gfHeaders, options.headers || {}),
                                            subtitles: []
                                        });
                                    }
                                }
                                if (outStreams.length > 0) return outStreams;
                            }
                        } catch(metaErr) {
                            console.error('[GoFile] content error:', metaErr);
                        }
                    }

                    // 4. HubCloud & Clones (hubcloud.*, vcloud.*, fastcloud.*, gamerxyt.*, cryptoinsights.*)
                    if (/hubcloud\.|vcloud\.|fastcloud\.|gamerxyt\.|cryptoinsights\./i.test(cleanUrl)) {
                        try {
                            var reqHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': options.referer || cleanUrl
                            }, options.headers || {});
                            var hResp = await fetch(cleanUrl, { headers: reqHdrs });
                            var hHtml = await hResp.text();

                            var redirMatch = hHtml.match(/var\s+url\s*=\s*['"]([^'"]+)['"]/i) ||
                                             hHtml.match(/window\.location(?:\.href)?\s*=\s*['"]([^'"]+)['"]/i) ||
                                             hHtml.match(/<a[^>]+href=["']([^"']*(?:hubcloud\.php|gamerxyt|r2\.dev)[^"']*)["']/i);
                            if (redirMatch && !redirMatch[1].startsWith('#')) {
                                var redirUrl = redirMatch[1].trim();
                                if (redirUrl.startsWith('/')) redirUrl = origin + redirUrl;
                                if (/^https?:\/\//i.test(redirUrl) && redirUrl !== cleanUrl) {
                                    return await EmbedExtractor.resolve(redirUrl, options);
                                }
                            }

                            var chDoc = cheerio.load(hHtml);
                            var hubStreams = [];
                            chDoc('a[href]').each(function(i, el) {
                                var href = chDoc(el).attr('href') || '';
                                var text = chDoc(el).text() || '';
                                if (href.startsWith('/')) href = origin + href;
                                if (!/^https?:\/\//i.test(href)) return;

                                var isServerBtn = /fsl|10gbps|pixeldrain|r2|cloudflare|direct|fastcdn|worker/i.test(text) ||
                                                  /pixeldrain\.com\/u\/|r2\.dev|workers\.dev|\.(?:mkv|mp4)/i.test(href);
                                if (isServerBtn) {
                                    var label = text.trim() || 'Direct Server';
                                    var qual = '1080p';
                                    if (/2160|4k/i.test(label) || /2160|4k/i.test(href)) qual = '4K';
                                    else if (/720/i.test(label) || /720/i.test(href)) qual = '720p';
                                    else if (/480/i.test(label) || /480/i.test(href)) qual = '480p';

                                    hubStreams.push({
                                        name: options.name ? (options.name + ' [' + label + ']') : label,
                                        title: label,
                                        url: href,
                                        quality: qual,
                                        headers: { 'Referer': cleanUrl }
                                    });
                                }
                            });

                            if (hubStreams.length > 0) {
                                var resolvedHub = [];
                                for (var hi = 0; hi < hubStreams.length; hi++) {
                                    var hStream = hubStreams[hi];
                                    if (EmbedExtractor.canHandle(hStream.url) && !/\.(?:mkv|mp4)/i.test(hStream.url)) {
                                        var subRes = await EmbedExtractor.resolve(hStream.url, { name: hStream.name, quality: hStream.quality });
                                        resolvedHub = resolvedHub.concat(subRes);
                                    } else {
                                        resolvedHub.push(hStream);
                                    }
                                }
                                if (resolvedHub.length > 0) return resolvedHub;
                            }
                        } catch(hErr) {
                            console.error('[HubCloud] error:', hErr);
                        }
                    }

                    // 5. GDFlix / Driveeed / FilePress (gdflix.*, driveeed.*, filepress.*)
                    if (/gdflix\.|driveeed\.|filepress\./i.test(cleanUrl)) {
                        try {
                            var gdfHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': options.referer || cleanUrl
                            }, options.headers || {});
                            var gdfResp = await fetch(cleanUrl, { headers: gdfHdrs });
                            var gdfHtml = await gdfResp.text();
                            var gdfDoc = cheerio.load(gdfHtml);
                            var gdfStreams = [];

                            gdfDoc('a[href]').each(function(i, el) {
                                var href = gdfDoc(el).attr('href') || '';
                                var text = gdfDoc(el).text() || '';
                                if (href.startsWith('/')) href = origin + href;
                                if (!/^https?:\/\//i.test(href)) return;

                                if (/fastcdn|r2\.dev|workers\.dev|pixeldrain|\.(?:mkv|mp4)/i.test(href) || /instant|fast cloud|download|worker/i.test(text)) {
                                    var label = text.trim() || 'Cloud Stream';
                                    gdfStreams.push({
                                        name: options.name ? (options.name + ' [' + label + ']') : label,
                                        title: label,
                                        url: href,
                                        quality: options.quality || '1080p',
                                        headers: { 'Referer': cleanUrl }
                                    });
                                }
                            });

                            if (gdfStreams.length > 0) {
                                var resolvedGdf = [];
                                for (var gi = 0; gi < gdfStreams.length; gi++) {
                                    var gItem = gdfStreams[gi];
                                    if (EmbedExtractor.canHandle(gItem.url) && !/\.(?:mkv|mp4)/i.test(gItem.url)) {
                                        var subG = await EmbedExtractor.resolve(gItem.url, { name: gItem.name, quality: gItem.quality });
                                        resolvedGdf = resolvedGdf.concat(subG);
                                    } else {
                                        resolvedGdf.push(gItem);
                                    }
                                }
                                if (resolvedGdf.length > 0) return resolvedGdf;
                            }
                        } catch(gdfErr) {
                            console.error('[GDFlix] error:', gdfErr);
                        }
                    }

                    // 6. Filemoon / StreamWish / Luluvdo / VidHide / Wishembed
                    if (/filemoon\.|streamwish\.|sfastwish\.|strwish\.|awish\.|dwish\.|hlswish\.|luluvdo\.|vidhide\.|filelions\.|vidhidepro\.|mycloud\.|lulustream\./i.test(cleanUrl)) {
                        try {
                            var fmHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': options.referer || cleanUrl
                            }, options.headers || {});
                            var fmResp = await fetch(cleanUrl, { headers: fmHdrs });
                            var fmHtml = await fmResp.text();

                            var iframeMatch = fmHtml.match(/<iframe[^>]+src=["']([^"']*(?:filemoon|streamwish|vidhide|filelions|lulustream|mycloud)[^"']*)["']/i);
                            if (iframeMatch) {
                                var ifrUrl = iframeMatch[1];
                                if (ifrUrl.startsWith('//')) ifrUrl = 'https:' + ifrUrl;
                                else if (ifrUrl.startsWith('/')) ifrUrl = origin + ifrUrl;
                                var ifrResp = await fetch(ifrUrl, { headers: Object.assign({}, fmHdrs, { 'Referer': cleanUrl }) });
                                fmHtml = await ifrResp.text();
                            }

                            var unpacked = Unpacker.unpack(fmHtml);
                            var combined = fmHtml + '\n' + unpacked;
                            var extracted = Unpacker.extractMediaUrls(combined, cleanUrl);

                            if (extracted.sources && extracted.sources.length > 0) {
                                return extracted.sources.map(function(s) {
                                    return {
                                        name: options.name || 'Stream',
                                        title: options.title || options.name || 'Stream',
                                        url: s.url,
                                        quality: options.quality || (/1080/i.test(s.url) ? '1080p' : (/720/i.test(s.url) ? '720p' : 'Auto')),
                                        headers: {
                                            'Referer': origin + '/',
                                            'Origin': origin,
                                            'User-Agent': fmHdrs['User-Agent']
                                        },
                                        subtitles: (extracted.subtitles || []).map(function(sub) {
                                            return { url: sub.url, language: sub.language || 'Subtitles', headers: { 'Referer': origin + '/' } };
                                        })
                                    };
                                });
                            }
                        } catch(fmErr) {
                            console.error('[Filemoon/Wish] HTTP extraction error:', fmErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 7. Voe (voe.sx and redirect domains)
                    if (/voe\.sx|voe-network\.|repack\./i.test(cleanUrl)) {
                        try {
                            var voeHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': options.referer || cleanUrl
                            }, options.headers || {});
                            var voeResp = await fetch(cleanUrl, { headers: voeHdrs });
                            var voeHtml = await voeResp.text();

                            var vRedir = voeHtml.match(/window\.location\.href\s*=\s*['"]([^'"]+)['"]/i);
                            if (vRedir) {
                                var target = vRedir[1];
                                if (target.startsWith('/')) target = origin + target;
                                voeResp = await fetch(target, { headers: voeHdrs });
                                voeHtml = await voeResp.text();
                            }

                            var hlsMatch = voeHtml.match(/['"]hls['"]\s*:\s*['"]([^'"]+)['"]/i) ||
                                           voeHtml.match(/var\s+sources\s*=\s*\{['"]hls['"]\s*:\s*['"]([^'"]+)['"]/i) ||
                                           voeHtml.match(/(https?:\/\/[^"'\s<>]+\.m3u8[^"'\s<>]*)/i);

                            var streamUrl = null;
                            if (hlsMatch) {
                                var rawHls = hlsMatch[1];
                                if (/^https?:\/\//i.test(rawHls)) {
                                    streamUrl = rawHls;
                                } else {
                                    try {
                                        var decoded = atob(rawHls);
                                        if (/^https?:\/\//i.test(decoded)) streamUrl = decoded;
                                    } catch(e) {}
                                }
                            }

                            if (streamUrl) {
                                return [{
                                    name: options.name || 'Voe',
                                    title: options.title || options.name || 'Voe',
                                    url: streamUrl,
                                    quality: options.quality || 'Auto',
                                    headers: {
                                        'Referer': origin ? (origin + '/') : 'https://voe.sx/',
                                        'Origin': origin || 'https://voe.sx'
                                    },
                                    subtitles: []
                                }];
                            }
                        } catch(voeErr) {
                            console.error('[Voe] HTTP error:', voeErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 8. DoodStream / D000d / Dooood (dood.*, d000d.*, dooood.*, ds2play.*)
                    if (/dood(?:stream)?\.|d000d\.|dooood\.|ds2play\.|dood\.(?:watch|pm|to|so|cx|la|ws|sh|re|yt|stream)/i.test(cleanUrl)) {
                        try {
                            var doodUrl = cleanUrl.replace(/\/d\/|\/f\//i, '/e/');
                            var doodHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': options.referer || doodUrl
                            }, options.headers || {});
                            var dResp = await fetch(doodUrl, { headers: doodHdrs });
                            var dHtml = await dResp.text();

                            var passMatch = dHtml.match(/\/pass_md5\/[a-zA-Z0-9_\/-]+/i);
                            var tokenMatch = dHtml.match(/token=([a-zA-Z0-9_-]+)/i);
                            if (passMatch) {
                                var passUrl = origin + passMatch[0];
                                var passResp = await fetch(passUrl, { headers: Object.assign({}, doodHdrs, { 'Referer': doodUrl }) });
                                var passBody = await passResp.text();
                                if (passBody && passBody.trim().startsWith('http')) {
                                    var randToken = '';
                                    var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
                                    for (var r = 0; r < 10; r++) randToken += chars.charAt(Math.floor(Math.random() * chars.length));
                                    var finalDoodUrl = passBody.trim() + randToken + (tokenMatch ? ('?token=' + tokenMatch[1] + '&expiry=' + Date.now()) : '');
                                    return [{
                                        name: options.name || 'DoodStream',
                                        title: options.title || options.name || 'DoodStream',
                                        url: finalDoodUrl,
                                        quality: options.quality || '1080p',
                                        headers: { 'Referer': origin + '/' },
                                        subtitles: []
                                    }];
                                }
                            }
                        } catch(dErr) {
                            console.error('[DoodStream] HTTP error:', dErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 9. Streamtape (streamtape.com, streamtape.to, strtape.*)
                    if (/streamtape\.|strtape\./i.test(cleanUrl)) {
                        try {
                            var stHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': options.referer || cleanUrl
                            }, options.headers || {});
                            var stResp = await fetch(cleanUrl, { headers: stHdrs });
                            var stHtml = await stResp.text();

                            var robotMatch = stHtml.match(/document\.getElementById\(['"](?:no)?robotlink['"]\)\.innerHTML\s*=\s*['"]([^'"]+)['"]\s*\+\s*['"]([^'"]+)['"]/i) ||
                                             stHtml.match(/id=['"](?:no)?robotlink['"][^>]*>([^<]+)<\/div>/i) ||
                                             stHtml.match(/['"](\/\/streamtape\.com\/get_video\?[^'"]+)['"]/i);

                            var stStreamUrl = null;
                            if (robotMatch) {
                                if (robotMatch[2]) {
                                    stStreamUrl = 'https:' + robotMatch[1] + robotMatch[2];
                                } else if (robotMatch[1].startsWith('//')) {
                                    stStreamUrl = 'https:' + robotMatch[1];
                                } else if (robotMatch[1].startsWith('http')) {
                                    stStreamUrl = robotMatch[1];
                                }
                            }

                            if (stStreamUrl) {
                                return [{
                                    name: options.name || 'Streamtape',
                                    title: options.title || options.name || 'Streamtape',
                                    url: stStreamUrl,
                                    quality: options.quality || '1080p',
                                    headers: { 'Referer': 'https://streamtape.com/' },
                                    subtitles: []
                                }];
                            }
                        } catch(stErr) {
                            console.error('[Streamtape] HTTP error:', stErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 9.1. Mp4Upload (mp4upload.com)
                    if (/mp4upload\./i.test(cleanUrl)) {
                        try {
                            var mp4EmbedUrl = cleanUrl;
                            var idMatch = cleanUrl.match(/mp4upload\.com\/(?:embed-)?([a-zA-Z0-9]+)(?:\.html)?/i);
                            if (idMatch && idMatch[1] && cleanUrl.indexOf('embed-') < 0) {
                                mp4EmbedUrl = 'https://www.mp4upload.com/embed-' + idMatch[1] + '.html';
                            }
                            var mp4Hdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': 'https://www.mp4upload.com/'
                            }, options.headers || {});
                            var mp4Resp = await fetch(mp4EmbedUrl, { headers: mp4Hdrs });
                            var mp4Html = await mp4Resp.text();
                            var mp4Unpacked = Unpacker.unpack(mp4Html);
                            var mp4Combined = mp4Html + '\n' + mp4Unpacked;

                            var srcMatch = mp4Combined.match(/player\.src\s*\(\s*["']([^"']+)["']/i) ||
                                           mp4Combined.match(/player\.src\s*\(\s*\{\s*src\s*:\s*["']([^"']+)["']/i) ||
                                           mp4Combined.match(/src\s*:\s*["'](https?:\/\/[^"']+\.mp4[^"']*)["']/i);
                            var directMp4 = srcMatch ? srcMatch[1] : null;
                            if (!directMp4) {
                                var extractedMp4 = Unpacker.extractMediaUrls(mp4Combined, 'https://www.mp4upload.com/');
                                if (extractedMp4.sources && extractedMp4.sources.length > 0) {
                                    directMp4 = extractedMp4.sources[0].url;
                                }
                            }
                            if (directMp4) {
                                return [{
                                    name: options.name || 'Mp4Upload',
                                    title: options.title || options.name || 'Mp4Upload',
                                    url: directMp4,
                                    quality: detectQuality(directMp4, options.quality || '1080p'),
                                    headers: {
                                        'Referer': 'https://www.mp4upload.com/',
                                        'Origin': 'https://www.mp4upload.com',
                                        'User-Agent': mp4Hdrs['User-Agent']
                                    },
                                    subtitles: []
                                }];
                            }
                        } catch(mp4Err) {
                            console.error('[Mp4Upload] HTTP error:', mp4Err);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 9.2. Vidoza (vidoza.net, vidoza.org)
                    if (/vidoza\.(?:net|org)/i.test(cleanUrl)) {
                        try {
                            var vHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': origin ? (origin + '/') : cleanUrl
                            }, options.headers || {});
                            var vResp = await fetch(cleanUrl, { headers: vHdrs });
                            var vHtml = await vResp.text();
                            var vUnpacked = Unpacker.unpack(vHtml);
                            var vCombined = vHtml + '\n' + vUnpacked;

                            var vStreams = [];
                            var scMatch = vCombined.match(/sourcesCode\s*=\s*\[([^\]]+)\]/i);
                            if (scMatch) {
                                var itemRegex = /\{([^}]+)\}/g;
                                var im;
                                while ((im = itemRegex.exec(scMatch[1])) !== null) {
                                    var objStr = im[1];
                                    var sMatch = objStr.match(/src\s*:\s*["']([^"']+)["']/i);
                                    var rMatch = objStr.match(/(?:res|label)\s*:\s*["']?(\d+p?)["']?/i);
                                    if (sMatch) {
                                        var sUrl = sMatch[1];
                                        if (sUrl.startsWith('//')) sUrl = 'https:' + sUrl;
                                        vStreams.push({
                                            url: sUrl,
                                            quality: rMatch ? (rMatch[1].endsWith('p') ? rMatch[1] : (rMatch[1] + 'p')) : 'Auto'
                                        });
                                    }
                                }
                            }

                            if (vStreams.length === 0) {
                                var srcTagRegex = /<source[^>]+src=["']([^"']+)["'][^>]*type=["']video\/mp4["']/gi;
                                var stMatch;
                                while ((stMatch = srcTagRegex.exec(vCombined)) !== null) {
                                    var tagUrl = stMatch[1];
                                    if (tagUrl.startsWith('//')) tagUrl = 'https:' + tagUrl;
                                    vStreams.push({ url: tagUrl, quality: detectQuality(tagUrl, 'Auto') });
                                }
                            }

                            if (vStreams.length === 0) {
                                var vExt = Unpacker.extractMediaUrls(vCombined, origin ? (origin + '/') : cleanUrl);
                                if (vExt.sources && vExt.sources.length > 0) {
                                    for (var vi = 0; vi < vExt.sources.length; vi++) {
                                        vStreams.push({ url: vExt.sources[vi].url, quality: detectQuality(vExt.sources[vi].url, vExt.sources[vi].label || 'Auto') });
                                    }
                                }
                            }

                            var vSubs = [];
                            var trackRegex = /<track[^>]+src=["']([^"']+)["'][^>]*label=["']([^"']+)["']/gi;
                            var trkMatch;
                            while ((trkMatch = trackRegex.exec(vCombined)) !== null) {
                                var trkUrl = trkMatch[1];
                                if (trkUrl.startsWith('//')) trkUrl = 'https:' + trkUrl;
                                else if (trkUrl.startsWith('/') && origin) trkUrl = origin + trkUrl;
                                vSubs.push({
                                    url: trkUrl,
                                    language: trkMatch[2] || 'Subtitles',
                                    headers: { 'Referer': origin ? (origin + '/') : 'https://vidoza.net/' }
                                });
                            }

                            if (vStreams.length > 0) {
                                return vStreams.map(function(vs) {
                                    return {
                                        name: options.name || 'Vidoza',
                                        title: options.title || options.name || 'Vidoza',
                                        url: vs.url,
                                        quality: options.quality || vs.quality || 'Auto',
                                        headers: {
                                            'Referer': origin ? (origin + '/') : 'https://vidoza.net/',
                                            'Origin': origin || 'https://vidoza.net',
                                            'User-Agent': vHdrs['User-Agent']
                                        },
                                        subtitles: vSubs
                                    };
                                });
                            }
                        } catch(vErr) {
                            console.error('[Vidoza] HTTP error:', vErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 9.3. StreamSB Family (streamsb, sbembed, sbplay, sbfull, watchsb, lvturbo)
                    if (/streamsb\.|sbembed\.|sbplay\.|sbfull\.|watchsb\.|lvturbo\./i.test(cleanUrl)) {
                        try {
                            var sbHdrs = Object.assign({
                                'Referer': origin ? (origin + '/') : cleanUrl
                            }, options.headers || {});
                            var sbOpts = Object.assign({}, options, { headers: sbHdrs });
                            return await Browser.sniffMedia(cleanUrl, sbOpts);
                        } catch(sbErr) {
                            console.error('[StreamSB] sniff error:', sbErr);
                            return [];
                        }
                    }

                    // 9.4. Upstream / Uqload (uqload, upstream.to)
                    if (/uqload\.|upstream\./i.test(cleanUrl)) {
                        try {
                            var upHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': origin ? (origin + '/') : cleanUrl
                            }, options.headers || {});
                            var upResp = await fetch(cleanUrl, { headers: upHdrs });
                            var upHtml = await upResp.text();
                            var upUnpacked = Unpacker.unpack(upHtml);
                            var upCombined = upHtml + '\n' + upUnpacked;

                            var upSources = [];
                            var srcArrMatch = upCombined.match(/sources\s*:\s*\[\s*["']([^"']+)["']\s*\]/i);
                            if (srcArrMatch) {
                                upSources.push(srcArrMatch[1]);
                            }
                            if (upSources.length === 0) {
                                var upExt = Unpacker.extractMediaUrls(upCombined, origin ? (origin + '/') : cleanUrl);
                                if (upExt.sources && upExt.sources.length > 0) {
                                    for (var upi = 0; upi < upExt.sources.length; upi++) {
                                        upSources.push(upExt.sources[upi].url);
                                    }
                                }
                            }

                            if (upSources.length > 0) {
                                return upSources.map(function(uUrl) {
                                    return {
                                        name: options.name || 'Upstream',
                                        title: options.title || options.name || 'Upstream',
                                        url: uUrl,
                                        quality: detectQuality(uUrl, options.quality || 'Auto'),
                                        headers: {
                                            'Referer': origin ? (origin + '/') : cleanUrl,
                                            'Origin': origin,
                                            'User-Agent': upHdrs['User-Agent']
                                        },
                                        subtitles: []
                                    };
                                });
                            }
                        } catch(upErr) {
                            console.error('[Upstream/Uqload] HTTP error:', upErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 9.5. Streamhub / Streamlare / Supervideo / DropLoad / Vtube
                    if (/streamlare\.|streamhub\.|supervideo\.|dropload\.|vtbe\.|vtube\./i.test(cleanUrl)) {
                        try {
                            var shHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': options.referer || cleanUrl
                            }, options.headers || {});
                            var shResp = await fetch(cleanUrl, { headers: shHdrs });
                            var shHtml = await shResp.text();
                            var shUnpacked = Unpacker.unpack(shHtml);
                            var shCombined = shHtml + '\n' + shUnpacked;
                            var shExt = Unpacker.extractMediaUrls(shCombined, cleanUrl);

                            if (shExt.sources && shExt.sources.length > 0) {
                                return shExt.sources.map(function(s) {
                                    return {
                                        name: options.name || 'Stream',
                                        title: options.title || options.name || 'Stream',
                                        url: s.url,
                                        quality: detectQuality(s.url, options.quality || s.label || 'Auto'),
                                        headers: {
                                            'Referer': origin ? (origin + '/') : cleanUrl,
                                            'Origin': origin,
                                            'User-Agent': shHdrs['User-Agent']
                                        },
                                        subtitles: (shExt.subtitles || []).map(function(sub) {
                                            return { url: sub.url, language: sub.language || 'Subtitles', headers: { 'Referer': origin ? (origin + '/') : cleanUrl } };
                                        })
                                    };
                                });
                            }
                        } catch(shErr) {
                            console.error('[Streamhub/Streamlare] HTTP error:', shErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 9.6. Vidguard / Listeamed / Bembed (vidguard, listeamed, bembed, vgfplay)
                    if (/vidguard\.|listeamed\.|bembed\.|vgfplay\./i.test(cleanUrl)) {
                        try {
                            var vgHdrs = Object.assign({
                                'Referer': origin ? (origin + '/') : cleanUrl
                            }, options.headers || {});
                            var vgOpts = Object.assign({}, options, { headers: vgHdrs });
                            return await Browser.sniffMedia(cleanUrl, vgOpts);
                        } catch(vgErr) {
                            console.error('[Vidguard] sniff error:', vgErr);
                            return [];
                        }
                    }

                    // 9.7. MediaFire (mediafire.com)
                    if (/mediafire\.com/i.test(cleanUrl)) {
                        try {
                            var mfHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': cleanUrl
                            }, options.headers || {});
                            var mfResp = await fetch(cleanUrl, { headers: mfHdrs });
                            var mfHtml = await mfResp.text();

                            var mfDlUrl = null;
                            var scrambledMatch = mfHtml.match(/data-scrambled-url=["']([^"']+)["']/i);
                            if (scrambledMatch) {
                                try {
                                    var dec = atob(scrambledMatch[1]);
                                    if (/^https?:\/\//i.test(dec)) mfDlUrl = dec;
                                } catch(e) {}
                            }
                            if (!mfDlUrl) {
                                var btnMatch = mfHtml.match(/<a[^>]+id=["']downloadButton["'][^>]+href=["']([^"']+)["']/i) ||
                                               mfHtml.match(/<a[^>]+href=["']([^"']+)["'][^>]+id=["']downloadButton["']/i) ||
                                               mfHtml.match(/aria-label=["']Download file["'][^>]+href=["']([^"']+)["']/i) ||
                                               mfHtml.match(/href=["'](https?:\/\/(?:download\d*|download\.[a-z0-9-]+)\.mediafire\.com\/[^"']+)["']/i);
                                if (btnMatch) mfDlUrl = btnMatch[1];
                            }
                            if (!mfDlUrl) {
                                var mfDoc = cheerio.load(mfHtml);
                                var aBtn = mfDoc('#downloadButton, a[aria-label="Download file"], a.popsok');
                                if (aBtn.length > 0) {
                                    var h = aBtn.attr('href');
                                    if (h && /^https?:\/\//i.test(h)) mfDlUrl = h;
                                }
                            }
                            if (mfDlUrl) {
                                return [{
                                    name: options.name || 'MediaFire',
                                    title: options.title || options.name || 'MediaFire',
                                    url: mfDlUrl,
                                    quality: detectQuality(mfDlUrl, options.quality || 'Direct'),
                                    headers: {
                                        'Referer': 'https://www.mediafire.com/',
                                        'User-Agent': mfHdrs['User-Agent']
                                    },
                                    subtitles: []
                                }];
                            }
                        } catch(mfErr) {
                            console.error('[MediaFire] HTTP error:', mfErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 9.8. KrakenFiles (krakenfiles.com)
                    if (/krakenfiles\.com/i.test(cleanUrl)) {
                        try {
                            var kfHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': cleanUrl
                            }, options.headers || {});
                            var kfResp = await fetch(cleanUrl, { headers: kfHdrs });
                            var kfHtml = await kfResp.text();

                            var kfStreamUrl = null;
                            var kfVidMatch = kfHtml.match(/<video[^>]+src=["']([^"']+)["']/i) ||
                                             kfHtml.match(/<source[^>]+src=["']([^"']+)["']/i) ||
                                             kfHtml.match(/data-file-url=["']([^"']+)["']/i) ||
                                             kfHtml.match(/var\s+videoSrc\s*=\s*["']([^"']+)["']/i);
                            if (kfVidMatch) {
                                kfStreamUrl = kfVidMatch[1];
                                if (kfStreamUrl.startsWith('//')) kfStreamUrl = 'https:' + kfStreamUrl;
                                else if (kfStreamUrl.startsWith('/') && origin) kfStreamUrl = origin + kfStreamUrl;
                            }
                            if (!kfStreamUrl) {
                                var kfDoc = cheerio.load(kfHtml);
                                var fAction = kfDoc('form.download-form, form#download-form, form[action*="download"]').attr('action');
                                if (fAction) {
                                    if (fAction.startsWith('/')) fAction = origin + fAction;
                                    if (/^https?:\/\//i.test(fAction)) kfStreamUrl = fAction;
                                }
                            }
                            if (!kfStreamUrl) {
                                var kfExt = Unpacker.extractMediaUrls(kfHtml, cleanUrl);
                                if (kfExt.sources && kfExt.sources.length > 0) {
                                    kfStreamUrl = kfExt.sources[0].url;
                                }
                            }
                            if (kfStreamUrl) {
                                return [{
                                    name: options.name || 'KrakenFiles',
                                    title: options.title || options.name || 'KrakenFiles',
                                    url: kfStreamUrl,
                                    quality: detectQuality(kfStreamUrl, options.quality || 'Direct'),
                                    headers: {
                                        'Referer': 'https://krakenfiles.com/',
                                        'User-Agent': kfHdrs['User-Agent']
                                    },
                                    subtitles: []
                                }];
                            }
                        } catch(kfErr) {
                            console.error('[KrakenFiles] HTTP error:', kfErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 9.9. Kwik (AnimePahe)
                    if (/kwik\./i.test(cleanUrl)) {
                        try {
                            var kwHdrs = Object.assign({
                                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                                'Referer': options.referer || cleanUrl
                            }, options.headers || {});
                            var kwResp = await fetch(cleanUrl, { headers: kwHdrs });
                            var kwHtml = await kwResp.text();
                            var kwUnpacked = Unpacker.unpack(kwHtml);
                            var kwCombined = kwHtml + '\n' + kwUnpacked;

                            var kwUrl = null;
                            var kwMatch = kwCombined.match(/(https?:\/\/[^"'\s<>]+\.m3u8[^"'\s<>]*)/i) ||
                                          kwCombined.match(/source\s*=\s*['"]([^'"]+)['"]/i);
                            if (kwMatch) kwUrl = kwMatch[1];
                            if (!kwUrl) {
                                var kwExt = Unpacker.extractMediaUrls(kwCombined, cleanUrl);
                                if (kwExt.sources && kwExt.sources.length > 0) kwUrl = kwExt.sources[0].url;
                            }

                            if (kwUrl) {
                                return [{
                                    name: options.name || 'Kwik',
                                    title: options.title || options.name || 'Kwik',
                                    url: kwUrl,
                                    quality: detectQuality(kwUrl, options.quality || 'Auto'),
                                    headers: {
                                        'Referer': cleanUrl,
                                        'Origin': origin || 'https://kwik.cx',
                                        'User-Agent': kwHdrs['User-Agent']
                                    },
                                    subtitles: []
                                }];
                            }
                        } catch(kwErr) {
                            console.error('[Kwik] HTTP error:', kwErr);
                        }
                        return await Browser.sniffMedia(cleanUrl, options);
                    }

                    // 9.10. Gogoanime / Vidstreaming / Anitaku Embeds
                    if (/gogoplay\d*\.|gogohd\d*\.|goload\d*\.|vidstreaming\.|anitaku\.|playgo\d*\./i.test(cleanUrl)) {
                        try {
                            var gogoHdrs = Object.assign({
                                'Referer': origin ? (origin + '/') : cleanUrl
                            }, options.headers || {});
                            var gogoOpts = Object.assign({}, options, { headers: gogoHdrs });
                            return await Browser.sniffMedia(cleanUrl, gogoOpts);
                        } catch(gErr) {
                            console.error('[Gogoanime/Vidstreaming] sniff error:', gErr);
                            return [];
                        }
                    }

                    // 10. MixDrop, Vidmoly, SuperStream, UpCloud, MegaCloud, VidSrc, VidLink (Fast HTTP + Unpack -> Sniff Fallback)
                    try {
                        var genHdrs = Object.assign({
                            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                            'Referer': options.referer || cleanUrl
                        }, options.headers || {});
                        var genResp = await fetch(cleanUrl, { headers: genHdrs });
                        var genHtml = await genResp.text();
                        var genUnpacked = Unpacker.unpack(genHtml);
                        var genExtracted = Unpacker.extractMediaUrls(genHtml + '\n' + genUnpacked, cleanUrl);

                        if (genExtracted.sources && genExtracted.sources.length > 0) {
                            return genExtracted.sources.map(function(s) {
                                return {
                                    name: options.name || 'Stream',
                                    title: options.title || options.name || 'Stream',
                                    url: s.url,
                                    quality: options.quality || detectQuality(s.url, s.label || 'Auto'),
                                    headers: {
                                        'Referer': origin ? (origin + '/') : cleanUrl,
                                        'Origin': origin,
                                        'User-Agent': genHdrs['User-Agent']
                                    },
                                    subtitles: (genExtracted.subtitles || []).map(function(sub) {
                                        return { url: sub.url, language: sub.language || 'Subtitles', headers: { 'Referer': origin ? (origin + '/') : cleanUrl } };
                                    })
                                };
                            });
                        }
                    } catch(genErr) {
                        console.error('[EmbedExtractor.generic] HTTP error:', genErr);
                    }

                    return await Browser.sniffMedia(cleanUrl, options);
                })();
                return sanitizeStreams(rawResult);
            } catch(topErr) {
                console.error('[EmbedExtractor.resolve] top-level error:', topErr);
                return [];
            }
        },

                resolveAll: async function(urlsOrItems, options) {
                    if (!Array.isArray(urlsOrItems) || urlsOrItems.length === 0) return [];
                    var concurrency = 4;
                    var results = [];
                    for (var i = 0; i < urlsOrItems.length; i += concurrency) {
                        var chunk = urlsOrItems.slice(i, i + concurrency);
                        var chunkPromises = chunk.map(function(item) {
                            var targetUrl = (typeof item === 'string') ? item : (item && (item.url || item.externalUrl));
                            var itemOpts = (typeof item === 'object' && item !== null) ? Object.assign({}, options, item) : (options || {});
                            return EmbedExtractor.resolve(targetUrl, itemOpts).catch(function(e) {
                                console.error('[EmbedExtractor.resolveAll] item error:', e);
                                return [];
                            });
                        });
                        var chunkResults = await Promise.all(chunkPromises);
                        for (var c = 0; c < chunkResults.length; c++) {
                            results = results.concat(chunkResults[c]);
                        }
                    }
                    return results;
                }
            };

            globalThis.EmbedExtractor = EmbedExtractor;
        }
    """.trimIndent()
}
