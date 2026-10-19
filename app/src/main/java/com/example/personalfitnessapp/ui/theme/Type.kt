package com.example.personalfitnessapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Scale from docs/01_design_system.md, mapped onto the closest Material3
// slots. A few named styles (ExerciseName, TimerLarge, TableHeader, StatLabel)
// don't have an M3 slot and are exported separately for direct use.

val Typography = Typography(
    headlineMedium = TextStyle( // screenTitle: "Workout", username on Profile
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle( // appBarTitle: "Statistics", "Add Exercise"
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle( // sectionTitle: "Routines", "Last 7 days body graph"
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle( // sectionLabel: "Dashboard", "Recent Exercises" (apply textSecondary at call site)
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle( // body: list titles, buttons
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle( // bodySecondary (apply textSecondary at call site)
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle( // statValue: "436 kg", "2"
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
    labelMedium = TextStyle( // statLabel (apply textSecondary at call site)
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

/** exerciseName: 19sp SemiBold, primary — exercise header in active workout. */
val ExerciseNameStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 19.sp,
    lineHeight = 24.sp,
)

/** tableHeader: 13sp uppercase, textSecondary — SET / PREVIOUS / KG / REPS. */
val TableHeaderStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp,
)

/** timerLarge: 32sp Medium, tabular figures — "02:21" rest timer. */
val TimerLargeStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 32.sp,
    lineHeight = 38.sp,
)
