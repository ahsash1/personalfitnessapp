package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineProposalValidatorTest {
    private fun exercise(id: String, name: String) = ExerciseEntity(
        id = id,
        name = name,
        aliases = emptyList(),
        primaryMuscles = listOf(Muscle.CHEST),
        secondaryMuscles = emptyList(),
        equipment = "Barbell",
        movementPattern = "push",
        mechanic = "compound",
        level = ExerciseLevel.INTERMEDIATE,
        setupSteps = emptyList(),
        executionSteps = emptyList(),
        formCues = emptyList(),
        commonMistakes = emptyList(),
        defaultRestSeconds = null,
        imageRefs = emptyList(),
        source = ExerciseSource.SEED,
        verified = true,
        formGuideAiGenerated = false,
    )

    private val known = mapOf("bench" to exercise("bench", "Bench Press"), "fly" to exercise("fly", "Cable Fly"))

    private fun validate(vararg raw: RawProposedExercise, name: String = "Push A") =
        RoutineProposalValidator.validate(name, "", raw.toList(), known)

    @Test
    fun `accepts known exercises and fills names from the database`() {
        val result = validate(RawProposedExercise("bench", 4, 5, 8)) as ProposalValidation.Valid

        assertEquals(ProposedExercise("bench", "Bench Press", 4, 5, 8), result.proposal.exercises.single())
    }

    @Test
    fun `rejects unknown ids and tells the model which ones`() {
        val result = validate(RawProposedExercise("bench", 3, 8, 12), RawProposedExercise("made-up", 3, 8, 12))

        assertTrue(result is ProposalValidation.Invalid)
        assertTrue((result as ProposalValidation.Invalid).reason.contains("made-up"))
    }

    @Test
    fun `applies defaults and clamps out-of-range numbers`() {
        val result = validate(RawProposedExercise("bench", null, null, null), RawProposedExercise("fly", 99, 12, 5))
            as ProposalValidation.Valid

        val (defaulted, clamped) = result.proposal.exercises
        assertEquals(Triple(3, 8, 12), Triple(defaulted.sets, defaulted.repMin, defaulted.repMax))
        assertEquals(8, clamped.sets)
        assertTrue(clamped.repMax >= clamped.repMin)
    }

    @Test
    fun `drops duplicate exercises`() {
        val result = validate(RawProposedExercise("bench", 3, 8, 12), RawProposedExercise("bench", 5, 5, 5))
            as ProposalValidation.Valid

        assertEquals(1, result.proposal.exercises.size)
        assertEquals(3, result.proposal.exercises.single().sets)
    }

    @Test
    fun `rejects blank names, empty routines and oversized routines`() {
        assertTrue(validate(RawProposedExercise("bench", 3, 8, 12), name = " ") is ProposalValidation.Invalid)
        assertTrue(validate() is ProposalValidation.Invalid)
        val tooMany = List(RoutineProposalValidator.MAX_EXERCISES + 1) { RawProposedExercise("bench", 3, 8, 12) }
        assertTrue(RoutineProposalValidator.validate("x", "", tooMany, known) is ProposalValidation.Invalid)
    }
}
