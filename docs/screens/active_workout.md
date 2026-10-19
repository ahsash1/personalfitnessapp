# Screen: Active Workout (Log Workout)

The core of the app — spend the most polish here.

References: `img/02` (empty), `img/09` (exercise added), `img/10` (⋮ sheet), `img/11` (editing),
`img/12`, `img/13` (set completed + PR toast + rest timer), `img/14`, `img/15` (editing completed set,
placeholder), `img/16` (discard dialog).

Full-screen route; bottom navigation hidden.

## 1. Top bar (`topBar` color, ~64dp)

- Left: chevron-down icon → **minimize** the workout (section 16).
- Title: "Log Workout" (appBarTitle) in some screenshots, the live duration in `primary` color ("1min 9s", "4s") in others.
  It is NOT simply scroll-based: img 24/25 show the duration title with the list unscrolled at 4–8s.
  Whenever the duration title shows, a small round `surface` tab with a "+" also hangs from the bottom-center of the top bar.
  Trigger and purpose unknown (see 99_open_questions.md). Until clarified: show "Log Workout" and ignore the "+" tab.
- Right: stopwatch icon → **Clock sheet** (section 12) and "Finish" `PrimaryButton` (compact).
- **Progress line** along the bottom edge of the top bar: thin (~3dp) `primary` bar whose width =
  completed sets ÷ total sets. Verified: 2 of 3 sets done → bar spans 2/3 of the width (img 13/14). 0 done → no blue.

## 2. Stats row

Three `StatColumn`s + mini muscle map, divider below:
- **Duration** — value in `primary` color, ticks every second.
- **Volume** — "436 kg" (completed sets only; see data model).
- **Sets** — completed set count.
- **Mini `MuscleMap`** (front + back, ~30dp wide each) — muscles of completed sets tint blue.

## 3. Empty state (no exercises) — img 02

Centered vertically in the upper area: large outline dumbbell icon (textSecondary), "Get started" (bold, 20sp),
"Add an exercise to start your workout" (textSecondary). Then:
- Full-width `PrimaryButton` "+ Add Exercise"
- Row: "Settings" `SecondaryButton` | "Discard Workout" `SecondaryButton` with `danger` text.

## 4. Exercise card (repeat per exercise) — img 09

1. **Header row**: 40dp circular thumbnail · exercise name in `exerciseName` style (primary blue, tappable →
   Exercise Detail) · trailing vertical ⋮ icon.
2. **Chip row**: "⏱ Rest: 2min 30s" chip and "📝 Note" chip (both `surface` pills with outline icons).
   Rest chip → **Rest Timer picker** (section 13). Shows "Rest: OFF" when disabled (default for cardio, img 26).
   Note chip → reveals the **session note** line (section 14).
3. **Table header** (tableHeader style): `SET` · `PREVIOUS` · `🏋 KG` (dumbbell icon + unit) · `REPS` · `✓`.
4. **Set rows**, columns roughly 15% / 25% / 22% / 22% / 16%:
   - **SET**: rounded-square `surface` cell with the set label: number (white) for normal sets, or a colored letter for
     set types — **W** amber, **F** red, **D** blue. Replaced by the gold medal icon when this set holds a PR.
     Tap → **Set Type sheet** (section 15).
   - **PREVIOUS**: "20kg x 15" in textSecondary; "—" if none.
   - **KG** and **REPS**: outlined input cells (1dp `surfaceHigh` border, 8dp radius), centered text.
     Empty → placeholder from previous in textSecondary. Focused → 2dp `primary` border. Text selected entirely on focus (img 14 shows "68" highlighted).
   - **✓**: rounded-square `surfaceHigh` button with grey check.
5. **"+ Add Set"** full-width `SecondaryButton`. Adds a row copying the previous row's values as placeholders.

**Swipe to delete a set** (img 25): drag a row left; content slides left and a full-row-height `danger` panel with
white "Delete" text is revealed on the right (~100dp). Tap Delete (or swipe fully) → remove the set and renumber.
Use Compose `SwipeToDismissBox` or an anchored-draggable row.

