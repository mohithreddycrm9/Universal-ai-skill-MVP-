package com.skillmcp.mentor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )

val BubbleShapeUser = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
val BubbleShapeAssistant = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
val ComposerShape = RoundedCornerShape(12.dp)
