package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE organization_id = :organizationId")
    fun getLocationsByOrganization(organizationId: Long): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE organization_id = :organizationId")
    suspend fun getLocationsByOrganizationSync(organizationId: Long): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE location_id = :locationId LIMIT 1")
    suspend fun getLocationByLocationId(locationId: String): LocationEntity?

    @Query("SELECT * FROM locations WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getLocationByRemoteId(remoteId: String): LocationEntity?

    // Positive references only. Never reinterpret/import legacy payloads or rewrite snapshots.
    @Query("""
        SELECT EXISTS(SELECT 1 FROM v6_campaign_snapshot_scope_items WHERE location_id = :locationId)
            OR EXISTS(SELECT 1 FROM campaign_scope cs
                JOIN storage_spaces sp ON sp.id=cs.storage_space_id
                JOIN storage_units su ON su.id=sp.storage_unit_id
                JOIN locations l ON l.id=su.location_id WHERE l.location_id=:locationId)
            OR EXISTS(SELECT 1 FROM stocktaking_records st
                JOIN storage_spaces sp ON sp.id=st.storage_space_id
                JOIN storage_units su ON su.id=sp.storage_unit_id
                JOIN locations l ON l.id=su.location_id WHERE l.location_id=:locationId)
    """)
    suspend fun hasHistoricalReferences(locationId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)
}
