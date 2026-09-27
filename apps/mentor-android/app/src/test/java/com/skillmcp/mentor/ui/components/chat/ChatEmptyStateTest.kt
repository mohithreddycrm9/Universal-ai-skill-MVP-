package com.skillmcp.mentor.ui.components.chat

import com.skillmcp.mentor.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatEmptyStateTest {
    @Test
    fun dayPartBoundaries() {
        assertEquals(DayPart.EVENING, dayPartFor(4))
        assertEquals(DayPart.MORNING, dayPartFor(5))
        assertEquals(DayPart.MORNING, dayPartFor(11))
        assertEquals(DayPart.AFTERNOON, dayPartFor(12))
        assertEquals(DayPart.AFTERNOON, dayPartFor(16))
        assertEquals(DayPart.EVENING, dayPartFor(17))
        assertEquals(DayPart.EVENING, dayPartFor(23))
    }

    @Test
    fun greetingUsesFirstNameOnlyAndNoPlaceholder() {
        assertEquals("Mohith", greetingName("  Mohith Reddy "))
        assertNull(greetingName(""))
        assertNull(greetingName("   "))
    }

    @Test
    fun greetingResourcePicksNamedVariantOnlyWithAName() {
        assertEquals(R.string.greeting_morning_name, greetingRes(DayPart.MORNING, hasName = true))
        assertEquals(R.string.greeting_morning, greetingRes(DayPart.MORNING, hasName = false))
        assertEquals(R.string.greeting_evening_name, greetingRes(DayPart.EVENING, hasName = true))
        assertEquals(R.string.greeting_afternoon, greetingRes(DayPart.AFTERNOON, hasName = false))
    }
}
