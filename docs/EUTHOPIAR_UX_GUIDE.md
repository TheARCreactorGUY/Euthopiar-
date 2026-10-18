# Euthopiar UX Guide

> Companion to `EUTHOPIAR_DESIGN_SYSTEM_GUIDE.md` (visual rules) and `DESIGN_AUDIT_REPORT.md` (compliance findings).
> The design guide says **how things look**. This guide says **how the app should behave and feel**.
> Benchmarks: Netflix, Prime Video, Apple TV, Disney+, YouTube.
> Observations come from code review and have not been tested on a device. Items marked 🔴 are gaps found in the code, 🟡 are partial, and 🟢 already work.

---

## 0. Where Euthopiar stands today

| UX pillar | Status | Evidence |
|---|---|---|
| Fast to "something playing" | 🟡 | Home → Details → Play is 2 taps (hero Play opens Details, not the player: `HomeScreen.kt:126`, `onPlayClick = onMediaClick`) |
| Resume / continue watching | 🟢 | Rail with progress, delete and clear-all; resume point computed on Details |
| Player gestures and controls | 🟢 | Double-tap seek, hold 2×, brightness/volume, PiP, cast, skip intro, next-episode countdown, audio/subtitle pickers |
| First-run experience | 🔴 | No onboarding. The app is empty until the user finds Settings → Sources |
| Error recovery | 🔴 | Raw messages (`"Stream extraction failed: …"`), "Try Again" with no alternative path |
| Feedback and undo | 🔴 | 14 Toasts, no Snackbar, no undo |
| Accessibility | 🔴 | 46 null content descriptions, no semantics roles, no key handling for D-pad |
| Personalization | 🔴 | No "Because you watched", no "My list" row on Home, no per-profile Home |
| Discovery | 🟡 | Chips, rows, Top 10 (heuristic), search with filters and recents; no trailers, no previews |
| Offline awareness | 🔴 | No offline banner or offline-first Home |
| Content trust | 🟡 | Top 10 is not a real ranking; quality badges possibly unverified |

---

## 1. Core UX principles

1. **One tap from intent to playback.** Anything between "I want to watch X" and the first frame is a cost.
2. **Never make the user wonder.** Every action shows state: loading, success, failure, or no change.
3. **Never lose the user's place.** Scroll position, tab, season, filter, playback position and selection survive navigation, rotation and process death.
4. **Failure is a normal state.** This app depends on third-party sources, so design for failure first.
5. **Content is the interface.** Chrome appears when needed and gets out of the way.
6. **Same behavior everywhere.** One pattern for confirm, delete, pick, retry and empty.
7. **Respect the user's device.** Phone, tablet, TV and low-end hardware all deserve a correct experience, not a scaled one.

---

## 2. First run and onboarding

The app has no content until a source is installed. A new user lands on a blank or error Home.

**Do**
- Show a 3-step first-run flow: **(1) Pick a profile name and avatar → (2) Add your first source → (3) Done, start browsing.**
- Make "Add source" a primary action in the Home empty state (`EmptyState(onActionClick)`), not only in Settings → Sources.
- Offer a *Skip for now* path. Never force an account login.
- Explain permissions only at the moment of use (notifications when the first download starts).
- Keep the first-run flow to under 60 seconds and 3 screens.

**Don't**
- Don't show `"No content available for current provider"` to a first-time user.
- Don't use the words *plugin, repository, EUP, extension, provider* in the flow. Use **Source**.
- Don't show a tutorial overlay on top of a populated Home.

---

## 3. Navigation and information architecture

**Structure** (current: Home · Search · Library · Downloads · Settings). This is fine for 5 tabs.

**Do**
- Tapping the active tab **scrolls to top**, and a second tap resets that tab's stack. Today `if (!isSelected) onTabSelected` ignores re-taps, which is the opposite of Netflix, Apple TV and Instagram behaviour.
- Preserve each tab's state (`saveState`/`restoreState`) and scroll position. Verify `AppNavHost.kt` uses both.
- Hide the nav bar on scroll down and show on scroll up (exists), but **always show it on tab roots when the list is at the top or the end**.
- Back from Details returns to the *exact* scroll position and focus item.
- Android back from a root tab other than Home goes to Home, then exits (currently Library/Downloads call `navigate(HOME)`, which is correct).
- Keep Details and Player full-screen without the nav bar.
- Add a profile avatar in the Home header, since profile switching is a primary action on Netflix, Prime and Disney+. It is currently buried in Settings → Profiles.

