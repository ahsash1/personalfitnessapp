package com.example.personalfitnessapp.domain

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutFormatTest {
    private val zone = ZoneId.of("Asia/Dubai")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0): Instant =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant()

    @Test
    fun `live duration matches the active workout header`() {
        assertEquals("46s", WorkoutFormat.liveDuration(46))
        assertEquals("1min 9s", WorkoutFormat.liveDuration(69))
        assertEquals("1h 5min", WorkoutFormat.liveDuration(3_900))
    }

    @Test
    fun `finished duration rounds to minutes`() {
        assertEquals("22min", WorkoutFormat.finishedDuration(21 * 60 + 54L))
        assertEquals("1h 12min", WorkoutFormat.finishedDuration(72 * 60L))
        assertEquals("1h", WorkoutFormat.finishedDuration(3_600))
    }

    @Test
    fun `volume uses a thousands separator`() {
        assertEquals("2,593 kg", WorkoutFormat.volume(2593.0))
        assertEquals("436 kg", WorkoutFormat.volume(436.0))
        assertEquals("12.5 kg", WorkoutFormat.volume(12.5))
    }

    @Test
    fun `weights drop trailing zeros`() {
        assertEquals("20", WorkoutFormat.weight(20.0))
        assertEquals("22.5", WorkoutFormat.weight(22.5))
        assertEquals("1.25", WorkoutFormat.weight(1.25))
        assertEquals("20kg x 15", WorkoutFormat.weightReps(20.0, 15))
    }

    @Test
    fun `rest labels match the picker`() {
        assertEquals("OFF", WorkoutFormat.rest(null))
        assertEquals("OFF", WorkoutFormat.rest(0))
        assertEquals("45s", WorkoutFormat.rest(45))
        assertEquals("2min 30s", WorkoutFormat.rest(150))
        assertEquals("3min 0s", WorkoutFormat.rest(180))
        assertEquals("Rest: 2min 30s", WorkoutFormat.restChip(150))
    }

    @Test
    fun `clock and cardio text`() {
        assertEquals("02:21", WorkoutFormat.clock(141))
        assertEquals("1:02:21", WorkoutFormat.clock(3_741))
        assertEquals("0 km in 05:00", WorkoutFormat.distanceTime(0.0, 300))
    }

    @Test
    fun `ordinals`() {
        assertEquals(listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "14th", "21st", "22nd", "111th"),
            listOf(1, 2, 3, 4, 11, 12, 13, 14, 21, 22, 111).map(WorkoutFormat::ordinal))
    }

    @Test
    fun `auto title by start hour`() {
        assertEquals("Morning workout 💪", WorkoutFormat.autoTitle(at(2026, 10, 19, 7), zone))
        assertEquals("Afternoon workout 💪", WorkoutFormat.autoTitle(at(2026, 10, 19, 15, 38), zone))
        assertEquals("Evening workout 💪", WorkoutFormat.autoTitle(at(2026, 10, 19, 20, 59), zone))
        assertEquals("Night workout 💪", WorkoutFormat.autoTitle(at(2026, 10, 19, 23), zone))
        assertEquals("Night workout 💪", WorkoutFormat.autoTitle(at(2026, 10, 19, 4), zone))
    }

    @Test
    fun `relative times`() {
        val now = at(2026, 10, 19, 16, 0)
        assertEquals("just now", WorkoutFormat.relative(now.minusSeconds(20), now, zone))
        assertEquals("22 minutes ago", WorkoutFormat.relative(now.minusSeconds(22 * 60), now, zone))
        assertEquals("3 hours ago", WorkoutFormat.relative(now.minusSeconds(3 * 3600), now, zone))
        assertEquals("Yesterday", WorkoutFormat.relative(at(2026, 10, 18, 9), now, zone))
        assertEquals("Oct 15", WorkoutFormat.relative(at(2026, 10, 15, 15), now, zone))
        assertEquals("Dec 31, 2025", WorkoutFormat.relative(at(2025, 12, 31, 15), now, zone))
    }

    @Test
    fun `date formats`() {
        assertEquals("15 Oct 2026, 3:00 PM", WorkoutFormat.dateTime(at(2026, 10, 15, 15), zone))
        assertEquals("Thursday, Oct 15, 2026", WorkoutFormat.longDate(at(2026, 10, 15, 15), zone))
        assertEquals("15 Oct 2026, 15:00", WorkoutFormat.historyDate(at(2026, 10, 15, 15), zone))
        assertEquals("3 sets Incline Bench Press", WorkoutFormat.setsOf(3, "Incline Bench Press"))
        assertEquals("1 set Treadmill", WorkoutFormat.setsOf(1, "Treadmill"))
    }
}
