package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MealType(val label: String) { BREAKFAST("Breakfast"), LUNCH("Lunch"), DINNER("Dinner"), SNACK("Snacks") }

enum class FoodSource { AI_TEXT, AI_PHOTO, BARCODE, MANUAL, RECENT }

/** One logged food. Macros are for the amount eaten, not per 100 g. */
@Entity(tableName = "food_entries", indices = [Index("date")])
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Local date, yyyy-MM-dd. */
    val date: String,
    val meal: MealType,
    val name: String,
    val grams: Float?,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val source: FoodSource,
    val barcode: String? = null,
    val createdAt: Long,
)
