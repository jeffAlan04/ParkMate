package com.example.parkmate.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val BrandBlue = Color(0xFF1457D9)
val BrandBlueLight = Color(0xFFDCE7FF)
val BrandNavy = Color(0xFF0B1C48)
val BrandTeal = Color(0xFF0D9488)
val BrandTealLight = Color(0xFFD5F5EF)
val BrandAccent = Color(0xFF39A9FF)

// Neutri
val LightBackground = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceMuted = Color(0xFFEEF2F8)
val LightTextPrimary = Color(0xFF172033)
val LightTextSecondary = Color(0xFF687386)
val LightOutline = Color(0xFFD5DCE6)
val LightOutlineSoft = Color(0xFFE6EBF2)

// Semantici
val LightSuccess = Color(0xFF1E9E5A)
val LightSuccessContainer = Color(0xFFDDF5E8)
val LightOnSuccessContainer = Color(0xFF0F5132)
val LightWarning = Color(0xFFE08A00)
val LightWarningContainer = Color(0xFFFFF1D6)
val LightOnWarningContainer = Color(0xFF7A4A00)
val LightError = Color(0xFFD93025)
val LightErrorContainer = Color(0xFFFDE7E6)
val LightOnErrorContainer = Color(0xFF8C1D18)

val DarkBrandBlue = Color(0xFF7AAEFF)
val DarkBrandBlueContainer = Color(0xFF1B3F8F)
val DarkBrandTeal = Color(0xFF4FD1C1)
val DarkBrandTealContainer = Color(0xFF0F4A44)
val DarkBrandAccent = Color(0xFF7CC4FF)

val DarkBackground = Color(0xFF0B1220)
val DarkSurface = Color(0xFF121A2B)
val DarkSurfaceMuted = Color(0xFF1E2839)
val DarkTextPrimary = Color(0xFFE6EAF2)
val DarkTextSecondary = Color(0xFFA3ADBF)
val DarkOutline = Color(0xFF3A4558)
val DarkOutlineSoft = Color(0xFF2A3446)

val DarkSuccess = Color(0xFF5BD69A)
val DarkSuccessContainer = Color(0xFF0F3D27)
val DarkOnSuccessContainer = Color(0xFFBDF0D3)
val DarkWarning = Color(0xFFFFC35A)
val DarkWarningContainer = Color(0xFF4A3200)
val DarkOnWarningContainer = Color(0xFFFFE2B0)
val DarkError = Color(0xFFFF8A80)
val DarkErrorContainer = Color(0xFF7A1F19)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

@Immutable
data class ExtendedColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val brandStrong: Color,
    val accent: Color
)

val LightExtendedColors = ExtendedColors(
    success = LightSuccess,
    successContainer = LightSuccessContainer,
    onSuccessContainer = LightOnSuccessContainer,
    warning = LightWarning,
    warningContainer = LightWarningContainer,
    onWarningContainer = LightOnWarningContainer,
    brandStrong = BrandNavy,
    accent = BrandAccent
)

val DarkExtendedColors = ExtendedColors(
    success = DarkSuccess,
    successContainer = DarkSuccessContainer,
    onSuccessContainer = DarkOnSuccessContainer,
    warning = DarkWarning,
    warningContainer = DarkWarningContainer,
    onWarningContainer = DarkOnWarningContainer,
    brandStrong = BrandBlueLight,
    accent = DarkBrandAccent
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }