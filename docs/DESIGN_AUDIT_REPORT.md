# Euthopiar – Design System Audit (Guide vs. Application)

> Scope: `app/src/main/java/com/euthopiar/ui/**` (the `incognito/` module excluded, as the guide itself excludes it).
> Method: static code review plus grep-based counts. Nothing was run on a device, so visual issues are inferred from code and need on-device confirmation. No code was changed.

---

## 1. Summary

The guide is solid on tokens and philosophy. The app follows it well at the foundation: `Color.kt`, `Type.kt`, `Shape.kt`, `Dimens.kt` and `Motion.kt` all exist, `pressable` is used 131 times, and shimmer, empty and error components exist. Compliance drops sharply at the edges: the **player, badges, settings, skeletons and dialogs**. The guide also has real gaps. It describes the app as it should be, not as it is.

| Area | Verdict |
|---|---|
| Tokens exist and are used in core screens | Good |
| Components exist (PosterCard, HeroCarousel, TopTenRow…) | Good |
| Strict token discipline (colors/sp/radii) | **Partial** |
| Navigation chrome matches guide | **Mismatch** |
| Copy / "no developer language" rule | **Violated** |
| Accessibility rules | **Violated in places** |
| System states (offline, no-results, etc.) | **Incomplete** |

---

## 2. Where the application does NOT match the guide

Counts below are in non-theme, non-incognito UI files.

### 2.1 Token violations (guide §0, §2, §3, §4, §14)

| Rule | Found | Where (main offenders) |
|---|---|---|
| No raw `Color(0x…)` | **33** | `MetadataBadges.kt` (~15), `PlayerScreen.kt`, `PlayerGestures.kt`, `AnimationModifiers.kt` (`0xFF0F1318/1B222B` shimmer), `SettingsScreen.kt`, `HeroCarousel.kt` |
| No `Color.White/Black/Gray/Red` | **47** | `ShimmerSkeleton.kt`, player files |
| No raw `RoundedCornerShape(n.dp)` | **50** | `MetadataBadges.kt` (4dp/5dp, not in ShapeS/M/L), `PlayerControls`, `ShimmerSkeleton` |
| No `fontSize =` / `n.sp` | **16 / 28** | `MetadataBadges.kt` (10.5sp, 11.5sp), `TopTenRow` (94/80sp), `PlayerControls` (12sp), `CinematicSplashScreen` (20sp), `SettingsScreen.kt:117` (`titleLarge.copy(fontSize = 28.sp)`; guide explicitly forbids this) |
| Use `AppTypography`, not Material typography | **230** uses of `MaterialTheme.typography` | Works only because `Typography` is mapped, but it contradicts guide §3 ("Always use `AppTypography.<role>`") |
| No deprecated aliases (`StarGold`, `PureWhite`, `TextMuted` …) | **20** outside incognito | e.g. `MetadataBadges.kt:148` uses `StarGold` |
| Only semantic colors | `PlayerScreen.kt:576-578` and `SettingsScreen.kt:400-402` use **yellow `#FACC15`, cyan `#38BDF8`, green `#4ADE80`** (subtitle color options); `PosterColorExtractor.kt:153` falls back to blue `#3B82F6` | Subtitle swatches are arguably legitimate user content, but the guide doesn't allow for it |
| Max 3-4 badges / calm (§1) | `MetadataBadges.kt` is 282 lines with its own glass backgrounds, 0.75dp borders and 4 different font sizes | Contradicts "badges recede" and "no new radii" |

### 2.2 Component / pattern mismatches

