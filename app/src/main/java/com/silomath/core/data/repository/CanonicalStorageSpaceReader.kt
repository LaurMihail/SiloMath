package com.silomath.core.data.repository

import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.entity.canonical.V6StorageSpacesRow
import com.silomath.core.domain.model.MasterStatus
import com.silomath.core.domain.model.StorageSpace
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.domain.repository.StorageSpaceReader
import com.silomath.core.model.*
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/** Approved dependency correction only: no write/API/legacy projection path. */
@OptIn(ExperimentalCoroutinesApi::class)
class CanonicalStorageSpaceReader @Inject constructor(private val database:SiloMathDatabase,private val auth:AuthRepository):StorageSpaceReader {
    override fun observe(organizationId:OrganizationId,locationId:LocationId,storageUnitId:StorageUnitId):Flow<Result<List<StorageSpace>>> =
        auth.sessionState.flatMapLatest { session ->
            if(!session.isLoggedIn || session.activeOrganizationId!=organizationId.value || session.authorizedOrganizations.none {
                    it.organizationId==organizationId.value && it.role in setOf("ADMIN","STANDARD")})
                flowOf(Result.failure(IllegalStateException("Storage Spaces context unavailable")))
            else database.canonicalStorageSpaceReadDao().observe(organizationId.value,locationId.value,storageUnitId.value).map { projections ->
                runCatching {
                    val projection=projections.singleOrNull() ?: error("Storage Spaces context unavailable")
                    check(projection.parent.organizationId==organizationId.value && projection.parent.locationId==locationId.value && projection.parent.storageUnitId==storageUnitId.value)
                    projection.spaces.map { it.domain(organizationId,storageUnitId) }.sortedWith(compareBy<StorageSpace>{it.code}.thenBy{it.storageSpaceId.value})
                }
            }
        }
    private fun V6StorageSpacesRow.domain(org:OrganizationId,parent:StorageUnitId):StorageSpace {
        check(organizationId==org.value && storageUnitId==parent.value)
        check(UUID.fromString(createdBy).toString()==createdBy && UUID.fromString(updatedBy).toString()==updatedBy)
        return StorageSpace(StorageSpaceId(storageSpaceId),parent,code,description,GeometryProfileId(geometryProfileId),
            MasterStatus.valueOf(lifecycleStatus),Instant.ofEpochMilli(createdAt),createdBy,Instant.ofEpochMilli(updatedAt),updatedBy,entityVersion)
    }
}
