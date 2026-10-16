package com.silomath.core.domain.repository

import com.silomath.core.domain.model.Location
import com.silomath.core.model.OrganizationId
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocations(
        organizationId: OrganizationId
    ): Flow<List<Location>>

    suspend fun saveLocation(
        location: Location
    ): Result<Unit>
}
