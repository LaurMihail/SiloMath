package com.silomath.core.domain.model

import com.silomath.core.model.CalculationResultId
import com.silomath.core.model.CampaignId
import com.silomath.core.model.CampaignSnapshotId
import com.silomath.core.model.ClientId
import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import com.silomath.core.model.EvidenceId
import com.silomath.core.model.FinalSnapshotId
import com.silomath.core.model.GeometryProfileId
import com.silomath.core.model.GeometryProfileVersionId
import com.silomath.core.model.IssueId
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.SnapshotEntryId
import com.silomath.core.model.StocktakingRevisionId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.StorageUnitId
import com.silomath.core.model.UserId
import com.silomath.core.model.CanonicalDecimal
import java.time.Instant

private fun requireFrozenText(label: String, value: String) {
    require(value.isNotBlank()) { "$label must not be blank" }
    require(value == value.trim()) { "$label must be trimmed" }
}

data class FinalInventorySnapshot(
    val finalSnapshotId: FinalSnapshotId,
    val organizationId: OrganizationId,
    val campaignId: CampaignId,
    val campaignSnapshotId: CampaignSnapshotId,
    val closedAt: Instant,
    val closedBy: UserId,
    val snapshotSchemaVersion: String,
    val calculationCompatibilityVersions: Set<String>,
    val createdAt: Instant,
    val createdBy: UserId
) {
    init {
        requireFrozenText("FinalInventorySnapshot snapshotSchemaVersion", snapshotSchemaVersion)
        require(calculationCompatibilityVersions.isNotEmpty()) {
            "FinalInventorySnapshot requires calculation compatibility versions"
        }
        calculationCompatibilityVersions.forEach {
            requireFrozenText("Calculation compatibility version", it)
        }
        require(!createdAt.isBefore(closedAt)) {
            "FinalInventorySnapshot createdAt must not be before closedAt"
        }
    }
}

data class FrozenHierarchySnapshot(
    val locationId: LocationId,
    val locationCode: String,
    val locationName: String,
    val storageUnitId: StorageUnitId,
    val storageUnitCode: String,
    val storageUnitName: String,
    val storageSpaceCode: String,
    val storageSpaceName: String
) {
    init {
        requireFrozenText("Frozen hierarchy locationCode", locationCode)
        requireFrozenText("Frozen hierarchy locationName", locationName)
        requireFrozenText("Frozen hierarchy storageUnitCode", storageUnitCode)
        requireFrozenText("Frozen hierarchy storageUnitName", storageUnitName)
        requireFrozenText("Frozen hierarchy storageSpaceCode", storageSpaceCode)
        requireFrozenText("Frozen hierarchy storageSpaceName", storageSpaceName)
    }
}

data class FrozenGeometrySnapshot(
    val geometryProfileId: GeometryProfileId,
    val geometryProfileVersionId: GeometryProfileVersionId,
    val geometryFamilyCode: String,
    val formulaVersion: String,
    val dimensionsM: Map<String, CanonicalDecimal>,
    val maximumFillHeightM: CanonicalDecimal,
    val capacityM3: CanonicalDecimal
) {
    init {
        requireFrozenText("Frozen geometry family code", geometryFamilyCode)
        requireFrozenText("Frozen geometry formula version", formulaVersion)
        require(dimensionsM.isNotEmpty()) { "Frozen geometry requires dimensions" }
        require(dimensionsM.keys.all { it.isNotBlank() && it == it.trim() }) {
            "Frozen geometry dimension keys must be non-blank and trimmed"
        }
        require(dimensionsM.values.all { it.value.signum() > 0 }) {
            "Frozen geometry dimensions must be > 0"
        }
        require(maximumFillHeightM.value.signum() > 0) {
            "Frozen geometry maximumFillHeightM must be > 0"
        }
        require(capacityM3.value.signum() > 0) {
            "Frozen geometry capacityM3 must be > 0"
        }
    }
}

data class FrozenCommoditySnapshot(
    val commodityId: CommodityId,
    val commodityVersionId: CommodityVersionId,
    val commodityCode: String,
    val commodityDisplayName: String
) {
    init {
        requireFrozenText("Frozen commodity code", commodityCode)
        requireFrozenText("Frozen commodity display name", commodityDisplayName)
    }
}

data class FrozenOwnershipClientSnapshot(
    val ownership: Ownership,
    val clientId: ClientId?,
    val clientCode: String?,
    val clientName: String?
) {
    init {
        when (ownership) {
            Ownership.OWNED -> require(clientId == null && clientCode == null && clientName == null) {
                "OWNED final snapshot must not fabricate Client data"
            }
            Ownership.THIRD_PARTY -> {
                require(clientId != null) { "THIRD_PARTY final snapshot requires clientId" }
                require(clientCode != null && clientName != null) {
                    "THIRD_PARTY final snapshot requires frozen Client Code and Name"
                }
                requireFrozenText("Frozen client code", clientCode)
                requireFrozenText("Frozen client name", clientName)
            }
        }
    }
}

