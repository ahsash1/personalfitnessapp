# Screen: Workout Detail

Reference: `img/20_workout_detail.jpg`

Opened from a workout in the Profile feed, Home feed, or an exercise's History tab (chevron row).
Bottom navigation stays visible.

## Layout

1. **Top bar**: back arrow · "Workout Detail" (centered) · horizontal ••• menu → same bottom sheet as the feed card
   (Share Workout, Save as Routine, Copy Workout, Edit Workout, Delete Workout — see screens/home.md).
2. **Section row**: "Workout" label (sectionLabel, textSecondary) on the left, "Edit Workout" link (primary) on the right
   → opens screens/edit_workout.md.
3. Above this (scrolled out of view in the screenshot) there is presumably a summary header: title, date, duration, volume,
   sets, records, muscle map. Reuse the Edit Workout header layout read-only until it's captured.
4. **Per exercise** (read-only):
   - 40dp thumbnail + exercise name in `exerciseName` style (primary, tappable → Exercise Detail).
   - Exercise session note if present: outline document icon + note text in textSecondary ("3rd set after 8 min rest").
   - Table header: `SET` · `WEIGHT & REPS`.
   - Set rows: set label (number, or W/F/D letter for set types) + "40 kg x 10";
     PR badges under the value (gold medal + "Weight", "Volume", "1RM"), multiple per set allowed.
   - Every second row has a `surface` stripe (full-bleed).
   - Cardio rows show distance/time instead, e.g. "0 km in 05:00" (assumed same format as the Previous column).
