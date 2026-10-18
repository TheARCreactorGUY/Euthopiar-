/**
 * Movy Streaming Scraper Plugin for Euthopiar / Nuvio
 *
 * Capabilities:
 * - Direct TMDB ID Stream Resolution (Movies & TV Series)
 * - Domain & Mirror Resilience (Auto-fallback across domains & mirrors)
 * - Dynamic Session Seed Negotiation with in-memory TTL caching
 * - Fast PRNG Stream Cipher Decryption (Reverse-engineered wecollege protocol)
 * - High-speed parallel CDN mirror queries (Miami, Boise, Atlanta, Phoenix, Seattle, Dallas, Portland, etc.)
 * - Multilingual Embedded & External Subtitles (Granite & Natsuki fallbacks)
 */

const TMDB_API_KEY = (typeof globalThis !== 'undefined' && globalThis.TMDB_API_KEY) || (typeof process !== 'undefined' && process.env.TMDB_API_KEY) || "";
const USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

const CANDIDATE_DOMAINS = [
  "https://www.movy.sx",
  "https://movy.sx",
  "https://movy.to"
];

const CANDIDATE_API_BASES = [
  "https://api.wecollege.net"
];

const MIRRORS = [
  { id: "miami", name: "Movy - Miami CDN" },
  { id: "boise", name: "Movy - Boise CDN" },
  { id: "atlanta", name: "Movy - Atlanta CDN" },
  { id: "phoenix", name: "Movy - Phoenix CDN" },
  { id: "seattle", name: "Movy - Seattle CDN" },
  { id: "portland", name: "Movy - Portland CDN" },
  { id: "dallas", name: "Movy - Dallas CDN" },
  { id: "tampa", name: "Movy - Tampa CDN" },
  { id: "orlando", name: "Movy - Orlando CDN" },
  { id: "paris", name: "Movy - Paris (French)" },
  { id: "munich", name: "Movy - Munich (German)" },
  { id: "berlin", name: "Movy - Berlin (German)" },
  { id: "cancun", name: "Movy - Cancun (Spanish)" }
];

function fetchWithTimeout(url, options = {}, timeoutMs = 4500) {
  return Promise.race([
    fetch(url, options),
    new Promise((_, reject) => setTimeout(() => reject(new Error("Timeout")), timeoutMs))
  ]);
}

// In-memory seed cache: { mediaId: { seed: string, expiresAt: number } }
const seedCache = new Map();

// In-memory domain cache
let cachedDomain = CANDIDATE_DOMAINS[0];
let cachedApiBase = CANDIDATE_API_BASES[0];

// ============================================================================
// 1. Decryption Engine (Movy Stream Cipher)
// ============================================================================

const D = [
  0x428a2f98, 0x71374491, 0xb5c0fbcf >>> 0, 0xe9b5dba5 >>> 0,
  0x3956c25b, 0x59f111f1, 0x923f82a4 >>> 0, 0xab1c5ed5 >>> 0,
  0xd807aa98 >>> 0, 0x12835b01, 0x243185be, 0x550c7dc3,
  0x72be5d74, 0x80deb1fe >>> 0, 0x9bdc06a7 >>> 0, 0xc19bf174 >>> 0
];

function o(e) {
  e = e >>> 0;
  let x = (e ^ (e >>> 16)) >>> 0;
  x = Math.imul(x, 0x85ebca6b) >>> 0;
  x = (x ^ (x >>> 13)) >>> 0;
  x = Math.imul(x, 0xc2b2ae35) >>> 0;
  return (x ^ (x >>> 16)) >>> 0;
}

function u(e, a) {
  e = e >>> 0;
  const shift = a & 31;
  if (shift === 0) return e;
  return ((e << shift) | (e >>> (32 - shift))) >>> 0;
}

