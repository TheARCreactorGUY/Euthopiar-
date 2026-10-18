# Euthopiar Plugin Development Guide

Euthopiar uses an ultra-fast, sandboxed **JavaScript Plugin Architecture** powered by QuickJS. Scrapers are lightweight, standalone `.js` files that can be installed directly from a remote repository (`manifest.json`) or imported locally from your device.

---

## 🌟 Overview & Key Concepts

Unlike legacy systems that required compiling Java/Kotlin bytecode or building complex `.eup` files, Euthopiar plugins are written in **standard modern JavaScript (ES2020 / CommonJS)**.

### How it Works:
1. **Catalog Presentation (Zero Scraping Needed)**:
   - Movies, TV shows, trending lists, carousels, and search results are automatically fetched directly from **TMDB (The Movie Database)**.
   - You **do not** need to scrape homepages, search lists, or episode guides unless you want custom catalog rows. Euthopiar handles rich metadata, high-res posters, backdrop art, and logos out of the box.

2. **Playback Resolution (`getStreams`)**:
   - When a user taps "Play" on a movie or episode, Euthopiar queries all installed and enabled JS plugins concurrently.
   - Each plugin's `getStreams(tmdbId, mediaType, season, episode)` function is called.
   - Streams and subtitles are streamed progressively to the player as each provider finishes resolving.

---

## 🛠️ Built-in Environment & Polyfills

Euthopiar provides a battery-included runtime for your scrapers. The following global objects and libraries are automatically available in your scripts:

| API / Library | Description |
|---|---|
| `fetch(url, options)` | Modern asynchronous HTTP client supporting `GET`, `POST`, custom `headers`, redirects, and JSON bodies. |
| `cheerio` / `cheerio.load(html)` | Fast, flexible jQuery-like HTML parser for scraping websites (`$('div.video-item').attr('data-id')`). |
| `Crypto` / `CryptoJS` | Cryptographic primitives for decrypters: AES (CBC, GCM), HMAC, MD5, SHA-256. |
| `btoa(str)` & `atob(b64)` | Base64 encoding and decoding. |
| `TextEncoder` & `TextDecoder` | UTF-8 byte stream conversions. |
| `URL` & `URLSearchParams` | Standard URL parsing and query string building. |
| `setTimeout`, `clearTimeout` | Coroutine-backed non-blocking timers. |
| `console.log`, `console.error` | Logging output visible in Android Logcat (`[QuickJS]`). |

---

## 📋 The Plugin Interface Contract

Every Euthopiar scraper is a CommonJS module exporting a `getStreams` function.

```javascript
module.exports = {
  // Plugin Metadata (Optional if defined in manifest.json)
  name: "MovieZone",
  version: "1.0.0",
  baseUrl: "https://moviezone.example",

  /**
   * Resolves direct video streams and subtitles for a media item.
   * 
   * @param {number|string} tmdbId - TMDB Movie or TV Show ID (e.g., 550 for Fight Club)
   * @param {'movie'|'tv'} mediaType - 'movie' or 'tv'
   * @param {number|null} season - Season number (1-indexed), or null/undefined for movies
   * @param {number|null} episode - Episode number (1-indexed), or null/undefined for movies
   * @returns {Promise<Array<StreamSource>>} Array of playable stream objects
   */
  getStreams: async function(tmdbId, mediaType, season, episode) {
    // Return array of StreamSource objects
    return [
      {
        server: "MovieZone VIP (HLS)",
        url: "https://stream.example/hls/master.m3u8",
        quality: "1080p",      // "Auto", "1080p", "720p", "4K", etc.
        isHls: true,           // true for .m3u8 (HLS), false for direct MP4/MKV
        headers: {             // Optional HTTP headers required by the video CDN
          "Referer": "https://moviezone.example/",
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        },
        subtitles: [           // Optional subtitles array
          {
            lang: "English",
            url: "https://stream.example/subs/en.vtt"
          }
        ]
      }
    ];
  }
};
```

### Stream Object Schema:
| Field | Type | Required | Description |
|---|---|---|---|
| `server` | `string` | **Yes** | Server or provider display name shown in the player source selector (e.g. `VidCloud [1080p]`). |
| `url` | `string` | **Yes** | Direct video stream URL (`.m3u8` or `.mp4`). |
| `quality` | `string` | No | Video quality tag: `"1080p"`, `"720p"`, `"4k"`, or `"Auto"` (default: `"Auto"`). |
| `isHls` | `boolean` | No | `true` if the stream is an HLS playlist (`.m3u8`), `false` for MP4. Defaults to `true` if the URL contains `.m3u8`. |
| `headers` | `object` | No | Key-value pairs for request headers needed by the CDN (e.g. `Referer`, `Origin`, `User-Agent`). |
| `subtitles` | `array` | No | Array of subtitle objects: `[{ lang: "English", url: "https://...vtt" }]`. |

---

## 🚀 Step-by-Step Tutorial: Writing a Scraper for Any Website

Let's build a real scraper for an example streaming site called `FlixWave` (`https://flixwave.to`).

### Step 1: Look Up the Title via TMDB
When `getStreams` is called, Euthopiar gives you the `tmdbId`. Most streaming websites use search bars rather than TMDB IDs, so we can fetch the title and release year from TMDB's free API:

```javascript
async function getMediaDetails(tmdbId, mediaType) {
  const url = `https://api.themoviedb.org/3/${mediaType}/${tmdbId}?api_key=4b2b3a1a09d3b0b7e41b2c45353597c2`;
  const res = await fetch(url);
  const data = await res.json();
  
  return {
    title: mediaType === 'movie' ? data.title : data.name,
    year: (mediaType === 'movie' ? data.release_date : data.first_air_date)?.substring(0, 4)
  };
}
```

### Step 2: Search the Website and Parse with Cheerio
Search the target website and parse the search results with Cheerio to find the matching movie or series page:

```javascript
const cheerio = require('cheerio'); // or cheerio is available globally

