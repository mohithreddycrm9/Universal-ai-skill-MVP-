package com.skillmcp.mentor.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendPolicyTest {
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
    fun hideDailyBlockUi_expiresAfterMidnightMarker() {
        val blocked = MessageAllowanceGuard.evaluate(50, 50, 50, 0)
        assertFalse(
            SpendPolicy.shouldHideDailyBlockUi(
                blocked,
                dismissedUntilMs = System.currentTimeMillis() - 1,
            ),
        )
    }
}