| Guide says | Reality |
|---|---|
| **FloatingNavbar = floating glass pill** (§6, §7, `GlassSurfaceNavbar`) | `FloatingNavbar.kt` is a **flat full-width bar**: `SurfaceLow @ 96%` + 0.5dp border, no glass, not floating. `GlassSurfaceNavbar` token appears unused. The Kdoc inside the file says "Modern Calm Bottom Navigation Bar", so the file is right and the guide is stale. |
| Nav rail 72dp for tablet/TV | Exists, but the logo is a text `"E"` in `Accent` red, while the guide says accent is for progress and selection only |
| Primary buttons white, accent not a fill | Followed. But the search Filter button uses `Accent.copy(0.2)` fill + accent border and text (`SearchScreen.kt:228-247`). That is accent as chrome. |
| `AppBottomSheet` for all pickers | **5** direct `ModalBottomSheet` uses remain, and **14** `AlertDialog` vs 8 `VelvetAlertDialog` |
| Alert dialogs: confirm/destructive only | `EcosystemSettingsDialogs.kt` uses `AlertDialog` for Trakt, Simkl and Debrid forms (token entry). They contain bold red/white text buttons rather than `PrimaryButton`/`SecondaryButton`. |
| `TopTenRow` driven by real ranking | `HomeScreen.kt:100-114` picks "Top 10" by **string-matching row titles** (`contains("Trending"/"Popular"/"Top")`), and falls back to hero items. A "Top 10" label over arbitrary content is a trust issue. |
| Settings: sentence case labels (§8) | Section headers are **ALL CAPS** (`"ACCOUNT & PROFILES"`, `"CONTENT SOURCES"`), the Downloads header is `"STORAGE"`, and `TopHeaderBar` and splash use letterSpacing 3-4sp caps. Guide §8 requires sentence case. |
| Settings: "tvOS/iOS grouped cards, 56dp rows" | Cannot verify row height; section icons + caps look more Android-Material than tvOS |
| `ErrorState`: friendly message, never raw exception | The component is correct, but **callers pass raw text**: `HomeViewModel.kt:283` `e.message ?: "Failed to load catalog"`; `PlayerViewModel.kt:1037` `"Stream extraction failed: ${e.localizedMessage}"` (this is the exact phrase §8 bans) |
| Never expose jargon (§8) | `CastPickerSheet.kt`: `"IP: …"`, `"Copy Direct Stream URL"`. `Settings`: "DNS-over-HTTPS (DoH)", "Debrid Stream Resolvers", "Broadcast real-time play and finish timestamps", "Scrobbling". `"Enable optional adult vault sources"` in the main Settings list. |
| Hand-typed `•` banned (§8) | Downloads and Library hand-build `"S1:E2 • Title"` and `"45% • 120 MB"` strings instead of using `MetaLine` (`MetaLine` is used in only **6** places) |
| `contentDescription` on every actionable icon (§11) | **46** `contentDescription = null`. Some are decorative, but concentrated in `SettingsScreen` (10), `CastPickerSheet` (6), `PlayerSheets` (6) – likely actionable. **Zero** uses of `semantics`/`Role`. |
| `TvFocus` on every clickable (§6) | 30 usages vs. 131 `pressable`. **No** `onKeyEvent` / D-pad key handling, and `FocusRequester` appears only in search overlays. TV support is cosmetic (focus ring) rather than functional. |
| Honor `LocalReduceMotion` (§5) | Used in only **2** places. The detection (Animator scale == 0) is also narrow; it does not read Android's "Remove animations" semantics beyond this. Hero auto-advance, shimmer, and the navbar spring are not obviously gated. |
| Spring only on Play button (§5) | `spring(` appears **8** times. The navbar uses a spring for hide/show and `bouncyClickable` still exists in 2 places. |
| Shared-element poster → details (500ms) (§7) | **Not implemented.** `sharedElement` appears only in `Motion.kt`; `AppNavHost` uses slide/fade transitions. |
| Snackbar for undo (§9) | **No `SnackbarHost`.** **14** `Toast.makeText` calls in non-incognito UI. No undo anywhere for destructive actions. |
| Loading uses shimmer, never spinners only (§12) | 7 `CircularProgressIndicator`s remain (PlayerControls, PluginManagerScreen ×3, CastPicker, InPluginSearch). Spinner during playback buffering is acceptable. |
| Spacing from `Dimens` | `HomeScreen.kt:136` uses `Dimens.spacing16` for chip padding instead of `horizontalMargin`, so the chips misalign with the hero/rows on tablet/TV. Raw `.dp` appears in 698 places (many legit sizes), but `FloatingNavbar` uses `2.dp`, `120.dp`, `24.dp`. |
| i18n | **0** `stringResource` calls and all copy hard-coded. Not a guide rule, but it blocks localization. |

### 2.3 Features in the guide's checklist (§9) that are absent in code

Based on greps (verify manually):

- Trailers on Details (none)
- "Mark as watched / unwatched" (none)
- Pull-to-refresh and swipe-to-dismiss (none)
- Onboarding / first run (none, even though the app is **useless until a source is added**)
- Snackbars with undo (none)
- Voice search (guide marks as future)
- "Because you watched" personalised rows (recommendations exist only on Details)
- Offline state *UI* (connectivity code exists only in the Download module; no screen shows an offline banner)
- Kids profile exists in the data model (`UserProfile.kt`), but no UI surface was found outside profile management

