package com.skillmcp.mentor.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room schema version history — add new Migration objects when version increments. */
object MentorMigrations {
    val ALL: Array<Migration> = emptyArray()

    /** Placeholder documenting v5 baseline; future upgrades add Migration(5, 6) { ... }. */
    internal fun assertV5Schema(db: SupportSQLiteDatabase) {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='chat_messages'").use { cursor ->
            if (!cursor.moveToFirst()) error("Expected chat_messages table at schema v5")
        }
    }
}
