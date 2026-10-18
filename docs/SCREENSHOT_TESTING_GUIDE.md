# Visual Regression & Screenshot Testing Guide

This guide details how to run, maintain, and extend visual regression and font-scaling tests for the Euthopiar application across different device profiles and accessibility font scales.

---

## 1. Overview

Euthopiar employs a two-tier visual testing strategy:
1. **Robolectric Font-Scale & Layout Regression Suite (`VisualRegressionScalingTest`):**
   - Headless JVM test suite evaluating Home, Media Details, Player Controls, and Settings surfaces.
   - Evaluates both standard (`1.0×`) and high-accessibility (`1.3×`) font scales.
   - Asserts interactive element visibility, text reflow, and compliance with the WCAG 48dp minimum touch target (`Dimens.minTouchTarget`).
2. **Roborazzi / Paparazzi Visual Diffs:**
   - Visual golden screenshot capture for automated bitmap difference tracking across pull requests.

---

## 2. Tested Surfaces & Scenarios

| Surface | Components Verified | Font Scales | Touch Target / Bounds Checks |
| :--- | :--- | :--- | :--- |
| **Home** | Category Filter Chips (`All`, `Movies`, `TV Shows`), Navigation Bars | `1.0×`, `1.3×` | ≥48dp min width & height on all chips |
| **Media Details** | Metadata line (`MetaLine`), Badges (`4K`, `HDR`, `TV-MA`), Primary/Secondary action buttons | `1.0×`, `1.3×` | Buttons maintain ≥48dp height without text truncation |
| **Player Controls** | `PlayerTopBar` (Title, S1 · E4 subtitle, Back button), `PlayerCenterCluster` (Play/Pause, seek glyphs), `PlayerBottomControls` (Speed, Audio & Subtitles, Settings) | `1.0×`, `1.3×` | All interactive hit-boxes ≥48dp; timecodes reflow cleanly |
| **Settings** | Category headers (`Playback`, `General`), navigation rows, switches, action rows | `1.0×`, `1.3×` | Standard 56dp row container preserved under `1.3×` typography |

---

## 3. How to Run the Tests

### Quick Verification (Local Machine / Terminal)

To execute the visual regression and font scaling suite locally:

```powershell
# Run the visual regression test suite via Gradle Daemon
.\gradlew :app:testReleaseUnitTest --tests "com.euthopiar.ui.screenshot.VisualRegressionScalingTest"
```

### Full UI Token & Test Suite Check

```powershell
# 1. Run design system token compliance check
.\scripts\check_ui_tokens.ps1

# 2. Run full release unit test suite
.\gradlew :app:testReleaseUnitTest
```

### Viewing HTML Reports

Upon completion, an HTML report with execution timings and stack traces is generated at:
```
app/build/reports/tests/testReleaseUnitTest/index.html
```

---

## 4. CI/CD Integration & Future Roborazzi Steps

To record or compare golden PNG screenshots with Roborazzi:
1. Ensure `io.github.takahirom.roborazzi` plugin is configured in Gradle.
2. Record golden images:
   ```bash
   ./gradlew recordRoborazziRelease
   ```
3. Verify against golden images on CI:
   ```bash
   ./gradlew verifyRoborazziRelease
   ```
   Diff output will appear under `app/build/outputs/roborazzi/`.
