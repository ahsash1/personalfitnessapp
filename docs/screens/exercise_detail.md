# Screen: Exercise Detail

References: `img/05_exercise_detail_summary_top.jpg`, `img/06_exercise_detail_summary_records.jpg`,
`img/07_exercise_detail_history.jpg`, `img/08_exercise_detail_howto.jpg`

## Top bar + tabs (`topBar` color)

- Back arrow · exercise name (appBarTitle, single line, ellipsize) · share icon · horizontal ••• overflow.
- Tab row: **Summary · History · How to**, equal width. Selected tab text + 3dp underline in `primary`; others textSecondary.
  Swipeable pager between tabs.
- When opened from the picker, a full-width `PrimaryButton` "Add exercise" floats at the bottom of every tab.

## Summary tab

1. **Media area** — white background, full width, ~16:9-ish (~300dp tall), looping exercise animation.
   Round grey pause/play button top-right.
2. Name (22sp bold). "Primary: Chest" and "Secondary: Shoulders, Triceps" in textSecondary.
3. **Chart header**: selected point value "25 kg" (bold white) + its date "Oct 15" (primary). Right: `PeriodDropdown` "Last 3 months" (options: Last 30 days, Last 3 months, Year, All time).
4. **Line chart**: one point per workout, `primary` line 2dp with filled dots, light gridlines,
   y-axis labels "10 kg / 20 kg" on the left, x-axis dates. Tapping/dragging selects a point and updates the header.
   (Vico chart library works well in Compose.)
5. **Metric chips** (horizontal scroll, `FilterChip`): Heaviest Weight · One Rep Max · Best Set Volume (cut off as "Best…") ·
   likely Session Volume · Total Reps.
6. **Personal Records** section: gold medal + "Personal Records" (textSecondary) + "?" help icon (explains PR types). Rows with dividers:
   - Heaviest Weight — `25kg`
   - Best 1RM — `30.12kg`
   - Best Set Volume — `20kg x 15`
   - Best Session Volume — `775kg`
   Values right-aligned in `primary`.
7. **Set Records** — collapsible row with chevron. Expanded (assumed): table of best weight for each rep count (1–N reps).

## History tab

List grouped by workout, newest first. For each:
- Workout title (bold 20sp) + "15 Oct 2026, 15:00" (textSecondary), trailing chevron → that workout's detail (not captured).
- Exercise row: 40dp thumbnail + name (bold).
- Table header: `SET` · `WEIGHT & REPS`.
- Set rows: number + "20 kg x 15"; under it, PR badges as small gold medal + label ("Volume", "Weight", "1RM").
  Every other row has a `surface` background stripe (img 07, row 2).

## How to tab

- Same media area as Summary.
- Name (22sp bold), then numbered instruction steps (bold number, 17sp body text, 12dp spacing).
- Bottom section on `surface` background: "Pinned exercise notes" (textSecondary) with "Add Note" (primary) on the right.
  Pinned notes also show at the top of this exercise's card in Active Workout (assumed).
