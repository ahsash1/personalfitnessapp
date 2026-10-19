package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.local.dao.BodyMetricDao
import com.example.personalfitnessapp.data.local.dao.UserProfileDao
import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface BodyMetricRepository {
    fun observeAll(): Flow<List<BodyMetricEntity>>
    suspend fun add(metric: BodyMetricEntity): Long
    suspend fun update(metric: BodyMetricEntity)
    suspend fun delete(metric: BodyMetricEntity)
}

/** Keeps the profile's weight equal to the newest logged bodyweight, so every calculation agrees. */
class BodyMetricRepositoryImpl @Inject constructor(
    private val dao: BodyMetricDao,
    private val profiles: UserProfileDao,
) : BodyMetricRepository {
    override fun observeAll(): Flow<List<BodyMetricEntity>> = dao.observeAll()
    override suspend fun add(metric: BodyMetricEntity): Long = dao.insert(metric).also { syncProfileWeight() }
    override suspend fun update(metric: BodyMetricEntity) { dao.update(metric); syncProfileWeight() }
    override suspend fun delete(metric: BodyMetricEntity) { dao.delete(metric); syncProfileWeight() }

    private suspend fun syncProfileWeight() {
        val latest = dao.latestWeight()?.weightKg ?: return
        val profile = profiles.get() ?: return
        if (profile.weightKg != latest) profiles.upsert(profile.copy(weightKg = latest))
    }
}
