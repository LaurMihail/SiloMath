package com.silomath.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GeometryFamilyRemote(
    val id: String,
    val name: String,
    @SerialName("lifecycle_status") val lifecycleStatus: String = "ACTIVE",
    val revision: Long = 1
)

@Serializable
data class GeometryProfileRemote(
    val id: String,
    @SerialName("geometry_family_id") val geometryFamilyId: String,
    @SerialName("profile_code") val profileCode: String,
    val name: String,
    @SerialName("lifecycle_status") val lifecycleStatus: String = "ACTIVE",
    val revision: Long = 1
)

@Serializable
data class GeometryVersionRemote(
    val id: String,
    @SerialName("geometry_profile_id") val geometryProfileId: String,
    @SerialName("version_number") val versionNumber: Int,
    @SerialName("fixed_parameters") val fixedParameters: JsonElement,
    @SerialName("required_dimensions_schema") val requiredDimensionsSchema: JsonElement,
    @SerialName("measurement_schema") val measurementSchema: JsonElement,
    @SerialName("supported_units") val supportedUnits: JsonElement,
    @SerialName("calculation_method") val calculationMethod: String,
    @SerialName("validation_constraints") val validationConstraints: JsonElement,
    @SerialName("capacity_rules") val capacityRules: JsonElement? = null
)

@Serializable
data class LocationRemote(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    val code: String,
    val name: String,
    @SerialName("lifecycle_status") val lifecycleStatus: String = "ACTIVE",
    val revision: Long = 1
)

@Serializable
data class StorageUnitRemote(
    val id: String,
    @SerialName("location_id") val locationId: String,
    val code: String,
    val name: String? = null,
    val type: String = "SILO",
    @SerialName("lifecycle_status") val lifecycleStatus: String = "ACTIVE",
    val revision: Long = 1
)

@Serializable
data class StorageSpaceRemote(
    val id: String,
    @SerialName("storage_unit_id") val storageUnitId: String,
    val code: String,
    @SerialName("geometry_profile_id") val geometryProfileId: String,
    @SerialName("geometry_version_id") val geometryVersionId: String,
    @SerialName("configured_dimensions") val configuredDimensions: JsonElement,
    @SerialName("lifecycle_status") val lifecycleStatus: String = "ACTIVE",
    val revision: Long = 1
)

@Serializable
data class CommodityRemote(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("reference_density") val referenceDensity: Double? = null,
    @SerialName("density_unit") val densityUnit: String,
    @SerialName("min_density") val minDensity: Double? = null,
    @SerialName("max_density") val maxDensity: Double? = null,
    @SerialName("validation_parameters") val validationParameters: JsonElement? = null,
    @SerialName("lifecycle_status") val lifecycleStatus: String = "ACTIVE",
    val revision: Long = 1
)

@Serializable
data class ClientRemote(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    val name: String,
    @SerialName("lifecycle_status") val lifecycleStatus: String = "ACTIVE",
    val revision: Long = 1
)

@Serializable
data class OrganizationMemberRemote(
    @SerialName("organization_id") val organizationId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CampaignRemote(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    val name: String,
    val status: String,
    val revision: Long = 1
)

@Serializable
data class CampaignScopeRemote(
    val id: String,
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("storage_space_id") val storageSpaceId: String,
    @SerialName("geometry_version_id") val geometryVersionId: String
)

@Serializable
data class CampaignAssignmentRemote(
    val id: String,
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("role_at_assignment") val roleAtAssignment: String
)

@Serializable
data class StocktakingRecordRemote(
    val id: String,
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("storage_space_id") val storageSpaceId: String,
    @SerialName("commodity_id") val commodityId: String? = null,
    val density: Double? = null,
    val ownership: String,
    @SerialName("client_id") val clientId: String? = null,
    val status: String,
    val revision: Long = 1
)

@Serializable
data class MeasurementRemote(
    val id: String,
    @SerialName("stocktaking_record_id") val stocktakingRecordId: String,
    val value: Double,
    @SerialName("measured_at") val measuredAt: String
)

@Serializable
data class CalculationResultRemote(
    val id: String,
    @SerialName("stocktaking_record_id") val stocktakingRecordId: String,
    val volume: Double,
    val mass: Double,
    @SerialName("engine_version") val engineVersion: String,
    @SerialName("metadata_json") val metadataJson: JsonElement
)

@Serializable
data class IssueRemote(
    val id: String,
    @SerialName("stocktaking_record_id") val stocktakingRecordId: String,
    val description: String,
    @SerialName("reported_at") val reportedAt: String
)

@Serializable
data class InventorySnapshotRemote(
    val id: String,
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("captured_at") val capturedAt: String,
    @SerialName("metadata_json") val metadataJson: JsonElement
)

@Serializable
data class InventorySnapshotItemRemote(
    val id: String,
    @SerialName("snapshot_id") val snapshotId: String,
    @SerialName("storage_space_code") val storageSpaceCode: String,
    @SerialName("captured_geometry_version_id") val capturedGeometryVersionId: String,
    @SerialName("commodity_name") val commodityName: String? = null,
    val density: Double? = null,
    val ownership: String,
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("final_volume") val finalVolume: Double,
    @SerialName("final_mass") val finalMass: Double,
    @SerialName("engine_version") val engineVersion: String,
    @SerialName("metadata_json") val metadataJson: JsonElement
)

@Serializable
data class CampaignTeamMemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("role_at_assignment") val roleAtAssignment: String
)

@Serializable
data class SaveAndLaunchCampaignRequestRemote(
    @SerialName("p_organization_id") val organizationId: String,
    @SerialName("p_name") val name: String,
    @SerialName("p_inventory_type") val inventoryType: String,
    @SerialName("p_start_date") val startDate: String,
    @SerialName("p_planned_end_date") val plannedEndDate: String,
    @SerialName("p_scope_storage_space_ids") val scopeStorageSpaceIds: List<String>,
    @SerialName("p_team") val team: List<CampaignTeamMemberDto>,
    @SerialName("p_idempotency_key") val idempotencyKey: String
)

@Serializable
data class SaveAndLaunchCampaignResponseRemote(
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("launch_snapshot_id") val launchSnapshotId: String,
    val reused: Boolean
)
