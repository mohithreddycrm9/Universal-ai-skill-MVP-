package com.skillmcp.mentor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

val BubbleShapeUser = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)
val BubbleShapeAssistant = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)
val ComposerShape = RoundedCornerShape(28.dp)
