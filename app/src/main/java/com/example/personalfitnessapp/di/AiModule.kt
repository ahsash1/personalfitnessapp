package com.example.personalfitnessapp.di

import com.example.personalfitnessapp.ai.AiService
import com.example.personalfitnessapp.ai.coach.CoachToolExecutor
import com.example.personalfitnessapp.ai.coach.CoachTools
import com.example.personalfitnessapp.ai.GeminiAiService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindAiService(impl: GeminiAiService): AiService

    @Binds
    abstract fun bindCoachToolExecutor(impl: CoachTools): CoachToolExecutor
}
