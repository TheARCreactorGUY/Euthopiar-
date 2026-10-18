# Agent Prompt – Insert Phase: Private Space (Vault / Incognito) UI/UX

**When to give this to your agent:** after the agent has finished and you approved **Phase 2** (it needs the Phase 1 tokens/components and the Phase 2 error mapper, Snackbar and copy rules), and **before Phase 3**. Call it **Phase 2.5**. If the agent is already past Phase 3, give it any time; it only depends on Phases 1-2.

Copy everything below the divider into your agent.

---

# PHASE 2.5 – Private space (`ui/incognito/*`) UX, privacy and design-system alignment

## Context
You are continuing the Euthopiar UI/UX overhaul. Phases 1-2 are complete and approved (tokens, shared components, `AppSnackbarHost`, `UserMessages.kt`, copy rules). The earlier rule "do NOT touch `ui/incognito/`" is **lifted for this phase only**, for the tasks below. After this phase it applies again to anything not listed.

The private area (internally "Faphouse / Incognito / Vault") is the part of the app that was left out of the audit: it is in the design guide's "known leftovers" and was never given a UX review. It deserves the same quality bar as the main app **plus** a stronger privacy and discretion bar, because mistakes here are embarrassing for the user, not just ugly.

Read first, in addition to the earlier documents: `docs/EUTHOPIAR_UX_GUIDE.md` §11 (profiles/safety), §12 (states/copy), and every file in `ui/incognito/` (`FaphouseVaultScaffold`, `IncognitoModeManager`, `VaultAuthManager`, `FaphouseFeedScreen`, `FaphouseCategoriesScreen`, `FaphouseDetailsScreen` (≈2000 lines, contains its own player), `FaphouseSavedScreen`, `FaphouseSearchOverlay`, `FaphouseCreatorScreen`, `FaphouseCard`, `FaphouseFloatingNavbar`, `FaphouseViewModel`, `VaultBookmarksRepository`, `VaultDownloadManager`).

## Hard boundaries (still apply)
- Do NOT modify: `core` module (including `FaphousePlugin`, other adult provider plugins, networking, DoH, extractors), plugin/EUP loading, Room entities/DAOs/migrations, download engine internals.
- Do NOT change scraping, parsing, or provider selection logic in `FaphouseViewModel`; presentation state only.
- Do NOT change existing token values; additive only.
- Preserve existing comments/KDoc unrelated to your change.
- **Do not add any new analytics, logging of titles/queries/URLs, or network calls.** No query text, titles or URLs may be written to logcat from this module (check existing `Log.*` calls; remove or redact any that print them).
- Where a task touches data/security/persistence (marked **[DECISION]** or **[VERIFY]**), first report what you found and ask before changing behaviour. Manifest/resource-only changes (backup rules, notification visibility) are allowed once confirmed.
- One commit per task: `private-space: V<n> <summary>`.

## Verify first (before any edit) – report findings in a short "Findings" message and continue unless a finding is marked blocker
The static audit found the following. Confirm each in code; say "not reproducible" where wrong.

**Design-system compliance (counts in `ui/incognito/`):** 75 raw `Color(0x…)`, 15 `Color.White/Black/Gray`, 33 `RoundedCornerShape(`, 62 `n.sp` literals, 53 `fontSize`, 11 `bouncyClickable`, 0 `pressable`, 0 `tvFocus`, 0 `EmptyState/ErrorState`, 0 shimmer, 7 `CircularProgressIndicator`, 13 `contentDescription = null`, 0 `stringResource`, deprecated aliases (`VelvetBlack`, `PureWhite`, `TextMuted`, `RefractiveGlassBorder`) in use, background is `VelvetBlack` instead of `Background`.