**Cardio exercises** (img 26, Treadmill): columns become `SET` · `PREVIOUS` · `KM` · `TIME` · `✓`.
Previous reads "0 km in 05:00". TIME is entered and shown as `mm:ss` (hh:mm:ss past an hour) — use a custom
duration input or a picker, not a free number. Volume ignores cardio sets; they still count toward "Sets".
Other exercise types (assumed, from the type enum): reps only (bodyweight), weight + reps, duration only (plank),
distance + duration (cardio), weight + distance (farmer's walk).

After all exercise cards: "+ Add Exercise" (primary), then "Settings" | "Discard Workout" row (same as empty state).

## 5. Completing a set — img 12, 13

On tapping ✓:
1. If weight/reps empty, fill from placeholders. If still nothing, shake the row and don't complete.
2. Row background animates to `successRow` (full-bleed, edge to edge); the check cell becomes solid `success` with white check.
   Input cells lose their borders and show plain white bold values.
3. Stats row + progress line update.
4. **Rest timer starts** with this exercise's rest duration (section 6).
5. **PR check** → toasts (section 7) and medal icon update.
6. Haptic tick.

Tap ✓ again on a completed row → un-complete (revert styling, recompute stats, stop timer if it was started by this set).
Tapping a value in a completed row still allows editing (img 14) — the row stays green.

## 6. Rest timer bar — img 12–15

Docked at the bottom, above the keyboard / bottom inset, `topBar` background:
- Thin `primary` progress bar across its top edge, shrinking from full to empty as time elapses.
- Row: "-15" button (`surface`) · large countdown "02:21" (timerLarge, centered) · "+15" button · "Skip" `PrimaryButton`.
- -15 / +15 adjust remaining time (and total, for the progress bar). Skip or reaching 0 → hide the bar.
- At 0: vibration + sound/notification (assumed). Timer must survive backgrounding → use a foreground
  service or store `restEndsAt` timestamp and derive remaining time from it.

## 7. PR toast — img 12, 13

- Slides down from the top, overlapping the top bar, horizontally inset ~24dp, fully rounded pill, `surfaceHigh` background.
- Content: 56dp circular exercise thumbnail · two lines: exercise name (white, 18sp) and
  "Best 1RM - 40 kg" / "Heaviest Weight - 68 kg" in `gold`.
- Visible ~2.5s, then slides up. Multiple PRs from one set queue one after another.
- Wording per type: "Heaviest Weight - X kg", "Best 1RM - X kg", "Best Set Volume - Xkg x N", "Best Session Volume - X kg".

## 8. Keyboard — img 11

The screenshots show the phone's own numeric keyboard (Samsung), not a custom keypad.
Use `KeyboardType.Decimal` for KG and `KeyboardType.Number` for REPS, `ImeAction.Next` to move
KG → REPS → next set's KG. "Done" closes the keyboard. The list must scroll the focused row above the keyboard + timer bar.

## 9. Exercise ⋮ bottom sheet — img 10

Modal bottom sheet (`surface`, top corners 24dp, drag handle), with a grouped `surfaceHigh` card of 4 rows separated by dividers:
- ↕ "Reorder Exercises" → collapse cards to a draggable list of names.
- ⟳ "Replace Exercise" → open picker in single-select mode; replaces exercise, clears sets to 1 empty set.
- + "Add To Superset" → pick another exercise in the workout to group with (shared `supersetGroupId`; show a colored left rail on grouped cards — assumed).
- ✕ "Remove Exercise" (danger color) → remove card (offer Undo snackbar).

## 10. Discard dialog — img 16

Centered dialog, `surface` card, 16dp radius, scrim ~60% black.
Message: "Are you sure you want to discard this workout?" (centered, 18sp).
Two full-width stacked buttons (`surfaceHigh`): "Discard Workout" (danger text) and "Cancel" (white).
Discard → delete workout + its sets, close screen.

## 11. Finish

Not captured. Minimum viable: confirm if any sets are incomplete ("Discard incomplete sets?"), then
set `endedAt`, store PR badges per set, open a save/summary screen (title, description, duration, volume)
and return to Profile/Home.

## 12. Clock sheet — img 28

Opened from the stopwatch icon in the top bar. Modal bottom sheet (`surface`, drag handle):
- Title "Clock" centered; sliders icon at right (clock settings — not captured).
- Segmented control (full width, outlined container, 12dp radius): **Timer** | **Stopwatch**; selected segment filled `primary`.
- **Timer mode**: large circular ring (~240dp, 8dp `primary` stroke, depletes as it runs) with "01:00" centered in huge
  type (~72sp). "-15s" and "+15s" as `primary` text buttons below-left and below-right of the ring. Full-width "Start"
  `PrimaryButton` (becomes Pause/Reset while running — assumed). Tapping the time likely opens a duration picker.
- **Stopwatch mode**: not captured; assume the same ring with elapsed time counting up, Start/Lap/Reset.
- This is independent of the automatic rest timer bar.

## 13. Rest Timer picker — img 23

Bottom sheet: title "Rest Timer", subtitle = exercise name (textSecondary), sliders icon at right.
Vertical **wheel picker** of durations: OFF, 5s, 10s … in 5s steps to 2min, then 15s steps (2min 30s, 2min 45s, 3min 0s …)
up to ~5min (exact list assumed; formatting is `2min 45s`, `3min 0s`). Selected item: white, larger, on a `surfaceHigh`
rounded highlight band; neighbours faded grey. Full-width "Done" `PrimaryButton` commits.
Saving applies to this exercise now and becomes its default next time.

## 14. Session note — img 27

After tapping the Note chip, a line appears below the chips: outline document icon + borderless text field,
hint "Add a session note" (textSecondary), standard text keyboard. Saved text shows in textSecondary the same way
("Set 1 at 45degree") and is displayed in Workout Detail and History. Empty note on blur → line disappears.
(The "Pinned exercise notes" from the How to tab are a separate, persistent note.)

## 15. Set Type sheet — img 24

Tap a set's label cell → bottom sheet titled "Select Set Type", one grouped `surfaceHigh` card with dividers:
| Icon | Label | Meaning |
|---|---|---|
| **W** (amber `#F5A623`) | Warm Up Set | Excluded from PRs and volume (assumed); not numbered |
| **1** (white) | Normal Set | Default |
| **F** (red `danger`) | Failure Set | Taken to failure; counts normally |
| **D** (blue `primary`) | Drop Set | Counts normally |
| ✕ (red) | Remove Set | Deletes the set (white text) |

Each type row has a grey "?" circle → small dialog explaining the type. Numbering counts only non-warm-up sets
(a warm-up then two normal sets shows W, 1, 2 — assumed, matches common behavior).

## 16. Minimized workout bar — img 36

Tapping the chevron-down slides Active Workout down and returns to the tabs (bottom nav visible). The workout,
duration and rest timer keep running. A **floating pill** sits just above the bottom navigation on every tab:
- Container: full width minus 16dp margins, ~80dp tall, fully rounded, `surface` background.
- Left: 64dp circular `surfaceHigh` button with chevron-up → re-opens Active Workout (slide up). Tapping the pill body does the same (assumed).
- Middle, two lines: a small `primary` dot (pulsing — assumed) + "**Rest** 01:02" (bold label, countdown) on line 1;
  current exercise name, ellipsized ("Decline Bench Press (Machi…"), in textSecondary on line 2.
  When no rest timer is running it most likely shows the workout duration instead, e.g. "Workout 12min 3s" (assumed — confirm).
- Right: 64dp circular `surfaceHigh` button with a red trash icon → the discard confirmation dialog.
- The content above should get bottom padding so the pill never covers the last list item.

Implementation: keep the active workout state in an app-scoped holder (a singleton repository / service exposing a
`StateFlow`), not in the screen's ViewModel, so both the pill and the full screen observe the same state. A foreground
service with an ongoing notification ("Workout in progress · Rest 01:02") keeps timers alive when the app is backgrounded.

While a workout is in progress, "Start Empty Workout" should ask "You have a workout in progress. Resume or discard it?" (assumed).

## ViewModel state sketch

```kotlin
data class ActiveWorkoutState(
  val workoutId: Long, val startedAt: Instant,
  val exercises: List<ExerciseCardState>,
  val volumeKg: Double, val completedSets: Int, val totalSets: Int,
  val restTimer: RestTimerState?,            // endsAt, totalSeconds
  val prToastQueue: List<PrEvent>,
  val workedMuscles: Map<Muscle, Float>
)
```
Persist every edit to Room immediately so a crash or process death never loses a workout.
