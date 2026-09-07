package com.silomath.core.domain.repository

import com.silomath.core.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getLocations(organizationId: Long): Flow<List<LocationEntity>>
    suspend fun saveLocation(location: LocationEntity)
}
