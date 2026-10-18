# Nuvio Architecture Reference: Library, Search, Collections & Downloads Screens

This document provides a source-grounded specification of Nuvio's secondary primary screens: **SearchScreen**, **LibraryScreen**, **CollectionsScreen & FolderDetailScreen**, and **DownloadsScreen**. Every UI component tree, state flow, network operation, and transition is extracted directly from the Kotlin Multiplatform and Android codebase.

---

## 1. SearchScreen (`AppRoute.Search`)

### Purpose and Entry Points
- **Purpose**: Provides a unified search experience across all enabled Stremio addons and media servers (Jellyfin/Emby/Plex), with debounced querying, recent search history, and an interactive Stremio catalog discovery browser with genre/type filter chips.
- **Entry Points**: 
  - Bottom navigation bar: `AppRoute.Search` (`NuvioTabSearch.imageset`).
  - Home screen search action: Navigates to `AppRoute.Search` with optional focus request count (`searchFocusRequestCount`).
- **Compose Equivalent**: Jetpack Compose `LazyColumn` with sticky header containing `NuvioInputField`, streaming results via `Channel` or Kotlin `Flow`.

### Files Involved
- UI:
  - [SearchScreen.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchScreen.kt#L1-L528)
  - [SearchDiscoverContent.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchDiscoverContent.kt#L1-L263)
- ViewModel / Repository:
  - [SearchRepository.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchRepository.kt#L1-L706)
  - [SearchHistoryRepository.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchHistoryRepository.kt#L1-L150)
  - [SearchHistoryStorage.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchHistoryStorage.kt#L1-L60)
- Models:
  - [SearchModels.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchModels.kt#L1-L115)

### Layout Structure (Component Tree, Top to Bottom)
```
BoxWithConstraints (fillMaxSize)
└── NuvioScreen (listState)
    ├── stickyHeader (key = "search_header")
    │   └── Box (fillMaxWidth, background = colorScheme.background, nuvioConsumePointerEvents)
    │       └── Column
    │           ├── NuvioScreenHeader (title = headerTitle ["Search" or "Discover"], horizontalPadding = 16.dp)
    │           ├── Spacer(6.dp)
    │           ├── NuvioInputField
    │           │   ├── Leading: Search icon
    │           │   ├── Value: query (String)
    │           │   ├── Placeholder: "Search movies, series, anime..."
    │           │   └── Trailing: Close button (IconButton) [visible when query.isNotBlank()]
    │           └── Spacer(14.dp)
    ├── [When query.isBlank()]
    │   ├── [When isSearchFocused && recentSearches.isNotEmpty()]
    │   │   └── SearchRecentSection (item: "recent_searches")
    │   │       └── Column
    │   │           ├── Text ("Recent Searches", titleMedium, SemiBold)
    │   │           └── For each recent query -> SearchRecentRow (Row, clickable, RoundedCornerShape(16.dp), trailing delete IconButton)
    │   └── discoverContent (LazyListScope extension)
    │       ├── item: DiscoverSectionHeader (Text "Discover", headlineSmall)
    │       ├── item: DiscoverFilterRow (horizontalScroll)
    │       │   ├── NuvioDropdownChip (Type filter: Movie, Series, Anime, Other)
    │       │   ├── NuvioDropdownChip (Catalog filter: Popular, Top Rated, Trending)
    │       │   └── NuvioDropdownChip (Genre filter: Action, Comedy, Drama, Sci-Fi)
    │       ├── [When selectedCatalog != null] item: Context subtitle Text ("Catalog from {addonName} • {type}")
    │       ├── [When loading] items(2): PosterGridSkeletonRow
    │       ├── [When empty] item: DiscoverEmptyStateCard / NuvioNetworkOfflineCard
    │       └── [When populated] items(grid rows): PosterGridRow (calculated columns: 2 to 6 posters)
    │           └── [When discoverUiState.isLoadingMore] item: NuvioLoadingIndicator
    └── [When query.isNotBlank()]
        ├── [When debouncing / waiting for query response] items(2): HomeSkeletonRow
        ├── [When empty & not loading] item: SearchEmptyStateCard
        └── [When populated]
            ├── items(uiState.sections): HomeCatalogRowSection (horizontal shelf with poster cards)
            └── [When uiState.isLoading] item: HomeSkeletonRow (horizontal skeleton)
```

### State Fields, Sources, and Mutations
| State Field | Type | Source | Mutated By |
|---|---|---|---|
| `query` | `String` | Local `rememberSaveable` | Typing in `NuvioInputField` or tapping a recent search chip |
| `lastRequestedQuery` | `String?` | Local `rememberSaveable` | Updated to `query.trim()` after 350ms debounce |
| `isSearchFocused` | `Boolean` | Local `remember` | `onFocusChanged` callback of `NuvioInputField` |
| `uiState` | `SearchUiState` | `SearchRepository.uiState` | Addon search responses, timeouts, or clear actions |
| `discoverUiState` | `DiscoverUiState` | `SearchRepository.discoverUiState` | Catalog page fetches, filter selection, pagination |
| `recentSearches` | `List<String>` | `SearchHistoryRepository.uiState` | Recording successful non-empty searches, deleting items |
| `networkCondition` | `NetworkCondition` | `NetworkStatusRepository.uiState` | Connectivity changes triggering auto-retry |
| `observedOfflineState`| `Boolean` | Local `remember` | Offline detector latch to trigger refresh on reconnect |

### Data Calls Made
- **On Open**:
  - `AddonRepository.initialize()`: Asserts addon manifests are in memory ([SearchScreen.kt:115](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchScreen.kt#L115)).
  - `WatchedRepository.ensureLoaded()`: Populates watched badges for search posters ([SearchScreen.kt:116](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchScreen.kt#L116)).
  - `SearchHistoryRepository.ensureLoaded()`: Reads recent query history from disk ([SearchScreen.kt:117](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchScreen.kt#L117)).
  - `SearchRepository.refreshDiscover(addons)`: Loads default catalog items for the Discover view ([SearchScreen.kt:154](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchScreen.kt#L154)).
- **On Query Input**:
  - Debounced by `delay(350)` ([SearchScreen.kt:164](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchScreen.kt#L164)).
  - Executes `SearchRepository.search(query, addons)`: Dispatches concurrent requests across all enabled Stremio addons declaring `"search"` in extraSupported, plus all configured Jellyfin/Emby servers ([SearchRepository.kt:109-150](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchRepository.kt#L109-L150)).
  - Streams results asynchronously via `Channel<IndexedSearchResult>(Channel.UNLIMITED)`; as each provider returns, sections are dynamically updated in the UI ([SearchRepository.kt:158-199](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchRepository.kt#L158-L199)).
- **On Scroll (Discover Infinite Loading)**:
  - Triggers when `lastVisible >= totalItemsCount - 4` ([SearchScreen.kt:178-185](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchScreen.kt#L178-L185)).
  - Calls `SearchRepository.loadMoreDiscover()`: Fetches the next page of 20 items (`CATALOG_PAGE_SIZE = 20`) via `skip={currentCount}`.
- **On Refresh / Retry**:
  - `NetworkStatusRepository.requestRefresh(force = true)` and `SearchRepository.refreshDiscover(forceRefresh = true)` or `SearchRepository.search(forceRefresh = true)`.

### User Interactions
- **Tap Search Input Field**: Activates keyboard and shows recent searches list.
- **Tap Clear Button (X)**: Clears text, hides recent searches, cancels active search coroutine, and restores Discover view.
- **Tap Recent Search Item**: Fills query field with previous search string and dispatches query.
- **Tap Delete on Recent Search**: Invokes `SearchHistoryRepository.removeSearch(query)`.
- **Tap Dropdown Filter Chip (Type / Catalog / Genre)**: Mutates `SearchRepository.selectDiscoverType()`, `selectDiscoverCatalog()`, or `selectDiscoverGenre()`, invalidating cache and fetching the selected catalog subset.
- **Tap Poster**: Invokes `onPosterClick(MetaPreview)` to navigate to `AppRoute.MetaDetails`.
- **Long-Press Poster**: Opens context preview sheet (`onPosterLongClick`).

### Motion and Transitions
- **Recent Searches List Appearance**: Height change animated implicitly via Compose layout measurement.
- **Sticky Header Transition**: Header sticky background pinned at top with `background(MaterialTheme.colorScheme.background)` and `nuvioConsumePointerEvents()`.
- **Skeleton Shimmer**: Animated gradient shimmer (`com.nuvio.app.core.ui.shimmer`) across skeleton rows during network queries.

### Visual Specs
- **Background**: `MaterialTheme.colorScheme.background`.
- **Search Field**: Height 52.dp, rounded corners 26.dp (capsule shape), surface variant background with onSurfaceVariant hint text.
- **Filter Chips**: Height 36.dp, rounded corners 18.dp, border 1.dp solid `colorScheme.outlineVariant`.
- **Grid Poster Padding**: 6.dp inter-item spacing, 16.dp screen margin padding.

---

## 2. LibraryScreen (`AppRoute.Library`)

### Purpose and Entry Points
- **Purpose**: Displays the user's saved watchlist and personal collections across local device storage and external tracking providers (Trakt, Simkl, MDBList), plus media server libraries (Jellyfin/Emby) and Real-Debrid cloud torrent drives.
- **Entry Points**: 
  - Bottom navigation bar: `AppRoute.Library` (`NuvioTabLibrary.imageset`).
- **Compose Equivalent**: Jetpack Compose `LazyColumn` supporting dynamic layout switching (`HORIZONTAL` shelves vs `VERTICAL` grid), multi-source tab switching, and custom particle disintegration animations upon deletion.

### Files Involved
- UI:
  - [LibraryScreen.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryScreen.kt#L1-L1370)
  - [LibraryListRows.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryListRows.kt#L1-L320)
  - [LibraryListEditor.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryListEditor.kt#L1-L240)
  - [LibraryDisplaySettingsStorage.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryDisplaySettingsStorage.kt#L1-L180)
- ViewModel / Repository:
  - [LibraryRepository.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryRepository.kt#L1-L680)
  - [TraktLibraryRepository.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktLibraryRepository.kt#L1-L450)
  - [CloudLibraryRepository.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/cloud/CloudLibraryRepository.kt#L1-L380)
- Models:
  - [LibraryModels.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryModels.kt#L1-L118)

### Layout Structure (Component Tree, Top to Bottom)
```
BoxWithConstraints (fillMaxSize)
└── NuvioScreen (listState)
    ├── stickyHeader (key = "library_header")
    │   └── Column
    │       ├── Row: NuvioScreenHeader (title = "Library")
    │       │   └── Actions:
    │       │       ├── Search button (IconButton -> expands cloudSearchQuery or in-library filter)
    │       │       ├── Layout mode toggle (IconButton: GridView vs ViewAgenda)
    │       │       ├── Refresh button (IconButton -> triggers pullFromServer)
    │       │       └── Downloads pill button (LibraryDownloadsButton -> navigates to Downloads)
    │       ├── Row: Mode selector tabs (horizontalScroll)
    │       │   ├── "Saved" (Local watchlist / Trakt / Simkl / MDBList depending on account sync)
    │       │   ├── "Cloud" (Debrid torrent drives: Real-Debrid, Premiumize, TorBox, AllDebrid) [if enabled]
    │       │   └── "Servers" (Jellyfin, Emby, Plex remote libraries) [if connected]
    │       └── Row: Sorting & Shelf Filter Chips (horizontalScroll)
    │           ├── Sort chip (Default, Name, Date Added, Rating, Release Date)
    │           └── Section type filter chips (Movies, Series, Anime, Custom Lists)
    │
    ├── [When sourceMode == LibraryViewMode.Saved]
    │   ├── [When layoutMode == HORIZONTAL]
    │   │   └── items(sortedSections): NuvioShelfSection
    │   │       ├── Header: Shelf title ("Watching", "Planning to Watch", "Completed", "Dropped") + "View All" pill
    │   │       └── LazyRow: DisintegratingContainer wrapping HomePosterCard
    │   └── [When layoutMode == VERTICAL]
    │       ├── item: Section & Type filter dropdown row
    │       └── items(verticalProjection.entries): LazyVerticalGrid of HomePosterCard
    │
    ├── [When sourceMode == LibraryViewMode.Cloud]
    │   ├── item: Cloud provider status card (storage quota, torrent active slots)
    │   └── items(cloudItems): CloudTorrentRow (folder name, file count, torrent size, delete action)
    │       └── [When expanded] nested items: CloudTorrentFileRow (direct file play button)
    │
    └── [When sourceMode == LibraryViewMode.Servers]
        └── items(serverShelves): HomeCatalogRowSection (populated from remote Jellyfin/Emby catalogs)
```

### State Fields, Sources, and Mutations
| State Field | Type | Source | Mutated By |
|---|---|---|---|
| `sourceModeName` | `String` | `rememberSaveable` | Tapping "Saved", "Cloud", or "Servers" tab |
| `displaySettings`| `LibraryDisplaySettings` | `LibraryDisplaySettingsRepository` | Toggling Grid/Horizontal layout, changing sort order |
| `uiState` | `LibraryUiState` | `LibraryRepository.uiState` | Local database reads, remote Trakt/Simkl/MDBList sync |
| `cloudUiState` | `CloudLibraryUiState` | `CloudLibraryRepository.uiState` | Real-Debrid/TorBox cloud drive fetches |
| `serverShelves` | `List<LibraryServerShelf>?` | Local state | Async Jellyfin/Emby catalog queries |
| `disintegrationRequest`| `DisintegrationRequest<String>?` | Passed as prop or local event | Removing an item from the library triggers particle animation |

### Motion and Transitions
- **Disintegration Animation**: When an item is deleted from the library, Nuvio executes a custom particle dissolve effect via `DisintegratingContainer` ([LibraryScreen.kt:35](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryScreen.kt#L35)):
  - Duration: 650ms.
  - The poster canvas is sliced into a 16x16 particle grid which fades out with random velocity and upward drift before the item is removed from the list state.
- **Layout Mode Crossfade**: Switching between `HORIZONTAL` shelves and `VERTICAL` grid utilizes `Crossfade(animationSpec = tween(250))` ([LibraryScreen.kt:3](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryScreen.kt#L3)).

---

## 3. CollectionsScreen & FolderDetailScreen

### Purpose and Entry Points
- **Purpose**:
  - `CollectionsScreen`: Displays custom user-curated folders and TMDB collection bundles (e.g. "Marvel Cinematic Universe", "Harry Potter Collection").
  - `FolderDetailScreen`: Displays the items within a specific folder or collection with a parallax collapsible hero banner, tabbed categorization, and quick batch play actions.
- **Entry Points**: 
  - Library tab -> Collections section.
  - Meta details screen -> "Part of Collection" link -> `AppRoute.FolderDetail(folderId)`.
- **Compose Equivalent**: Jetpack Compose `LazyVerticalGrid` with collapsible hero image managed via `NestedScrollConnection`.

### Files Involved
- UI:
  - [FolderDetailScreen.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/FolderDetailScreen.kt#L1-L441)
  - [CollectionEditorScreen.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionEditorScreen.kt#L1-L320)
- ViewModel / Repository:
  - [FolderDetailRepository.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/FolderDetailRepository.kt#L1-L190)
  - [CollectionRepository.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionRepository.kt#L1-L420)
- Models:
  - [CollectionModels.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionModels.kt#L1-L150)

### Layout Structure (FolderDetailScreen)
```
Box (fillMaxSize)
├── [When coverImageUrl != null]
│   └── Collapsible Hero Header (height = heroHeightPx, max 176.dp)
│       ├── AsyncImage (coverImageUrl, ContentScale.Crop)
│       └── Box (background = vertical gradient: Transparent to background)
├── NestedScrollContainer (heroScrollConnection)
│   └── LazyColumn / LazyVerticalGrid
│       ├── Spacer (height = heroHeightPx)
│       ├── item: Folder Title, Description & Item Count
│       ├── item: ScrollableTabRow (All, Movies, Series)
│       └── items(folderItems): NuvioPosterCard grid (columns: 2 to 6 based on screen width)
└── stickyHeader: NuvioScreenHeader (back navigation arrow, folder title fade-in)
```

### State & Motion Specs
- **Collapsible Hero Header**:
  - `FolderCoverHeight = 176.dp` ([FolderDetailScreen.kt:76](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/FolderDetailScreen.kt#L76)).
  - `heroScrollConnection`: Implements `NestedScrollConnection.onPreScroll` to dynamically subtract scroll delta from `heroHeightPx` between `0f` and `maxHeroHeightPx` ([FolderDetailScreen.kt:99-115](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/FolderDetailScreen.kt#L99-L115)).

---

## 4. DownloadsScreen (`AppRoute.Downloads`)

### Purpose and Entry Points
- **Purpose**: Manages active background file downloads (pause, resume, cancel) and displays completed offline media files with direct playback launch capabilities.
- **Entry Points**: 
  - Deep link: `nuvio://downloads`.
  - Library screen -> Top bar Downloads pill button (`onDownloadsClick`).
  - Settings -> Storage & Downloads section.
- **Compose Equivalent**: Jetpack Compose `LazyColumn` observing `DownloadsRepository.uiState` Flow, bound to AndroidX WorkManager / foreground service.

### Files Involved
- UI:
  - [DownloadsScreen.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsScreen.kt#L1-L532)
- Engine / Repository:
  - [DownloadsRepository.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsRepository.kt#L1-L510)
  - [DownloadsPlatformDownloader.android.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsPlatformDownloader.android.kt#L1-L148)
  - [AndroidDownloadScheduler.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/AndroidDownloadScheduler.kt#L1-L280)
- Models:
  - [DownloadsModels.kt](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsModels.kt#L1-L115)

### Layout Structure (Component Tree, Top to Bottom)
```
NuvioScreen
├── stickyHeader
│   └── NuvioScreenHeader
│       ├── Title: "Downloads" (or Show Title when inside series view)
│       ├── Back navigation button
│       └── Action: Open Downloads Directory button (Icons.Rounded.Folder)
│
├── [When selectedShowId == null (Root Downloads Content)]
│   ├── [Active Downloads Section (if any in progress)]
│   │   ├── item: Section header ("Downloading ({count})")
│   │   └── items(activeItems): DownloadActiveRow
│   │       ├── Poster thumbnail (or episode preview)
│   │       ├── Column: Title, Season/Episode info, Download speed, ETA
│   │       ├── LinearProgressIndicator (progressFraction 0f..1f)
│   │       └── Controls Row:
│   │           ├── Pause / Resume button (Icons.Rounded.Pause / PlayArrow)
│   │           └── Delete / Cancel button (Icons.Rounded.Delete)
│   │
│   ├── [Completed Downloads Section]
│   │   ├── item: Section header ("Downloaded ({count})")
│   │   ├── items(completedMovies): DownloadMovieRow
│   │   │   ├── Poster thumbnail
│   │   │   ├── Title + File size (e.g. "2.4 GB")
│   │   │   ├── Play action button
│   │   │   └── Delete action button
│   │   └── items(completedSeriesGroups): DownloadShowGroupRow
│   │       ├── Poster thumbnail + Badge count of downloaded episodes
│   │       ├── Series title + Total size across all episodes
│   │       └── Trailing chevron: Navigates into show-specific episode list
│   │
│   └── [When items is empty]
│       └── item: HomeEmptyStateCard ("No downloads yet", "Downloaded media will appear here for offline playback")
│
└── [When selectedShowId != null (Show-Specific Episode View)]
    └── items(completedEpisodes): DownloadEpisodeRow
        ├── Episode thumbnail + Episode title + "S{season} E{episode}"
        ├── File size + Direct play button
        └── Delete button
```

### State Fields, Sources, and Mutations
| State Field | Type | Source | Mutated By |
|---|---|---|---|
| `uiState` | `DownloadsUiState` | `DownloadsRepository.uiState` | Real-time byte progress from `AndroidDownloadScheduler`, job state transitions |
| `selectedShowId` | `String?` | Local `rememberSaveable` | Tapping a series group card sets show ID; back button clears it |
| `downloadPendingDeletionId`| `String?` | Local `rememberSaveable` | Tapping delete icon sets ID, prompting `NuvioStatusModal` confirmation |
| `status` | `DownloadStatus` | `DownloadItem.status` | `Downloading`, `Paused`, `Completed`, `Failed` |
| `downloadedBytes` | `Long` | `DownloadItem.downloadedBytes`| Updated incrementally during network stream write |
| `totalBytes` | `Long?` | `DownloadItem.totalBytes` | Extracted from HTTP `Content-Length` header |

### Data Calls and Background Operations
1. **Enqueueing**:
   - Tapping download on a stream invokes `DownloadsRepository.enqueueDownload(stream, meta, season, episode)` ([DownloadsModels.kt:90-100](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsModels.kt#L90-L100)).
   - Writes request payload including stream URL, request headers, and custom subtitle tracks to disk storage (`downloads_store.json`).
2. **Transfer Execution**:
   - Android platform invokes `AndroidDownloadScheduler` which triggers `DownloadsTransferJobService` (`RUN_USER_INITIATED_JOBS` on Android 14+ / SDK 34) ([DownloadsPlatformDownloader.android.kt:29-60](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsPlatformDownloader.android.kt#L29-L60)).
   - Streams chunks into `context.filesDir/downloads/{destinationFileName}`.
3. **Offline Playback Launch**:
   - When user taps a completed download item, Nuvio constructs `AppRoute.Player` passing `mediaUrl = item.localFileUri` (resolved via `resolveLocalFileUri()` to `file:///...`) ([DownloadsPlatformDownloader.android.kt:84-100](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsPlatformDownloader.android.kt#L84-L100)).
   - Offline subtitle tracks are read directly from companion `.srt` files downloaded into the same directory.