**Don't**
- Don't put source or provider switching in a header pill (the design guide already forbids it).
- Don't push more than one Details screen deep from "Related" without a visible way to get home. Cap the stack or offer *Close* (✕).
- Don't place destructive actions next to primary ones.
- Don't rely on a hidden gesture for any essential function.

---

## 4. Home

**Target:** a Netflix/Apple TV calm hero + personal rows first, generic rows later.

**Row order (recommended)**
1. Hero (max 5 items)
2. Category chips
3. **Continue watching** (profile-specific)
4. **My list** (watchlist quick row)
5. **Top 10** (only when backed by real ranking)
6. **Because you watched X** / **New episodes** (when history exists)
7. Genre and source rows

**Do**
- Hero **Play** must go straight to the player (resume if progress exists, otherwise S1E1 or the movie). A second secondary action goes to Details. Currently both go to Details, so the Play button doesn't play.
- Label the Play button by state: *Play*, *Resume S2 E4*, *Watch again*.
- Show the resume progress bar on the hero if the item is in progress.
- Pull-to-refresh on Home and Library.
- Lazy rows should show shimmer cards per row (not a full-screen skeleton) while later rows load, so the hero can appear first.
- A row failing should show an inline *Couldn't load. Retry*, and not hide the other rows.
- Cache the last Home and render it instantly (stale-while-revalidate), then refresh in the background.
- Long-press (touch) or menu button (TV) on a poster should open quick actions: *Add to list, Mark watched, Download, Details, Share*.

**Don't**
- Don't label a row "Top 10" unless it's a ranked list. Rename to *Trending now* if not.
- Don't determine Top 10 or trending by matching row titles with strings.
- Don't auto-advance hero while the user is touching, focused on, or scrolling past it, or when reduce-motion is on.
- Don't reorder rows during a session after render (layout shift).
- Don't show more than 6-8 rows before the end-of-list (use lazy loading with end-of-feed footer).

---

## 5. Search

**Do**
- Open with the keyboard focused on first entry from the nav tab.
- Empty query: show **Recent searches** (chips with ✕ to remove, plus *Clear all*) and **Trending searches**.
- Debounce 300ms, show results as the user types, cancel in-flight requests.
- Results: grouped by *Movies / Shows* with count; filters persist for the session and a visible *Clear filters* action is always shown with the active filter count (Search already does the latter).
- No-results state: say what was searched, suggest checking spelling, and offer *Search another source*. Never leave a blank grid.
- Show which source returned each result only in a secondary place (details), not on every card.
- Support IME Search action and hide the keyboard on scroll.

**Don't**
- Don't reset the query and scroll position when returning from Details.
- Don't make the user pick a source before searching. Search all active sources and merge.
- Don't block the whole screen for a slow source. Stream results in as each source answers (partial results with a small progress indicator).

---

## 6. Details

**Do**
- Backdrop with a gradient, title (or logo), `MetaLine`, a two-line synopsis with *More*, then the primary action row. Keep the primary actions visible without scrolling on a 6" phone.
- One primary button, state-aware: *Play / Resume / Play S1 E1 / Watch again*, with a thin progress bar and remaining time ("38 min left").
- Series: remember the last season. Show episode thumbnails, runtime, a per-episode progress bar, and a watched check. Add **Mark as watched / unwatched** (long-press or ⋯), and *Download season*.
- Trailer: a *Trailer* button or auto-preview muted backdrop after 2s (respect data saver and reduce motion). This is a core Netflix/Prime/Disney+ feature that is currently absent.
- Show real availability: audio languages, subtitle languages, quality (4K/HDR) as ≤2 small text badges.
- Cast: tap a person to see their filmography (search by actor).
- Share: sharing must produce a useful link or text, not just an internal ID. Verify the share intent payload.
- Related and cast rails lazy-load and fail silently with a retry.

**Don't**
- Don't make the user choose a server/quality before playing. Auto-pick and keep *Change source* inside the player.
- Don't show a hard blank while metadata loads. Show the poster and title immediately (from the clicked item) and fill in the rest.
- Don't hide the Watchlist state. The icon must switch filled/outlined and confirm via Snackbar.
- Don't show a fixed tab order that opens on *Related* for a series. Default to *Episodes*.

---

## 7. Player

The player is the strongest area. Raise it to Netflix level.

