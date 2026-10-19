package com.example.personalfitnessapp.ai

import com.example.personalfitnessapp.ai.backend.AiProvider
import com.example.personalfitnessapp.ai.backend.ModelRoute

/**
 * The only place model names and endpoints live.
 *
 * Checked against the live docs on 2026-10-19:
 * - ai.google.dev/gemini-api/docs/models: Gemini 2.5 models are now "limited to users who
 *   have actively used them in the past" and new projects are pointed at 3.5 Flash-Lite
 *   or 3.8 Flash. The old fallback (gemini-2.5-flash) therefore fails for a new key, which
 *   is why the coach kept reporting "busy": 3.8 Flash returned 503 and the fallback had
 *   nothing to fall back to.
 * - Free-tier projects regularly get 503 "high demand" from gemini-3.8-flash on the
 *   Interactions API (Google AI developer forum, Oct 2026), so it must not be the only model.
 * - console.groq.com/docs: free plan (no card) serves openai/gpt-oss-120b, openai/gpt-oss-20b
 *   and qwen/qwen3.8-27b, all with tool calling, at 30 RPM / 1K RPD / 8K TPM each.
 *
 * Quotas are per model, so walking a chain of models multiplies the free capacity.
 */
object AiModelConfig {
    const val GEMINI_INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
    const val GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"

    /**
     * Tried in order for every request, skipping models in cooldown and putting the last
     * model that answered first. Providers alternate early so one provider's outage costs
     * at most two attempts before the other is tried.
     */
    val CHAIN: List<ModelRoute> = listOf(
        ModelRoute(AiProvider.GEMINI, "gemini-3.8-flash"),
        ModelRoute(AiProvider.GEMINI, "gemini-3.5-flash-lite"),
        ModelRoute(AiProvider.GROQ, "openai/gpt-oss-120b"),
        ModelRoute(AiProvider.GEMINI, "gemini-3.6-flash"),
        ModelRoute(AiProvider.GEMINI, "gemini-3.1-flash-lite"),
        ModelRoute(AiProvider.GROQ, "qwen/qwen3.8-27b"),
        ModelRoute(AiProvider.GROQ, "openai/gpt-oss-20b"),
    )

    const val CONNECT_TIMEOUT_SECONDS = 10L

    /** Per HTTP call. Free-tier 503s come back in 5-8s; a hang this long means try elsewhere. */
    const val CALL_TIMEOUT_SECONDS = 35L

    /** After a full pass of transient failures, wait this long and try once more. */
    const val SECOND_PASS_DELAY_MILLIS = 2_000L

    /** No second pass if the first one already took this long; the user has waited enough. */
    const val SECOND_PASS_BUDGET_MILLIS = 45_000L

    const val COOLDOWN_OVERLOADED_MILLIS = 90_000L
    const val COOLDOWN_RATE_LIMITED_MILLIS = 60_000L
    const val COOLDOWN_DAILY_QUOTA_MILLIS = 60 * 60_000L
    const val COOLDOWN_UNAVAILABLE_MILLIS = 6 * 60 * 60_000L
    const val COOLDOWN_BAD_REQUEST_MILLIS = 10 * 60_000L
    const val COOLDOWN_BAD_RESPONSE_MILLIS = 30_000L
    const val COOLDOWN_AUTH_MILLIS = 10 * 60_000L

    /** Groq answers 429 with retry-after; waiting a few seconds beats switching models. */
    const val MAX_INLINE_RETRY_WAIT_MILLIS = 6_000L

    const val MAX_TOOL_ROUNDS = 5
}
