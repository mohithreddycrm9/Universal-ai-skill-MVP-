package com.skillmcp.mentor.policy

import com.skillmcp.mentor.data.db.MentorDao
import java.util.concurrent.TimeUnit

enum class AllowanceBlockReason {
    NONE,
    DAILY,
    WEEKLY,
}

data class AllowanceCheck(
    val allowed: Boolean,
    val message: String? = null,
    val dayCount: Int = 0,
    val weekCount: Int = 0,
    val dailyLimit: Int = 0,
    val weeklyLimit: Int = 0,
    val blockReason: AllowanceBlockReason = AllowanceBlockReason.NONE,
    val warningMessage: String? = null,
)

object MessageAllowanceGuard {
    private const val WARN_RATIO = 0.8

    fun evaluate(
        dayCount: Int,
        weekCount: Int,
        dailyLimit: Int,
        weeklyLimit: Int,
    ): AllowanceCheck {
        val warning = buildWarning(dayCount, weekCount, dailyLimit, weeklyLimit)
        if (dailyLimit > 0 && dayCount >= dailyLimit) {
            return AllowanceCheck(
                allowed = false,
                message =
                    "You reached today's message limit ($dailyLimit). " +
                        "It resets on a rolling 24-hour window.",
                dayCount = dayCount,
                weekCount = weekCount,
                dailyLimit = dailyLimit,
                weeklyLimit = weeklyLimit,
                blockReason = AllowanceBlockReason.DAILY,
            )
        }
        if (weeklyLimit > 0 && weekCount >= weeklyLimit) {
            return AllowanceCheck(
                allowed = false,
                message =
                    "You reached this week's message limit ($weeklyLimit). " +
                        "It resets on a rolling 7-day window.",
                dayCount = dayCount,
                weekCount = weekCount,
                dailyLimit = dailyLimit,
                weeklyLimit = weeklyLimit,
                blockReason = AllowanceBlockReason.WEEKLY,
            )
        }
        return AllowanceCheck(
            allowed = true,
            dayCount = dayCount,
            weekCount = weekCount,
            dailyLimit = dailyLimit,
            weeklyLimit = weeklyLimit,
            warningMessage = warning,
        )
    }

    suspend fun check(
        dao: MentorDao,
        dailyLimit: Int,
        weeklyLimit: Int,
    ): AllowanceCheck {
        val now = System.currentTimeMillis()
        val dayStart = now - TimeUnit.DAYS.toMillis(1)
        val weekStart = now - TimeUnit.DAYS.toMillis(7)
        return evaluate(
            dayCount = dao.countRequestsSince(dayStart),
            weekCount = dao.countRequestsSince(weekStart),
            dailyLimit = dailyLimit,
            weeklyLimit = weeklyLimit,
        )
    }

    private fun buildWarning(
        dayCount: Int,
        weekCount: Int,
        dailyLimit: Int,
        weeklyLimit: Int,
    ): String? {
        if (dailyLimit > 0 && dayCount >= dailyLimit * WARN_RATIO && dayCount < dailyLimit) {
            val pct = ((dayCount.toFloat() / dailyLimit) * 100).toInt()
            return "$pct% of today's message limit used."
        }
        if (weeklyLimit > 0 && weekCount >= weeklyLimit * WARN_RATIO && weekCount < weeklyLimit) {
            val pct = ((weekCount.toFloat() / weeklyLimit) * 100).toInt()
            return "$pct% of this week's message limit used."
        }
        return null
    }
}
