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
- **Open Source Mirror:** `C:\Euthopiar Open Source Release` (linked to GitHub repo `https://github.com/TheARCreactorGUY/Euthopiar-.git`).
- **Safety Backups:** `C:\Euthopiar-System\Backups` (timestamped zip snapshots).
- **Production Builds:** `C:\Euthopiar-System\Production-Releases` (signed/clean release APKs and AABs).
- **Control Center App:** `C:\Euthopiar-System\Control-Center` (desktop visual manager).

---

## 🔒 2. CRITICAL PROTECTED FILES (STRICT DO NOT TOUCH LIST)

The following files contain real, operational API keys and cryptographic bindings:
1. 🛑 `core/src/main/java/com/euthopiar/core/util/TmdbBridge.kt`
2. 🛑 `core/src/main/java/com/euthopiar/core/js/host/HostFunctions.kt`
3. 🛑 `core/src/main/java/com/euthopiar/core/subtitle/external/SubDlClient.kt`
4. 🛑 `core/src/main/java/com/euthopiar/core/sync/simkl/SimklClient.kt`
5. 🛑 `core/src/main/java/com/euthopiar/core/sync/trakt/TraktClient.kt`

### Rules for Protected Files:
- **NEVER** edit, refactor, replace, or "sanitize" the API keys in these files without explicit, written instruction from the user.
- **NEVER** delete or mock these files.
- The user has planned a future migration to Cloudflare Workers for key abstraction. Until that specific session, keep all keys intact and functional.

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
