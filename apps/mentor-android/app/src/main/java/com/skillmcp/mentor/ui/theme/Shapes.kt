package com.skillmcp.mentor.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(MentorDimens.CardRadius),
        extraLarge = RoundedCornerShape(MentorDimens.SheetRadius),
    )

val BubbleShapeUser = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
val BubbleShapeAssistant = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
val ComposerShape = RoundedCornerShape(28.dp)
val PillShape = RoundedCornerShape(50.dp)
val SendButtonShape = CircleShape
