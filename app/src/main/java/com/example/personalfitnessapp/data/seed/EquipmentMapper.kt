package com.example.personalfitnessapp.data.seed

object EquipmentMapper {
    private val DISPLAY_NAMES: Map<String, String> = mapOf(
        "body only" to "Bodyweight",
        "barbell" to "Barbell",
        "cable" to "Cable",
        "dumbbell" to "Dumbbell",
        "e-z curl bar" to "EZ Curl Bar",
        "exercise ball" to "Exercise Ball",
        "foam roll" to "Foam Roller",
        "kettlebells" to "Kettlebell",
        "machine" to "Machine",
        "medicine ball" to "Medicine Ball",
        "bands" to "Resistance Band",
        "other" to "Other",
    )

    fun map(rawEquipment: String?): String {
        if (rawEquipment == null) return "None"
        return DISPLAY_NAMES[rawEquipment] ?: "Other"
    }
}
