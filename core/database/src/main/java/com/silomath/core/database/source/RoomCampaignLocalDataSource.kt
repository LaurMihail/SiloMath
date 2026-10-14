package com.silomath.core.database.source

import androidx.room.withTransaction
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.dao.CampaignDao
import com.silomath.core.database.dao.GeometryDao
import com.silomath.core.database.dao.OrganizationDao
import com.silomath.core.database.dao.StorageDao
import com.silomath.core.database.entity.CampaignAssignmentEntity
import com.silomath.core.database.entity.CampaignEntity
import com.silomath.core.database.entity.CampaignScopeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomCampaignLocalDataSource @Inject constructor(
    private val campaignDao: CampaignDao,
    private val organizationDao: OrganizationDao,
    private val storageDao: StorageDao,
    private val geometryDao: GeometryDao,
    private val siloMathDatabase: SiloMathDatabase
) : CampaignLocalDataSource {

    override fun observeActiveCampaignCount(): Flow<Int> {
        return campaignDao.observeActiveCampaignCount()
    }

    override suspend fun getOrganizationByOrganizationId(
        organizationId: String
    ): CampaignLocalOrganizationRef? {
        val organization =
            organizationDao.getOrganizationByOrganizationId(
                organizationId
            ) ?: return null

        return CampaignLocalOrganizationRef(
            legacyId = organization.id,
            organizationId = organization.organizationId
        )
    }

    override suspend fun getOrganizationByLegacyId(
        organizationId: Long
    ): CampaignLocalOrganizationRef? {
        val organization =
            organizationDao.getOrganizationById(
                organizationId
            ) ?: return null

        return CampaignLocalOrganizationRef(
            legacyId = organization.id,
            organizationId = organization.organizationId
        )
    }

    override suspend fun upsertCampaign(
        organizationLegacyId: Long,
        canonicalOrganizationId: String,
        remoteId: String,
        name: String,
        legacyStatus: String,
        canonicalStatus: String,
        remoteRevision: Long,
        campaignCode: String?,
        inventoryType: String?,
        startDate: String?,
        plannedEndDate: String?,
        launchedAt: Long?,
        launchedBy: String?,
        completedAt: Long?,
        completedBy: String?,
        campaignSnapshotId: String?,
        createdAt: Long?,
        createdBy: String?,
        updatedAt: Long?,
        updatedBy: String?
    ) {
        val existing =
            campaignDao.getCampaignByRemoteId(
                remoteId
            )

        if (existing != null) {
            campaignDao.updateCampaign(
                existing.copy(
                    organizationId = organizationLegacyId,
                    name = name,
                    status = legacyStatus,
                    remoteRevision = remoteRevision,
                    campaignId = remoteId.lowercase(),
                    canonicalOrganizationId =
                        canonicalOrganizationId.lowercase(),
                    campaignCode =
                        campaignCode ?: existing.campaignCode,
                    inventoryType =
                        inventoryType ?: existing.inventoryType,
                    startDate =
                        startDate ?: existing.startDate,
                    plannedEndDate =
                        plannedEndDate ?: existing.plannedEndDate,
                    canonicalStatus = canonicalStatus,
                    launchedAt =
                        launchedAt ?: existing.launchedAt,
                    launchedBy =
                        launchedBy ?: existing.launchedBy,
                    completedAt =
                        completedAt ?: existing.completedAt,
                    completedBy =
                        completedBy ?: existing.completedBy,
                    campaignSnapshotId =
                        campaignSnapshotId
                            ?: existing.campaignSnapshotId,
                    createdAt =
                        createdAt ?: existing.createdAt,
                    createdBy =
                        createdBy ?: existing.createdBy,
                    updatedAt =
                        updatedAt ?: existing.updatedAt,
                    updatedBy =
                        updatedBy ?: existing.updatedBy,
                    entityVersion = remoteRevision
                )
            )
        } else {
            campaignDao.insertCampaign(
                CampaignEntity(
                    organizationId = organizationLegacyId,
                    name = name,
                    status = legacyStatus,
                    remoteId = remoteId,
                    remoteRevision = remoteRevision,
                    campaignId = remoteId.lowercase(),
                    canonicalOrganizationId =
                        canonicalOrganizationId.lowercase(),
                    campaignCode = campaignCode,
                    inventoryType = inventoryType,
                    startDate = startDate,
                    plannedEndDate = plannedEndDate,
                    canonicalStatus = canonicalStatus,
                    launchedAt = launchedAt,
                    launchedBy = launchedBy,
                    completedAt = completedAt,
                    completedBy = completedBy,
                    campaignSnapshotId = campaignSnapshotId,
                    createdAt = createdAt,
                    createdBy = createdBy,
                    updatedAt = updatedAt,
                    updatedBy = updatedBy,
                    entityVersion = remoteRevision
                )
            )
        }
    }

    override suspend fun upsertScope(
        remoteId: String,
        campaignRemoteId: String,
        storageSpaceRemoteId: String,
        geometryVersionRemoteId: String?,
        snapshotHierarchyRef: String?,
        createdAt: Long?,
        createdBy: String?
    ) {
        val campaign =
            campaignDao.getCampaignByRemoteId(
                campaignRemoteId
            )
                ?: throw IllegalStateException(
                    "Unresolved parent campaign remoteId for scope $remoteId"
                )

        val storageSpace =
            storageDao.getStorageSpaceByRemoteId(
                storageSpaceRemoteId
            )
                ?: throw IllegalStateException(
                    "Unresolved parent storage_space remoteId for scope $remoteId"
                )

        val geometryVersionId =
            geometryVersionRemoteId?.let { remoteGeometryId ->
                geometryDao.getGeometryVersionByRemoteId(
                    remoteGeometryId
                )?.id
                    ?: throw IllegalStateException(
                        "Unresolved parent geometry_version remoteId for scope $remoteId"
                    )
            }

        val existing =
            campaignDao.getScopeByRemoteId(
                remoteId
            )

        if (existing != null) {
            campaignDao.updateScope(
                existing.copy(
                    campaignId = campaign.id,
                    storageSpaceId = storageSpace.id,
                    geometryVersionId =
                        geometryVersionId
                            ?: existing.geometryVersionId,
                    campaignScopeId =
                        remoteId.lowercase(),
                    canonicalCampaignId =
                        campaignRemoteId.lowercase(),
                    canonicalStorageSpaceId =
                        storageSpaceRemoteId.lowercase(),
                    snapshotHierarchyRef =
                        snapshotHierarchyRef
                            ?: existing.snapshotHierarchyRef,
                    createdAt =
                        createdAt ?: existing.createdAt,
                    createdBy =
                        createdBy ?: existing.createdBy
                )
            )
        } else {
            campaignDao.insertScope(
                CampaignScopeEntity(
                    campaignId = campaign.id,
                    storageSpaceId = storageSpace.id,
                    geometryVersionId = geometryVersionId,
                    remoteId = remoteId,
                    campaignScopeId = remoteId.lowercase(),
                    canonicalCampaignId =
                        campaignRemoteId.lowercase(),
                    canonicalStorageSpaceId =
                        storageSpaceRemoteId.lowercase(),
                    snapshotHierarchyRef =
                        snapshotHierarchyRef,
                    createdAt = createdAt,
                    createdBy = createdBy
                )
            )
        }
    }

    override suspend fun upsertAssignment(
        remoteId: String,
        campaignRemoteId: String,
        userId: String,
        roleAtAssignment: String?,
        organizationMembershipId: String?,
        assignmentStatus: String?,
        assignedAt: Long?,
        assignedBy: String?,
        removedAt: Long?,
        removedBy: String?,
        createdAt: Long?,
        createdBy: String?,
        updatedAt: Long?,
        updatedBy: String?,
        entityVersion: Long?
    ) {
        val campaign =
            campaignDao.getCampaignByRemoteId(
                campaignRemoteId
            )
                ?: throw IllegalStateException(
                    "Unresolved parent campaign remoteId for assignment $remoteId"
                )

        val existing =
            campaignDao.getAssignmentByRemoteId(
                remoteId
            )

        if (existing != null) {
            campaignDao.updateAssignment(
                existing.copy(
                    campaignId = campaign.id,
                    userId = userId,
                    roleAtAssignment =
                        roleAtAssignment
                            ?: existing.roleAtAssignment,
                    assignmentId =
                        remoteId.lowercase(),
                    canonicalCampaignId =
                        campaignRemoteId.lowercase(),
                    organizationMembershipId =
                        organizationMembershipId
                            ?: existing.organizationMembershipId,
                    assignmentStatus =
                        assignmentStatus
                            ?: existing.assignmentStatus,
                    assignedAt =
                        assignedAt ?: existing.assignedAt,
                    assignedBy =
                        assignedBy ?: existing.assignedBy,
                    removedAt =
                        removedAt ?: existing.removedAt,
                    removedBy =
                        removedBy ?: existing.removedBy,
                    createdAt =
                        createdAt ?: existing.createdAt,
                    createdBy =
                        createdBy ?: existing.createdBy,
                    updatedAt =
                        updatedAt ?: existing.updatedAt,
                    updatedBy =
                        updatedBy ?: existing.updatedBy,
                    entityVersion =
                        entityVersion ?: existing.entityVersion
                )
            )
        } else {
            campaignDao.insertAssignment(
                CampaignAssignmentEntity(
                    campaignId = campaign.id,
                    userId = userId,
                    roleAtAssignment = roleAtAssignment,
                    remoteId = remoteId,
                    assignmentId = remoteId.lowercase(),
                    canonicalCampaignId =
                        campaignRemoteId.lowercase(),
                    organizationMembershipId =
                        organizationMembershipId,
                    assignmentStatus =
                        assignmentStatus,
                    assignedAt = assignedAt,
                    assignedBy = assignedBy,
                    removedAt = removedAt,
                    removedBy = removedBy,
                    createdAt = createdAt,
                    createdBy = createdBy,
                    updatedAt = updatedAt,
                    updatedBy = updatedBy,
                    entityVersion = entityVersion
                )
            )
        }
    }

    override suspend fun <T> inTransaction(
        block: suspend CampaignLocalDataSource.() -> T
    ): T {
        return siloMathDatabase.withTransaction {
            block(this@RoomCampaignLocalDataSource)
        }
    }
}
