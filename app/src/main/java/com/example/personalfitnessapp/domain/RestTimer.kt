package com.example.personalfitnessapp.domain

/**
 * Timestamp-based rest timer state: while running we store
 * the end timestamp; while paused we store the remaining millis. This makes
 * "how much time is left" correct even if the process was killed and restarted
 * mid-rest, since it's never derived from an in-memory tick count.
 */
data class RestTimerState(
    val endAtEpochMillis: Long?,
    val pausedRemainingMillis: Long?,
) {
    val isRunning: Boolean get() = endAtEpochMillis != null
    val isPaused: Boolean get() = pausedRemainingMillis != null
    val isIdle: Boolean get() = endAtEpochMillis == null && pausedRemainingMillis == null

    fun remainingMillis(nowEpochMillis: Long): Long = when {
        endAtEpochMillis != null -> (endAtEpochMillis - nowEpochMillis).coerceAtLeast(0)
        pausedRemainingMillis != null -> pausedRemainingMillis
        else -> 0
    }

    fun isFinished(nowEpochMillis: Long): Boolean =
        endAtEpochMillis != null && nowEpochMillis >= endAtEpochMillis

    companion object {
        fun idle() = RestTimerState(endAtEpochMillis = null, pausedRemainingMillis = null)

        fun started(durationMillis: Long, nowEpochMillis: Long) =
            RestTimerState(endAtEpochMillis = nowEpochMillis + durationMillis, pausedRemainingMillis = null)
    }
}

object RestTimerEngine {
    fun pause(state: RestTimerState, nowEpochMillis: Long): RestTimerState {
        if (!state.isRunning) return state
        return RestTimerState(
            endAtEpochMillis = null,
            pausedRemainingMillis = state.remainingMillis(nowEpochMillis),
        )
    }

    fun resume(state: RestTimerState, nowEpochMillis: Long): RestTimerState {
        if (!state.isPaused) return state
        return RestTimerState(
            endAtEpochMillis = nowEpochMillis + (state.pausedRemainingMillis ?: 0),
            pausedRemainingMillis = null,
        )
    }

    /** Positive [deltaMillis] adds time, negative subtracts; never goes below zero. */
    fun adjust(state: RestTimerState, deltaMillis: Long, nowEpochMillis: Long): RestTimerState = when {
        state.isRunning -> {
            val newEnd = (state.endAtEpochMillis!! + deltaMillis).coerceAtLeast(nowEpochMillis)
            state.copy(endAtEpochMillis = newEnd)
        }
        state.isPaused -> {
            val newRemaining = (state.pausedRemainingMillis!! + deltaMillis).coerceAtLeast(0)
            state.copy(pausedRemainingMillis = newRemaining)
        }
        else -> state
    }

    fun skip(): RestTimerState = RestTimerState.idle()
}
