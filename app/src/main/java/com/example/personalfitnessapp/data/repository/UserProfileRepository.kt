package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.local.dao.BodyMetricDao
import com.example.personalfitnessapp.data.local.dao.EquipmentIncrementDao
import com.example.personalfitnessapp.data.local.dao.UserProfileDao
import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity
import com.example.personalfitnessapp.data.local.entity.EquipmentIncrementEntity
import com.example.personalfitnessapp.data.local.entity.UserProfileEntity
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeProfile(): Flow<UserProfileEntity?>
    suspend fun getProfile(): UserProfileEntity?
    suspend fun saveProfile(profile: UserProfileEntity)
    fun observeEquipmentIncrements(): Flow<List<EquipmentIncrementEntity>>
    suspend fun saveEquipmentIncrement(increment: EquipmentIncrementEntity)
    suspend fun getEquipmentIncrement(equipmentType: String): EquipmentIncrementEntity?
}

class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val equipmentIncrementDao: EquipmentIncrementDao,
    private val bodyMetrics: BodyMetricDao,
    private val clock: Clock,
) : UserProfileRepository {
    override fun observeProfile(): Flow<UserProfileEntity?> = userProfileDao.observe()

    override suspend fun getProfile(): UserProfileEntity? = userProfileDao.get()

    /**
     * A weight typed into the profile is also logged as today's bodyweight; otherwise an older
     * logged weight would keep winning and resting calories, protein and workout calories would
     * ignore the change.
     */
    override suspend fun saveProfile(profile: UserProfileEntity) {
        val latestLogged = bodyMetrics.latestWeight()?.weightKg
        userProfileDao.upsert(profile)
        if (latestLogged != null && latestLogged != profile.weightKg) {
            bodyMetrics.insert(
                BodyMetricEntity(
                    date = clock.millis(), weightKg = profile.weightKg, waistCm = null, chestCm = null, hipsCm = null,
                    leftArmCm = null, rightArmCm = null, leftThighCm = null, rightThighCm = null, neckCm = null,
                )
            )
        }
    }

    override fun observeEquipmentIncrements(): Flow<List<EquipmentIncrementEntity>> =
        equipmentIncrementDao.observeAll()

    override suspend fun saveEquipmentIncrement(increment: EquipmentIncrementEntity) =
        equipmentIncrementDao.upsert(increment)

    override suspend fun getEquipmentIncrement(equipmentType: String): EquipmentIncrementEntity? =
        equipmentIncrementDao.get(equipmentType)
}
