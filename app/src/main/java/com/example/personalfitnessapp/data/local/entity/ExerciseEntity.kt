package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.personalfitnessapp.data.model.Muscle

enum class ExerciseSource { SEED, AI_GENERATED, USER }

enum class ExerciseLevel { BEGINNER, INTERMEDIATE, ADVANCED }

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aliases: List<String>,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle>,
    val equipment: String,
    val movementPattern: String,
    val mechanic: String?,
    val level: ExerciseLevel,
    val setupSteps: List<String>,
    val executionSteps: List<String>,
    val formCues: List<String>,
    val commonMistakes: List<String>,
    val defaultRestSeconds: Int?,
    val imageRefs: List<String>,
    val source: ExerciseSource,
    val verified: Boolean,
    val formGuideAiGenerated: Boolean,
    /** True once the user has manually edited a seed-sourced row; re-seeding on a
     * dataset version bump then skips it instead of overwriting their edit. */
    val userEdited: Boolean = false,
    /** "Pinned exercise notes" from the How to tab; shown on the exercise's card in a workout. */
    val pinnedNote: String? = null,
    /** Set for custom exercises; seed exercises derive theirs from category/equipment. */
    val exerciseType: ExerciseType? = null,
)

/** Which columns the set table shows (docs/screens/active_workout.md, cardio rows). */
enum class ExerciseType {
    WEIGHT_REPS,
    REPS_ONLY,
    DURATION,
    DISTANCE_DURATION,

    /** Assisted pull-ups/dips: the kg entered is help from the machine, so less is better and it isn't volume. */
    ASSISTED_BODYWEIGHT,
}
