package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.local.dao.ExerciseDao
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeAll(): Flow<List<ExerciseEntity>>
    fun observeById(id: String): Flow<ExerciseEntity?>
    suspend fun getById(id: String): ExerciseEntity?
    suspend fun getAll(): List<ExerciseEntity>
    suspend fun update(exercise: ExerciseEntity)
    suspend fun insert(exercise: ExerciseEntity)
}

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
) : ExerciseRepository {
    override fun observeAll(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    override fun observeById(id: String): Flow<ExerciseEntity?> = exerciseDao.observeById(id)

    override suspend fun getById(id: String): ExerciseEntity? = exerciseDao.getById(id)

    override suspend fun getAll(): List<ExerciseEntity> = exerciseDao.getAll()

    override suspend fun update(exercise: ExerciseEntity) = exerciseDao.update(exercise)

    override suspend fun insert(exercise: ExerciseEntity) = exerciseDao.upsert(exercise)
}
