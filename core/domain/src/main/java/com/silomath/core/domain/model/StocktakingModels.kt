package com.silomath.core.domain.model

import com.silomath.core.model.CalculationResultId
import com.silomath.core.model.CampaignId
import com.silomath.core.model.CanonicalDecimal
import com.silomath.core.model.ClientId
import com.silomath.core.model.CommodityVersionId
import com.silomath.core.model.GeometryProfileVersionId
import com.silomath.core.model.MeasurementId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.StocktakingRecordId
import com.silomath.core.model.StocktakingRevisionId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.UserId
import java.time.Instant

enum class StocktakingClassification {
    TO_DO,
    ISSUES,
    VALIDATED
}

enum class StocktakingRevisionState {
    WORKING,
    VALIDATED
}

enum class Ownership {
    OWNED,
    THIRD_PARTY
}

enum class MeasurementType {
    ULLAGE
}

data class MeasurementDatum(
    val reference: String,
    val measurementReferenceHeightM: CanonicalDecimal
) {
    init {
        require(reference.isNotBlank()) {
            "MeasurementDatum reference must not be blank"
        }
        require(reference == reference.trim()) {
            "MeasurementDatum reference must be trimmed"
        }
        require(measurementReferenceHeightM.value.signum() > 0) {
            "MeasurementDatum measurementReferenceHeightM must be > 0"
        }
    }
}

data class StocktakingRecord(
    val stocktakingRecordId: StocktakingRecordId,
    val organizationId: OrganizationId,
    val campaignId: CampaignId,
    val storageSpaceId: StorageSpaceId,
    val currentRevisionId: StocktakingRevisionId?,
    val createdAt: Instant,
    val createdBy: UserId,
    val updatedAt: Instant,
    val updatedBy: UserId,
    val entityVersion: Long
) {
    init {
        require(entityVersion >= 1L) {
            "StocktakingRecord entityVersion must be >= 1"
        }
        require(!updatedAt.isBefore(createdAt)) {
            "StocktakingRecord updatedAt must not be before createdAt"
        }
    }

    fun deriveClassification(
        currentRevisionState: StocktakingRevisionState?,
        hasOpenIssue: Boolean
    ): StocktakingClassification {
        require(currentRevisionId != null || currentRevisionState == null) {
            "StocktakingRecord without currentRevisionId cannot have a revision state"
        }
        require(currentRevisionId == null || currentRevisionState != null) {
            "StocktakingRecord with currentRevisionId requires the current revision state"
        }

        return when {
            hasOpenIssue -> StocktakingClassification.ISSUES
            currentRevisionState == StocktakingRevisionState.VALIDATED ->
                StocktakingClassification.VALIDATED
            else -> StocktakingClassification.TO_DO
        }
    }
}

data class StocktakingRevision(
    val stocktakingRevisionId: StocktakingRevisionId,
    val stocktakingRecordId: StocktakingRecordId,
    val revisionNumber: Int,
    val revisionState: StocktakingRevisionState,
    val basedOnRevisionId: StocktakingRevisionId?,
    val isEmpty: Boolean,
    val commodityVersionId: CommodityVersionId?,
    val actualBulkDensityKgM3: CanonicalDecimal?,
    val ownership: Ownership?,
    val clientId: ClientId?,
    val notes: String?,
    val calculationResultId: CalculationResultId?,
    val validatedAt: Instant?,
    val validatedBy: UserId?,
    val createdAt: Instant,
    val createdBy: UserId,
    val entityVersion: Long
) {
    init {
        require(revisionNumber >= 1) {
            "StocktakingRevision revisionNumber must be >= 1"
        }
        require(entityVersion >= 1L) {
            "StocktakingRevision entityVersion must be >= 1"
        }
        require(basedOnRevisionId != stocktakingRevisionId) {
            "StocktakingRevision cannot be based on itself"
        }

        when (revisionState) {
            StocktakingRevisionState.WORKING -> {
                require(validatedAt == null && validatedBy == null) {
                    "WORKING StocktakingRevision must not define validation metadata"
                }
            }

            StocktakingRevisionState.VALIDATED -> {
                require(validatedAt != null && validatedBy != null) {
                    "VALIDATED StocktakingRevision requires validatedAt and validatedBy"
                }
                require(!validatedAt.isBefore(createdAt)) {
                    "StocktakingRevision validatedAt must not be before createdAt"
                }
            }
        }

        if (isEmpty) {
            require(commodityVersionId == null) {
                "Mark Empty must not fabricate CommodityVersion"
            }
            require(actualBulkDensityKgM3 == null) {
                "Mark Empty must not fabricate Actual Bulk Density"
            }
            require(ownership == null) {
                "Mark Empty must not fabricate Ownership"
            }
            require(clientId == null) {
                "Mark Empty must not fabricate Client"
            }
        } else {
            require(commodityVersionId != null) {
                "Non-empty StocktakingRevision requires CommodityVersion"
            }
            require(actualBulkDensityKgM3 != null && actualBulkDensityKgM3.value.signum() > 0) {
                "Non-empty StocktakingRevision requires Actual Bulk Density > 0"
            }
            require(ownership != null) {
                "Non-empty StocktakingRevision requires Ownership"
            }
            when (ownership) {
                Ownership.OWNED -> require(clientId == null) {
                    "OWNED StocktakingRevision must not require Client"
                }
                Ownership.THIRD_PARTY -> require(clientId != null) {
                    "THIRD_PARTY StocktakingRevision requires Client"
                }
                null -> Unit
            }
            if (revisionState == StocktakingRevisionState.VALIDATED) {
                require(calculationResultId != null) {
                    "Validated non-empty StocktakingRevision requires a fresh CalculationResult"
                }
            }
        }
    }
}

