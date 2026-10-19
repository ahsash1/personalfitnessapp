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
