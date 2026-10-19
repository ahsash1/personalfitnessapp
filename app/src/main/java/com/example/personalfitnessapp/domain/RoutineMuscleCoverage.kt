package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.model.Muscle

/** Which muscles a routine targets, computed from its exercises. */
object RoutineMuscleCoverage {
    fun primaryMuscles(exercises: List<ExerciseEntity>): Set<Muscle> =
        exercises.flatMap { it.primaryMuscles }.toSet()

    fun allMuscles(exercises: List<ExerciseEntity>): Set<Muscle> =
        exercises.flatMap { it.primaryMuscles + it.secondaryMuscles }.toSet()
}
