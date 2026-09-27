package com.skillmcp.mentor.data.db

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class MentorMigrationsTest {
    @Test
    fun migration5to6AddsFolderTagColumn_onExportedV5Schema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = openDatabaseAtVersion(context, version = 5)
        createV5SchemaFromExport(db)
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
    fun migration6to7CreatesReplyVersionsMatchingTheV7Export() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = openDatabaseAtVersion(context, version = 6)
        createV5SchemaFromExport(db, version = 6)
        MentorMigrations.MIGRATION_6_7.migrate(db)
        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info(reply_versions)").use { c ->
            while (c.moveToNext()) columns += c.getString(c.getColumnIndexOrThrow("name"))
        }
        assertEquals(listOf("id", "messageId", "projectId", "content", "idx", "createdAt"), columns)
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_reply_versions_messageId'").use {
            assertTrue(it.moveToFirst())
        }
        // Same DDL Room exported for v7, so Room's schema validation will accept the migrated table.
        val v7 = JSONObject(File("schemas/com.skillmcp.mentor.data.db.MentorDatabase/7.json").readText())
        val entities = v7.getJSONObject("database").getJSONArray("entities")
        val sql = (0 until entities.length()).map { entities.getJSONObject(it) }.first { it.getString("tableName") == "reply_versions" }
        assertTrue(sql.getString("createSql").contains("`idx` INTEGER NOT NULL"))
        db.close()
    }

    @Test
    fun allMigrationsRegistered() {
        assertEquals(2, MentorMigrations.ALL.size)
        assertEquals(listOf(5 to 6, 6 to 7), MentorMigrations.ALL.map { it.startVersion to it.endVersion })
    }

    private fun createV5SchemaFromExport(db: SupportSQLiteDatabase, version: Int = 5) {
        val schemaFile = File("schemas/com.skillmcp.mentor.data.db.MentorDatabase/$version.json")
        assertTrue("Missing Room schema export ${schemaFile.path}", schemaFile.exists())
        val root = JSONObject(schemaFile.readText())
        val entities = root.getJSONObject("database").getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val tableName = entity.getString("tableName")
            val createSql =
                entity
                    .getString("createSql")
                    .replace("`", "")
                    .replace("\${TABLE_NAME}", tableName)
            db.execSQL(createSql)
        }
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