async function searchWebsite(query, year) {
  const searchUrl = `https://flixwave.to/search?keyword=${encodeURIComponent(query)}`;
  const res = await fetch(searchUrl, {
    headers: {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
  });
  const html = await res.text();
  const $ = cheerio.load(html);

  let targetLink = null;

  // Iterate over search items
  $('.film-item').each((i, el) => {
    const itemTitle = $(el).find('.film-name').text().trim();
    const itemYear = $(el).find('.fdi-item').text().trim();

    if (itemTitle.toLowerCase() === query.toLowerCase()) {
      targetLink = $(el).find('a').attr('href');
      return false; // Break loop
    }
  });

  return targetLink;
}
```

### Step 3: Extract Server Embeds & Direct Streams
Once on the movie or episode page, extract the embed iframes or API endpoints:

```javascript
async function extractStreams(pageUrl, season, episode) {
  // If TV show, navigate to specific episode
  const targetUrl = season && episode 
    ? `${pageUrl}/season-${season}-episode-${episode}`
    : pageUrl;

  const res = await fetch(targetUrl);
  const html = await res.text();
  const $ = cheerio.load(html);

  const streams = [];

  // Example: site embeds an iframe with data-src
  const embedUrl = $('iframe#player').attr('data-src') || $('iframe#player').attr('src');
  if (embedUrl) {
    // Resolve embed or add direct HLS stream
    streams.push({
      server: "FlixWave Main",
      url: embedUrl.startsWith('//') ? 'https:' + embedUrl : embedUrl,
      quality: "1080p",
      isHls: true,
      headers: {
        "Referer": "https://flixwave.to/"
      }
    });
  }

  return streams;
}
```

### Step 4: Putting It All Together
Assemble everything into your `module.exports`:

```javascript
module.exports = {
  name: "FlixWave",
  version: "1.0.0",

  getStreams: async function(tmdbId, mediaType, season, episode) {
    try {
      // 1. Get Title from TMDB
      const details = await getMediaDetails(tmdbId, mediaType);
      if (!details || !details.title) return [];

      // 2. Search target website
      const pageLink = await searchWebsite(details.title, details.year);
      if (!pageLink) return [];

      // 3. Extract stream sources
      const results = await extractStreams(pageLink, season, episode);
      return results;
    } catch (e) {
      console.error("FlixWave error:", e.message || e);
      return [];
    }
  }
};
```

---

## 📦 Creating a Repository (`manifest.json`)

If you want to host multiple plugins on GitHub or any web server, create a `manifest.json` file in the root of your repository:

```json
{
  "name": "Community Plugins",
  "description": "High-speed streaming providers for Euthopiar",
  "scrapers": [
    {
      "id": "flixwave",
      "name": "FlixWave",
      "version": "1.0.0",
      "description": "Fast multi-server movie and TV provider",
      "file": "scrapers/flixwave.js",
      "icon": "https://example.com/icons/flixwave.png",
      "types": ["movie", "tv"]
    },
    {
      "id": "anime-pulse",
      "name": "Anime Pulse",
      "version": "1.1.0",
      "description": "Anime sub and dub streaming",
      "file": "scrapers/anime-pulse.js",
      "icon": "https://example.com/icons/anime.png",
      "types": ["anime", "tv", "movie"]
    }
  ]
}
```

### Manifest Fields:
- `id`: Unique lowercase identifier for the scraper (e.g. `flixwave`).
- `name`: Display name shown in Euthopiar.
- `version`: Version string (e.g. `"1.0.0"`).
- `file`: Path to the `.js` file, relative to `manifest.json` (or an absolute HTTPS URL).
- `icon`: Optional icon image URL.
- `types`: Supported media types (`["movie", "tv", "anime"]`).

---

## 📲 How to Install & Test Your Plugins

### Method 1: Local File Import (Best for Development)
1. Write and save your `.js` file on your computer or phone (e.g. `my-provider.js`).
2. Transfer the file to your Android device (via USB, local network, or Google Drive).
3. Open **Euthopiar**.
4. Go to **Settings > Plugin Manager**.
5. Tap **Import Local (.js)**.
6. Select your `my-provider.js` file. The plugin will immediately appear in your installed list!

### Method 2: Remote Repository URL (Best for Distribution)
1. Push your `manifest.json` and `.js` files to GitHub (e.g. `https://github.com/YourUsername/euthopiar-plugins`).
2. Copy the **Raw** URL of the `manifest.json` file:
   `https://raw.githubusercontent.com/YourUsername/euthopiar-plugins/main/manifest.json`
3. In Euthopiar, open **Settings > Plugin Manager > Add Repository**.
4. Paste the URL and tap **Save**.
5. Browse and tap **Install** on any scraper.

---

## 💡 Best Practices & Pro Tips

1. **Always Return an Array**:
   `getStreams` must always resolve to an array of stream objects, even if empty (`return []`).

2. **Handle Errors Gracefully**:
   Wrap network requests in `try ... catch` blocks so an individual failed request doesn't crash the plugin execution.

3. **Referer & Origin Headers**:
   Many video CDNs check the `Referer` or `Origin` header. Always pass the origin website's URL in `headers: { "Referer": "https://site.to/" }`.

4. **Multiple Servers**:
   If a website provides multiple mirrors (e.g., VidCloud, MegaCloud, StreamWish), return all of them in the array! Euthopiar displays all returned servers in the in-player source switcher, allowing users to choose their favorite.

5. **No Emulators Needed**:
   You can develop and debug plugins using standard Node.js on your computer or test directly on your Android device.
