package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.local.dao.EquipmentIncrementDao
import com.example.personalfitnessapp.data.local.dao.UserProfileDao
import com.example.personalfitnessapp.data.local.entity.EquipmentIncrementEntity
import com.example.personalfitnessapp.data.local.entity.UserProfileEntity
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
) : UserProfileRepository {
    override fun observeProfile(): Flow<UserProfileEntity?> = userProfileDao.observe()

    override suspend fun getProfile(): UserProfileEntity? = userProfileDao.get()

    override suspend fun saveProfile(profile: UserProfileEntity) = userProfileDao.upsert(profile)

    override fun observeEquipmentIncrements(): Flow<List<EquipmentIncrementEntity>> =
        equipmentIncrementDao.observeAll()

    override suspend fun saveEquipmentIncrement(increment: EquipmentIncrementEntity) =
        equipmentIncrementDao.upsert(increment)

    override suspend fun getEquipmentIncrement(equipmentType: String): EquipmentIncrementEntity? =
        equipmentIncrementDao.get(equipmentType)
}
