# Euthopiar Design System & UI Guide

> [!NOTE]
> **Authoritative Specification & Reference Guide**  
> **Last verified against commit:** `3cc5dbeea4155b572cfb27eea8d86d7ddb761b25` (Phase 6 Complete)  
> This document governs design tokens, shared UI components, interaction paradigms, and accessibility requirements across all Euthopiar application surfaces.

---

## 0. How to Use This Guide

When building or modifying UI:
1. **Locate Existing Components:** Reference Section 6 and reuse shared components (`ui/components/`). Do not create parallel or one-off implementations.
2. **Strict Token Adherence:** Use only design tokens defined in `ui/theme/` (Sections 2–5). Never introduce hardcoded `Color(0x…)`, raw `RoundedCornerShape(n.dp)`, arbitrary `n.sp`, or ad-hoc `dp` spacing.
3. **Pre-commit Linter:** Run `.\scripts\check_ui_tokens.ps1` before committing to ensure 100% token compliance, accessibility description coverage, and UTF-8 validation.

---

## 1. Design Philosophy

Euthopiar is designed around three principles inspired by Apple TV, Netflix, and Prime Video:
1. **Artwork First:** Posters and backdrops dominate the visual hierarchy. Chrome, borders, and controls recede into deep dark backgrounds.
2. **Calm Velvet Atmosphere:** A restrained palette built on a single red accent (`Accent` `#E50914`), solid high-contrast cards, and minimal badge noise.
3. **Glass Overlays, Solid Cards:** Frosted glass translucency (`GlassSurfaceNavbar`, `SurfaceMid.copy(alpha = 0.85f)`) is reserved exclusively for navigation pills, player HUDs, and modal sheets. Media cards and list rows remain solid `SurfaceLow` or `SurfaceMid`.
4. **Restrained Motion:** Consistent duration and easing curves (`Motion.StandardEasing`). Springs are used exclusively on the primary Play action. Honor `LocalReduceMotion` globally.
5. **Universal Accessibility:** 48dp minimum interactive touch target (`Dimens.minTouchTarget`), WCAG AA text contrast, TV D-pad focus (`tvFocusable`), and full resilience under `1.3×` and `2.0×` font scaling.

---

## 2. Color Tokens (`Color.kt`)

| Token | Value | Primary Usage |
|---|---|---|
| `Background` | `#0B0B0F` | Core application and canvas background |
| `SurfaceLow` | `#14141A` | Cards, list rows, image placeholder backing |
| `SurfaceMid` | `#1C1C24` | Bottom sheets, alert dialogs, text fields, search bars |
| `SurfaceHigh` | `#26262F` | Selected tabs/chips, pressed rows, elevated panels |
| `OverlayScrim` | `#000000 @ 60%` | Dialog backdrops and bottom sheet dimming |
| `PlayerTopScrim` | `#000000 @ 65% → 0%` | Top gradient scrim (110dp) for player controls |
| `PlayerBottomScrim`| `#000000 @ 75% → 0%` | Bottom gradient scrim (170dp) for scrubber & playback actions |
| `Outline` | `#FFFFFF @ 10%` | 1dp hairline borders where separation is required |
| `TextPrimary` | `#FFFFFF` | Primary headlines, titles, active icons, primary button fill |
| `TextSecondary` | `#B3B3BD` | Body copy, secondary metadata (meets WCAG AA contrast) |
| `TextTertiary` | `#8A8A96` | Captions, unselected indicators, decorative icons |
| `Accent` | `#E50914` | Progress indicators, active tab dots, Top-10 numerals |
| `Success` | `#34C759` | Connected accounts, online source status, ready downloads |
| `Warning` | `#FF9F0A` | Caution alerts, storage low warnings, plugin updates |
| `Error` | `#FF453A` | Network failures, destructive action buttons, errors |
| `GlassSurfaceNavbar` | `#0E1118 @ 85%` | Floating navigation bar pill background |

### Color Rules
- **Primary Button Fill:** Pure white (`TextPrimary`) with dark text (`Background`). Accent red is reserved for progress, badges, and selection emphasis.
- **HUD Badges & Overlays:** Use `SurfaceMid.copy(alpha = 0.85f)` with 1dp `Outline` hairline borders.
- **Zero Raw Hex Colors:** Never invoke `Color(0x...)` outside `Color.kt`. Deprecated aliases (`VelvetBlack`, `ObsidianDark`, `PureWhite`, `PureBlack`, `TextMuted`, `StarGold`) are retained only as deprecated stubs and must not be used in active code.

---

## 3. Typography Tokens (`Type.kt`)

All text rendering must utilize `AppTypography.<role>`.

