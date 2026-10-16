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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)
}
