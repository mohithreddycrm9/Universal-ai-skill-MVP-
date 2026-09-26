package com.skillmcp.mentor.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendGuardTest {
    @Test
    fun allowsWhenBudgetsZero() {
        val check = SpendGuard.checkSpend(50.0, 200.0, 0.0, 0.0)
        assertTrue(check.allowed)
    }

    @Test
    fun blocksWhenDailyExceeded() {
        val check = SpendGuard.checkSpend(5.0, 5.0, 4.99, 0.0)
        assertFalse(check.allowed)
    }

    @Test
    fun blocksWhenWeeklyExceeded() {
        val check = SpendGuard.checkSpend(1.0, 10.0, 0.0, 9.99)
        assertFalse(check.allowed)
    }
}
