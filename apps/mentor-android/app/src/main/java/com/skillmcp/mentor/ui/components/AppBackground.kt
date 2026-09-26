package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.skillmcp.mentor.ui.theme.AppColors

@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    if (dark) Color(0xFF0B0A12) else Color(0xFFFAFAFC),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    AppColors.AuroraMid.copy(alpha = if (dark) 0.22f else 0.14f),
                                    Color.Transparent,
                                ),
                            center = Offset(0.15f, 0.05f),
                            radius = 900f,
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    AppColors.AuroraEnd.copy(alpha = if (dark) 0.18f else 0.10f),
                                    Color.Transparent,
                                ),
                            center = Offset(1.1f, 0.25f),
                            radius = 800f,
                        ),
                    ),
        )
        content()
    }
}
