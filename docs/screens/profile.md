# Screen: Profile Tab

Reference: `img/17_profile.jpg`

## Layout (scrollable)

1. **Header row**: username "ahsash" (`screenTitle`). Right icons: pencil (edit profile), share/upload, gear (settings).
2. **Identity row**: 80dp `LetterAvatar` (initial "R" on `avatarDefault`) · name (bold 20sp) above three `StatColumn`s:
   Workouts `13` · Followers `0` · Following `0`.
   (For a personal clone, you can drop Followers/Following or keep them static.)
3. **Completion banner**: full-width `bannerBg` card, 12dp radius, "Your profile is 80% finished" + → arrow. Tap → edit profile.
4. **Chart header**: "**0 hours** this week" (bold value + textSecondary rest). Right: `PeriodDropdown` "Last 3 months".
   The headline changes with the metric: "X kg this week", "X reps this week".
5. **Bar chart**: one bar per week, `primary` bars, y-axis "0 hrs / 2 hrs / 4 hrs" with gridlines, x-axis dates every 2 weeks ("Aug 1 … Oct 10").
   Tap a bar → headline shows that week's value (assumed).
6. **Metric chips**: Duration (selected) · Volume · Reps.
7. **"Dashboard"** label, then 2×2 grid of `DashboardTile`s (icon + label, left-aligned):
   📈 Statistics · 🏋 Exercises · 🧍 Measures · 📅 Calendar.
8. **"Workouts"** label, then workout feed cards: avatar + username + "Thursday, Oct 15, 2026" + ••• menu;
   title "Afternoon workout 💪" … (rest of card not captured — likely duration/volume/records summary and exercise list).
9. Bottom navigation, Profile selected.

## Navigation

- Statistics → screens/statistics.md
- Exercises / Measures / Calendar → not captured (see open questions).
