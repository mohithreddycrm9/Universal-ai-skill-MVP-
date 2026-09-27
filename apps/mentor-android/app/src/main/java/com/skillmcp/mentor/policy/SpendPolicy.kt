package com.skillmcp.mentor.policy

import java.util.concurrent.TimeUnit

object SpendPolicy {
    /** Hides the daily limit banner for one rolling 24-hour window (matches message limit reset). */
    fun nextRollingDayDismissMs(nowMs: Long = System.currentTimeMillis()): Long =
        nowMs + TimeUnit.HOURS.toMillis(24)

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
