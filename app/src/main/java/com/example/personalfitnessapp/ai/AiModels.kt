package com.example.personalfitnessapp.ai

import com.example.personalfitnessapp.data.model.Muscle
import kotlinx.serialization.json.JsonObject

sealed interface AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>
    /** [retryable] distinguishes "try again" (network/rate-limit) from "won't work" (bad request). */
    data class Failure(val message: String, val retryable: Boolean) : AiResult<Nothing>
}

data class MachineIdentification(
    val machineName: String,
    val equipmentType: String,
    val plausibleExercises: List<String>,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle>,
    val movementPattern: String,
    val setupSteps: List<String>,
    val formCues: List<String>,
    val commonMistakes: List<String>,
    val confidence: Float,
)

data class FormGuideResult(
    val formCues: List<String>,
    val commonMistakes: List<String>,
)

/** Provider-neutral tool description; [parameters] is a JSON-schema object. */
data class ToolDeclaration(val name: String, val description: String, val parameters: JsonObject)

data class ToolCall(val id: String, val name: String, val arguments: JsonObject)

data class ToolResult(val callId: String, val name: String, val content: String)

/** One food from an AI meal estimate; macros are for the [grams] eaten. */
/** A logged food after an AI edit: [id] is the existing entry it replaces, 0 for a new one. */
data class FoodRevision(val id: Long, val food: FoodEstimate)

data class FoodEstimate(
    val name: String,
    val grams: Float?,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
)
