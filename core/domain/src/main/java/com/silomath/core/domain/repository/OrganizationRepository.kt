package com.silomath.core.domain.repository

import com.silomath.core.domain.model.OrganizationReadModel
import com.silomath.core.model.UserId
import kotlinx.coroutines.flow.Flow

interface OrganizationRepository {
    fun observeAuthorizedOrganizations(
        userId: UserId
    ): Flow<List<OrganizationReadModel>>

    suspend fun refreshOrganizations(): Result<Unit>
}