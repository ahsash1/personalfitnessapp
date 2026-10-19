package com.example.personalfitnessapp.domain.workout

import com.example.personalfitnessapp.data.local.entity.SetType
import kotlin.math.floor
import kotlin.math.roundToInt

/** Plate calculator and warm-up ramp (docs/screens/workout_settings.md). */
object PlateMath {
    val PLATES_KG = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    data class Loading(val perSide: List<Double>, val remainderKg: Double)

    /** Greedy per side: (target - bar) / 2, largest plates first; what can't be made is the remainder. */
    fun plates(targetKg: Double, barKg: Double, available: List<Double> = PLATES_KG): Loading {
        var side = ((targetKg - barKg) / 2).coerceAtLeast(0.0)
        val out = mutableListOf<Double>()
        for (p in available.sortedDescending()) {
            while (side + 1e-9 >= p) {
                out += p
                side -= p
            }
        }
        return Loading(out, (side * 2).let { if (it < 1e-6) 0.0 else it })
    }

    /** Warm-ups before a working weight: 50% x 8, 70% x 5, 85% x 3, rounded down to the load step. */
    fun warmups(workingKg: Double, stepKg: Double, minKg: Double = 0.0): List<DraftSet> =
        listOf(0.5 to 8, 0.7 to 5, 0.85 to 3).map { (pct, reps) -> roundDown(workingKg * pct, stepKg).coerceAtLeast(minKg) to reps }
            .filter { it.first < workingKg }
            .distinctBy { it.first }
            .map { (kg, reps) -> DraftSet(type = SetType.WARMUP, weight = com.example.personalfitnessapp.domain.WorkoutFormat.weight(kg), reps = reps.toString()) }

    private fun roundDown(kg: Double, step: Double) = floor(kg / step + 1e-9) * step

    /** RPE scale with what each value means in reps left in the tank. */
    val RPE = listOf(
        10.0 to "Max effort, no reps left", 9.5 to "No reps left, maybe more weight", 9.0 to "1 rep left",
        8.5 to "1-2 reps left", 8.0 to "2 reps left", 7.5 to "2-3 reps left", 7.0 to "3 reps left",
        6.5 to "3-4 reps left", 6.0 to "4+ reps left",
    )

    fun rpeLabel(rpe: Double): String = if (rpe % 1.0 == 0.0) rpe.roundToInt().toString() else rpe.toString()
}
