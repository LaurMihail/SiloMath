package com.silomath.core.data.network.source

import com.silomath.core.data.network.dto.CampaignAssignmentRemote
import com.silomath.core.data.network.dto.CampaignRemote
import com.silomath.core.data.network.dto.CampaignScopeRemote
import com.silomath.core.data.network.dto.CampaignTeamMemberDto
import com.silomath.core.data.network.dto.SaveAndLaunchCampaignRequestRemote
import com.silomath.core.data.network.dto.SaveAndLaunchCampaignResponseRemote
import com.silomath.core.model.campaign.CampaignAssignmentTransferRecord
import com.silomath.core.model.campaign.CampaignLaunchCommand
import com.silomath.core.model.campaign.CampaignLaunchTransportResult
import com.silomath.core.model.campaign.CampaignScopeTransferRecord
import com.silomath.core.model.campaign.CampaignTransferRecord
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

/**
 * Temporary legacy transport implementation.
 *
 * Supabase/PostgREST DTOs are contained entirely inside :core:network.
 *
 * Removal target:
 * replace this implementation with the frozen V6 REST /api/v1 transport.
 */
class SupabaseCampaignRemoteDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) : CampaignRemoteDataSource {

    override suspend fun getCampaigns(
        organizationId: String
    ): Result<List<CampaignTransferRecord>> {
        return try {
            val response =
                supabaseClient
                    .postgrest
                    .from("campaigns")
                    .select {
                        filter {
                            eq(
                                "organization_id",
                                organizationId
                            )
                        }
                    }
                    .decodeList<CampaignRemote>()

            Result.success(
                response.map {
                    CampaignTransferRecord(
                        id =
                            it.id,
                        organizationId =
                            it.organizationId,
                        name =
                            it.name,
                        status =
                            it.status,
                        revision =
                            it.revision
                    )
                }
            )
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
            val response =
                supabaseClient
                    .postgrest
                    .from("campaign_scope")
                    .select {
                        filter {
                            isIn(
                                "campaign_id",
                                campaignIds
                            )
                        }
                    }
                    .decodeList<CampaignScopeRemote>()

            Result.success(
                response.map {
                    CampaignScopeTransferRecord(
                        id =
                            it.id,
                        campaignId =
                            it.campaignId,
                        storageSpaceId =
                            it.storageSpaceId,
                        geometryVersionId =
                            it.geometryVersionId
                    )
                }
            )
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
            val response =
                supabaseClient
                    .postgrest
                    .from("campaign_assignments")
                    .select {
                        filter {
                            isIn(
                                "campaign_id",
                                campaignIds
                            )
                        }
                    }
                    .decodeList<CampaignAssignmentRemote>()

            Result.success(
                response.map {
                    CampaignAssignmentTransferRecord(
                        id =
                            it.id,
                        campaignId =
                            it.campaignId,
                        userId =
                            it.userId,
                        roleAtAssignment =
                            it.roleAtAssignment
                    )
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveAndLaunchCampaign(
        command: CampaignLaunchCommand
    ): Result<CampaignLaunchTransportResult> {
        return try {
            val request =
                SaveAndLaunchCampaignRequestRemote(
                    organizationId =
                        command.organizationId,
                    name =
                        command.name,
                    inventoryType =
                        command.inventoryType,
                    startDate =
                        command.startDate,
                    plannedEndDate =
                        command.plannedEndDate,
                    scopeStorageSpaceIds =
                        command.scopeStorageSpaceIds,
                    team =
                        command.team.map {
                            CampaignTeamMemberDto(
                                userId =
                                    it.userId,
                                roleAtAssignment =
                                    it.roleAtAssignment
                            )
                        },
                    idempotencyKey =
                        command.idempotencyKey
                )

            val jsonParams =
                Json
                    .encodeToJsonElement(request)
                    .jsonObject

            val response =
                supabaseClient
                    .postgrest
                    .rpc(
                        function =
                            "save_and_launch_campaign",
                        parameters =
                            jsonParams
                    )
                    .decodeList<SaveAndLaunchCampaignResponseRemote>()
                    .first()

            Result.success(
                CampaignLaunchTransportResult(
                    campaignId =
                        response.campaignId,
                    launchSnapshotId =
                        response.launchSnapshotId
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