**Do**
- Time to first frame: show a poster/backdrop with a spinner for ≤1s, then a **plain-language status** ("Finding a source…" → "Starting…") if it takes longer. Never a bare spinner for >5s.
- On source failure **auto-fail over silently** to the next source (exists in code at `PlayerViewModel.kt:2142`), tell the user after 2 failures with a one-tap *Try another source*, and list sources with plain labels (*Source 1 · 1080p*).
- Controls: auto-hide after 3-4s while playing, never while paused, buffering, or a sheet is open. Tap toggles. Keep the control layout identical between phone and tablet.
- Scrubber: show a **thumbnail/time preview** on drag, and a 48dp-high touch area even though the track is thin. Haptic tick on chapter or skip-intro boundaries only.
- Gestures: double-tap ±10s with accumulated ripple (exists). Add configurable seek step. Hold for 2× speed shows a persistent pill. Brightness (left) and volume (right) drag show HUD (exists).
- Always display *Skip intro* and *Skip recap* when data exists, and *Next episode* ≥ 5s before the end with a visible cancel. Keep the 6-second perimeter countdown (design guide §5).
- Remember per-title choices: audio language, subtitle language and size, and last speed reset to 1× per session.
- Subtitles: default to profile language, style preview in settings with a live sample, and offset ±0.1s steps.
- Orientation: lock to landscape while playing by default, with an in-player *Lock rotation* option; return to the previous orientation on exit.
- Keep the screen on, honour audio focus, pause on headphones unplugged, resume on focus gain.
- Save progress every 10-15s and on pause, back, and process death.
- PiP and background audio-only for downloads where the platform allows.
- Back button: first press hides sheets/controls, second exits. Never exit on the first press while a panel is open.

**Don't**
- Don't show `"Stream extraction failed: …"` or `Stream source unreachable. Please choose another server.` Use: *"This source isn't working right now. Try another one."* with action buttons.
- Don't put 3+ rows of icons in the top bar. Group extras under a ⋯ menu (*Speed, Sleep timer, Aspect, Lock, Cast*).
- Don't use dialogs inside the player. Use side panels or sheets that don't pause video unexpectedly.
- Don't use raw white/black, ad-hoc colors, or new fonts for the HUD. Use tokens.
- Don't let a gesture trigger accidentally near screen edges (keep a 24dp edge guard for system back gestures).

---

## 8. Library

**Do**
- Three clear segments: *Watchlist · Favorites · History*, each with a count.
- Each segment: sort (*Recently added / A-Z / Release year*), grid or list toggle remembered.
- Swipe-to-remove (list view) with a Snackbar **Undo**. Long-press enters multi-select.
- Empty states must be segment-specific: "Your list is empty. Tap + on any title to save it for later" + *Browse* action.
- History: group by *Today / Yesterday / This week / Earlier*, with per-item *Remove*. *Clear history* requires confirmation and offers undo for 5 seconds.
- Offer *Mark as watched* and *Continue* actions on history items.

**Don't**
- Don't call the tab "Library" in the navigation and "My Library" in the title. Pick one name.
- Don't delete on a single tap. Don't ask confirmation for single-item removal if Undo is provided.

---

## 9. Downloads

**Do**
- Group by show; expand to episodes. Show size, status, and remaining time.
- A global storage bar (exists) with a clear warning when below 1 GB free.
- Quality chooser on start with the estimated size per option (exists via `DownloadQualitySheet`; confirm it displays size).
- Failed downloads: reason in plain words (*No connection / Not enough space / Source unavailable*) with a *Retry* button.
- Wi-Fi-only default **on**, and prompt *Download anyway?* when on mobile data.
- Smart cleanup: *Delete watched downloads* and a setting for auto-delete.
- A system notification with Pause/Resume and progress.
- Tapping a completed item plays immediately (resume) without a Details detour.
- Downloads must work (and Home should surface them) when offline.

**Don't**
- Don't display only "Failed" with no reason or action.
- Don't let downloads silently stop when the app is backgrounded.
- Don't use the term *vault* or leak file paths.

---

## 10. Settings and Sources

**Do**
- Order by user value: **Profiles → Playback → Subtitles & audio → Downloads → Sources → Accounts → Privacy → About**. Today Account/Sources come first. Daily-use settings are lower.
- Use sentence-case headers ("Playback"), not ALL CAPS.
- Every row: title + one-line plain-language subtitle; toggles apply immediately with a subtle confirmation.
- Technical options (DoH, Debrid, API tokens) go under **Advanced**, collapsed by default.
- *Connect Trakt* via the code flow with a copy/open button and live status. Show account status as *Connected as <name>*.
- Destructive actions (Clear cache, Sign out) require confirmation and say what will be lost.
- Show app version and a *What's new* link.
- Sources screen: each source shows status, language, last-updated, and a **Test** button. Show a clear *Last worked: 2h ago* or *Not working* line. Add an *Update available* badge.

