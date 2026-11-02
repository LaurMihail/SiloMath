package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.silomath.core.database.entity.canonical.V6LocationsRow
import com.silomath.core.database.entity.canonical.V6OrganizationsRow
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationCanonicalDao {

    @Query("SELECT * FROM v6_locations WHERE organization_id=:organizationId ORDER BY name COLLATE NOCASE, location_id")
    fun observeByOrganization(organizationId: String): Flow<List<V6LocationsRow>>

    @Query("SELECT * FROM v6_locations WHERE location_id=:locationId LIMIT 1")
    suspend fun get(locationId: String): V6LocationsRow?

    @Query("SELECT * FROM v6_organizations WHERE organization_id=:organizationId LIMIT 1")
    suspend fun organization(organizationId: String): V6OrganizationsRow?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: V6LocationsRow)

    @Update
    suspend fun update(row: V6LocationsRow): Int

    @Query("SELECT EXISTS(SELECT 1 FROM v6_locations WHERE organization_id=:organizationId AND upper(code)=upper(:code) AND location_id!=:locationId)")
    suspend fun duplicateCode(organizationId: String, code: String, locationId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM v6_storage_units WHERE location_id=:locationId) OR EXISTS(SELECT 1 FROM v6_campaign_snapshot_scope_items WHERE location_id=:locationId)")
    suspend fun historicallyReferenced(locationId: String): Boolean
}
