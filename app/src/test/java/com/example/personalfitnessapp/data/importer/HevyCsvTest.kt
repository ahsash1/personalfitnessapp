package com.example.personalfitnessapp.data.importer

import com.example.personalfitnessapp.data.local.entity.SetType
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HevyCsvTest {
    private val header = "\"title\",\"start_time\",\"end_time\",\"description\",\"exercise_title\",\"superset_id\"," +
        "\"exercise_notes\",\"set_index\",\"set_type\",\"weight_kg\",\"reps\",\"distance_km\",\"duration_seconds\",\"rpe\""

    @Test
    fun `groups rows into workouts and exercises`() {
        val csv = listOf(
            header,
            "\"Push, heavy\",\"19 Oct 2026, 15:38\",\"19 Oct 2026, 16:30\",\"felt \"\"good\"\"\",\"Bench Press (Barbell)\",,\"pause reps\",0,\"warmup\",40,10,,,",
            "\"Push, heavy\",\"19 Oct 2026, 15:38\",\"19 Oct 2026, 16:30\",\"felt \"\"good\"\"\",\"Bench Press (Barbell)\",,\"pause reps\",1,\"normal\",80,5,,,8",
            "\"Push, heavy\",\"19 Oct 2026, 15:38\",\"19 Oct 2026, 16:30\",\"felt \"\"good\"\"\",\"Treadmill\",,,0,\"normal\",,,1.2,600,",
            "\"Legs\",\"20 Oct 2026, 09:00\",\"20 Oct 2026, 10:00\",,\"Squat (Barbell)\",,,0,\"dropset\",100,5,,,",
        ).joinToString("\n")
        val workouts = HevyCsv.parse(csv, ZoneOffset.UTC)
        assertEquals(2, workouts.size)
        val push = workouts[0]
        assertEquals("Push, heavy", push.title)
        assertEquals("felt \"good\"", push.description)
        assertEquals(52 * 60_000L, push.endedAt - push.startedAt)
        assertEquals(listOf("Bench Press (Barbell)", "Treadmill"), push.exercises.map { it.name })
        assertEquals(SetType.WARMUP, push.exercises[0].sets[0].type)
        assertEquals(80.0, push.exercises[0].sets[1].weightKg!!, 1e-9)
        assertEquals(8.0, push.exercises[0].sets[1].rpe!!, 1e-9)
        assertEquals(600, push.exercises[1].sets[0].durationSec)
        assertNull(push.exercises[1].sets[0].weightKg)
        assertEquals(SetType.DROP, workouts[1].exercises[0].sets[0].type)
    }

    @Test
    fun `pounds are converted to kilograms`() {
        val csv = "title,start_time,end_time,exercise_title,set_index,set_type,weight_lbs,reps\n" +
            "A,\"Oct 19, 2026, 3:38 PM\",\"Oct 19, 2026, 4:00 PM\",Curl,0,normal,100,10"
        val set = HevyCsv.parse(csv, ZoneOffset.UTC).single().exercises.single().sets.single()
        assertEquals(45.359237, set.weightKg!!, 1e-6)
    }
}
