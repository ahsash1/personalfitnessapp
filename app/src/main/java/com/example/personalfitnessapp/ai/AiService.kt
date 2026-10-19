package com.example.personalfitnessapp.ai

import com.example.personalfitnessapp.data.model.Muscle

/**
 * Single entry point for all AI calls in the app. Every feature that needs AI
 * (form guide generation, photo -> exercise identification, coach chat) talks to
 * this interface only, never to a provider SDK directly, so swapping providers
 * stays a one-file change.
 */
interface AiService {
    /** [imageBytes] should already be downscaled/compressed. */
    suspend fun identifyMachine(imageBytes: ByteArray): AiResult<MachineIdentification>

    suspend fun generateFormGuide(
        exerciseName: String,
        primaryMuscles: List<Muscle>,
        equipment: String,
    ): AiResult<FormGuideResult>

    /**
     * One model round of the coach chat. The app, not the provider, runs tools: a response
     * with [ChatResponse.toolCalls] is answered by another call with [ChatInput.ToolResults].
     */
    suspend fun chat(
        systemInstruction: String,
        tools: List<ToolDeclaration>,
        input: ChatInput,
    ): AiResult<ChatResponse>
}
