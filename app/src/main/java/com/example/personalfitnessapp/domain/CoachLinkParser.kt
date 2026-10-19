package com.example.personalfitnessapp.domain

sealed interface CoachSegment {
    data class Text(val text: String) : CoachSegment
    data class ExerciseLink(val label: String, val exerciseId: String) : CoachSegment
}

/** Splits coach text into plain runs and `[Name](exercise:ID)` links to database exercises. */
object CoachLinkParser {
    private val LINK = Regex("""\[([^\]]+)]\(exercise:([^)\s]+)\)""")

    fun parse(text: String): List<CoachSegment> {
        val segments = mutableListOf<CoachSegment>()
        var cursor = 0
        for (match in LINK.findAll(text)) {
            if (match.range.first > cursor) segments += CoachSegment.Text(text.substring(cursor, match.range.first))
            segments += CoachSegment.ExerciseLink(match.groupValues[1], match.groupValues[2])
            cursor = match.range.last + 1
        }
        if (cursor < text.length) segments += CoachSegment.Text(text.substring(cursor))
        return segments
    }
}
