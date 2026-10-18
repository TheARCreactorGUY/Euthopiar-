# Agent Prompt – Euthopiar UI/UX Overhaul (Full Specification)

Copy everything below the divider into your agent.

---

# ROLE
You are a senior Android engineer and product designer. You are improving the UI/UX of **Euthopiar**, a streaming app (Kotlin, Jetpack Compose, Material3, Coil3, Media3/ExoPlayer, Room, Navigation-Compose). Target quality: Netflix, Prime Video, Apple TV, Disney+.

# AUTHORISATION AND OVERRIDES
The product owner **explicitly authorises** a compliance and UX improvement pass. This overrides the CAUTION block ("read-only, do not modify") in `docs/EUTHOPIAR_DESIGN_SYSTEM_GUIDE.md`, **only for the items in this document**. Anything not listed here stays untouched.

# MANDATORY READING (before any edit)
1. `docs/EUTHOPIAR_DESIGN_SYSTEM_GUIDE.md` – visual rules and tokens
2. `docs/DESIGN_AUDIT_REPORT.md` – compliance findings with file references
3. `docs/EUTHOPIAR_UX_GUIDE.md` – behaviour rules, copy, states, budgets
4. Token files: `ui/theme/{Color,Type,Shape,Dimens,Motion,Adaptive,Theme}.kt`
5. Shared components in `ui/components/`

Findings in the audit were produced by static analysis. **Verify each item in the code before changing it.** If an item is already fixed, or the premise is wrong, say so in your report and skip it. Do not invent problems.

# HARD BOUNDARIES (never violate)
- Do NOT modify: plugin/EUP loading and logic (`app/plugin`, `PluginRepositoryService`, `DynamicPluginLoader`), anything in the `core` module (networking, DoH, scrapers, extractors, debrid/trakt/simkl clients, AniSkip, TMDB bridge), Room entities/DAOs/migrations, repositories, download engine internals.
- Do NOT touch `ui/incognito/` (Faphouse) in any phase unless a task says so. Never add references to it from new UI code.
- Do NOT change existing token VALUES in `ui/theme/`. You may ADD new tokens.
- ViewModels: you may add presentation-only state and pure mapping of errors to user-facing text. No business logic changes, no new network calls.
- Preserve all existing comments and KDoc not related to your change.
- The Skip Intro 6-second perimeter countdown must remain unchanged.
- Never commit secrets, APKs, or the `scratch/`, `dist_apk/`, `release_dist/`, `build_server/` folders.
- If a task needs a data source or product decision that does not exist, **stop and ask**. Do not fake data.

# WORKING METHOD
- Create branch `ui-ux-overhaul`. One commit per numbered task, with a clear message.
- Work **phase by phase**. At the end of each phase: build, test, write the phase report (format at the end), then **STOP and wait for my approval**.
- Before each phase, capture baseline screenshots (Home, Details movie, Details series, Player controls visible, Search, Library, Downloads, Settings) at 1.0× font, 1.3× font, phone and tablet widths, and save them under `docs/screens/before/`. After the phase, capture `after/` and note visual differences.
- Commands: `.\gradlew.bat :app:compileReleaseKotlin`, `.\gradlew.bat :app:testDebugUnitTest`, and the token-check script once it exists (Phase 1).
- Reuse existing components (`PosterCard`, `AppFilterChip`, `AppBottomSheet`, `VelvetAlertDialog`, `EmptyState`, `ErrorState`, `ShimmerSkeleton`, `MetaLine`, `PrimaryButton`, `SecondaryButton`, `SectionHeader`, `TvFocus`). Do not create parallel versions. If a needed component is missing (listed in Phase 1), add it to `ui/components/` and document it.
- Use only tokens. No raw colors, radii, sp, or ad-hoc dp (except 1dp hairlines and sizes inside token files).

---

# PHASE 1 – Foundation: tokens, shared components, enforcement

