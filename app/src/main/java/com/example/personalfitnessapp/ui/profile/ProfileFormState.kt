package com.example.personalfitnessapp.ui.profile

import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.local.entity.FitnessGoal
import com.example.personalfitnessapp.data.local.entity.UnitPreference
import com.example.personalfitnessapp.data.local.entity.UserProfileEntity
import com.example.personalfitnessapp.domain.UnitConverter

/**
 * Editable form state shared by onboarding and Settings. Numeric fields are kept
 * as raw strings while editing (so the user can type freely / clear a field) and
 * are only parsed and converted to canonical kg/cm when [toProfileOrNull] is called.
 */
data class ProfileFormState(
    val heightInput: String = "",
    val feetInput: String = "",
    val inchesInput: String = "",
    val weightInput: String = "",
    val ageInput: String = "",
    val sex: String? = null,
    val experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
    val goal: FitnessGoal = FitnessGoal.GENERAL,
    val daysPerWeek: Int = 3,
    val injuriesOrLimitations: String = "",
    val unitPreference: UnitPreference = UnitPreference.METRIC,
) {
    companion object {
        fun fromProfile(profile: UserProfileEntity): ProfileFormState {
            val (feet, inches) = UnitConverter.cmToFeetAndInches(profile.heightCm)
            return ProfileFormState(
                heightInput = profile.heightCm.toString(),
                feetInput = feet.toString(),
                inchesInput = inches.toString(),
                weightInput = when (profile.unitPreference) {
                    UnitPreference.METRIC -> profile.weightKg.toString()
                    UnitPreference.IMPERIAL -> "%.1f".format(UnitConverter.kgToLb(profile.weightKg))
                },
                ageInput = profile.age.toString(),
                sex = profile.sex,
                experienceLevel = profile.experienceLevel,
                goal = profile.goal,
                daysPerWeek = profile.daysPerWeek,
                injuriesOrLimitations = profile.injuriesOrLimitations,
                unitPreference = profile.unitPreference,
            )
        }
    }
}

sealed interface ProfileFormResult {
    data class Valid(val profile: UserProfileEntity) : ProfileFormResult
    data class Invalid(val errors: List<String>) : ProfileFormResult
}

object ProfileFormMapper {
    fun validate(form: ProfileFormState): ProfileFormResult {
        val errors = mutableListOf<String>()

        val heightCm = when (form.unitPreference) {
            UnitPreference.METRIC -> form.heightInput.toFloatOrNull()
            UnitPreference.IMPERIAL -> {
                val feet = form.feetInput.toIntOrNull()
                val inches = form.inchesInput.toIntOrNull()
                if (feet != null && inches != null) UnitConverter.feetAndInchesToCm(feet, inches) else null
            }
        }
        if (heightCm == null || heightCm <= 0f) errors += "Enter a valid height"

        val weightKg = when (form.unitPreference) {
            UnitPreference.METRIC -> form.weightInput.toFloatOrNull()
            UnitPreference.IMPERIAL -> form.weightInput.toFloatOrNull()?.let { UnitConverter.lbToKg(it) }
        }
        if (weightKg == null || weightKg <= 0f) errors += "Enter a valid weight"

        val age = form.ageInput.toIntOrNull()
        if (age == null || age <= 0 || age > 120) errors += "Enter a valid age"

        if (form.daysPerWeek !in 1..7) errors += "Days per week must be between 1 and 7"

        if (errors.isNotEmpty()) return ProfileFormResult.Invalid(errors)

        return ProfileFormResult.Valid(
            UserProfileEntity(
                heightCm = heightCm!!,
                weightKg = weightKg!!,
                age = age!!,
                sex = form.sex,
                experienceLevel = form.experienceLevel,
                goal = form.goal,
                daysPerWeek = form.daysPerWeek,
                injuriesOrLimitations = form.injuriesOrLimitations,
                unitPreference = form.unitPreference,
            )
        )
    }
}
