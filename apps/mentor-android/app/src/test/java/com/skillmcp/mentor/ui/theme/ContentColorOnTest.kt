package com.skillmcp.mentor.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentColorOnTest {
    @Test
    fun picksLightTextOnDarkPrimary() {
        val darkBlue = Color(0xFF1D4ED8)
        assertEquals(Color.White, contentColorOn(darkBlue))
    }

    @Test
    fun picksDarkTextOnLightPrimary() {
        val lightBlue = Color.hsv(220f, 0.32f, 0.82f)
        assertEquals(Color(0xFF171717), contentColorOn(lightBlue))
    }
}
