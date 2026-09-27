package com.skillmcp.mentor.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageAllowanceGuardTest {
    @Test
    fun blocksWhenDailyLimitReached() {
        val check = MessageAllowanceGuard.evaluate(50, 50, dailyLimit = 50, weeklyLimit = 0)
        assertFalse(check.allowed)
        assertFalse(check.message!!.contains('$'))
    }

    @Test
    fun warnsWithoutCurrency() {
        val check = MessageAllowanceGuard.evaluate(8, 8, dailyLimit = 10, weeklyLimit = 0)
        assertTrue(check.allowed)
        assertTrue(check.warningMessage!!.contains('%'))
        assertFalse(check.warningMessage!!.contains('$'))
    }
}
