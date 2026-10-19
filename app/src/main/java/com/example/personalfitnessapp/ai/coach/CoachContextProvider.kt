package com.example.personalfitnessapp.ai.coach

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.data.repository.UserProfileRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
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

/** Gathers the app's data into the compact facts the coach sees; the wording lives in [CoachContextBuilder]. */
class CoachContextProvider @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val routineRepository: RoutineRepository,
    private val userProfileRepository: UserProfileRepository,
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
        )
    }

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
