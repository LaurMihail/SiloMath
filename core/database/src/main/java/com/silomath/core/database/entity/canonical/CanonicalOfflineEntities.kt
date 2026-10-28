package com.silomath.core.database.entity.canonical

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// IADR-0002 EXPAND: explicit V6 rows; no legacy projection or data conversion.

@Entity(tableName = "v6_calculation_results", foreignKeys = [
        ForeignKey(entity = V6StocktakingRevisionsRow::class, parentColumns = ["stocktaking_revision_id"], childColumns = ["stocktaking_revision_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryFamiliesRow::class, parentColumns = ["geometry_family_id"], childColumns = ["geometry_family_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryProfileVersionsRow::class, parentColumns = ["geometry_profile_version_id"], childColumns = ["geometry_profile_version_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["stocktaking_revision_id"], unique = false),
        Index(value = ["geometry_family_id"], unique = false),
        Index(value = ["geometry_profile_version_id"], unique = false)
    ])
data class V6CalculationResultsRow(
    @PrimaryKey @ColumnInfo(name = "calculation_result_id") val calculationResultId: String,
    @ColumnInfo(name = "stocktaking_revision_id") val stocktakingRevisionId: String,
    @ColumnInfo(name = "surface_model_version") val surfaceModelVersion: String,
    @ColumnInfo(name = "aggregation_method") val aggregationMethod: String,
    @ColumnInfo(name = "aggregation_rule_version") val aggregationRuleVersion: Long,
    @ColumnInfo(name = "input_schema_version") val inputSchemaVersion: String,
    @ColumnInfo(name = "engine_version") val engineVersion: String,
    @ColumnInfo(name = "normalization_version") val normalizationVersion: String,
    @ColumnInfo(name = "formula_code") val formulaCode: String? = null,
    @ColumnInfo(name = "formula_version") val formulaVersion: String? = null,
    @ColumnInfo(name = "geometry_family_id") val geometryFamilyId: String? = null,
    @ColumnInfo(name = "geometry_profile_version_id") val geometryProfileVersionId: String? = null,
    @ColumnInfo(name = "frozen_calculation_context") val frozenCalculationContext: String,
    @ColumnInfo(name = "datum_metadata") val datumMetadata: String? = null,
    @ColumnInfo(name = "geometry_inputs_snapshot") val geometryInputsSnapshot: String? = null,
    @ColumnInfo(name = "ordered_normalized_readings") val orderedNormalizedReadings: String? = null,
    @ColumnInfo(name = "geometry_origin") val geometryOrigin: String? = null,
    @ColumnInfo(name = "measurement_reference_height_m") val measurementReferenceHeightM: String? = null,
    @ColumnInfo(name = "maximum_fill_height_m") val maximumFillHeightM: String? = null,
    @ColumnInfo(name = "max_capacity_m3") val maxCapacityM3: String? = null,
    @ColumnInfo(name = "effective_ullage_m") val effectiveUllageM: String? = null,
    @ColumnInfo(name = "effective_fill_height_m") val effectiveFillHeightM: String? = null,
    @ColumnInfo(name = "product_volume_m3") val productVolumeM3: String,
    @ColumnInfo(name = "entered_density_value") val enteredDensityValue: String? = null,
    @ColumnInfo(name = "entered_density_unit") val enteredDensityUnit: String? = null,
    @ColumnInfo(name = "actual_bulk_density_kg_m3") val actualBulkDensityKgM3: String? = null,
    @ColumnInfo(name = "physical_quantity_kg") val physicalQuantityKg: String,
    @ColumnInfo(name = "input_fingerprint") val inputFingerprint: String,
    @ColumnInfo(name = "rounding_policy") val roundingPolicy: String,
    @ColumnInfo(name = "calculated_at") val calculatedAt: Long,
    @ColumnInfo(name = "calculated_by") val calculatedBy: String? = null,
    @ColumnInfo(name = "calculation_source") val calculationSource: String
)

@Entity(tableName = "v6_campaign_scopes", foreignKeys = [
        ForeignKey(entity = V6InventoryCampaignsRow::class, parentColumns = ["campaign_id"], childColumns = ["campaign_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StorageSpacesRow::class, parentColumns = ["storage_space_id"], childColumns = ["storage_space_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["campaign_id", "storage_space_id"], unique = true),
        Index(value = ["campaign_id"], unique = false),
        Index(value = ["storage_space_id"], unique = false)
    ])
data class V6CampaignScopesRow(
    @PrimaryKey @ColumnInfo(name = "campaign_scope_id") val campaignScopeId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "storage_space_id") val storageSpaceId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String
)

@Entity(tableName = "v6_campaign_snapshot_commodity_items", foreignKeys = [
        ForeignKey(entity = V6CampaignSnapshotsRow::class, parentColumns = ["campaign_snapshot_id"], childColumns = ["campaign_snapshot_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6CommoditiesRow::class, parentColumns = ["commodity_id"], childColumns = ["commodity_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6CommodityVersionsRow::class, parentColumns = ["commodity_version_id"], childColumns = ["commodity_version_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["campaign_snapshot_id", "commodity_id"], unique = true),
        Index(value = ["campaign_snapshot_id"], unique = false),
        Index(value = ["commodity_id"], unique = false),
        Index(value = ["commodity_version_id"], unique = false)
    ])
data class V6CampaignSnapshotCommodityItemsRow(
    @PrimaryKey @ColumnInfo(name = "snapshot_commodity_item_id") val snapshotCommodityItemId: String,
    @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String? = null,
    @ColumnInfo(name = "commodity_id") val commodityId: String? = null,
    @ColumnInfo(name = "commodity_version_id") val commodityVersionId: String? = null
)

@Entity(tableName = "v6_campaign_snapshot_scope_items", foreignKeys = [
        ForeignKey(entity = V6CampaignSnapshotsRow::class, parentColumns = ["campaign_snapshot_id"], childColumns = ["campaign_snapshot_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StorageSpacesRow::class, parentColumns = ["storage_space_id"], childColumns = ["storage_space_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6LocationsRow::class, parentColumns = ["location_id"], childColumns = ["location_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StorageUnitsRow::class, parentColumns = ["storage_unit_id"], childColumns = ["storage_unit_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryFamiliesRow::class, parentColumns = ["geometry_family_id"], childColumns = ["geometry_family_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryProfilesRow::class, parentColumns = ["geometry_profile_id"], childColumns = ["geometry_profile_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryProfileVersionsRow::class, parentColumns = ["geometry_profile_version_id"], childColumns = ["geometry_profile_version_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["campaign_snapshot_id", "storage_space_id"], unique = true),
        Index(value = ["campaign_snapshot_id"], unique = false),
        Index(value = ["storage_space_id"], unique = false),
        Index(value = ["location_id"], unique = false),
        Index(value = ["storage_unit_id"], unique = false),
        Index(value = ["geometry_family_id"], unique = false),
        Index(value = ["geometry_profile_id"], unique = false),
        Index(value = ["geometry_profile_version_id"], unique = false)
    ])
data class V6CampaignSnapshotScopeItemsRow(
    @PrimaryKey @ColumnInfo(name = "snapshot_scope_item_id") val snapshotScopeItemId: String,
    @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String,
    @ColumnInfo(name = "storage_space_id") val storageSpaceId: String,
    @ColumnInfo(name = "geometry_profile_id") val geometryProfileId: String,
    @ColumnInfo(name = "geometry_profile_version_id") val geometryProfileVersionId: String,
    @ColumnInfo(name = "location_id") val locationId: String,
    @ColumnInfo(name = "location_code") val locationCode: String,
    @ColumnInfo(name = "location_name") val locationName: String,
    @ColumnInfo(name = "storage_unit_id") val storageUnitId: String,
    @ColumnInfo(name = "storage_unit_code") val storageUnitCode: String,
    @ColumnInfo(name = "storage_unit_name") val storageUnitName: String,
    @ColumnInfo(name = "storage_space_code") val storageSpaceCode: String,
    @ColumnInfo(name = "geometry_family_id") val geometryFamilyId: String,
    @ColumnInfo(name = "geometry_family_code") val geometryFamilyCode: String,
    @ColumnInfo(name = "geometry_family_name") val geometryFamilyName: String,
    @ColumnInfo(name = "geometry_profile_code") val geometryProfileCode: String,
    @ColumnInfo(name = "geometry_profile_name") val geometryProfileName: String,
    @ColumnInfo(name = "geometry_formula_version") val geometryFormulaVersion: String,
    @ColumnInfo(name = "geometry_dimensions") val geometryDimensions: String,
    @ColumnInfo(name = "measurement_reference_height_m") val measurementReferenceHeightM: String,
    @ColumnInfo(name = "maximum_fill_height_m") val maximumFillHeightM: String,
    @ColumnInfo(name = "max_capacity_m3") val maxCapacityM3: String
)

@Entity(tableName = "v6_campaign_snapshot_team_items", foreignKeys = [
        ForeignKey(entity = V6CampaignSnapshotsRow::class, parentColumns = ["campaign_snapshot_id"], childColumns = ["campaign_snapshot_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6UsersRow::class, parentColumns = ["user_id"], childColumns = ["user_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6OrganizationMembershipsRow::class, parentColumns = ["membership_id"], childColumns = ["organization_membership_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["campaign_snapshot_id", "user_id"], unique = true),
        Index(value = ["campaign_snapshot_id"], unique = false),
        Index(value = ["user_id"], unique = false),
        Index(value = ["organization_membership_id"], unique = false)
    ])
data class V6CampaignSnapshotTeamItemsRow(
    @PrimaryKey @ColumnInfo(name = "snapshot_team_item_id") val snapshotTeamItemId: String,
    @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "organization_membership_id") val organizationMembershipId: String,
    @ColumnInfo(name = "role_at_assignment") val roleAtAssignment: String,
    @ColumnInfo(name = "user_full_name") val userFullName: String,
    @ColumnInfo(name = "user_email") val userEmail: String
)

@Entity(tableName = "v6_campaign_snapshots", foreignKeys = [
        ForeignKey(entity = V6InventoryCampaignsRow::class, parentColumns = ["campaign_id", "organization_id"], childColumns = ["campaign_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["campaign_id"], unique = true),
        Index(value = ["campaign_snapshot_id", "organization_id"], unique = true),
        Index(value = ["campaign_id", "organization_id"], unique = false),
        Index(value = ["organization_id"], unique = false)
    ])
data class V6CampaignSnapshotsRow(
    @PrimaryKey @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "campaign_code") val campaignCode: String,
    @ColumnInfo(name = "campaign_name") val campaignName: String,
    @ColumnInfo(name = "inventory_type") val inventoryType: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "planned_end_date") val plannedEndDate: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "calculation_engine_version") val calculationEngineVersion: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "organization_name") val organizationName: String,
    @ColumnInfo(name = "organization_code") val organizationCode: String,
    @ColumnInfo(name = "snapshot_schema_version") val snapshotSchemaVersion: String,
    @ColumnInfo(name = "calculation_schema_versions") val calculationSchemaVersions: String
)

@Entity(tableName = "v6_campaign_team_assignments", foreignKeys = [
        ForeignKey(entity = V6InventoryCampaignsRow::class, parentColumns = ["campaign_id"], childColumns = ["campaign_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6UsersRow::class, parentColumns = ["user_id"], childColumns = ["user_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["campaign_id"], unique = false),
        Index(value = ["user_id"], unique = false)
    ])
data class V6CampaignTeamAssignmentsRow(
    @PrimaryKey @ColumnInfo(name = "assignment_id") val assignmentId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "assigned_at") val assignedAt: Long,
    @ColumnInfo(name = "assigned_by") val assignedBy: String,
    @ColumnInfo(name = "removed_at") val removedAt: Long? = null,
    @ColumnInfo(name = "removed_by") val removedBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_clients", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["client_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false)
    ])
data class V6ClientsRow(
    @PrimaryKey @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "tax_id") val taxId: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_commodities", foreignKeys = [
        ForeignKey(entity = V6CommodityVersionsRow::class, parentColumns = ["commodity_version_id"], childColumns = ["current_version_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["current_version_id"], unique = false)
    ])
data class V6CommoditiesRow(
    @PrimaryKey @ColumnInfo(name = "commodity_id") val commodityId: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "current_version_id") val currentVersionId: String,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String
)

@Entity(tableName = "v6_commodity_versions", foreignKeys = [
        ForeignKey(entity = V6CommoditiesRow::class, parentColumns = ["commodity_id"], childColumns = ["commodity_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["commodity_id", "version_number"], unique = true),
        Index(value = ["commodity_id"], unique = false)
    ])
data class V6CommodityVersionsRow(
    @PrimaryKey @ColumnInfo(name = "commodity_version_id") val commodityVersionId: String,
    @ColumnInfo(name = "commodity_id") val commodityId: String,
    @ColumnInfo(name = "version_number") val versionNumber: Long,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "reference_test_weight_value") val referenceTestWeightValue: String? = null,
    @ColumnInfo(name = "reference_test_weight_unit") val referenceTestWeightUnit: String? = null,
    @ColumnInfo(name = "moisture_reference") val moistureReference: String? = null,
    @ColumnInfo(name = "impurities_reference") val impuritiesReference: String? = null,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "definition_metadata") val definitionMetadata: String? = null
)

@Entity(tableName = "v6_device_installations", foreignKeys = [
        ForeignKey(entity = V6UsersRow::class, parentColumns = ["user_id"], childColumns = ["user_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["user_id"], unique = false)
    ])
data class V6DeviceInstallationsRow(
    @PrimaryKey @ColumnInfo(name = "device_installation_id") val deviceInstallationId: String,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_authorized_at") val lastAuthorizedAt: Long? = null,
    @ColumnInfo(name = "revoked_at") val revokedAt: Long? = null
)

@Entity(tableName = "v6_evidence", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6InventoryCampaignsRow::class, parentColumns = ["campaign_id", "organization_id"], childColumns = ["campaign_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StocktakingRecordsRow::class, parentColumns = ["stocktaking_record_id", "organization_id"], childColumns = ["stocktaking_record_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6IssuesRow::class, parentColumns = ["issue_id", "organization_id"], childColumns = ["issue_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StocktakingRevisionsRow::class, parentColumns = ["stocktaking_revision_id"], childColumns = ["stocktaking_revision_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["evidence_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false),
        Index(value = ["campaign_id", "organization_id"], unique = false),
        Index(value = ["stocktaking_record_id", "organization_id"], unique = false),
        Index(value = ["issue_id", "organization_id"], unique = false),
        Index(value = ["stocktaking_revision_id"], unique = false)
    ])
data class V6EvidenceRow(
    @PrimaryKey @ColumnInfo(name = "evidence_id") val evidenceId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "stocktaking_record_id") val stocktakingRecordId: String? = null,
    @ColumnInfo(name = "issue_id") val issueId: String? = null,
    @ColumnInfo(name = "stocktaking_revision_id") val stocktakingRevisionId: String? = null,
    @ColumnInfo(name = "evidence_type") val evidenceType: String,
    @ColumnInfo(name = "note_text") val noteText: String? = null,
    @ColumnInfo(name = "local_private_ref") val localPrivateRef: String? = null,
    @ColumnInfo(name = "remote_object_ref") val remoteObjectRef: String? = null,
    @ColumnInfo(name = "sha256") val sha256: String? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "byte_size") val byteSize: Long? = null,
    @ColumnInfo(name = "pixel_width") val pixelWidth: Long? = null,
    @ColumnInfo(name = "pixel_height") val pixelHeight: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: String,
    @ColumnInfo(name = "removed_at") val removedAt: Long? = null,
    @ColumnInfo(name = "removed_by") val removedBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_geometry_families")
data class V6GeometryFamiliesRow(
    @PrimaryKey @ColumnInfo(name = "geometry_family_id") val geometryFamilyId: String,
    @ColumnInfo(name = "family_code") val familyCode: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "formula_version") val formulaVersion: String,
    @ColumnInfo(name = "parameter_contract") val parameterContract: String
)

@Entity(tableName = "v6_geometry_profile_versions", foreignKeys = [
        ForeignKey(entity = V6GeometryProfilesRow::class, parentColumns = ["geometry_profile_id"], childColumns = ["geometry_profile_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryFamiliesRow::class, parentColumns = ["geometry_family_id"], childColumns = ["geometry_family_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["geometry_profile_id", "version_number"], unique = true),
        Index(value = ["geometry_profile_id"], unique = false),
        Index(value = ["geometry_family_id"], unique = false)
    ])
data class V6GeometryProfileVersionsRow(
    @PrimaryKey @ColumnInfo(name = "geometry_profile_version_id") val geometryProfileVersionId: String,
    @ColumnInfo(name = "geometry_profile_id") val geometryProfileId: String,
    @ColumnInfo(name = "version_number") val versionNumber: Long,
    @ColumnInfo(name = "geometry_family_id") val geometryFamilyId: String,
    @ColumnInfo(name = "diameter_m") val diameterM: String? = null,
    @ColumnInfo(name = "cylinder_height_m") val cylinderHeightM: String? = null,
    @ColumnInfo(name = "hopper_height_m") val hopperHeightM: String? = null,
    @ColumnInfo(name = "outlet_diameter_m") val outletDiameterM: String? = null,
    @ColumnInfo(name = "side_length_m") val sideLengthM: String? = null,
    @ColumnInfo(name = "length_m") val lengthM: String? = null,
    @ColumnInfo(name = "width_m") val widthM: String? = null,
    @ColumnInfo(name = "body_height_m") val bodyHeightM: String? = null,
    @ColumnInfo(name = "height_m") val heightM: String? = null,
    @ColumnInfo(name = "verified_area_m2") val verifiedAreaM2: String? = null,
    @ColumnInfo(name = "area_provenance") val areaProvenance: String? = null,
    @ColumnInfo(name = "measurement_reference_height_m") val measurementReferenceHeightM: String,
    @ColumnInfo(name = "maximum_fill_height_m") val maximumFillHeightM: String,
    @ColumnInfo(name = "geometry_origin") val geometryOrigin: String,
    @ColumnInfo(name = "max_capacity_m3") val maxCapacityM3: String,
    @ColumnInfo(name = "formula_version") val formulaVersion: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String
)

@Entity(tableName = "v6_geometry_profiles", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryFamiliesRow::class, parentColumns = ["geometry_family_id"], childColumns = ["geometry_family_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryProfileVersionsRow::class, parentColumns = ["geometry_profile_version_id"], childColumns = ["current_version_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["geometry_profile_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false),
        Index(value = ["geometry_family_id"], unique = false),
        Index(value = ["current_version_id"], unique = false)
    ])
data class V6GeometryProfilesRow(
    @PrimaryKey @ColumnInfo(name = "geometry_profile_id") val geometryProfileId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "geometry_family_id") val geometryFamilyId: String,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "current_version_id") val currentVersionId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_inventory_campaigns", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6CampaignSnapshotsRow::class, parentColumns = ["campaign_snapshot_id", "organization_id"], childColumns = ["campaign_snapshot_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["organization_id", "code"], unique = true),
        Index(value = ["organization_id", "campaign_year", "sequence"], unique = true),
        Index(value = ["campaign_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false),
        Index(value = ["campaign_snapshot_id", "organization_id"], unique = false)
    ])
data class V6InventoryCampaignsRow(
    @PrimaryKey @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "campaign_year") val campaignYear: Long,
    @ColumnInfo(name = "sequence") val sequence: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "inventory_type") val inventoryType: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "planned_end_date") val plannedEndDate: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "launched_at") val launchedAt: Long,
    @ColumnInfo(name = "launched_by") val launchedBy: String,
    @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_issue_resolutions", foreignKeys = [
        ForeignKey(entity = V6IssuesRow::class, parentColumns = ["issue_id"], childColumns = ["issue_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["issue_id"], unique = false)
    ])
data class V6IssueResolutionsRow(
    @PrimaryKey @ColumnInfo(name = "issue_resolution_id") val issueResolutionId: String,
    @ColumnInfo(name = "issue_id") val issueId: String,
    @ColumnInfo(name = "resolution_reason") val resolutionReason: String,
    @ColumnInfo(name = "resolution_context") val resolutionContext: String,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long,
    @ColumnInfo(name = "resolved_by") val resolvedBy: String,
    @ColumnInfo(name = "audit_event_id") val auditEventId: String
)

@Entity(tableName = "v6_issue_status_events", foreignKeys = [
        ForeignKey(entity = V6IssuesRow::class, parentColumns = ["issue_id"], childColumns = ["issue_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["issue_id"], unique = false)
    ])
data class V6IssueStatusEventsRow(
    @PrimaryKey @ColumnInfo(name = "issue_status_event_id") val issueStatusEventId: String,
    @ColumnInfo(name = "issue_id") val issueId: String,
    @ColumnInfo(name = "from_status") val fromStatus: String? = null,
    @ColumnInfo(name = "to_status") val toStatus: String,
    @ColumnInfo(name = "reason") val reason: String? = null,
    @ColumnInfo(name = "changed_at") val changedAt: Long,
    @ColumnInfo(name = "changed_by") val changedBy: String,
    @ColumnInfo(name = "audit_event_id") val auditEventId: String
)

@Entity(tableName = "v6_issues", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6InventoryCampaignsRow::class, parentColumns = ["campaign_id", "organization_id"], childColumns = ["campaign_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StocktakingRecordsRow::class, parentColumns = ["stocktaking_record_id", "organization_id"], childColumns = ["stocktaking_record_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["issue_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false),
        Index(value = ["campaign_id", "organization_id"], unique = false),
        Index(value = ["stocktaking_record_id", "organization_id"], unique = false)
    ])
data class V6IssuesRow(
    @PrimaryKey @ColumnInfo(name = "issue_id") val issueId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "stocktaking_record_id") val stocktakingRecordId: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "reported_at") val reportedAt: Long,
    @ColumnInfo(name = "reported_by") val reportedBy: String,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long? = null,
    @ColumnInfo(name = "resolved_by") val resolvedBy: String? = null,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_locations", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["location_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false)
    ])
data class V6LocationsRow(
    @PrimaryKey @ColumnInfo(name = "location_id") val locationId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "city") val city: String? = null,
    @ColumnInfo(name = "country") val country: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "latitude_deg") val latitudeDeg: String? = null,
    @ColumnInfo(name = "longitude_deg") val longitudeDeg: String? = null,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_measurements", foreignKeys = [
        ForeignKey(entity = V6StocktakingRevisionsRow::class, parentColumns = ["stocktaking_revision_id"], childColumns = ["stocktaking_revision_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryProfileVersionsRow::class, parentColumns = ["geometry_profile_version_id"], childColumns = ["geometry_profile_version_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["stocktaking_revision_id", "sequence_number"], unique = true),
        Index(value = ["stocktaking_revision_id"], unique = false),
        Index(value = ["geometry_profile_version_id"], unique = false)
    ])
data class V6MeasurementsRow(
    @PrimaryKey @ColumnInfo(name = "measurement_id") val measurementId: String,
    @ColumnInfo(name = "stocktaking_revision_id") val stocktakingRevisionId: String,
    @ColumnInfo(name = "sequence_number") val sequenceNumber: Long,
    @ColumnInfo(name = "raw_value") val rawValue: String,
    @ColumnInfo(name = "raw_unit") val rawUnit: String,
    @ColumnInfo(name = "normalized_ullage_m") val normalizedUllageM: String,
    @ColumnInfo(name = "datum_metadata") val datumMetadata: String,
    @ColumnInfo(name = "geometry_profile_version_id") val geometryProfileVersionId: String,
    @ColumnInfo(name = "captured_at") val capturedAt: Long,
    @ColumnInfo(name = "captured_by") val capturedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_organization_memberships", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6UsersRow::class, parentColumns = ["user_id"], childColumns = ["user_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["organization_id", "user_id"], unique = true),
        Index(value = ["membership_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false),
        Index(value = ["user_id"], unique = false)
    ])
data class V6OrganizationMembershipsRow(
    @PrimaryKey @ColumnInfo(name = "membership_id") val membershipId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "disabled_at") val disabledAt: Long? = null,
    @ColumnInfo(name = "disabled_by") val disabledBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_organizations")
data class V6OrganizationsRow(
    @PrimaryKey @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "city") val city: String? = null,
    @ColumnInfo(name = "country") val country: String? = null,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_stocktaking_records", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6InventoryCampaignsRow::class, parentColumns = ["campaign_id", "organization_id"], childColumns = ["campaign_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StorageSpacesRow::class, parentColumns = ["storage_space_id", "organization_id"], childColumns = ["storage_space_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StocktakingRevisionsRow::class, parentColumns = ["stocktaking_revision_id"], childColumns = ["current_revision_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["campaign_id", "storage_space_id"], unique = true),
        Index(value = ["stocktaking_record_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false),
        Index(value = ["campaign_id", "organization_id"], unique = false),
        Index(value = ["storage_space_id", "organization_id"], unique = false),
        Index(value = ["current_revision_id"], unique = false)
    ])
data class V6StocktakingRecordsRow(
    @PrimaryKey @ColumnInfo(name = "stocktaking_record_id") val stocktakingRecordId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "storage_space_id") val storageSpaceId: String,
    @ColumnInfo(name = "current_revision_id") val currentRevisionId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_stocktaking_revisions", foreignKeys = [
        ForeignKey(entity = V6StocktakingRecordsRow::class, parentColumns = ["stocktaking_record_id"], childColumns = ["stocktaking_record_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StocktakingRevisionsRow::class, parentColumns = ["stocktaking_revision_id"], childColumns = ["based_on_revision_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6CommodityVersionsRow::class, parentColumns = ["commodity_version_id"], childColumns = ["commodity_version_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6ClientsRow::class, parentColumns = ["client_id"], childColumns = ["client_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6CalculationResultsRow::class, parentColumns = ["calculation_result_id"], childColumns = ["calculation_result_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["stocktaking_record_id", "revision_number"], unique = true),
        Index(value = ["stocktaking_record_id"], unique = false),
        Index(value = ["based_on_revision_id"], unique = false),
        Index(value = ["commodity_version_id"], unique = false),
        Index(value = ["client_id"], unique = false),
        Index(value = ["calculation_result_id"], unique = false)
    ])
data class V6StocktakingRevisionsRow(
    @PrimaryKey @ColumnInfo(name = "stocktaking_revision_id") val stocktakingRevisionId: String,
    @ColumnInfo(name = "stocktaking_record_id") val stocktakingRecordId: String,
    @ColumnInfo(name = "revision_number") val revisionNumber: Long,
    @ColumnInfo(name = "revision_state") val revisionState: String,
    @ColumnInfo(name = "based_on_revision_id") val basedOnRevisionId: String? = null,
    @ColumnInfo(name = "is_empty") val isEmpty: Boolean,
    @ColumnInfo(name = "commodity_version_id") val commodityVersionId: String? = null,
    @ColumnInfo(name = "actual_bulk_density_kg_m3") val actualBulkDensityKgM3: String? = null,
    @ColumnInfo(name = "ownership") val ownership: String? = null,
    @ColumnInfo(name = "client_id") val clientId: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "calculation_result_id") val calculationResultId: String? = null,
    @ColumnInfo(name = "validated_at") val validatedAt: Long? = null,
    @ColumnInfo(name = "validated_by") val validatedBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_storage_spaces", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6StorageUnitsRow::class, parentColumns = ["storage_unit_id", "organization_id"], childColumns = ["storage_unit_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6GeometryProfilesRow::class, parentColumns = ["geometry_profile_id", "organization_id"], childColumns = ["geometry_profile_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["storage_space_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false),
        Index(value = ["storage_unit_id", "organization_id"], unique = false),
  Index(value = ["storage_unit_id", "code"], unique = true),
        Index(value = ["geometry_profile_id", "organization_id"], unique = false)
    ])
data class V6StorageSpacesRow(
    @PrimaryKey @ColumnInfo(name = "storage_space_id") val storageSpaceId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "storage_unit_id") val storageUnitId: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "geometry_profile_id") val geometryProfileId: String,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_storage_units", foreignKeys = [
        ForeignKey(entity = V6OrganizationsRow::class, parentColumns = ["organization_id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true),
        ForeignKey(entity = V6LocationsRow::class, parentColumns = ["location_id", "organization_id"], childColumns = ["location_id", "organization_id"], onDelete = ForeignKey.RESTRICT, deferred = true)
    ], indices = [
        Index(value = ["storage_unit_id", "organization_id"], unique = true),
        Index(value = ["organization_id"], unique = false),
        Index(value = ["location_id", "organization_id"], unique = false)
    ])
data class V6StorageUnitsRow(
    @PrimaryKey @ColumnInfo(name = "storage_unit_id") val storageUnitId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "location_id") val locationId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)

@Entity(tableName = "v6_users")
data class V6UsersRow(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "email_normalized") val emailNormalized: String,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long
)
