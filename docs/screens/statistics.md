# Screen: Statistics

References: `img/18_statistics_top.jpg`, `img/19_statistics_advanced.jpg`

Pushed from Profile; bottom nav remains visible (Profile selected).

## Top bar
Back arrow · "Statistics" centered (`topBar` color).

## Section 1 — Last 7 days body graph

- Title "Last 7 days body graph" (sectionTitle) + grey "?" help icon right.
- **7 day chips** in a row (equal width, 4dp gaps, `surface`, 10dp radius, ~56dp tall):
  weekday letter on top (textSecondary), date number below.
  Days with a workout: the number sits in a filled `primary` circle (white text).
  Today: small `primary` dot under the chip. Order: oldest on the left, today on the right.
- **Large `MuscleMap`**: front and back figures side by side, centered, ~250dp tall, on black.
  Muscles trained in the last 7 days tinted `muscleHighlight`; intensity (opacity) by set count.
  Tapping a day chip could filter to that day (assumed).

## Section 2 — Advanced statistics

- Section header strip: `surface` background full-bleed, "Advanced statistics" (sectionLabel).
- `NavListRow`s with dividers (leading outline icon, title 18sp, subtitle textSecondary, trailing chevron):
  1. Set count per muscle group — "Number of sets logged for each muscle group." — `ProBadge` instead of chevron in the original (unlock it in your clone).
  2. Muscle distribution (Chart) — "Compare your current and previous muscle distributions." (radar/spider chart; pentagon icon)
  3. Muscle distribution (Body) — "Weekly heat map of muscles worked."
  4. Main exercises — "List of exercises you do most often."
  5. Leaderboard Exercises — "List of the leaderboard-eligible exercises." (social feature; optional)
  6. Monthly Report — "Recap of your monthly workouts and statistics."

Detail screens for these are not captured.
