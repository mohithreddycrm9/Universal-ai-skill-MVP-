package com.skillmcp.mentor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

enum class ThemeMode { SYSTEM, LIGHT, DARK }

fun ThemeMode.userLabel(): String =
    when (this) {
        ThemeMode.SYSTEM -> "Match phone"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

fun ThemeMode.resolvesDark(systemDark: Boolean): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

/** @deprecated Brand uses fixed indigo; kept for settings slider compatibility. */
fun accentFromHue(hue: Float, dark: Boolean): Color {
    val h = hue.coerceIn(0f, 360f)
    return if (dark) {
        Color.hsv(h, 0.32f, 0.82f)
    } else {
        Color.hsv(h, 0.38f, 0.38f)
    }
}

fun contentColorOn(background: Color): Color {
    val r = background.red
    val g = background.green
    val b = background.blue
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    return if (luminance > 0.55f) Color(0xFF171717) else Color.White
}

private val LightScheme =
    lightColorScheme(
        primary = BrandColors.Indigo,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEEF2FF),
        onPrimaryContainer = Color(0xFF312E81),
        secondary = BrandColors.Coral,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE4E8),
        onSecondaryContainer = Color(0xFF881337),
        tertiary = Color(0xFF64748B),
        onTertiary = Color.White,
        background = BrandColors.LightSurface,
        onBackground = Color(0xFF0F172A),
        surface = BrandColors.LightSurface,
        onSurface = Color(0xFF0F172A),
        surfaceContainerLow = Color(0xFFF4F4F8),
        surfaceContainer = Color(0xFFEEEEF4),
        surfaceContainerHigh = BrandColors.LightCard,
        surfaceContainerHighest = Color(0xFFE8E8F0),
        onSurfaceVariant = Color(0xFF64748B),
        outline = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFFE2E8F0),
        error = Color(0xFFDC2626),
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF7F1D1D),
    )

private val DarkScheme =
    darkColorScheme(
        primary = BrandColors.IndigoDark,
        onPrimary = Color.White,
        primaryContainer = Color(0xFF312E81),
        onPrimaryContainer = Color(0xFFE0E7FF),
        secondary = BrandColors.Coral,
        onSecondary = Color(0xFF1F0A12),
        secondaryContainer = Color(0xFF4C1D2E),
        onSecondaryContainer = Color(0xFFFFD5DD),
        tertiary = Color(0xFF94A3B8),
        onTertiary = Color(0xFF0F172A),
        background = BrandColors.Charcoal,
        onBackground = Color(0xFFF1F5F9),
        surface = BrandColors.Charcoal,
        onSurface = Color(0xFFF1F5F9),
        surfaceContainerLow = Color(0xFF13161C),
        surfaceContainer = BrandColors.CharcoalCard,
        surfaceContainerHigh = Color(0xFF1C2028),
        surfaceContainerHighest = Color(0xFF252A34),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF334155),
        outlineVariant = Color(0xFF1E293B),
        error = Color(0xFFF87171),
        errorContainer = Color(0xFF450A0A),
        onErrorContainer = Color(0xFFFECACA),
    )

@Composable
fun CodeMentorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentHue: Float = 239f,
    fontScale: Float = 1f,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val fontFamily = mentorFontFamily()
    val base =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkScheme
            else -> LightScheme
        }
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            base
        } else {
            base.copy(
                primary = if (darkTheme) BrandColors.IndigoDark else BrandColors.Indigo,
                onPrimary = Color.White,
            )
        }

    val baseType = mentorTypography(fontFamily)
    val s = fontScale.coerceIn(0.85f, 2f)
    fun TextStyle.scaled() = copy(fontSize = fontSize * s, lineHeight = lineHeight * s)
    val scaledTypography =
        baseType.copy(
            headlineLarge = baseType.headlineLarge.scaled(),
            headlineMedium = baseType.headlineMedium.scaled(),
            titleLarge = baseType.titleLarge.scaled(),
            titleMedium = baseType.titleMedium.scaled(),
            bodyLarge = baseType.bodyLarge.scaled(),
            bodyMedium = baseType.bodyMedium.scaled(),
            labelLarge = baseType.labelLarge.scaled(),
        )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        shapes = AppShapes,
        content = content,
    )
}
