package com.silomath.core.domain.repository

import com.silomath.core.domain.model.Client
import com.silomath.core.model.OrganizationId
import kotlinx.coroutines.flow.Flow

interface ClientRepository {
    fun observeClients(
        organizationId: OrganizationId
    ): Flow<List<Client>>

    suspend fun saveClient(
        client: Client
    ): Result<Unit>
}
