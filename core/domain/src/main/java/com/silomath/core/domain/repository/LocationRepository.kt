package com.silomath.core.domain.repository

import com.silomath.core.domain.model.Location
import com.silomath.core.model.OrganizationId
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocations(
        organizationId: OrganizationId
    ): Flow<List<Location>>

    /** Local historical-reference guard; failures must not unlock business Code editing. */
    suspend fun hasHistoricalReferences(organizationId: OrganizationId, locationId: com.silomath.core.model.LocationId): Result<Boolean>

    suspend fun saveLocation(
        location: Location
    ): Result<Unit>
}
