package com.example.personalfitnessapp.data.seed

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HevyCatalogTest {
    private val entries = HevyCatalog.parse(File("src/main/assets/seed/hevy_catalog.txt").readText())

    @Test
    fun `catalog parses and names are unique`() {
        assertTrue(entries.size > 200)
        assertTrue(entries.map { it.name.lowercase() }.toSet().size == entries.size)
    }

    @Test
    fun `every exercise from my Hevy export has a catalog name`() {
        val names = setOf(
            "Treadmill", "Incline Bench Press (Dumbbell)", "Decline Bench Press (Machine)", "Chest Fly (Machine)",
            "Bicep Curl (Machine)", "Seated Shoulder Press (Machine)", "Lateral Raise (Machine)", "Triceps Pushdown",
            "Rear Delt Reverse Fly (Machine)", "Iso-Lateral High Row (Machine)", "Pull Up (Assisted)", "Hammer Curl (Cable)",
            "Seated Cable Row - Bar Grip", "Chest Dip (Assisted)", "Cycling", "Triceps Extension (Machine)",
            "Triceps Extension (Cable)", "Recumbent Bike", "Leg Extension (Machine)", "Lying Leg Curl (Machine)",
            "Seated Calf Raise", "Squat (Barbell)", "Lat Pulldown (Cable)",
        )
        val catalog = entries.map { it.name }.toSet()
        assertTrue((names - catalog).toString(), catalog.containsAll(names))
    }
}