---

## 3. What is missing or wrong in the guide itself

1. **Stale facts.** It says the nav is a floating glass pill; the code is a flat bar. It also says "no gold" but `StarGold` is in use, and "spring only on Play" while the navbar uses a spring.
2. **"Do not modify" is overloaded.** A CAUTION block forbids improving anything. A guide that freezes an imperfect UI becomes a barrier to fixing the violations above. Split into *Rules for new UI* and *Approved backlog for fixes*.
3. **Not enforceable.** There are no lint rules, no detekt/ktlint custom checks and no screenshot tests. Add a grep CI script, e.g. fail on `Color(0x` outside `theme/`.
4. **Missing token categories:**
   - Overlay/scrim and gradient tokens (hero gradient, player scrim). Gradients appear in 11 places, all ad hoc.
   - Badge/chip spec (the biggest violator has no spec: only "max 2 labelSmall badges").
   - Icon sizes (16/20/24/32) and icon-button spec.
   - Z-order/elevation scale for sheets, dialogs and HUD.
   - Player-specific type (time labels use 12sp raw; not in `AppTypography`).
   - Subtitle color/size options (legit exceptions that need a documented list).
   - Skeleton color tokens (hex in `AnimationModifiers`).
   - A `bodySmall`-equivalent: the guide admits it is missing and tells people to work around it.
