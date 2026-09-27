package com.skillmcp.mentor.ui.theme

import com.skillmcp.mentor.ui.components.chat.starterColumns
import org.junit.Assert.assertEquals
import org.junit.Test

class FontScaleTest {
    private fun total(inApp: Float, system: Float) = inAppTypeMultiplier(inApp, system) * system

    @Test
    fun inAppScaleDoesNotStackOnLargeSystemFont() {
        assertEquals(2f, total(inApp = 1.35f, system = 2f), 0.001f)
        assertEquals(2f, total(inApp = 2f, system = 2f), 0.001f)
    }

    @Test
    fun inAppScaleRaisesSmallSystemFontToTarget() {
        assertEquals(1.35f, total(inApp = 1.35f, system = 1f), 0.001f)
        assertEquals(1.5f, total(inApp = 1.5f, system = 1.15f), 0.001f)
    }

    @Test
    fun totalIsCappedAtTwoUnlessSystemIsLarger() {
        assertEquals(2f, total(inApp = 3f, system = 1f), 0.001f)
        assertEquals(1f, inAppTypeMultiplier(2f, 2.5f), 0.001f)
    }

    @Test
    fun defaultIsIdentity() {
        assertEquals(1f, inAppTypeMultiplier(1f, 1f), 0.001f)
    }

    @Test
    fun starterCardsUseOneColumnAtLargeText() {
        assertEquals(2, starterColumns(widthDp = 411f, textScale = 1f))
        assertEquals(1, starterColumns(widthDp = 411f, textScale = 2f))
        assertEquals(1, starterColumns(widthDp = 411f, textScale = 1.3f))
        assertEquals(1, starterColumns(widthDp = 300f, textScale = 1f))
    }
}
