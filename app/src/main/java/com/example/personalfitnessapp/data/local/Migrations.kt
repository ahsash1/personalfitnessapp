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

    /**
     * Hevy-style logging: workout title/gym/photos/in-progress draft, set types, cardio
     * values, PR badges, per-exercise rest and supersets, pinned notes, custom exercise
     * types, and the rest timer's total length. Every column is nullable, so existing
     * rows read as "normal set, no extras".
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "ALTER TABLE `workout_sessions` ADD COLUMN `title` TEXT",
                "ALTER TABLE `workout_sessions` ADD COLUMN `gym` TEXT",
                "ALTER TABLE `workout_sessions` ADD COLUMN `mediaUris` TEXT",
                "ALTER TABLE `workout_sessions` ADD COLUMN `draftJson` TEXT",
                "ALTER TABLE `set_logs` ADD COLUMN `setType` TEXT",
                "ALTER TABLE `set_logs` ADD COLUMN `distanceKm` REAL",
                "ALTER TABLE `set_logs` ADD COLUMN `durationSec` INTEGER",
                "ALTER TABLE `set_logs` ADD COLUMN `prBadges` TEXT",
                "ALTER TABLE `session_exercises` ADD COLUMN `restSeconds` INTEGER",
                "ALTER TABLE `session_exercises` ADD COLUMN `supersetGroup` INTEGER",
                "ALTER TABLE `exercises` ADD COLUMN `pinnedNote` TEXT",
                "ALTER TABLE `exercises` ADD COLUMN `exerciseType` TEXT",
                "ALTER TABLE `active_rest_timer` ADD COLUMN `totalMillis` INTEGER",
            ).forEach(db::execSQL)
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
