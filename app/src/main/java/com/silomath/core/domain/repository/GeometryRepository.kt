package com.silomath.core.domain.repository

import com.silomath.core.database.entity.GeometryFamilyEntity
import com.silomath.core.database.entity.GeometryProfileEntity
import com.silomath.core.database.entity.GeometryVersionEntity
import kotlinx.coroutines.flow.Flow

interface GeometryRepository {
    fun getFamilies(): Flow<List<GeometryFamilyEntity>>
    fun getProfiles(): Flow<List<GeometryProfileEntity>>
    fun getVersions(profileId: Long): Flow<List<GeometryVersionEntity>>
    suspend fun saveFamily(family: GeometryFamilyEntity)
    suspend fun saveProfile(profile: GeometryProfileEntity)
    suspend fun createNewVersion(version: GeometryVersionEntity)
}
