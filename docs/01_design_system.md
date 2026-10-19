# Design System

Dark-only theme (no light mode observed). Values are estimated from screenshots
(1080×2340 px, ~2.75 density → divide px by ~2.75 for dp). Tune against the images.

## Colors

| Token | Hex (approx) | Used for |
|---|---|---|
| `background` | `#000000` | Screen background (true black) |
| `surface` | `#1C1C1E` | Cards, buttons, input-cell backgrounds, dashboard tiles |
| `surfaceHigh` | `#2C2C2E` | Dialog buttons, bottom-sheet rows, day chips |
| `topBar` | `#1C1C1E` | App bars / tab rows (slightly lighter than background) |
| `primary` | `#0A84FF` | Primary buttons, links, active tab, durations, selected chips |
| `primaryContainer` | `#0B2A4A` | Active nav pill, selected list row in picker |
| `onPrimary` | `#FFFFFF` | Text on primary |
| `textPrimary` | `#FFFFFF` | Titles, values |
| `textSecondary` | `#8E8E93` | Labels, subtitles, "previous" column, placeholders |
| `divider` | `#2C2C2E` | Thin row separators |
| `success` | `#4CC22A` | Completed-set check button |
| `successRow` | `#16300E` | Completed-set row background |
| `gold` | `#F5C518` | PR medal icon, PR toast text |
| `proBadgeBg` | `#FFE600` | PRO pill background |
| `proBadgeText` | `#B89A00` | PRO pill text |
| `danger` | `#E5533D` | Discard / remove actions |
| `bannerBg` | `#2A3A4F` | "Profile is 80% finished" banner |
| `avatarDefault` | `#C1380E` | Default avatar circle (letter avatar) |
| `muscleHighlight` | `#1E88E5` | Worked muscles on body map |

## Typography

System font (looks like Roboto/Samsung One UI default). Suggested Compose scale:

| Style | Size | Weight | Example |
|---|---|---|---|
| `screenTitle` | 30sp | Bold | "Workout", username on Profile |
| `appBarTitle` | 20sp | Regular | "Statistics", "Add Exercise" |
| `sectionTitle` | 18sp | SemiBold | "Routines", "Last 7 days body graph" |
| `sectionLabel` | 17sp | Regular, textSecondary | "Dashboard", "Advanced statistics", "Recent Exercises" |
| `exerciseName` | 19sp | SemiBold, primary | Exercise header in active workout |
| `body` | 16sp | Regular | List titles, buttons |
| `bodySecondary` | 15sp | Regular, textSecondary | Subtitles |
| `statValue` | 18sp | Regular | "436 kg", "2" |
| `statLabel` | 12sp | Regular, textSecondary | "Duration", "Volume" |
| `tableHeader` | 13sp | Regular, uppercase, textSecondary | SET / PREVIOUS / KG / REPS |
| `timerLarge` | 32sp | Medium, tabular figures | "02:21" rest timer |

## Shapes & spacing

- Screen horizontal padding: 16dp.
- Corner radius: 12dp for cards/buttons/tiles, 8dp for set-table cells, full pill for chips and PRO badge.
- Primary/secondary buttons: ~40dp tall, full width or half width with 16dp gap.
- List rows: ~76dp tall with 52dp circular thumbnails (exercise picker), dividers between rows.
- Set table rows: ~44dp tall, 4dp vertical gap.

## Shared components (build these first in `ui/components/`)

- `PrimaryButton` — filled `primary`, white text, optional leading `+` icon ("Add Exercise", "Finish").
- `SecondaryButton` — `surface` fill, white text ("Add Set", "Settings", "Start Empty Workout"). Variant with `danger` text ("Discard Workout").
- `FilterChip` — pill; selected = `primary` fill + white text, unselected = `surface` fill + white text ("Duration / Volume / Reps", "Heaviest Weight / One Rep Max / Best…"). Rows of these scroll horizontally.
- `PeriodDropdown` — `primary` text + chevron-down ("Last 3 months ▾").
- `ProBadge` — yellow pill with "PRO".
- `ExerciseThumbnail` — circular image with white background, 52dp (list) / 40dp (headers).
- `LetterAvatar` — colored circle with initial.
- `DashboardTile` — `surface` card, icon + label, half width.
- `NavListRow` — leading outline icon, title + 2-line subtitle, trailing chevron or `ProBadge`.
- `StatColumn` — small grey label above value.
- `MuscleMap` — front + back body figures; muscles tinted `muscleHighlight` by intensity. Small variant (two ~30dp figures) for the workout header, large variant for Statistics.
- `PrToast` — top-anchored rounded pill (see active_workout.md).
- Bottom navigation: Material 3 `NavigationBar`, 3 items (Home, Workout, Profile), outline icons; selected item uses `primary` icon + label with a `primaryContainer` pill indicator. Background `surface`.

## Icons

Outline style, ~1.5dp stroke (Lucide/Tabler-like). Material Symbols Outlined is a close free match.
Dumbbell icon for Workout tab/Exercises; stopwatch for rest; clipboard for New Routine; magnifier for Explore.
