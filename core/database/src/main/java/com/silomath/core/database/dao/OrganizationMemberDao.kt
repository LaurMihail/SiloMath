package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.silomath.core.database.entity.OrganizationMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationMemberDao {
    @Query("SELECT * FROM organization_members WHERE organization_id = :organizationId")
    fun getMembersByOrganization(organizationId: Long): Flow<List<OrganizationMemberEntity>>

    @Query("SELECT * FROM organization_members WHERE organization_id = :organizationId")
    suspend fun getMembersByOrganizationSync(organizationId: Long): List<OrganizationMemberEntity>

    @Query("SELECT * FROM organization_members WHERE organization_id = :organizationId AND user_id = :userId LIMIT 1")
    suspend fun getMember(organizationId: Long, userId: String): OrganizationMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<OrganizationMemberEntity>)

    @Query("DELETE FROM organization_members WHERE organization_id = :organizationId")
    suspend fun deleteMembersByOrganization(organizationId: Long)
}
