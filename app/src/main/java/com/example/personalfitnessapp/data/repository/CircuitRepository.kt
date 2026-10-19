package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.importer.ExerciseNameMatcher
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.local.entity.RoutineExerciseEntity
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.domain.workout.CircuitPlan
import com.example.personalfitnessapp.domain.workout.DraftExercise
import com.example.personalfitnessapp.domain.workout.DraftSet
import com.example.personalfitnessapp.domain.workout.ExerciseTypes
import com.example.personalfitnessapp.domain.workout.WorkoutDraft
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class CircuitMove(val exerciseId: String, val name: String, val imageRefs: List<String>)
data class LoadedCircuit(val routine: RoutineEntity, val moves: List<CircuitMove>)

/** Timed circuits are routines with work/rest/rounds set; their moves are timed exercises. */
class CircuitRepository @Inject constructor(
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val log: WorkoutLogRepository,
) {
    suspend fun create(plan: CircuitPlan): Long {
        val all = exercises.getAll().toMutableList()
        val routineId = routines.saveRoutine(
            RoutineEntity(name = plan.name, notes = "", workSeconds = plan.workSec, restSeconds = plan.restSec, rounds = plan.rounds)
        )
        plan.moves.forEachIndexed { index, move ->
            routines.addRoutineExercise(
                RoutineExerciseEntity(routineId = routineId, exerciseId = timedExerciseFor(move, all), order = index, targetSets = 1, repMin = 0, repMax = 0, restSecondsOverride = null)
            )
        }
        return routineId
    }

    /** Prefers a timed/bodyweight exercise with that name (so "Single Leg Extensions" isn't the leg machine), else makes one. */
    private suspend fun timedExerciseFor(name: String, all: MutableList<ExerciseEntity>): String {
        val timed = all.filter { ExerciseTypes.of(it).let { t -> t == ExerciseType.DURATION || t == ExerciseType.REPS_ONLY } }
        timed.firstOrNull { it.name.equals(name, true) || it.name.equals("$name (Core)", true) }?.let { return it.id }
        ExerciseNameMatcher.match(name, timed)?.let { return it.id }
        val core = Regex("crunch|plank|leg|heel|scissor|twist|sit|v.?up|hollow|flutter|bicycle|ab", RegexOption.IGNORE_CASE).containsMatchIn(name)
        val taken = all.any { it.name.equals(name, true) }
        val entity = ExerciseEntity(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            name = if (taken) "$name (Core)" else name,
            aliases = emptyList(), primaryMuscles = if (core) listOf(Muscle.ABS) else emptyList(), secondaryMuscles = emptyList(),
            equipment = "Bodyweight", movementPattern = "", mechanic = null, level = ExerciseLevel.INTERMEDIATE,
            setupSteps = emptyList(), executionSteps = emptyList(), formCues = emptyList(), commonMistakes = emptyList(),
            defaultRestSeconds = 0, imageRefs = emptyList(), source = ExerciseSource.USER, verified = false,
            formGuideAiGenerated = false, exerciseType = ExerciseType.DURATION,
        )
        exercises.insert(entity)
        all += entity
        return entity.id
    }

    suspend fun load(routineId: Long): LoadedCircuit? {
        val routine = routines.observeRoutine(routineId).first() ?: return null
        val moves = routines.observeRoutineExercises(routineId).first().sortedBy { it.order }.mapNotNull { row ->
            exercises.getById(row.exerciseId)?.let { CircuitMove(it.id, it.name, it.imageRefs) }
        }
        return LoadedCircuit(routine, moves)
    }

    suspend fun exists(name: String): Boolean = routines.observeRoutines().first().any { it.name.equals(name, true) && it.isCircuit }

    /** Saves a finished (or stopped) circuit: one timed set per completed work interval. */
    suspend fun saveSession(circuit: LoadedCircuit, startedAt: Long, endedAt: Long, worked: List<Pair<Int, Int>>): Long {
        val byMove = worked.groupBy({ it.first }, { it.second })
        val draft = WorkoutDraft(
            circuit.moves.mapIndexedNotNull { i, move ->
                val secs = byMove[i] ?: return@mapIndexedNotNull null
                DraftExercise(exerciseId = move.exerciseId, restSeconds = 0, sets = secs.map { DraftSet(durationSec = it, completed = true, completedAt = endedAt) })
            }
        )
        return log.insertFinished(
            WorkoutSessionEntity(startedAt = startedAt, endedAt = endedAt, routineId = circuit.routine.id, notes = "", title = circuit.routine.name),
            draft,
        )
    }
}
