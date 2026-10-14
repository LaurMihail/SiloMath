package com.silomath.core.database.source

import kotlinx.coroutines.flow.Flow

interface CampaignLocalDataSource {

    fun observeActiveCampaignCount(): Flow<Int>

    suspend fun getOrganizationByOrganizationId(
        organizationId: String
    ): CampaignLocalOrganizationRef?

    suspend fun getOrganizationByLegacyId(
        organizationId: Long
    ): CampaignLocalOrganizationRef?

    suspend fun upsertCampaign(
        organizationLegacyId: Long,
        canonicalOrganizationId: String,
        remoteId: String,
        name: String,
        legacyStatus: String,
        canonicalStatus: String,
        remoteRevision: Long,
        campaignCode: String? = null,
        inventoryType: String? = null,
        startDate: String? = null,
        plannedEndDate: String? = null,
        launchedAt: Long? = null,
        launchedBy: String? = null,
        completedAt: Long? = null,
        completedBy: String? = null,
        campaignSnapshotId: String? = null,
        createdAt: Long? = null,
        createdBy: String? = null,
        updatedAt: Long? = null,
        updatedBy: String? = null
    )

    suspend fun upsertScope(
        remoteId: String,
        campaignRemoteId: String,
        storageSpaceRemoteId: String,
        geometryVersionRemoteId: String? = null,
        snapshotHierarchyRef: String? = null,
        createdAt: Long? = null,
        createdBy: String? = null
    )

    suspend fun upsertAssignment(
        remoteId: String,
        campaignRemoteId: String,
        userId: String,
        roleAtAssignment: String? = null,
        organizationMembershipId: String? = null,
        assignmentStatus: String? = null,
        assignedAt: Long? = null,
        assignedBy: String? = null,
        removedAt: Long? = null,
        removedBy: String? = null,
        createdAt: Long? = null,
        createdBy: String? = null,
        updatedAt: Long? = null,
        updatedBy: String? = null,
        entityVersion: Long? = null
    )

    suspend fun <T> inTransaction(
        block: suspend CampaignLocalDataSource.() -> T
    ): T
}
