package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.OrganizationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organizations")
    fun getAllOrganizations(): Flow<List<OrganizationEntity>>

    @Query("SELECT * FROM organizations WHERE id = :id LIMIT 1")
    suspend fun getOrganizationById(id: Long): OrganizationEntity?

    @Query("SELECT * FROM organizations WHERE organization_id = :organizationId LIMIT 1")
    suspend fun getOrganizationByOrganizationId(organizationId: String): OrganizationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrganization(organization: OrganizationEntity): Long

    @Update
    suspend fun updateOrganization(organization: OrganizationEntity)

    @Delete
    suspend fun deleteOrganization(organization: OrganizationEntity)
}
