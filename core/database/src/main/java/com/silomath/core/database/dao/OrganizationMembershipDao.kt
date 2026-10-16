package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.silomath.core.database.entity.OrganizationMembershipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationMembershipDao {

    @Query(
        """
        SELECT *
        FROM organization_memberships
        WHERE organization_id = :organizationId
          AND membership_id IS NOT NULL
          AND lifecycle_status = 'ACTIVE'
          AND role IN ('ADMIN', 'STANDARD')
          AND entity_version IS NOT NULL
          AND entity_version >= 1
        ORDER BY user_id
        """
    )
    fun observeActiveCanonicalMemberships(
        organizationId: String
    ): Flow<List<OrganizationMembershipEntity>>

    @Query(
        """
        SELECT *
        FROM organization_memberships
        WHERE organization_id = :organizationId
          AND membership_id IS NOT NULL
          AND lifecycle_status = 'ACTIVE'
          AND role IN ('ADMIN', 'STANDARD')
          AND entity_version IS NOT NULL
          AND entity_version >= 1
        ORDER BY user_id
        """
    )
    suspend fun getActiveCanonicalMemberships(
        organizationId: String
    ): List<OrganizationMembershipEntity>

    @Query(
        """
        SELECT *
        FROM organization_memberships
        WHERE organization_id = :organizationId
          AND user_id = :userId
          AND membership_id IS NOT NULL
          AND lifecycle_status IN ('ACTIVE', 'DISABLED')
          AND role IN ('ADMIN', 'STANDARD')
          AND entity_version IS NOT NULL
          AND entity_version >= 1
        LIMIT 1
        """
    )
    suspend fun getCanonicalMembership(
        organizationId: String,
        userId: String
    ): OrganizationMembershipEntity?

    @Query(
        """
        SELECT COUNT(*)
        FROM organization_memberships
        WHERE organization_id = :organizationId
          AND membership_id IS NOT NULL
          AND lifecycle_status IN ('ACTIVE', 'DISABLED')
          AND role IN ('ADMIN', 'STANDARD')
          AND entity_version IS NOT NULL
          AND entity_version >= 1
        """
    )
    fun observeAuthoritativeCanonicalMembershipCount(
        organizationId: String
    ): Flow<Int>

    @Query(
        """
        SELECT COUNT(*)
        FROM organization_memberships
        WHERE organization_id = :organizationId
          AND membership_id IS NOT NULL
          AND lifecycle_status IN ('ACTIVE', 'DISABLED')
          AND role IN ('ADMIN', 'STANDARD')
          AND entity_version IS NOT NULL
          AND entity_version >= 1
        """
    )
    suspend fun countAuthoritativeCanonicalMemberships(
        organizationId: String
    ): Int

    @Query(
        """
        SELECT DISTINCT organization_id
        FROM organization_memberships
        WHERE organization_id IS NOT NULL
          AND membership_id IS NOT NULL
          AND lifecycle_status IN ('ACTIVE', 'DISABLED')
          AND role IN ('ADMIN', 'STANDARD')
          AND entity_version IS NOT NULL
          AND entity_version >= 1
        """
    )
    fun observeAuthoritativeCanonicalOrganizationIds():
        Flow<List<String>>

    @Query(
        """
        SELECT *
        FROM organization_memberships
        WHERE user_id = :userId
          AND organization_id IS NOT NULL
          AND membership_id IS NOT NULL
          AND lifecycle_status = 'ACTIVE'
          AND role IN ('ADMIN', 'STANDARD')
          AND entity_version IS NOT NULL
          AND entity_version >= 1
        ORDER BY organization_id
        """
    )
    fun observeActiveCanonicalMembershipsForUser(
        userId: String
    ): Flow<List<OrganizationMembershipEntity>>

    @Upsert
    suspend fun upsertCanonicalMemberships(
        memberships: List<OrganizationMembershipEntity>
    )
}