**Don't**
- Don't use *Scrobbling, DoH, Debrid, Resolver, Broadcast timestamps*. Use *Sync watch history*, *Secure DNS*, *Premium link services*.
- Don't put adult content toggles in the main list. Place them behind a profile PIN under Privacy, and never name it *vault* in the open.
- Don't make users type long tokens without a *Paste* button and *Test connection* feedback.

---

## 11. Profiles and safety

**Do**
- Profile chooser on cold start when more than one profile exists, with the last profile preselected.
- Profile avatar visible on Home. Switching takes ≤2 taps.
- Kids profile: filtered catalog, no Settings access, PIN to exit.
- PIN entry: 4 boxes + numeric pad, error shake, lockout after 5 attempts, haptic on failure.
- Each profile has its own history, watchlist, subtitle and audio preferences.

**Don't**
- Don't expose the incognito module through the normal UI paths. It must be reachable only through the deliberate hidden entry and PIN, and it must never show up in recents, notifications or screenshots (`FLAG_SECURE`).

---

## 12. Feedback, states and microcopy

### 12.1 Every screen must handle seven states
Loading (shimmer) · Empty · Error · Offline · No results · Permission denied · Content unavailable.

| State | Pattern |
|---|---|
| Loading | Shimmer shaped like final content. Never a spinner alone for lists |
| Empty | Icon, one-line title, one-line reason, one action |
| Error | What happened + what to do + Retry |
| Offline | Persistent slim banner *You're offline. Showing saved items.* + Downloads shortcut |
| No results | Echo query, tips, alternative action |
| Unavailable | *This title isn't available from your sources right now.* + *Try another source* |

### 12.2 Feedback
- Success: Snackbar (3s), with **Undo** for reversible actions.
- Blocking errors: inline in the screen, not a toast.
- Haptics: only on thresholds and long-press confirmations (design guide §5). The code has 68 haptic call sites, so audit them.
- Use optimistic UI for watchlist toggle: update instantly and roll back on failure.

### 12.3 Microcopy
| Instead of | Write |
|---|---|
| Stream extraction failed: … | This source isn't working. Try another. |
| No content available for current provider | Nothing here yet. Add a source to get started. |
| Failed to load catalog | Couldn't load. Check your connection and try again. |
| Try Again | Try again (sentence case) |
| Copy Direct Stream URL | Copy link (hide for most users, move to Advanced) |
| IP: 192.168.x.x | Remove |
| Scrobble | Sync watch history |
| Debrid Stream Resolvers | Premium link services |

Rules: sentence case, max ~12 words per message, active voice, never blame the user, one verb per button. Move all strings to `strings.xml` (0 `stringResource` today).

---

## 13. Performance as UX

| Target | Budget |
|---|---|
| Cold start to interactive Home | < 2s (cached content) |
| Tap to visual response | < 100ms |
| Scrolling | 60fps, no frame > 16ms on mid-range device |
| Details open (shell with art) | < 300ms |
| Play tap to first frame | < 3s median |

**Do**
- Prefetch the next Details and Player sources when a poster is focused for >300ms (TV) or pressed.
- Coil memory + disk cache, correct downsizing, and fixed-aspect placeholders to avoid layout shift.
- Keep the splash ≤ 1.2s and skip it on warm start. The splash animation should not delay content.
- Use baseline profiles and `key =` for all lazy lists (rows already do this).
- Stop hero auto-advance and animations when off-screen or the app is backgrounded.

**Don't**
- Don't block first paint on network. Don't run shimmer on 5+ rows at once on low-end devices (use one skeleton animation clock).

---

## 14. Accessibility and inclusive UX

**Do**
- Every actionable icon has a localized `contentDescription`. Decorative icons use `null` deliberately and nothing else.
- Make `pressable` expose `Role.Button` and `onClick` semantics (or switch to `clickable` + custom indication). **Verify with TalkBack**, because the code has zero `semantics` calls.
- Contrast ≥ 4.5:1. `TextTertiary #8A8A96` on `#0B0B0F` ≈ 5.5:1, which is fine for captions; do not use it for essential info on `SurfaceHigh`.
- 200% font scale tested for Home, Details, Player controls, and Settings.
- Never use color alone: add icon or text for state (Ready/Failed).
- Subtitles: size, color, edge/background, and position options; provide presets (*Default, Large, High contrast*).
- Respect system reduced motion. Offer an in-app override.
- Captions-on preference per profile; audio-description track if available.

