package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryModelTest {
    private val now = 1_700_000_000_000L // arbitrary fixed epoch millis

    private fun exercise(primary: List<Muscle>, secondary: List<Muscle> = emptyList()) = ExerciseEntity(
        id = "x",
        name = "X",
        aliases = emptyList(),
        primaryMuscles = primary,
        secondaryMuscles = secondary,
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

    private fun setLog(weight: Float, reps: Int, hoursAgo: Double, session: Long = 1) = SetLogEntity(
        sessionId = session,
        exerciseId = "x",
        setIndex = 0,
        weightKg = weight,
        reps = reps,
        rpe = null,
        completedAt = (now - hoursAgo * 3_600_000).toLong(),
    )

    private fun statusOf(sets: List<RecoverySetInput>, muscle: Muscle, level: ExperienceLevel = ExperienceLevel.INTERMEDIATE) =
        RecoveryModel.compute(sets, level, now).first { it.muscle == muscle }

    @Test
    fun `a muscle never trained is ready with no last-trained date`() {
        val result = statusOf(emptyList(), Muscle.CHEST)
        assertEquals(RecoveryStatus.READY, result.status)
        assertEquals(0.0, result.fatigueScore, 0.001)
        assertNull(result.lastTrainedAt)
    }

    @Test
    fun `a fresh heavy compound set fatigues its primary muscle`() {
        val ex = exercise(primary = listOf(Muscle.CHEST))
        val sets = listOf(RecoverySetInput(setLog(100f, 8, hoursAgo = 0.0), ex))
        val result = statusOf(sets, Muscle.CHEST)
        assertEquals(RecoveryStatus.FATIGUED, result.status)
        assertEquals(now, result.lastTrainedAt)
    }

    @Test
    fun `secondary muscles get half the stimulus of primary`() {
        val ex = exercise(primary = listOf(Muscle.CHEST), secondary = listOf(Muscle.TRICEPS))
        val sets = listOf(RecoverySetInput(setLog(100f, 8, hoursAgo = 0.0), ex))
        val chest = statusOf(sets, Muscle.CHEST)
        val triceps = statusOf(sets, Muscle.TRICEPS)
        assertEquals(chest.fatigueScore / 2, triceps.fatigueScore, 0.001)
    }

    @Test
    fun `stimulus decays toward zero as time passes`() {
        val ex = exercise(primary = listOf(Muscle.BICEPS))
        val fresh = statusOf(listOf(RecoverySetInput(setLog(20f, 10, hoursAgo = 0.0), ex)), Muscle.BICEPS)
        val old = statusOf(listOf(RecoverySetInput(setLog(20f, 10, hoursAgo = 200.0), ex)), Muscle.BICEPS)
        assertTrue(old.fatigueScore < fresh.fatigueScore)
        assertEquals(RecoveryStatus.READY, old.status)
    }

    @Test
    fun `a small muscle is roughly recovered around its 48h baseline`() {
        val ex = exercise(primary = listOf(Muscle.BICEPS))
        val sets = listOf(RecoverySetInput(setLog(20f, 8, hoursAgo = 48.0), ex))
        val result = statusOf(sets, Muscle.BICEPS)
        assertTrue(result.status != RecoveryStatus.FATIGUED)
    }

    @Test
    fun `legs take longer than arms after the same session`() {
        val quadsEx = exercise(primary = listOf(Muscle.QUADS))
        val bicepsEx = exercise(primary = listOf(Muscle.BICEPS))
        val quads = statusOf(List(6) { RecoverySetInput(setLog(60f, 8, hoursAgo = 48.0), quadsEx) }, Muscle.QUADS)
        val biceps = statusOf(List(6) { RecoverySetInput(setLog(20f, 8, hoursAgo = 48.0), bicepsEx) }, Muscle.BICEPS)
        assertEquals(RecoveryStatus.READY, biceps.status)
        assertEquals(24.0, quads.hoursUntilReady, 0.01)
    }

    @Test
    fun `advanced lifters recover modestly faster than beginners`() {
        val ex = exercise(primary = listOf(Muscle.QUADS))
        val sets = List(6) { RecoverySetInput(setLog(100f, 8, hoursAgo = 40.0), ex) }
        val beginner = statusOf(sets, Muscle.QUADS, ExperienceLevel.BEGINNER)
        val advanced = statusOf(sets, Muscle.QUADS, ExperienceLevel.ADVANCED)
        assertTrue(advanced.fatigueScore < beginner.fatigueScore)
    }

    @Test
    fun `an exercise that does not touch a muscle contributes nothing`() {
        val ex = exercise(primary = listOf(Muscle.CHEST))
        val sets = listOf(RecoverySetInput(setLog(100f, 8, hoursAgo = 0.0), ex))
        val result = statusOf(sets, Muscle.HAMSTRINGS)
        assertEquals(0.0, result.fatigueScore, 0.001)
    }

    @Test
    fun `bodyweight sets still register nonzero stimulus`() {
        val ex = exercise(primary = listOf(Muscle.LATS))
        val sets = listOf(RecoverySetInput(setLog(0f, 10, hoursAgo = 0.0), ex))
        val result = statusOf(sets, Muscle.LATS)
        assertTrue(result.fatigueScore > 0.0)
    }

    @Test
    fun `multiple sets accumulate stimulus`() {
        val ex = exercise(primary = listOf(Muscle.CALVES))
        val oneSet = statusOf(listOf(RecoverySetInput(setLog(40f, 12, hoursAgo = 1.0), ex)), Muscle.CALVES)
        val threeSets = statusOf(
            List(3) { RecoverySetInput(setLog(40f, 12, hoursAgo = 1.0), ex) },
            Muscle.CALVES,
        )
        assertTrue(threeSets.fatigueScore > oneSet.fatigueScore)
        assertTrue(threeSets.hoursUntilReady > oneSet.hoursUntilReady)
    }

    @Test
    fun `volume summary counts only primary-muscle sets, not secondary`() {
        val primaryEx = exercise(primary = listOf(Muscle.CHEST))
        val secondaryEx = exercise(primary = listOf(Muscle.TRICEPS), secondary = listOf(Muscle.CHEST))
        val sets = listOf(
            RecoverySetInput(setLog(100f, 8, hoursAgo = 1.0), primaryEx),
            RecoverySetInput(setLog(100f, 6, hoursAgo = 2.0), primaryEx),
            RecoverySetInput(setLog(50f, 10, hoursAgo = 1.0), secondaryEx),
        )
        val summary = RecoveryModel.volumeSummary(Muscle.CHEST, sets)
        assertEquals(2, summary.setCount)
        assertEquals(100f * 8 + 100f * 6, summary.totalVolumeKg, 0.01f)
    }

    @Test
    fun `volume summary is zero for an untrained muscle`() {
        val summary = RecoveryModel.volumeSummary(Muscle.HAMSTRINGS, emptyList())
        assertEquals(0, summary.setCount)
        assertEquals(0f, summary.totalVolumeKg, 0.01f)
    }

    // Evidence-based expectations (sources in RecoveryConstants).

    @Test
    fun `nine hard chest sets are ready about 55 hours later, not days on end`() {
        // Example: incline DB press, decline machine press, machine fly, 3 sets each.
        val ex = exercise(primary = listOf(Muscle.CHEST))
        val fresh = statusOf(List(9) { RecoverySetInput(setLog(20f, 15, hoursAgo = 0.0), ex) }, Muscle.CHEST)
        assertEquals(48.0 * 1.15, fresh.hoursUntilReady, 0.01)
        val fourDaysLater = statusOf(List(9) { RecoverySetInput(setLog(20f, 15, hoursAgo = 96.0), ex) }, Muscle.CHEST)
        assertEquals(RecoveryStatus.READY, fourDaysLater.status)
    }

    @Test
    fun `reps and load don't change recovery, sets do`() {
        val ex = exercise(primary = listOf(Muscle.CHEST))
        val light = statusOf(List(4) { RecoverySetInput(setLog(10f, 20, hoursAgo = 1.0), ex) }, Muscle.CHEST)
        val heavy = statusOf(List(4) { RecoverySetInput(setLog(120f, 5, hoursAgo = 1.0), ex) }, Muscle.CHEST)
        assertEquals(light.hoursUntilReady, heavy.hoursUntilReady, 0.001)
    }

    @Test
    fun `volume nudges recovery but is capped`() {
        val ex = exercise(primary = listOf(Muscle.QUADS))
        fun hours(n: Int) = statusOf(List(n) { RecoverySetInput(setLog(100f, 8, hoursAgo = 0.0), ex) }, Muscle.QUADS).hoursUntilReady
        assertEquals(72.0, hours(6), 0.01)
        assertEquals(72.0 * 0.85, hours(3), 0.01)
        assertEquals(72.0 * 1.25, hours(20), 0.01)
    }

    @Test
    fun `warm-ups and cardio add no recovery debt`() {
        val press = exercise(primary = listOf(Muscle.CHEST))
        val warmup = setLog(40f, 10, hoursAgo = 1.0).copy(setType = com.example.personalfitnessapp.data.local.entity.SetType.WARMUP)
        assertEquals(RecoveryStatus.READY, statusOf(listOf(RecoverySetInput(warmup, press)), Muscle.CHEST).status)
        val treadmill = exercise(primary = listOf(Muscle.QUADS)).copy(exerciseType = com.example.personalfitnessapp.data.local.entity.ExerciseType.DISTANCE_DURATION)
        assertEquals(RecoveryStatus.READY, statusOf(listOf(RecoverySetInput(setLog(0f, 0, hoursAgo = 1.0), treadmill)), Muscle.QUADS).status)
    }

    @Test
    fun `the session whose window ends last decides`() {
        val ex = exercise(primary = listOf(Muscle.CHEST))
        val sets = List(9) { RecoverySetInput(setLog(20f, 12, hoursAgo = 70.0, session = 1), ex) } +
            List(3) { RecoverySetInput(setLog(20f, 12, hoursAgo = 10.0, session = 2), ex) }
        // Session 2 (3 sets, 40.8 h window) ends later than session 1 (55.2 h window, 70 h ago).
        assertEquals(48.0 * 0.85 - 10.0, statusOf(sets, Muscle.CHEST).hoursUntilReady, 0.01)
    }
}
