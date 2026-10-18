# Euthopiar APK Release Documentation

## Build Information
- **Application**: Euthopiar
- **Release Version**: v1.0.0-stable (Movy High-Speed Multi-Server Streaming Engine, PRNG Decryptor & Local Network Distribution)
- **Build Timestamp**: 2026-09-30 02:19:30
- **File Name**: `euthopiar-stable.apk` (also mirrored as `euthopiar.apk`)
- **File Size**: ~25.95 MB (27,207,583 bytes)
- **SHA256 Checksum**: `8C618DFBA9606C54FC4B5BE698D75890064E51B8DC2996FF5BBA65E97F774B16`
- **Package Path**: `dist_apk/euthopiar-stable.apk`
- **Build Output**: `app/build/outputs/apk/release/app-release.apk`

---

## Local Network Download Access

The APK is hosted on the active local HTTP server on port 8080:

- **Local Wi-Fi Download Link (Direct Stable APK)**:
  [http://192.168.1.100:8080/euthopiar-stable.apk](http://192.168.1.100:8080/euthopiar-stable.apk)
- **Local Wi-Fi Download Link (Alias APK)**:
  [http://192.168.1.100:8080/euthopiar.apk](http://192.168.1.100:8080/euthopiar.apk)
- **Local Network Web Page**:
  [http://192.168.1.100:8080/](http://192.168.1.100:8080/)
- **Localhost Link**:
  [http://127.0.0.1:8080/euthopiar-stable.apk](http://127.0.0.1:8080/euthopiar-stable.apk)

Any phone, tablet, TV, or PC connected to the same Wi-Fi network (`192.168.1.x`) can open the link in any browser or scan the QR code to download and install the APK directly.

---

## Root Causes Identified & Fixes Implemented

### 1. Details Page Empty Metadata Fix
- **Root Cause**:
  1. `MediaDetailsViewModel.loadDetails(mediaId, provider, type)` calls `repository.getDetails(mediaId, type, providerName)`.
  2. `ProviderRepository.getDetails(mediaId, ...)` constructed a dummy `MediaItem(id = mediaId, title = "", url = "", ...)` with an empty URL.
  3. In `HubFlixPlugin`, `mediaItem.url` was `""`, which failed HTML fetching and caused `synopsis`, `genres`, `cast`, and `episodes` to be blank.
  4. Cards originally had `provider = "4KHDHub"`, `"HDHub4u"`, or `"BollyFlix"`. Because these weren't registered plugin names, `PluginRegistry.getPlugin(provider)` returned null and fell back to `CinejoyPlugin`.
- **Fix Implemented**:
  - `MediaItem.provider` is now set to `"HubFlix"` across all 3 sites.
  - `PluginRegistry.getPlugin()` now recognizes `"4KHDHub"`, `"HDHub4u"`, and `"BollyFlix"` as direct aliases for `HubFlix`.
  - `HubFlixPlugin` now encodes the full post URL safely into `MediaItem.id` using URL-safe Base64 (`4khd:<base64>`, `hdhub:<base64>`, `bolly:<base64>`).
  - `resolveUrlAndSite()` reconstructs the exact post URL and site whether `url` is provided, encoded in `id`, or passed as a slug.
  - Page title, year, poster image, and storyline synopsis are extracted directly from the post HTML with robust fallback regexes, and enriched via Cinemeta when IMDb/TMDB ID is present.
  - If IMDb ID is missing from post text, `searchCinemetaImdb()` searches the Cinemeta catalog using the clean title to resolve the ID automatically.

### 2. Player "No Source / Source Error" Fix
- **Root Cause**:
  1. When clicking Play, `AppNavHost` navigates with `episodeData = episode.data ?: detail.id`.
  2. If `getDetails` had failed, `episodeData` was `4khd:...` or empty.
  3. `parseEpisodePayload` failed to resolve non-JSON or raw ID data, causing empty stream sources and passing an invalid URL to ExoPlayer.
- **Fix Implemented**:
  - `parseEpisodePayload()` safely handles JSON payloads, raw media IDs, and direct URLs.
  - Decrypts native HDHub4u `hubstream.art` AES-128-CBC payloads into Cloudflare edge HLS master playlists (`cfNative`) and CDN streams (`source`).
  - Decodes base64 `?r=...` from `hubcdn.club` to obtain direct Cloudflare R2 video streams (`pub-*.r2.dev/*.mkv` or `.mp4`).
  - Integrates 4KHDHub Videasy embeds (`player.videasy.net/movie/{tmdbId}`) and BollyFlix LinksMod direct stream mirrors.
  - Universal Aether/Meridian cloud stream fallback ensures playable 1080p/720p HLS sources are available for every title.
  - Multilingual subtitle tracks in English, Hindi, Tamil, and Telugu (`sub.vdrk.site/subtitles/...`).

### 3. Quality & Audio Track Options
- ExoPlayer automatically inspects HLS multi-variant playlists and progressive video tracks.
- Populates full video quality selection: **4K 2160p**, **1080p (Full HD)**, **720p (HD)**, **480p (SD)**, and **Auto (Adaptive)**.
- Populates detected audio language tracks (Hindi, English, Dual Audio, Tamil, Telugu) and subtitles in the player controls sheet.

### 4. Download Options Fix
- **Root Cause**:
  1. `MediaDetailsScreen` line 195 set `episodeToDownload = null` for movies, causing `DownloadQualitySheet` to pass `epData = detail.id` which was unresolved.
  2. `getDownloadLinks` didn't extract the latest download hosts.
- **Fix Implemented**:
  - `MediaDetailsScreen` now passes `episodeToDownload = detail.episodes.firstOrNull()` for movies, ensuring `epData` carries the complete JSON payload with pre-parsed download links.
  - `HubFlixPlugin.getDownloadLinks()` extracts:
    - **HDHub4u**: Direct Cloudflare R2 downloads from `hubcdn.club` and HubCloud fast mirrors from `greenmotors.club`.
    - **BollyFlix**: Multi-host mirrors from `linksmod.top` (MegaUp, 1Fichier, GoFile, DailyUploads, MixDrop) and Google Drive fast downloads from `fastdlserver.site`.
    - **4KHDHub**: HubCloud & HubDrive mirrors from `greenmotors.club`.
    - **Stream Fallbacks**: Direct video streams offered as download options if post hosts are unreachable.
  - `DownloadEngine.kt` injects all required `Referer`, `Origin`, and `User-Agent` headers across all plugin domains.

---

## Verification & Testing
- Unit Test Suite: [HubFlixPluginTest.kt](file:///C:/Euthopiar/core/src/test/java/com/euthopiar/core/provider/HubFlixPluginTest.kt)
- Test Command: `./gradlew.bat :core:testDebugUnitTest --tests "com.euthopiar.core.provider.HubFlixPluginTest"`
- Verification Results:
  1. `testHomeCatalogLoadsRowsFromAllSites` -> **PASSED** (all 4 rows and all 3 site badges verified, provider="HubFlix")
  2. `testSearchReturnsResultsFromMultipleSites` -> **PASSED** (multi-site unified search verified)
  3. `testMovieDetailsAndMetadataEnrichmentWithEmptyUrl` -> **PASSED** (verified full resolution when URL is blank, matching `MediaDetailsViewModel`)
  4. `testTvSeriesDetailsAndEpisodeThumbnails` -> **PASSED** (TV series episode count, titles, and payload verified)
  5. `testStreamExtractionAndDecryption` -> **PASSED** (AES-128-CBC decrypted HLS streams and subtitles verified)
  6. `testDownloadLinkExtraction` -> **PASSED** (multi-host download options extracted from mirrors)
  7. `testPluginRegistryResolvesHubFlixAndAliases` -> **PASSED** (verified "HubFlix", "4KHDHub", "HDHub4u", "BollyFlix" all resolve to HubFlixPlugin)
- Overall Test Result: **7/7 Tests Passed (100% Green)**
- Local Server Status: **Running on `0.0.0.0:8080` (HTTP 200 OK verified on `192.168.1.100` and `127.0.0.1`)**
