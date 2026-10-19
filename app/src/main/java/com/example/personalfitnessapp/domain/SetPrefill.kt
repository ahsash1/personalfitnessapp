package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity

data class SetPrefill(val weightKg: Float, val reps: Int)

/**
 * Baseline prefill: same weight/reps as the same set index last session. This
 * is the fallback ("or from the same set index last
 * session if no suggestion exists"); Milestone 5 layers the double-progression
 * suggestion on top of this for when a suggestion *does* exist.
 */
object SetPrefillCalculator {
    fun prefillFor(previousSessionSets: List<SetLogEntity>, setIndex: Int): SetPrefill? {
        val match = previousSessionSets.firstOrNull { it.setIndex == setIndex } ?: return null
        return SetPrefill(match.weightKg, match.reps)
    }
}
