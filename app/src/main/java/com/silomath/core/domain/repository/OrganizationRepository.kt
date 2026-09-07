package com.silomath.core.domain.repository

import com.silomath.core.database.entity.OrganizationEntity
import kotlinx.coroutines.flow.Flow

interface OrganizationRepository {
    fun getOrganizations(): Flow<List<OrganizationEntity>>
    suspend fun saveOrganization(organization: OrganizationEntity)
}
