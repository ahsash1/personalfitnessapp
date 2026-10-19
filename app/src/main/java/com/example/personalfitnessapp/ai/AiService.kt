package com.example.personalfitnessapp.ai

import com.example.personalfitnessapp.data.model.Muscle

/**
 * Single entry point for all AI calls in the app. Every feature that needs AI
 * (form guide generation, photo -> exercise identification, coach chat) talks to
 * this interface only, never to a provider directly; which free model actually
 * answers is decided behind it.
 */
interface AiService {
    /** [imageBytes] should already be downscaled/compressed (~1024px long side, JPEG). */
    suspend fun identifyMachine(imageBytes: ByteArray): AiResult<MachineIdentification>

    suspend fun generateFormGuide(
        exerciseName: String,
        primaryMuscles: List<Muscle>,
        equipment: String,
    ): AiResult<FormGuideResult>

    /**
     * One coach exchange: [userText] in, final answer out. When the model asks for a tool,
     * [runTool] executes it and its returned text goes back to the model; this repeats until
     * the model answers in plain text. [runTool] may be called again for the same request if
     * a model fails part-way and the exchange is retried on another one, so tools must be
     * safe to repeat.
     */
    suspend fun chat(
        systemInstruction: String,
        tools: List<ToolDeclaration>,
        userText: String,
        runTool: suspend (ToolCall) -> String,
    ): AiResult<String>
}
