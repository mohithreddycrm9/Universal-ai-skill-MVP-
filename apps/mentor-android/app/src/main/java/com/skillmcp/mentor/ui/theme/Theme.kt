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

enum class ThemeMode { SYSTEM, LIGHT, DARK }

fun ThemeMode.resolvesDark(systemDark: Boolean): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

/** Subtle accent shift for Settings — low chroma. */
fun accentFromHue(hue: Float, dark: Boolean): Color {
    val h = hue.coerceIn(0f, 360f)
    return if (dark) {
        Color.hsv(h, 0.32f, 0.82f)
    } else {
        Color.hsv(h, 0.38f, 0.38f)
    }
}

/** Text/icon color that meets contrast on filled primary buttons and chips. */
fun contentColorOn(background: Color): Color {
    val r = background.red
    val g = background.green
    val b = background.blue
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    return if (luminance > 0.55f) Color(0xFF171717) else Color.White
}

private val LightScheme =
    lightColorScheme(
        primary = Color(0xFF1D4ED8),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEFF6FF),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = Color(0xFF475569),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1F5F9),
        onSecondaryContainer = Color(0xFF334155),
        tertiary = Color(0xFF64748B),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF8FAFC),
        onTertiaryContainer = Color(0xFF334155),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF171717),
        surface = Color(0xFFFAFAFA),
        onSurface = Color(0xFF171717),
        surfaceContainerLow = Color(0xFFF5F5F5),
        surfaceContainer = Color(0xFFF0F0F0),
        surfaceContainerHigh = Color(0xFFEAEAEA),
        surfaceContainerHighest = Color(0xFFE5E5E5),
        onSurfaceVariant = Color(0xFF525252),
        outline = Color(0xFFD4D4D4),
        outlineVariant = Color(0xFFE5E5E5),
        error = Color(0xFFB91C1C),
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF7F1D1D),
    )

private val DarkScheme =
    darkColorScheme(
        primary = Color(0xFF93C5FD),
        onPrimary = Color(0xFF0C1929),
        primaryContainer = Color(0xFF1E3A5F),
        onPrimaryContainer = Color(0xFFDBEAFE),
        secondary = Color(0xFF94A3B8),
        onSecondary = Color(0xFF0F172A),
        secondaryContainer = Color(0xFF334155),
        onSecondaryContainer = Color(0xFFE2E8F0),
        tertiary = Color(0xFF94A3B8),
        onTertiary = Color(0xFF0F172A),
        tertiaryContainer = Color(0xFF1E293B),
        onTertiaryContainer = Color(0xFFCBD5E1),
        background = Color(0xFF121212),
        onBackground = Color(0xFFFAFAFA),
        surface = Color(0xFF121212),
        onSurface = Color(0xFFFAFAFA),
        surfaceContainerLow = Color(0xFF1A1A1A),
        surfaceContainer = Color(0xFF1F1F1F),
        surfaceContainerHigh = Color(0xFF262626),
        surfaceContainerHighest = Color(0xFF2E2E2E),
        onSurfaceVariant = Color(0xFFA3A3A3),
        outline = Color(0xFF404040),
        outlineVariant = Color(0xFF2E2E2E),
        error = Color(0xFFF87171),
        errorContainer = Color(0xFF450A0A),
        onErrorContainer = Color(0xFFFECACA),
    )

@Composable
fun CodeMentorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentHue: Float = 220f,
    fontScale: Float = 1f,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val accent = accentFromHue(accentHue, darkTheme)
    val context = LocalContext.current
    val base =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkScheme
            else -> LightScheme
        }
    val onAccent = contentColorOn(accent)
    val colorScheme =
        base.copy(
            primary = accent,
            onPrimary = onAccent,
        )

    val scaledTypography =
        AppTypography.let { baseType ->
            val s = fontScale.coerceIn(0.85f, 1.35f)
            baseType.copy(
                bodyLarge = baseType.bodyLarge.copy(fontSize = baseType.bodyLarge.fontSize * s),
                bodyMedium = baseType.bodyMedium.copy(fontSize = baseType.bodyMedium.fontSize * s),
                titleLarge = baseType.titleLarge.copy(fontSize = baseType.titleLarge.fontSize * s),
                labelLarge = baseType.labelLarge.copy(fontSize = baseType.labelLarge.fontSize * s),
            )
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        shapes = AppShapes,
        content = content,
    )
}
