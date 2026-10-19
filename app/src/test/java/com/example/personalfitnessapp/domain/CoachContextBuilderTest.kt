package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.local.entity.FitnessGoal
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.UnitPreference
import com.example.personalfitnessapp.data.local.entity.UserProfileEntity
import com.example.personalfitnessapp.data.model.Muscle
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachContextBuilderTest {
    private val profile = UserProfileEntity(
        heightCm = 180f,
        weightKg = 82.5f,
        age = 30,
        sex = "male",
        experienceLevel = ExperienceLevel.INTERMEDIATE,
        goal = FitnessGoal.MUSCLE,
        daysPerWeek = 4,
        injuriesOrLimitations = "sore left shoulder",
        unitPreference = UnitPreference.METRIC,
    )

    private fun input(recovery: List<MuscleRecovery> = emptyList(), training: List<String> = emptyList()) =
        CoachContextInput(profile, training, recovery, routineLines = emptyList(), scheduleLines = emptyList())

    @Test
    fun `includes the summarized profile and injuries but not sex`() {
        val text = CoachContextBuilder.build(input())

        assertTrue(text.contains("Age 30"))
        assertTrue(text.contains("82.5 kg"))
        assertTrue(text.contains("sore left shoulder"))
        assertFalse(text.contains("male"))
    }

    @Test
    fun `states the safety and no-invented-numbers rules`() {
        val text = CoachContextBuilder.build(input())

        assertTrue(text.contains("not a medical professional"))
        assertTrue(text.contains("Never invent numbers when there is no data"))
        assertTrue(text.contains("nothing is saved until they confirm"))
    }

    @Test
    fun `tells the coach to troubleshoot technique but still stop for pain`() {
        val text = CoachContextBuilder.build(input())

        assertTrue(text.contains("get_exercise_details"))
        assertTrue(text.contains("feeling it in the wrong muscle"))
        assertTrue(text.contains("stop that movement and get it checked"))
    }

    @Test
    fun `lists only muscles that are not ready`() {
        val recovery = listOf(
            MuscleRecovery(Muscle.QUADS, RecoveryStatus.FATIGUED, 3.0, 0L),
            MuscleRecovery(Muscle.BICEPS, RecoveryStatus.READY, 0.0, null),
        )

        val text = CoachContextBuilder.build(input(recovery))

        assertTrue(text.contains("Quads: fatigued"))
        assertFalse(text.contains("Biceps:"))
    }

    @Test
    fun `handles a missing profile`() {
        val text = CoachContextBuilder.build(CoachContextInput(null, emptyList(), emptyList(), emptyList(), emptyList()))

        assertTrue(text.contains("Not set up yet"))
    }

    @Test
    fun `training summary is capped and most-trained first`() {
        val names = (1..20).associate { "e$it" to "Exercise $it" }
        val logs = names.keys.flatMap { id ->
            // e20 is trained in three sessions, the rest in one.
            val sessions = if (id == "e20") 3 else 1
            (1..sessions).map { s ->
                SetLogEntity(sessionId = s.toLong() + id.hashCode(), exerciseId = id, setIndex = 0, weightKg = 60f, reps = 8, rpe = null, completedAt = 86_400_000L * s)
            }
        }

        val lines = CoachContextBuilder.trainingSummary(logs, names, ZoneOffset.UTC)

        assertEquals(12, lines.size)
        assertTrue(lines.first().contains("Exercise 20"))
        assertTrue(lines.first().contains("3 sessions"))
        assertTrue(lines.first().contains("best set 60 kg x 8"))
    }

    @Test
    fun `whole context stays compact with a realistic amount of data`() {
        val names = (1..12).associate { "e$it" to "Exercise number $it" }
        val logs = names.keys.map { SetLogEntity(0, 1, it, 0, 100f, 5, null, 1_000L) }
        val text = CoachContextBuilder.build(input(training = CoachContextBuilder.trainingSummary(logs, names, ZoneOffset.UTC)))

        // Rules + training knowledge are ~5.5k chars; the data part must stay small on top (free-tier token limits).
        assertTrue("context was ${text.length} chars", text.length < 8_000)
    }
}
