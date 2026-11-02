package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Additive V6 Room baseline entities introduced while legacy tables remain in
 * controlled EXPAND/MIGRATE state. These tables use the frozen Room physical
 * mappings from Document 10: UUID/LocalDate/decimal/structured values are TEXT,
 * instants/counters are INTEGER/Long, and no binary Evidence payload is stored.
 */

@Entity(
    tableName = "commodity_versions",
    indices = [Index(value = ["commodity_id", "version_number"], unique = true)]
)
data class CommodityVersionRoomEntity(
    @PrimaryKey @ColumnInfo(name = "commodity_version_id") val commodityVersionId: String,
    @ColumnInfo(name = "commodity_id") val commodityId: String,
    @ColumnInfo(name = "version_number") val versionNumber: Long,
    @ColumnInfo(name = "display_name") val displayName: String,
    val category: String,
    @ColumnInfo(name = "reference_test_weight_value") val referenceTestWeightValue: String? = null,
    @ColumnInfo(name = "reference_test_weight_unit") val referenceTestWeightUnit: String? = null,
    @ColumnInfo(name = "moisture_reference") val moistureReference: String? = null,
    @ColumnInfo(name = "impurities_reference") val impuritiesReference: String? = null,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String
)

@Entity(
    tableName = "campaign_snapshots",
    indices = [Index(value = ["campaign_id"], unique = true)]
)
data class CampaignSnapshotRoomEntity(
    @PrimaryKey @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "organization_name") val organizationName: String,
    @ColumnInfo(name = "organization_code") val organizationCode: String,
    @ColumnInfo(name = "campaign_code") val campaignCode: String,
    @ColumnInfo(name = "campaign_name") val campaignName: String,
    @ColumnInfo(name = "inventory_type") val inventoryType: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "planned_end_date") val plannedEndDate: String? = null,
    @ColumnInfo(name = "snapshot_schema_version") val snapshotSchemaVersion: String,
    @ColumnInfo(name = "calculation_schema_versions_json") val calculationSchemaVersionsJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String
)

@Entity(
    tableName = "campaign_snapshot_scope_items",
    indices = [
        Index("campaign_snapshot_id"),
        Index(value = ["campaign_snapshot_id", "storage_space_id"], unique = true)
    ]
)
data class CampaignSnapshotScopeItemRoomEntity(
    @PrimaryKey @ColumnInfo(name = "snapshot_scope_item_id") val snapshotScopeItemId: String,
    @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String,
    @ColumnInfo(name = "storage_space_id") val storageSpaceId: String,
    @ColumnInfo(name = "location_id") val locationId: String,
    @ColumnInfo(name = "location_code") val locationCode: String,
    @ColumnInfo(name = "location_name") val locationName: String,
    @ColumnInfo(name = "storage_unit_id") val storageUnitId: String,
    @ColumnInfo(name = "storage_unit_code") val storageUnitCode: String,
    @ColumnInfo(name = "storage_unit_name") val storageUnitName: String,
    @ColumnInfo(name = "storage_space_code") val storageSpaceCode: String,
    @ColumnInfo(name = "geometry_profile_id") val geometryProfileId: String,
    @ColumnInfo(name = "geometry_profile_version_id") val geometryProfileVersionId: String,
    @ColumnInfo(name = "geometry_family_id") val geometryFamilyId: String,
    @ColumnInfo(name = "geometry_family_code") val geometryFamilyCode: String,
    @ColumnInfo(name = "geometry_family_name") val geometryFamilyName: String,
    @ColumnInfo(name = "geometry_profile_code") val geometryProfileCode: String,
    @ColumnInfo(name = "geometry_profile_name") val geometryProfileName: String,
    @ColumnInfo(name = "geometry_formula_version") val geometryFormulaVersion: String,
    @ColumnInfo(name = "geometry_dimensions_json") val geometryDimensionsJson: String,
    @ColumnInfo(name = "measurement_reference_height_m") val measurementReferenceHeightM: String,
    @ColumnInfo(name = "maximum_fill_height_m") val maximumFillHeightM: String,
    @ColumnInfo(name = "max_capacity_m3") val maxCapacityM3: String
)

