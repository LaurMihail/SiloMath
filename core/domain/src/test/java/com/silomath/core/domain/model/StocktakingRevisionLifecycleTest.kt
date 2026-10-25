package com.silomath.core.domain.model

import com.silomath.core.model.CalculationResultId
import com.silomath.core.model.CanonicalDecimal
import com.silomath.core.model.CommodityVersionId
import com.silomath.core.model.GeometryProfileVersionId
import com.silomath.core.model.MeasurementId
import com.silomath.core.model.StocktakingRecordId
import com.silomath.core.model.StocktakingRevisionId
import com.silomath.core.model.UserId
import java.time.Instant
import org.junit.Assert.assertThrows
import org.junit.Test

class StocktakingRevisionLifecycleTest {
    private val recordId = StocktakingRecordId("11111111-1111-4111-8111-111111111111")
    private val validatedId = StocktakingRevisionId("22222222-2222-4222-8222-222222222222")
    private val workingId = StocktakingRevisionId("33333333-3333-4333-8333-333333333333")
    private val commodityVersionId = CommodityVersionId("44444444-4444-4444-8444-444444444444")
    private val calculationResultId = CalculationResultId("55555555-5555-4555-8555-555555555555")
    private val geometryVersionId = GeometryProfileVersionId("66666666-6666-4666-8666-666666666666")
    private val userId = UserId("77777777-7777-4777-8777-777777777777")
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val validatedAt = Instant.parse("2026-01-02T00:00:00Z")

    @Test
    fun `reset validation creates next working revision based on validated history`() {
        StocktakingDomainRules.validateResetValidation(validatedRevision(), workingRevision())

        assertThrows(IllegalArgumentException::class.java) {
            StocktakingDomainRules.validateResetValidation(
                validatedRevision(),
                workingRevision(revisionNumber = 3)
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            StocktakingDomainRules.validateResetValidation(
                validatedRevision(),
                workingRevision(basedOnRevisionId = null)
            )
        }
    }

    @Test
    fun `mark empty cannot exist as working revision`() {
        assertThrows(IllegalArgumentException::class.java) {
            StocktakingRevision(
                stocktakingRevisionId = workingId,
                stocktakingRecordId = recordId,
                revisionNumber = 2,
                revisionState = StocktakingRevisionState.WORKING,
                basedOnRevisionId = validatedId,
                isEmpty = true,
                commodityVersionId = null,
                actualBulkDensityKgM3 = null,
                ownership = null,
                clientId = null,
                notes = "Empty",
                calculationResultId = null,
                validatedAt = null,
                validatedBy = null,
                createdAt = createdAt,
                createdBy = userId,
                entityVersion = 1L
            )
        }
    }

    @Test
    fun `measurement ordering must be contiguous from one`() {
        val revision = workingRevision()
        assertThrows(IllegalArgumentException::class.java) {
            StocktakingDomainRules.validateMeasurementSet(
                revision,
                listOf(measurement(1, 1), measurement(3, 2))
            )
        }
    }

    private fun validatedRevision() = StocktakingRevision(
        stocktakingRevisionId = validatedId,
        stocktakingRecordId = recordId,
        revisionNumber = 1,
        revisionState = StocktakingRevisionState.VALIDATED,
        basedOnRevisionId = null,
        isEmpty = false,
        commodityVersionId = commodityVersionId,
        actualBulkDensityKgM3 = CanonicalDecimal.from("750"),
        ownership = Ownership.OWNED,
        clientId = null,
        notes = "Validated",
        calculationResultId = calculationResultId,
        validatedAt = validatedAt,
        validatedBy = userId,
        createdAt = createdAt,
        createdBy = userId,
        entityVersion = 1L
    )

    private fun workingRevision(
        revisionNumber: Int = 2,
        basedOnRevisionId: StocktakingRevisionId? = validatedId
    ) = StocktakingRevision(
        stocktakingRevisionId = workingId,
        stocktakingRecordId = recordId,
        revisionNumber = revisionNumber,
        revisionState = StocktakingRevisionState.WORKING,
        basedOnRevisionId = basedOnRevisionId,
        isEmpty = false,
        commodityVersionId = commodityVersionId,
        actualBulkDensityKgM3 = CanonicalDecimal.from("750"),
        ownership = Ownership.OWNED,
        clientId = null,
        notes = "Working",
        calculationResultId = null,
        validatedAt = null,
        validatedBy = null,
        createdAt = validatedAt,
        createdBy = userId,
        entityVersion = 1L
    )

    private fun measurement(sequence: Int, suffix: Int) = Measurement(
        measurementId = MeasurementId(String.format("%08x-0000-4000-8000-%012x", suffix, suffix)),
        stocktakingRevisionId = workingId,
        sequenceNumber = sequence,
        rawValue = CanonicalDecimal.from("2"),
        rawUnit = "m",
        normalizedUllageM = CanonicalDecimal.from("2"),
        datumMetadata = MeasurementDatum("PROFILE_REFERENCE", CanonicalDecimal.from("12")),
        geometryProfileVersionId = geometryVersionId,
        capturedAt = createdAt,
        capturedBy = userId,
        entityVersion = 1L
    )
}
