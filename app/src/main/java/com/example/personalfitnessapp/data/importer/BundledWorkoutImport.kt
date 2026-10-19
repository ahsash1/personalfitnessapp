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
) {
    suspend fun runOnce() {
        if (appMetaDao.get(AppMetaKeys.BUNDLED_IMPORT_DONE) == ASSET) return
        val text = runCatching { context.assets.open(ASSET).use { it.readBytes().decodeToString() } }.getOrNull() ?: return
        importer.import(text)
        appMetaDao.set(AppMetaEntity(AppMetaKeys.BUNDLED_IMPORT_DONE, ASSET))
    }

    private companion object {
        const val ASSET = "seed/owner_hevy_export.csv"
    }
}
