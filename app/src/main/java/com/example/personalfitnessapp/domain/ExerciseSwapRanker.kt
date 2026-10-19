package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity

data class EquipmentFilter(
    /** If non-null, only these equipment types are allowed. */
    val onlyEquipment: Set<String>? = null,
    /** Equipment types to exclude, e.g. "the cable machine is taken". */
    val excludedEquipment: Set<String> = emptySet(),
)

data class SwapCandidate(val exercise: ExerciseEntity, val reason: String)

/**
 * Ranks swap candidates for a given exercise: same primary
 * muscles is a hard requirement, then ranked by movement pattern match,
 * mechanic match, and equipment fit. Deterministic, no AI.
 */
object ExerciseSwapRanker {
    fun rank(
        target: ExerciseEntity,
        pool: List<ExerciseEntity>,
        equipmentFilter: EquipmentFilter = EquipmentFilter(),
        limit: Int = 5,
    ): List<SwapCandidate> {
        val targetPrimary = target.primaryMuscles.toSet()

        return pool
            .asSequence()
            .filter { it.id != target.id }
            .filter { it.primaryMuscles.toSet() == targetPrimary }
            .filter { equipmentFilter.onlyEquipment == null || it.equipment in equipmentFilter.onlyEquipment }
            .filter { it.equipment !in equipmentFilter.excludedEquipment }
            .map { candidate -> candidate to score(target, candidate) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (candidate, _) -> SwapCandidate(candidate, reason(target, candidate)) }
            .toList()
    }

    private fun score(target: ExerciseEntity, candidate: ExerciseEntity): Int {
        var score = 0
        if (candidate.movementPattern == target.movementPattern) score += 2
        if (candidate.mechanic == target.mechanic) score += 1
        return score
    }

    private fun reason(target: ExerciseEntity, candidate: ExerciseEntity): String {
        val parts = mutableListOf("Same muscles")
        if (candidate.movementPattern == target.movementPattern) parts += "same movement pattern"
        parts += "uses ${candidate.equipment.lowercase()}"
        return parts.joinToString(", ").replaceFirstChar { it.uppercase() }
    }
}
