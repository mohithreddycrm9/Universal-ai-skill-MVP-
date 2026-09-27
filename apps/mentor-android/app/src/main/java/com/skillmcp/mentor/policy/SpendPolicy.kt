package com.skillmcp.mentor.policy

import java.util.Calendar

object SpendPolicy {
    fun nextLocalMidnightMs(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun formatResetTime(ms: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        return String.format("%d:%02d %s", cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM")
    }

    /** Hides the daily block card until midnight; does not clear the spend limit. */
    fun shouldHideDailyBlockUi(
        check: AllowanceCheck,
        dismissedUntilMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean =
        !check.allowed &&
            check.blockReason == AllowanceBlockReason.DAILY &&
            dismissedUntilMs > nowMs
}
