package com.example.personalfitnessapp.di

import android.content.Context
import androidx.room.Room
import com.example.personalfitnessapp.data.local.AppDatabase
import com.example.personalfitnessapp.data.local.Migrations
import com.example.personalfitnessapp.data.local.dao.ActiveRestTimerDao
import com.example.personalfitnessapp.data.local.dao.AppMetaDao
import com.example.personalfitnessapp.data.local.dao.BodyMetricDao
import com.example.personalfitnessapp.data.local.dao.ChatMessageDao
import com.example.personalfitnessapp.data.local.dao.EquipmentIncrementDao
import com.example.personalfitnessapp.data.local.dao.ExerciseDao
import com.example.personalfitnessapp.data.local.dao.RoutineDao
import com.example.personalfitnessapp.data.local.dao.RoutineExerciseDao
import com.example.personalfitnessapp.data.local.dao.RoutineScheduleDao
import com.example.personalfitnessapp.data.local.dao.SessionExerciseDao
import com.example.personalfitnessapp.data.local.dao.SetLogDao
import com.example.personalfitnessapp.data.local.dao.UserProfileDao
import com.example.personalfitnessapp.data.local.dao.WorkoutLogDao
import com.example.personalfitnessapp.data.local.dao.WorkoutSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // No destructive fallback: the database now holds real workout history, so a
            // schema change without a Migration must fail loudly instead of wiping it.
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideEquipmentIncrementDao(db: AppDatabase): EquipmentIncrementDao =
        db.equipmentIncrementDao()

    @Provides
    fun provideBodyMetricDao(db: AppDatabase): BodyMetricDao = db.bodyMetricDao()

    @Provides
    fun provideRoutineDao(db: AppDatabase): RoutineDao = db.routineDao()

    @Provides
    fun provideRoutineExerciseDao(db: AppDatabase): RoutineExerciseDao = db.routineExerciseDao()

    @Provides
    fun provideRoutineScheduleDao(db: AppDatabase): RoutineScheduleDao = db.routineScheduleDao()

    @Provides
    fun provideWorkoutSessionDao(db: AppDatabase): WorkoutSessionDao = db.workoutSessionDao()

    @Provides
    fun provideSetLogDao(db: AppDatabase): SetLogDao = db.setLogDao()

    @Provides
    fun provideAppMetaDao(db: AppDatabase): AppMetaDao = db.appMetaDao()

    @Provides
    fun provideActiveRestTimerDao(db: AppDatabase): ActiveRestTimerDao = db.activeRestTimerDao()

    @Provides
    fun provideSessionExerciseDao(db: AppDatabase): SessionExerciseDao = db.sessionExerciseDao()

    @Provides
    fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()

    @Provides
    fun provideFoodDao(db: AppDatabase): com.example.personalfitnessapp.data.local.dao.FoodDao = db.foodDao()

    @Provides
    fun provideWorkoutLogDao(db: AppDatabase): WorkoutLogDao = db.workoutLogDao()
}
