package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.SyncOperationEntity
import com.silomath.core.database.entity.canonical.*
import kotlinx.coroutines.flow.Flow

data class CanonicalUnitSpaces(
    @Embedded val parent:V6StorageUnitsRow,
    @Relation(parentColumn="storage_unit_id",entityColumn="storage_unit_id") val spaces:List<V6StorageSpacesRow>
)

@Dao
interface CanonicalStorageSpaceReadDao {
    @Transaction
    @Query("""SELECT u.* FROM v6_storage_units u
        JOIN v6_locations l ON l.location_id=u.location_id AND l.organization_id=u.organization_id
        JOIN v6_organizations o ON o.organization_id=l.organization_id
        WHERE u.storage_unit_id=:unit AND u.organization_id=:org AND l.location_id=:location
        AND o.lifecycle_status='ACTIVE'""")
    fun observe(org:String,location:String,unit:String):Flow<List<CanonicalUnitSpaces>>

    @Query("""SELECT s.* FROM v6_storage_spaces s
        JOIN v6_storage_units u ON u.storage_unit_id=s.storage_unit_id AND u.organization_id=s.organization_id
        JOIN v6_locations l ON l.location_id=u.location_id AND l.organization_id=u.organization_id
        WHERE s.organization_id=:org AND s.storage_unit_id=:unit ORDER BY s.code,s.storage_space_id""")
    fun observeByUnit(org:String,unit:String):Flow<List<V6StorageSpacesRow>>

    @Query("""SELECT s.* FROM v6_storage_spaces s
        JOIN v6_storage_units u ON u.storage_unit_id=s.storage_unit_id AND u.organization_id=s.organization_id
        JOIN v6_locations l ON l.location_id=u.location_id AND l.organization_id=u.organization_id
        WHERE s.organization_id=:org AND s.storage_space_id=:id LIMIT 1""")
    suspend fun get(org:String,id:String):V6StorageSpacesRow?

    @Query("""SELECT u.* FROM v6_storage_units u JOIN v6_locations l
        ON l.location_id=u.location_id AND l.organization_id=u.organization_id
        WHERE u.organization_id=:org AND u.storage_unit_id=:unit LIMIT 1""")
    suspend fun parent(org:String,unit:String):V6StorageUnitsRow?

    @Query("SELECT * FROM v6_geometry_profiles WHERE organization_id=:org AND geometry_profile_id=:id LIMIT 1")
    suspend fun geometry(org:String,id:String):V6GeometryProfilesRow?

    @Insert(onConflict=OnConflictStrategy.ABORT)
    suspend fun insert(row:V6StorageSpacesRow)

    @Update
    suspend fun update(row:V6StorageSpacesRow):Int

    @Query("SELECT EXISTS(SELECT 1 FROM v6_storage_spaces WHERE storage_unit_id=:unit AND code=:code AND storage_space_id<>:except)")
    suspend fun duplicate(unit:String,code:String,except:String):Boolean

    @Query("""SELECT EXISTS(
        SELECT 1 FROM v6_campaign_scopes WHERE storage_space_id=:id
        UNION SELECT 1 FROM v6_campaign_snapshot_scope_items WHERE storage_space_id=:id
        UNION SELECT 1 FROM v6_stocktaking_records WHERE storage_space_id=:id)""")
    suspend fun historicallyReferenced(id:String):Boolean

    @Query("SELECT * FROM sync_operations WHERE organization_id=:org AND entity_type='STORAGE_SPACE' AND state IN ('PENDING','IN_FLIGHT','RETRYABLE_ERROR','CONFLICT','PERMANENT_ERROR') ORDER BY created_at,sync_operation_id")
    suspend fun pending(org:String):List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations WHERE entity_id=:entity AND state IN ('PENDING','IN_FLIGHT','RETRYABLE_ERROR','CONFLICT','PERMANENT_ERROR') ORDER BY created_at,sync_operation_id")
    suspend fun unresolved(entity:String):List<SyncOperationEntity>

    @Query("SELECT COALESCE(MAX(created_at),0) FROM sync_operations WHERE entity_id=:entity")
    suspend fun lastQueuedAt(entity:String):Long
}
