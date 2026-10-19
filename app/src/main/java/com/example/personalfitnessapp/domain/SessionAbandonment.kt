package com.example.personalfitnessapp.domain

/**
 * An active session this old with no recent activity is treated as abandoned rather than
 * something to blindly offer "Resume" for. Centralized here, like
 * RestDefaults/RecoveryConstants, so the threshold is easy to find and tune.
 */
object SessionAbandonment {
    const val STALE_AFTER_HOURS = 16

    /** [lastActivityMillis] is the session's most recent set, or its start time if no sets yet. */
    fun isStale(lastActivityMillis: Long, nowMillis: Long): Boolean =
        nowMillis - lastActivityMillis >= STALE_AFTER_HOURS * 3_600_000L
}
