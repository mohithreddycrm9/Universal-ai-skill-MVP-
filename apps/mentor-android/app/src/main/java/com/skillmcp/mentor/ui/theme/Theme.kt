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
import kotlin.math.roundToInt

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

@Composable
fun CodeMentorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentHue: Float = 210f,
    fontScale: Float = 1f,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val accent = accentFromHue(accentHue)
    val context = LocalContext.current
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme ->
                darkColorScheme(
                    primary = accent,
                    secondary = accent.copy(alpha = 0.8f),
                    tertiary = Color(0xFF94A3B8),
                )
            else ->
                lightColorScheme(
                    primary = accent,
                    secondary = accent.copy(alpha = 0.85f),
                    tertiary = Color(0xFF475569),
                )
        }

    val scaledTypography =
        MaterialTheme.typography.let { base ->
            val s = fontScale.coerceIn(0.85f, 1.35f)
            base.copy(
                bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize * s),
                bodyMedium = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize * s),
                titleLarge = base.titleLarge.copy(fontSize = base.titleLarge.fontSize * s),
                labelLarge = base.labelLarge.copy(fontSize = base.labelLarge.fontSize * s),
            )
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content,
    )
}
