package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-editable smallest loadable jump per equipment type, used by the
 * progressive-overload engine to round suggested weights.
 * Seeded with sane defaults on first launch; editable in Settings.
 */
@Entity(tableName = "equipment_increments")
data class EquipmentIncrementEntity(
    @PrimaryKey val equipmentType: String,
    val incrementKg: Float,
)

object DefaultEquipmentIncrements {
    const val BARBELL = "Barbell"
    const val BARBELL_LOWER_COMPOUND = "Barbell (lower-body compound)"
    const val DUMBBELL = "Dumbbell"
    const val MACHINE_CABLE = "Machine/Cable"

    val defaults = listOf(
        EquipmentIncrementEntity(BARBELL, 2.5f),
        EquipmentIncrementEntity(BARBELL_LOWER_COMPOUND, 5.0f),
        EquipmentIncrementEntity(DUMBBELL, 2.0f),
        EquipmentIncrementEntity(MACHINE_CABLE, 2.5f),
    )
}
