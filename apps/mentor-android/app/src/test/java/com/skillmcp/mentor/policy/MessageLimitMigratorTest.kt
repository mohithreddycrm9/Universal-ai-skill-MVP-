package com.skillmcp.mentor.policy

import com.skillmcp.mentor.data.MentorPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MessageLimitMigratorTest {
    @Test
    fun applyIfNeeded_mapsLegacyBudgets() {
        val prefs =
            MentorPrefs(
                dailyBudgetUsd = 2.0,
                weeklyBudgetUsd = 10.0,
            )
        val migrated = MessageLimitMigrator.applyIfNeeded(prefs)
        assertNotNull(migrated)
        assertEquals(60, migrated!!.dailyMessageLimit)
        assertEquals(350, migrated.weeklyMessageLimit)
        assertEquals(0.0, migrated.dailyBudgetUsd, 0.001)
    }

    @Test
    fun applyIfNeeded_skipsWhenAlreadyMigrated() {
        val prefs = MentorPrefs(hasSeenMessageLimitMigration = true, dailyBudgetUsd = 5.0)
        assertNull(MessageLimitMigrator.applyIfNeeded(prefs))
    }
}