data class FrozenMeasurementSnapshot(
    val sequenceNumber: Int,
    val rawValue: CanonicalDecimal,
    val rawUnit: String,
    val normalizedUllageM: CanonicalDecimal,
    val datumMetadata: MeasurementDatum,
    val geometryProfileVersionId: GeometryProfileVersionId
) {
    init {
        require(sequenceNumber in 1..5) { "Frozen measurement sequenceNumber must be in 1..5" }
        require(rawValue.value.signum() >= 0) { "Frozen measurement rawValue must be >= 0" }
        requireFrozenText("Frozen measurement rawUnit", rawUnit)
        require(normalizedUllageM.value.signum() >= 0) {
            "Frozen measurement normalizedUllageM must be >= 0"
        }
        require(normalizedUllageM.value <= datumMetadata.measurementReferenceHeightM.value) {
            "Frozen measurement normalizedUllageM must not exceed frozen reference height"
        }
    }
}

data class FrozenOperationalValues(
    val effectiveUllageM: CanonicalDecimal?,
    val effectiveFillHeightM: CanonicalDecimal?,
    val actualBulkDensityKgM3: CanonicalDecimal?,
    val productVolumeM3: CanonicalDecimal,
    val physicalQuantityKg: CanonicalDecimal
) {
    init {
        require(productVolumeM3.value.signum() >= 0) { "Frozen productVolumeM3 must be >= 0" }
        require(physicalQuantityKg.value.signum() >= 0) { "Frozen physicalQuantityKg must be >= 0" }
        require(effectiveUllageM == null || effectiveUllageM.value.signum() >= 0) {
            "Frozen effectiveUllageM must be >= 0"
        }
        require(effectiveFillHeightM == null || effectiveFillHeightM.value.signum() >= 0) {
            "Frozen effectiveFillHeightM must be >= 0"
        }
        require(actualBulkDensityKgM3 == null || actualBulkDensityKgM3.value.signum() > 0) {
            "Frozen actualBulkDensityKgM3 must be > 0 when present"
        }
    }
}

data class FrozenCalculationMetadata(
    val calculationResultId: CalculationResultId?,
    val engineVersion: String,
    val aggregationMethod: String,
    val aggregationRuleVersion: Int,
    val formulaVersion: String?,
    val inputFingerprint: String?,
    val roundingPolicy: String
) {
    init {
        requireFrozenText("Frozen calculation engineVersion", engineVersion)
        requireFrozenText("Frozen calculation aggregationMethod", aggregationMethod)
        require(aggregationRuleVersion > 0) { "Frozen calculation aggregationRuleVersion must be > 0" }
        requireFrozenText("Frozen calculation roundingPolicy", roundingPolicy)
        if (calculationResultId != null) {
            require(formulaVersion != null && inputFingerprint != null) {
                "Frozen CalculationResult requires formulaVersion and inputFingerprint"
            }
            requireFrozenText("Frozen calculation formulaVersion", formulaVersion)
            requireFrozenText("Frozen calculation inputFingerprint", inputFingerprint)
        }
    }
}

data class FrozenValidationMetadata(
    val stocktakingRevisionId: StocktakingRevisionId,
    val revisionNumber: Int,
    val validatedAt: Instant,
    val validatedBy: UserId
) {
    init {
        require(revisionNumber >= 1) { "Frozen validation revisionNumber must be >= 1" }
    }
}

data class FrozenIssueReference(
    val issueId: IssueId,
    val category: IssueCategory,
    val finalStatus: IssueStatus,
    val resolutionReason: String?,
    val resolutionContext: IssueResolutionContext?,
    val resolvedAt: Instant?,
    val resolvedBy: UserId?
) {
    init {
        when (finalStatus) {
            IssueStatus.OPEN -> require(
                resolutionReason == null && resolutionContext == null &&
                    resolvedAt == null && resolvedBy == null
            ) { "OPEN frozen Issue reference must not carry effective resolution metadata" }
            IssueStatus.RESOLVED -> {
                require(
                    resolutionReason != null && resolutionContext != null &&
                        resolvedAt != null && resolvedBy != null
                ) { "RESOLVED frozen Issue reference requires resolution metadata" }
                requireFrozenText("Frozen Issue resolution reason", resolutionReason)
            }
        }
    }
}

data class FrozenEvidenceReference(
    val evidenceId: EvidenceId,
    val evidenceType: EvidenceType,
    val noteText: String?,
    val sha256: String?,
    val remoteObjectRef: String?
) {
    init {
        when (evidenceType) {
            EvidenceType.NOTE -> {
                require(noteText != null) { "Frozen NOTE reference requires noteText" }
                requireFrozenText("Frozen NOTE text", noteText)
                require(sha256 == null && remoteObjectRef == null) {
                    "Frozen NOTE reference must not carry binary metadata"
                }
            }
            EvidenceType.PHOTO -> {
                require(noteText == null) { "Frozen PHOTO reference must not carry noteText" }
                require(sha256 != null && remoteObjectRef != null) {
                    "Frozen PHOTO reference requires hash and object reference"
                }
                require(sha256.matches(Regex("^[0-9a-f]{64}$"))) {
                    "Frozen PHOTO sha256 must be lowercase SHA-256"
                }
                requireFrozenText("Frozen PHOTO remoteObjectRef", remoteObjectRef)
            }
        }
    }
}

