package com.example.personalfitnessapp.di

import com.example.personalfitnessapp.data.repository.ChatRepository
import com.example.personalfitnessapp.data.repository.ChatRepositoryImpl
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.ExerciseRepositoryImpl
import com.example.personalfitnessapp.data.repository.BodyMetricRepository
import com.example.personalfitnessapp.data.repository.BodyMetricRepositoryImpl
import com.example.personalfitnessapp.data.repository.RestTimerRepository
import com.example.personalfitnessapp.data.repository.RestTimerRepositoryImpl
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.data.repository.RoutineRepositoryImpl
import com.example.personalfitnessapp.data.repository.UserProfileRepository
import com.example.personalfitnessapp.data.repository.UserProfileRepositoryImpl
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutSessionRepository(impl: WorkoutSessionRepositoryImpl): WorkoutSessionRepository

    @Binds
    @Singleton
    abstract fun bindRestTimerRepository(impl: RestTimerRepositoryImpl): RestTimerRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindBodyMetricRepository(impl: BodyMetricRepositoryImpl): BodyMetricRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
