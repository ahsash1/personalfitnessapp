package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED }

enum class FitnessGoal { STRENGTH, MUSCLE, GENERAL }

enum class UnitPreference { METRIC, IMPERIAL }

/** Single-row table: this app has exactly one user. id is always 1. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val heightCm: Float,
    val weightKg: Float,
    val age: Int,
    val sex: String?,
    val experienceLevel: ExperienceLevel,
    val goal: FitnessGoal,
    val daysPerWeek: Int,
    val injuriesOrLimitations: String,
    val unitPreference: UnitPreference,
)
