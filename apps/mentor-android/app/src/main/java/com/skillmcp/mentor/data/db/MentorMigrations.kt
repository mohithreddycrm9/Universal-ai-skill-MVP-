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

    /** Regenerate: keeps every version of an assistant reply. */
    val MIGRATION_6_7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reply_versions` (`id` TEXT NOT NULL, `messageId` TEXT NOT NULL, " +
                        "`projectId` TEXT NOT NULL, `content` TEXT NOT NULL, `idx` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reply_versions_messageId` ON `reply_versions` (`messageId`)")
            }
        }

    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6, MIGRATION_6_7)

    internal fun assertV5Schema(db: SupportSQLiteDatabase) {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='chat_messages'").use { cursor ->
            if (!cursor.moveToFirst()) error("Expected chat_messages table at schema v5")
        }
    }
}
