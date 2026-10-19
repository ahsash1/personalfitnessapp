package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.local.dao.BodyMetricDao
import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface BodyMetricRepository {
    fun observeAll(): Flow<List<BodyMetricEntity>>
    suspend fun add(metric: BodyMetricEntity): Long
    suspend fun update(metric: BodyMetricEntity)
    suspend fun delete(metric: BodyMetricEntity)
}

class BodyMetricRepositoryImpl @Inject constructor(
    private val dao: BodyMetricDao,
) : BodyMetricRepository {
    override fun observeAll(): Flow<List<BodyMetricEntity>> = dao.observeAll()
    override suspend fun add(metric: BodyMetricEntity): Long = dao.insert(metric)
    override suspend fun update(metric: BodyMetricEntity) = dao.update(metric)
    override suspend fun delete(metric: BodyMetricEntity) = dao.delete(metric)
}
