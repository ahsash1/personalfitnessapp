# Data Model & Calculations

Local-first with Room. Units shown are kg (make the unit a user setting later).

## Entities

```kotlin
Exercise(
  id: Long, name: String,              // "Decline Bench Press (Machine)"
  equipment: Equipment,                // MACHINE, DUMBBELL, BARBELL, CABLE, BODYWEIGHT, CARDIO_MACHINE...
  primaryMuscle: Muscle,               // CHEST
  secondaryMuscles: List<Muscle>,      // [SHOULDERS, TRICEPS]
  type: ExerciseType,                  // WEIGHT_REPS, DURATION, DISTANCE_DURATION (Treadmill)...
  imageUri: String?, animationUri: String?,
  instructions: List<String>,          // How-to steps
  isCustom: Boolean                    // created via "Create" in picker
)

Workout(
  id: Long, title: String,             // auto: "Afternoon workout" (time-of-day based), emoji allowed
  startedAt: Instant, endedAt: Instant?, // endedAt null = in progress
  description: String?,                // "How did your workout go?"
  gymId: Long?, visibility: Visibility, // EVERYONE / FOLLOWERS / PRIVATE (optional for personal use)
  mediaUris: List<String>              // photos/videos from Edit/Save screen
)

Gym(id: Long, name: String)

WorkoutExercise(
  id: Long, workoutId: Long, exerciseId: Long,
  orderIndex: Int,
  supersetGroupId: Long?,              // "Add To Superset"
  restSeconds: Int,                    // per exercise, default 150 ("Rest: 2min 30s")
  sessionNote: String?                 // "Set 1 at 45degree"
)

WorkoutSet(
  id: Long, workoutExerciseId: Long, orderIndex: Int,
  weightKg: Double?, reps: Int?,       // null = user left blank (placeholder shown)
  distanceKm: Double?, durationSec: Int?, // cardio / timed exercises
  rpe: Double?,                        // only when RPE Tracking is on (6.0–10.0)
  setType: SetType,                    // NORMAL, WARMUP, FAILURE, DROP
  isCompleted: Boolean, completedAt: Instant?
)

ExerciseNote(exerciseId: Long, text: String, pinned: Boolean)   // "Pinned exercise notes"

Routine(id, title, ...)   // needed for "Save as Routine" — editor not captured yet

BodyMeasurement(...)   // Measures screen — not captured yet
```

`Muscle` enum should map to regions in the muscle-map SVG (chest, front delts, side delts,
rear delts, biceps, triceps, forearms, abs, obliques, traps, lats, upper back, lower back,
glutes, quads, hamstrings, calves, adductors, abductors).

Global workout preferences (rest default, toggles) live in DataStore — see screens/workout_settings.md.

## Calculations

**Set volume** = `weightKg × reps`.

**Workout volume** (header "Volume") = sum of set volume for **completed sets only**.
Verified: 30×10 + 68×2 = 300 + 136 = **436 kg** (img 14). Clearing set 2's weight dropped it to 300 (img 15),
so blank/placeholder values do not count while blank.

**Workout sets** (header "Sets") = count of completed sets (including cardio sets — confirmed: 3 bench sets + 1
treadmill set = 4, img 30; volume 20×12 + 20×12 + 20×13 = 740 kg).
Example: the edited workout shows 13 sets and 2,593 kg (img 21) — format volume with a thousands separator.

**Warm-up sets** are excluded from PR detection (and, by assumption, from volume). Rest per exercise defaults to OFF
for cardio exercises and is remembered per exercise when changed.

**Editing a past workout** can change PRs: after saving, recompute PR badges for that workout and every later workout
containing the same exercises.

**Duration** = now − startedAt, ticking every second. Format: `46s`, `1min 9s`, `1h 5min`.

**Estimated 1RM** — Epley: `weight × (1 + reps / 30)`; when reps == 1, 1RM = weight.
Verified: 30 kg × 10 → **40 kg** (img 12 toast "Best 1RM - 40 kg").
(Historical record showed 30.12 kg, which doesn't exactly match Epley for 25×7 = 30.83, so the original may
blend formulas. Epley is fine.)

**Previous column** = the same set index from the most recent *completed* workout containing this exercise,
formatted `"20kg x 15"` (weight) or `"0 km in 05:00"` (cardio). If the current workout has more sets than last time, show `—`.

**Placeholders** — when an exercise is added, each new set's weight/reps fields are empty but show the previous
values as grey hint text (img 09, 15). If the user taps ✓ on a set with empty fields, fill them from the
placeholder values, then complete. Typed values render white.

**Auto workout title** by start hour: 5–11 "Morning workout", 12–16 "Afternoon workout",
17–20 "Evening workout", else "Night workout". Applied on save when the title field is left empty;
the original appears to append " 💪".

**Workout ordinal** (celebration "This is your 14th workout") = count of completed workouts including this one,
with English ordinal suffixes (1st, 2nd, 3rd, 4th, 11th–13th, 21st…).

**Week streak** = number of consecutive calendar weeks, ending with the current week, that contain ≥1 completed
workout. Celebration copy: "You've kept up your workouts for N weeks running!". (Week start = Monday, or follow
the device locale — make it a setting.)

**Finished-workout duration** is displayed rounded to minutes ("22min" for 21min 54s); the live timer shows seconds.

## Personal records (PRs)

Per exercise, track all-time bests across completed workouts:

| PR type | Definition | Label in History |
|---|---|---|
| Heaviest Weight | max `weightKg` of any set | `Weight` |
| Best 1RM | max estimated 1RM of any set | `1RM` |
| Best Set Volume | max `weight × reps` of a single set (display as `20kg x 15`) | `Volume` |
| Best Session Volume | max sum of set volume in one workout | — |

**Live PR detection during a workout:** when a set is marked complete, compare against the all-time bests
*excluding the current workout* plus sets already completed earlier in this workout. For each PR beaten,
enqueue a toast (see active_workout.md). Show toasts one at a time.

**Medal icon on set number:** within the current workout, the set that currently holds a PR for that exercise
shows a gold medal instead of its number. Only the best set keeps it — after set 2 (68×2) beat set 1 (30×10)
on both weight and 1RM, set 1 reverted to "1" (compare img 12 vs img 13).

**History tab badges** are stored per set at save time (img 07: set 1 "Volume", set 3 "Weight" + "1RM").

## Queries needed

- Recent exercises (distinct exercises from recent workouts, newest first) — picker.
- Per-exercise history grouped by workout — History tab.
- Per-exercise per-workout metric series (heaviest weight, best 1RM, best set volume, session volume, total reps) for a date range — Summary chart.
- Weekly totals of duration / volume / reps for a date range — Profile chart.
- Sets per muscle in last 7 days — Statistics heat map (primary muscle = full weight; secondary = half is a reasonable choice).
- Workout count — Profile header.
