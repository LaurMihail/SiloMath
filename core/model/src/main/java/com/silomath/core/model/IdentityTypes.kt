package com.silomath.core.model

import java.util.UUID
import kotlinx.serialization.Serializable

private fun requireCanonicalUuid(value: String) {
    require(value.isNotBlank()) { "Identity UUID must not be blank" }
    val parsed = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Identity must be a valid UUID: $value")
    }
    require(parsed.toString() == value) {
        "Identity UUID must use canonical lowercase representation: $value"
    }
}

@JvmInline
@Serializable
value class UserId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class OrganizationId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class OrganizationMembershipId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class LocationId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class StorageUnitId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class StorageSpaceId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class ClientId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class GeometryProfileId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class GeometryProfileVersionId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class CommodityId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class CommodityVersionId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class CampaignId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class CampaignScopeId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class CampaignTeamAssignmentId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class CampaignSnapshotId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class OfflineWorkingSetManifestId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class StocktakingRecordId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class StocktakingRevisionId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class MeasurementId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class CalculationResultId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class IssueId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class IssueResolutionId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class IssueStatusEventId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class EvidenceId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class FinalSnapshotId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class SnapshotEntryId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class SyncOperationId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class SyncConflictId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class AuditEventId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class NotificationId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class DeviceInstallationId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}

@JvmInline
@Serializable
value class SyncDependencyId(val value: String) {
    init { requireCanonicalUuid(value) }
    override fun toString(): String = value
}
