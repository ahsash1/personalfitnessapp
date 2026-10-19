package com.example.personalfitnessapp.data.importer

import android.content.Context
import com.example.personalfitnessapp.data.local.dao.AppMetaDao
import com.example.personalfitnessapp.data.local.entity.AppMetaEntity
import com.example.personalfitnessapp.data.local.entity.AppMetaKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * My own Hevy export, shipped in assets and imported once on first launch so my
 * history is there without picking a file. Re-running is harmless: the importer skips
 * workouts whose start time is already saved.
 */
class BundledWorkoutImport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importer: HevyImporter,
    private val appMetaDao: AppMetaDao,
    private val log: com.example.personalfitnessapp.data.repository.WorkoutLogRepository,
) {
    /** Re-derives saved PR badges when the record rules change (v2: 1RM only from <= 10-rep sets). */
    suspend fun refreshRecordsOnce() {
        if (appMetaDao.get(AppMetaKeys.RECORDS_RULES_VERSION) == RECORDS_RULES) return
        log.recomputeRecords(log.recentExerciseIds(Int.MAX_VALUE))
        appMetaDao.set(AppMetaEntity(AppMetaKeys.RECORDS_RULES_VERSION, RECORDS_RULES))
    }

    suspend fun runOnce() {
        if (appMetaDao.get(AppMetaKeys.BUNDLED_IMPORT_DONE) == ASSET) return
        val text = runCatching { context.assets.open(ASSET).use { it.readBytes().decodeToString() } }.getOrNull() ?: return
        importer.import(text)
        appMetaDao.set(AppMetaEntity(AppMetaKeys.BUNDLED_IMPORT_DONE, ASSET))
    }

    private companion object {
        const val ASSET = "seed/owner_hevy_export.csv"
        const val RECORDS_RULES = "2"
    }
}
