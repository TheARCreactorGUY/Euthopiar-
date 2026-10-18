# Nuvio Architecture Reference: Settings & Profiles Subsystem

This document specifies the complete **Profiles Subsystem** (`ProfileSelectionScreen`, `ProfileEditScreen`, PIN security) and the entire **Settings Hierarchy** (Root, Playback, Streams, Debrid, Appearance, Homescreen, MetaScreen, ContinueWatching, Tracking, Addons/Plugins, Notifications, Advanced).

---

## 1. Profile Management Subsystem

### 1.1 ProfileSelectionScreen (`AppRoute.ProfileSelection`)

#### Purpose and Entry Points
- **Purpose**: Displays the profile selection carousel upon app startup (if multiple profiles exist and auto-login is disabled) or via Settings -> Switch Profile. Manages profile switching, PIN entry gating, and profile management mode.
- **Entry Points**: 
  - `AppGate.kt`: Displayed if `activeProfileId == null` or startup lock is active.
  - Settings Root -> "Switch Profile" action.
  - Navigation route: `AppRoute.ProfileSelection(onSelectedRoute)`.
- **Compose Equivalent**: Jetpack Compose fullscreen `Box` with horizontal scrollable Row of avatar circles, animated PIN entry bottom sheet dialog.

#### Files Involved
- UI:
  - [ProfileSelectionScreen.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSelectionScreen.kt#L1-L560)
  - [PinEntryDialog.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/PinEntryDialog.kt#L1-L240)
  - [ProfileSwitcherTab.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSwitcherTab.kt#L1-L150)
- Repository & Storage:
  - [ProfileRepository.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileRepository.kt#L1-L450)
  - [ProfileStorage.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileStorage.kt#L1-L180)
  - [ProfilePinCrypto.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfilePinCrypto.kt#L1-L90)
- Models:
  - [ProfileModels.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileModels.kt#L1-L120)

#### Component Tree & Layout
```
BoxWithConstraints (fillMaxSize, background = Color.Black)
├── AsyncImage (optional profile background artwork, ContentScale.Crop, alpha = 0.25f)
├── Column (verticalScroll)
│   ├── Spacer (statusBars padding)
│   ├── Top Bar Row:
│   │   ├── Back Button (visible if onBack != null)
│   │   ├── Spacer(weight = 1f)
│   │   └── Manage Profiles Button (Text / Edit icon: toggles isEditMode)
│   ├── MemberBrandWordmark ("Nuvio" animated header)
│   ├── Text ("Who's Watching?", headlineMedium, Bold, titleAlpha & titleOffset)
│   ├── Spacer(32.dp)
│   ├── Row (horizontalScroll, Arrangement.Center, fillMaxWidth)
│   │   ├── For each profile in profileState.profiles:
│   │   │   └── ProfileAvatarItem
│   │   │       ├── Box (size = 100.dp, CircleShape, border = 2.dp if active)
│   │   │       │   ├── AsyncImage (avatarUrl) / Initials fallback
│   │   │       │   ├── [If hasPin] Lock Icon badge (Icons.Rounded.Lock)
│   │   │       │   └── [If isEditMode] Edit Pencil overlay (Icons.Rounded.Edit)
│   │   │       ├── Spacer(8.dp)
│   │   │       └── Text (profile.name, bodyLarge, maxLines = 1)
│   │   └── [If profiles.size < 6] AddProfileButton
│   │       └── Circle dashed container + Icons.Rounded.Add ("Add Profile")
│   └── Spacer(48.dp)
└── [When pinDialogProfile != null] PinEntryDialog (modal PIN pad)
```

#### PIN Security & Hashing
- `ProfilePinCrypto.kt`:
  - PINs are 4 digits (`^[0-9]{4}$`).
  - Hashing: SHA-256 with profile UUID as salt (`hashPin(pin, profile.id)`).
  - Stored in `ProfilePinCacheStorage.kt` (AES-256 GCM encrypted SharedPreferences on Android, Keychain on iOS).
  - Temporary session unlock cache (`ProfilePinCache.kt`) keeps PIN verified for 15 minutes of inactivity before re-prompting.

---

### 1.2 ProfileEditScreen (`AppRoute.ProfileEdit`)

#### Purpose and Entry Points
- **Purpose**: Creates a new profile or modifies an existing profile's name, avatar image, background cosmetic theme, separate addon workspace flag (`usesPrimaryAddons`), and 4-digit security PIN.
- **Entry Points**: `ProfileSelectionScreen` (tapping "Add Profile" or pencil icon in edit mode).
- **Files Involved**:
  - [ProfileEditScreen.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileEditScreen.kt#L1-L598)
  - [AvatarPicker.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/AvatarPicker.kt#L1-L180)
  - [ProfileBackgroundPicker.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileBackgroundPicker.kt#L1-L140)

#### State Fields and Configuration
| Setting / Field | Type | Default | Storage / Target | Description |
|---|---|---|---|---|
| `name` | `String` | `""` | `NuvioProfile.name` | Display name for the profile |
| `avatarId` | `String?`| Preset 1 | `NuvioProfile.avatarId` | Selected CDN avatar ID from catalog |
| `avatarUrl` | `String?`| `""` | `NuvioProfile.avatarUrl`| Custom avatar image URL |
| `profileBackgroundId` | `String?`| `null` | `NuvioProfile.profileBackgroundId` | Cosmetic backdrop theme ID (Supporter perk) |
| `usesPrimaryAddons` | `Boolean` | `false` | `NuvioProfile.usesPrimaryAddons` | If true, shares main profile's Stremio addons; if false, profile has isolated addon storage |
| `hasPin` | `Boolean` | `false` | `ProfilePinCache` | Whether a 4-digit lock PIN is enforced |

---

## 2. Settings Hierarchy (`SettingsScreen.kt`)

Nuvio implements a unified hierarchical settings navigator structured into categories:

```
SettingsScreen (SettingsPage.Root)
├── Category: Account
│   ├── Account (Profile switcher, Supabase cloud sync status, sign out)
│   └── Tracking / TraktAuthentication (Trakt, Simkl, MDBList OAuth & auto-scrobble)
│
├── Category: General
│   ├── Playback (Engine choice, hardware decoding, buffer sizes, language tracks, auto-skip)
│   ├── Appearance
│   │   ├── Theme (Dark mode, AMOLED, accent color, haze blur, kmpalette)
│   │   ├── Homescreen (Catalog order, hide unreleased, hero carousel styles)
│   │   ├── MetaScreen (Backdrop blur, actor cast, Trakt comments, stream sort)
│   │   ├── ContinueWatching (Threshold %, auto-delete finished items)
│   │   ├── Streams (Scraper timeouts, thread pool limits, TorrServer loopback)
│   │   └── PosterCustomization (Custom poster URL templates)
│   ├── ContentDiscovery
│   │   ├── Addons (Stremio manifest URLs, toggle active status, install from URL)
│   │   ├── Plugins (QuickJS scraper plugins, repo sync, JS permissions)
│   │   └── MediaServers (Jellyfin, Emby, Plex remote server connections)
│   ├── Integrations
│   │   ├── Debrid (Real-Debrid, Premiumize, TorBox, AllDebrid API keys)
│   │   ├── TmdbEnrichment (TMDB API key, fallback metadata, artwork language)
│   │   └── MdbListRatings (MDBList API key, IMDb/Metacritic/RT badges)
│   └── Notifications (Episode release notifications, background sync worker)
│
├── Category: Advanced
│   └── Advanced (Sentry crash reporting, Kermit log level, clear cache, export config)
│
└── Category: About
    ├── SupportersContributors (Patron wall, supporter perks)
    ├── LicensesAttributions (Open-source software licenses)
    └── CheckForUpdates (GitHub release OTA self-updater)
```

---

## 3. Comprehensive Settings Specification Table

Every setting in Nuvio, its key, default value, storage repository, and runtime effect:

| Setting Category | Setting Key / Symbol | Type | Default Value | Storage Repository | Runtime Effect |
|---|---|---|---|---|---|
| **Playback** | `playbackEngine` | `AndroidPlaybackEngine` | `EXOPLAYER` | `PlayerSettingsRepository` | Toggles between Google Media3 ExoPlayer and libmpv engine (`AndroidLibmpvVideoOutput`). |
| **Playback** | `hardwareAcceleration` | `Boolean` | `true` | `PlayerSettingsRepository` | Configures `MediaCodecSelector` hardware vs software decoders in ExoPlayer / `hwdec=auto` in MPV. |
| **Playback** | `audioPassthrough` | `Boolean` | `false` | `PlayerSettingsRepository` | Enables HDMI/optical Bitstream passthrough for Dolby Atmos/DTS-HD MA. |
| **Playback** | `bufferForPlaybackMs` | `Int` | `2500` (2.5s) | `PlayerSettingsRepository` | ExoPlayer `DefaultLoadControl.minBufferMs` required before starting initial playback. |
| **Playback** | `maxBufferMs` | `Int` | `50000` (50s) | `PlayerSettingsRepository` | ExoPlayer `DefaultLoadControl.maxBufferMs` cap on cached memory buffer. |
| **Playback** | `doubleTapSeekSeconds`| `Int` | `10` | `PlayerSettingsRepository` | Duration in seconds skipped on double-tap left/right edge gestures. |
| **Playback** | `autoPlayNextEpisode`| `Boolean` | `true` | `PlayerSettingsRepository` | Automatically transitions to S(x)E(x+1) when remaining time < 10 seconds. |
| **Playback** | `autoSkipIntro` | `Boolean` | `true` | `PlayerSettingsRepository` | Auto-seeks past intro chapter markers when detected in stream metadata. |
| **Playback** | `autoSkipOutro` | `Boolean` | `false` | `PlayerSettingsRepository` | Auto-seeks to next episode when credits chapter marker is reached. |
| **Playback** | `preferredAudioLanguage`| `String` | `"en"` | `PlayerSettingsRepository` | Default audio track selection rule (`TrackSelectionParameters.setPreferredAudioLanguage`). |
| **Playback** | `preferredSubtitleLanguage`|`String` | `"en"` | `PlayerSettingsRepository` | Default subtitle track selection rule (`setPreferredTextLanguage`). |
| **Playback** | `subtitleTextSize` | `Int` | `20` (sp) | `PlayerSettingsRepository` | Scaled font size passed to `SubtitleView` / `libass.sub-font-size`. |
| **Streams** | `maxConcurrentScrapers`| `Int` | `10` | `StreamsSettingsRepository` | Thread pool concurrency limit for QuickJS scraper execution. |
| **Streams** | `scraperTimeoutSeconds`| `Int` | `60` | `StreamsSettingsRepository` | Timeout duration for JS provider execution before coroutine cancellation. |
| **Streams** | `p2pTorrServerEnabled`| `Boolean` | `true` (full flavor) | `StreamsSettingsRepository` | Enables TorrServer loopback proxy engine for raw torrent hash resolution. |
| **Streams** | `torrServerPort` | `Int` | `8090` | `StreamsSettingsRepository` | Local TCP port for loopback streaming (`127.0.0.1:8090`). |
| **Streams** | `streamFilterQuality` | `Set<String>` | `["4K", "1080p", "720p"]` | `StreamsSettingsRepository` | Auto-filter list removing unwanted resolutions from the stream picker. |
| **Debrid** | `realDebridApiKey` | `String` | `""` | `DebridSettingsRepository` | API token for `api.real-debrid.com/rest/1.0/`. |
| **Debrid** | `torboxApiKey` | `String` | `""` | `DebridSettingsRepository` | API token for `api.torbox.app/v1/api/`. |
| **Debrid** | `premiumizeApiKey` | `String` | `""` | `DebridSettingsRepository` | API token for `www.premiumize.me/api/`. |
| **Debrid** | `allDebridApiKey` | `String` | `""` | `DebridSettingsRepository` | API token for `api.alldebrid.com/v4/`. |
| **Debrid** | `checkAvailabilityBeforePlayback`| `Boolean`| `true` | `DebridSettingsRepository` | Pre-checks torrent instant cache status before presenting streams. |
| **Debrid** | `cloudLibraryEnabled` | `Boolean` | `true` | `DebridSettingsRepository` | Surfaces Debrid cloud storage folder inside the Library screen. |
| **Appearance** | `themeMode` | `AppThemeMode` | `SYSTEM` | `ThemeSettingsRepository` | `LIGHT`, `DARK`, `SYSTEM`, or `AMOLED` (pure black `#000000` background). |
| **Appearance** | `accentColor` | `Color` | `#7C4DFF` (Violet) | `ThemeSettingsRepository` | Seed color passed to `dynamicColorScheme` / custom Material 3 palette. |
| **Appearance** | `enableHazeBlur` | `Boolean` | `true` | `ThemeSettingsRepository` | Toggles runtime glassmorphism blur effects (`dev.chrisbanes.haze:haze`). |
| **Appearance** | `enableAmbientGlow`| `Boolean` | `true` | `ThemeSettingsRepository` | Uses `kmpalette` to tint background headers using poster colors. |
| **Homescreen** | `hideUnreleasedContent`| `Boolean`| `true` | `HomeCatalogSettingsRepository`| Filters out catalog movies/episodes whose release timestamp > current date. |
| **Homescreen** | `heroCarouselStyle` | `CarouselStyle`| `EXPANDED` | `HomeCatalogSettingsRepository`| Display style of top banner (`COMPACT`, `EXPANDED`, `CARD`). |
| **Homescreen** | `enabledRows` | `List<String>`| All default | `HomeCatalogSettingsRepository`| Controls visibility and display order of Stremio catalog rows. |
| **MetaScreen** | `showBackdropBlur` | `Boolean` | `true` | `MetaScreenSettingsRepository`| Toggles heavy blur under title/overview in detail view. |
| **MetaScreen** | `showCast` | `Boolean` | `true` | `MetaScreenSettingsRepository`| Displays TMDB actors/crew horizontal row. |
| **MetaScreen** | `showTraktComments` | `Boolean` | `true` | `MetaScreenSettingsRepository`| Displays user reviews from Trakt API. |
| **ContinueWatching**| `resumeThresholdPercent`| `Float`| `0.05f` (5%) | `ContinueWatchingPreferencesRepository`| Minimum playback percentage required to generate a Continue Watching card. |
| **ContinueWatching**| `completionThresholdPercent`|`Float`| `0.85f` (85%)| `ContinueWatchingPreferencesRepository`| Playback percentage at which media is marked watched and removed from card list. |
| **ContinueWatching**| `autoRemoveFinished`| `Boolean` | `true` | `ContinueWatchingPreferencesRepository`| Automatically evicts items when playback crosses completion threshold. |
| **Tracking** | `traktAccessToken` | `String?` | `null` | `TraktAuthRepository` | OAuth 2.0 bearer token for Trakt scrobbling & watchlist sync. |
| **Tracking** | `simklAccessToken` | `String?` | `null` | `SimklAuthRepository` | OAuth 2.0 bearer token for Simkl scrobbling & history sync. |
| **Tracking** | `scrobbleThreshold` | `Float` | `0.80f` (80%) | `TrackingSettingsRepository` | Video watch progress threshold before dispatching scrobble API call. |
| **Metadata** | `tmdbApiKey` | `String` | Built-in | `TmdbSettingsRepository` | API key for TMDB metadata enrichment and cast details. |
| **Metadata** | `mdbListApiKey` | `String` | `""` | `MdbListSettingsRepository` | API key for IMDb, Rotten Tomatoes, and Metacritic rating scores. |
| **Notifications** | `notifyNewEpisodes` | `Boolean` | `true` | `EpisodeReleaseNotificationsRepository` | AndroidX WorkManager task schedules checks for bookmarked series air dates. |
| **Advanced** | `sentryCrashReporting`| `Boolean` | `true` | `SentrySettingsRepository` | Enables Sentry crash breadcrumbs and unhandled exception logging. |
| **Advanced** | `kermitLogLevel` | `Severity` | `INFO` | `LoggerSettingsRepository` | Kermit log filter (`VERBOSE`, `DEBUG`, `INFO`, `WARN`, `ERROR`). |
| **Advanced** | `updateChannel` | `UpdateChannel`| `STABLE` | `UpdateChannelSettingsRepository`| Release channel for in-app OTA APK updater (`STABLE` vs `BETA`). |

---

## 4. Sub-Settings Detail Specifications

### 4.1 PlaybackSettingsPage (`SettingsPage.Playback`)
- **Engine Selection**: Toggle button for `ExoPlayer` (default, custom decoders) vs `libmpv` (desktop mpv engine ported to Android).
- **Buffer Sliders**: Two Material 3 `Slider` controls configuring initial playback buffer (0.5s to 10s) and max memory buffer (10s to 120s).
- **Audio/Subtitle Pickers**: Dropdown menus for default language tags conforming to ISO 639-1 (`"en"`, `"es"`, `"fr"`, `"ja"`, `"de"`).

### 4.2 AddonsSettingsPage (`SettingsPage.Addons`)
- **Addon List**: Reorderable list of installed Stremio manifests (`ManagedAddon`).
- **Toggle Switch**: Enables or disables an addon without uninstalling it.
- **Install from URL**: Dialog with `NuvioInputField` accepting Stremio manifest URLs (`https://.../manifest.json` or `stremio://...`).
- **Sync All**: Action button triggering `AddonRepository.refreshAll()` which queries each manifest endpoint concurrently.

### 4.3 PluginsSettingsPage (`SettingsPage.Plugins`)
- **Scraper Repository**: Displays loaded QuickJS scrapers with version, author, and enabled toggle.
- **Plugin Sandbox Diagnostics**: Provides memory usage and script execution logs for debugging third-party scraper scripts.

### 4.4 DebridSettingsPage (`SettingsPage.Debrid`)
- **Debrid Provider Cards**: Real-Debrid, Premiumize, TorBox, AllDebrid, Debrid-Link.
- **Account Verification**: Tapping "Verify Key" executes a lightweight user account query (`/user` on Real-Debrid, `/me` on TorBox) displaying account subscription status and expiration timestamp.
- **Cloud Library Switch**: Enables browsing unrestrict downloads as a virtual disk in the Library tab.
