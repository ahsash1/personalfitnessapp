package com.example.personalfitnessapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// docs/01_design_system.md: "Dark-only theme (no light mode observed)." Hevy's
// UI doesn't offer a light mode, so this app doesn't follow system theme here.
private val HevyDarkColorScheme = darkColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = AppTextPrimary,
    secondary = AppPrimary,
    onSecondary = AppOnPrimary,
    secondaryContainer = AppSurfaceHigh,
    onSecondaryContainer = AppTextPrimary,
    tertiary = AppGold,
    onTertiary = AppBackground,
    background = AppBackground,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = AppTextSecondary,
    outline = AppDivider,
    error = AppDanger,
    onError = AppOnPrimary,
)

// docs/01_design_system.md: "Corner radius: 12dp for cards/buttons/tiles, 8dp
// for set-table cells, full pill for chips and PRO badge."
private val HevyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PersonalFitnessAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HevyDarkColorScheme,
        typography = Typography,
        shapes = HevyShapes,
        content = content
    )
}
