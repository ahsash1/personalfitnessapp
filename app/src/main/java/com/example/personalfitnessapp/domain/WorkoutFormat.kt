package com.example.personalfitnessapp.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Text formats from docs/02_data_model.md and the screen specs, kept pure for unit tests. */
object WorkoutFormat {
    /** Live timer: `46s`, `1min 9s`, `1h 5min`. */
    fun liveDuration(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val seconds = s % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            minutes > 0 -> "${minutes}min ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /** Finished workouts round to whole minutes: 21min 54s -> `22min`, 72min -> `1h 12min`. */
    fun finishedDuration(totalSeconds: Long): String {
        val minutes = (totalSeconds.coerceAtLeast(0) / 60.0).roundToLong()
        val hours = minutes / 60
        val rem = minutes % 60
        return when {
            hours > 0 && rem > 0 -> "${hours}h ${rem}min"
            hours > 0 -> "${hours}h"
            else -> "${minutes}min"
        }
    }

    /** `0 kg`, `436 kg`, `2,593 kg`; one decimal only when there is a fraction. */
    fun volume(kg: Double): String = "${number(kg)} kg"

    fun number(value: Double): String {
        val rounded = Math.round(value * 10) / 10.0
        return if (rounded % 1.0 == 0.0) {
            String.format(Locale.US, "%,d", rounded.toLong())
        } else {
            String.format(Locale.US, "%,.1f", rounded)
        }
    }

    /** Weights in tables: `20`, `22.5`, `1.25` (no thousands separator, trailing zeros dropped). */
    fun weight(kg: Double): String {
        val rounded = Math.round(kg * 100) / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
        else rounded.toString().trimEnd('0').trimEnd('.')
    }

    /** Rest picker labels: `OFF`, `45s`, `2min 0s`, `2min 30s`. */
    fun rest(seconds: Int?): String {
        if (seconds == null || seconds <= 0) return "OFF"
        val m = seconds / 60
        val s = seconds % 60
        return if (m == 0) "${s}s" else "${m}min ${s}s"
    }

    /** Chip label, e.g. `Rest: 2min 30s` / `Rest: OFF`. */
    fun restChip(seconds: Int?): String = "Rest: ${rest(seconds)}"

    /** `02:21`; hours appear only past an hour: `1:02:21`. */
    fun clock(totalSeconds: Long): String {
        val s = abs(totalSeconds)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        val body = if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%02d:%02d", m, sec)
        return if (totalSeconds < 0) "-$body" else body
    }

    /** Previous-column / history text for a weight set: `20kg x 15`. */
    fun weightReps(kg: Double, reps: Int): String = "${weight(kg)}kg x $reps"

    /** Cardio: `0 km in 05:00`. */
    fun distanceTime(km: Double?, seconds: Int?): String =
        "${weight(km ?: 0.0)} km in ${clock((seconds ?: 0).toLong())}"

    fun ordinal(n: Int): String {
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    /** Auto title by start hour (docs/02_data_model.md), with the 💪 the original appends. */
    fun autoTitle(startedAt: Instant, zone: ZoneId): String {
        val hour = startedAt.atZone(zone).hour
        val part = when (hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
        return "$part workout 💪"
    }

    /** Feed timestamps: `just now`, `22 minutes ago`, `3 hours ago`, `Yesterday`, `Oct 15`. */
    fun relative(then: Instant, now: Instant, zone: ZoneId): String {
        val minutes = ChronoUnit.MINUTES.between(then, now)
        val thenDate = then.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            thenDate == today -> {
                val hours = minutes / 60
                if (hours == 1L) "1 hour ago" else "$hours hours ago"
            }
            thenDate == today.minusDays(1) -> "Yesterday"
            thenDate.year == today.year -> thenDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
            else -> thenDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        }
    }

    /** `15 Oct 2026, 3:00 PM` (Edit Workout date row). */
    fun dateTime(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.US))

    /** `Thursday, Oct 15, 2026` (Profile workout cards). */
    fun longDate(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US))

    /** `15 Oct 2026, 15:00` (Exercise history headers). */
    fun historyDate(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.US))

    fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))

    /** `3 sets Incline Bench Press`, `1 set Treadmill`. */
    fun setsOf(count: Int, exerciseName: String): String =
        "$count ${if (count == 1) "set" else "sets"} $exerciseName"
}
