package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.canonical.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageUnitCanonicalDao {
    @Query("SELECT * FROM v6_storage_units WHERE organization_id=:org AND location_id=:location ORDER BY name COLLATE NOCASE, storage_unit_id")
    fun observe(org:String,location:String):Flow<List<V6StorageUnitsRow>>
    @Query("SELECT * FROM v6_storage_units WHERE storage_unit_id=:id")
    suspend fun get(id:String):V6StorageUnitsRow?
    @Query("SELECT * FROM v6_locations WHERE location_id=:id AND organization_id=:org")
    suspend fun parent(org:String,id:String):V6LocationsRow?
    @Query("SELECT * FROM v6_organizations WHERE organization_id=:id")
    suspend fun organization(id:String):V6OrganizationsRow?
    @Query("SELECT * FROM sync_operations WHERE organization_id=:org AND entity_type='STORAGE_UNIT' AND state!='ACKNOWLEDGED' ORDER BY created_at,sync_operation_id")
    suspend fun pending(org:String):List<com.silomath.core.database.entity.SyncOperationEntity>
    @Query("SELECT * FROM sync_operations WHERE entity_id=:id AND state!='ACKNOWLEDGED' ORDER BY created_at,sync_operation_id")
    suspend fun unresolved(id:String):List<com.silomath.core.database.entity.SyncOperationEntity>
    @Query("SELECT COALESCE(MAX(created_at),0) FROM sync_operations WHERE entity_type='STORAGE_UNIT' AND entity_id=:id")
    suspend fun lastQueuedAt(id:String):Long
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insert(row:V6StorageUnitsRow)
    @Update suspend fun update(row:V6StorageUnitsRow):Int
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertParent(row:V6LocationsRow)
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertOrganization(row:V6OrganizationsRow)
    @Query("SELECT EXISTS(SELECT 1 FROM v6_storage_units WHERE location_id=:location AND upper(code)=:code AND storage_unit_id!=:id)")
    suspend fun duplicate(location:String,code:String,id:String):Boolean
    @Query("SELECT EXISTS(SELECT 1 FROM v6_campaign_snapshot_scope_items WHERE storage_unit_id=:id) OR EXISTS(SELECT 1 FROM v6_campaign_scopes c JOIN v6_storage_spaces s ON s.storage_space_id=c.storage_space_id WHERE s.storage_unit_id=:id) OR EXISTS(SELECT 1 FROM v6_stocktaking_records r JOIN v6_storage_spaces s ON s.storage_space_id=r.storage_space_id WHERE s.storage_unit_id=:id)")
    suspend fun historicallyReferenced(id:String):Boolean
}
