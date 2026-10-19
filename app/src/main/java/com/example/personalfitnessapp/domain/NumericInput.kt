package com.example.personalfitnessapp.domain

sealed interface NumericValidation<out T> {
    data class Valid<T>(val value: T) : NumericValidation<T>
    data class Invalid(val reason: String) : NumericValidation<Nothing>
}

/**
 * Centralized numeric-input rules for every weight/rep/measurement entry point in the app.
 * Rejects clearly-invalid input with a reason the UI can show inline, rather than silently
 * accepting garbage or silently transforming what the user typed (blank text is "invalid",
 * never coerced to a default).
 */
object NumericInput {
    const val MAX_SET_WEIGHT_KG = 500f
    const val MAX_REPS = 100
    const val MAX_PLANNED_SETS = 20
    const val MIN_BODY_WEIGHT_KG = 20f
    const val MAX_BODY_WEIGHT_KG = 400f
    const val MIN_MEASUREMENT_CM = 10f
    const val MAX_MEASUREMENT_CM = 300f

    /** A lifted set's weight: 0 is valid (bodyweight exercises with no added load). */
    fun setWeightKg(text: String): NumericValidation<Float> =
        float(text, min = 0f, max = MAX_SET_WEIGHT_KG, zeroMessage = null)

    /** A completed set always has at least one rep. */
    fun reps(text: String): NumericValidation<Int> = int(text, min = 1, max = MAX_REPS)

    /** A routine exercise's planned number of sets. */
    fun setsCount(text: String): NumericValidation<Int> = int(text, min = 1, max = MAX_PLANNED_SETS)

    fun bodyWeightKg(text: String): NumericValidation<Float> =
        float(text, min = MIN_BODY_WEIGHT_KG, max = MAX_BODY_WEIGHT_KG, zeroMessage = "Enter a weight")

    fun measurementCm(text: String): NumericValidation<Float> =
        float(text, min = MIN_MEASUREMENT_CM, max = MAX_MEASUREMENT_CM, zeroMessage = "Enter a measurement")

    private fun float(text: String, min: Float, max: Float, zeroMessage: String?): NumericValidation<Float> {
        if (text.isBlank()) return NumericValidation.Invalid("Required")
        val value = text.toFloatOrNull() ?: return NumericValidation.Invalid("Not a number")
        if (value.isNaN() || value.isInfinite()) return NumericValidation.Invalid("Not a number")
        if (value == 0f && zeroMessage != null) return NumericValidation.Invalid(zeroMessage)
        if (value < min) return NumericValidation.Invalid("Too low")
        if (value > max) return NumericValidation.Invalid("Too high")
        return NumericValidation.Valid(value)
    }

    private fun int(text: String, min: Int, max: Int): NumericValidation<Int> {
        if (text.isBlank()) return NumericValidation.Invalid("Required")
        val value = text.toIntOrNull() ?: return NumericValidation.Invalid("Not a number")
        if (value < min) return NumericValidation.Invalid("Must be at least $min")
        if (value > max) return NumericValidation.Invalid("Too high")
        return NumericValidation.Valid(value)
    }
}
