package com.example.personalfitnessapp.domain

data class ExerciseHighlight(val exerciseName: String, val description: String)

data class WorkoutSummary(
    val durationMinutes: Long,
    val totalSets: Int,
    val prCount: Int,
    val highlights: List<ExerciseHighlight>,
)

/**
 * One concise line per exercise that actually improved this session - a PR, or the exercise's
 * best single-set delta. Exercises that were merely matched or behind are omitted entirely:
 * this is a short "that counted for something" moment, not a full report.
 */
object WorkoutSummaryBuilder {
    data class ExerciseSetResult(val exerciseName: String, val comparison: SetComparison, val isNewRecord: Boolean)

    fun build(
        startedAtMillis: Long,
        endedAtMillis: Long,
        setResultsByExercise: Map<String, List<ExerciseSetResult>>,
    ): WorkoutSummary {
        val totalSets = setResultsByExercise.values.sumOf { it.size }
        val prCount = setResultsByExercise.values.sumOf { results -> results.count { it.isNewRecord } }
        val highlights = setResultsByExercise.values.mapNotNull { highlightFor(it) }
        val durationMinutes = (endedAtMillis - startedAtMillis).coerceAtLeast(0) / 60_000L
        return WorkoutSummary(durationMinutes, totalSets, prCount, highlights)
    }

    private fun highlightFor(results: List<ExerciseSetResult>): ExerciseHighlight? {
        if (results.isEmpty()) return null
        val name = results.first().exerciseName
        if (results.any { it.isNewRecord }) return ExerciseHighlight(name, "New PR")

        val best = results.maxByOrNull { (it.comparison.weightDeltaKg ?: 0f) * 1_000 + (it.comparison.repsDelta ?: 0) }
            ?: return null
        val weightDelta = best.comparison.weightDeltaKg
        val repsDelta = best.comparison.repsDelta
        val description = when {
            weightDelta != null && weightDelta > 0 -> "+${formatKg(weightDelta)} kg"
            repsDelta != null && repsDelta > 0 -> "+$repsDelta rep${if (repsDelta == 1) "" else "s"}"
            else -> null
        }
        return description?.let { ExerciseHighlight(name, it) }
    }

    private fun formatKg(kg: Float): String = if (kg % 1f == 0f) kg.toInt().toString() else "%.1f".format(kg)
}
