package com.example.personalfitnessapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    /** Adds the coach chat history; existing tables are untouched. */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `chat_messages` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`role` TEXT NOT NULL, " +
                    "`text` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`proposalJson` TEXT, " +
                    "`proposalStatus` TEXT)"
            )
        }
    }

    /** Adds a per-exercise note to session_exercises (7.4a); existing rows get NULL. */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `session_exercises` ADD COLUMN `note` TEXT")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4)
}
