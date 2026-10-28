package com.silomath.core.domain.repository

import com.silomath.core.domain.model.StorageSpace
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.StorageUnitId
import kotlinx.coroutines.flow.Flow

/** Read-only prerequisite for STU-02, not the complete MD-005 repository/API.
 * Success(emptyList()) certifies an authorized existing canonical parent with no children.
 * Failure means unavailable/unauthorized/invalid context, never a verified empty collection.
 */
interface StorageSpaceReader {
    fun observe(organizationId:OrganizationId,locationId:LocationId,storageUnitId:StorageUnitId):Flow<Result<List<StorageSpace>>>
}
