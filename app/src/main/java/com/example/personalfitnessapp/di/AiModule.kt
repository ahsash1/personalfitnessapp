package com.example.personalfitnessapp.di

import com.example.personalfitnessapp.BuildConfig
import com.example.personalfitnessapp.ai.AiBackends
import com.example.personalfitnessapp.ai.AiModelConfig
import com.example.personalfitnessapp.ai.AiService
import com.example.personalfitnessapp.ai.ResilientAiService
import com.example.personalfitnessapp.ai.Sleeper
import com.example.personalfitnessapp.ai.backend.AiHttp
import com.example.personalfitnessapp.ai.backend.AiProvider
import com.example.personalfitnessapp.ai.backend.GeminiBackend
import com.example.personalfitnessapp.ai.backend.OpenAiChatBackend
import com.example.personalfitnessapp.ai.coach.CoachToolExecutor
import com.example.personalfitnessapp.ai.coach.CoachTools
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindAiService(impl: ResilientAiService): AiService

    @Binds
    abstract fun bindCoachToolExecutor(impl: CoachTools): CoachToolExecutor

    companion object {
        /** A provider without a key in local.properties is simply left out of the chain. */
        @Provides
        @Singleton
        fun provideBackends(http: AiHttp): AiBackends = AiBackends(
            AiModelConfig.CHAIN.mapNotNull { route ->
                when (route.provider) {
                    AiProvider.GEMINI -> BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
                        ?.let { GeminiBackend(route.model, it, http) }
                    AiProvider.GROQ -> BuildConfig.GROQ_API_KEY.takeIf { it.isNotBlank() }
                        ?.let { OpenAiChatBackend(route, it, http) }
                    AiProvider.OPENROUTER -> BuildConfig.OPENROUTER_API_KEY.takeIf { it.isNotBlank() }
                        ?.let { OpenAiChatBackend(route, it, http) }
                }
            }
        )

        @Provides
        fun provideSleeper(): Sleeper = Sleeper { delay(it) }
    }
}