### 1.1 Add missing tokens (additive only)
Create in `ui/theme/`:
- **Gradients/scrims** (`Scrims.kt`): `HeroBottomGradient`, `PlayerTopScrim`, `PlayerBottomScrim`, `DetailsBackdropGradient`, built from `Background`/`OverlayScrim`.
- **Skeleton colours**: `SkeletonBase`, `SkeletonHighlight` (replace hex `0xFF0F1318 / 0xFF1B222B` in `AnimationModifiers.kt` and `ShimmerSkeleton.kt`).
- **Typography additions** in `Type.kt`: `AppTypography.timeLabel` (12/16 Medium, tabular numerals if Inter supports it), `AppTypography.badge` (11/14 SemiBold, letterSpacing 0.2sp), `AppTypography.numeralDisplay` (for Top 10 numerals instead of raw 94sp/80sp), `AppTypography.bodySmall` (equivalent to labelMedium but named, to end the "no bodySmall" workaround; update the guide).
- **Icon sizes** in `Dimens.kt`: `iconS=16`, `iconM=20`, `iconL=24`, `iconXL=32`.
- **Subtitle colour presets** (`SubtitleColors.kt`): White, Yellow, Cyan, Green as documented constants, used by both `PlayerScreen.kt` (lines ~576-578) and `SettingsScreen.kt` (lines ~400-402). Mark them as "user content, not UI chrome".
- **Hairline/border** constants if missing (`HairlineWidth = 1.dp`).

### 1.2 Replace raw values with tokens in these files (and only these)
`MetadataBadges.kt`, `ShimmerSkeleton.kt`, `AnimationModifiers.kt`, `HeroCarousel.kt` (`Color(0x33FFFFFF)`), `TopTenRow.kt`, `TopHeaderBar.kt`, `PlayerControls.kt`, `PlayerGestures.kt`, `PlayerScreen.kt`, `PlayerSidePanel.kt`, `PlayerSheets.kt`, `EpisodeSelectorPanel.kt`, `SettingsScreen.kt`, `EcosystemSettingsDialogs.kt` (Trakt red stays: documented brand exception), `CinematicSplashScreen.kt`, `PosterColorExtractor.kt` (fallback colour → `SurfaceHigh`).
- Replace `MaterialTheme.typography.*` with `AppTypography.*` (about 230 usages). Map: bodySmall→`bodySmall`, others 1:1.
- Remove `.copy(fontSize = …)` (e.g. `SettingsScreen.kt:117` → `titleLarge` or `displayHero` as appropriate).
- Remove deprecated aliases (`StarGold`, `PureWhite`, `TextMuted`, etc.) from non-incognito files.
- Replace `RoundedCornerShape(n.dp)` with `ShapeS/M/L/Full`. Where a 4-5dp radius was used in badges, use `ShapeS`.

### 1.3 Rebuild `MetadataBadges.kt`
Spec: text-only or icon+text badges, `ShapeS`, background `SurfaceHigh`, text `AppTypography.badge` in `TextSecondary`, no custom borders, no gold/4K glow. **Max 2 badges** per card or detail header. Provide: `QualityBadge("4K"/"HD"/"HDR")`, `RatingBadge`, `StatusBadge` (for Downloads: Ready/Downloading/Paused/Failed with icon + text, never colour alone). Keep public function names to avoid call-site churn; if signatures change, update call sites.

### 1.4 Add missing shared components (all tokens, previewed, with TvFocus + semantics)
- `AppSnackbarHost` + `AppSnackbar` (SurfaceMid, ShapeM, 3s, optional action "Undo", respects nav bar insets).
- `SettingsGroup` and `SettingsRow` (56dp, title + subtitle, optional trailing switch/chevron/value). Refactor `SettingsScreen.kt` to use them.
- `AppSwitch` (token-coloured), `AppTextField` (SurfaceMid, ShapeM, focus/error states, Paste button option).
- `AppSegmentedControl` and `AppTabRow` (replace the hand-made one in Search and the Material `tabIndicatorOffset` one in Details).
- `AppIconButton` (48dp hit area, required `contentDescription` parameter that is not nullable).
- `ProgressBarThin` (Accent, 3dp) used by Continue Watching, Details resume, Downloads.
- `OfflineBanner` (slim, SurfaceMid, icon + text + optional action).

