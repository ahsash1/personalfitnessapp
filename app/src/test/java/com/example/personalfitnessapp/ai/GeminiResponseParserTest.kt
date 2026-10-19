package com.example.personalfitnessapp.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiResponseParserTest {
    private fun parse(json: String) = Json.parseToJsonElement(json) as JsonObject

    @Test
    fun `reads text from model_output steps`() {
        val response = parse(
            """{"steps":[{"type":"model_output","content":[{"type":"text","text":"Hello "},{"type":"text","text":"there"}]}]}"""
        )

        assertEquals("Hello there", GeminiResponseParser.outputText(response))
    }

    @Test
    fun `prefers a flattened output_text when present`() {
        assertEquals("flat", GeminiResponseParser.outputText(parse("""{"output_text":"flat","steps":[]}""")))
    }

    @Test
    fun `returns null when there is no text`() {
        assertNull(GeminiResponseParser.outputText(parse("""{"steps":[{"type":"function_call","name":"x","id":"1"}]}""")))
    }

    @Test
    fun `reads function calls with their arguments`() {
        val response = parse(
            """{"steps":[{"type":"function_call","id":"abc","name":"search_exercises","arguments":{"muscle":"CHEST"}}]}"""
        )

        val call = GeminiResponseParser.toolCalls(response).single()

        assertEquals("abc", call.id)
        assertEquals("search_exercises", call.name)
        assertEquals("\"CHEST\"", call.arguments["muscle"].toString())
    }

    @Test
    fun `tolerates missing ids and stringified arguments`() {
        val response = parse("""{"steps":[{"type":"function_call","name":"get_current_plan","arguments":"{\"a\":1}"}]}""")

        val call = GeminiResponseParser.toolCalls(response).single()

        assertEquals("get_current_plan", call.id)
        assertEquals("1", call.arguments["a"].toString())
    }
}
