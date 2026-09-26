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

fun accentFromHue(hue: Float): Color {
    val h = hue.coerceIn(0f, 360f)
    return Color.hsv(h, 0.58f, if (h in 45f..65f) 0.82f else 0.9f)
}

fun tertiaryFromHue(hue: Float): Color = Color.hsv((hue + 42f) % 360f, 0.52f, 0.88f)

private fun primaryContainerFromHue(hue: Float, dark: Boolean): Color =
    if (dark) {
        Color.hsv(hue, 0.42f, 0.26f)
    } else {
        Color.hsv(hue, 0.28f, 0.97f)
    }

private fun onPrimaryContainerFromHue(hue: Float, dark: Boolean): Color =
    if (dark) {
        Color.hsv(hue, 0.35f, 0.92f)
    } else {
        Color.hsv(hue, 0.55f, 0.28f)
    }

private val LightScheme =
    lightColorScheme(
        primary = AppColors.AuroraStart,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEDE9FE),
        onPrimaryContainer = Color(0xFF312E81),
        secondary = AppColors.AuroraMid,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFCE7F3),
        onSecondaryContainer = Color(0xFF701A75),
        tertiary = AppColors.AuroraSky,
        onTertiary = Color(0xFF0C4A6E),
        tertiaryContainer = Color(0xFFE0F2FE),
        onTertiaryContainer = Color(0xFF0C4A6E),
        surface = Color(0xFFFBFAFF),
        surfaceContainerHigh = Color(0xFFF4F2FA),
        surfaceContainerHighest = Color(0xFFEBE8F4),
        onSurface = Color(0xFF1C1917),
        onSurfaceVariant = Color(0xFF57534E),
        outline = Color(0xFFD6D3D1),
        outlineVariant = Color(0xFFE7E5E4),
    )

private val DarkScheme =
    darkColorScheme(
        primary = Color(0xFFA5B4FC),
        onPrimary = Color(0xFF1E1B4B),
        primaryContainer = Color(0xFF3730A3),
        onPrimaryContainer = Color(0xFFE0E7FF),
        secondary = Color(0xFFE879F9),
        onSecondary = Color(0xFF4A044E),
        secondaryContainer = Color(0xFF701A75),
        onSecondaryContainer = Color(0xFFFDF4FF),
        tertiary = Color(0xFF67E8F9),
        onTertiary = Color(0xFF083344),
        tertiaryContainer = Color(0xFF155E75),
        onTertiaryContainer = Color(0xFFCFFAFE),
        surface = Color(0xFF0F0D14),
        surfaceContainerHigh = Color(0xFF1C1828),
        surfaceContainerHighest = Color(0xFF28243A),
        onSurface = Color(0xFFF5F5F4),
        onSurfaceVariant = Color(0xFFA8A29E),
        outline = Color(0xFF44403C),
        outlineVariant = Color(0xFF292524),
    )

@Composable
fun CodeMentorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentHue: Float = 265f,
    fontScale: Float = 1f,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val accent = accentFromHue(accentHue)
    val tertiary = tertiaryFromHue(accentHue)
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
            onPrimary = if (darkTheme) Color(0xFF1A1625) else Color.White,
            primaryContainer = primaryContainerFromHue(accentHue, darkTheme),
            onPrimaryContainer = onPrimaryContainerFromHue(accentHue, darkTheme),
            secondary = accent.copy(alpha = 0.88f),
            tertiary = tertiary,
            tertiaryContainer =
                if (darkTheme) {
                    Color.hsv((accentHue + 42f) % 360f, 0.38f, 0.22f)
                } else {
                    Color.hsv((accentHue + 42f) % 360f, 0.22f, 0.96f)
                },
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
