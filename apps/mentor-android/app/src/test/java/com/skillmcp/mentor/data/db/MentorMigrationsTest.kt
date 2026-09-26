package com.skillmcp.mentor.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class MentorMigrationsTest {
    @Test
    fun v5BaselineHasNoMigrationsYet() {
        assertEquals(0, MentorMigrations.ALL.size)
    }
}