**Session and privacy behaviours:**
1. `FaphouseVaultScaffold` calls `IncognitoModeManager.disableIncognito()` on **`ON_PAUSE` or `ON_STOP`**. `ON_PAUSE` fires for the notification shade, system dialogs, permission dialogs, share sheet, cast picker, multi-window focus change, incoming-call heads-up and **entering Picture-in-Picture**. Likely effects: the private session is killed abruptly (user dumped to the main app mid-video), PiP from the private player is probably broken. Reproduce and report.
2. Entry is a hidden search trigger in `SearchScreen.kt:137` and `InPluginSearchOverlay.kt:71`: typing `::fap`, `::incognito`, `::vault`, `faphouse` or plain `fap` calls `enableIncognito()`. Confirm whether `VaultAuthManager.authenticate(...)` is invoked before it at both sites. Confirm the trigger text is **not** saved into recent searches/search history or left in the search field.
3. `VaultAuthManager`: if the device has no secure lock screen it calls `onSuccess()` silently; fallback to keyguard also allows entry when the intent is null. Titles read "Incognito Vault".
4. `AndroidManifest.xml` has `android:allowBackup="true"`. Find where `VaultBookmarksRepository` and vault downloads/metadata are stored and whether they are included in backups/device transfer.
5. Image cache: check whether vault thumbnails go through the shared Coil disk cache and whether the shared Media3 cache (`StreamingPlaybackCacheManager`) stores vault streams. Cached thumbnails/segments survive exiting the private space.
6. Notifications: check what the download notification shows for vault downloads (title/thumbnail) and its lock-screen visibility.
7. Confirm vault playback never calls: history save (guarded in `PlayerViewModel.savePlaybackProgress` for providerName "Faphouse"/incognito), Trakt/Simkl scrobble, TMDB bridge, AniSkip, or writes to the main Continue Watching/Library/Downloads lists. Confirm vault downloads never appear in the main Downloads tab.
8. Check whether private mode is reachable from a **Kids** profile (`UserProfile.isKids`).
9. `FLAG_SECURE` is set in `AppNavHost` while incognito is active: confirm it also covers the player, dialogs, sheets, PiP and the Recents thumbnail, and that it is cleared on exit.

## Decisions to ask me (list in the Findings message; propose a default; wait if blocking)
| # | Decision | Proposed default |
|---|---|---|
| D1 | Visible name of this area | **Private** (never "vault", "quarantined", "adult" in UI) |
| D2 | Visual identity | Same tokens as the main app (no separate palette); discretion by calm, neutral UI |
| D3 | Lock policy | Curtain on pause, auto-lock after a grace period (Immediately / 30 s / 1 min), default **30 s**; PiP exempt |
| D4 | Devices without a lock screen | Allow with a one-time notice "Set a screen lock to protect this area", user can proceed |
| D5 | Plain-word triggers (`fap`, `faphouse`) | Keep only `::private` (and `::vault`, `::incognito` for backward compatibility); remove plain words to avoid accidental activation |
| D6 | Kids profiles | Entry disabled and trigger ignored |
| D7 | In-session "resume" | Remember positions in memory only, cleared on exit |

---

## Tasks

### V1 – Entry and authentication flow
1. All entry points must go through one function, e.g. `PrivateSpaceEntry.request(activity, profile, onGranted)`; **never** call `enableIncognito()` directly from `SearchScreen`/`InPluginSearchOverlay`. The function: checks the profile is not Kids (D6) → shows the 18+ confirmation (V2) if first time → calls `VaultAuthManager.authenticate` → on success enables the space.
2. After a trigger is recognised: **clear the search field, do not save the trigger to recents**, dismiss the keyboard, then navigate. If auth is cancelled, return to the previous screen exactly as it was with no message and no error (silent, discreet).
3. Authentication copy becomes neutral: prompt title "Confirm it's you", subtitle "Unlock your private space"; keyguard fallback text likewise. No "Incognito Vault" text anywhere.
4. Devices without secure lock (D4): show a `VelvetAlertDialog` once ("Protect this space"/"Set a screen lock to keep this area private." → **Continue** / **Cancel**) and remember the acknowledgement in preferences only if I approve; otherwise show every time.
5. Settings (the main Settings from Phase 2.3): the adult-sources toggle moved under **Privacy** is the master switch. If it is off, the trigger does nothing at all (no auth prompt, no hint). Add under the same group: *Require unlock* (existing `vaultBiometricLockEnabled`), *Quick exit by flipping device* (existing `vaultPanicFlipEnabled`), *Auto-lock* (D3). Use `SettingsRow` with plain-language subtitles; no jargon.