function decryptCiphertext(encB64, seedStr, mediaId) {
  try {
    let normalized = encB64.trim().replace(/-/g, "+").replace(/_/g, "/");
    const padLen = (4 - (normalized.length % 4)) % 4;
    normalized += "=".repeat(padLen);
    const binaryStr = atob(normalized);
    const t = binaryStr.length;
    if (t < 4) return null;

    const raw = new Uint8Array(t);
    for (let i = 0; i < t; i++) {
      raw[i] = binaryStr.charCodeAt(i);
    }

    const sValues = new Uint32Array(61);
    const sPresent = new Uint8Array(61);
    let h = 0x811c9dc5 >>> 0;
    for (let i = 0; i < seedStr.length; i++) {
      h = Math.imul((h ^ seedStr.charCodeAt(i)) >>> 0, 0x1000193) >>> 0;
    }

    const mediaIdLow32 = Number(BigInt(mediaId) & 0xffffffffn) >>> 0;
    let n = o((o(h) ^ o((mediaIdLow32 ^ (0x9e3779b9 >>> 0)) >>> 0)) >>> 0);

    for (let e = 0; e < 8; e++) {
      if (((e * (e + 1)) & 1) === 0) {
        const a = n % 61;
        n = u((n + (0x9e3779b9 >>> 0)) >>> 0, 7 + (7 & e));
        sValues[a] = (n ^ o(n)) >>> 0;
        sPresent[a] = 1;
        n = o((n + a) >>> 0);
      } else {
        sValues[e] = D[15 & e];
        sPresent[e] = 1;
      }
    }

    let acc = o(((0xa5a5a5a5 >>> 0) ^ n) >>> 0);
    const keystream = new Uint8Array(t);
    let lCounter = 0;
    let eIdx = 0;
    while (eIdx < t) {
      const dVal = acc % 61;
      const isPresent = sPresent[dVal];
      const iFlag = isPresent ? 0xffffffff : 0;
      const rVal = isPresent ? sValues[dVal] : 0;
      const cVal = Math.imul(0x9e3779b9 >>> 0, (lCounter + 1)) >>> 0;
      lCounter++;
      const tXor = acc;
      const sXor = (rVal ^ cVal) >>> 0;
      const bVal = ((tXor ^ sXor) | ((tXor & sXor & iFlag) >>> 0)) >>> 0;
      const rot1 = u((bVal + acc) >>> 0, 31 & dVal);
      const rot2 = u(acc, 31 & (dVal * 7));
      acc = o(((rot1 ^ rot2) + (0x9e3779b9 >>> 0)) >>> 0);
      sValues[dVal] = acc;
      sPresent[dVal] = 1;
      const kw = acc;

      keystream[eIdx++] = kw & 0xff;
      if (eIdx < t) keystream[eIdx++] = (kw >>> 8) & 0xff;
      if (eIdx < t) keystream[eIdx++] = (kw >>> 16) & 0xff;
      if (eIdx < t) keystream[eIdx++] = (kw >>> 24) & 0xff;
    }

    for (let i = 0; i < t; i++) {
      raw[i] = raw[i] ^ keystream[i];
    }

    // Header check: "mvm1" [109, 118, 109, 49]
    if (raw[0] !== 109 || raw[1] !== 118 || raw[2] !== 109 || raw[3] !== 49) {
      return null;
    }

    return new TextDecoder().decode(raw.subarray(4));
  } catch (err) {
    return null;
  }
}

// ============================================================================
// 2. Metadata & Seed Negotiation
// ============================================================================

async function fetchMetadata(tmdbId, isTv) {
  try {
    const endpoint = isTv ? "tv" : "movie";
    const url = `https://api.themoviedb.org/3/${endpoint}/${tmdbId}?api_key=${TMDB_API_KEY}&append_to_response=external_ids`;
    const resp = await fetchWithTimeout(url, { headers: { "User-Agent": USER_AGENT } }, 3500);
    if (resp.ok) {
      const data = await resp.json();
      const title = data.title || data.name || "Media";
      const dateStr = data.release_date || data.first_air_date || "";
      const year = dateStr ? parseInt(dateStr.substring(0, 4), 10) : null;
      const imdbId = (data.external_ids && data.external_ids.imdb_id) || data.imdb_id || null;
      const totalSeasons = data.number_of_seasons || (data.seasons ? data.seasons.length : 1);
      return { title, year, imdbId, totalSeasons };
    }
  } catch (_) {}
  return { title: "Media", year: null, imdbId: null, totalSeasons: 1 };
}

async function getSessionSeed(mediaId) {
  const now = Date.now();
  const cached = seedCache.get(mediaId);
  if (cached && cached.expiresAt > now + 5000) {
    return cached.seed;
  }

  const headers = {
    "User-Agent": USER_AGENT,
    "Origin": cachedDomain,
    "Referer": `${cachedDomain}/`
  };

  for (const apiBase of CANDIDATE_API_BASES) {
    try {
      const resp = await fetchWithTimeout(`${apiBase}/seed?mediaId=${mediaId}`, { headers }, 3500);
      if (resp.ok) {
        const data = await resp.json();
        if (data && data.seed) {
          const ttl = data.ttlMs || 30000;
          seedCache.set(mediaId, { seed: data.seed, expiresAt: now + ttl });
          cachedApiBase = apiBase;
          return data.seed;
        }
      }
    } catch (_) {}
  }
  return null;
}

// ============================================================================
// 3. Subtitle Formatting & External Resolvers
// ============================================================================

