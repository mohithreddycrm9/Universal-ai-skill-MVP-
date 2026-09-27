package com.skillmcp.mentor.data.db

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class MentorMigrationsTest {
    @Test
    fun migration5to6AddsFolderTagColumn() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = openDatabaseAtVersion(context, version = 5)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS projects (" +
                "id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, goal TEXT NOT NULL, " +
                "updatedAt INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0)",
        )
        MentorMigrations.MIGRATION_5_6.migrate(db)
        db.query("PRAGMA table_info(projects)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "folderTag") {
                    found = true
                }
            }
            assertTrue(found)
        }
        db.close()
    }

    @Test
    fun allMigrationsRegistered() {
        assertEquals(1, MentorMigrations.ALL.size)
    }

    private fun openDatabaseAtVersion(
        context: android.content.Context,
        version: Int,
    ): SupportSQLiteDatabase {
        val config =
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("mentor-migration-unit-test.db")
                .callback(
                    object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) {}

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {}
                    },
                )
                .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }
}
