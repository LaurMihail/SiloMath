package com.silomath.core.data.repository

import com.silomath.core.database.dao.LocationDao
import com.silomath.core.database.entity.LocationEntity
import com.silomath.core.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineLocationRepository @Inject constructor(
    private val locationDao: LocationDao
) : LocationRepository {
    override fun getLocations(organizationId: Long): Flow<List<LocationEntity>> = 
        locationDao.getLocationsByOrganization(organizationId)

    override suspend fun saveLocation(location: LocationEntity) {
        locationDao.insertLocation(location)
    }
}
