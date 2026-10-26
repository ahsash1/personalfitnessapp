package com.example.personalfitnessapp.data.importer

import android.content.Context
import com.example.personalfitnessapp.data.local.dao.AppMetaDao
import com.example.personalfitnessapp.data.local.entity.AppMetaEntity
import com.example.personalfitnessapp.data.local.entity.AppMetaKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** One-time setup run on launch: bundled timer circuits and PR re-derivation when the rules change. */
class BundledWorkoutImport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appMetaDao: AppMetaDao,
    private val log: com.example.personalfitnessapp.data.repository.WorkoutLogRepository,
    private val circuits: com.example.personalfitnessapp.data.repository.CircuitRepository,
) {
    /** My own timer circuits (assets/seed/owner_circuits.txt), created once as routines. */
    suspend fun circuitsOnce() {
        if (appMetaDao.get(AppMetaKeys.BUNDLED_CIRCUITS_DONE) == "1") return
        val text = runCatching { context.assets.open("seed/owner_circuits.txt").use { it.readBytes().decodeToString() } }.getOrNull() ?: return
        text.split(Regex("\\n\\s*\\n")).mapNotNull { com.example.personalfitnessapp.domain.workout.CircuitText.parse(it) }.forEach { plan ->
            if (!circuits.exists(plan.name)) circuits.create(plan)
        }
        appMetaDao.set(AppMetaEntity(AppMetaKeys.BUNDLED_CIRCUITS_DONE, "1"))
    }

    /** Re-derives saved PR badges when the record rules change (v2: 1RM only from <= 10-rep sets). */
    suspend fun refreshRecordsOnce() {
        if (appMetaDao.get(AppMetaKeys.RECORDS_RULES_VERSION) == RECORDS_RULES) return
        log.recomputeRecords(log.recentExerciseIds(Int.MAX_VALUE))
        appMetaDao.set(AppMetaEntity(AppMetaKeys.RECORDS_RULES_VERSION, RECORDS_RULES))
    }


    private companion object {
        const val RECORDS_RULES = "2"
    }
}
