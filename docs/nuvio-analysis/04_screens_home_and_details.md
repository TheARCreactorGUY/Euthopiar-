# 04. Screen Reference: Home, Media Details & Catalog Browsing

This document specifies the exact UI architecture, state fields, component trees, lifecycle hooks, visual tokens, and animations for Nuvio's discovery and detail screens.

---

## 1. Home Screen (`HomeScreen`)

### 1.1 Purpose and Entry Points
- **Purpose**: The primary discovery surface. Houses the spotlight featured carousel (Hero), user in-progress resume items (Continue Watching), customized catalog rows (Addon & TMDB catalogs), and personal collections.
- **Entry Points**: Default route upon app launch (`TabsRoute`, selected tab: `AppScreenTab.Home`). Navigated to via bottom navigation bar or tapping the Home icon.

### 1.2 Files Involved
- **UI**: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeScreen.kt`
- **Sub-Components**:
  - `components/HomeHeroSection.kt` (Spotlight carousel)
  - `components/HomeContinueWatchingSection.kt` (Resume shelf)
  - `components/HomeCatalogRowSection.kt` (Horizontal poster rows)
  - `components/HomeCollectionRowSection.kt` (Custom collections)
  - `components/HomeSkeletonHero.kt` & `HomeSkeletonRow.kt` (Shimmer loading)
- **Repositories & State Holders**:
  - `HomeRepository.kt` (`HomeUiState`)
  - `HomeCatalogSettingsRepository.kt` (`HomeCatalogSettingsUiState`)
  - `WatchProgressRepository.kt` (`WatchProgressUiState`)
  - `WatchedRepository.kt` (`WatchedUiState`)
  - `CollectionRepository.kt` (`CollectionUiState`)
- **Models**: `HomeCatalogSection`, `MetaPreview`, `ContinueWatchingItem`

### 1.3 Layout Structure (Top to Bottom)
1. `NuvioScreen` scaffold with `BoxWithConstraints`
2. `LazyColumn` with nested scroll connection (`rememberHeroStretchState`):
   - **Item 0: Spotlight Hero (`HomeHeroSection`)**: Full-width backdrop poster with ambient bottom gradient, title logo graphic (`AsyncImage`), genre pills, release year, overview text snippet, and action buttons (`Play`, `Details`, `Add to Watchlist`).
   - **Item 1: Continue Watching Shelf (`HomeContinueWatchingSection`)**: Visible if user has in-progress items. Horizontal scroll of landscape cards displaying episode thumbnails, series title, season/episode code (`S1:E4`), remaining time, and a high-contrast progress bar.
   - **Items 2..N: Dynamic Catalog Rows (`HomeCatalogRowSection`)**:
     - Header: Category title (e.g. "Trending Movies", "Popular Series - CyberFlix", "Top Anime") + clickable "View All" chevron.
     - Content: Lazy horizontal row of poster cards (`PosterCard`), each displaying poster artwork, title, rating pill, and watched badge.
   - **Items N+1: Collection Rows (`HomeCollectionRowSection`)**: User's custom lists and folders.
   - **Item Bottom**: Safe area bottom spacer (`nuvioSafeBottomPadding`) to clear bottom navigation bar.

### 1.4 State Fields & Mutations
| State Field | Type | Source | Mutator |
| :--- | :--- | :--- | :--- |
| `homeUiState.sections` | `List<HomeCatalogSection>` | `HomeRepository.uiState` | Addon response fetch, reordering in settings |
| `homeUiState.isLoading` | `Boolean` | `HomeRepository.uiState` | Network fetch start / completion |
| `continueWatchingItems` | `List<ContinueWatchingItem>` | `WatchProgressRepository.uiState` | Player playback ticks, deletions, scrobbles |
| `watchedBadges` | `Map<String, Boolean>` | `WatchedRepository.uiState` | Toggle watched mark, scrobble sync |
| `collections` | `List<Collection>` | `CollectionRepository.collections`| Collection sync or local edit |
| `isOffline` | `Boolean` | `NetworkStatusRepository.uiState` | OS network connectivity changes |

### 1.5 Data Calls & Lifecycle
- **On Open**: `LaunchedEffect(Unit)` calls `AddonRepository.initialize()`, `CollectionRepository.initialize()`, `WatchProgressRepository.ensureLoaded()`, `WatchedRepository.ensureLoaded()`.
- **On Scroll**: Poster images load lazily via Coil 3 disk/memory cache.
- **On Refresh**: Pull-to-refresh calls `HomeRepository.refresh(addons, force = true)`.

### 1.6 User Interactions & Gestures
- **Tap Poster**: Navigates to `DetailRoute(type, id)`.
- **Long-Press Poster**: Triggers haptic feedback (`LongPress`), consumes anchor position (`PosterZoomAnchorHolder`), and opens `NuvioPosterZoomActionOverlay` (quick actions: Play, Add to Watchlist, Mark Watched).
- **Tap Continue Watching Item**: Triggers `onContinueWatchingClick` -> invokes `launchPlaybackWithDownloadPreference` to immediately resume stream.
- **Long-Press Continue Watching Item**: Opens `NuvioContinueWatchingActionSheet` (Resume, Start from beginning, Mark as Watched, Remove from history).
- **Tap "View All" on Row Header**: Navigates to `CatalogRoute(launchId)`.

### 1.7 Motion & Visual Specs
- **Animations**: Crossfade between skeleton shimmer and loaded cards (`tween(350ms, LinearOutSlowInEasing)`).
- **Hero Stretch**: `rememberHeroStretchState` expands backdrop height dynamically during overscroll drag.
- **Tokens**: Background `Color(0xFF020404)`, Surface `Color(0xFF0E1213)`, Accent `Color(0xFF00E5FF)` / Theme accent, Title font `CabinetGrotesk-Bold`, Body font `Inter-Regular`.

---

## 2. Media Details Screen (`MetaDetailsScreen`)

### 2.1 Purpose and Entry Points
- **Purpose**: Displays full metadata for a Movie or TV Series: backdrop, title logo, synopsis, ratings, primary action button, season/episode list, actor cast rail, community comments, and trailers.
- **Entry Points**: `DetailRoute(type: String, id: String, title: String?)`. Navigated from Home posters, Search results, Library shelves, or Deep links (`nuvio://meta?...`).

