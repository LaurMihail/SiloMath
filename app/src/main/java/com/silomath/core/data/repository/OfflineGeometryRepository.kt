package com.silomath.core.data.repository

import com.silomath.core.database.dao.GeometryDao
import com.silomath.core.database.entity.GeometryFamilyEntity
import com.silomath.core.database.entity.GeometryProfileEntity
import com.silomath.core.database.entity.GeometryVersionEntity
import com.silomath.core.domain.repository.GeometryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineGeometryRepository @Inject constructor(
    private val geometryDao: GeometryDao
) : GeometryRepository {
    override fun getFamilies(): Flow<List<GeometryFamilyEntity>> = geometryDao.getAllFamilies()

    override fun getProfiles(): Flow<List<GeometryProfileEntity>> = geometryDao.getAllProfiles()

    override fun getVersions(profileId: Long): Flow<List<GeometryVersionEntity>> = 
        geometryDao.getVersionsByProfile(profileId)

    override suspend fun saveFamily(family: GeometryFamilyEntity) {
        geometryDao.insertFamily(family)
    }

    override suspend fun saveProfile(profile: GeometryProfileEntity) {
        geometryDao.insertProfile(profile)
    }

    override suspend fun createNewVersion(version: GeometryVersionEntity) {
        val maxVersion = geometryDao.getMaxVersionNumber(version.geometryProfileId) ?: 0
        require(version.versionNumber > maxVersion) { 
            "Version number must be greater than current max version ($maxVersion)" 
        }
        geometryDao.insertVersion(version)
    }
}
