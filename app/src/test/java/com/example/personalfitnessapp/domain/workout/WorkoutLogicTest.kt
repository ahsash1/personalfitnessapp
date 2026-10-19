package com.example.personalfitnessapp.domain.workout

import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutLogicTest {
    private fun set(w: String, r: String, done: Boolean = true, type: SetType = SetType.NORMAL) =
        DraftSet(weight = w, reps = r, completed = done, type = type)

    @Test
    fun `warm-ups are lettered and not numbered`() {
        val sets = listOf(set("", "", type = SetType.WARMUP), set("", ""), set("", "", type = SetType.FAILURE), set("", ""))
        assertEquals(listOf("W", "1", "F", "3"), WorkoutMath.setLabels(sets))
    }

    @Test
    fun `volume counts completed non-warm-up weighted sets (img 13 = 436 kg)`() {
        val draft = WorkoutDraft(
            listOf(
                DraftExercise(exerciseId = "a", sets = listOf(set("30", "10"), set("68", "2"), set("90", "1", done = false))),
                DraftExercise(exerciseId = "b", sets = listOf(set("20", "10", type = SetType.WARMUP))),
            )
        )
        val types = mapOf("a" to ExerciseType.WEIGHT_REPS, "b" to ExerciseType.WEIGHT_REPS)
        assertEquals(436.0, WorkoutMath.volume(draft, types), 1e-9)
        assertEquals(3, WorkoutMath.completedSets(draft))
    }

    @Test
    fun `completing an empty set copies its placeholder, and nothing to copy fails`() {
        val previous = listOf(PreviousSet(40.0, 8, null, null, SetType.NORMAL))
        val sets = listOf(DraftSet())
        val done = WorkoutMath.complete(sets[0], WorkoutMath.placeholder(sets, 0, previous), ExerciseType.WEIGHT_REPS, 5L)!!
        assertEquals("40", done.weight)
        assertEquals("8", done.reps)
        assertTrue(done.completed)
        assertNull(WorkoutMath.complete(DraftSet(), null, ExerciseType.WEIGHT_REPS, 5L))
    }

    @Test
    fun `first time doing an exercise sets no records`() {
        val prs = WorkoutMath.prsFor("a", set("100", "5"), emptyList(), HistoryBests.NONE, ExerciseType.WEIGHT_REPS)
        assertTrue(prs.isEmpty())
    }

    @Test
    fun `a heavier set beats history`() {
        val history = Records.bests(listOf(LoggedSet(1, 1, 0, 0, 60.0, 5, SetType.NORMAL)))
        val kinds = WorkoutMath.prsFor("a", set("65", "5"), emptyList(), history, ExerciseType.WEIGHT_REPS).map { it.kind }
        assertTrue(PrKind.HEAVIEST_WEIGHT in kinds)
        assertTrue(PrKind.BEST_1RM in kinds)
    }

    @Test
    fun `saved badges skip the first workout and mark later bests`() {
        val sets = listOf(
            LoggedSet(1, 1, 100, 0, 60.0, 5, SetType.NORMAL),
            LoggedSet(2, 2, 200, 0, 62.5, 5, SetType.NORMAL),
            LoggedSet(3, 2, 200, 1, 50.0, 5, SetType.NORMAL),
        )
        val badges = Records.badges(sets)
        assertTrue(badges[1].isNullOrEmpty())
        assertTrue(PrKind.HEAVIEST_WEIGHT in badges.getValue(2))
        assertTrue(badges[3].isNullOrEmpty())
    }

    @Test
    fun `only completed sets are saved and come back completed`() {
        val draft = WorkoutDraft(
            listOf(DraftExercise(exerciseId = "a", note = "slow", sets = listOf(set("40", "10"), set("", "", done = false))))
        )
        val rows = DraftConversion.toRows(7, draft, endedAt = 99)
        assertEquals(1, rows.sets.size)
        val back = DraftConversion.fromRows(rows.exercises, rows.sets)
        assertEquals("slow", back.exercises.single().note)
        assertEquals("40", back.exercises.single().sets.single().weight)
        assertTrue(back.exercises.single().sets.single().completed)
    }

    @Test
    fun `removing one of two superset members dissolves the superset`() {
        var draft = WorkoutDraft(listOf(DraftExercise(key = "x", exerciseId = "a"), DraftExercise(key = "y", exerciseId = "b")))
        draft = DraftOps.addToSuperset(draft, "x", "y")
        assertEquals(2, draft.exercises.count { it.supersetGroup != null })
        draft = DraftOps.removeExercise(draft, "x")
        assertNull(draft.exercises.single().supersetGroup)
    }
}

class PlateMathTest {
    @Test
    fun `plates per side for 100 kg on a 20 kg bar`() {
        val loading = PlateMath.plates(100.0, 20.0)
        assertEquals(listOf(25.0, 15.0), loading.perSide)
        assertEquals(0.0, loading.remainderKg, 1e-9)
    }

    @Test
    fun `weights that can't be made report a remainder`() {
        assertEquals(0.5, PlateMath.plates(23.0, 20.0).remainderKg, 1e-9)
        assertTrue(PlateMath.plates(15.0, 20.0).perSide.isEmpty())
    }

    @Test
    fun `warm-ups ramp to the working weight on the load step`() {
        val sets = PlateMath.warmups(100.0, 2.5, minKg = 20.0)
        assertEquals(listOf("50", "70", "85"), sets.map { it.weight })
        assertTrue(sets.all { it.type == SetType.WARMUP })
        assertEquals(listOf("20", "25"), PlateMath.warmups(30.0, 2.5, minKg = 20.0).map { it.weight })
    }
}

class OneRepMaxCapTest {
    @Test
    fun `sets over 10 reps don't estimate a 1RM or earn 1RM records`() {
        assertNull(WorkoutMath.oneRepMaxOrNull(27.5, 30))
        assertEquals(26.67, WorkoutMath.oneRepMaxOrNull(20.0, 10)!!, 0.01)
        val sets = listOf(
            LoggedSet(1, 1, 100, 0, 27.5, 12, SetType.NORMAL),
            LoggedSet(2, 2, 200, 0, 27.5, 30, SetType.NORMAL),
        )
        assertNull(Records.bests(sets).best1RmKg)
        assertTrue(Records.badges(sets).values.none { PrKind.BEST_1RM in it })
        val history = Records.bests(listOf(LoggedSet(1, 1, 100, 0, 20.0, 8, SetType.NORMAL)))
        val prs = WorkoutMath.prsFor("x", DraftSet(weight = "15", reps = "40", completed = true), emptyList(), history, ExerciseType.WEIGHT_REPS)
        assertTrue(prs.none { it.kind == PrKind.BEST_1RM })
    }
}

