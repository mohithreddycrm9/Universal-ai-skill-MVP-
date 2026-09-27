package com.skillmcp.mentor.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendPolicyTest {
    @Test
    fun hideDailyBlockUi_doesNotAllowSending() {
        val blocked =
            SpendGuard.checkSpend(
                daySpend = 10.0,
                weekSpend = 10.0,
                dailyBudgetUsd = 5.0,
                weeklyBudgetUsd = 0.0,
            )
        assertFalse(blocked.allowed)
        val dismissedUntil = System.currentTimeMillis() + 60_000
        assertTrue(SpendPolicy.shouldHideDailyBlockUi(blocked, dismissedUntil))
        assertFalse(blocked.allowed)
    }

    @Test
    fun hideDailyBlockUi_expiresAfterMidnightMarker() {
        val blocked =
            SpendGuard.checkSpend(10.0, 10.0, 5.0, 0.0)
        assertFalse(
            SpendPolicy.shouldHideDailyBlockUi(
                blocked,
                dismissedUntilMs = System.currentTimeMillis() - 1,
            ),
        )
    }
}