@Entity(
    tableName = "campaign_snapshot_team_items",
    indices = [
        Index("campaign_snapshot_id"),
        Index(value = ["campaign_snapshot_id", "user_id"], unique = true)
    ]
)
data class CampaignSnapshotTeamItemRoomEntity(
    @PrimaryKey @ColumnInfo(name = "snapshot_team_item_id") val snapshotTeamItemId: String,
    @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "organization_membership_id") val organizationMembershipId: String,
    @ColumnInfo(name = "role_at_assignment") val roleAtAssignment: String,
    @ColumnInfo(name = "user_full_name") val userFullName: String,
    @ColumnInfo(name = "user_email") val userEmail: String
)

@Entity(
    tableName = "campaign_snapshot_commodity_items",
    indices = [
        Index("campaign_snapshot_id"),
        Index(value = ["campaign_snapshot_id", "commodity_id", "commodity_version_id"], unique = true)
    ]
)
data class CampaignSnapshotCommodityItemRoomEntity(
    @PrimaryKey @ColumnInfo(name = "snapshot_commodity_item_id") val snapshotCommodityItemId: String,
    @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String,
    @ColumnInfo(name = "commodity_id") val commodityId: String,
    @ColumnInfo(name = "commodity_version_id") val commodityVersionId: String
)

@Entity(
    tableName = "offline_working_set_manifests",
    indices = [Index(value = ["campaign_id", "device_installation_id", "package_version"], unique = true)]
)
data class OfflineWorkingSetManifestRoomEntity(
    @PrimaryKey @ColumnInfo(name = "manifest_id") val manifestId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "device_installation_id") val deviceInstallationId: String,
    @ColumnInfo(name = "package_version") val packageVersion: String,
    @ColumnInfo(name = "content_manifest_json") val contentManifestJson: String,
    @ColumnInfo(name = "integrity_metadata_json") val integrityMetadataJson: String,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    @ColumnInfo(name = "downloaded_at") val downloadedAt: Long? = null,
    @ColumnInfo(name = "verified_at") val verifiedAt: Long? = null,
    @ColumnInfo(name = "is_current") val isCurrent: Boolean
)

@Entity(
    tableName = "stocktaking_revisions",
    indices = [Index(value = ["stocktaking_record_id", "revision_number"], unique = true)]
)
data class StocktakingRevisionRoomEntity(
    @PrimaryKey @ColumnInfo(name = "stocktaking_revision_id") val stocktakingRevisionId: String,
    @ColumnInfo(name = "stocktaking_record_id") val stocktakingRecordId: String,
    @ColumnInfo(name = "revision_number") val revisionNumber: Long,
    @ColumnInfo(name = "revision_state") val revisionState: String,
    @ColumnInfo(name = "based_on_revision_id") val basedOnRevisionId: String? = null,
    @ColumnInfo(name = "is_empty") val isEmpty: Boolean,
    @ColumnInfo(name = "commodity_version_id") val commodityVersionId: String? = null,
    @ColumnInfo(name = "actual_bulk_density_kg_m3") val actualBulkDensityKgM3: String? = null,
    val ownership: String? = null,
    @ColumnInfo(name = "client_id") val clientId: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "calculation_result_id") val calculationResultId: String? = null,
    @ColumnInfo(name = "validated_at") val validatedAt: Long? = null,
    @ColumnInfo(name = "validated_by") val validatedBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long = 1
)

