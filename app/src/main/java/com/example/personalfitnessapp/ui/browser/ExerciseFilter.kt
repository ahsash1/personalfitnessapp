package com.example.personalfitnessapp.ui.browser

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.model.Muscle

/** Pure filtering logic for the exercise browser, kept out of the ViewModel so it's unit-testable. */
object ExerciseFilter {
    /** Gym shorthand expanded before matching ("db incline press" finds the dumbbell press). */
    private val SYNONYMS = mapOf(
        "db" to "dumbbell", "dbs" to "dumbbell", "bb" to "barbell", "kb" to "kettlebell",
        "ohp" to "overhead press", "rdl" to "romanian deadlift", "sldl" to "straight leg deadlift",
        "tri" to "triceps", "bi" to "biceps", "bis" to "biceps", "pec" to "chest",
        "pullup" to "pull up", "chinup" to "chin up", "pushup" to "push up", "situp" to "sit up",
    )

    /**
     * Every word of the query must start a word of the exercise's name, aliases, equipment or
     * muscles, in any order; results whose name matches best come first.
     */
    fun apply(
        exercises: List<ExerciseEntity>,
        query: String,
        muscle: Muscle?,
        equipment: String?,
    ): List<ExerciseEntity> {
        val tokens = tokens(query).flatMap { t -> SYNONYMS[t]?.let(::tokens) ?: listOf(t) }
        val filtered = exercises.filter { exercise ->
            (muscle == null || muscle in exercise.primaryMuscles || muscle in exercise.secondaryMuscles) &&
                (equipment == null || exercise.equipment == equipment) &&
                (tokens.isEmpty() || matches(exercise, tokens))
        }
        if (tokens.isEmpty()) return filtered
        val q = query.trim().lowercase()
        return filtered.sortedWith(
            compareBy<ExerciseEntity> { e ->
                val name = e.name.lowercase()
                when {
                    name == q -> 0
                    name.startsWith(q) -> 1
                    tokens.all { t -> words(e.name).any { it.startsWith(t) } } -> 2
                    else -> 3
                }
            }.thenBy { it.name.length },
        )
    }

    private fun matches(exercise: ExerciseEntity, tokens: List<String>): Boolean {
        val haystack = buildList {
            addAll(words(exercise.name))
            exercise.aliases.forEach { addAll(words(it)) }
            addAll(words(exercise.equipment))
            exercise.primaryMuscles.forEach { addAll(words(it.displayName)) }
        }
        return tokens.all { t -> haystack.any { it.startsWith(t) } }
    }

    private fun words(text: String) = tokens(text)

    /** Lowercase words with a plural "s" dropped, so "curls" and "curl" match. */
    private fun tokens(text: String): List<String> = text.lowercase()
        .replace(Regex("[^a-z0-9]+"), " ").trim().split(" ")
        .filter { it.isNotEmpty() }
        .map { if (it.length > 3 && it.endsWith("s") && !it.endsWith("ss")) it.dropLast(1) else it }
}
