package com.example.parkmate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkBrandBlue,
    onPrimary = Color(0xFF00296B),
    primaryContainer = DarkBrandBlueContainer,
    onPrimaryContainer = BrandBlueLight,

    secondary = DarkBrandTeal,
    onSecondary = Color(0xFF003731),
    secondaryContainer = DarkBrandTealContainer,
    onSecondaryContainer = Color(0xFFBFF3EA),

    tertiary = DarkBrandAccent,
    onTertiary = Color(0xFF00315A),
    tertiaryContainer = Color(0xFF0D3F6B),
    onTertiaryContainer = Color(0xFFD3EAFF),

    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceMuted,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = DarkBrandBlue,

    surfaceBright = Color(0xFF283349),
    surfaceDim = DarkBackground,
    surfaceContainerLowest = Color(0xFF0A1120),
    surfaceContainerLow = Color(0xFF101828),
    surfaceContainer = Color(0xFF151E30),
    surfaceContainerHigh = Color(0xFF1B2538),
    surfaceContainerHighest = Color(0xFF222D42),

    outline = DarkOutline,
    outlineVariant = DarkOutlineSoft,

    error = DarkError,
    onError = Color(0xFF5F1410),
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,

    inverseSurface = DarkTextPrimary,
    inverseOnSurface = Color(0xFF1B2433),
    inversePrimary = BrandBlue,
    scrim = Color.Black

)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandNavy,

    secondary = BrandTeal,
    onSecondary = Color.White,
    secondaryContainer = BrandTealLight,
    onSecondaryContainer = Color(0xFF04443D),

    tertiary = BrandAccent,
    onTertiary = BrandNavy,
    tertiaryContainer = Color(0xFFDDF0FF),
    onTertiaryContainer = BrandNavy,

    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceMuted,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = BrandBlue,

    // Senza questi Material 3 usa toni violacei di default per card, sheet e barre
    surfaceBright = LightSurface,
    surfaceDim = Color(0xFFDCE2EC),
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = Color(0xFFF9FAFD),
    surfaceContainer = Color(0xFFF1F4F9),
    surfaceContainerHigh = Color(0xFFEBEFF5),
    surfaceContainerHighest = Color(0xFFE4E9F1),

    outline = LightOutline,
    outlineVariant = LightOutlineSoft,

    error = LightError,
    onError = Color.White,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,

    inverseSurface = Color(0xFF1B2433),
    inverseOnSurface = Color(0xFFF1F4F9),
    inversePrimary = Color(0xFFAFC6FF),
    scrim = Color.Black

)

@Composable
fun ParkMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (darkTheme)
            DarkColorScheme
        else
            LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ParkMateTypography,
            shapes = ParkMateShapes,
            content = content
        )
    }
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current