data class FinalInventorySnapshotEntry(
    val snapshotEntryId: SnapshotEntryId,
    val finalSnapshotId: FinalSnapshotId,
    val storageSpaceId: StorageSpaceId,
    val hierarchySnapshot: FrozenHierarchySnapshot,
    val geometrySnapshot: FrozenGeometrySnapshot,
    val commoditySnapshot: FrozenCommoditySnapshot?,
    val ownershipClientSnapshot: FrozenOwnershipClientSnapshot?,
    val isEmpty: Boolean,
    val measurementsSnapshot: List<FrozenMeasurementSnapshot>,
    val operationalValues: FrozenOperationalValues,
    val calculationMetadata: FrozenCalculationMetadata,
    val validationMetadata: FrozenValidationMetadata,
    val issueReferences: List<FrozenIssueReference>,
    val evidenceReferences: List<FrozenEvidenceReference>
) {
    init {
        require(measurementsSnapshot.map { it.sequenceNumber }.distinct().size == measurementsSnapshot.size) {
            "Frozen measurement sequence numbers must be unique"
        }
        require(issueReferences.map { it.issueId }.distinct().size == issueReferences.size) {
            "Frozen Issue references must be unique"
        }
        require(evidenceReferences.map { it.evidenceId }.distinct().size == evidenceReferences.size) {
            "Frozen Evidence references must be unique"
        }

        if (isEmpty) {
            require(commoditySnapshot == null) { "Empty final snapshot entry must not fabricate Commodity" }
            require(ownershipClientSnapshot == null) { "Empty final snapshot entry must not fabricate Ownership/Client" }
            require(measurementsSnapshot.isEmpty()) { "Empty final snapshot entry must have no measurements" }
            require(operationalValues.actualBulkDensityKgM3 == null) {
                "Empty final snapshot entry must not fabricate density"
            }
            require(operationalValues.effectiveUllageM == null && operationalValues.effectiveFillHeightM == null) {
                "Empty final snapshot entry must not fabricate effective measurement values"
            }
            require(operationalValues.productVolumeM3.value.signum() == 0) {
                "Empty final snapshot entry must freeze zero product volume"
            }
            require(operationalValues.physicalQuantityKg.value.signum() == 0) {
                "Empty final snapshot entry must freeze zero physical quantity"
            }
        } else {
            require(commoditySnapshot != null) { "Non-empty final snapshot entry requires Commodity snapshot" }
            require(ownershipClientSnapshot != null) { "Non-empty final snapshot entry requires Ownership/Client snapshot" }
            require(measurementsSnapshot.size in 1..5) {
                "Non-empty final snapshot entry requires 1..5 frozen measurements"
            }
            require(operationalValues.actualBulkDensityKgM3 != null) {
                "Non-empty final snapshot entry requires actual bulk density"
            }
            require(operationalValues.effectiveUllageM != null && operationalValues.effectiveFillHeightM != null) {
                "Non-empty final snapshot entry requires effective ullage and fill height"
            }
            require(calculationMetadata.calculationResultId != null) {
                "Non-empty final snapshot entry requires frozen CalculationResult"
            }
            require(measurementsSnapshot.all {
                it.geometryProfileVersionId == geometrySnapshot.geometryProfileVersionId
            }) {
                "Frozen measurements must use the same GeometryProfileVersion as the final entry"
            }
        }
    }
}

object FinalInventorySnapshotDomainRules {
    fun validateScopeCoverage(
        snapshot: FinalInventorySnapshot,
        entries: List<FinalInventorySnapshotEntry>,
        scopedStorageSpaceIds: Set<StorageSpaceId>
    ) {
        require(scopedStorageSpaceIds.isNotEmpty()) { "Final snapshot scope must not be empty" }
        require(entries.isNotEmpty()) { "Final snapshot requires entries" }
        require(entries.all { it.finalSnapshotId == snapshot.finalSnapshotId }) {
            "Every final snapshot entry must belong to the same FinalInventorySnapshot"
        }
        require(entries.map { it.snapshotEntryId }.distinct().size == entries.size) {
            "Final snapshot entry identities must be unique"
        }
        require(entries.map { it.storageSpaceId }.distinct().size == entries.size) {
            "Final snapshot must contain exactly one entry per StorageSpace"
        }
        require(entries.map { it.storageSpaceId }.toSet() == scopedStorageSpaceIds) {
            "Final snapshot coverage must equal frozen Campaign scope exactly"
        }
    }
}
