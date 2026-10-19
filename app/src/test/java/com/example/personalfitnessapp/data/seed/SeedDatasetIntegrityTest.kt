package com.example.personalfitnessapp.data.seed

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the real bundled dataset through the seed mapper. This is the
 * regression net for the dataset mapping: if a future
 * dataset update introduces a muscle/level name our mappers don't know about,
 * this test fails loudly instead of the seeder silently dropping or crashing
 * on first app launch.
 */
class SeedDatasetIntegrityTest {
    private val seedFile = File("src/main/assets/seed/free_exercise_db_v1.json")
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `every bundled exercise maps without throwing`() {
        assertTrue("seed asset not found at ${seedFile.absolutePath}", seedFile.exists())
        val raw = json.decodeFromString<List<RawExerciseDto>>(seedFile.readText())

        assertTrue("expected a non-trivial exercise count", raw.size > 500)

        val failures = mutableListOf<String>()
        raw.forEach { dto ->
            try {
                ExerciseSeedMapper.toEntity(dto)
            } catch (e: Exception) {
                failures += "${dto.id}: ${e.message}"
            }
        }
        assertTrue("Mapping failures:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun `ids are unique`() {
        val raw = json.decodeFromString<List<RawExerciseDto>>(seedFile.readText())
        assertEquals(raw.size, raw.map { it.id }.toSet().size)
    }
}
