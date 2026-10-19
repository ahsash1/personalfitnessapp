package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weightKg: Float?,
    val waistCm: Float?,
    val chestCm: Float?,
    val hipsCm: Float?,
    val leftArmCm: Float?,
    val rightArmCm: Float?,
    val leftThighCm: Float?,
    val rightThighCm: Float?,
    val neckCm: Float?,
)
