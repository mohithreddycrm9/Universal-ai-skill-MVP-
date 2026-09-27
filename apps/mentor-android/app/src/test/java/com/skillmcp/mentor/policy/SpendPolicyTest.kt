package com.skillmcp.mentor.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendPolicyTest {
    @Test
    fun rollingDismiss_is24HoursFromNow() {
        val now = 1_000_000L
        val until = SpendPolicy.nextRollingDayDismissMs(now)
        assertEquals(now + 86_400_000L, until)
    }

    @Test
    fun hideDailyBlockUi_doesNotAllowSending() {
        val blocked =
            MessageAllowanceGuard.evaluate(
                dayCount = 50,
                weekCount = 50,
                dailyLimit = 50,
                weeklyLimit = 0,
            )
        assertFalse(blocked.allowed)
        val dismissedUntil = System.currentTimeMillis() + 60_000
        assertTrue(SpendPolicy.shouldHideDailyBlockUi(blocked, dismissedUntil))
        assertFalse(blocked.allowed)
    }

    @Test
    fun hideDailyBlockUi_expiresAfterDismissWindow() {
        val blocked = MessageAllowanceGuard.evaluate(50, 50, 50, 0)
        assertFalse(
            SpendPolicy.shouldHideDailyBlockUi(
                blocked,
                dismissedUntilMs = System.currentTimeMillis() - 1,
            ),
        )
    }
}