### 1.5 Enforcement
- Add `scripts/check_ui_tokens.ps1` that fails (exit 1) on these patterns outside `ui/theme/` and `ui/incognito/`: `Color\(0x`, `Color\.(White|Black|Gray|Red)`, `RoundedCornerShape\(`, `fontSize\s*=`, `\d\.sp\b`, `MaterialTheme\.typography`, `Toast\.makeText`, `bouncyClickable`, deprecated alias names, and `contentDescription = null` on anything that is clickable (heuristic: same composable has `pressable`/`clickable`). Allowlist file with justifications for legit exceptions (Trakt red).
- Add an encoding check: fail on U+FFFD or control characters (0x00-0x08) in any `.kt` file.
- Document usage in the design guide §14.

**Phase 1 acceptance:** script passes; project compiles; Home, Details, Player, Settings visually unchanged except badges (now calmer); screenshot diff reported.

---

# PHASE 2 – Trust: errors, feedback, copy, localisation

### 2.1 User-facing error mapping
Create `ui/util/UserMessages.kt`:
```kotlin
sealed interface UserError { Offline; Timeout; SourceUnavailable; NoSources; NoResults; Unknown }
fun Throwable.toUserError(): UserError
fun UserError.title(): Int  // string resource
fun UserError.hint(): Int
```
Fix all call sites:
- `HomeViewModel` (`e.message ?: "Failed to load catalog"` and `"No content available for current provider"`)
- `PlayerViewModel` (`"Stream extraction failed: …"`, `"Stream source unreachable. Please choose another server."`, `"No playable stream sources found for this title."`)
- `MediaDetailsViewModel` error state, Search, Plugin manager, Downloads, Cast.
Rules: never show `message`, `localizedMessage`, class names, URLs, IPs or status codes. Log technical detail with `Log.w` only. Each error shows **what happened + what to do** with a primary action (Retry) and, for playback, a secondary action (*Try another source*).

Copy table (use exactly, via `strings.xml`):
| Situation | Title | Hint |
|---|---|---|
| Offline | You're offline | Check your connection and try again. |
| Source failed | This source isn't working | Try another source. |
| No sources installed | No sources yet | Add a source to start watching. |
| Generic | Something went wrong | Try again in a moment. |
| No results | No results for "{query}" | Check the spelling or try another source. |

### 2.2 Snackbar + Undo replaces Toasts
- Host `AppSnackbarHost` once in `AppNavHost` above the nav bar, expose via `LocalSnackbarController`.
- Replace every non-incognito `Toast.makeText` (about 14: Settings, Cast, Player, Library, Details…).
- Add **Undo** (5s) for: remove from watchlist/favorites, remove history item, remove from Continue Watching, *Clear all* on Continue Watching/History, delete download, remove profile (soft-delete approach if the data layer already supports it; otherwise show confirmation dialog and ask me).
- Optimistic UI for watchlist/favorite toggles: update immediately, revert and show an error snackbar on failure.
- Player toasts (`PlayerViewModel.toastMessage`) become a small in-player pill (token-styled, `SurfaceMid @ 85%`) rather than system Toast.

### 2.3 Copy and jargon cleanup
- Settings: "Trakt.tv Scrobbling" → **Sync watch history**; "Auto-Scrobble to Trakt" → **Sync automatically**; subtitle "Broadcast real-time play and finish timestamps" → "Keep Trakt up to date as you watch"; "DNS-over-HTTPS (DoH)" → **Secure DNS**; "Debrid Services / Stream Resolvers" → **Premium link services**; "Installed Sources / Manage streaming source extensions" → **Sources / Add, update or remove sources**; "Card Scale Mode" → **Poster size**.
- Cast sheet: remove `IP: …`; "Copy Direct Stream URL" → hide behind an Advanced section or remove for non-debug builds.
- Move "Extra Content / Enable optional adult vault sources" out of the main list: put under **Privacy**, require profile PIN (use the existing PIN mechanism, do not invent new auth), neutral wording "Additional sources". Do not mention "vault" in visible text.
- All section headers and labels to **sentence case** (remove ALL CAPS `ACCOUNT & PROFILES`, `STORAGE`, etc.; remove 3-4sp letter spacing used for caps headings in `TopHeaderBar`, `EcosystemSettingsDialogs`, `CinematicSplashScreen` where it is only used for caps).
- Buttons: sentence case ("Try again", "Get code").
- Rename **My Library** vs **Library** so both are *Library*.
- Hand-typed `•` (Downloads, Library, other) → `MetaLine`. Fix any mojibake characters.