@Entity(tableName = "issue_resolutions", indices = [Index("issue_id")])
data class IssueResolutionRoomEntity(
    @PrimaryKey @ColumnInfo(name = "issue_resolution_id") val issueResolutionId: String,
    @ColumnInfo(name = "issue_id") val issueId: String,
    @ColumnInfo(name = "resolution_reason") val resolutionReason: String,
    @ColumnInfo(name = "resolution_context") val resolutionContext: String,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long,
    @ColumnInfo(name = "resolved_by") val resolvedBy: String,
    @ColumnInfo(name = "audit_event_id") val auditEventId: String
)

@Entity(tableName = "issue_status_events", indices = [Index("issue_id")])
data class IssueStatusEventRoomEntity(
    @PrimaryKey @ColumnInfo(name = "issue_status_event_id") val issueStatusEventId: String,
    @ColumnInfo(name = "issue_id") val issueId: String,
    @ColumnInfo(name = "from_status") val fromStatus: String? = null,
    @ColumnInfo(name = "to_status") val toStatus: String,
    val reason: String? = null,
    @ColumnInfo(name = "changed_at") val changedAt: Long,
    @ColumnInfo(name = "changed_by") val changedBy: String,
    @ColumnInfo(name = "audit_event_id") val auditEventId: String
)

@Entity(
    tableName = "evidence",
    indices = [
        Index(value = ["stocktaking_record_id", "stocktaking_revision_id", "evidence_type"]),
        Index(value = ["issue_id", "evidence_type"]),
        Index("sync_state")
    ]
)
data class EvidenceRoomEntity(
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
    val sha256: String? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "byte_size") val byteSize: Long? = null,
    @ColumnInfo(name = "pixel_width") val pixelWidth: Long? = null,
    @ColumnInfo(name = "pixel_height") val pixelHeight: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: String,
    @ColumnInfo(name = "removed_at") val removedAt: Long? = null,
    @ColumnInfo(name = "removed_by") val removedBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long = 1
)

@Entity(
    tableName = "final_inventory_snapshots",
    indices = [
        Index(value = ["campaign_id"], unique = true),
        Index(value = ["organization_id", "closed_at"])
    ]
)
data class FinalInventorySnapshotRoomEntity(
    @PrimaryKey @ColumnInfo(name = "final_snapshot_id") val finalSnapshotId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "campaign_snapshot_id") val campaignSnapshotId: String,
    @ColumnInfo(name = "closed_at") val closedAt: Long,
    @ColumnInfo(name = "closed_by") val closedBy: String,
    @ColumnInfo(name = "snapshot_schema_version") val snapshotSchemaVersion: String,
    @ColumnInfo(name = "calculation_compatibility_versions_json") val calculationCompatibilityVersionsJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String
)

@Entity(
    tableName = "final_inventory_snapshot_entries",
    indices = [Index(value = ["final_snapshot_id", "storage_space_id"], unique = true)]
)
data class FinalInventorySnapshotEntryRoomEntity(
    @PrimaryKey @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "final_snapshot_id") val finalSnapshotId: String,
    @ColumnInfo(name = "storage_space_id") val storageSpaceId: String,
    @ColumnInfo(name = "hierarchy_snapshot_json") val hierarchySnapshotJson: String,
    @ColumnInfo(name = "geometry_snapshot_json") val geometrySnapshotJson: String,
    @ColumnInfo(name = "commodity_snapshot_json") val commoditySnapshotJson: String? = null,
    @ColumnInfo(name = "ownership_client_snapshot_json") val ownershipClientSnapshotJson: String? = null,
    @ColumnInfo(name = "is_empty") val isEmpty: Boolean,
    @ColumnInfo(name = "measurements_snapshot_json") val measurementsSnapshotJson: String? = null,
    @ColumnInfo(name = "operational_values_json") val operationalValuesJson: String,
    @ColumnInfo(name = "calculation_metadata_json") val calculationMetadataJson: String,
    @ColumnInfo(name = "validation_metadata_json") val validationMetadataJson: String,
    @ColumnInfo(name = "issue_references_resolution_summary_json") val issueReferencesResolutionSummaryJson: String,
    @ColumnInfo(name = "evidence_references_json") val evidenceReferencesJson: String
)