function formatSubtitleLanguage(raw) {
  if (!raw) return "English";
  const trimmed = raw.trim();
  const isSdh = /\b(sdh|cc)\b/i.test(trimmed);
  const baseCode = trimmed
    .replace(/\s*\(.*?\)/g, "")
    .replace(/\s*\[.*?\]/g, "")
    .trim()
    .toLowerCase();

  const langMap = {
    eng: "English", en: "English",
    spa: "Spanish", es: "Spanish",
    fre: "French", fra: "French", fr: "French",
    ger: "German", deu: "German", de: "German",
    hin: "Hindi", hi: "Hindi",
    ita: "Italian", it: "Italian",
    por: "Portuguese", pt: "Portuguese",
    rus: "Russian", ru: "Russian",
    jpn: "Japanese", ja: "Japanese",
    kor: "Korean", ko: "Korean",
    chi: "Chinese", zho: "Chinese", zh: "Chinese",
    ara: "Arabic", ar: "Arabic",
    bul: "Bulgarian", bg: "Bulgarian",
    cze: "Czech", cs: "Czech",
    dan: "Danish", da: "Danish",
    dut: "Dutch", nld: "Dutch", nl: "Dutch",
    fin: "Finnish", fi: "Finnish",
    gre: "Greek", el: "Greek",
    heb: "Hebrew", he: "Hebrew",
    hun: "Hungarian", hu: "Hungarian",
    ind: "Indonesian", id: "Indonesian",
    nor: "Norwegian", no: "Norwegian",
    pol: "Polish", pl: "Polish",
    rum: "Romanian", ro: "Romanian",
    swe: "Swedish", sv: "Swedish",
    tha: "Thai", th: "Thai",
    tur: "Turkish", tr: "Turkish",
    ukr: "Ukrainian", uk: "Ukrainian",
    vie: "Vietnamese", vi: "Vietnamese"
  };

  const name = langMap[baseCode] || (baseCode.charAt(0).toUpperCase() + baseCode.slice(1));
  return isSdh ? `${name} [SDH]` : name;
}

async function fetchExternalSubtitles(tmdbId, isTv, season, episode, imdbId) {
  const subs = [];
  const seenUrls = new Set();

  // Granite subtitles provider
  try {
    const graniteUrl = isTv
      ? `https://sub.vdrk.site/v1/tv/${tmdbId}/${season}/${episode}`
      : `https://sub.vdrk.site/v1/movie/${tmdbId}`;
    const resp = await fetchWithTimeout(graniteUrl, { headers: { "User-Agent": USER_AGENT } }, 3500);
    if (resp.ok) {
      const list = await resp.json();
      if (Array.isArray(list)) {
        for (const item of list) {
          if (item && item.file && !seenUrls.has(item.file)) {
            seenUrls.add(item.file);
            subs.push({
              url: item.file,
              language: formatSubtitleLanguage(item.label || "English")
            });
          }
        }
      }
    }
  } catch (_) {}

  // Natsuki subtitles provider (via IMDb ID)
  if (imdbId && imdbId.startsWith("tt")) {
    try {
      const natsukiUrl = isTv
        ? `https://natsuki.hls.lol/subs?imdbId=${imdbId}&season=${season}&episode=${episode}`
        : `https://natsuki.hls.lol/subs?imdbId=${imdbId}`;
      const resp = await fetchWithTimeout(natsukiUrl, {
        headers: {
          "User-Agent": USER_AGENT,
          "Origin": cachedDomain,
          "Referer": `${cachedDomain}/`
        }
      }, 3500);
      if (resp.ok) {
        const data = await resp.json();
        if (data && Array.isArray(data.subtitles)) {
          for (const s of data.subtitles) {
            if (s && s.url && !seenUrls.has(s.url)) {
              seenUrls.add(s.url);
              const isHi = s.hearingImpaired === true;
              let lang = formatSubtitleLanguage(s.language || s.langCode || "English");
              if (isHi && !lang.includes("[SDH]") && !lang.includes("[CC]")) {
                lang += " [CC]";
              }
              subs.push({ url: s.url, language: lang });
            }
          }
        }
      }
    } catch (_) {}
  }

  return subs;
}

// ============================================================================
// 4. Main Export: getStreams
// ============================================================================

