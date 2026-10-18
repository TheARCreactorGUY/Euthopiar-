# 01. Stack, Build System & Project Structure

## 1. Architectural Overview & Technology Stack

Nuvio is built as a **Kotlin Multiplatform (KMP)** and **Compose Multiplatform** application targeting **Android** (API 24 to 36+) and **iOS** (iOS 15+). The codebase leverages modern declarative UI across platforms while using platform-specific native media player engines, background services, and native libraries for heavy lifting.

### 1.1 Core Platform & Language Versions
Ground truth citations: (`gradle/libs.versions.toml:1-37`, `androidApp/build.gradle.kts:52-76`, `composeApp/build.gradle.kts:341-356`)

| Component | Version | Source Citation | Notes |
| :--- | :--- | :--- | :--- |
| **Kotlin** | `2.4.10` | `gradle/libs.versions.toml:21` | Kotlin 2.x compiler with Compose compiler plugin |
| **Android Gradle Plugin (AGP)** | `9.2.0` | `gradle/libs.versions.toml:2` | Latest generation Android build tooling |
| **Compose Multiplatform** | `1.12.0` | `gradle/libs.versions.toml:17` | JetBrains Compose Multiplatform |
| **Compose Material 3** | `1.12.0-alpha03` | `gradle/libs.versions.toml:27` | Material 3 design system with dynamic theming |
| **Navigation 3 UI** | `1.2.0-alpha02` | `gradle/libs.versions.toml:8` | `org.jetbrains.androidx.navigation3:navigation3-ui` |
| **Java Target** | `Java 11` | `androidApp/build.gradle.kts:141-142` | JVM 11 target bytecode with desugaring |
| **Core Desugaring** | `2.1.5` | `gradle/libs.versions.toml:34, libs.desugarJdkLibs` | Enables Java 8+ time & streams on Android 24+ |
| **Min SDK** | `24` (Android 7.0) | `gradle/libs.versions.toml:5`, `androidApp/build.gradle.kts:70` | Broad device coverage down to Android Nougat |
| **Target SDK** | `36` (Android 15) | `gradle/libs.versions.toml:6`, `androidApp/build.gradle.kts:71` | Modern foreground service & storage compliance |
| **Compile SDK** | `37` (Minor: `0`) | `gradle/libs.versions.toml:3-4`, `androidApp/build.gradle.kts:54-55` | Future-proofed compilation environment |
| **Version Name / Code** | `0.5.9` (Code: `141`)| `iosApp/Configuration/Version.xcconfig:1-2` | Shared across Android & iOS builds |

---

## 2. Dependency Inventory & Version Catalog

### 2.1 Networking & Communication
- **Ktor Client (`3.4.1`)** (`gradle/libs.versions.toml:26`): Multiplatform HTTP engine. Uses `ktor-client-okhttp` on Android (`composeApp/build.gradle.kts:452`) and `ktor-client-darwin` on iOS (`composeApp/build.gradle.kts:400`).
- **OkHttp (`4.12.0`)** (`composeApp/build.gradle.kts:449`): Underlying HTTP transport for ExoPlayer data sources and network operations.
- **Supabase Kotlin SDK (`3.4.1`)** (`gradle/libs.versions.toml:30`):
  - `postgrest-kt`: Profile sync, cloud collections, watched tracking.
  - `auth-kt`: User account management and authentication sessions.
  - `functions-kt`: Edge functions invocation.
  - `storage-kt`: Remote storage access.

### 2.2 Coroutines, Serialization & State
- **Kotlinx Coroutines (`1.10.2`)** (`gradle/libs.versions.toml:22`): Structured concurrency (`Dispatchers.IO`, `Dispatchers.Default`, `Dispatchers.Main`).
- **Kotlinx Serialization JSON (`1.10.0`)** (`gradle/libs.versions.toml:23`): High-speed polymorphic JSON parsing for Stremio manifests, TMDB, Trakt, and settings.
- **Kotlinx AtomicFU (`0.32.1`)** (`gradle/libs.versions.toml:24`): Lock-free atomic primitives across KMP runtimes.
- **AndroidX Lifecycle & ViewModel (`2.11.0`)** (`gradle/libs.versions.toml:13,50-51`): `lifecycle-viewmodel-compose` and `lifecycle-runtime-compose`.
- **AndroidX SavedState (`1.4.0`)** (`gradle/libs.versions.toml:14,52-53`): Process death state restoration.

