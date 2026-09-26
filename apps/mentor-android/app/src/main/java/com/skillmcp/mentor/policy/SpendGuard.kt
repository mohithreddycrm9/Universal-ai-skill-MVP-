package com.skillmcp.mentor.policy

import com.skillmcp.mentor.data.db.MentorDao
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class SpendBlockReason {
    NONE,
    DAILY,
    WEEKLY,
}

data class SpendCheck(
    val allowed: Boolean,
    val message: String? = null,
    val daySpend: Double = 0.0,
    val weekSpend: Double = 0.0,
    val dailyBudgetUsd: Double = 0.0,
    val weeklyBudgetUsd: Double = 0.0,
    val blockReason: SpendBlockReason = SpendBlockReason.NONE,
    val warningMessage: String? = null,
)

object SpendGuard {
    private const val WARN_RATIO = 0.8

    fun checkSpend(
        daySpend: Double,
        weekSpend: Double,
        dailyBudgetUsd: Double,
        weeklyBudgetUsd: Double,
    ): SpendCheck {
        val warning =
            buildWarning(daySpend, weekSpend, dailyBudgetUsd, weeklyBudgetUsd)
        if (dailyBudgetUsd > 0 && daySpend >= dailyBudgetUsd) {
            return SpendCheck(
                allowed = false,
                message =
                    "Daily estimated spend reached " +
                        "$${format(daySpend)} of your $${format(dailyBudgetUsd)} cap. " +
                        "Costs are estimates from the Usage tab.",
                daySpend = daySpend,
                weekSpend = weekSpend,
                dailyBudgetUsd = dailyBudgetUsd,
                weeklyBudgetUsd = weeklyBudgetUsd,
                blockReason = SpendBlockReason.DAILY,
            )
        }
        if (weeklyBudgetUsd > 0 && weekSpend >= weeklyBudgetUsd) {
            return SpendCheck(
                allowed = false,
                message =
                    "Weekly estimated spend reached " +
                        "$${format(weekSpend)} of your $${format(weeklyBudgetUsd)} cap. " +
                        "Costs are estimates from the Usage tab.",
                daySpend = daySpend,
                weekSpend = weekSpend,
                dailyBudgetUsd = dailyBudgetUsd,
                weeklyBudgetUsd = weeklyBudgetUsd,
                blockReason = SpendBlockReason.WEEKLY,
            )
        }
        return SpendCheck(
            allowed = true,
            daySpend = daySpend,
            weekSpend = weekSpend,
            dailyBudgetUsd = dailyBudgetUsd,
            weeklyBudgetUsd = weeklyBudgetUsd,
            warningMessage = warning,
        )
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

    private fun buildWarning(
        daySpend: Double,
        weekSpend: Double,
        dailyBudgetUsd: Double,
        weeklyBudgetUsd: Double,
    ): String? {
        if (dailyBudgetUsd > 0 && daySpend >= dailyBudgetUsd * WARN_RATIO && daySpend < dailyBudgetUsd) {
            return "You've used about ${percent(daySpend, dailyBudgetUsd)} of today's $${format(dailyBudgetUsd)} estimated spend cap."
        }
        if (weeklyBudgetUsd > 0 && weekSpend >= weeklyBudgetUsd * WARN_RATIO && weekSpend < weeklyBudgetUsd) {
            return "You've used about ${percent(weekSpend, weeklyBudgetUsd)} of this week's $${format(weeklyBudgetUsd)} estimated spend cap."
        }
        return null
    }

    private fun percent(spent: Double, budget: Double): String {
        if (budget <= 0) return "0%"
        return "${((spent / budget) * 100).toInt()}%"
    }

    private fun format(v: Double): String = String.format(Locale.US, "%.2f", v)
}