### 2.2 Files Involved
- **UI**: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsScreen.kt`
- **Sub-Components**:
  - `components/DetailHero.kt` (Backdrop, title logo, metadata pills)
  - `components/DetailActions.kt` (Play, Select Source, Watchlist, Watched, Shuffle)
  - `components/DetailSeriesContent.kt` (Season tabs, episode cards)
  - `components/DetailCastSection.kt` (Cast avatars)
  - `components/DetailCommentsSection.kt` (Trakt reviews)
  - `components/DetailTrailersSection.kt` (Trailer player)
  - `components/DetailPosterRailSection.kt` (More Like This)
- **Repository**: `MetaDetailsRepository.kt` (`MetaDetailsUiState`)

### 2.3 Layout Structure (Top to Bottom)
1. **Backdrop & Hero Section**:
   - High-resolution backdrop (`AsyncImage`) with vertical gradient scrim (`Brush.verticalGradient`).
   - Floating back button (`NuvioBackButton`) pinned to top status bar inset.
   - Title logo graphic or large typography title (`CabinetGrotesk-Black`, 32sp).
   - Metadata badges row: Release Year, Runtime / Season count, Content Rating (`PG-13`, `TV-MA`), Resolution tag (`4K UHD`), Genres.
2. **Action Buttons**:
   - **Primary CTA Button**: "Resume S1:E2" (for in-progress shows) or "Play" (for movies). Full-width accent pill button.
   - **Secondary Row**:
     - "Sources" button (manual stream picker)
     - Watchlist icon toggle (`Add` / `Check`)
     - Watched status toggle (`CheckCircleOutline` / `CheckCircle`)
     - Shuffle Episodes button (series only)
     - Trailer button (launches YouTube popup / direct player)
3. **Overview & Synopsis**: Expandable text block with "...more" toggle.
4. **Ratings Rail**: MDBList badge container displaying IMDb rating (`★ 8.8`), Rotten Tomatoes score (`🍅 94%`), and Metacritic (`78`).
5. **Series Episode Browser (Series Only)**:
   - Season horizontal chip selector (Season 1, Season 2, Season 3, Specials).
   - View mode toggle button: Grid Mode vs List Mode.
   - Episode cards: Episode number, thumbnail, title, release date, runtime, synopsis, and watched checkmark.
6. **Cast & Crew Rail**: Horizontal row of circular actor photos, actor names, and character roles.
7. **Trakt Comments & Reviews**: Top community reviews with user avatars and star ratings.
8. **"More Like This" Shelf**: Horizontal poster rail of recommended similar titles.

### 2.4 State Fields & Mutations
| State Field | Type | Source | Mutator |
| :--- | :--- | :--- | :--- |
| `metaState.meta` | `MetaDetails?` | `MetaDetailsRepository.uiState` | Addon meta fetch, TMDB fallback |
| `selectedSeason` | `Int` | Local `rememberSaveable` | User tapping season chip |
| `seasonViewMode` | `SeasonViewMode` | `SeasonViewModeStorage` | User tapping grid/list toggle |
| `isWatched` | `Boolean` | `WatchedRepository.uiState` | User tapping Mark Watched |
| `isBookmarked` | `Boolean` | `LibraryRepository.uiState` | User tapping Add to Watchlist |
| `watchProgress` | `WatchProgressEntry?` | `WatchProgressRepository.uiState`| Playback sync |

### 2.5 Motion & Visual Specs
- **Shared Transition**: Shared element bounds animation (`sharedElement`) transitions the poster seamlessly from the Home row into the Details screen.
- **Glassmorphism Header**: As user scrolls down, a frosted glass floating header (`DetailFloatingHeader`) fades in with blurred background (`hazeSource`).

---

## 3. Catalog & Full Browse Screen (`CatalogScreen`)

### 3.1 Purpose and Entry Points
- **Purpose**: Paginated, infinite-scrolling grid displaying the full catalog contents of an addon row or library section.
- **Entry Points**: `CatalogRoute(launchId: Long, title: String, subtitle: String?)` via "View All" on Home or Library rows.

### 3.2 Component & Grid Structure
- Header: Back button + Large Title + Subtitle (source addon name).
- Content: `LazyVerticalGrid` with adaptive columns (`GridCells.Adaptive(minSize = 120.dp)`).
- Pagination: Triggered when scroll position reaches `items.size - 6`, dispatching `CatalogRepository.loadNextPage()`.
- Pull-to-refresh: Swipe gesture triggers complete cache invalidation and page 1 reload.
