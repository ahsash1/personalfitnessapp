# Screen: Add Exercise (picker)

References: `img/03_add_exercise_search_focused.jpg`, `img/04_add_exercise_one_selected.jpg`

## Layout

1. **Top bar** (`topBar` color): "Cancel" (primary, left) · "Add Exercise" (centered title) · "Create" (primary, right).
2. **Search field** — `surface` rounded field, magnifier icon, hint "Search exercise".
3. **Filter buttons** — two half-width `SecondaryButton`s: "All Equipment", "All Muscles".
   Each opens a bottom sheet with a single-select list; button label changes to the chosen value.
4. **Section label** "Recent Exercises" (textSecondary). After recents, an "All Exercises" section A–Z (assumed).
5. **Exercise rows** (~76dp, divider between rows):
   - 52dp circular `ExerciseThumbnail`
   - Name (body, white, can wrap to 2 lines) + primary muscle (textSecondary) below
   - Trailing grey filled circle "i" info icon
6. **Floating "Add N exercise(s)" button** — full-width `PrimaryButton` pinned above the bottom inset,
   only visible when ≥1 exercise is selected. Text pluralizes: "Add 1 exercise", "Add 2 exercises".

## Behavior

- Tap a row → toggle selection. Selected row: background `primaryContainer`, blue circular check badge
  overlapping the top-right of the thumbnail. Multi-select allowed.
- Tap "i" → opens Exercise Detail (screens/exercise_detail.md) with an "Add exercise" button at its bottom.
- Search filters by name (case-insensitive, contains), combined with equipment + muscle filters.
  While searching, hide the "Recent" grouping and show flat results.
- "Add N exercises" → append selected exercises to the active workout in selection order, each with
  sets prefilled from its last session (same count, empty fields with placeholders). If never done before, 1 empty set.
  Then close picker.
- Cancel → close without changes. Create → custom exercise form (name, equipment, primary/secondary muscles, type) — not captured.