### 2.4 Localisation
Move all user-visible strings in files touched by Phases 1-3 to `res/values/strings.xml` with `stringResource`/`pluralStringResource` (e.g. "Delete {n} items", "{n} min left"). Name keys by screen (`home_*`, `details_*`, `player_*`, `settings_*`). Do not translate yet.

**Phase 2 acceptance:** grep for `Toast.makeText`, `.message`, `localizedMessage`, `"Stream extraction` in non-incognito UI returns no user-visible usage; every destructive/removal action has Undo or confirmation; no ALL CAPS labels remain.

---

# PHASE 3 – Core flows (behaviour)

### 3.1 Hero Play actually plays
`HomeScreen.kt` currently passes `onPlayClick = { onMediaClick(it) }` (opens Details). Change:
- **Play** → navigate to the player with resume (if `PlaybackHistoryEntity` exists), else movie or S1E1. Reuse the same logic as Details' `targetEpisodeForPlay/resumePlaybackForPlay`; extract it into a shared helper (e.g. `ui/details/PlaybackTarget.kt`) instead of duplicating.
- **More info** (secondary) → Details.
- Button label by state: *Play*, *Resume S2 E4*, *Watch again* (completed). In-progress items show `ProgressBarThin` + "38 min left" under the buttons.
- If no source can resolve, the player's error flow handles it (Phase 2 messages); do not block on Home.

### 3.2 Honest Top 10
`HomeScreen.kt` lines ~100-114 pick the row by `title.contains("Trending"/"Popular"/"Featured"/"Top")`, falling back to hero items. Required:
1. Inspect `HomeViewModel`/`HomeRow` for a real ranked list. If one exists, bind to it by an explicit row type/id, not title matching.
2. If not, **ask me** before choosing: (a) rename the row to **Trending now** and drop the numerals, or (b) keep numerals only when a ranked row exists and otherwise hide the row. Default to (b) if I do not answer.
3. Never fall back to hero items for a ranked row.

### 3.3 Navigation behaviour
- Re-tapping the active tab: first tap scrolls that tab's list to top (animated), a second tap while at top pops to the tab root. Implement for Home, Library, Downloads, Search (focus the field), Settings. Provide it in `FloatingNavbar` and `AdaptiveNavigationRail` through an `onTabReselected` callback; remove the `if (!isSelected)` swallowing.
- Preserve per-tab state: `navigate { popUpTo(start){ saveState = true }; launchSingleTop = true; restoreState = true }`; verify in `AppNavHost.kt`. Preserve scroll position (`rememberLazyListState` hoisted via `rememberSaveable`).
- Back from Details to Home/Search/Library restores scroll position and (TV) focused item.
- Cap deep chains: from Details → Related → Details... after 3 levels show a **Close** (✕) that returns to the tab root.
- Add a **profile avatar button** in `TopHeaderBar` (right side) that opens `ProfileSelectorSheet`; avatar uses the active profile.
- Align Home category chips: `contentPadding = PaddingValues(horizontal = rememberWindowDimensions().horizontalMargin)`.

### 3.4 Home loading and refresh
- Add **pull-to-refresh** (Material3 `PullToRefreshBox`) to Home and Library. Use the existing `viewModel.loadCatalog()`; show tokens-only indicator.
- Show cached Home immediately if the ViewModel already caches it (do not add networking). Otherwise show hero skeleton + row skeletons (not a single full-screen block) and let rows appear as they arrive if the ViewModel streams them; if not, keep the current approach and note it in the report.
- Per-row error: if one row fails, render an inline `ErrorState(compact)` with *Try again* for that row only, never blank the full Home. (Compact variant to add in `ErrorState`.)
- Add **long-press / ⋯ quick actions** on `PosterCard`/`MediaCard` (touch: long-press with haptic; TV: Menu key) opening `AppBottomSheet` with: Add to / Remove from list, Mark as watched, Download, Details, Share. Provide the callback plumbing without changing data logic; call existing ViewModel functions.

