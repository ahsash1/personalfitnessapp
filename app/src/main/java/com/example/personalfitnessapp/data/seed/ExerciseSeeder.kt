package com.example.personalfitnessapp.data.seed

import android.content.Context
import com.example.personalfitnessapp.data.local.dao.AppMetaDao
import com.example.personalfitnessapp.data.local.dao.EquipmentIncrementDao
import com.example.personalfitnessapp.data.local.dao.ExerciseDao
import com.example.personalfitnessapp.data.local.entity.AppMetaEntity
import com.example.personalfitnessapp.data.local.entity.AppMetaKeys
import com.example.personalfitnessapp.data.local.entity.DefaultEquipmentIncrements
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.json.Json

/**
 * Loads the bundled exercise dataset into Room on first launch, and re-applies
 * it when [SEED_VERSION] is bumped (e.g. a dataset correction) without
 * clobbering exercises the user has since edited. Bump [SEED_VERSION] whenever
 * assets/seed/free_exercise_db_v1.json or hevy_catalog.txt changes.
 */
class ExerciseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val equipmentIncrementDao: EquipmentIncrementDao,
    private val appMetaDao: AppMetaDao,
) {
    companion object {
        const val SEED_VERSION = "2"
        private const val SEED_ASSET_PATH = "seed/free_exercise_db_v1.json"
        private const val CATALOG_ASSET_PATH = "seed/hevy_catalog.txt"
        private val json = Json { ignoreUnknownKeys = true }
    }

    suspend fun seedIfNeeded() {
        equipmentIncrementDao.insertAllIfAbsent(DefaultEquipmentIncrements.defaults)

        val appliedVersion = appMetaDao.get(AppMetaKeys.EXERCISE_SEED_VERSION)
        if (appliedVersion == SEED_VERSION) return

        val jsonText = context.assets.open(SEED_ASSET_PATH).use { it.readBytes().decodeToString() }
        val rawExercises = json.decodeFromString<List<RawExerciseDto>>(jsonText)

        val catalog = HevyCatalog.parse(context.assets.open(CATALOG_ASSET_PATH).use { it.readBytes().decodeToString() })
        val editedIds = exerciseDao.userEditedSeedIds().toSet()
        val entities = HevyCatalog.apply(rawExercises.map { ExerciseSeedMapper.toEntity(it) }, catalog)
            .filter { it.id !in editedIds }
            // Keep a rest time the user picked for an exercise (stored on the row) across re-seeds.
            .map { e -> exerciseDao.getById(e.id)?.let { old -> e.copy(defaultRestSeconds = old.defaultRestSeconds, pinnedNote = old.pinnedNote) } ?: e }

        exerciseDao.upsertAll(entities)
        appMetaDao.set(AppMetaEntity(AppMetaKeys.EXERCISE_SEED_VERSION, SEED_VERSION))
    }
}
