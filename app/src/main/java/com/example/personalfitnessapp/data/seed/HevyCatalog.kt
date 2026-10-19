package com.example.personalfitnessapp.data.seed

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.model.Muscle

/**
 * Gym-floor names in the "Name (Equipment)" style (assets/seed/hevy_catalog.txt) layered over
 * free-exercise-db: renames dataset exercises it maps (keeping the old name as an alias, so
 * search and history still find them) and adds machine/assisted variants the dataset lacks,
 * borrowing a similar exercise's photos and steps.
 */
object HevyCatalog {
    data class Entry(
        val name: String,
        val type: ExerciseType,
        val equipment: String?,
        val primary: List<Muscle>,
        val secondary: List<Muscle>,
        val renames: String?,
        val borrows: String?,
    )

    fun parse(text: String): List<Entry> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { line ->
            val p = line.split('|')
            require(p.size == 6) { "Bad catalog line: $line" }
            Entry(
                name = p[0],
                type = when (p[1]) {
                    "R" -> ExerciseType.REPS_ONLY
                    "D" -> ExerciseType.DURATION
                    "C" -> ExerciseType.DISTANCE_DURATION
                    "A" -> ExerciseType.ASSISTED_BODYWEIGHT
                    else -> ExerciseType.WEIGHT_REPS
                },
                equipment = p[2].ifBlank { null },
                primary = muscles(p[3]),
                secondary = muscles(p[4]),
                renames = p[5].takeIf { it.startsWith("=") }?.drop(1),
                borrows = p[5].takeIf { it.startsWith("~") }?.drop(1),
            )
        }.toList()

    private fun muscles(text: String) = text.split(',').filter { it.isNotBlank() }.map { Muscle.valueOf(it.trim()) }

    /** Same id scheme as the CSV importer, so an exercise it created earlier becomes the catalog one. */
    fun idFor(name: String) = "hevy_" + name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    /** Applies the catalog to the mapped dataset exercises; returns the full list to upsert. */
    fun apply(dataset: List<ExerciseEntity>, entries: List<Entry>): List<ExerciseEntity> {
        val byId = dataset.associateBy { it.id }.toMutableMap()
        val added = mutableListOf<ExerciseEntity>()
        for (e in entries) {
            val renamed = e.renames?.let(byId::get)
            if (renamed != null) {
                byId[renamed.id] = renamed.copy(
                    name = e.name,
                    aliases = (renamed.aliases + renamed.name).distinct().filter { it != e.name },
                    equipment = e.equipment ?: renamed.equipment,
                    primaryMuscles = e.primary.ifEmpty { renamed.primaryMuscles },
                    secondaryMuscles = if (e.primary.isEmpty()) renamed.secondaryMuscles else e.secondary,
                    exerciseType = e.type,
                )
                continue
            }
            val source = e.borrows?.let(byId::get)
            added += ExerciseEntity(
                id = idFor(e.name),
                name = e.name,
                aliases = emptyList(),
                primaryMuscles = e.primary,
                secondaryMuscles = e.secondary,
                equipment = e.equipment ?: source?.equipment ?: "Other",
                movementPattern = source?.movementPattern.orEmpty(),
                mechanic = source?.mechanic,
                level = source?.level ?: com.example.personalfitnessapp.data.local.entity.ExerciseLevel.INTERMEDIATE,
                setupSteps = source?.setupSteps.orEmpty(),
                executionSteps = source?.executionSteps.orEmpty(),
                formCues = emptyList(),
                commonMistakes = emptyList(),
                defaultRestSeconds = null,
                imageRefs = source?.imageRefs.orEmpty(),
                source = com.example.personalfitnessapp.data.local.entity.ExerciseSource.SEED,
                verified = source != null,
                formGuideAiGenerated = false,
                exerciseType = e.type,
            )
        }
        return byId.values.toList() + added
    }
}
