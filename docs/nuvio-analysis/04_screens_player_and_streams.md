# 04. Screen Reference: Streams Resolution & Video Player

This document specifies the UI architecture, state fields, component trees, lifecycle hooks, visual tokens, and gesture systems for Nuvio's stream resolution picker and video player screens.

---

## 1. Stream Loading & Resolution Picker Screen (`StreamsScreen`)

### 1.1 Purpose and Entry Points
- **Purpose**: Gathers, filters, badges, and displays available playback streams from Stremio addons, QuickJS plugins, and Debrid providers. Handles auto-play countdowns, Debrid cache verification, and fallback stream selection.
- **Entry Points**: `StreamRoute(launchId: Long, title: String)`. Triggered from the Details screen "Play" or "Select Source" CTA, or directly from Home's "Continue Watching".

### 1.2 Files Involved
- **UI**: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamsScreen.kt`
- **Sub-Components**:
  - `StreamLoadingScreen.kt` (Full-screen poster loading overlay with status messages)
  - `components/ProviderFilterRow.kt` (Horizontal filter chips for active addons)
  - `components/StreamCard.kt` (Stream candidate item)
  - `components/StreamBadgeChip.kt` (Quality, HDR, Audio, Debrid badges)
- **Repository**: `StreamsRepository.kt` (`StreamsUiState`)
- **Models**: `StreamItem`, `AddonStreamGroup`, `StreamLaunch`

### 1.3 Layout Structure (Top to Bottom)
1. **Background**: Blurred hero backdrop (`AsyncImage` with `Modifier.blur(24.dp)` and dark gradient overlay `Color.Black.copy(alpha = 0.85f)`).
2. **Top Navigation Bar**:
   - `NuvioBackButton` to dismiss stream resolution and return to metadata.
   - Title header: Episode title or Movie name + release year.
   - Loading indicator / Shimmer counter showing active addon completions.
3. **Provider Filter Bar (`ProviderFilterRow`)**: Horizontal scrollable chips allowing the user to filter candidate streams by specific addon (e.g. "All", "Torrentio", "MediaFusion", "Comet", "Peerflix").
4. **Stream Groups List (`LazyColumn`)**:
   - Section headers: Provider addon name + status count (e.g. "Torrentio - 14 streams").
   - **Stream Cards (`StreamCard`)**:
     - Line 1: Stream title, resolution tag (`4K`, `1080p`, `720p`), file size (`💾 4.2 GB`), seeds (`👤 185`).
     - Line 2: Format badges (`Dolby Vision`, `HDR10+`, `HEVC`, `Atmos`, `DTS-HD`).
     - Line 3: Debrid status indicator (`[RD+]`, `[TB+]`, `[AD+]`, `[P2P]`).
     - Line 4: Release group / uploader metadata (e.g. `PSA`, `QxR`, `FLUX`).

### 1.4 State Fields & Mutations
| State Field | Type | Source | Mutator |
| :--- | :--- | :--- | :--- |
| `uiState.groups` | `List<AddonStreamGroup>` | `StreamsRepository.uiState` | Addon and plugin completion signals |
| `uiState.isAnyLoading` | `Boolean` | `StreamsRepository.uiState` | Completion of all provider coroutines |
| `uiState.autoPlayStream`| `StreamItem?` | `StreamsRepository.uiState` | Auto-play policy evaluation |
| `selectedProvider` | `String?` | Local UI state | Tapping provider chip |
| `showLoadingOverlay` | `Boolean` | Local UI state | Auto-play in progress |

### 1.5 User Interactions & Gestures
- **Tap Stream Card**: Immediately resolves Debrid/TorrServer target, builds `PlayerLaunch`, and navigates to `PlayerRoute`.
- **Long-Press Stream Card**: Opens `NuvioModalBottomSheet` with actions: "Copy Stream URL", "Open in External Player" (VLC/MPV), "Download Video" (Background download worker).

---

## 2. Fullscreen Video Player Screen (`PlayerScreen`)

### 2.1 Purpose and Entry Points
- **Purpose**: Fullscreen, hardware-accelerated video playback surface with gesture controls, multi-track audio/subtitle selection, PiP support, lock controls, and episode navigation.
- **Entry Points**: `PlayerRoute(launchId: Long, title: String)`. Enters from `StreamsScreen` (or direct download play).

### 2.2 Files Involved
- **UI**:
  - `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreen.kt`
  - `PlayerScreenContent.kt` & `PlayerScreenRuntimeUi.kt`
  - `PlayerControls.kt` (Play/Pause, Timeline slider, Track buttons)
  - `PlayerGestureOverlay.kt` (Brightness, Volume, Seek, Speed boost gestures)
  - `PlayerSidePanel.kt`, `PlayerSourcesPanel.kt`, `PlayerEpisodesPanel.kt`
  - `SubtitleModal.kt`, `AudioTrackModal.kt`, `SubtitleStylePanel.kt`
  - `PlayerPlaybackOverlays.kt` (Opening skip button, Pause overlay, Next up card)
- **Engine Platform Implementations**:
  - `androidMain/.../PlayerEngine.android.kt` (`ExoPlayerSurface`, `LibmpvPlayerSurface`)
  - `androidMain/.../PlayerPlaybackNetworking.kt` (OkHttp DataSource, Trust-All SSL, IPv4 DNS)
  - `androidMain/.../PlayerPictureInPictureManager.android.kt`
- **State Holders**:
  - `PlayerSettingsRepository.kt` (`PlayerSettingsUiState`)
  - `WatchProgressRepository.kt` (`WatchProgressUiState`)

### 2.3 Layout Structure (Z-Index Stack, Back to Front)
1. **Layer 0 (Video Surface)**: `PlatformPlayerSurface` (`ExoPlayer` SurfaceView or `libmpv` Native Surface).
2. **Layer 1 (Subtitles)**:
   - Built-in `SubtitleView` (for standard SRT/VTT).
   - `AssSubtitleView` (native `libass` overlay for styled ASS/SSA anime subtitles).
3. **Layer 2 (Touch Gesture Interceptor)**: `PlayerGestureOverlay` handling edge swipes, taps, and drag seeks.
4. **Layer 3 (Overlays)**:
   - **Opening Skip Overlay (`OpeningOverlay`)**: Displays "Skip Intro" or "Skip Recap" button when IntroDB timestamps match current position.
   - **Pause Metadata Overlay (`PauseMetadataOverlay`)**: Fades in on pause with title logo, episode title, and synopsis.
   - **Next Episode Auto-Play Card**: Appears in the bottom-right corner when 30s remain in an episode.
5. **Layer 4 (Player Controls HUD - `PlayerControlsShell`)**:
   - **Top Bar**: Back button, Media Title, Episode code (`S1:E4`), Provider Name, Subtitle track icon, Audio track icon, Resize mode toggle (`Fit`, `Zoom`, `Stretch`), Aspect Ratio button, Lock controls button.
   - **Center Cluster**: Rewind 10s (`Replay10`), Play/Pause toggle (large circular button), Fast Forward 10s (`Forward10`).
   - **Bottom Bar**: Current time elapsed, Scrubber timeline slider (`Slider` with buffer indicator), Total duration / Remaining time toggle, Playback speed selector (`0.5x` to `2.0x`), Sources side-panel launcher, Episodes side-panel launcher.
6. **Layer 5 (Modals & Sheets)**:
   - `SubtitleModal`: External and embedded subtitle track list + subtitle offset slider (`-10.0s` to `+10.0s`).
   - `AudioTrackModal`: Multi-channel audio stream picker.
   - `PlayerSourcesPanel`: Right slide-over panel to switch streams without exiting player.
   - `PlayerEpisodesPanel`: Right slide-over panel to select and play another episode.

### 2.4 State Fields & Mutations
| State Field | Type | Source | Mutator |
| :--- | :--- | :--- | :--- |
| `playbackSnapshot.currentPositionMs`| `Long` | ExoPlayer / MPV listener | Playback progress ticks |
| `playbackSnapshot.durationMs` | `Long` | ExoPlayer / MPV listener | Media container metadata |
| `playbackSnapshot.isPlaying` | `Boolean` | ExoPlayer / MPV listener | Play/Pause toggle, buffering |
| `controlsVisible` | `Boolean` | Local UI state | Surface tap, 4s auto-hide timer |
| `playerControlsLocked` | `Boolean` | Local UI state | Lock button toggle |
| `activeSubtitleTrack` | `SubtitleTrack?`| TrackSelector | User selection in Subtitle modal |
| `subtitleDelayMs` | `Int` | Local UI state | Subtitle sync offset adjustments |
| `activeAudioTrack` | `AudioTrack?` | TrackSelector | User selection in Audio modal |
| `resizeMode` | `PlayerResizeMode`| Local UI state | Tapping resize mode button |
| `playbackSpeed` | `Float` | Local UI state | Speed picker or hold-to-speed |

### 2.5 Gesture System Specifications
Ground truth citation: (`PlayerGestureOverlay.kt:100-194`)

1. **Left-Edge Vertical Drag**: Adjusts screen brightness (0.0 to 1.0). Renders vertical brightness bar with percentage pill on the left screen margin.
2. **Right-Edge Vertical Drag**: Adjusts system media volume (0 to 100%). Renders vertical volume bar with percentage pill on the right screen margin.
3. **Horizontal Drag (Across Surface)**: Smooth scrubbing/seeking. Shows time preview pill (e.g. `+0:30 (14:20)`). Commits seek on touch release.
4. **Double-Tap Left Half**: Instantly rewinds playback by 10 seconds (`seekBy(-10_000)`). Triggers ripple animation.
5. **Double-Tap Right Half**: Instantly fast-forwards playback by 10 seconds (`seekBy(+10_000)`). Triggers ripple animation.
6. **Long-Press & Hold Anywhere**: Activates **"Hold to Speed"** (temporarily boosts playback rate to `2.0x`). Releasing finger immediately returns rate to normal `1.0x`.
7. **Single Tap on Surface**: Toggles visibility of Player Controls HUD (`controlsVisible = !controlsVisible`). Auto-dismisses after 4 seconds of inactivity.

### 2.6 Picture-in-Picture (PiP) & External Player
Ground truth citations: (`MainActivity.kt:162-173`, `PlayerPictureInPictureManager.android.kt:1-90`)
- **Automatic PiP on Home Gesture**: `MainActivity.onUserLeaveHint` enters PiP mode automatically if video is actively playing.
- **PiP Actions**: Remote media action receiver (`PipRemoteActionReceiver`) responds to Play, Pause, and Close actions from Android's system PiP window.
- **External Player Hand-Off**: If user selects "Open in External Player" or enables external playback in settings, `ExternalPlayerPlatform.launch` fires an Android Intent to VLC, MX Player, Just Player, or Infuse with exact stream URL, HTTP headers, subtitle tracks, and resume timestamp.