### V2 – First-time experience (age gate and expectation setting)
- One-time `AppBottomSheet`: title "Before you continue", body 3 short bullets: **You must be 18 or older / Nothing here is saved to your watch history / Screenshots are blocked and the space locks when you leave the app**, buttons **I'm 18 or older** (primary) and **Cancel**. Store the acknowledgement via the existing preferences mechanism (ask before adding a key).
- No decorative hero, no explicit imagery in this sheet.

### V3 – Session, lifecycle and auto-lock (replace the "ON_PAUSE kills session" behaviour)
Implement the D3 policy in `FaphouseVaultScaffold`/`IncognitoModeManager` (presentation/session logic only):
1. `ON_PAUSE` → show an opaque **privacy curtain** (solid `Background`, no content, no title) immediately; do not purge state, do not exit. This covers notification shade, dialogs, share, cast and app switcher.
2. `ON_STOP` → unless the activity is in PiP, start the grace timer. If the user returns within the timeout, remove the curtain and restore the exact screen. After the timeout (or `ON_DESTROY`/process death), run the existing purge (`disableIncognito()`, `viewModel.clearDetail()`, clear in-memory lists/search text/recents) and, on next return, show the **main app Home**, not the private space.
3. PiP: entering PiP from the private player must keep the session alive; the PiP window may not show anything other than the video; leaving PiP back into the app restores the player. If PiP cannot be made safe, disable the PiP button inside the private space and report.
4. Rotation/config change must not trigger lock (`configChanges` already handled; verify).
5. Back behaviour: Back on a private root tab → exits the private space to **Home** of the normal app with no residue; Back inside Details → previous private screen (as now). Add `BackHandler` ordering so sheets/dialogs close first.
6. Add a unit test for the lock state machine (pause → curtain; stop within grace → resume; stop beyond grace → purged).

### V4 – Quick exit and discretion
- Persistent **Exit** button (icon `Close`/`Lock`, `AppIconButton`, content description "Exit private space") top-right on every private screen, including Details and the player controls (today the Exit tab is hidden on Details). One tap, no confirmation, instant (`Motion.DurationMicro` crossfade) to the normal Home. 
- Wire the existing panic-flip preference to the same exit, with a haptic tick and nothing else (no toast).
- Exit must clear: current player (stop and release), PiP, keyboard, in-memory state, and ensure the main back stack's top is Home.
- Do not display "Private" watermark text over content; instead keep a **small neutral indicator** (lock glyph in the header) so the user knows the mode they are in.
- Notifications, share sheets, window title and Recents label must never contain vault names or titles (V9).

### V5 – Design-system alignment of every private screen
Apply Phase 1 rules to `ui/incognito/*` (the check script from Phase 1 must now include this folder; remove the exclusion):
- Replace raw colors with tokens; `VelvetBlack` → `Background`; surfaces `SurfaceLow/Mid/High`; remove `RefractiveGlassBorder`, `PureWhite`, `TextMuted`, `StarGold`.
- Replace `fontSize`/`.sp` with `AppTypography` roles; replace `RoundedCornerShape(n.dp)` with `ShapeS/M/L/Full`.
- `bouncyClickable` → `Modifier.pressable { }`; add `tvFocusable` to every clickable card/button/chip/nav item.
- Replace `FaphouseCard` with the shared **`BackdropCard`** (16:9) + `MetaLine` (duration · views/quality), 8dp gaps, `ShapeS`, `SurfaceLow` placeholder, `ShimmerSkeleton` while loading, fallback image.
- Replace `FaphouseFloatingNavbar` with the app's `FloatingNavbar` pattern (same height 64dp, rail on tablets/TV), tabs: **Home · Categories · Search · Saved**; remove the in-bar EXIT tab (exit lives in the header per V4).
- Replace custom empty/error UIs with `EmptyState`/`ErrorState` + Phase 2 messages; spinners only for player buffering.
- Replace custom dialogs/sheets with `AppBottomSheet`/`VelvetAlertDialog`; segmented controls with `AppSegmentedControl`.
- Remove the "Top 10 in Vault Today" row unless backed by a real ranking (same rule as Home 3.2); otherwise name it "Popular now".
- Reduce badge noise: ≤2 badges per card using the Phase 1 badge component.
- Honor `LocalReduceMotion`; remove any non-Play springs; loading uses one shared shimmer clock.

