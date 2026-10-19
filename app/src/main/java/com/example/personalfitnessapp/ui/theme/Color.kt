package com.example.personalfitnessapp.ui.theme

import androidx.compose.ui.graphics.Color

// Dark-only palette matching docs/01_design_system.md (reverse-engineered from
// Hevy screenshots). Kept as named tokens rather than only a ColorScheme so
// components can reach for e.g. successRow / gold / bannerBg directly, since
// Material3's ColorScheme has no slot for several of these.

val AppBackground = Color(0xFF000000)
val AppSurface = Color(0xFF1C1C1E)
val AppSurfaceHigh = Color(0xFF2C2C2E)
val AppTopBar = Color(0xFF1C1C1E)

val AppPrimary = Color(0xFF0A84FF)
val AppPrimaryContainer = Color(0xFF0B2A4A)
val AppOnPrimary = Color(0xFFFFFFFF)

val AppTextPrimary = Color(0xFFFFFFFF)
val AppTextSecondary = Color(0xFF8E8E93)
val AppDivider = Color(0xFF2C2C2E)

val AppSuccess = Color(0xFF4CC22A)
val AppSuccessRow = Color(0xFF16300E)

val AppGold = Color(0xFFF5C518)
val AppProBadgeBg = Color(0xFFFFE600)
val AppProBadgeText = Color(0xFFB89A00)

val AppDanger = Color(0xFFE5533D)
val AppBannerBg = Color(0xFF2A3A4F)
val AppAvatarDefault = Color(0xFFC1380E)
val AppMuscleHighlight = Color(0xFF1E88E5)
