package com.example.personalfitnessapp.data.model

/**
 * The single fixed muscle-group taxonomy used everywhere in the app (heatmap,
 * recovery, swap, routine coverage, coach). Every dataset muscle name must be
 * mapped into one of these — see [com.example.personalfitnessapp.data.seed.MuscleMapper].
 */
enum class Muscle(val displayName: String) {
    CHEST("Chest"),
    LATS("Lats"),
    UPPER_BACK("Upper Back"),
    TRAPS("Traps"),
    LOWER_BACK("Lower Back"),
    FRONT_DELTS("Front Delts"),
    SIDE_DELTS("Side Delts"),
    REAR_DELTS("Rear Delts"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    ABS("Abs"),
    OBLIQUES("Obliques"),
    GLUTES("Glutes"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    CALVES("Calves"),
    HIP_ADDUCTORS("Hip Adductors"),
    HIP_ABDUCTORS("Hip Abductors"),
}
