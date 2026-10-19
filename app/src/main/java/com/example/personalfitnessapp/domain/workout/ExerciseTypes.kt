package com.example.personalfitnessapp.domain.workout

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseType

/**
 * Which columns an exercise's set table uses. Custom exercises store their type; seed
 * exercises are classified from the dataset's category (kept in movementPattern) and
 * equipment: cardio -> KM + TIME, stretches and planks -> TIME, bodyweight -> REPS only.
 */
object ExerciseTypes {
    private val DISTANCE_NAMES = Regex("run|walk|jog|treadmill|bike|bicycl|cycling|row|elliptical|stair|step mill|skating|ski|swim|sled|prowler", RegexOption.IGNORE_CASE)

    fun of(exercise: ExerciseEntity): ExerciseType = exercise.exerciseType ?: when {
        exercise.movementPattern == "cardio" ->
            if (DISTANCE_NAMES.containsMatchIn(exercise.name)) ExerciseType.DISTANCE_DURATION else ExerciseType.DURATION
        exercise.movementPattern == "stretch" -> ExerciseType.DURATION
        exercise.movementPattern == "isometric" && exercise.equipment in BODYWEIGHT -> ExerciseType.DURATION
        exercise.equipment in BODYWEIGHT -> ExerciseType.REPS_ONLY
        else -> ExerciseType.WEIGHT_REPS
    }

    private val BODYWEIGHT = setOf("Bodyweight", "None")

    fun usesWeight(type: ExerciseType) = type == ExerciseType.WEIGHT_REPS || type == ExerciseType.ASSISTED_BODYWEIGHT
    fun usesReps(type: ExerciseType) =
        type == ExerciseType.WEIGHT_REPS || type == ExerciseType.REPS_ONLY || type == ExerciseType.ASSISTED_BODYWEIGHT
    fun usesDistance(type: ExerciseType) = type == ExerciseType.DISTANCE_DURATION
    fun usesDuration(type: ExerciseType) = type == ExerciseType.DURATION || type == ExerciseType.DISTANCE_DURATION

    /** Hevy defaults rest to OFF for cardio (img 26); timed holds get a short rest. */
    fun isCardio(type: ExerciseType) = type == ExerciseType.DISTANCE_DURATION
}