### V6 – Copy (neutral, calm, no jargon, sentence case, `strings.xml`)
Create `res/values/strings_private.xml` and use `stringResource`. Required rewrites (use these unless I change D1):
| Current | New |
|---|---|
| Incognito Vault / Vault / Vault Library | Private / Private space / Saved |
| Quarantined & Private | Only on this device |
| Content Provider | Source |
| Switch between adult content libraries | Choose where videos come from |
| Switch Provider / Switch Layout | Change source / Change layout |
| Top 10 in Vault Today | Popular now (or hidden, see V5) |
| Feed Unavailable | Couldn't load videos |
| Search 2,000,000+ clips... | Search |
| No clips found for "{q}" | No results for "{q}" |
| Type any keyword or category to search | Search by title, creator or category |
| No Saved Bookmarks / Tap the bookmark icon… | Nothing saved yet / Tap the bookmark on any video to keep it here |
| No Offline Downloads / …protected with .nomedia… | No downloads yet / Downloads stay in this app and don't appear in your gallery |
| Offline Ready | Downloaded |
| Stream Quality / Select your preferred playback resolution | Quality / Choose video quality |
| 2X SPEED | 2× speed |
| Clear All | Clear saved (confirmation + Undo) |
| A-B Loop, PiP, Aspect | Repeat section, Picture-in-picture, Aspect ratio (accessibility labels and ⋯ menu) |
Remove the unverifiable "2,000,000+" statistic. Never expose provider URLs, `.nomedia`, "stream extraction" or exception text (use `UserMessages`).

### V7 – Screen-by-screen UX
- **Home (Feed):** hero removed or limited to a calm single featured row (no autoplay); rows via the shared `ContentRow`; pull-to-refresh; per-row inline error; layout toggle remembered for the session; source switcher is a bottom sheet from the header "Source" chip (not a hidden icon). Ensure first paint ≤ 2 shimmer rows then content.
- **Categories:** responsive grid (2 phone / 3-4 tablet / 5 TV) of 16:9 tiles with the category name below (no text overlaid on imagery where unreadable; use scrim token), with search-within-categories; Back returns to the same scroll offset.
- **Category/Creator pages:** header with name, count if available, sort menu (Popular / Newest if supported by existing view-model), infinite scroll with a shimmer footer, end-of-list footer.
- **Search:** results appear as the user types (300 ms debounce if the ViewModel supports it, else on IME Search); **recents are in-memory only and cleared on exit**, shown as removable chips; no-results and error states per V6; keyboard hides on scroll; autoCorrect disabled and no learning suggestions for the field (`KeyboardOptions(autoCorrectEnabled = false)`).
- **Saved:** `AppSegmentedControl` *Bookmarks · Downloads* with counts; swipe/⋯ to remove with **Undo** snackbar (Phase 2 host); *Clear saved* confirmation; download items use `StatusBadge`, size, progress bar, *Retry* with plain reason; tap plays offline immediately.
- **Details:** instant shell (thumbnail + title + `MetaLine`), actions row (Play primary, Save, Download, Share disabled/hidden for private content, see V9), related videos rail using `BackdropCard`, creator link. Remove duplicate controls shown both in the info area and in the player.
- **Creator:** header (avatar, name, type), tabs if more than one list exists, same grid rules.

