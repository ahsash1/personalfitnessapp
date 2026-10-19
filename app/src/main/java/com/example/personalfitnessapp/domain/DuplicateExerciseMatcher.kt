package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity

/**
 * Fuzzy name match against existing exercises/aliases, so the
 * photo -> exercise flow can ask "Is this X?" instead of creating a duplicate.
 * Deterministic (Levenshtein-based similarity), no AI involved.
 */
object DuplicateExerciseMatcher {
    /** 0.8 was chosen so close spelling/wording variants match ("Lat Pulldown" vs
     * "Lat Pull-down") without conflating genuinely different exercises. */
    private const val SIMILARITY_THRESHOLD = 0.8

    fun findLikelyMatch(candidateName: String, existing: List<ExerciseEntity>): ExerciseEntity? {
        val normalizedCandidate = normalize(candidateName)
        var best: ExerciseEntity? = null
        var bestScore = 0.0

        for (exercise in existing) {
            val names = listOf(exercise.name) + exercise.aliases
            val score = names.maxOf { similarity(normalizedCandidate, normalize(it)) }
            if (score > bestScore) {
                bestScore = score
                best = exercise
            }
        }

        return if (bestScore >= SIMILARITY_THRESHOLD) best else null
    }

    private fun normalize(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun similarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        val distance = levenshtein(a, b)
        val maxLen = maxOf(a.length, b.length)
        return if (maxLen == 0) 1.0 else 1.0 - distance.toDouble() / maxLen
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }
}
