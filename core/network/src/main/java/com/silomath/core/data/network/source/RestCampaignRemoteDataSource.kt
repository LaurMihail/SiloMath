package com.silomath.core.data.network.source

import com.silomath.core.data.network.api.CampaignApi
import com.silomath.core.data.network.auth.AuthenticatedApiCallExecutor
import com.silomath.core.data.network.dto.CampaignLaunchRequestDto
import com.silomath.core.data.network.dto.CampaignLaunchTeamMemberDto
import com.silomath.core.data.network.dto.CampaignResponseDto
import com.silomath.core.data.network.dto.CampaignScopeItemDto
import com.silomath.core.data.network.dto.CampaignSummaryDto
import com.silomath.core.data.network.dto.CampaignTeamAssignmentDto
import com.silomath.core.model.campaign.CampaignAssignmentTransferRecord
import com.silomath.core.model.campaign.CampaignLaunchCommand
import com.silomath.core.model.campaign.CampaignLaunchTransportResult
import com.silomath.core.model.campaign.CampaignScopeTransferRecord
import com.silomath.core.model.campaign.CampaignTransferRecord
import javax.inject.Inject

class RestCampaignRemoteDataSource @Inject constructor(
    private val campaignApi: CampaignApi,
    private val authenticatedApiCallExecutor:
        AuthenticatedApiCallExecutor
) : CampaignRemoteDataSource {

    override suspend fun getCampaigns(
        organizationId: String
    ): Result<List<CampaignTransferRecord>> {
        return try {
            val result =
                mutableListOf<CampaignTransferRecord>()

            var cursor: String? = null
            var hasMore: Boolean

            do {
                val page =
                    authenticatedApiCallExecutor.execute {
                        campaignApi.getCampaigns(
                            organizationId =
                                organizationId,
                            cursor =
                                cursor
                        )
                    }

                result +=
                    page.items.map {
                        it.toTransferRecord()
                    }

                hasMore =
                    page.hasMore

                cursor =
                    nextCursorOrThrow(
                        hasMore =
                            page.hasMore,
                        nextCursor =
                            page.nextCursor,
                        resource =
                            "Campaign"
                    )
            } while (hasMore)

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCampaignScope(
        organizationId: String,
        campaignIds: List<String>
    ): Result<List<CampaignScopeTransferRecord>> {
        if (campaignIds.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            val result =
                mutableListOf<CampaignScopeTransferRecord>()

            for (campaignId in campaignIds) {
                var cursor: String? = null
                var hasMore: Boolean

                do {
                    val page =
                        authenticatedApiCallExecutor.execute {
                            campaignApi.getCampaignScope(
                                organizationId =
                                    organizationId,
                                campaignId =
                                    campaignId,
                                cursor =
                                    cursor
                            )
                        }

                    result +=
                        page.items.map {
                            it.toTransferRecord()
                        }

                    hasMore =
                        page.hasMore

                    cursor =
                        nextCursorOrThrow(
                            hasMore =
                                page.hasMore,
                            nextCursor =
                                page.nextCursor,
                            resource =
                                "Campaign Scope"
                        )
                } while (hasMore)
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCampaignAssignments(
        organizationId: String,
        campaignIds: List<String>
    ): Result<List<CampaignAssignmentTransferRecord>> {
        if (campaignIds.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            val result =
                mutableListOf<CampaignAssignmentTransferRecord>()

            for (campaignId in campaignIds) {
                var cursor: String? = null
                var hasMore: Boolean

                do {
                    val page =
                        authenticatedApiCallExecutor.execute {
                            campaignApi.getCampaignTeam(
                                organizationId =
                                    organizationId,
                                campaignId =
                                    campaignId,
                                cursor =
                                    cursor
                            )
                        }

                    result +=
                        page.items.map {
                            it.toTransferRecord()
                        }

                    hasMore =
                        page.hasMore

                    cursor =
                        nextCursorOrThrow(
                            hasMore =
                                page.hasMore,
                            nextCursor =
                                page.nextCursor,
                            resource =
                                "Campaign Team"
                        )
                } while (hasMore)
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveAndLaunchCampaign(
        command: CampaignLaunchCommand
    ): Result<CampaignLaunchTransportResult> {
        return try {
            val inventoryType =
                normalizeInventoryType(
                    command.inventoryType
                )

            val request =
                CampaignLaunchRequestDto(
                    name =
                        command.name,
                    inventoryType =
                        inventoryType,
                    startDate =
                        command.startDate,
                    plannedEndDate =
                        command.plannedEndDate,
                    scopeStorageSpaceIds =
                        command.scopeStorageSpaceIds,
                    team =
                        command.team.map {
                            CampaignLaunchTeamMemberDto(
                                userId =
                                    it.userId
                            )
                        }
                )

            val response =
                authenticatedApiCallExecutor.execute {
                    campaignApi.launchCampaign(
                        organizationId =
                            command.organizationId,
                        idempotencyKey =
                            command.idempotencyKey,
                        request =
                            request
                    )
                }

            Result.success(
                CampaignLaunchTransportResult(
                    campaignId =
                        response.id,
                    launchSnapshotId =
                        response.campaignSnapshotId,
                    campaign =
                        response.toTransferRecord()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun nextCursorOrThrow(
        hasMore: Boolean,
        nextCursor: String?,
        resource: String
    ): String? {
        if (!hasMore) {
            return null
        }

        return nextCursor
            ?: throw IllegalStateException(
                "$resource response has hasMore=true but nextCursor is null."
            )
    }

    private fun normalizeInventoryType(
        value: String
    ): String {
        val normalized =
            value
                .trim()
                .uppercase()

        if (
            normalized != "ANNUAL" &&
            normalized != "MONTHLY" &&
            normalized != "CONTROL"
        ) {
            throw IllegalArgumentException(
                "Unsupported Campaign inventory type: $value"
            )
        }

        return normalized
    }

    private fun CampaignSummaryDto.toTransferRecord():
        CampaignTransferRecord {
        return CampaignTransferRecord(
            id =
                id,
            organizationId =
                organizationId,
            name =
                name,
            status =
                status,
            revision =
                entityVersion,
            campaignCode =
                campaignCode,
            inventoryType =
                inventoryType,
            startDate =
                startDate,
            plannedEndDate =
                plannedEndDate
        )
    }

    private fun CampaignResponseDto.toTransferRecord():
        CampaignTransferRecord {
        return CampaignTransferRecord(
            id =
                id,
            organizationId =
                organizationId,
            name =
                name,
            status =
                status,
            revision =
                entityVersion,
            campaignCode =
                campaignCode,
            inventoryType =
                inventoryType,
            startDate =
                startDate,
            plannedEndDate =
                plannedEndDate,
            launchedAt =
                launchedAt,
            launchedBy =
                launchedBy,
            completedAt =
                completedAt,
            completedBy =
                completedBy,
            campaignSnapshotId =
                campaignSnapshotId,
            createdAt =
                createdAt,
            createdBy =
                createdBy,
            updatedAt =
                updatedAt,
            updatedBy =
                updatedBy
        )
    }

    private fun CampaignScopeItemDto.toTransferRecord():
        CampaignScopeTransferRecord {
        return CampaignScopeTransferRecord(
            id =
                id,
            campaignId =
                campaignId,
            storageSpaceId =
                storageSpaceId,
            geometryVersionId =
                null,
            snapshotHierarchyRef =
                snapshotHierarchyRef,
            createdAt =
                createdAt,
            createdBy =
                createdBy
        )
    }

    private fun CampaignTeamAssignmentDto.toTransferRecord():
        CampaignAssignmentTransferRecord {
        return CampaignAssignmentTransferRecord(
            id =
                id,
            campaignId =
                campaignId,
            userId =
                userId,
            roleAtAssignment =
                null,
            organizationMembershipId =
                organizationMembershipId,
            assignmentStatus =
                assignmentStatus,
            assignedAt =
                assignedAt,
            assignedBy =
                assignedBy,
            removedAt =
                removedAt,
            removedBy =
                removedBy,
            createdAt =
                createdAt,
            createdBy =
                createdBy,
            updatedAt =
                updatedAt,
            updatedBy =
                updatedBy,
            entityVersion =
                entityVersion
        )
    }
}
