package com.example.personalfitnessapp.ui.profile

import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.local.entity.FitnessGoal
import com.example.personalfitnessapp.data.local.entity.UnitPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFormMapperTest {
    @Test
    fun `valid metric form maps directly to kg-cm`() {
        val form = ProfileFormState(
            heightInput = "180",
            weightInput = "80",
            ageInput = "30",
            daysPerWeek = 4,
            unitPreference = UnitPreference.METRIC,
        )

        val result = ProfileFormMapper.validate(form) as ProfileFormResult.Valid
        assertEquals(180f, result.profile.heightCm, 0.01f)
        assertEquals(80f, result.profile.weightKg, 0.01f)
        assertEquals(30, result.profile.age)
    }

    @Test
    fun `imperial form converts feet-inches and lb to metric`() {
        val form = ProfileFormState(
            feetInput = "5",
            inchesInput = "11",
            weightInput = "176",
            ageInput = "30",
            daysPerWeek = 4,
            unitPreference = UnitPreference.IMPERIAL,
        )

        val result = ProfileFormMapper.validate(form) as ProfileFormResult.Valid
        assertEquals(180.34f, result.profile.heightCm, 0.1f)
        assertEquals(79.83f, result.profile.weightKg, 0.1f)
    }

    @Test
    fun `blank required fields are invalid`() {
        val result = ProfileFormMapper.validate(ProfileFormState())
        assertTrue(result is ProfileFormResult.Invalid)
        assertTrue((result as ProfileFormResult.Invalid).errors.isNotEmpty())
    }

    @Test
    fun `days per week out of range is invalid`() {
        val form = ProfileFormState(
            heightInput = "180",
            weightInput = "80",
            ageInput = "30",
            daysPerWeek = 8,
        )
        val result = ProfileFormMapper.validate(form)
        assertTrue(result is ProfileFormResult.Invalid)
    }

    @Test
    fun `experience and goal pass through unchanged`() {
        val form = ProfileFormState(
            heightInput = "180",
            weightInput = "80",
            ageInput = "30",
            daysPerWeek = 4,
            experienceLevel = ExperienceLevel.ADVANCED,
            goal = FitnessGoal.STRENGTH,
        )
        val result = ProfileFormMapper.validate(form) as ProfileFormResult.Valid
        assertEquals(ExperienceLevel.ADVANCED, result.profile.experienceLevel)
        assertEquals(FitnessGoal.STRENGTH, result.profile.goal)
    }
}
