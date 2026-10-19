# Screen: Edit Workout

References: `img/21_edit_workout_top.jpg`, `img/22_edit_workout_exercises.jpg`, `img/23_rest_timer_picker.jpg`

Opened from Workout Detail → "Edit Workout". Full screen, bottom nav hidden.
Confirmed: the post-Finish "Save Workout" screen uses the same header block (see screens/finish_flow.md for the
differences). Build it once as a shared composable `WorkoutMetaForm` and reuse it for both.
Also reachable from the feed card ••• menu → Edit Workout (img 34). `img/35` shows a freshly saved workout being
edited: Date = start time (19 Oct 2026, 3:38 PM), Duration 22min, Volume 740 kg, Sets 4 (the treadmill set counts).

## Top bar
"Cancel" (primary text, left) · "Edit Workout" (centered) · "Save" compact `PrimaryButton` (right).
Progress line under the bar is full width here because every set is completed (same rule as Active Workout).

## Header block (scrolls with content)

1. **Title**: editable single-line text field styled as a heading (bold 26sp, no border) — "Afternoon workout 💪"
   (emoji allowed). Grey filled circular ✕ at the right clears the field.
2. **Stats row**: Duration "51min" (primary) · Volume "2,593 kg" (thousands separator) · Sets "13" · mini muscle map
   tinted by worked muscles. Divider below.
   Duration format for finished workouts: `51min`, `1h 12min`.
3. **Media picker**: full-width box ~110dp tall with a **dashed** 1.5dp `surfaceHigh` border, 12dp radius;
   centered image-plus icon + "Add a photo / video". Opens the Android photo picker (`PickVisualMedia`).
   Show thumbnails in a horizontal row once added (assumed).
4. **Description**: small label "Description" (textSecondary) + borderless multiline field,
   hint "How did your workout go? Leave some notes here...". Divider below.
5. **Settings rows** (icon · label · value in textSecondary · chevron, dividers between, ~64dp tall):
   - 📅 Date — "15 Oct 2026, 3:00 PM" → date picker then time picker; changes `startedAt` (keep duration).
   - 📍 Gym — "Select" → simple list of saved gym names + "Add gym" (personal clone: free text is fine).
   - 👁 Visibility — "Everyone" → Everyone / Followers / Private (social; optional for personal use — can drop).
6. **Section break**: full-bleed `surface` strip (~16dp tall) separating metadata from exercises.

## Exercise list

Identical exercise cards to Active Workout (see active_workout.md): header, Rest/Note chips, session note line,
sets table with completed (green) rows, "+ Add Set". All the same interactions apply: set-type sheet, swipe to delete,
edit values, un-complete. Then "+ Add Exercise" at the bottom (assumed — same as Active Workout).

Rest timer changes made here are saved as the exercise's default for future workouts (assumed).

## Save / Cancel
- Save → validate (title not blank), recalculate volume/sets/PR badges for this workout **and any later workouts of the
  same exercises** (editing history can change which sets are PRs), write in one Room transaction, pop back.
- Cancel with unsaved changes → confirm "Discard changes?" (assumed). Edit on a working copy in the ViewModel,
  never directly on the saved rows.
