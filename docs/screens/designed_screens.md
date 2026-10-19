# Screens designed without screenshots

Built in Hevy's visual language. What was assumed:

- **Calendar** (Profile > Calendar): month grid, Monday first; days with a workout are filled
  `primary` circles, today is outlined in `primary`; below, that month's workouts (title, long
  date, duration, volume) open Workout Detail. No future months.
- **Statistics > advanced statistics**: shown inline on the Statistics screen instead of separate
  pushed screens: set count per muscle group (7/30/90-day chips, secondary muscles count half),
  main exercises (last 3 months, ranked by workouts then sets), monthly report vs last month.
  "Muscle distribution (Chart)" (radar) and the social Leaderboard are not built.
- **Create Exercise** (picker > Create): bottom sheet with name, equipment, primary muscle and
  exercise type (weight & reps, bodyweight reps, assisted bodyweight, duration, distance &
  duration). The new exercise is selected in the picker straight away.
- **Clock sheet, Stopwatch mode**: same ring as Timer; ring fills once per minute; Start/Pause and
  Reset. Timer: -15s/+15s, haptic tick at zero. Both keep running while the sheet is closed.
- **Workout Settings > Sounds**: reduced to one choice, "Sound + vibrate" or "Vibrate only" for
  the end of the rest timer. Warm-up calculator is an exercise-menu action ("Add Warm-up Sets":
  50/70/85% of the working weight) rather than a separate settings page.
- **Recovery**: not a Hevy screen. Front/back anatomical map (MIT paths from
  react-native-body-highlighter) colored red -> amber -> green by readiness, ready-group chips,
  per-muscle bars with hours until ready.
- **Exercise animations**: Hevy's are licensed media we can't reuse; the form demo loops the open
  dataset's start/end photos with a crossfade.
- **Food tab** (bottom nav, between Workout and Profile): not a Hevy screen. Day switcher; Eaten
  card (kcal ring vs target, protein bar, carbs/fat); Burned card (resting + digestion + daily
  activity + workouts, balance vs eaten, Health Connect connect button); meals list (breakfast,
  lunch, dinner, snacks). Add food: describe it or snap/pick a photo (AI estimate, editable before
  saving), scan a barcode (Open Food Facts, enter grams), recent foods, or manual entry. Goal sheet:
  lose / recomp (default) / maintain / gain, optional kcal and protein overrides, bike watts per
  resistance level. Daily activity comes from Health Connect (Samsung Health syncs into it); steps
  and active calories recorded during a logged workout are subtracted so they aren't counted twice.
  Daily balance card: last 7 finished days as bars (eaten − burned; below the line = deficit),
  each locked in after midnight and re-checked for 48 h for late Samsung Health syncs; days with
  no food logged get no number. Daily activity = everyday movement a step counter can't see
  (25% of resting energy: sitting/standing/chores) + walking from steps, or Samsung's active
  calories when those are higher (a watch covers all movement); never both. Photo logging: the photo shows first with an
  optional notes box ("fried in butter", "ate half"); the AI runs on "Estimate" and the notes
  override what the photo seems to show. Tapping an estimated item opens an editor where
  changing grams rescales calories and macros.
