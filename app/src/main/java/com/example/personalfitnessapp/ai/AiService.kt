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
    /**
     * Splits a meal description and/or photo into foods with estimated portions, calories and
     * macros. Estimates, not measurements: home-cooked oil and portion size are the big unknowns.
     */
    suspend fun estimateFood(description: String?, imageBytes: ByteArray?): AiResult<List<FoodEstimate>> =
        AiResult.Failure("Food estimates aren't available.", retryable = false)

    /**
     * Applies a plain-language correction ("I only ate half", "no butter") to an already logged meal
     * and returns the whole meal afterwards: same ids for kept items, 0 for new ones; missing = removed.
     */
    suspend fun reviseFood(items: List<FoodRevision>, instruction: String): AiResult<List<FoodRevision>> =
        AiResult.Failure("Meal edits aren't available.", retryable = false)

    suspend fun chat(
        systemInstruction: String,
        tools: List<ToolDeclaration>,
        userText: String,
        runTool: suspend (ToolCall) -> String,
    ): AiResult<String>
}
