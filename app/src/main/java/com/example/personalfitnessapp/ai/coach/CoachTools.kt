package com.example.personalfitnessapp.ai.coach

import com.example.personalfitnessapp.ai.ToolCall
import com.example.personalfitnessapp.ai.ToolDeclaration
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.Records
import com.example.personalfitnessapp.ui.browser.ExerciseFilter
import com.example.personalfitnessapp.domain.ProposalValidation
import com.example.personalfitnessapp.domain.RawProposedExercise
import com.example.personalfitnessapp.domain.RecoveryStatus
import com.example.personalfitnessapp.domain.RoutineProposalValidator
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private const val MAX_HISTORY_DAYS = 90
private const val DEFAULT_HISTORY_DAYS = 28
private const val MAX_SEARCH_RESULTS = 15
private const val MILLIS_PER_DAY = 24 * 3_600_000L

/**
 * The coach's read-only window into the app plus [PROPOSE_ROUTINE], which only drafts.
 * Nothing here writes to the database: saving happens in the UI after the user confirms.
 */
class CoachTools @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val workoutLog: WorkoutLogRepository,
    private val contextProvider: CoachContextProvider,
    private val clock: Clock,
) : CoachToolExecutor {

    override val declarations: List<ToolDeclaration> = listOf(
        ToolDeclaration(
            GET_WORKOUT_HISTORY,
            "Sets the user logged for one exercise over the last N days, grouped by workout (weights in kg).",
            schema(
                required = listOf("exercise"),
                "exercise" to ("string" to "Exercise ID or name"),
                "days" to ("integer" to "How many days back, default $DEFAULT_HISTORY_DAYS, max $MAX_HISTORY_DAYS"),
            ),
        ),
        ToolDeclaration(
            GET_PERSONAL_RECORDS,
            "All-time bests for one exercise: heaviest weight, best estimated 1RM, best set volume, best weight at each rep count, and the last session's sets.",
            schema(required = listOf("exercise"), "exercise" to ("string" to "Exercise ID or name")),
        ),
        ToolDeclaration(
            GET_EXERCISE_DETAILS,
            "Muscles worked, setup, execution steps, form cues and common mistakes for one exercise. " +
                "Use it to troubleshoot technique or explain how an exercise should feel.",
            schema(required = listOf("exercise"), "exercise" to ("string" to "Exercise ID or name")),
        ),
        ToolDeclaration(
            GET_RECOVERY_STATUS,
            "Current recovery status (ready, recovering, fatigued) for every muscle group.",
            schema(required = emptyList()),
        ),
        ToolDeclaration(
            SEARCH_EXERCISES,
            "Find exercises in the app's database. Returns IDs that must be used when recommending exercises.",
            schema(
                required = emptyList(),
                "muscle" to ("string" to "Primary muscle, one of: ${Muscle.entries.joinToString { it.name }}"),
                "equipment" to ("string" to "Equipment text to match, e.g. barbell, dumbbell, cable, machine"),
                "name" to ("string" to "Words from the exercise name in any order, e.g. incline dumbbell press"),
            ),
        ),
        ToolDeclaration(
            GET_CURRENT_PLAN,
            "The user's saved routines (with exercise IDs) and which routine is scheduled on which weekday.",
            schema(required = emptyList()),
        ),
        ToolDeclaration(
            PROPOSE_ROUTINE,
            "Draft a routine for the user to review. Nothing is saved until they confirm in the app. " +
                "Every exercise_id must come from search_exercises.",
            proposeRoutineSchema(),
        ),
    )

    override suspend fun execute(call: ToolCall): ToolOutcome = when (call.name) {
        GET_WORKOUT_HISTORY -> ToolOutcome(workoutHistory(call.arguments))
        GET_EXERCISE_DETAILS -> ToolOutcome(exerciseDetails(call.arguments))
        GET_PERSONAL_RECORDS -> ToolOutcome(personalRecords(call.arguments))
        GET_RECOVERY_STATUS -> ToolOutcome(recoveryStatus())
        SEARCH_EXERCISES -> ToolOutcome(searchExercises(call.arguments))
        GET_CURRENT_PLAN -> ToolOutcome(currentPlan())
        PROPOSE_ROUTINE -> proposeRoutine(call.arguments)
        else -> ToolOutcome("Unknown tool ${call.name}. Available: ${declarations.joinToString { it.name }}.")
    }

    private suspend fun workoutHistory(args: JsonObject): String {
        val query = args.string("exercise")
        val exercise = findExercise(query)
            ?: return "No exercise matches \"$query\". Use search_exercises to find it."
        val days = (args.int("days") ?: DEFAULT_HISTORY_DAYS).coerceIn(1, MAX_HISTORY_DAYS)
        val since = clock.millis() - days * MILLIS_PER_DAY
        val sets = sessionRepository.setLogsSince(since).filter { it.exerciseId == exercise.id }
        if (sets.isEmpty()) return "No sets logged for ${exercise.name} in the last $days days."

        return sets.groupBy { it.sessionId }.values
            .sortedBy { group -> group.minOf { it.completedAt } }
            .joinToString("\n", prefix = "${exercise.name} (id ${exercise.id}), kg x reps:\n") { group ->
                val date = Instant.ofEpochMilli(group.minOf { it.completedAt }).atZone(clock.zone).toLocalDate()
                val sorted = group.sortedBy { it.setIndex }.joinToString(", ") { "${formatKg(it.weightKg)}x${it.reps}" }
                "$date: $sorted"
            }
    }

    private suspend fun personalRecords(args: JsonObject): String {
        val query = args.string("exercise")
        val exercise = findExercise(query)
            ?: return "No exercise matches \"$query\". Use search_exercises to find it."
        val sets = workoutLog.allLoggedSets(exercise.id)
        if (sets.isEmpty()) return "No finished workouts with ${exercise.name} yet."
        val bests = Records.bests(sets)
        val byReps = Records.setRecords(sets).joinToString(", ") { (reps, kg) -> "$reps reps: ${WorkoutFormat.weight(kg)} kg" }
        val lastSession = sets.maxOf { it.sessionStartedAt }
        val last = sets.filter { it.sessionStartedAt == lastSession }.sortedBy { it.setIndex }
            .joinToString(", ") { "${WorkoutFormat.weight(it.weightKg)}x${it.reps}" }
        val lastDate = Instant.ofEpochMilli(lastSession).atZone(clock.zone).toLocalDate()
        return buildString {
            appendLine("${exercise.name} (id ${exercise.id}), ${sets.map { it.sessionId }.distinct().size} workouts logged")
            bests.heaviestKg?.let { appendLine("Heaviest: ${WorkoutFormat.weight(it)} kg") }
            bests.best1RmKg?.let { appendLine("Best estimated 1RM: ${WorkoutFormat.weight(Math.round(it * 10) / 10.0)} kg") }
            bests.bestSetVolumeLabel?.let { appendLine("Best set (volume): $it") }
            if (byReps.isNotEmpty()) appendLine("Best weight per rep count: $byReps")
            append("Last session ($lastDate): $last")
        }
    }

    private suspend fun exerciseDetails(args: JsonObject): String {
        val query = args.string("exercise")
        val e = findExercise(query) ?: return "No exercise matches \"$query\". Use search_exercises to find it."
        fun section(title: String, items: List<String>) =
            if (items.isEmpty()) "$title: (none recorded; use general knowledge)"
            else "$title:\n" + items.joinToString("\n") { "- $it" }
        return listOf(
            "${e.name} (id ${e.id}), ${e.equipment}, ${e.mechanic ?: "unspecified"}",
            "Primary muscles: ${e.primaryMuscles.joinToString { it.displayName }}",
            "Secondary muscles: ${e.secondaryMuscles.joinToString { it.displayName }.ifEmpty { "none" }}",
            section("Setup", e.setupSteps),
            section("Execution", e.executionSteps),
            section("Form cues", e.formCues),
            section("Common mistakes", e.commonMistakes),
        ).joinToString("\n")
    }

    private suspend fun recoveryStatus(): String = contextProvider.recovery().joinToString("\n") { r ->
        val label = when (r.status) {
            RecoveryStatus.READY -> "ready"
            RecoveryStatus.RECOVERING -> "recovering"
            RecoveryStatus.FATIGUED -> "fatigued"
        }
        "${r.muscle.displayName}: $label"
    }

    private suspend fun searchExercises(args: JsonObject): String {
        val muscleQuery = args.string("muscle")
        val muscle = if (muscleQuery.isBlank()) null else Muscle.entries.firstOrNull {
            it.name.equals(muscleQuery.replace(' ', '_'), ignoreCase = true) ||
                it.displayName.equals(muscleQuery, ignoreCase = true)
        } ?: return "Unknown muscle \"$muscleQuery\". Valid values: ${Muscle.entries.joinToString { it.name }}."
        val equipment = args.string("equipment")

        val name = args.string("name")
        val all = exerciseRepository.getAll()
            .filter { muscle == null || muscle in it.primaryMuscles }
            .filter { equipment.isBlank() || it.equipment.contains(equipment, ignoreCase = true) }
        // Common gym-floor exercises (the catalog names, which carry the dataset name as an alias) first.
        val matches = if (name.isNotBlank()) ExerciseFilter.apply(all, name, null, null)
        else all.sortedWith(compareByDescending<ExerciseEntity> { it.aliases.isNotEmpty() || it.id.startsWith("hevy_") }.thenBy { it.name })
        if (matches.isEmpty()) return "No exercises found for those filters."

        val shown = matches.take(MAX_SEARCH_RESULTS)
        val lines = shown.joinToString("\n") {
            "${it.id} | ${it.name} | ${it.equipment} | ${it.primaryMuscles.joinToString { m -> m.name }}"
        }
        val more = if (matches.size > shown.size) "\n(${matches.size - shown.size} more; narrow the filters)" else ""
        return "id | name | equipment | primary muscles\n$lines$more"
    }

    private suspend fun currentPlan(): String {
        val routines = contextProvider.routineLines().ifEmpty { listOf("No routines saved.") }
        val schedule = contextProvider.scheduleLines().ifEmpty { listOf("Nothing scheduled.") }
        return "Routines:\n${routines.joinToString("\n")}\nWeekly schedule:\n${schedule.joinToString("\n")}"
    }

    private suspend fun proposeRoutine(args: JsonObject): ToolOutcome {
        val raw = (args["exercises"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            RawProposedExercise(item.string("exercise_id"), item.int("sets"), item.int("rep_min"), item.int("rep_max"))
        }
        val known = exerciseRepository.getAll().associateBy { it.id }
        return when (val result = RoutineProposalValidator.validate(args.string("name"), args.string("notes"), raw, known)) {
            is ProposalValidation.Invalid -> ToolOutcome("Proposal rejected: ${result.reason}")
            is ProposalValidation.Valid -> ToolOutcome(
                content = "Proposal shown to the user for confirmation. It is NOT saved yet; " +
                    "tell them to review and tap Save.",
                proposal = result.proposal,
            )
        }
    }

    /** IDs match exactly; otherwise the shortest name containing the query wins (closest to what was typed). */
    private suspend fun findExercise(query: String): ExerciseEntity? {
        if (query.isBlank()) return null
        val all = exerciseRepository.getAll()
        return all.firstOrNull { it.id == query }
            ?: all.firstOrNull { it.name.equals(query, ignoreCase = true) }
            ?: ExerciseFilter.apply(all, query, null, null).firstOrNull()
    }

    private fun formatKg(kg: Float): String = if (kg % 1f == 0f) kg.toInt().toString() else "%.1f".format(kg)

    private fun schema(
        required: List<String>,
        vararg properties: Pair<String, Pair<String, String>>,
    ): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            properties.forEach { (name, typeAndDescription) ->
                putJsonObject(name) {
                    put("type", typeAndDescription.first)
                    put("description", typeAndDescription.second)
                }
            }
        }
        putJsonArray("required") { required.forEach { add(it) } }
    }

    private fun proposeRoutineSchema(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("name") { put("type", "string"); put("description", "Short routine name, e.g. Push A") }
            putJsonObject("notes") { put("type", "string"); put("description", "Optional one-line note") }
            putJsonObject("exercises") {
                put("type", "array")
                put("description", "Ordered exercises, at most ${RoutineProposalValidator.MAX_EXERCISES}")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("exercise_id") { put("type", "string") }
                        putJsonObject("sets") { put("type", "integer") }
                        putJsonObject("rep_min") { put("type", "integer") }
                        putJsonObject("rep_max") { put("type", "integer") }
                    }
                    putJsonArray("required") { add("exercise_id") }
                }
            }
        }
        putJsonArray("required") { add("name"); add("exercises") }
    }

    private fun JsonObject.string(key: String): String = (this[key] as? JsonPrimitive)?.content.orEmpty()

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.content.toDoubleOrNull()?.toInt() }

    companion object {
        const val GET_WORKOUT_HISTORY = "get_workout_history"
        const val GET_EXERCISE_DETAILS = "get_exercise_details"
        const val GET_PERSONAL_RECORDS = "get_personal_records"
        const val GET_RECOVERY_STATUS = "get_recovery_status"
        const val SEARCH_EXERCISES = "search_exercises"
        const val GET_CURRENT_PLAN = "get_current_plan"
        const val PROPOSE_ROUTINE = "propose_routine"
    }
}