| Role | Size / Line | Weight | Color | Use Case |
|---|---|---|---|---|
| `displayHero` | 34 / 38 | Bold | `TextPrimary` | Hero billboard titles, huge codes |
| `titleLarge` | 22 / 28 | SemiBold | `TextPrimary` | Screen headers, major section titles |
| `titleMedium` | 18 / 24 | SemiBold | `TextPrimary` | Dialog headers, sheet titles, card group titles |
| `titleSmall` | 16 / 22 | SemiBold | `TextPrimary` | Settings section titles, list row titles |
| `bodyLarge` | 16 / 24 | Normal | `TextPrimary` | Media synopses, dialog body text |
| `bodyMedium` | 14 / 20 | Normal | `TextSecondary` | General body, subtitle text |
| `labelLarge` | 14 / 18 | SemiBold | `TextPrimary` | Button labels, interactive tabs, chip text |
| `labelMedium` | 12 / 16 | Medium | `TextSecondary` | Metadata lines, helper labels |
| `labelSmall` | 11 / 14 | Medium | `TextTertiary` | Small captions, quality & rating badges |

---

## 4. Shapes, Dimensions & Sizing

### Shapes (`Shape.kt`)
- `ShapeS` (8dp): Media posters, episode thumbnails, inner badges, filter chips.
- `ShapeM` (12dp): Media cards, text inputs, dialog action buttons, settings containers.
- `ShapeL` (20dp): Bottom sheets, alert dialog surfaces, HUD floating panels.
- `ShapeFull`: Pills (Primary/Secondary buttons, navigation bar, scrubber thumb).

### Spacing & Dimensions (`Dimens.kt`)
- **Spacing Scale:** `spacing4`, `spacing8`, `spacing12`, `spacing16`, `spacing24`, `spacing32`, `spacing48`.
- **Minimum Interactive Touch Target:** `Dimens.minTouchTarget` = **48dp** (strictly enforced across all buttons, chips, icons, and scrubber tracks).
- **Icon Sizing:**
  - `Dimens.iconS` = **16dp** (inline metadata icons, status dots).
  - `Dimens.iconM` = **24dp** (standard action bar and row icons).
  - `Dimens.iconL` = **32dp** (player center transport icons).
- **Standard Heights:**
  - Button height: 48dp.
  - Filter chip height: 36dp visual (with 48dp touch bounds).
  - Settings row min height: 56dp.
  - Floating navbar height: 64dp.
  - Navigation rail width: 72dp.
- **Screen Margins:** 16dp (phone), 24dp (tablet), 48dp or 5% (TV) via `rememberWindowDimensions().horizontalMargin`.
- **Aspect Ratios:** Posters **2:3**, Backdrops **16:9**.

---

## 5. Motion & Interaction (`Motion.kt`)

- **Timing Constants:**
  - `Motion.DurationFast` = 120ms (micro-interactions, button state changes).
  - `Motion.DurationStandard` = 220ms (standard transitions, image crossfades, sheet slide).
  - `Motion.DurationEmphasized` = 360ms (modal reveals, full-screen crossfades).
  - `Motion.DurationHero` = 500ms (hero carousel slides, poster-to-details shared element).
- **Easing:** `Motion.StandardEasing` (FastOutSlowIn) and `Motion.EmphasizedDecelerateEasing`.
- **Interactive Feedback:** Standard items use `Modifier.pressable { }` (alpha 0.85, scale 0.98).
- **Spring Physics:** Reserved solely for primary transport play buttons via `Modifier.playButtonSpringPress { }`.
- **Reduce Motion:** Every motion effect (auto-advance, hero Ken-Burns, shared element transition, shimmer sweep) checks `LocalReduceMotion.current` and falls back to an instantaneous or gentle 220ms crossfade.

---

## 6. Shared Component Catalog (`ui/components/`)

| Component | Specification & Behavior |
|---|---|
| `AppAsyncImage` | Standard image loader. Backed by `SurfaceLow` placeholder, 220ms crossfade, and fallback displaying the title initial upon error. Accepts `sizeHints` for memory efficiency. |
| `PosterCard` | 2:3 artwork, `ShapeS`, no elevation, title below (1–2 lines), optional `MetaLine`. |
| `BackdropCard` | 16:9 card with bottom `Accent` progress bar for continue watching and episodes. |
| `MediaCard` / `ContentRow` | Horizontal rail item and scrolling row with `SectionHeader` and 8dp gaps. |
| `HeroCarousel` | Left-aligned title, `MetaLine`, 48dp white Play pill, secondary button, 5s auto-advance. |
| `TopTenRow` | Outlined numerals 1–10 behind poster artwork without distracting fire/crown badges. |
| `ContinueWatchingRow` | Backdrop cards with progress meter; episode metadata displayed cleanly. |
| `MetaLine` | Formats metadata lists (`"2024 · TV-MA · 2h 15m · Sci-Fi"`) using UTF-8 bullet separators (`·`). |
| `QualityBadge` | Small badge (`ShapeS`, `SurfaceHigh`, `TextSecondary`) indicating 4K, HDR, 1080p. |
| `ContentRatingBadge` | Standardized age-rating badge (`TV-MA`, `PG-13`, `R`) with hairline outline. |
| `AppFilterChip` | Interactive chip (`ShapeFull`, 36dp visual, 48dp touch target) with `selected` semantics. |
| `PrimaryButton` | 48dp pill button with white background (`TextPrimary`) and dark text (`Background`). |
| `SecondaryButton` | 48dp pill button with dark background (`SurfaceMid`) and 1dp `Outline` border. |
| `SettingsGroup` | Grouped container with uppercase category title, category icon, and rounded card. |
| `SettingsRow` | Standard 56dp settings row supporting title, subtitle, switch, chevron, and value text. |
| `AppSwitch` | Custom toggle switch using `Accent` for active state and `SurfaceHigh` for inactive. |
| `AppTextField` | Outlined text input (`SurfaceMid`, `ShapeM`, `Accent` focus ring). |
| `AppBottomSheet` | Standard bottom sheet (`SurfaceMid`, `ShapeL` top corners, drag handle). |
| `VelvetAlertDialog` | Centered modal dialog (`SurfaceMid`, `ShapeL`) with primary, secondary, or destructive actions. |
| `EmptyState` | Informative icon, title, description, and optional action button. |
| `ErrorState` | Plain-language error description with primary Retry button. Never displays raw exceptions. |
| `ShimmerSkeleton` | Coordinated loading skeleton sharing a synchronized pulse clock. |
| `tvFocusable` | Accessible D-pad focus treatment (scale 1.05 + subtle glow/border) for Android TV. |