### 3.5 Details screen
- Default tab is **Episodes** for series, **Details & Cast** for movies. Never default to Related.
- Remember last selected season per title (presentation state, `rememberSaveable` or existing preferences if available).
- Show the primary action as state-aware (*Play / Resume S1 E3 / Watch again*) with `ProgressBarThin` and "N min left".
- Episode rows: thumbnail, title, runtime, per-episode progress bar, watched check. Add **Mark as watched / unwatched** via ⋯ menu and long-press using existing `PlaybackHistoryDao` functions through the ViewModel (if no suitable function exists, **ask me** before changing data layer).
- Instant shell: when navigating from a card, display poster, title and `MetaLine` immediately from the clicked `MediaItem`; load the rest below with shimmer. No blank screen.
- Watchlist button shows filled/outlined state and confirms via Snackbar (with Undo on removal).
- Share: verify the shared text is meaningful ("Watch {title} on Euthopiar" + deep link if one exists); otherwise share title + year only. Never share an internal id or raw provider URL.
- Add a `Trailer` button **only if** `MediaDetail` already contains a trailer URL/key; otherwise list it in "Needs product decision".
- Tablet/TV: keep two-column layout; verify the Play button gets initial focus.

### 3.6 Onboarding and empty states
- If no source is installed: Home shows `EmptyState` – title "No sources yet", message "Add a source to start watching", primary action **Add a source** opening the existing Add Source sheet. Also show it in Search, and in Library when empty.
- First run (flag in `PreferencesRepository`, presentation-only boolean): 3 short screens – (1) Welcome + profile name/avatar, (2) Add your first source (Install from file / Add link / Sync community sources, reusing the Sources sheet), (3) All set → Home. **Skip** always visible. Under 60 seconds. Do not use the words plugin, repository, EUP, extension, provider.
- Segment-specific empty states for Library tabs (Watchlist, Favorites, History) and Downloads, each with a **Browse** action navigating to Home/Search.

### 3.7 Search
- Empty query: **Recent searches** chips (each with ✕; *Clear all* link) using the existing `recentSearches` state. Optional *Trending searches* only if data exists.
- Preserve query, filters, and scroll when returning from Details (hoist to ViewModel/`rememberSaveable`).
- No-results state uses the copy from 2.1 and shows an action *Try another source* (opens the filter sheet at the source section).
- Show a result count and *Clear filters* whenever filters are active (extend the existing `ActiveFilterPill`).
- Replace the Accent-tinted filter button with neutral chrome: `SurfaceMid` background, `Outline` border; selected state uses `SurfaceHigh` + a count badge. Accent is reserved for progress/selection (per the guide).
- Hide the keyboard on scroll; focus the field when arriving via the Search tab tap and the field is empty.

### 3.8 Offline awareness
Add a read-only `ConnectivityObserver` in `ui/util` (wrapping `ConnectivityManager.NetworkCallback`, presentation only). Show `OfflineBanner` ("You're offline · View downloads") under the header on Home and Search; hide when back online with a brief "Back online" confirmation. Do not change the networking layer.

### 3.9 Library and Downloads
- Library: segments *Watchlist · Favorites · History* with counts; sort menu (Recently added / A–Z / Year); grid/list toggle remembered; swipe-to-remove in list view with Undo; history grouped *Today / Yesterday / This week / Earlier*.
- Downloads: group episodes under their show (expandable); failed items show a **plain reason** (No connection / Not enough space / Source unavailable) and a *Retry* button; low-storage warning under 1 GB; use `StatusBadge`; tapping a completed item plays directly with resume; *Delete watched downloads* action.

**Phase 3 acceptance:** each sub-task has a manual test note in the report; navigation state tests pass; hero Play launches the player with correct resume; nothing in Home shows the label "Top 10" without real ranking.

---

# PHASE 4 – Player

Only presentation and interaction in `ui/player/*`. Do not change `PlayerViewModel` playback, source selection or caching logic beyond exposing presentation state.

