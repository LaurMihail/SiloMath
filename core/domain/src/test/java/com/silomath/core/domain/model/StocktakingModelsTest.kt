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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StocktakingModelsTest {

    private val recordId = StocktakingRecordId("11111111-1111-4111-8111-111111111111")
    private val revisionId = StocktakingRevisionId("22222222-2222-4222-8222-222222222222")
    private val priorRevisionId = StocktakingRevisionId("33333333-3333-4333-8333-333333333333")
    private val organizationId = OrganizationId("44444444-4444-4444-8444-444444444444")
    private val campaignId = CampaignId("55555555-5555-4555-8555-555555555555")
    private val storageSpaceId = StorageSpaceId("66666666-6666-4666-8666-666666666666")
    private val userId = UserId("77777777-7777-4777-8777-777777777777")
    private val commodityVersionId = CommodityVersionId("88888888-8888-4888-8888-888888888888")
    private val clientId = ClientId("99999999-9999-4999-8999-999999999999")
    private val calculationResultId = CalculationResultId("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
    private val geometryVersionId = GeometryProfileVersionId("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val validatedAt = Instant.parse("2026-01-02T00:00:00Z")
    private val datum = MeasurementDatum(
        reference = "GEOMETRY_PROFILE_VERSION_REFERENCE",
        measurementReferenceHeightM = CanonicalDecimal.from("12.000000")
    )

    @Test
    fun `stocktaking classifications are exactly to do issues and validated`() {
        assertEquals(
            listOf("TO_DO", "ISSUES", "VALIDATED"),
            StocktakingClassification.entries.map { it.name }
        )
    }

    @Test
    fun `issue condition takes precedence without changing validation lifecycle`() {
        val record = record(currentRevisionId = revisionId)
        assertEquals(
            StocktakingClassification.ISSUES,
            record.deriveClassification(StocktakingRevisionState.VALIDATED, hasOpenIssue = true)
        )
        assertEquals(
            StocktakingClassification.VALIDATED,
            record.deriveClassification(StocktakingRevisionState.VALIDATED, hasOpenIssue = false)
        )
        assertEquals(
            StocktakingClassification.TO_DO,
            record.deriveClassification(StocktakingRevisionState.WORKING, hasOpenIssue = false)
        )
    }

    @Test
    fun `record revision pointer and revision state must be coherent`() {
        assertThrows(IllegalArgumentException::class.java) {
            record(currentRevisionId = null)
                .deriveClassification(StocktakingRevisionState.WORKING, hasOpenIssue = false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            record(currentRevisionId = revisionId)
                .deriveClassification(null, hasOpenIssue = false)
        }
    }

    @Test
    fun `working revision must not define validation metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            nonEmptyRevision(
                revisionState = StocktakingRevisionState.WORKING,
                validatedAt = validatedAt,
                validatedBy = userId
            )
        }
    }

    @Test
    fun `validated non empty revision requires validation metadata and calculation result`() {
        assertThrows(IllegalArgumentException::class.java) {
            nonEmptyRevision(
                revisionState = StocktakingRevisionState.VALIDATED,
                validatedAt = null,
                validatedBy = null,
                calculationResultId = calculationResultId
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            nonEmptyRevision(
                revisionState = StocktakingRevisionState.VALIDATED,
                validatedAt = validatedAt,
                validatedBy = userId,
                calculationResultId = null
            )
        }
    }

    @Test
    fun `third party requires client and owned rejects client`() {
        assertThrows(IllegalArgumentException::class.java) {
            nonEmptyRevision(ownership = Ownership.THIRD_PARTY, clientId = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            nonEmptyRevision(ownership = Ownership.OWNED, clientId = clientId)
        }
    }

    @Test
    fun `mark empty does not fabricate commodity density ownership or client`() {
        val empty = emptyRevision()
        StocktakingDomainRules.validateMeasurementSet(empty, emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            emptyRevision(commodityVersionId = commodityVersionId)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StocktakingDomainRules.validateMeasurementSet(empty, listOf(measurement(1)))
        }
    }

    @Test
    fun `non empty revision requires one to five compatible measurements`() {
        val revision = nonEmptyRevision()
        StocktakingDomainRules.validateMeasurementSet(revision, listOf(measurement(1)))
        StocktakingDomainRules.validateMeasurementSet(
            revision,
            (1..5).map { measurement(it) }
        )

        assertThrows(IllegalArgumentException::class.java) {
            StocktakingDomainRules.validateMeasurementSet(revision, emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            StocktakingDomainRules.validateMeasurementSet(
                revision,
                (1..6).map { index ->
                    measurement((index - 1) % 5 + 1, idSuffix = index)
                }
            )
        }
    }

    @Test
    fun `measurement set rejects mixed geometry datum and duplicate sequence`() {
        val revision = nonEmptyRevision()
        val otherGeometry = GeometryProfileVersionId("cccccccc-cccc-4ccc-8ccc-cccccccccccc")
        val otherDatum = MeasurementDatum(
            reference = "OTHER_REFERENCE",
            measurementReferenceHeightM = CanonicalDecimal.from("12.000000")
        )

        assertThrows(IllegalArgumentException::class.java) {
            StocktakingDomainRules.validateMeasurementSet(
                revision,
                listOf(measurement(1), measurement(2, geometry = otherGeometry, idSuffix = 2))
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            StocktakingDomainRules.validateMeasurementSet(
                revision,
                listOf(measurement(1), measurement(2, datum = otherDatum, idSuffix = 2))
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            StocktakingDomainRules.validateMeasurementSet(
                revision,
                listOf(measurement(1), measurement(1, idSuffix = 2))
            )
        }
    }

    @Test
    fun `measurement ullage cannot exceed frozen datum reference height`() {
        assertThrows(IllegalArgumentException::class.java) {
            measurement(
                sequence = 1,
                normalized = CanonicalDecimal.from("12.000001")
            )
        }
    }

    @Test
    fun `reset revision source cannot reference itself`() {
        assertThrows(IllegalArgumentException::class.java) {
            nonEmptyRevision(basedOnRevisionId = revisionId)
        }
        val reset = nonEmptyRevision(basedOnRevisionId = priorRevisionId, revisionNumber = 2)
        assertEquals(priorRevisionId, reset.basedOnRevisionId)
    }

    private fun record(currentRevisionId: StocktakingRevisionId?) = StocktakingRecord(
        stocktakingRecordId = recordId,
        organizationId = organizationId,
        campaignId = campaignId,
        storageSpaceId = storageSpaceId,
        currentRevisionId = currentRevisionId,
        createdAt = createdAt,
        createdBy = userId,
        updatedAt = createdAt,
        updatedBy = userId,
        entityVersion = 1L
    )

    private fun nonEmptyRevision(
        revisionNumber: Int = 1,
        revisionState: StocktakingRevisionState = StocktakingRevisionState.WORKING,
        basedOnRevisionId: StocktakingRevisionId? = null,
        ownership: Ownership = Ownership.OWNED,
        clientId: ClientId? = null,
        validatedAt: Instant? = null,
        validatedBy: UserId? = null,
        calculationResultId: CalculationResultId? = null
    ) = StocktakingRevision(
        stocktakingRevisionId = revisionId,
        stocktakingRecordId = recordId,
        revisionNumber = revisionNumber,
        revisionState = revisionState,
        basedOnRevisionId = basedOnRevisionId,
        isEmpty = false,
        commodityVersionId = commodityVersionId,
        actualBulkDensityKgM3 = CanonicalDecimal.from("750.000000"),
        ownership = ownership,
        clientId = clientId,
        notes = "Checked",
        calculationResultId = calculationResultId,
        validatedAt = validatedAt,
        validatedBy = validatedBy,
        createdAt = createdAt,
        createdBy = userId,
        entityVersion = 1L
    )

    private fun emptyRevision(
        commodityVersionId: CommodityVersionId? = null
    ) = StocktakingRevision(
        stocktakingRevisionId = revisionId,
        stocktakingRecordId = recordId,
        revisionNumber = 1,
        revisionState = StocktakingRevisionState.VALIDATED,
        basedOnRevisionId = null,
        isEmpty = true,
        commodityVersionId = commodityVersionId,
        actualBulkDensityKgM3 = null,
        ownership = null,
        clientId = null,
        notes = "Empty",
        calculationResultId = null,
        validatedAt = validatedAt,
        validatedBy = userId,
        createdAt = createdAt,
        createdBy = userId,
        entityVersion = 1L
    )

    private fun measurement(
        sequence: Int,
        geometry: GeometryProfileVersionId = geometryVersionId,
        datum: MeasurementDatum = this.datum,
        normalized: CanonicalDecimal = CanonicalDecimal.from("2.000000"),
        idSuffix: Int = sequence
    ) = Measurement(
        measurementId = MeasurementId(
            String.format("%08x-0000-4000-8000-%012x", idSuffix, idSuffix)
        ),
        stocktakingRevisionId = revisionId,
        sequenceNumber = sequence,
        rawValue = normalized,
        rawUnit = "m",
        normalizedUllageM = normalized,
        datumMetadata = datum,
        geometryProfileVersionId = geometry,
        capturedAt = createdAt,
        capturedBy = userId,
        entityVersion = 1L
    )
}
