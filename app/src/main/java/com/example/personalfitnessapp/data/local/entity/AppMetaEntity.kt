package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tiny key-value table for internal bookkeeping, e.g. the applied seed dataset version. */
@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

object AppMetaKeys {
    const val EXERCISE_SEED_VERSION = "exercise_seed_version"
    const val BUNDLED_IMPORT_DONE = "bundled_import_done"
    const val RECORDS_RULES_VERSION = "records_rules_version"
    const val BUNDLED_CIRCUITS_DONE = "bundled_circuits_done"
}
