package com.example.personalfitnessapp.ai.coach

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.data.repository.UserProfileRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.ExerciseTypes
import com.example.personalfitnessapp.domain.workout.WorkoutMath
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import com.example.personalfitnessapp.domain.CoachContextBuilder
import com.example.personalfitnessapp.domain.CoachContextInput
import com.example.personalfitnessapp.domain.MuscleRecovery
import com.example.personalfitnessapp.domain.RecoveryConstants
import com.example.personalfitnessapp.domain.RecoveryModel
import com.example.personalfitnessapp.domain.RecoverySetInput
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

private const val TRAINING_SUMMARY_DAYS = 28L
private const val MILLIS_PER_DAY = 24 * 3_600_000L
private const val RECENT_WORKOUTS = 6

/** Gathers the app's data into the compact facts the coach sees; the wording lives in [CoachContextBuilder]. */
class CoachContextProvider @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val routineRepository: RoutineRepository,
    private val userProfileRepository: UserProfileRepository,
    private val workoutLog: WorkoutLogRepository,
    private val clock: Clock,
) {
    suspend fun systemInstruction(): String {
        val exercisesById = exerciseRepository.getAll().associateBy { it.id }
        val since = clock.millis() - TRAINING_SUMMARY_DAYS * MILLIS_PER_DAY
        return CoachContextBuilder.build(
            CoachContextInput(
                profile = userProfileRepository.getProfile(),
                trainingLines = CoachContextBuilder.trainingSummary(
                    setLogs = sessionRepository.setLogsSince(since),
                    exerciseNames = exercisesById.mapValues { it.value.name },
                    zone = clock.zone,
                ),
                recovery = recovery(exercisesById),
                routineLines = routineLines(exercisesById),
                scheduleLines = scheduleLines(),
            )
        ) + "\n\n" + workoutLines(exercisesById)
    }

    /** Today's date, the last few workouts by day, and the workout in progress (if any). */
    suspend fun workoutLines(exercisesById: Map<String, ExerciseEntity>): String = buildString {
        val today = Instant.ofEpochMilli(clock.millis()).atZone(clock.zone).toLocalDate()
        appendLine("TODAY: ${today.format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd", Locale.US))}")
        appendLine()
        appendLine("LAST WORKOUTS (newest first)")
        val recent = workoutLog.finishedSessions().take(RECENT_WORKOUTS)
        if (recent.isEmpty()) appendLine("- None yet.")
        recent.forEach { session ->
            val date = Instant.ofEpochMilli(session.startedAt).atZone(clock.zone).toLocalDate()
            val daysAgo = ChronoUnit.DAYS.between(date, today)
            val sets = workoutLog.setsOf(session.id)
            val perExercise = sets.groupBy { it.exerciseId }.entries.joinToString("; ") { (id, list) ->
                val e = exercisesById[id]
                val type = e?.let(ExerciseTypes::of) ?: ExerciseType.WEIGHT_REPS
                val shown = list.sortedBy { it.setIndex }.joinToString(", ") { s ->
                    val value = when (type) {
                        ExerciseType.WEIGHT_REPS -> "${WorkoutFormat.weight(s.weightKg.toDouble())}x${s.reps}"
                        ExerciseType.REPS_ONLY -> "${s.reps} reps"
                        else -> WorkoutFormat.distanceTime(s.distanceKm?.toDouble(), s.durationSec)
                    }
                    if (s.type == SetType.NORMAL) value else "$value (${s.type.name.lowercase()})"
                }
                "${e?.name ?: id} (id $id): $shown"
            }
            appendLine("- $date ($daysAgo days ago) ${session.title ?: "Workout"}: $perExercise")
        }
        val active = workoutLog.active.value ?: return@buildString
        appendLine()
        val minutes = (clock.millis() - active.startedAt) / 60_000
        appendLine("CURRENT WORKOUT (in progress, started $minutes min ago; weights kg)")
        if (active.draft.exercises.isEmpty()) appendLine("- No exercises added yet.")
        active.draft.exercises.forEach { ex ->
            val e = exercisesById[ex.exerciseId]
            val type = e?.let(ExerciseTypes::of) ?: ExerciseType.WEIGHT_REPS
            val previous = workoutLog.previousSets(ex.exerciseId, active.sessionId)
            val rest = ex.restSeconds?.takeIf { it > 0 }?.let { ", rest ${WorkoutFormat.rest(it)}" }.orEmpty()
            appendLine("- ${e?.name ?: ex.exerciseId} (id ${ex.exerciseId})$rest${ex.note?.let { ", note: $it" }.orEmpty()}")
            val labels = WorkoutMath.setLabels(ex.sets)
            ex.sets.forEachIndexed { i, set ->
                val r = WorkoutMath.resolve(set, null)
                val entered = when (type) {
                    ExerciseType.WEIGHT_REPS -> if (r.weightKg != null || r.reps != null) "${r.weightKg?.let(WorkoutFormat::weight) ?: "?"}x${r.reps ?: "?"}" else null
                    ExerciseType.REPS_ONLY -> r.reps?.let { "$it reps" }
                    else -> if (r.durationSec != null || r.distanceKm != null) WorkoutFormat.distanceTime(r.distanceKm, r.durationSec) else null
                }
                val last = previous.getOrNull(i)?.let { p ->
                    when (type) {
                        ExerciseType.WEIGHT_REPS -> "${WorkoutFormat.weight(p.weightKg ?: 0.0)}x${p.reps ?: 0}"
                        ExerciseType.REPS_ONLY -> "${p.reps ?: 0} reps"
                        else -> WorkoutFormat.distanceTime(p.distanceKm, p.durationSec)
                    }
                }
                val status = if (set.completed) "done ${entered.orEmpty()}" else "not done${entered?.let { " (entered $it)" }.orEmpty()}"
                appendLine("  set ${labels[i]}: $status${last?.let { "; last time $it" }.orEmpty()}")
            }
        }
    }.trimEnd()

    suspend fun recovery(exercisesById: Map<String, ExerciseEntity>? = null): List<MuscleRecovery> {
        val exercises = exercisesById ?: exerciseRepository.getAll().associateBy { it.id }
        val since = clock.millis() - RecoveryConstants.LOOKBACK_DAYS * MILLIS_PER_DAY
        val inputs = sessionRepository.setLogsSince(since).mapNotNull { log ->
            exercises[log.exerciseId]?.let { RecoverySetInput(log, it) }
        }
        val experience = userProfileRepository.getProfile()?.experienceLevel ?: ExperienceLevel.INTERMEDIATE
        return RecoveryModel.compute(inputs, experience, clock.millis())
    }

    suspend fun routineLines(exercisesById: Map<String, ExerciseEntity>? = null): List<String> {
        val exercises = exercisesById ?: exerciseRepository.getAll().associateBy { it.id }
        return routineRepository.observeRoutines().first().map { routine ->
            val items = routineRepository.observeRoutineExercises(routine.id).first().joinToString(", ") {
                val name = exercises[it.exerciseId]?.name ?: it.exerciseId
                "$name (id ${it.exerciseId}) ${it.targetSets}x${it.repMin}-${it.repMax}"
            }
            "- ${routine.name} (routine ${routine.id}): ${items.ifEmpty { "no exercises" }}"
        }
    }

    suspend fun scheduleLines(): List<String> {
        val routinesById = routineRepository.observeRoutines().first().associateBy { it.id }
        return routineRepository.observeSchedule().first()
            .sortedBy { it.dayOfWeek.ordinal }
            .mapNotNull { entry ->
                val routine = entry.routineId?.let(routinesById::get) ?: return@mapNotNull null
                "- ${entry.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}: ${routine.name}"
            }
    }
}
