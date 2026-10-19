package com.example.personalfitnessapp.domain.workout

/** A timed circuit: every move gets [workSec] then [restSec], [rounds] times through. */
data class CircuitPlan(val name: String, val workSec: Int, val restSec: Int, val rounds: Int, val moves: List<String>)

/**
 * Reads circuits pasted from timer apps, e.g.
 * "Workout // 50s work, 10s rest" then lines like "00:20 Leg Switches". Timestamps and list
 * numbering are ignored; work/rest default to the gap between timestamps when not stated.
 */
object CircuitText {
    private val WORK = Regex("(\\d+)\\s*s(?:ec(?:onds?)?)?\\s*(?:of\\s*)?work", RegexOption.IGNORE_CASE)
    private val REST = Regex("(\\d+)\\s*s(?:ec(?:onds?)?)?\\s*(?:of\\s*)?rest", RegexOption.IGNORE_CASE)
    private val ROUNDS = Regex("(\\d+)\\s*(?:rounds?|sets?|x)\\b", RegexOption.IGNORE_CASE)
    private val STAMP = Regex("^\\s*(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})\\s+")
    private val BULLET = Regex("^\\s*(?:[-*•]|\\d+[.)])\\s+")

    fun parse(text: String): CircuitPlan? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null
        val header = lines.first().takeIf { WORK.containsMatchIn(it) || REST.containsMatchIn(it) || it.contains("//") }
        val body = if (header != null) lines.drop(1) else lines
        val stamps = mutableListOf<Int>()
        val moves = body.mapNotNull { line ->
            val m = STAMP.find(line)
            if (m != null) stamps += (m.groupValues[1].toIntOrNull() ?: 0) * 3600 + m.groupValues[2].toInt() * 60 + m.groupValues[3].toInt()
            line.replace(STAMP, "").replace(BULLET, "").trim().takeIf { it.isNotEmpty() }
        }
        if (moves.isEmpty()) return null
        val gap = stamps.zipWithNext { a, b -> b - a }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val rest = header?.let { REST.find(it)?.groupValues?.get(1)?.toInt() } ?: if (gap != null) 10 else 15
        val work = header?.let { WORK.find(it)?.groupValues?.get(1)?.toInt() } ?: gap?.minus(rest)?.takeIf { it > 0 } ?: 45
        val rounds = header?.let { ROUNDS.find(it)?.groupValues?.get(1)?.toInt() }?.coerceIn(1, 20) ?: 1
        val rawName = header?.substringBefore("//")?.trim().orEmpty()
        val name = rawName.takeIf { it.isNotBlank() && !it.equals("workout", true) } ?: "Circuit"
        return CircuitPlan(name, work.coerceIn(5, 600), rest.coerceIn(0, 600), rounds, moves)
    }
}

enum class PhaseKind { PREP, WORK, REST }

data class Phase(val kind: PhaseKind, val seconds: Int, val moveIndex: Int, val round: Int)

object CircuitTimeline {
    const val PREP_SECONDS = 10

    /** Get ready, then work/rest for every move and round; no rest after the very last move. */
    fun phases(workSec: Int, restSec: Int, rounds: Int, moveCount: Int): List<Phase> {
        if (moveCount == 0) return emptyList()
        val out = mutableListOf(Phase(PhaseKind.PREP, PREP_SECONDS, 0, 1))
        for (r in 1..rounds) {
            for (i in 0 until moveCount) {
                out += Phase(PhaseKind.WORK, workSec, i, r)
                val last = r == rounds && i == moveCount - 1
                if (!last && restSec > 0) out += Phase(PhaseKind.REST, restSec, if (i + 1 < moveCount) i + 1 else 0, r)
            }
        }
        return out
    }
}
