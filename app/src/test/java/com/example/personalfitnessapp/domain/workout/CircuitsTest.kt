package com.example.personalfitnessapp.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class CircuitsTest {
    private val owners = """
        Workout // 50s work, 10s rest
        00:20 Leg Switches
        01:20 Reverse Crunch + Leg Opener
        02:20 Leg Lowers
        03:20 Scissor Crossovers
        04:20 Cross Crunches
        05:20 Butterfly Crunches
        06:20 Single Leg Extensions
        07:20 Heel Taps
        08:20 Plank Knee Tucks
        09:20 Spider Crunches
    """.trimIndent()

    @Test
    fun `parses my ab circuit`() {
        val plan = CircuitText.parse(owners)!!
        assertEquals(50, plan.workSec)
        assertEquals(10, plan.restSec)
        assertEquals(1, plan.rounds)
        assertEquals(10, plan.moves.size)
        assertEquals("Reverse Crunch + Leg Opener", plan.moves[1])
        assertEquals("Circuit", plan.name)
    }

    @Test
    fun `work comes from timestamp gaps when the header doesn't say`() {
        val plan = CircuitText.parse("0:00 Plank\n0:45 Crunch\n1:30 V Up")!!
        assertEquals(35, plan.workSec)
        assertEquals(10, plan.restSec)
    }

    @Test
    fun `timeline has prep, work and rest but no trailing rest`() {
        val phases = CircuitTimeline.phases(50, 10, 1, 10)
        assertEquals(PhaseKind.PREP, phases.first().kind)
        assertEquals(10, phases.count { it.kind == PhaseKind.WORK })
        assertEquals(9, phases.count { it.kind == PhaseKind.REST })
        assertEquals(PhaseKind.WORK, phases.last().kind)
        assertEquals(10 + 10 * 50 + 9 * 10, phases.sumOf { it.seconds })
    }
}