---

## 7. Navigation & Surfaces

### Floating Navigation Bar (`FloatingNavbar`)
- **Handheld Phone:** Centered floating pill layout (`GlassSurfaceNavbar`, 85% opacity, `ShapeFull`, height 64dp, 20dp side padding).
- **Tablet / Landscape / TV:** Replaced automatically by a 72dp side navigation rail or TV sidebar.
- **User Configurable:** Settings provides a toggle between `NavbarStyle.FLOATING_PILL` and `NavbarStyle.FIXED_BAR`.
- **Contextual Visibility:** Automatically concealed during first-run onboarding, full-screen player playback, and immersive dialogs.

### Settings Category Hierarchy
Settings categories are ordered strictly as follows:
1. **Profiles** (Active avatar, switch profile, kids profile)
2. **Playback** (Auto-play, default quality, seek step, dialogue boost)
3. **Subtitles & audio** (Subtitle preview, language selection, size slider)
4. **Downloads** (Download quality, Wi-Fi only, storage meter)
5. **Sources** (Installed plugins, status indicators, test actions, update badges)
6. **Accounts** (Trakt and Simkl connections with `@username` badges)
7. **Privacy** (Additional sources toggle, PIN protection, biometric lock)
8. **Accessibility** (Reduce motion override, large text adaptations)
9. **Advanced (Collapsible)** (Secure DNS / DoH, Debrid API keys, advanced network configurations)
10. **About** (App version, commit hash, "What's new" release notes sheet)

---

## 8. Rules for New UI vs. Approved Backlog

### Part A: Rules for New UI
1. **No External Color/Typography Libs:** All UI must strictly import from `com.euthopiar.ui.theme.*` and `com.euthopiar.ui.components.*`.
2. **Accessible by Default:** All actionable elements must supply non-null `contentDescription` or visible text and adhere to `Dimens.minTouchTarget` (≥48dp). Decorative icons must pass `contentDescription = null` with an explanatory comment `// decorative`.
3. **Seven Core States:** Every new screen must account for all seven states: Loading (shimmer), Empty, Error, Offline, No Results, Unavailable, and Permission Required.
4. **Resilience to Scaling:** Test all new screens at `1.0×` and `1.3×` font scales. No fixed-height text wrappers; enforce `maxLines` with `TextOverflow.Ellipsis`.
5. **Private Space Isolation:** Never touch `ui/incognito/*`. Private space is permanently sealed and protected by `FLAG_SECURE`.

### Part B: Approved Feature Backlog
The following items represent designed features queued for future releases:
- Real-time Top 10 regional rankings backend integration.
- In-app trailer playback overlays with automated audio ducking.
- Cloud scrobble conflict resolution sheet for concurrent Trakt/Simkl updates.
- Scrubber thumbnail preview generation for streaming sources providing image sprite tracks.
- Voice-enabled search queries on Android TV and large screen form factors.

---

## 9. Pre-Commit Verification Checklist

Before creating a commit, verify that:
- [x] `powershell -ExecutionPolicy Bypass -File .\scripts\check_ui_tokens.ps1` completes with 0 errors.
- [x] Zero raw `Color(0x...)` or deprecated color aliases exist outside `ui/theme/`.
- [x] All lazy lists and rows specify explicit, stable `key = { ... }`.
- [x] Visual regression test suite passes: `.\gradlew :app:testReleaseUnitTest --tests "com.euthopiar.ui.screenshot.VisualRegressionScalingTest"`.
- [x] Release compilation passes cleanly: `.\gradlew :app:compileReleaseKotlin`.
