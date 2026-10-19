package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.local.dao.RoutineDao
import com.example.personalfitnessapp.data.local.dao.RoutineExerciseDao
import com.example.personalfitnessapp.data.local.dao.RoutineScheduleDao
import com.example.personalfitnessapp.data.local.entity.DayOfWeek
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.local.entity.RoutineExerciseEntity
import com.example.personalfitnessapp.data.local.entity.RoutineScheduleEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeRoutines(): Flow<List<RoutineEntity>>
    fun observeRoutine(routineId: Long): Flow<RoutineEntity?>
    suspend fun saveRoutine(routine: RoutineEntity): Long
    suspend fun deleteRoutine(routine: RoutineEntity)

    fun observeRoutineExercises(routineId: Long): Flow<List<RoutineExerciseEntity>>
    suspend fun addRoutineExercise(routineExercise: RoutineExerciseEntity): Long
    suspend fun removeRoutineExercise(routineExercise: RoutineExerciseEntity)

    fun observeSchedule(): Flow<List<RoutineScheduleEntity>>
    suspend fun setScheduleForDay(day: DayOfWeek, routineId: Long?)
    suspend fun getScheduleForDay(day: DayOfWeek): RoutineScheduleEntity?
}

class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val routineScheduleDao: RoutineScheduleDao,
) : RoutineRepository {
    override fun observeRoutines(): Flow<List<RoutineEntity>> = routineDao.observeAll()

    override fun observeRoutine(routineId: Long): Flow<RoutineEntity?> = routineDao.observeById(routineId)

    override suspend fun saveRoutine(routine: RoutineEntity): Long =
        if (routine.id == 0L) routineDao.insert(routine) else {
            routineDao.update(routine)
            routine.id
        }

    override suspend fun deleteRoutine(routine: RoutineEntity) = routineDao.delete(routine)

    override fun observeRoutineExercises(routineId: Long): Flow<List<RoutineExerciseEntity>> =
        routineExerciseDao.observeForRoutine(routineId)

    override suspend fun addRoutineExercise(routineExercise: RoutineExerciseEntity): Long =
        routineExerciseDao.upsert(routineExercise)

    override suspend fun removeRoutineExercise(routineExercise: RoutineExerciseEntity) =
        routineExerciseDao.delete(routineExercise)

    override fun observeSchedule(): Flow<List<RoutineScheduleEntity>> = routineScheduleDao.observeAll()

    override suspend fun setScheduleForDay(day: DayOfWeek, routineId: Long?) =
        routineScheduleDao.upsert(RoutineScheduleEntity(day, routineId))

    override suspend fun getScheduleForDay(day: DayOfWeek): RoutineScheduleEntity? = routineScheduleDao.get(day)
}
