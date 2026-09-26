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

private fun accentFromHue(hue: Float): Color {
    val h = hue.coerceIn(0f, 360f)
    val c = 0.55f
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val (r1, g1, b1) =
        when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
    val m = 0.12f
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
    )
}

private val LightScheme =
    lightColorScheme(
        primary = AppColors.AuroraStart,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE0E7FF),
        onPrimaryContainer = Color(0xFF1E1B4B),
        secondary = AppColors.AuroraMid,
        surface = Color(0xFFFAFAFC),
        surfaceContainerHigh = Color(0xFFF1F5F9),
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF64748B),
        outlineVariant = Color(0xFFE2E8F0),
    )

private val DarkScheme =
    darkColorScheme(
        primary = Color(0xFF818CF8),
        onPrimary = Color(0xFF1E1B4B),
        primaryContainer = Color(0xFF3730A3),
        onPrimaryContainer = Color(0xFFE0E7FF),
        secondary = Color(0xFFA78BFA),
        surface = Color(0xFF0B0A12),
        surfaceContainerHigh = Color(0xFF1A1828),
        onSurface = Color(0xFFF8FAFC),
        onSurfaceVariant = Color(0xFF94A3B8),
        outlineVariant = Color(0xFF334155),
    )

@Composable
fun CodeMentorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentHue: Float = 210f,
    fontScale: Float = 1f,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val accent = accentFromHue(accentHue)
    val context = LocalContext.current
    val base =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkScheme
            else -> LightScheme
        }
    val colorScheme =
        base.copy(
            primary = accent,
            secondary = accent.copy(alpha = 0.85f),
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