### 2.3 Image Loading & Dynamic Theming
- **Coil 3 (`3.6.3`)** (`gradle/libs.versions.toml:18,62-66`):
  - `coil-compose`: Compose image loading components.
  - `coil-network-ktor3`: Ktor-backed image fetcher.
  - `coil-network-cache-control`: HTTP Cache-Control header support.
  - `coil-svg`: Vector SVG rendering.
  - `coil-gif`: Animated GIF playback (`composeApp/build.gradle.kts:447`).
- **KMColor / KMPalette (`4.0.0-beta02`)** (`gradle/libs.versions.toml:25,70`): Extracts dynamic dominant color palettes from movie posters for ambient UI background glow.
- **Haze (`1.7.2`)** (`composeApp/build.gradle.kts:501`): High-performance glassmorphism/frosted glass blur effects in Compose.
- **Reorderable (`3.0.0`)** (`gradle/libs.versions.toml:33,94`): Drag-and-drop reordering for addon lists and home catalogs.

### 2.4 HTML Parsing & JavaScript Engine
- **Ksoup (`0.2.6`)** (`gradle/libs.versions.toml:32,93`): Multiplatform HTML DOM scraping library.
- **QuickJS-kt (`1.0.15` / `1.0.5-nuvio`)** (`gradle/libs.versions.toml:31,92`, `composeApp/libs/quickjs-kt-android-1.0.5-nuvio.aar`): Embedded JavaScript execution engine used to run scraper plugins in a sandboxed runtime.

### 2.5 Media Playback Engines (Android)
Ground truth citation: (`composeApp/build.gradle.kts:454-470, 550-558`)

The project explicitly **excludes** standard Maven `androidx.media3` dependencies (`composeApp/build.gradle.kts:550-558`) and replaces them with **custom prebuilt AARs** located in `composeApp/libs/`:
1. `lib-exoplayer-release.aar`: Core ExoPlayer engine.
2. `lib-decoder-ffmpeg-release.aar` (9.2 MB): Custom FFmpeg audio/video decoder extension, supporting AC3, E-AC3, TrueHD, DTS, DTS-HD, and custom video formats.
3. `lib-decoder-av1-release.aar` (2.9 MB): Custom libgav1/dav1d software decoder for AV1 video streams.
4. `lib-decoder-mpegh-release.aar` (1.3 MB): MPEG-H 3D audio decoder.
5. `lib-exoplayer-hls-release.aar`: HLS streaming support.
6. `lib-extractor-release.aar`: Container extractors (MKV, MP4, TS, FLV, AVI).
7. `lib-datasource-okhttp-release.aar`: OkHttp-powered data source with custom HTTP headers, cookies, and connection pooling.
8. `lib-datasource-release.aar`: Base data source abstractions.
9. `lib-common-release.aar`: Media3 common interfaces.
10. `lib-ui-release.aar`: AndroidX Media3 UI components and SurfaceViews.
11. `lib-nuvio-engine-android-0.1.4.aar` (16.3 MB): Custom C++ / native streaming engine providing low-level torrent/HTTP demuxing.
12. **MPV Android Lib (`0.1.12`)** (`gradle/libs.versions.toml:29,87`): Embedded `libmpv` video player providing an alternate playback engine for complex anime subtitles and unsupported formats.
13. **Ass-Media (`0.5.1`)** (`composeApp/build.gradle.kts:451`): `io.github.peerless2012:ass-media` integrating native `libass` for pixel-perfect ASS/SSA subtitle rendering over ExoPlayer.

---

## 3. Product Flavors & Target Distribution

Nuvio defines two product flavors in `androidApp/build.gradle.kts:77-85`:

| Flavor Dimension | Flavor | Source Sets | Purpose & Capabilities |
| :--- | :--- | :--- | :--- |
| `distribution` | **`full`** | `composeApp/src/fullCommonMain`<br>`composeApp/src/androidFull`<br>`androidApp/src/full` | **The Complete Streaming App**: Full addon support, JS scraper plugin engine (`QuickJS`), torrent P2P client / TorrServer integration, in-app GitHub APK updater, YouTube trailer direct stream extraction. |
| `distribution` | **`playstore`** | `composeApp/src/androidPlaystore`<br>`androidApp/src/playstore` | **Store Compliant Build**: Stubs out scraper plugins, disables torrent streaming, disables external APK downloading, restricts features to compliant catalog viewing and Debrid/HTTP playback. |

### 3.1 ABI Splits & Native Binaries
`androidApp/build.gradle.kts:112-119` configures ABI splits for release builds:
- `armeabi-v7a` (32-bit ARM)
- `arm64-v8a` (64-bit ARM)
- `x86` (32-bit Intel)
- `x86_64` (64-bit Intel/AMD)

