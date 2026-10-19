package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("routineId")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long?,
    val routineId: Long?,
    /** The workout's description ("How did your workout go?"). */
    val notes: String,
    /** Null until saved; an empty title is replaced by the auto title on save. */
    val title: String? = null,
    val gym: String? = null,
    val mediaUris: List<String>? = null,
    /**
     * While the workout is in progress (endedAt == null), the whole editable set table
     * as JSON ([com.example.personalfitnessapp.domain.workout.WorkoutDraft]). Cleared on save.
     */
    val draftJson: String? = null,
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val setIndex: Int,
    val weightKg: Float,
    val reps: Int,
    val rpe: Float?,
    val completedAt: Long,
    /** Null means a normal working set (rows logged before set types existed). */
    val setType: SetType? = null,
    val distanceKm: Float? = null,
    val durationSec: Int? = null,
    /** Records this set broke when it was logged, e.g. "WEIGHT,ONE_RM"; recomputed on every save. */
    val prBadges: String? = null,
) {
    val type: SetType get() = setType ?: SetType.NORMAL
}

/** Hevy's set types (img 24). Warm-ups count toward Sets but not volume or records. */
enum class SetType(val letter: String) {
    NORMAL(""),
    WARMUP("W"),
    FAILURE("F"),
    DROP("D"),
}
