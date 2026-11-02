# 🛡️ EUTHOPIAR AI GUARDRAILS & ARCHITECTURE GUIDE

> **MANDATORY INSTRUCTIONS FOR ALL AI CODING AGENTS (Cursor, Windsurf, Claude Code, Gemini, Copilot, etc.)**
> Every AI agent operating in this repository MUST read, understand, and strictly follow these rules before making any changes.

---

## 📁 1. EXACT FOLDER & CODEBASE ARCHITECTURE

`C:\Euthopiar` is an Android streaming application built with Kotlin, Jetpack Compose, ExoPlayer, and an embedded QuickJS engine for extensions/addons.

```text
C:\Euthopiar\
├── app/                                       # Android Application Module
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/euthopiar/
│       │   ├── EuthopiarApp.kt                # Application entrypoint & DI setup
│       │   ├── app/plugin/                    # Plugin repository service
│       │   ├── data/                          # Data layer (PreferencesRepository, TopTenRepository)
│       │   ├── download/                      # DownloadEngine & DownloaderFactory
│       │   └── ui/                            # Jetpack Compose UI
│       │       ├── components/                # Reusable Compose widgets (ContinueWatching, Hero, etc.)
│       │       ├── details/                   # MediaDetailsScreen & AmbientBackdropPlayer
│       │       ├── navigation/                # AppNavHost & screen routing
│       │       ├── player/                    # PlayerActivity, PlayerViewModel, EpisodeSelectorPanel
│       │       └── settings/                  # Settings pages (Integrations, Plugins, Appearance)
│       └── res/                               # Android UI resources (drawables, layouts, values)
│
├── core/                                      # Kotlin Multiplatform / Core JVM Android Library
│   └── src/main/java/com/euthopiar/core/
│       ├── addons/                            # Addon repository, manifest & meta parser
│       ├── js/                                # QuickJS runtime & host environment
│       │   ├── JsRuntime.kt                   # QuickJS engine wrapper
│       │   ├── host/HostFunctions.kt          # JavaScript bridge bindings (DO NOT TOUCH)
│       │   └── network/HttpRequestRaw.kt      # Raw HTTP engine for plugins
│       ├── network/                           # DohDns (DNS-over-HTTPS), OkHttpClient, interceptors
│       ├── streams/                           # Stream resolver repositories & models
│       ├── subtitle/external/SubDlClient.kt   # SubDl subtitles API client (DO NOT TOUCH)
│       ├── sync/                              # Media tracking sync providers
│       │   ├── simkl/SimklClient.kt           # Simkl OAuth & tracking client (DO NOT TOUCH)
│       │   └── trakt/TraktClient.kt           # Trakt.tv OAuth & tracking client (DO NOT TOUCH)
│       └── util/TmdbBridge.kt                 # TMDb API bridge & image caching (DO NOT TOUCH)
│
├── gradle/                                    # Gradle wrapper and version catalogs
├── build.gradle.kts                           # Root Gradle build script
├── settings.gradle.kts                        # Gradle settings & project modules
├── scripts/ & tools/                          # Internal testing & provider analysis tools
└── scratch/                                   # Safe sandbox for temporary test files
```

### System Ecosystem Context
- **Daily Workshop:** `C:\Euthopiar` (where development and vibe-coding happen).
- **Plugins Repo (Sole Source of Truth):** `C:\Euthopiar-Plugins` (linked to GitHub repo `https://github.com/TheARCreactorGUY/Plugins.git`).
- **Open Source Mirror:** `C:\Euthopiar Open Source Release` (linked to GitHub repo `https://github.com/TheARCreactorGUY/Euthopiar-.git`).
- **Safety Backups:** `C:\Euthopiar-System\Backups` (timestamped zip snapshots).
- **Production Builds:** `C:\Euthopiar-System\Production-Releases` (signed/clean release APKs and AABs).
- **Control Center App:** `C:\Euthopiar-System\Control-Center` (desktop visual manager).
- **Cold Storage Archive:** `E:\Device backup\Euthopiar-ColdStorage\` (historical multi-gigabyte archives).

---

## 🔒 2. CREDENTIALS ARCHITECTURE & PROTECTED BRIDGES

All production API credentials are now strictly abstracted into `local.properties` (gitignored across all repos) and dynamically loaded at compile time into `BuildConfig.*`. Never re-introduce hardcoded plaintext keys into source files.

The following files act as protected cryptographic runtimes and credential bridges:
1. 🛑 `core/src/main/java/com/euthopiar/core/util/TmdbBridge.kt` (consumes `BuildConfig.TMDB_API_KEY`)
2. 🛑 `core/src/main/java/com/euthopiar/core/js/host/HostFunctions.kt` (QuickJS bridge runtime, exposes `__get_freekz_key_token()`)
3. 🛑 `core/src/main/java/com/euthopiar/core/subtitle/external/SubDlClient.kt` (consumes `BuildConfig.SUBDL_API_KEY`)
4. 🛑 `core/src/main/java/com/euthopiar/core/sync/simkl/SimklClient.kt` (Simkl Auth V2 + PKCE flow; redirect URI: `Euthopiar://auth/simkl`)
5. 🛑 `core/src/main/java/com/euthopiar/core/sync/trakt/TraktClient.kt` (consumes `BuildConfig.TRAKT_CLIENT_ID` / `SECRET`)

