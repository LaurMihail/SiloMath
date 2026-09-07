package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.OrganizationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organizations")
    fun getAllOrganizations(): Flow<List<OrganizationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganization(organization: OrganizationEntity): Long

    @Delete
    suspend fun deleteOrganization(organization: OrganizationEntity)
}