5. **Missing components in the catalog:** `SettingsRow`/`SettingsGroup`, `StatusBadge` (download status), `ProgressBar`, `IconButton`, `Snackbar/Toast`, `TextField`, `Switch`, `SegmentedControl` (Search has one hand-made), `TabRow` (Details has one with Material `tabIndicatorOffset`).
6. **Missing guidance:** content ratings, image fallbacks, text truncation per component, landscape phone, foldable posture, cutouts, system-bar behavior on player, back-stack behavior, text input states (focus/error), haptics inventory (68 haptic calls vs. guide's "thresholds only"), data density on TV, and the "overscan safe area" (TV margin is 48dp, but TV standard is ≈5% safe area).
7. **Contradictions:**
   - "Hero auto-advances ~5s" vs. "Honor reduce motion" vs. TV focus (auto-advance steals focus/time from a D-pad user).
   - "Accent is for progress/selection" but Top-10 emphasis uses it as well.
   - §0 says `ui/theme/` is the only token source, but `Adaptive.kt`/`rememberWindowDimensions` is a layout API.
   - "Poster gap 8dp" vs. Netflix/Apple's 8-12dp; fine, but the guide never justifies it.
8. **Section 15 understates the debt.** "Known leftovers" lists only incognito; actual leftovers are 33 raw colors, 50 raw shapes and 28 raw `.sp` in the main UI.
9. **Typos/encoding.** The guide's own warning about `?` and `•` is needed because encoding bugs already happened. Add a CI check for U+FFFD and stray control characters.
10. **No versioning or changelog.** Add `Last verified against commit …`.

---

## 4. Suggested improvements – Application (design-system level)

Priority order; none applied.

**P0 – Trust and accessibility**
1. Replace every raw error message (`e.message`, `"Stream extraction failed…"`) with mapped consumer copy plus a *Try another source* action.
2. Add contentDescription to all actionable icons; add `semantics { role = Role.Button }` where `pressable` replaces `clickable`. **Verify `pressable` exposes click semantics to TalkBack.** If it uses `pointerInput`, TalkBack users cannot activate those items.
3. Replace `Toast` with a single `SnackbarHost` (with Undo for deletes/removals).
4. Honor system "Remove animations" everywhere (`LocalReduceMotion`) and stop hero auto-advance on TV or when focused.

**P1 – System consistency**
5. Rebuild `MetadataBadges.kt` on tokens: `ShapeS`, `SurfaceHigh`, `labelSmall`, with no 0.75dp borders and no four font sizes.
6. Fix `Settings` typography: sentence-case headers, remove `.copy(fontSize = 28.sp)`.
7. Move Trakt/Simkl/Debrid dialogs to `AppBottomSheet` + `PrimaryButton`/`SecondaryButton`.
8. Make Top 10 real (rank by an actual trending endpoint or remove the label).
9. Align category chips to `horizontalMargin`.
10. Decide on the navbar: either update the guide to "flat bar" (cheaper) or build the floating pill.

**P2 – Polish**
11. Implement the shared-element poster → details transition promised in §7.
12. Replace hand-typed separators with `MetaLine`.
13. Move all strings to `strings.xml`.
14. Add detekt/grep CI and screenshot tests (Paparazzi/Roborazzi) for Home, Details, Player controls at 1.0× and 1.3× font scale.

---

## 5. Priority matrix

| # | Item | Impact | Effort |
|---|---|---|---|
| 1 | Raw error text → friendly copy | High | Low |
| 2 | Accessibility semantics | High | Medium |
| 3 | Snackbar + undo | High | Low |
| 4 | Badge rebuild | Medium | Medium |
| 5 | Real Top 10 | Medium | Medium |
| 6 | Sentence-case/typography cleanup | Low | Low |
| 7 | Navbar decision (doc vs. code) | Low | Low |
| 8 | Shared-element transition | Medium | High |
| 9 | CI token linting | High (long-term) | Low |

---

## 6. Phase 2.5 Audit & Resolution: Private Space (`ui/incognito/*`)

The private area ("Faphouse / Incognito / Vault") was audited in Phase 2.5 to achieve the same visual bar as the primary app and enforce consumer-grade privacy and discretion.

### Audit Findings Confirmed
- **Token Debt:** 75 raw `Color(0x...)`, 15 `Color.White/Black/Gray`, 33 `RoundedCornerShape(`, 62 `.sp` literals, 53 `fontSize =`, 11 `bouncyClickable`, 0 `pressable`, 0 `EmptyState`/`ErrorState`, 7 custom spinners, 13 `contentDescription = null`, and deprecated aliases (`VelvetBlack`, `PureWhite`, `TextMuted`, `RefractiveGlassBorder`).
- **Session Lifecycle Defect:** Abrupt session termination on `ON_PAUSE` (notification pull-down or incoming call dumped the user to the main app mid-playback; broken PiP).
- **Entry & Discretion Leaks:** Plain adult trigger keywords (`fap`, `faphouse`); unauthenticated entry paths; triggers saved into search recents.
- **Cache & Persistence Leaks:** Shared Coil disk cache retained thumbnails; backup rules included private data; download notifications revealed titles and thumbnails on lock screens.

### Resolution Summary (Tasks V1–V11)
- **V1 (Entry & Auth):** Single gateway `PrivateSpaceEntry.request(...)`, Kids profile block, device/biometric auth via `VaultAuthManager`, trigger search terms cleared and never saved to recents, silent auth cancellation.
- **V2 (Age Gate):** Discreet one-time bottom sheet (`PrivateSpaceAgeGateSheet`) setting expectations with zero explicit imagery.
- **V3 (Lifecycle & Curtain):** Privacy curtain (solid `Background`) on `ON_PAUSE`; 30s auto-lock grace timer on `ON_STOP`; verified lock state machine unit tests; safe PiP support.
- **V4 (Quick Exit):** Persistent Close `AppIconButton` on every screen; accelerometer-based panic flip-to-exit (`PanicFlipDetector`); instant stack pop to `HOME`.
- **V5 (Tokens):** Total replacement of raw hex colors, raw shapes, and deprecated aliases with obsidian system tokens (`Background`, `Surface*`, `Text*`, `Shape*`, `Dimens`).
- **V6 (Microcopy):** Neutral, sentence-case copy in `res/values/strings_private.xml`; adult jargon, raw provider URLs, and unverifiable statistics removed.
- **V7 (Screens):** Redesigned Feed, Categories, Search, Saved, Creator, and Details with `BackdropCard`, `ContentRow`, `EmptyState`, and `AppSegmentedControl`.
- **V8 (Player Parity):** Extracted `FaphousePlayerControls` with edge-guarded gestures, 3.5s auto-hide, 2× speed hold HUD, audio focus management, and memory-only playback state.
- **V9 (Leak Prevention):** In-memory-only Coil loader (`PrivateImageLoader`), cloud backup exclusion via `data_extraction_rules.xml` and `backup_rules.xml`, discreet notifications (`VISIBILITY_SECRET`), and zero scrobbling/history cross-contamination.
- **V10 (Accessibility):** All 13 `contentDescription = null` fixed with localized strings; `heading()`, `Role.Tab`, and `Role.RadioButton` semantics added; $\ge$ 48dp hit targets enforced.
- **V11 (Enforcement):** `scripts/check_ui_tokens.ps1` updated to enforce 100% compliance across `ui/incognito/*` (0 violations across 183 Kotlin files).