- **Startup feedback:** show backdrop + status text that progresses *Finding a source…* → *Starting…* after 1.5s; after 8s show *Taking longer than usual* with **Try another source**. Never a bare spinner for more than 5 seconds.
- **Failure UI:** after the existing auto-failover, show the Phase 2 message with two actions: **Try another source** (opens source sheet) and **Back**. Source list labels: *Source 1 · 1080p* (no URLs, no hostnames).
- **Controls layout:** top bar has Back, title (+ episode), and at most **3 icons** (Cast, Subtitles/Audio, ⋯). Move Speed, Sleep timer, Aspect ratio, Lock, PiP, Quality/Source into the ⋯ sheet (`AppBottomSheet`/side panel on tablet). Controls auto-hide after 3.5s while playing; never while paused, buffering, or a panel is open.
- **Scrubber:** ≥48dp touch area with thin visual track; time label above the thumb while dragging; show a thumbnail preview if frames are available from existing player APIs (else skip and note it).
- **Gestures:** keep double-tap seek, hold 2×, brightness/volume. Add a 24dp edge guard to avoid conflicts with system back gestures. Seek step (5/10/15/30s) as a setting.
- **Back behaviour:** first Back hides panels/controls; second exits.
- **Skip/Next:** show *Skip recap* where data exists; Next Episode pill appears ≥5s before end with a visible cancel; keep the 6s perimeter countdown unchanged.
- **Preferences:** remember per-title audio and subtitle choices if `PreferencesRepository` can store them (ask before schema changes). Reset speed to 1× each session.
- **Subtitle settings:** live preview sample, presets (*Default, Large, High contrast*), size slider using tokens.
- **Orientation/system:** landscape lock by default with an in-player lock toggle; restore orientation on exit; keep screen on; pause on headphones unplug; save progress every 10-15s plus on pause/back (verify existing).
- **Haptics:** audit the ~68 haptic call sites; keep thresholds and long-press confirmations only; remove ordinary-tap haptics.
- **TV:** D-pad `onKeyEvent` – Left/Right seek ±10s (hold accelerates), Up shows controls, Center toggles play, Back hides controls first, Media keys supported.

---

# PHASE 5 – Accessibility, TV, motion

### 5.1 Semantics
- Verify whether `Modifier.pressable` provides click semantics. If it uses `pointerInput` only, rebuild it on `clickable` (with `indication = null` and custom feedback) or add `semantics { role = Role.Button; onClick(label) { … } }` so TalkBack can activate it. Add a Compose UI test proving a pressable node is clickable via `performClick` and exposes `Role.Button`.
- Audit all `contentDescription = null` (46 non-incognito: `SettingsScreen` 10, `CastPickerSheet` 6, `PlayerSheets` 6, `PlayerGestures` 3, `LibraryScreen` 2, `MediaDetailsScreen` 2, `PluginManagerScreen` 2, `SearchScreen` 2 …). Actionable → meaningful, localized description. Decorative → keep null **and** add a comment `// decorative`.
- Group card semantics: poster cards read as "{title}, {year}, {type}" with `mergeDescendants = true`; progress items add "{n} percent watched". Tabs and chips expose `selected` state.
- Reading order matches visual order; headings flagged with `heading()`.

### 5.2 Targets and scaling
- Ensure ≥48dp hit areas for chips (36dp visual), scrubber, icons in the player and nav rail.
- Test at 1.3× and 2.0× font scale: Home, Details, Player controls, Settings, dialogs. Remove fixed heights on text containers; use `maxLines` + ellipsis. Fix clipping.
- Contrast: do not use `TextTertiary` for essential info on `SurfaceHigh`.
- State never by colour alone (icons/text for Ready/Failed/Active).

### 5.3 TV and large screens
- Initial focus via `FocusRequester`: Home hero Play, Details Play, Player play/pause, Search input.
- Remember focused item per row when moving between rows; restore after returning from Details.
- Hero: stop auto-advance while focus is inside or the user is touching; stop when off-screen or app backgrounded.
- TV margin: 5% safe area (replace fixed 48dp with `max(48dp, 5% of width)` inside `Dimens`/`Adaptive`, not a new literal in screens). Body text minimum equivalent of 16sp on TV via an `AppTypography` TV scale in `Adaptive`.
- Add `TvFocus` to every clickable card, chip, button, nav item, and sheet row that lacks it (currently 30 vs. 131 pressables). Avoid focus traps in sheets; Back closes sheets.
- Keyboard/mouse (tablet, ChromeOS): hover states, `Esc` = back, `Space` = play/pause.

