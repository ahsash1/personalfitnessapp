package com.example.personalfitnessapp.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The Interactions API describes a turn as a list of `steps`, but earlier responses
 * also carried a flattened `output_text`. Reading both keeps us working if the
 * server drops one of them.
 */
internal object GeminiResponseParser {
    fun outputText(response: JsonObject): String? {
        (response["output_text"] as? JsonPrimitive)?.content?.let { return it }
        val parts = steps(response)
            .filter { it.string("type") == "model_output" }
            .flatMap { (it["content"] as? JsonArray).orEmpty() }
            .mapNotNull { (it as? JsonObject) }
            .filter { it.string("type") == "text" }
            .map { it.string("text") }
        return parts.takeIf { it.isNotEmpty() }?.joinToString("")
    }

    fun toolCalls(response: JsonObject): List<ToolCall> = steps(response)
        .filter { it.string("type") == "function_call" }
        .map { step ->
            val name = step.string("name")
            ToolCall(
                // The id is what a function_result must echo back; fall back to the name
                // rather than dropping a call the model clearly made.
                id = step.string("id").ifEmpty { name },
                name = name,
                arguments = arguments(step["arguments"]),
            )
        }

    private fun steps(response: JsonObject): List<JsonObject> =
        (response["steps"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

    private fun arguments(element: JsonElement?): JsonObject = when (element) {
        is JsonObject -> element
        is JsonPrimitive -> runCatching { Json.parseToJsonElement(element.content) as? JsonObject }.getOrNull()
        else -> null
    } ?: JsonObject(emptyMap())

    private fun JsonObject.string(key: String): String = (this[key] as? JsonPrimitive)?.content.orEmpty()
}