### V8 – Private player (inside `FaphouseDetailsScreen`)
The file is ~2000 lines and contains a separate player UI. Goal: parity with the main player's UX rules (Phase 4) without changing playback behaviour.
1. **Refactor safely:** split the file into `FaphouseDetailsScreen` (info) and `FaphousePlayerOverlay`/`FaphousePlayerControls` with no behaviour change; or **reuse** the main `PlayerControls`/`PlayerGestures` components if feasible (preferred). If reuse needs ViewModel changes, stop and ask.
2. Controls: top bar = Back, title, and ≤3 icons (Quality, Save, ⋯); move Repeat section (A-B loop), Aspect ratio, Mute, Download, Speed, PiP into the ⋯ sheet. Auto-hide after 3.5 s while playing; never while paused/buffering/sheet open.
3. Scrubber: ≥48dp touch area, time label above thumb while dragging, tokens for all colors/text (`timeLabel`).
4. Gestures: double-tap ±10 s with ripple, hold 2× with persistent pill ("2× speed"), brightness/volume drag, 24dp edge guard; same behaviours as the main player.
5. Startup: backdrop + status text ("Starting…"), after 8 s "Taking longer than usual" + **Try another quality/source**; failure uses `UserMessages` with Retry/Back. Never a bare spinner > 5 s.
6. Back: first Back hides controls/panels, second exits to Details.
7. Orientation: landscape lock by default with a lock toggle; keep screen on; pause when headphones unplug; audio focus handled (verify).
8. **No progress persistence beyond memory** (D7); remove any call that writes position to Room/DataStore/main history. The Skip-intro 6-second perimeter countdown is not used here, so do not add it.
9. Quick-exit button remains visible with controls; hidden when controls hide, reappears on tap.

### V9 – Leak prevention (verify, then fix what is presentation/manifest/resource only)
For each item: **Finding → Fix → Test**. Anything touching repositories or cache internals: ask first.
1. `FLAG_SECURE` active for the whole private session, including player, sheets, dialogs, PiP; cleared on exit. Test with screenshot, screen recording, and Recents.
2. `allowBackup`: exclude vault bookmarks/downloads/metadata and any private preferences from backup and device transfer via `dataExtractionRules` and `fullBackupContent` XML (or set `allowBackup=false` if I approve). 
3. Image/stream caches: use an **in-memory-only Coil `ImageLoader`** (`diskCache = null`) for private screens via a dedicated `LocalImageLoader`/parameter; purge on exit. For the shared Media3 cache, propose a plan (separate cache dir with purge on exit) and ask before implementing.
4. Notifications for private downloads: title "Downloading" (no video title, no thumbnail), `VISIBILITY_SECRET` on lock screen, channel named neutrally. Include completion notifications.
5. Share/cast: hide Share for private items; Cast shows a confirmation sheet ("Show this on another screen?") with **Cast** / **Cancel**; never expose raw stream URLs ("Copy Direct Stream URL" must not be available here).
6. Logs: remove any `Log.*` that prints titles, queries, URLs or IPs in `ui/incognito` and in private code paths of `PlayerViewModel` (presentation logging only).
7. Search recents and the trigger string never persisted; keyboard autocorrect/learning off in private fields.
8. Private downloads/bookmarks never appear in the main Library/Downloads/Search/Home, in Trakt/Simkl sync, nor in "Continue watching". Add a small unit/UI test for each where feasible.
9. Kids profile: private entry disabled; when switching to a Kids profile while inside the space, exit it first.
10. Deep links/intents: ensure no exported component can open the private space.

