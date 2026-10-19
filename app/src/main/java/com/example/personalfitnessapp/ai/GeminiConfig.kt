package com.example.personalfitnessapp.ai

/**
 * Single place for Gemini model/endpoint config, so model names are
 * never hardcoded in scattered places. Uses the current Interactions API
 * (generativelanguage.googleapis.com/v1beta/interactions), not the older
 * models/{model}:generateContent REST shape - verified against the live docs
 * in September 2026, since this is exactly the kind of API that changes often.
 */
object GeminiConfig {
    const val MODEL = "gemini-3.8-flash"

    /**
     * Used once, automatically, when [MODEL] comes back 5xx ("high demand") - an older,
     * more established free-tier model is less likely to be capacity-constrained at the
     * same moment as the newest one (amended 2026-09-30, confirmed against live 503s).
     */
    const val FALLBACK_MODEL = "gemini-2.5-flash"
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
    const val REQUEST_TIMEOUT_SECONDS = 30L
}
