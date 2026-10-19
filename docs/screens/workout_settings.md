# Screen: Workout Settings

References: `img/37_workout_settings_top.jpg`, `img/38_workout_settings_toggles.jpg`

Opened from the "Settings" button in Active Workout (and probably from app Settings on Profile).
These are **global preferences** — store them in Jetpack DataStore (Preferences), not Room.

## Top bar
Back arrow · "Workout Settings" (centered) · "Done" (primary text button). Both close the screen; changes save immediately.

## Rows

Navigation rows (title left, value in textSecondary + chevron right, dividers, ~72dp):

| Row | Value shown | Behavior |
|---|---|---|
| Sounds | — | Sub-screen (not captured): rest-timer-end sound on/off + choice, set-complete sound. |
| Default Rest Timer | `Off` | Same wheel picker as the Rest Timer sheet. Used as the rest for exercises that have no saved rest of their own. |
| Previous Workout Values | `Default` | Sub-screen (not captured). Assumed options: take "Previous" from the last time you did the exercise in any workout (Default) vs. only from the same routine. |
| Warm-up Calculator | `ProBadge` | Generates warm-up sets from the working weight (e.g. 40/60/80% of working weight). Optional; unlock it in your clone. |
| Warm-up Sets | — | Sub-screen (not captured) — likely configures the warm-up percentages/reps used by the calculator. |

Toggle rows (title + M3 `Switch` on the right, description below in textSecondary 16sp, divider between rows).
Switch style: on = `primary` track + white thumb; off = `surfaceHigh` track + white thumb (no thumb icons).

| Setting | Default (as shown) | What to implement |
|---|---|---|
| Keep Awake During Workout | Off | When on, set `FLAG_KEEP_SCREEN_ON` (Compose: `LocalView.current.keepScreenOn = true`) while Active Workout is visible. |
| Plate Calculator | On | For **barbell** exercises, show a "Calculator" button above the keyboard when the KG field is focused → sheet showing plates per side for the entered weight (bar 20 kg; plates 25, 20, 15, 10, 5, 2.5, 1.25 kg — make bar weight and available plates configurable later). Greedy algorithm per side: `(target − bar) / 2`. |
| RPE Tracking | Off | When on, add an `RPE` column to weight sets (picker 6–10 in 0.5 steps, plus a short description of each value). Store `rpe: Double?` on `WorkoutSet`. |
| Smart Superset Scrolling | Off | When on, completing a set in a superset auto-scrolls the list to the next exercise in that superset group. |
| Inline Timer | On | **Duration** exercises (plank, treadmill time) get a ▶ button in the TIME cell that runs a per-set stopwatch and writes the elapsed time into the field. |
| Live Personal Record Notification | On | Gates the PR toast in Active Workout. Medal icons and saved PR badges still apply when off. |

There may be more rows below "Live Personal Record Notification" (screenshot ends there).

```kotlin
data class WorkoutPrefs(
  val defaultRestSeconds: Int? = null,        // null = Off
  val previousValuesMode: PreviousMode = PreviousMode.DEFAULT,
  val keepAwake: Boolean = false,
  val plateCalculator: Boolean = true,
  val rpeTracking: Boolean = false,
  val smartSupersetScrolling: Boolean = false,
  val inlineTimer: Boolean = true,
  val livePrNotification: Boolean = true,
  val restTimerSound: Boolean = true
)
```