@Entity(
    tableName = "sync_operation_dependencies",
    indices = [
        Index(value = ["parent_operation_id", "dependent_operation_id"], unique = true),
        Index("dependent_operation_id")
    ]
)
data class SyncOperationDependencyRoomEntity(
    @PrimaryKey @ColumnInfo(name = "dependency_id") val dependencyId: String,
    @ColumnInfo(name = "parent_operation_id") val parentOperationId: String,
    @ColumnInfo(name = "dependent_operation_id") val dependentOperationId: String,
    @ColumnInfo(name = "dependency_type") val dependencyType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "sync_conflicts", indices = [Index("sync_operation_id"), Index("status")])
data class SyncConflictRoomEntity(
    @PrimaryKey @ColumnInfo(name = "sync_conflict_id") val syncConflictId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "sync_operation_id") val syncOperationId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "local_attempt_json") val localAttemptJson: String,
    @ColumnInfo(name = "server_value_json") val serverValueJson: String,
    @ColumnInfo(name = "base_entity_version") val baseEntityVersion: Long? = null,
    @ColumnInfo(name = "server_entity_version") val serverEntityVersion: Long? = null,
    @ColumnInfo(name = "detected_at") val detectedAt: Long,
    val status: String,
    @ColumnInfo(name = "resolution_type") val resolutionType: String? = null,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long? = null,
    @ColumnInfo(name = "resolved_by") val resolvedBy: String? = null
)

@Entity(
    tableName = "audit_events",
    indices = [
        Index(value = ["organization_id", "occurred_at"]),
        Index(value = ["entity_type", "entity_id", "occurred_at"])
    ]
)
data class AuditEventRoomEntity(
    @PrimaryKey @ColumnInfo(name = "audit_event_id") val auditEventId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String? = null,
    @ColumnInfo(name = "actor_user_id") val actorUserId: String? = null,
    val action: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "before_data_json") val beforeDataJson: String? = null,
    @ColumnInfo(name = "after_data_json") val afterDataJson: String? = null,
    @ColumnInfo(name = "change_metadata_json") val changeMetadataJson: String? = null,
    @ColumnInfo(name = "correlation_id") val correlationId: String,
    val source: String,
    @ColumnInfo(name = "system_actor") val systemActor: String? = null
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["recipient_user_id", "read_at", "created_at"])]
)
data class NotificationRoomEntity(
    @PrimaryKey @ColumnInfo(name = "notification_id") val notificationId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "recipient_user_id") val recipientUserId: String,
    @ColumnInfo(name = "notification_type") val notificationType: String,
    val title: String,
    val body: String,
    @ColumnInfo(name = "context_json") val contextJson: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "read_at") val readAt: Long? = null
)

@Entity(tableName = "user_preferences", indices = [Index(value = ["user_id"], unique = true)])
data class UserPreferenceRoomEntity(
    @PrimaryKey @ColumnInfo(name = "user_preference_id") val userPreferenceId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val theme: String,
    @ColumnInfo(name = "notification_preferences_json") val notificationPreferencesJson: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "organization_configurations",
    indices = [Index(value = ["organization_id", "configuration_key"], unique = true)]
)
data class OrganizationConfigurationRoomEntity(
    @PrimaryKey @ColumnInfo(name = "organization_configuration_id") val organizationConfigurationId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "configuration_key") val configurationKey: String,
    @ColumnInfo(name = "value_type") val valueType: String,
    @ColumnInfo(name = "boolean_value") val booleanValue: Boolean? = null,
    @ColumnInfo(name = "integer_value") val integerValue: Long? = null,
    @ColumnInfo(name = "decimal_value") val decimalValue: String? = null,
    @ColumnInfo(name = "text_value") val textValue: String? = null,
    @ColumnInfo(name = "enum_value") val enumValue: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "entity_version") val entityVersion: Long = 1
)
