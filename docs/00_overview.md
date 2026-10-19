# Workout Tracker Clone — Overview

Personal-use rebuild of a strength-training logger, modeled on the Hevy app's UX.
Built with Kotlin + Jetpack Compose + Material 3. All specs in this folder were
written from Hevy screenshots (not included); read the relevant screen spec before building a screen.

## App map

```
Bottom nav (3 tabs)
├── Home            → screens/home.md (feed card ••• menu: share/save as routine/copy/edit/delete)
├── Workout         → screens/workout_tab.md
│   ├── Start Empty Workout → Active Workout (screens/active_workout.md)
│   │   ├── Add Exercise    → screens/add_exercise.md
│   │   │   └── (i) info    → screens/exercise_detail.md
│   │   ├── Exercise ⋮ menu, Set Type, Rest Timer, Clock (bottom sheets)
│   │   ├── Settings → screens/workout_settings.md
│   │   ├── Minimize → floating workout pill on all tabs
│   │   ├── Discard dialog
│   │   └── Finish → Save Workout → Celebration → Home  (screens/finish_flow.md)
│   ├── New Routine         (not captured yet)
│   └── Explore Routines    (not captured yet)
└── Profile         → screens/profile.md
    ├── Workout feed item → screens/workout_detail.md
    │   └── Edit Workout   → screens/edit_workout.md
    ├── Statistics  → screens/statistics.md
    ├── Exercises   (not captured yet)
    ├── Measures    (not captured yet)
    └── Calendar    (not captured yet)
```

## Spec files

| File | Contents |
|---|---|
| `01_design_system.md` | Colors, typography, spacing, shared components |
| `02_data_model.md` | Entities, Room schema, calculations (volume, 1RM, PRs) |
| `screens/workout_tab.md` | Workout tab landing |
| `screens/active_workout.md` | The core logging screen (largest spec) |
| `screens/add_exercise.md` | Exercise picker with search, filters, multi-select |
| `screens/exercise_detail.md` | Summary / History / How to tabs |
| `screens/finish_flow.md` | Finish → Save Workout → Celebration/share cards → Home |
| `screens/home.md` | Home feed, workout card, card ••• menu, suggested athletes |
| `screens/workout_settings.md` | Workout preferences (rest default, plate calc, RPE, keep awake…) |
| `screens/workout_detail.md` | Read-only past workout |
| `screens/edit_workout.md` | Edit past workout; shared metadata form for the future Save screen |
| `screens/profile.md` | Profile, weekly chart, dashboard grid |
| `screens/statistics.md` | Body heat map, advanced stats list |
| `99_open_questions.md` | Missing screens and unknown behavior |

## Screenshot index

| Image | Shows |
|---|---|
| `01_workout_tab.jpg` | Workout tab |
| `02_active_workout_empty.jpg` | Active workout, no exercises (empty state) |
| `03_add_exercise_search_focused.jpg` | Exercise picker, search focused |
| `04_add_exercise_one_selected.jpg` | Exercise picker, one exercise selected |
| `05_exercise_detail_summary_top.jpg` | Exercise detail, Summary tab top |
| `06_exercise_detail_summary_records.jpg` | Summary tab scrolled to Personal Records |
| `07_exercise_detail_history.jpg` | History tab |
| `08_exercise_detail_howto.jpg` | How to tab |
| `09_active_workout_exercise_added.jpg` | Exercise added, sets prefilled from last session |
| `10_active_workout_exercise_menu_sheet.jpg` | Exercise ⋮ bottom sheet |
| `11_active_workout_editing_reps.jpg` | Typing into a reps field |
| `12_active_workout_pr_toast_1rm.jpg` | Set completed → rest timer + "Best 1RM" PR toast |
| `13_active_workout_pr_toast_weight.jpg` | Second set completed → "Heaviest Weight" PR toast |
| `14_active_workout_editing_completed_set.jpg` | Editing a completed set's weight |
| `15_active_workout_field_cleared_placeholder.jpg` | Cleared field showing placeholder hint |
| `16_active_workout_discard_dialog.jpg` | Discard workout confirmation |
| `17_profile.jpg` | Profile tab |
| `18_statistics_top.jpg` | Statistics: last 7 days body graph |
| `19_statistics_advanced.jpg` | Statistics: advanced statistics list |
| `20_workout_detail.jpg` | Past workout detail with PR badges and exercise note |
| `21_edit_workout_top.jpg` | Edit Workout: title, stats, photo, description, date/gym/visibility |
| `22_edit_workout_exercises.jpg` | Edit Workout: exercise cards with completed sets |
| `23_rest_timer_picker.jpg` | Rest Timer wheel picker sheet |
| `24_set_type_sheet.jpg` | Select Set Type sheet (W / 1 / F / D / Remove) |
| `25_swipe_to_delete_set.jpg` | Set row swiped left revealing Delete |
| `26_cardio_exercise_row.jpg` | Treadmill: KM / TIME columns, Rest OFF |
| `27_session_note_input.jpg` | Typing a session note |
| `28_clock_timer_sheet.jpg` | Clock sheet: Timer / Stopwatch |
| `29_finish_all_sets_done.jpg` | All sets complete, Finish tapped |
| `30_save_workout.jpg` | Save Workout screen (empty title placeholder) |
| `31_save_workout_saving.jpg` | Save Workout while saving (Save dimmed) |
| `32_celebration_share.jpg` | "Good job!" celebration, week-streak share card, share targets |
| `33_home_feed.jpg` | Home feed card + Suggested Athletes |
| `34_workout_card_menu_sheet.jpg` | Feed card ••• sheet |
| `35_edit_new_workout.jpg` | Edit Workout on the newly saved workout |
| `36_minimized_workout_bar.jpg` | Workout minimized: floating pill with rest timer above bottom nav |
| `37_workout_settings_top.jpg` | Workout Settings: navigation rows |
| `38_workout_settings_toggles.jpg` | Workout Settings: toggles |

(One original screenshot was a duplicate of the How to tab with a screenshot-tool overlay and was dropped.)

## Recommended build order

1. Project setup, theme (`01_design_system.md`), bottom navigation with 3 placeholder tabs.
2. Room database + seed exercise library (`02_data_model.md`).
3. Workout tab → Start Empty Workout → Active Workout empty state.
4. Add Exercise picker (search, recent list, multi-select).
5. Active Workout sets table: add set, edit, complete, previous values, placeholders.
6. Live stats header (duration, volume, sets, progress bar).
7. Rest timer bar.
8. PR detection + top toast + medal icons.
9. Set types, swipe-to-delete, session notes, rest timer picker, cardio rows.
10. Exercise ⋮ bottom sheet, Clock sheet, discard dialog, minimized pill + foreground service, Workout Settings (DataStore).
11. Finish flow: Save Workout (`WorkoutMetaForm`) → Celebration (confetti, streak card, share) → Home.
12. Home feed + card ••• menu, Workout Detail, Edit Workout.
13. Exercise Detail (History first, then Summary chart + PRs, then How to).
14. Profile (stats, weekly chart, dashboard grid, workout feed).
15. Statistics (7-day strip, muscle heat map, advanced list).
16. Everything in `99_open_questions.md` once those screens are captured.

## Asset note

Do not reuse the original app's exercise illustrations/animations, icons, or name.
Use an open dataset for exercises (e.g. the public-domain `free-exercise-db` on GitHub, or the wger API)
and draw or source an open-licensed front/back muscle map SVG for the body graphs.