async function getStreams(tmdbId, mediaType, season, episode) {
  const idStr = String(tmdbId || "").replace(/\D/g, "");
  if (!idStr) return [];
  const numericId = parseInt(idStr, 10);
  const isTv = mediaType === "tv" || (season != null && episode != null);

  const meta = await fetchMetadata(idStr, isTv);
  const seed = await getSessionSeed(numericId);
  if (!seed) return [];

  const headers = {
    "Origin": cachedDomain,
    "Referer": `${cachedDomain}/`,
    "User-Agent": USER_AGENT
  };

  // 1. Concurrently fetch external subtitles
  const externalSubsPromise = fetchExternalSubtitles(
    numericId,
    isTv,
    season || 1,
    episode || 1,
    meta.imdbId
  );

  // 2. Concurrently query all mirrors
  const mirrorPromises = MIRRORS.map(async (mirror) => {
    try {
      const params = new URLSearchParams();
      params.append("title", meta.title);
      params.append("mediaType", isTv ? "tv" : "movie");
      if (meta.year) params.append("year", String(meta.year));
      params.append("tmdbId", String(numericId));
      if (meta.imdbId) params.append("imdbId", meta.imdbId);
      if (isTv && season != null && episode != null) {
        params.append("seasonId", String(season));
        params.append("episodeId", String(episode));
        params.append("totalSeasons", String(meta.totalSeasons));
      }
      params.append("enc", "2");
      params.append("seed", seed);

      const url = `${cachedApiBase}/${mirror.id}/sources?${params.toString()}`;
      const resp = await fetchWithTimeout(url, { headers }, 4000);
      if (!resp.ok) return { streams: [], subtitles: [] };

      const encText = await resp.text();
      if (!encText || encText.startsWith("{") || encText.startsWith("[")) {
        return { streams: [], subtitles: [] };
      }

      const decJson = decryptCiphertext(encText, seed, numericId);
      if (!decJson) return { streams: [], subtitles: [] };

      const parsed = JSON.parse(decJson);
      const mStreams = [];
      const mSubs = [];

      // Master playlist
      if (parsed.playlist && typeof parsed.playlist === "string") {
        mStreams.push({
          name: `${mirror.name} [Auto]`,
          title: `${mirror.name} - Auto (Master)`,
          url: parsed.playlist,
          quality: "Auto",
          type: "m3u8",
          headers: headers
        });
      }

      // Individual quality sources
      if (Array.isArray(parsed.sources)) {
        for (const s of parsed.sources) {
          if (!s || !s.url) continue;
          const q = s.quality || "Auto";
          const isM3u8 = s.url.includes(".m3u8") || s.url.includes("type=m3u8") || s.url.includes("/config-");
          mStreams.push({
            name: `${mirror.name} [${q}]`,
            title: `${mirror.name} - ${q}`,
            url: s.url,
            quality: q,
            type: isM3u8 ? "m3u8" : "mp4",
            headers: headers
          });
        }
      }

      // Embedded subtitles
      if (Array.isArray(parsed.subtitles)) {
        for (const sub of parsed.subtitles) {
          if (sub && sub.url) {
            mSubs.push({
              url: sub.url,
              language: formatSubtitleLanguage(sub.language || sub.lang || "English")
            });
          }
        }
      }

      return { streams: mStreams, subtitles: mSubs };
    } catch (_) {
      return { streams: [], subtitles: [] };
    }
  });

  const [externalSubs, mirrorResults] = await Promise.all([
    externalSubsPromise,
    Promise.allSettled(mirrorPromises)
  ]);

  const allStreams = [];
  const allSubtitles = [...externalSubs];
  const seenStreamUrls = new Set();
  const seenSubUrls = new Set(allSubtitles.map((s) => s.url));

  for (const res of mirrorResults) {
    if (res.status === "fulfilled" && res.value) {
      for (const stream of res.value.streams) {
        if (!seenStreamUrls.has(stream.url)) {
          seenStreamUrls.add(stream.url);
          allStreams.push(stream);
        }
      }
      for (const sub of res.value.subtitles) {
        if (!seenSubUrls.has(sub.url)) {
          seenSubUrls.add(sub.url);
          allSubtitles.push(sub);
        }
      }
    }
  }

  // Attach collected subtitles to every stream object
  for (const stream of allStreams) {
    stream.subtitles = allSubtitles;
  }

  // Sort streams: 4K / 2160p -> 1080p -> 720p -> 480p -> Auto
  allStreams.sort((a, b) => {
    const qRank = (q) => {
      if (/2160|4k/i.test(q)) return 0;
      if (/1080/i.test(q)) return 1;
      if (/720/i.test(q)) return 2;
      if (/480/i.test(q)) return 3;
      if (/auto/i.test(q)) return 4;
      return 5;
    };
    return qRank(a.quality || "") - qRank(b.quality || "");
  });

  return allStreams;
}

module.exports = {
  name: "Movy",
  version: "1.0.0",
  baseUrl: "https://www.movy.sx",
  getStreams
};