### 5.4 Motion
- Gate hero auto-advance, Ken-Burns, shimmer sweep, scale/press animation and the navbar spring behind `LocalReduceMotion`. Extend detection to also respect system "Remove animations" and add an in-app *Reduce motion* override in Settings → Accessibility (new section).
- Replace the navbar spring with `tween(Motion.DurationStandard, StandardEasing)`. Remove the 2 remaining `bouncyClickable` usages. Keep spring only on the Play button.
- Use one shared shimmer clock for all skeletons (performance).

**Phase 5 acceptance:** TalkBack walkthrough of Home → Details → Play works; 2.0× font has no clipped text; D-pad can complete Home → Details → Play → seek → exit; reduce-motion verified.

---

# PHASE 6 – Polish (ask before starting)
1. Shared-element transition poster → Details (500ms, `Motion.DurationHero`) using `SharedTransitionLayout`; fall back to crossfade when reduce-motion is on.
2. Convert remaining `ModalBottomSheet` (5) and `AlertDialog` (14, non-confirm uses: Trakt, Simkl, Debrid, profile rename) to `AppBottomSheet`/`VelvetAlertDialog` with `PrimaryButton`/`SecondaryButton` and `AppTextField` (Paste button + *Test connection* feedback where a test function already exists).
3. Settings reorder: **Profiles → Playback → Subtitles & audio → Downloads → Sources → Accounts → Privacy → Accessibility → About**; Advanced items (Secure DNS, Premium link services, tokens) collapsed under *Advanced*. Show *Connected as {name}* for Trakt/Simkl; footer shows version + *What's new*.
4. Sources screen: status dot, language, last updated, *Update available* badge, **Test** action if existing APIs allow; plain *Not working* state.
5. Splash ≤1.2s, skipped on warm start.
6. Image handling: ensure every `AsyncImage` has `SurfaceLow` placeholder, fallback with title initial, `crossfade(220)`, and size hints.
7. Baseline profile and `key =` on all lazy lists (verify).
8. Screenshot tests (Paparazzi/Roborazzi) for Home, Details, Player controls, Settings at 1.0× and 1.3×; add to docs how to run.
9. **Update `docs/EUTHOPIAR_DESIGN_SYSTEM_GUIDE.md`:** new tokens/components, correct the navbar description (or implement the pill if I ask), refresh §15 leftovers, split "rules for new UI" from "approved backlog", add "Last verified against commit {hash}", add badge/icon/scrim/settings-row specs.

# NEEDS PRODUCT DECISION (never guess – list these in reports)
- Real Top 10 data source or rename
- Trailer data
- Soft-delete / undo support for profile deletion
- Persisting per-title audio/subtitle preferences (schema change)
- Scrubber thumbnails (data availability)
- Navbar: floating glass pill vs. flat bar

# DEFINITION OF DONE (global)
- `scripts/check_ui_tokens.ps1` passes; `:app:compileReleaseKotlin` and unit tests pass
- No raw error text, no Toasts, no ALL CAPS labels, no jargon in non-incognito UI
- Every screen implements the 7 states (loading, empty, error, offline, no results, unavailable, permission)
- TalkBack, 2.0× font, tablet, TV D-pad and reduce-motion verified
- Design guide and UX guide updated to match reality

# REPORT FORMAT (after every phase)
1. **Summary** (3-5 lines)
2. **Per-task table:** task id · status (done/partial/skipped) · files (links) · before → after in one line
3. **Verified-false findings** (items you found already fixed or wrong)
4. **Deviations and reasons**
5. **Build/test/script output** (tail)
6. **Screenshots** before/after paths
7. **Manual test checklist** for me (steps and expected result)
8. **Open questions / product decisions needed**

# START
Begin with **Phase 1 only**. Do not start Phase 2 until I approve.