### Rules for Protected Files:
- **NEVER** re-hardcode plaintext keys or tokens into these files.
- **NEVER** edit, delete, or mock the `BuildConfig` bridge mappings without explicit user instruction.
- **Reference Code Hygiene:** `Refrence Nuviou/` is for local developer reading only and must remain untracked and gitignored.
- **Operational Guide:** For full step-by-step instructions on Mode A (Emulator), Mode B (Local Wi-Fi), and the 4 everyday developer situations, see `docs/PIPELINE_WORKFLOW_GUIDE.md`.

---

## 🚫 3. CODE INTEGRITY & PACKAGING RULES

- **DO NOT MOVE OR RENAME KOTLIN PACKAGES OR SOURCE FILES:**
  The project currently builds successfully (`BUILD SUCCESSFUL`). Moving `.kt` files or renaming packages breaks import hierarchies, reflection lookups, and Gradle compilation.
- **NO MASS UNPROMPTED REFACTORING:**
  Do not rewrite working components to "modernize" them unless requested. Keep changes minimal, targeted, and safe.
- **DO NOT BREAK EXISTING TESTS:**
  Never comment out, weaken, or delete assertions in existing test files to hide build issues.

---

## 🧹 4. ROOT DIRECTORY HYGIENE RULES

- **NO JUNK IN THE ROOT FOLDER:**
  Never leave temporary scripts (`test.py`, `temp.js`), generated screenshots (`*.png`), build artifacts (`*.apk`, `*.zip`), or debug logs (`*.log`) in `C:\Euthopiar\`.
- **USE `scratch/` FOR EXPERIMENTS:**
  If you must generate temporary verification scripts, write them inside `C:\Euthopiar\scratch\`, and delete them when the verification is done.
- **NEVER COMMIT CREDENTIALS OR LOCAL PROPERTIES:**
  Ensure `local.properties`, keystore files (`*.jks`, `*.keystore`), and personal credentials are never tracked by Git.

---

## 🩺 5. VERIFICATION MANDATE

Before completing any task or declaring success:
1. Run `./gradlew.bat assembleDebug` in `C:\Euthopiar`.
2. Confirm that the output terminates with:
   ```text
   BUILD SUCCESSFUL
   ```
3. If the build fails, fix your changes immediately before reporting.

---

## 🎬 6. STREAMING ENGINE & SCRAPER ARCHITECTURE RULES

### Scraper Invariants (QuickJS Plugins)
- **Ad Redirector Blacklist**: When scraping gateways/lockers (GDFlix, HubCloud, LinksMod, etc.), NEVER extract links solely based on button text (`download`, `instant`, `fast cloud`). Always verify that `href` does not match `/busycdn|rtmark|redirect|advert|ad\.|shortlink|clk/i` and strictly matches valid media (`.mkv`, `.mp4`, `.m3u8`) or verified lockers (`r2.dev`, `pixeldrain`, `googleusercontent`, `workers.dev`, `gofile.io`). Ad redirectors return HTML pages that crash ExoPlayer with `UnrecognizedInputFormatException`.
- **Metadata Fallbacks**: Stream items must never have `serverName` or `displayName` as `undefined`. Always assign fallbacks: `item.serverName || item.displayName || item.title || 'Direct Server'`.
- **Defunct Worker Filtering**: Always filter out known defunct Cloudflare Worker subdomains (e.g. `icy-king-205d`, `download.goldmines`) that return HTTP 403 Forbidden.
- **Manual Redirects on Gateways (`redirect: 'manual'` Rule)**: Always probe redirect gateways (`pixel.hubcloud`, `fast-dl.one`, `driveseed`) with `redirect: 'manual'` to read the `Location` header before returning URLs to ExoPlayer. Never call `await res.text()` with default `follow` on binary video streams (2GB Binary Redirect RAM trap).
- **Pre-Signed Cloud Header Rules**: Never attach foreign `Origin`, `Referer`, or `Authorization` headers to pre-signed S3, R2, or Google CDN links.

### Progressive Cloud Streaming Pipeline
- **HTTP Range Seeking Acceleration**: In `SequentialStreamSession` and `SequentialStreamDataSource`, whenever `acceptsRanges` is true, forward seek requests outside the buffered region must immediately dispatch an HTTP Range request (`Range: bytes=<target>-`) rather than blocking on linear sequential bridging.
- **Disk Chunk Caching**: Maintain chunked local disk persistence (`chunk_*.bin`) and EBML cluster indexing so local re-reads, backwards seeking, and repeat playback hit disk cache with zero network overhead.

### Stream Testing Protocol
- When testing stream sources, disable Smart Failover to prevent automatic fallback. Collect data across multiple pieces of content before applying code changes. Respect the 3-second player HUD auto-hide window.

