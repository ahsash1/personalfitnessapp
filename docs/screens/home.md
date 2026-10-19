# Screen: Home Tab (feed)

References: `img/33_home_feed.jpg`, `img/34_workout_card_menu_sheet.jpg`

## Top bar (no colored bar — sits on black)
"Home" (`screenTitle`) + small circular `surface` chevron-down button (likely feed filter: "Home" / "Discover" — assumed).
Right: search icon (find users/exercises) and bell icon (notifications). For a personal clone, the chevron and these
icons can be no-ops or hidden.

## Feed card (one per workout, newest first)
1. Header: 52dp `LetterAvatar` · username (white 18sp) + relative time "22 minutes ago" (textSecondary) · horizontal ••• button.
2. Title "Afternoon workout" (bold 22sp). (Tap card → Workout Detail.)
3. Stats: small grey labels "Time", "Volume" with values "22min", "740 kg" below (two columns, ~24dp gap).
   Likely also "Records" when PRs were set (assumed).
4. Divider.
5. Exercise summary rows (~76dp): 64dp circular thumbnail + "3 sets Incline Bench Press (Dumbbell)" /
   "1 set Treadmill" (singular/plural). Show at most ~3 rows then "See N more exercises" (assumed).
6. Divider.
7. Action row: outline thumbs-up (like), speech bubble (comment), share icons, left-aligned, ~32dp apart.
   Personal clone: keep Share, drop like/comment.
8. Full-bleed `surface` strip (~16dp) separates cards/sections.

Relative time: "just now", "N minutes ago", "N hours ago", "Yesterday", then "Oct 15".

## Card ••• menu — img 34
Bottom sheet with drag handle, one grouped `surfaceHigh` card, rows with outline icons:
- ⬆ **Share Workout** → opens the share/celebration-style card carousel (assumed) or system share.
- ⬇ **Save as Routine** → creates a Routine from the workout's exercises + sets (needs routines feature).
- ⧉ **Copy Workout** → starts a new Active Workout prefilled with the same exercises and sets (uncompleted).
  If a workout is already running, ask first.
- ✎ **Edit Workout** → screens/edit_workout.md.
- ✕ **Delete Workout** (danger) → confirm dialog → delete, recompute PRs for later workouts of the same exercises.
The same menu is what Workout Detail's ••• shows (assumed).

## Suggested Athletes (social — optional)
Section header "Suggested Athletes" (textSecondary) + "+ Invite a friend" (primary) on the right.
Horizontal row of cards (~190×300dp, `surface`, 16dp radius): ✕ dismiss top-right, 130dp circular photo,
name (ellipsized), "Featured" (textSecondary), "Follow" `PrimaryButton`.
Skip this entirely in a personal, offline clone.

## Empty state
Not captured. Suggest: dumbbell icon + "No workouts yet" + "Start a workout" button → Workout tab.
