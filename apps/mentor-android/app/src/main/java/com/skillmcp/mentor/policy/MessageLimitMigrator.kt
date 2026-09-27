package com.skillmcp.mentor.policy

import com.skillmcp.mentor.data.MentorPrefs

object MessageLimitMigrator {
    /**
     * Maps legacy USD budget prefs to message caps when the user never set explicit limits.
     */
    fun limitsFromLegacyBudgets(
        dailyBudgetUsd: Double,
        weeklyBudgetUsd: Double,
    ): Pair<Int, Int> {
        val daily =
            when {
                dailyBudgetUsd <= 0.0 -> 0
                dailyBudgetUsd < 1.0 -> 30
                dailyBudgetUsd < 3.0 -> 60
                dailyBudgetUsd < 10.0 -> 120
                else -> 200
            }
        val weekly =
            when {
                weeklyBudgetUsd <= 0.0 -> if (daily > 0) daily * 5 else 0
                weeklyBudgetUsd < 5.0 -> 150
                weeklyBudgetUsd < 20.0 -> 350
                else -> 700
            }
        return daily to weekly
    }

    fun applyIfNeeded(prefs: MentorPrefs): MentorPrefs? {
        if (prefs.hasSeenMessageLimitMigration) return null
        if (prefs.dailyMessageLimit > 0 || prefs.weeklyMessageLimit > 0) {
            return prefs.copy(hasSeenMessageLimitMigration = true)
        }
        if (prefs.dailyBudgetUsd <= 0.0 && prefs.weeklyBudgetUsd <= 0.0) {
            return prefs.copy(hasSeenMessageLimitMigration = true)
        }
        val (daily, weekly) = limitsFromLegacyBudgets(prefs.dailyBudgetUsd, prefs.weeklyBudgetUsd)
        return prefs.copy(
            dailyMessageLimit = daily,
            weeklyMessageLimit = weekly,
            dailyBudgetUsd = 0.0,
            weeklyBudgetUsd = 0.0,
            hasSeenMessageLimitMigration = true,
        )
    }
}
