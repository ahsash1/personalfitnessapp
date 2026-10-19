package com.example.personalfitnessapp.domain.workout

import com.example.personalfitnessapp.data.local.entity.SetType

/** Pure edits on a [WorkoutDraft]; screens call these and persist the result. */
object DraftOps {
    /** Each new exercise gets as many empty sets as it had last time (placeholders fill them), or one. */
    fun addExercise(draft: WorkoutDraft, exerciseId: String, restSeconds: Int?, setCount: Int): WorkoutDraft =
        draft.copy(
            exercises = draft.exercises + DraftExercise(
                exerciseId = exerciseId,
                restSeconds = restSeconds,
                sets = List(setCount.coerceAtLeast(1)) { DraftSet() },
            )
        )

    fun removeExercise(draft: WorkoutDraft, exerciseKey: String): WorkoutDraft {
        val removed = draft.exercises.firstOrNull { it.key == exerciseKey } ?: return draft
        val remaining = draft.exercises.filter { it.key != exerciseKey }
        return draft.copy(exercises = dissolveLoneSupersets(remaining, removed.supersetGroup))
    }

    /** "Replace Exercise" keeps the slot but starts over with one empty set (spec §9). */
    fun replaceExercise(draft: WorkoutDraft, exerciseKey: String, newExerciseId: String, restSeconds: Int?, setCount: Int) =
        mapExercise(draft, exerciseKey) {
            it.copy(
                exerciseId = newExerciseId,
                restSeconds = restSeconds,
                note = null,
                noteOpen = false,
                sets = List(setCount.coerceAtLeast(1)) { DraftSet() },
            )
        }

    fun moveExercise(draft: WorkoutDraft, from: Int, to: Int): WorkoutDraft {
        if (from !in draft.exercises.indices || to !in draft.exercises.indices || from == to) return draft
        val list = draft.exercises.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        return draft.copy(exercises = list)
    }

    fun setRest(draft: WorkoutDraft, exerciseKey: String, seconds: Int?) =
        mapExercise(draft, exerciseKey) { it.copy(restSeconds = seconds?.takeIf { s -> s > 0 }) }

    fun openNote(draft: WorkoutDraft, exerciseKey: String) = mapExercise(draft, exerciseKey) { it.copy(noteOpen = true) }

    /** An empty note closes the line again (spec §14: "Empty note on blur → line disappears"). */
    fun setNote(draft: WorkoutDraft, exerciseKey: String, text: String, closeIfEmpty: Boolean) =
        mapExercise(draft, exerciseKey) {
            val note = text.ifBlank { null }
            it.copy(note = note, noteOpen = if (note == null && closeIfEmpty) false else it.noteOpen || note != null)
        }

    /** "+ Add Set": a fresh row; its placeholder copies the row above (see [WorkoutMath.placeholder]). */
    fun addSet(draft: WorkoutDraft, exerciseKey: String) = mapExercise(draft, exerciseKey) {
        val lastType = it.sets.lastOrNull()?.type?.takeIf { t -> t != SetType.WARMUP } ?: SetType.NORMAL
        it.copy(sets = it.sets + DraftSet(type = lastType))
    }

    fun removeSet(draft: WorkoutDraft, exerciseKey: String, setKey: String) =
        mapExercise(draft, exerciseKey) { it.copy(sets = it.sets.filter { s -> s.key != setKey }) }

    fun updateSet(draft: WorkoutDraft, exerciseKey: String, setKey: String, transform: (DraftSet) -> DraftSet) =
        mapExercise(draft, exerciseKey) { it.copy(sets = it.sets.map { s -> if (s.key == setKey) transform(s) else s }) }

    fun setType(draft: WorkoutDraft, exerciseKey: String, setKey: String, type: SetType) =
        updateSet(draft, exerciseKey, setKey) { it.copy(type = type) }

    /** Groups [exerciseKey] with [otherKey]; joining an existing superset adds to it. */
    fun addToSuperset(draft: WorkoutDraft, exerciseKey: String, otherKey: String): WorkoutDraft {
        if (exerciseKey == otherKey) return draft
        val a = draft.exercises.firstOrNull { it.key == exerciseKey } ?: return draft
        val b = draft.exercises.firstOrNull { it.key == otherKey } ?: return draft
        val group = b.supersetGroup ?: a.supersetGroup ?: ((draft.exercises.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1)
        val previousGroup = a.supersetGroup
        val updated = draft.exercises.map {
            if (it.key == exerciseKey || it.key == otherKey) it.copy(supersetGroup = group) else it
        }
        return draft.copy(exercises = if (previousGroup != null && previousGroup != group) dissolveLoneSupersets(updated, previousGroup) else updated)
    }

    fun removeFromSuperset(draft: WorkoutDraft, exerciseKey: String): WorkoutDraft {
        val group = draft.exercises.firstOrNull { it.key == exerciseKey }?.supersetGroup ?: return draft
        val updated = draft.exercises.map { if (it.key == exerciseKey) it.copy(supersetGroup = null) else it }
        return draft.copy(exercises = dissolveLoneSupersets(updated, group))
    }

    /** A superset of one is just an exercise. */
    private fun dissolveLoneSupersets(exercises: List<DraftExercise>, group: Int?): List<DraftExercise> {
        if (group == null) return exercises
        val members = exercises.count { it.supersetGroup == group }
        return if (members >= 2) exercises else exercises.map { if (it.supersetGroup == group) it.copy(supersetGroup = null) else it }
    }

    /** Finishing keeps completed sets only (incomplete ones are discarded after confirmation). */
    fun withoutIncompleteSets(draft: WorkoutDraft): WorkoutDraft = draft.copy(
        exercises = draft.exercises
            .map { ex -> ex.copy(sets = ex.sets.filter { it.completed }) }
            .filter { it.sets.isNotEmpty() }
    )

    private fun mapExercise(draft: WorkoutDraft, exerciseKey: String, transform: (DraftExercise) -> DraftExercise) =
        draft.copy(exercises = draft.exercises.map { if (it.key == exerciseKey) transform(it) else it })
}
