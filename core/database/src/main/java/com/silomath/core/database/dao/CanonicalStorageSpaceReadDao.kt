package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.canonical.V6StorageSpacesRow
import com.silomath.core.database.entity.canonical.V6StorageUnitsRow
import kotlinx.coroutines.flow.Flow

data class CanonicalUnitSpaces(
    @Embedded val parent:V6StorageUnitsRow,
    @Relation(parentColumn="storage_unit_id",entityColumn="storage_unit_id") val spaces:List<V6StorageSpacesRow>
)

/** Canonical tables only; parent + children are read in one Room transaction. */
@Dao
interface CanonicalStorageSpaceReadDao {
    @Transaction
    @Query("""SELECT u.* FROM v6_storage_units u
        JOIN v6_locations l ON l.location_id=u.location_id AND l.organization_id=u.organization_id
        JOIN v6_organizations o ON o.organization_id=l.organization_id
        WHERE u.storage_unit_id=:unit AND u.organization_id=:org AND l.location_id=:location
        AND o.lifecycle_status='ACTIVE'""")
    fun observe(org:String,location:String,unit:String):Flow<List<CanonicalUnitSpaces>>
}