Universal APK is disabled (`isUniversalApk = false`) to keep download sizes small. `pickFirsts` (`androidApp/build.gradle.kts:98-104`) resolves native shared library collisions for:
- `libc++_shared.so`
- `libavcodec.so`
- `libavutil.so`
- `libswscale.so`
- `libswresample.so`

---

## 4. Android Manifest & System Permissions

### 4.1 Declared Permissions
Ground truth citation: (`androidApp/src/main/AndroidManifest.xml:4-10`, `androidApp/src/full/AndroidManifest.xml:4`, `composeApp/src/androidMain/AndroidManifest.xml:4-7`)

```xml
<!-- Network Access -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Foreground Services (Android 14+) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- Background Work & Transfers -->
<uses-permission android:name="android.permission.RUN_USER_INITIATED_JOBS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Full Flavor Only: Self-Updating APK Installation -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

### 4.2 Application Configuration
(`androidApp/src/main/AndroidManifest.xml:11-23`)
- `android:name="com.nuvio.app.NuvioApplication"`: Custom application class initializing logging, crash reporting, and services.
- `android:largeHeap="true"`: Grants higher memory headroom (crucial for 4K video buffering, libass font rendering, and multi-stream scraper operations).
- `android:usesCleartextTraffic="true"`: Permitted globally to allow streaming from local servers (`http://127.0.0.1:8090` TorrServer) and unencrypted HTTP CDNs.
- `android:networkSecurityConfig="@xml/network_security_config"`: Custom trust anchors bundling ISRG Root X1 & X2 certificates for legacy Android OS compatibility.

### 4.3 Exported Activities & Dynamic Launcher Icons
Nuvio supports 6 dynamic launcher icon activity-aliases via separate activity declarations (`androidApp/src/main/AndroidManifest.xml:74-163`):
1. `AppIconDefault` (enabled by default)
2. `AppIconArcticBlue`
3. `AppIconEmerald`
4. `AppIconRoseGold`
5. `AppIconCopper`
6. `AppIconGraphite`

Each activity alias points to splash themes and can be dynamically enabled/disabled via `PackageManager.setComponentEnabledSetting` (`androidApp/src/main/kotlin/com/nuvio/app/launcher/AppIconActivities.kt:1-15`).

### 4.4 Registered Services & Receivers
(`androidApp/src/main/AndroidManifest.xml:164-186`, `composeApp/src/androidMain/AndroidManifest.xml:10-18`)
- **`PlayerNowPlayingService`**: Foreground service (`foregroundServiceType="mediaPlayback"`) managing lock screen media controls, notification playback state, and background audio streaming.
- **`PlayerNowPlayingActionReceiver`**: Broadcast receiver handling media notification actions (Play, Pause, Seek Forward, Seek Backward, Stop).
- **`DownloadsNotificationActionReceiver`**: Broadcast receiver managing background download actions (Pause, Resume, Cancel).
- **`DownloadsTransferJobService`**: JobService bound to `RUN_USER_INITIATED_JOBS` for reliable background downloading on Android 14+.
- **`SystemForegroundService`** (`androidx.work`): Merged with `foregroundServiceType="dataSync"` for background syncing tasks.
- **`FileProvider`**: Authority `${applicationId}.fileprovider` mapped to `@xml/file_paths` for sharing files and launching APK installers.

---

## 5. Compose Equivalents for Euthopiar

Since Euthopiar is built using pure Android Kotlin + Jetpack Compose (not KMP), the table below specifies how Nuvio's KMP architecture maps directly into standard Jetpack Compose:

| Nuvio Multiplatform Mechanism | Euthopiar Compose Equivalent |
| :--- | :--- |
| `org.jetbrains.compose.material3.*` | `androidx.compose.material3.*` (exact 1:1 drop-in) |
| `org.jetbrains.androidx.navigation3:navigation3-ui` | `androidx.navigation:navigation-compose:2.8.x` or Navigation3 |
| `io.coil-kt.coil3:coil-compose` | `io.coil-kt:coil-compose:2.7.0` or `io.coil-kt.coil3:coil-compose` |
| `co.touchlab:kermit` | `android.util.Log` or `com.jakewharton.timber:timber` |
| `dev.chrisbanes.haze:haze` | `dev.chrisbanes.haze:haze-android:1.7.2` |
| Custom AAR Media3 Stack | `androidx.media3:media3-exoplayer:1.4.x` with OkHttp DataSource + ExoPlayer FFmpeg extension |
| `sh.calvin.reorderable:reorderable` | `sh.calvin.reorderable:reorderable:2.4.x` (pure Compose list reordering) |
