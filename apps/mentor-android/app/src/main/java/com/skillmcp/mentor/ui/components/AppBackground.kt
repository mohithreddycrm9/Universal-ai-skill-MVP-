package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.skillmcp.mentor.ui.theme.AppColors

@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                scheme.surface,
                                if (dark) Color(0xFF12101A) else Color(0xFFF8F6FF),
                            ),
                    ),
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
                                    scheme.primary.copy(alpha = if (dark) 0.28f else 0.16f),
                                    Color.Transparent,
                                ),
                            center = Offset(0.12f, 0.02f),
                            radius = 1200f,
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
                                    scheme.tertiary.copy(alpha = if (dark) 0.2f else 0.12f),
                                    Color.Transparent,
                                ),
                            center = Offset(1.05f, 0.18f),
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
                                    AppColors.AuroraFuchsia.copy(alpha = if (dark) 0.14f else 0.08f),
                                    Color.Transparent,
                                ),
                            center = Offset(0.5f, 1.05f),
                            radius = 1000f,
                        ),
                    ),
        )
        content()
    }
}