**Don't**
- Don't make 36dp chips the hit area (the guide says hit area ≥48dp; verify).
- Don't auto-dismiss messages containing actions faster than 5s.
- Don't rely on long-press as the only path to an action (provide a visible ⋯).

---

## 15. TV and large screens

Today TV support is a focus ring (`tvFocusable`) and a nav rail. **No D-pad key handling and almost no `FocusRequester`s.**

**Do**
- Set initial focus: Home → Play on hero; Details → Play; Player → Play/pause; Search → input field.
- Remember the focused item per row when moving up and down (Netflix behaviour).
- D-pad on Player: ◀▶ seek ±10s (hold to accelerate), ▲ shows controls, OK pauses, Back hides controls first.
- Menu or long-press OK opens quick actions on any poster.
- Use a 5% overscan-safe margin and a 10-foot type scale (minimum body 18sp equivalent).
- Hero never auto-advances while focus is inside it.
- Tablet/foldable: two-pane Details, list-detail Library, and `NavigationRail` (exists). Handle posture and landscape phones (hero height cap, ≤ 60% of height).
- Support keyboard and mouse (hover states, Esc = back, Space = play/pause) for ChromeOS and tablets.

**Don't**
- Don't use touch-only gestures (swipe/pinch) as the sole path on TV.
- Don't show hover/press-only affordances. Don't create focus traps in sheets.

---

## 16. Motion

**Do**
- Hero → Details: shared-element poster expansion (500ms, promised by the design guide, not implemented).
- Use the same duration tokens everywhere. Screens enter with a fade+slide ≤ 300ms.
- Crossfade images (220ms). Animate list item add/remove with `animateItem`.
- Gate all non-essential animation behind `LocalReduceMotion`.

**Don't**
- Don't animate on every scroll frame (parallax, Ken-Burns) on low-end devices.
- Don't mix springs and tweens in the same component family.
- Don't block interaction while animating.

---

## 17. Trust, legality and content hygiene

- Never promise rankings, ratings or availability that aren't real. If a number is shown, it must come from a source.
- State clearly in Settings → About that Euthopiar doesn't host content, with a plain explanation of sources.
- Age-ratings: show the rating on Details and block via the profile limit.
- Don't ship adult-source toggles in the same settings list as general sources.
- Analytics/crash reporting (if any) need a clear opt-in.

---

## 18. Quick Do / Don't cheat sheet

| ✅ Do | ❌ Don't |
|---|---|
| Hero Play plays | Hero Play opens Details |
| Resume from last position, always | Restart from 0 silently |
| Auto-fail over to the next source | Show server errors |
| Undo via Snackbar | Confirm every small action |
| Persist scroll, tab, filters | Reset state on back |
| Show shimmer shaped like content | Blank screen or lone spinner |
| Plain language, sentence case | Jargon, ALL CAPS, raw exceptions |
| Real rankings or honest labels | "Top 10" from a string match |
| Test with TalkBack, 200% font, D-pad | Assume touch-only |
| One pattern per interaction | Per-screen custom dialogs |
| Tap active tab = scroll to top | Ignore tab re-tap |
| Put profile and quick actions one tap away | Bury them in Settings |

---

## 19. Roadmap to "Netflix-grade" (UX only)

**Phase 1 – Trust (1-2 weeks):** friendly errors everywhere, Snackbar + Undo, hero Play → player, tab re-tap scroll-to-top, accessibility semantics, rename Top 10.
**Phase 2 – Retention (2-4 weeks):** onboarding, profile avatar on Home, My list and "Because you watched" rows, mark watched, pull-to-refresh, offline banner, cached Home.
**Phase 3 – Delight (4-8 weeks):** trailers/previews, scrubber thumbnails, shared-element transitions, long-press quick actions, per-title audio/subtitle memory, TV D-pad completeness.
**Phase 4 – Scale:** localization, screenshot tests, performance budgets in CI, A/B-able row ordering.

---

## 20. Acceptance checklist for any screen

