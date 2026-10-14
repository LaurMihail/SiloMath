package com.silomath.core.domain.repository

import com.silomath.core.database.entity.OrganizationEntity
import com.silomath.core.database.entity.OrganizationMemberEntity
import kotlinx.coroutines.flow.Flow

interface OrganizationRepository {
    fun getOrganizations(): Flow<List<OrganizationEntity>>
    suspend fun saveOrganization(organization: OrganizationEntity)
    suspend fun refreshOrganizations(): Result<Unit>
    fun getMembersByOrganization(organizationId: String): Flow<List<OrganizationMemberEntity>>
    suspend fun refreshOrganizationMembers(organizationId: String): Result<Unit>
    suspend fun getOrganizationRemoteId(organizationId: String): Result<String>
}
