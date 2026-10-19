# Screen: Workout Tab

Reference: `img/01_workout_tab.jpg`

## Layout (top to bottom)

1. **Header row** (16dp padding): "Workout" in `screenTitle` + small circular `surface` button with chevron-down
   (purpose unknown — likely switches between Workout/folders; leave as no-op). Right: `ProBadge`
   (in a personal clone, just hide it or make it decorative).
2. **Start Empty Workout** — full-width `SecondaryButton`, leading `+`, left-aligned text, ~56dp tall.
3. **"Routines"** in `sectionTitle`.
4. **Two tiles side by side** (equal width, 16dp gap, ~100dp tall, `surface`, 12dp radius), icon above centered label:
   - Clipboard icon — "New Routine"
   - Magnifier icon — "Explore Routines"
5. Below: list of the user's saved routines (empty here; design TBD once captured).
6. Bottom navigation, Workout selected.
7. When a workout is minimized, the floating workout pill sits above the bottom nav (img 36; see active_workout.md §16).

## Behavior

- Start Empty Workout → creates `Workout(startedAt = now)` and opens Active Workout
  as a full-screen route that hides the bottom nav.
- If a workout is already in progress, ask to resume or discard it rather than starting a second one.
- New Routine / Explore Routines → placeholder screens for now.
