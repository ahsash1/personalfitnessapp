package com.example.personalfitnessapp.domain.workout

import com.example.personalfitnessapp.data.local.entity.SetType
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * The editable state of a workout: what the Active Workout and Edit Workout screens show.
 * Values are kept as the strings the user typed so a half-typed "22." or a cleared field
 * survives process death exactly as it was; they are parsed only when needed.
 */
@Serializable
data class WorkoutDraft(val exercises: List<DraftExercise> = emptyList())

@Serializable
data class DraftExercise(
    val key: String = newKey(),
    val exerciseId: String,
    /** Seconds; null or 0 = rest timer off for this exercise. */
    val restSeconds: Int? = null,
    val note: String? = null,
    /** The note line is visible (tapped "Note" but nothing typed yet). */
    val noteOpen: Boolean = false,
    val supersetGroup: Int? = null,
    val sets: List<DraftSet> = listOf(DraftSet()),
)

@Serializable
data class DraftSet(
    val key: String = newKey(),
    val type: SetType = SetType.NORMAL,
    val weight: String = "",
    val reps: String = "",
    val distance: String = "",
    /** Cardio/timed sets store seconds directly (entered through a duration field). */
    val durationSec: Int? = null,
    /** Treadmill incline % / machine resistance level, as typed. */
    val incline: String = "",
    val resistance: String = "",
    val rpe: Double? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
)

fun newKey(): String = UUID.randomUUID().toString()

/** What a previous workout logged for one set index; used for the PREVIOUS column and placeholders. */
data class PreviousSet(
    val weightKg: Double?,
    val reps: Int?,
    val distanceKm: Double?,
    val durationSec: Int?,
    val type: SetType,
    val inclinePct: Double? = null,
    val resistanceLevel: Int? = null,
)