- [ ] One clear primary action, reachable in ≤ 2 taps from Home
- [ ] All 7 states designed
- [ ] State persists across back, rotation and process death
- [ ] No jargon, no raw exceptions, sentence case, strings in `strings.xml`
- [ ] Undo instead of confirm for reversible actions
- [ ] TalkBack labels and roles verified
- [ ] 200% font scale and tablet width verified
- [ ] D-pad: initial focus, logical order, Back behaviour
- [ ] Reduce-motion honoured
- [ ] Cold and warm load times within the budget in §13

---

## 21. Private space checklist & privacy requirements

The private space (`ui/incognito/*`) adheres to consumer-grade discretion and the same design system as the main app, with strict privacy, security, and leak-prevention boundaries.

### 21.1 Naming and Visual Identity
- **Naming (D1):** Visible area name is **Private** or **Private space**; saved items are **Saved**. Never use terms like "Vault", "Quarantined", or explicit provider tags in the UI.
- **Tokens (D2):** Uses the exact same Obsidian theme palette (`Background`, `Surface*`, `Text*`, `Accent`, `Outline`) as the primary application. No separate or loud styling; discretion is achieved through calm, neutral UI.

### 21.2 Entry, Authentication & Triggers
- **Single Entry Gate:** All entry attempts must pass through `PrivateSpaceEntry.request(...)`.
- **Kids Profile Protection (D6):** Hard-blocked when `UserProfile.isKids == true`; trigger queries are completely ignored.
- **Age Gate (V2):** One-time discreet `PrivateSpaceAgeGateSheet` explaining 18+ requirement, zero history recording, and auto-lock behavior before authentication.
- **Biometric / Device Auth:** Device screen lock or biometric verification via `VaultAuthManager`. Silent, discreet return to the previous screen on cancellation (zero toasts or error banners).
- **Trigger Discretion (D5):** Plain adult words (`fap`, `faphouse`) are removed. Only `::private` (and legacy `::vault`, `::incognito`) trigger entry. Trigger queries are wiped from the search input and never written to recent searches or search history.
- **Settings Master Switch:** Adult sources toggle under Settings > Privacy is the master switch. When disabled, trigger queries produce standard search results with zero auth prompts or mode hints.

### 21.3 Session Lifecycle & Auto-Lock (D3)
- **Privacy Curtain:** Solid opaque `Background` privacy curtain activates immediately on `ON_PAUSE` covering notification shade, dialogs, app switcher, and incoming calls.
- **Auto-Lock Grace Timer:** Starts on `ON_STOP` (default: 30 seconds). Returning within the timeout restores the exact screen without re-auth. Returning after timeout purges in-memory session and returns to main app Home.
- **Picture-in-Picture:** Entering PiP keeps the playback session active without triggering auto-lock. Exiting PiP back into the app smoothly restores player controls.
- **Back Navigation:** Pressing Back on any private root tab exits the private space immediately with zero residue.

### 21.4 Quick Exit (V4)
- **Persistent Exit Button:** Standard `AppIconButton` with Close icon located top-right on every screen (Feed, Categories, Search, Saved, Creator, and Player Controls). Instant crossfade exit to normal Home.
- **Panic-Flip Sensor:** Accelerometer-based flip detector triggers instantaneous exit with tactile haptic tick when the device is placed face-down.
- **Session Purge on Exit:** Player released, PiP closed, soft keyboard dismissed, in-memory caches, recents, and search queries purged, and main navigation back stack reset to `HOME`.

### 21.5 Storage & Cache Leak Prevention (V9)
- **Window Protection:** `FLAG_SECURE` active continuously across the session, blocking screenshots, screen recording, and Recent apps snapshot thumbnails.
- **Backup Exclusion:** `backup_rules.xml` and `data_extraction_rules.xml` exclude private bookmarks, download files, and preferences from cloud backup and device transfer.
- **In-Memory Image Loader:** `PrivateImageLoader` with `diskCache = null` ensures video thumbnails are cached in memory only and completely cleared on session exit.
- **Discreet Notifications:** Download notifications use neutral title "Downloading" with generic progress, zero titles or thumbnails, and `VISIBILITY_SECRET` on lock screen.
- **Zero Cross-Contamination:**
  - Private playback never writes to Room/DataStore history, TMDB bridge, Trakt/Simkl scrobblers, or AniSkip.
  - Private content never appears in Continue Watching, main Library, main Search recents, or main Downloads tab.
  - Zero titles, URLs, query strings, or IPs written to logcat from private paths.