### V10 – Accessibility and TV
- Fix all 13 `contentDescription = null` on actionable elements; labels per V6 (neutral wording). Semantics via the Phase 5 `pressable` fix.
- ≥48dp hit areas; headings flagged; selected state announced on tabs/chips.
- 1.3× and 2.0× font scale without clipping.
- TV: initial focus (first card; Play on Details), D-pad handling in the private player (same map as main player), focus restoration per row, no focus traps in sheets, Back order as in V3.5.
- Reduce-motion respected; no autoplay previews.

### V11 – Enforcement and documentation
- Extend `scripts/check_ui_tokens.ps1` to include `ui/incognito/` (no allowlist unless justified in the allowlist file).
- Add a **private-space checklist** to `docs/EUTHOPIAR_UX_GUIDE.md` (new section "Private space") covering: naming, entry, auth, auto-lock, quick exit, leak list (V9), copy rules, the "never persist / never share / never notify with titles" rules.
- Update `docs/EUTHOPIAR_DESIGN_SYSTEM_GUIDE.md` §15: remove "incognito is intentionally separate"; state it now follows the system.
- Update `docs/DESIGN_AUDIT_REPORT.md` with the new findings and their status.

---

## Acceptance criteria (all must pass)
- `scripts/check_ui_tokens.ps1` passes including `ui/incognito/`; `:app:compileReleaseKotlin` and unit tests pass.
- No deprecated aliases, raw colors, `bouncyClickable`, raw `.sp`, or hard-coded user strings in `ui/incognito/`.
- Entry always goes through one function: kids blocked, age gate once, auth enforced, trigger text not stored, cancel is silent.
- Notification shade, share sheet, cast picker, rotation, or an incoming-call banner do **not** kick the user out; app switcher shows no content; leaving the app beyond the grace period returns to the normal Home.
- PiP either works safely or is disabled in the private player.
- Quick exit works from every private screen and via flip-to-exit; the normal app's top screen is Home afterwards.
- Private thumbnails and streams leave no disk cache (or an approved plan is documented); backups exclude private data; notifications reveal nothing.
- Private screens reuse `BackdropCard`, `EmptyState`, `ErrorState`, `ShimmerSkeleton`, `AppBottomSheet`, `VelvetAlertDialog`, `AppSegmentedControl`, `StatusBadge`.
- TalkBack, 2.0× font, D-pad and reduce-motion checks pass.

## Manual test checklist to include in your report
1. Type each trigger string with the toggle on/off, Kids profile, no lock screen, cancelled auth: confirm behaviour and that recents contain nothing.
2. Enter, open a video, pull the notification shade, rotate, open share/cast: session stays; app switcher shows the curtain.
3. Enter PiP from the private player, then return.
4. Background the app for 10 s and for 2 minutes: resume vs. return to Home.
5. Kill the app process: next launch shows the normal Home and nothing from the private session.
6. Take a screenshot, screen record, and check Recents.
7. Download a private video: notification text, lock screen, gallery app, main Downloads tab.
8. Bookmark, remove with Undo, clear saved with Undo.
9. Back-up/restore (or `adb shell bmgr`) and verify private data is absent.
10. Check after exit: Library, Search suggestions, Continue watching, Trakt/Simkl show nothing from the session.

## Report format
1. Findings (verified / not reproducible / blockers) and decisions needed (D1-D7)
2. Per-task table: V# · status · files (links) · before → after (one line)
3. Leak audit table (item · finding · fix · test result)
4. Build/test/script output (tail)
5. Before/after screenshots under `docs/screens/private/` (screens must be **non-explicit**: use placeholder or blurred thumbnails; do not commit real adult imagery)
6. Manual test results
7. Open questions

Start with the **Findings and decisions message** (no code changes yet). Wait for my answers on D1-D7 and any blocker, then proceed with V1.


---

# AMENDMENT A – Private space is MOBILE-ONLY (phone and tablet). It must NOT exist on Android TV

**This amendment supersedes anything above that mentions TV, D-pad, rail or `tvFocusable` for the private space.**

