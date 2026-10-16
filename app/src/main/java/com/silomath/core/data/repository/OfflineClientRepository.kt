package com.silomath.core.data.repository

import com.silomath.core.database.dao.ClientDao
import com.silomath.core.database.dao.OrganizationDao
import com.silomath.core.database.entity.ClientEntity
import com.silomath.core.domain.model.Client
import com.silomath.core.domain.model.MasterStatus
import com.silomath.core.domain.repository.ClientRepository
import com.silomath.core.model.ClientId
import com.silomath.core.model.OrganizationId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineClientRepository @Inject constructor(
    private val clientDao: ClientDao,
    private val organizationDao: OrganizationDao
) : ClientRepository {

    override fun observeClients(
        organizationId: OrganizationId
    ): Flow<List<Client>> = flow {
        val organization =
            organizationDao.getOrganizationByOrganizationId(
                organizationId.value
            )

        if (organization == null) {
            emit(emptyList())
            return@flow
        }

        emitAll(
            clientDao
                .getClientsByOrganization(organization.id)
                .map { entities ->
                    entities.mapNotNull { entity ->
                        entity.toCanonicalClientOrNull(
                            expectedOrganizationId = organizationId
                        )
                    }
                }
        )
    }

    override suspend fun saveClient(
        client: Client
    ): Result<Unit> = runCatching {
        val organization =
            organizationDao.getOrganizationByOrganizationId(
                client.organizationId.value
            )
                ?: error(
                    "Canonical Organization not found for Client: " +
                        client.organizationId.value
                )

        val existing =
            clientDao.getClientByClientId(client.clientId.value)

        val entity = ClientEntity(
            id = existing?.id ?: 0L,
            organizationId = organization.id,
            remoteId = existing?.remoteId,
            name = client.name,
            lifecycleStatus = client.lifecycleStatus.name,
            remoteRevision = existing?.remoteRevision,
            clientId = client.clientId.value,
            canonicalOrganizationId = client.organizationId.value,
            code = client.code,
            registrationTaxId = client.registrationTaxId,
            notes = client.notes,
            createdAt = client.createdAt.toEpochMilli(),
            createdBy = client.createdBy,
            updatedAt = client.updatedAt.toEpochMilli(),
            updatedBy = client.updatedBy,
            entityVersion = client.entityVersion
        )

        if (existing == null) {
            clientDao.insertClient(entity)
        } else {
            clientDao.updateClient(entity)
        }
    }

    private fun ClientEntity.toCanonicalClientOrNull(
        expectedOrganizationId: OrganizationId
    ): Client? {
        val canonicalClientId = clientId ?: return null
        val canonicalOrganizationId =
            canonicalOrganizationId ?: return null
        val canonicalCode = code ?: return null
        val canonicalCreatedAt = createdAt ?: return null
        val canonicalCreatedBy = createdBy ?: return null
        val canonicalUpdatedAt = updatedAt ?: return null
        val canonicalUpdatedBy = updatedBy ?: return null
        val canonicalEntityVersion = entityVersion ?: return null

        if (canonicalOrganizationId != expectedOrganizationId.value) {
            return null
        }

        val status = when (lifecycleStatus) {
            MasterStatus.ACTIVE.name -> MasterStatus.ACTIVE
            MasterStatus.INACTIVE.name -> MasterStatus.INACTIVE
            else -> return null
        }

        return runCatching {
            Client(
                clientId = ClientId(canonicalClientId),
                organizationId =
                    OrganizationId(canonicalOrganizationId),
                name = name,
                code = canonicalCode,
                registrationTaxId = registrationTaxId,
                notes = notes,
                lifecycleStatus = status,
                createdAt = Instant.ofEpochMilli(canonicalCreatedAt),
                createdBy = canonicalCreatedBy,
                updatedAt = Instant.ofEpochMilli(canonicalUpdatedAt),
                updatedBy = canonicalUpdatedBy,
                entityVersion = canonicalEntityVersion
            )
        }.getOrNull()
    }
}
