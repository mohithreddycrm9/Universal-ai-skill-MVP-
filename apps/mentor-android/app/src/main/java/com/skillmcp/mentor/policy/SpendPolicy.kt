package com.skillmcp.mentor.policy

import java.util.Calendar
import java.util.concurrent.TimeUnit

object SpendPolicy {
    /** Hides the daily limit banner for one rolling 24-hour window (matches message limit reset). */
    fun nextRollingDayDismissMs(nowMs: Long = System.currentTimeMillis()): Long =
        nowMs + TimeUnit.HOURS.toMillis(24)

    fun formatResetTime(ms: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        return String.format("%d:%02d %s", cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM")
    }

    /** Hides the daily block card for the rolling dismiss window; does not raise the limit. */
    fun shouldHideDailyBlockUi(
        check: AllowanceCheck,
        dismissedUntilMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean =
        !check.allowed &&
            check.blockReason == AllowanceBlockReason.DAILY &&
            dismissedUntilMs > nowMs
}
