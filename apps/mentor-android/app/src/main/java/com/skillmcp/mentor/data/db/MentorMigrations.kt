package com.skillmcp.mentor.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MentorMigrations {
    val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE projects ADD COLUMN folderTag TEXT NOT NULL DEFAULT ''",
                )
            }
        }

    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6)

    internal fun assertV5Schema(db: SupportSQLiteDatabase) {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='chat_messages'").use { cursor ->
            if (!cursor.moveToFirst()) error("Expected chat_messages table at schema v5")
        }
    }
}
