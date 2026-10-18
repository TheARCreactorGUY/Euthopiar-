# Nuvio Architecture Reference: Cross-Cutting Systems

This document provides a source-grounded specification of Nuvio's cross-cutting architectural subsystems: Theming and Design Tokens, Image Loading and Caching, Local Storage and Cloud Synchronization, Background Services, Notifications, In-App Self-Updater, Diagnostics/Logging, and Build Variants/Feature Flags.

---

## 1. Theming and Design Tokens

### 1.1 Theme Engine (`Theme.kt`, `ThemeColors.kt`, `AppTheme.kt`)
- **Theme Modes**: 
  - `AppTheme.LIGHT`: Light background with dark typography.
  - `AppTheme.DARK`: Default dark theme (`#121517` background).
  - `AppTheme.AMOLED`: Pure black (`#000000`) background designed for OLED displays ([Theme.kt:51](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Theme.kt#L51)).
  - `AppTheme.SYSTEM`: Follows Android system dark mode state via `isSystemInDarkTheme()`.
- **Color Scheme Construction**: Built via Material 3 `darkColorScheme`:
  - `primary`: `palette.secondary` (Accent violet/cyan/emerald).
  - `background`: `if (amoled) Color.Black else palette.background`.
  - `onBackground`: `Color(0xFFF5F7F8)`.
  - `surface`: `palette.backgroundElevated`.
  - `surfaceVariant`: `palette.backgroundCard` (`#1A1F24`).
  - `onSurfaceVariant`: `Color(0xFF969CA3)`.
  - `outline`: `Color(0xFF252A2A)`.
  - `error`: `Color(0xFFE36A8A)`.
- **Glassmorphism Blur**: Implemented via `dev.chrisbanes.haze:haze` library. Applied to the bottom navigation bar and floating player controls (`Modifier.hazeChild(state = hazeState)`).
- **Ambient Poster Glow**: Uses `kmpalette` (`com.kmpalette:kmpalette-core`) to sample dominant and vibrant colors from media posters, tinting the backdrop header and gradient scrims.

### 1.2 Design Tokens (`NuvioTokens.kt`)
- **Spacing Scale**:
  - `Space.none = 0.dp`, `Space.s1 = 1.dp`, `Space.s2 = 2.dp`, `Space.s4 = 4.dp`, `Space.s6 = 6.dp`, `Space.s8 = 8.dp`, `Space.s10 = 10.dp`, `Space.s12 = 12.dp`, `Space.s14 = 14.dp`, `Space.s16 = 16.dp`, `Space.s20 = 20.dp`, `Space.s24 = 24.dp`, `Space.s32 = 32.dp`, `Space.s48 = 48.dp`, `Space.s64 = 64.dp`, `Space.s96 = 96.dp`.
- **Corner Radii**:
  - `Radius.compactCard = 12.dp`, `Radius.card = 24.dp`, `Radius.poster = 12.dp`, `Radius.button = 16.dp`, `Radius.sheet = 24.dp`, `Radius.dialog = 28.dp`, `Radius.chip = 999.dp` (Capsule).
- **Elevation**:
  - `Elevation.none = 0.dp`, `Elevation.raised = 2.dp`, `Elevation.playerControls = 4.dp`, `Elevation.modal = 8.dp`, `Elevation.overlay = 12.dp`.
- **Motion and Durations**:
  - `Duration.instant = 100L`, `Duration.short = 180L`, `Duration.medium = 260L`, `Duration.long = 380L`.
  - Easings: `CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)` (Emphasized Decelerate) and `CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)` (Emphasized Accelerate).

### 1.3 Typography (`JetBrainsSans`)
- Typography across the application is rendered in `JetBrainsSans` using three bundled weights:
  - `Font(Res.font.jetbrains_sans_bold, FontWeight.Bold)`
  - `Font(Res.font.jetbrains_sans_semibold, FontWeight.SemiBold)`
  - `Font(Res.font.jetbrains_sans_regular, FontWeight.Normal)`
- Style definitions:
  - `displayLarge`: 32.sp, LineHeight 38.sp, Bold.
  - `headlineLarge`: 24.sp, LineHeight 30.sp, SemiBold.
  - `titleLarge`: 18.sp, LineHeight 24.sp, SemiBold.
  - `titleMedium`: 16.sp, LineHeight 22.sp, SemiBold.
  - `bodyLarge`: 15.sp, LineHeight 21.sp, Normal.
  - `bodyMedium`: 13.sp, LineHeight 18.sp, Normal.
  - `labelSmall`: 11.sp, LineHeight 14.sp, Medium.

---

## 2. Image Loading and Caching

### 2.1 Coil 3 Architecture
- Initialized in `NuvioApplication.onCreate()` via `SingletonImageLoader.setSafe { newImageLoader(context) }`.
- Memory Cache: Sized to 25% of available JVM heap.
- Disk Cache: Sized to 250 MB located at `context.cacheDir/image_cache`.
- Networking: Custom OkHttp client configured with cross-protocol redirect following (`followRedirects(true)`, `followSslRedirects(true)`) and 30-second read timeouts.

### 2.2 CustomPosterFallbackInterceptor (`CustomPosterFallbackInterceptor.kt`)
- **Problem**: When users configure custom RPDB or custom poster URL patterns (e.g. `https://api.ratingposterdb.com/...`), missing entries or expired API keys cause image loads to fail.
- **Solution**: Interceptor intercepts `ErrorResult` from Coil chain ([CustomPosterFallbackInterceptor.kt:21-32](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/core/poster/CustomPosterFallbackInterceptor.kt#L21-L32)):
  - Reads `memoryCacheKeyExtras[FALLBACK_URL_KEY]`.
  - If custom URL fails with 404 or network timeout, transparently falls back to the original TMDB/Cinemeta poster URL without flickering.

---

## 3. Local Storage and Cloud Synchronization

### 3.1 Local Storage Layout
Nuvio uses a hybrid local storage approach:

1. **Android SharedPreferences**:
   - `nuvio_preferences`: Theme mode, AMOLED flag, accent color, active profile ID.
   - `nuvio_player`: Playback engine (`EXOPLAYER`/`LIBMPV`), hardware acceleration, audio passthrough, buffer sizes, language preferences.
   - `nuvio_debrid`: Debrid provider API keys, auto-unrestrict flags.
   - `nuvio_tracking`: Trakt OAuth tokens, Simkl tokens, scrobble threshold.
   - `nuvio_updater`: Ignored release tag, update channel (`STABLE`/`BETA`).
   - `nuvio_sentry`: Sentry crash reporting toggle.

2. **JSON Storage (`context.filesDir`)**:
   - `library_store.json`: Serialized `List<LibraryItem>` with bookmark timestamps, custom list membership, and ratings.
   - `watched_store.json`: Serialized `StoredWatchedPayload` mapping `itemId -> WatchedEntry` with completed flags.
   - `watch_progress_store.json`: Serialized `WatchProgressStorage` containing position, duration, and last watched timestamps.
   - `collections_store.json`: Serialized user-created collections and folder structures.
   - `addons_store.json`: Serialized list of installed Stremio manifests (`ManagedAddon`).
   - `plugins_store.json`: Serialized list of QuickJS scraper plugins and JS sandbox scripts.
   - `downloads_store.json`: Serialized `List<DownloadItem>` tracking active and completed downloads.

3. **Secure Keystore / SharedPreferences**:
   - `nuvio_pin_cache`: Encrypted with Android Keystore (`MasterKey.DEFAULT_AES256_GCM_SPEC`) storing hashed profile PINs.

### 3.2 Supabase PostgREST Synchronization
When a user signs in with a Nuvio account, local JSON stores synchronize bidirectionally with Supabase PostgREST database:
- **`profiles` table**: UUID, user_id, name, avatar_id, avatar_url, background_id, uses_primary_addons, updated_at.
- **`library_items` table**: UUID, profile_id, meta_id, type, name, poster, list_keys, saved_at.
- **`watched_items` table**: UUID, profile_id, item_id, watched, updated_at.
- **`watch_progress` table**: UUID, profile_id, item_id, position_ms, duration_ms, updated_at.
- **`collections` table**: UUID, profile_id, folder_name, cover_url, item_ids, created_at.
- Conflict Resolution: Last-write-wins based on `updated_at` epoch timestamp. Offline mutations are enqueued in `SyncQueue` and flushed when `NetworkStatusRepository.condition == NetworkCondition.Online`.

---

## 4. Background Services and Foreground Tasks

### 4.1 PlayerNowPlayingService (`PlayerNowPlayingService.android.kt`)
- **Android Component**: Foreground `Service` ([PlayerNowPlayingService.android.kt:23-81](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerNowPlayingService.android.kt#L23-L81)).
- **Notification Channel**: `nuvio_playback` ("Media Playback").
- **Notification ID**: `0x4E55` (`20053`).
- **Media Controls**: Integrates with Android `MediaSessionCompat`:
  - Actions: Play, Pause, Skip Next (Next Episode), Seek Backward 10s, Seek Forward 10s, Stop.
  - Metadata: Displays poster bitmap, title, series title, and playback scrubber bar in Android system shade and lock screen.
- **Lifecycle**: Promoted to foreground via `startForeground()` when playback begins; demoted via `stopForeground(STOP_FOREGROUND_REMOVE)` when player is disposed or stopped.

### 4.2 DownloadsTransferJobService (`AndroidDownloadScheduler.kt`)
- **Android Component**: `JobService` utilizing `RUN_USER_INITIATED_JOBS` on Android 14+ (SDK 34) and `ServiceType.DATA_SYNC`.
- **Foreground Notification**: Displays persistent progress notification showing file name, downloaded MB, total MB, transfer speed in MB/s, and Pause/Cancel actions.
- **Concurrency**: Max 2 simultaneous downloads; additional items queued in `Paused` state until active slot finishes.

### 4.3 Episode Release Notifications Worker
- Managed via AndroidX `WorkManager` (`PeriodicWorkRequestBuilder<EpisodeReleaseSyncWorker>(12, TimeUnit.HOURS)`).
- Runs every 12 hours under `NetworkType.CONNECTED` constraint:
  - Queries Stremio/TMDB metadata for series currently in the user's Library or Continue Watching list.
  - Checks if a new episode release date falls within the last 24 hours.
  - Posts system notification (`channel_id = "episode_notifications"`): "New Episode Available: {Show Title} S{season}E{episode} - {Title}".

---

## 5. In-App Self-Updater (`AndroidAppUpdaterPlatform.kt`)

- **Enabled in Variant**: `androidFull` only (disabled in `androidPlaystore` for Google Play compliance).
- **Check Endpoint**: GitHub Releases API (`api.github.com/repos/.../releases`).
- **Release Channels**:
  - `STABLE`: Filters out prerelease tags.
  - `BETA`: Accepts tags marked as prerelease.
- **Download and Installation Pipeline** ([AndroidAppUpdaterPlatform.kt:71-155](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/androidFull/kotlin/com/nuvio/app/features/updater/AndroidAppUpdaterPlatform.kt#L71-L155)):
  1. Compares semantic version against `BuildConfig.VERSION_NAME`.
  2. Downloads release APK chunk by chunk into `context.cacheDir/updates/{safeName}.apk` via OkHttpClient.
  3. Emits progress percentage via `onProgress(downloaded, total)`.
  4. Generates content URI via `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)`.
  5. Fires install intent:
     ```kotlin
     val intent = Intent(Intent.ACTION_VIEW).apply {
         setDataAndType(contentUri, "application/vnd.android.package-archive")
         flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
     }
     context.startActivity(intent)
     ```

---

## 6. Diagnostics, Logging, and Error Handling

### 6.1 Kermit Structured Logging
- Utilizes `co.touchlab.kermit.Logger`.
- Configured in `NuvioApplication`:
  - Debug builds: Logcat output with colorized tags.
  - Release builds: Log level filtered to `Severity.INFO` or higher; sensitive URLs (Debrid tokens, auth headers) sanitized via regex redaction (`Bearer [REDACTED]`, `token=[REDACTED]`).

### 6.2 Sentry Crash Reporting
- Initialized in `MainActivity.onCreate()` via `SentryAndroid.init(context)`.
- Options:
  - `dsn`: Configured via build config.
  - `tracesSampleRate`: 0.1 (10% sample rate for performance traces).
  - Breadcrumbs: Records route navigation events (`AppRoute`), network error statuses (HTTP 401/403/500), and player engine state changes (`STATE_BUFFERING`, `STATE_ENDED`).
  - Privacy: User email, IP address, and third-party API keys stripped before transport.

### 6.3 Self-Healing Error Conventions
- **401/403 Debrid Expiry**: `tryRefreshCredentialedSourceAfterError` transparently requests a new unrestrict URL from the Debrid API and restarts playback from the last saved millisecond position.
- **Corrupt Cache Resilience**: If a JSON file fails decoding via `kotlinx.serialization`, the loader renames the corrupt file to `.bak` and falls back to an empty in-memory state rather than crashing.

---

## 7. Build Variants and Feature Flags

The application is structured around two Android product flavors: `full` and `playstore`.

| Feature Flag (`AppFeaturePolicy`) | `androidFull` | `androidPlaystore` | Architectural Reason |
|---|---|---|---|
| `pluginsEnabled` | **`true`** | **`false`** | Play Store policies prohibit dynamic JavaScript execution sandboxes downloading remote scrapers. |
| `inAppUpdaterEnabled` | **`true`** | **`false`** | Google Play prohibits self-updating APKs outside the Play Store billing and distribution system. |
| `p2pEnabled` | **`true`** | **`true`** | TorrServer engine bundled for peer-to-peer torrent streams. |
| `trailerPlaybackMode` | **`IN_APP`** | **`EXTERNAL`** | Full plays trailers inline via ExoPlayer; Play Store delegates to YouTube app intent. |
| `heroTrailerPlaybackSupported`| **`true`** | **`false`** | Auto-playing video hero banners disabled in Play Store build. |
| `mediaPlaybackForegroundServiceEnabled`| **`true`** | **`false`** | Background audio service enabled in full build. |
| `downloadForegroundServiceEnabled`| **`true`** | **`false`** | In-app video file downloader restricted in Play Store build. |
| `customServerConnectionsEnabled`| **`true`** | **`false`** | Jellyfin/Emby server connections only exposed in full build. |
| `donationActionsEnabled` | **`true`** | **`false`** | External donations (BuyMeACoffee/Crypto) banned on Play Store. |
