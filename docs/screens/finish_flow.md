# Flow: Finish → Save Workout → Celebration → Home

References: `img/29_finish_all_sets_done.jpg`, `img/30_save_workout.jpg`, `img/31_save_workout_saving.jpg`,
`img/32_celebration_share.jpg`, `img/33_home_feed.jpg`, `img/35_edit_new_workout.jpg`

## Sequence

1. **Active Workout → tap "Finish"** (img 29: all 4 sets done, progress line full width, Finish shows a dimmed pressed state).
   - If some sets are incomplete: show a confirm dialog before continuing (not captured; assumed —
     "You have incomplete sets. Discard them?" with "Discard incomplete & finish" / "Cancel").
   - If no sets are completed at all: block with a message (assumed).
2. **Save Workout screen** (img 30) opens as a full-screen route.
3. **Tap Save** → button dims while saving (img 31), workout is written (`endedAt = now`, PR badges stored).
4. **Celebration screen** (img 32) with confetti.
5. Close celebration → land on **Home tab**, new workout at the top of the feed (img 33, "22 minutes ago").

## Save Workout screen — img 30

Same building blocks as Edit Workout (`WorkoutMetaForm`), with these differences:

| | Save Workout | Edit Workout |
|---|---|---|
| Top bar left | ← back arrow (returns to the running workout, nothing lost) | "Cancel" |
| Title "Save Workout" / "Edit Workout" | ✓ | ✓ |
| Title field | **empty**, bold grey placeholder "Workout title", no ✕ | prefilled + ✕ clear |
| Stats row | Duration · Volume · Sets, **no muscle map** | with muscle map |
| Photo / description / Date / Gym / Visibility | ✓ (Date = workout start, e.g. 19 Oct 2026, 3:38 PM) | ✓ |
| Exercise list | **none** | full editable cards |
| Bottom | centered "Discard Workout" text button in `danger` → same discard dialog as Active Workout | — |

- Duration on this screen is rounded to whole minutes: 21min 54s → "22min".
- **Empty title on save → auto title** ("Afternoon workout" by start hour; the Edit screen later shows it as
  "Afternoon workout 💪", so the default title appears to get a 💪 appended — img 35). Save stays enabled when the title is empty.
- Save button: `PrimaryButton`; disabled/dim + spinner while writing.

## Celebration screen — img 32

Full screen, black background, confetti particles (red, yellow, blue, green rectangles) falling from the top
for ~3s on open. Compose: a simple particle animation in a `Canvas`, or the `konfetti` library.

1. **Headline** "Good job!" (bold ~36sp, centered) and "This is your 14th workout" (18sp) — ordinal of
   total completed workouts (Profile showed 13 before this one).
2. **Round button top-right** with 🎉 party-popper emoji, `surface` circle with thin border → replays confetti (assumed).
3. **Share card carousel** — horizontal pager of rounded (24dp) cards, ~square, with page dots below (8 cards;
   active dot `primary`, others grey). Card 1 shown:
   - Deep navy background `#060B1E` with large faint diagonal rounded stripes in dark maroon (decorative).
   - 🔥 flame illustration (orange), giant "4" (bold ~90sp), "Week Streak" (bold 36sp),
     "You've kept up your workouts for 4 weeks running!" (17sp).
   - Footer: app logo + wordmark left, "@username" right.
   - **Use your own logo/name here** — don't reproduce the original wordmark.
   - Other 7 cards not captured; likely: workout summary (duration/volume/sets), muscle map, exercise list,
     PRs achieved, total volume comparison, etc. Build card 1 + a summary card first.
4. **"Share workout - Tag @…"** label (centered 20sp). Replace the handle with your own text or drop it.
5. **Share targets row** (horizontal scroll, 64dp circles + caption under each):
   Background toggle (striped icon — transparent vs solid card background, assumed) · Stories (Instagram gradient) ·
   Snapchat (yellow) · More (system share sheet) · Copy Text · Download (save card image to gallery) · Send/… (cut off).
   For a personal clone, implement **More** (share an image via `Intent.ACTION_SEND`), **Download**, and **Copy Text**.
   Render the selected card to a bitmap with `GraphicsLayer.toImageBitmap()` (Compose 1.7+).
6. Close/Done control is not visible in the screenshot (covered by a screenshot-tool overlay) — add a ✕ or "Done" at
   the top-left/bottom; back gesture also closes.
