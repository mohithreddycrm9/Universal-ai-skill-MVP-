package com.skillmcp.mentor.policy

import com.skillmcp.mentor.data.db.MentorDao
import java.util.concurrent.TimeUnit

data class SpendCheck(
    val allowed: Boolean,
    val message: String? = null,
)

object SpendGuard {
    fun checkSpend(
        daySpend: Double,
        weekSpend: Double,
        dailyBudgetUsd: Double,
        weeklyBudgetUsd: Double,
    ): SpendCheck {
        if (dailyBudgetUsd > 0 && daySpend >= dailyBudgetUsd) {
            return SpendCheck(
                allowed = false,
                message = "Daily spend limit reached ($${format(dailyBudgetUsd)}). Adjust limits in Settings or try tomorrow.",
            )
        }
        if (weeklyBudgetUsd > 0 && weekSpend >= weeklyBudgetUsd) {
            return SpendCheck(
                allowed = false,
                message = "Weekly spend limit reached ($${format(weeklyBudgetUsd)}). Adjust limits in Settings.",
            )
        }
        return SpendCheck(allowed = true)
    }

    suspend fun check(
        dao: MentorDao,
        dailyBudgetUsd: Double,
        weeklyBudgetUsd: Double,
    ): SpendCheck {
        val now = System.currentTimeMillis()
        val dayStart = now - TimeUnit.DAYS.toMillis(1)
        val weekStart = now - TimeUnit.DAYS.toMillis(7)
        return checkSpend(
            daySpend = dao.sumSpendSince(dayStart),
            weekSpend = dao.sumSpendSince(weekStart),
            dailyBudgetUsd = dailyBudgetUsd,
            weeklyBudgetUsd = weeklyBudgetUsd,
        )
    }

    private fun format(v: Double): String = String.format("%.2f", v)
}
