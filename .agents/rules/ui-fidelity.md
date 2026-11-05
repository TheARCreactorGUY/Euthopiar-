---
description: Mandatory UI/UX High-Fidelity Design Craft rules based on Emil Kowalski, Impeccable, and Taste-Skill
always_on: true
---

# UI/UX High-Fidelity Design Craft Rules

Every UI change, addition, refactor, or polish in Euthopiar must consult and strictly align with the 3 locked-in UI design skills located in `.agents/skills/`:

## 1. Emil Kowalski Principles (`.agents/skills/emil-design-eng/`, `.agents/skills/animate/`, `.agents/skills/mobile-native/`)
- **Physics-Based Motion**: Always use Jetpack Compose springs (`spring(dampingRatio, stiffness)`) for natural handoffs, critical damping, and physical elasticity.
- **Directional Easings**: Enter animations must accelerate fast and decelerate gently (`ease-out`); exit animations can be subtle and fast. Never use `ease-in` on elements appearing to the user.
- **Immediate Tactile Feedback**: Every interactive surface must provide instant press response (`scale(0.96f-0.97f)` on press with fast recovery).
- **Spatial Consistency**: Avoid `scale(0)` pops; scale gracefully from `0.94f-0.96f` with alpha fade.
- **Touch & Cutout Ergonomics**: Respect camera cutouts, system bar insets, edge boundaries, and ensure a minimum 48dp touch target with comfortable tap margins.

## 2. Paul Bakaus Impeccable Principles (`.agents/skills/impeccable/`)
- **Refinement vs Redesign**: Refinement keeps incumbent identity, layout muscle memory, and copy; redesign replaces intentionally. Never deliver half-baked compromise.
- **The Craft Floor**: Before finalizing any UI modification, verify visual hierarchy, optical alignment, spacing consistency, contrast legibility, and state handling (loading, empty, error, long text).
- **Restraint Over Decoration**: Let content lead; interface chrome and controls must recede quietly into the background until needed.

## 3. Leonxlnx Taste-Skill Principles (`.agents/skills/taste-skill/`)
- **Anti-Slop Standard**: Reject synthetic AI visual tropes (unnecessary fluorescent glows, saturated card borders, arbitrary floating tags, clutter).
- **Hairlines & Elevation**: Use quiet hairlines (`0.5dp` or `1dp` with `0.08f - 0.12f` alpha) and tonal depth rather than aggressive drop shadows.
- **Tabular & Typographic Precision**: Strict typographic scale, tabular figures for timecodes/counters (`FontFeatureSettings = "tnum"`), and balanced line heights.
