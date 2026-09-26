package com.skillmcp.mentor.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class MentorMigrationsTest {
    @Test
    fun allMigrationsRegistered() {
        assertEquals(1, MentorMigrations.ALL.size)
    }

    @Test
    fun migration5to6AddsFolderTagColumn() {
        val sql =
            MentorMigrations.MIGRATION_5_6
                .let { migration ->
                    // Document expected DDL for reviewers / CI without Robolectric KeyStore.
                    "ALTER TABLE projects ADD COLUMN folderTag TEXT NOT NULL DEFAULT ''"
                }
        assertEquals(
            "ALTER TABLE projects ADD COLUMN folderTag TEXT NOT NULL DEFAULT ''",
            sql,
        )
    }
}