## Rule
The private space (Faphouse / incognito / vault) is available on **phones and tablets/foldables only**. On TV (`rememberWindowDimensions().isTV == true`, i.e. leanback feature or `UI_MODE_TYPE_TELEVISION`) it must be **completely absent**: no entry, no trigger, no setting, no hint, no code path reachable by remote control. A shared living-room screen is the wrong place for this feature.

## Required changes
1. **Single gate.** Add `PrivateSpaceAvailability.isAvailable(windowDimensions, profile, prefs)` which returns `false` when `isTV`, when the profile is Kids (D6), or when the master toggle is off. `PrivateSpaceEntry.request(...)` (V1) must call it first and do nothing if false. No other code may call `enableIncognito()`.
2. **Triggers ignored on TV.** The hidden search triggers in `SearchScreen.kt` and `InPluginSearchOverlay.kt` must be inert on TV: no navigation, no auth prompt, no clearing of the field, and the typed text is treated as a normal search query.
3. **Settings hidden on TV.** The Privacy rows from V1.5 (adult-sources master switch, *Require unlock*, *Quick exit by flipping device*, *Auto-lock*) are not composed on TV. The "Extra content" toggle moved in Phase 2.3 is also hidden on TV. If the toggle was previously on (e.g. data restored from a phone backup), ignore it on TV.
4. **Defensive exit.** In `AppNavHost`, if `isIncognitoActive` is ever true while `isTV` (config change, device docked, bug), call `disableIncognito()` immediately and never render `FaphouseVaultScaffold`. Also ignore `isIncognitoActive` when computing `showNavRail`/`showBottomNav` on TV.
5. **No private content in TV surfaces.** Confirm that nothing from the private space can appear on TV in Home, Search, Library, Downloads, Cast targets, or notifications. Private sources must be filtered out of any provider list shown on TV (presentation filter only; do not change the registry). If the plugin registry exposes adult sources in the normal source picker on TV, hide them there too and report how.
6. **Cast to TV.** The private player must not offer Cast (V9.5 changes from "confirmation" to "removed") so private content is never pushed to a shared screen. Hide the Cast button in the private player and cast picker entry points.
7. **Remove TV work for private screens.** Drop the TV bullets from V10: do **not** add `FocusRequester`s, D-pad key handling, focus restoration, TV grid columns (5), `tvFocusable`, or rail layouts to `ui/incognito/`. Keep touch accessibility (TalkBack, 48dp targets, 2.0× font) and tablet/foldable layouts (2 phone / 3-4 tablet columns). Where V5 says "rail on tablets/TV", use rail on tablets only. In V8 remove the TV D-pad map for the private player.
8. **Tablets are in scope.** Landscape phones, foldables and tablets use the same rules as phones, with the tablet nav rail and adaptive grids. Gate strictly on `isTV`, not on `isTablet`.
9. **Docs.** In `docs/EUTHOPIAR_UX_GUIDE.md` (new "Private space" section) and `docs/EUTHOPIAR_DESIGN_SYSTEM_GUIDE.md`, state: "Private space is mobile-only and hidden on TV." Add it to the checklist in V11.
10. **Tests.** Add unit tests for `PrivateSpaceAvailability` (phone → allowed, tablet → allowed, TV → blocked, Kids → blocked, toggle off → blocked) and a UI/instrumentation test (or documented manual test) that typing `::vault` on a TV configuration only performs a normal search.

## Additions to acceptance and manual tests
- On a TV device or emulator (Android TV image): typing every trigger string only runs a normal search; Settings shows no privacy/adult rows; no source named after adult providers appears anywhere; no vault code path renders; cast list never receives private streams.
- Simulate `isTV=true` while incognito is active: the session ends immediately and the app shows the normal Home.
- Phone/tablet behaviour is unchanged from the main prompt.

## Report additions
List every place where TV gating was added (file:line), how adult sources were filtered from TV source pickers, and any case where gating could not be done without touching the registry/core (ask me before touching those).