data class Measurement(
    val measurementId: MeasurementId,
    val stocktakingRevisionId: StocktakingRevisionId,
    val sequenceNumber: Int,
    val rawValue: CanonicalDecimal,
    val rawUnit: String,
    val normalizedUllageM: CanonicalDecimal,
    val datumMetadata: MeasurementDatum,
    val geometryProfileVersionId: GeometryProfileVersionId,
    val measurementType: MeasurementType = MeasurementType.ULLAGE,
    val capturedAt: Instant,
    val capturedBy: UserId,
    val entityVersion: Long
) {
    init {
        require(sequenceNumber in 1..5) {
            "Measurement sequenceNumber must be in 1..5"
        }
        require(rawValue.value.signum() >= 0) {
            "Measurement rawValue must be >= 0"
        }
        require(rawUnit.isNotBlank() && rawUnit == rawUnit.trim()) {
            "Measurement rawUnit must be non-blank and trimmed"
        }
        require(normalizedUllageM.value.signum() >= 0) {
            "Measurement normalizedUllageM must be >= 0"
        }
        require(normalizedUllageM.value <= datumMetadata.measurementReferenceHeightM.value) {
            "Measurement normalizedUllageM must not exceed the frozen reference height"
        }
        require(entityVersion >= 1L) {
            "Measurement entityVersion must be >= 1"
        }
    }
}

object StocktakingDomainRules {

    fun validateMeasurementSet(
        revision: StocktakingRevision,
        measurements: List<Measurement>
    ) {
        if (revision.isEmpty) {
            require(measurements.isEmpty()) {
                "Mark Empty StocktakingRevision must have zero measurements"
            }
            return
        }

        require(measurements.size in 1..5) {
            "Non-empty StocktakingRevision requires 1..5 measurements"
        }
        require(measurements.all { it.stocktakingRevisionId == revision.stocktakingRevisionId }) {
            "Measurements must belong to the same StocktakingRevision"
        }
        require(measurements.map { it.measurementId }.distinct().size == measurements.size) {
            "Measurement identity must be unique within a revision"
        }
        require(measurements.map { it.sequenceNumber }.distinct().size == measurements.size) {
            "Measurement sequenceNumber must be unique within a revision"
        }

        val geometryVersion = measurements.first().geometryProfileVersionId
        val datum = measurements.first().datumMetadata
        val type = measurements.first().measurementType

        require(measurements.all { it.geometryProfileVersionId == geometryVersion }) {
            "Measurements must use the same GeometryProfileVersion"
        }
        require(measurements.all { it.datumMetadata == datum }) {
            "Measurements must use the same frozen datum/reference"
        }
        require(measurements.all { it.measurementType == type }) {
            "Measurements must use the same measurement type"
        }
    }
}
