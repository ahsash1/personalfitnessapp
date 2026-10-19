package com.example.personalfitnessapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.example.personalfitnessapp.data.local.dao.WorkoutSessionDao
import com.example.personalfitnessapp.data.local.entity.ActiveRestTimerEntity
import com.example.personalfitnessapp.data.local.entity.AppMetaEntity
import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity
import com.example.personalfitnessapp.data.local.entity.ChatMessageEntity
import com.example.personalfitnessapp.data.local.entity.EquipmentIncrementEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.local.entity.RoutineExerciseEntity
import com.example.personalfitnessapp.data.local.entity.RoutineScheduleEntity
import com.example.personalfitnessapp.data.local.entity.SessionExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.UserProfileEntity
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        UserProfileEntity::class,
        EquipmentIncrementEntity::class,
        BodyMetricEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        RoutineScheduleEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        AppMetaEntity::class,
        ActiveRestTimerEntity::class,
        SessionExerciseEntity::class,
        ChatMessageEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun equipmentIncrementDao(): EquipmentIncrementDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao
    abstract fun routineScheduleDao(): RoutineScheduleDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun appMetaDao(): AppMetaDao
    abstract fun activeRestTimerDao(): ActiveRestTimerDao
    abstract fun sessionExerciseDao(): SessionExerciseDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val DATABASE_NAME = "personal_fitness_app.db"
    }
}
