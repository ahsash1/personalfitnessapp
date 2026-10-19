package com.example.personalfitnessapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CoachLinkParserTest {
    @Test
    fun `splits text around exercise links`() {
        val segments = CoachLinkParser.parse("Try [Bench Press](exercise:Barbell_Bench_Press) then rest.")

        assertEquals(
            listOf(
                CoachSegment.Text("Try "),
                CoachSegment.ExerciseLink("Bench Press", "Barbell_Bench_Press"),
                CoachSegment.Text(" then rest."),
            ),
            segments,
        )
    }

    @Test
    fun `plain text and other markdown links are left alone`() {
        assertEquals(listOf(CoachSegment.Text("No links [here](https://x.com)")), CoachLinkParser.parse("No links [here](https://x.com)"))
        assertEquals(emptyList<CoachSegment>(), CoachLinkParser.parse(""))
    }

    @Test
    fun `handles adjacent links`() {
        val segments = CoachLinkParser.parse("[A](exercise:a)[B](exercise:b)")

        assertEquals(listOf(CoachSegment.ExerciseLink("A", "a"), CoachSegment.ExerciseLink("B", "b")), segments)
    }
}
