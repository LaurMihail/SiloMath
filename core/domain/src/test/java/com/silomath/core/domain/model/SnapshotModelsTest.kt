package com.silomath.core.domain.model

import com.silomath.core.model.CalculationResultId
import com.silomath.core.model.CampaignId
import com.silomath.core.model.CampaignSnapshotId
import com.silomath.core.model.CanonicalDecimal
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
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SnapshotModelsTest {
    private val organizationId = OrganizationId(uuid(1))
    private val campaignId = CampaignId(uuid(2))
    private val campaignSnapshotId = CampaignSnapshotId(uuid(3))
    private val finalSnapshotId = FinalSnapshotId(uuid(4))
    private val userId = UserId(uuid(5))
    private val storageSpace1 = StorageSpaceId(uuid(6))
    private val storageSpace2 = StorageSpaceId(uuid(7))
    private val geometryProfileId = GeometryProfileId(uuid(8))
    private val geometryVersionId = GeometryProfileVersionId(uuid(9))
    private val locationId = LocationId(uuid(10))
    private val storageUnitId = StorageUnitId(uuid(11))
    private val revisionId = StocktakingRevisionId(uuid(12))
    private val commodityId = CommodityId(uuid(13))
    private val commodityVersionId = CommodityVersionId(uuid(14))
    private val clientId = ClientId(uuid(15))
    private val calculationResultId = CalculationResultId(uuid(16))
    private val closedAt = Instant.parse("2026-10-18T08:00:00Z")

    @Test
    fun `final snapshot requires immutable version metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(calculationCompatibilityVersions = emptySet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(snapshotSchemaVersion = " ")
        }
    }

    @Test
    fun `final snapshot coverage equals frozen campaign scope exactly`() {
        val snapshot = snapshot()
        val entries = listOf(nonEmptyEntry(1, storageSpace1), emptyEntry(2, storageSpace2))

        FinalInventorySnapshotDomainRules.validateScopeCoverage(
            snapshot,
            entries,
            setOf(storageSpace1, storageSpace2)
        )

        assertThrows(IllegalArgumentException::class.java) {
            FinalInventorySnapshotDomainRules.validateScopeCoverage(
                snapshot,
                entries.take(1),
                setOf(storageSpace1, storageSpace2)
            )
        }
    }

    @Test
    fun `final snapshot allows only one entry per scoped storage space`() {
        val snapshot = snapshot()
        val duplicate = listOf(nonEmptyEntry(1, storageSpace1), emptyEntry(2, storageSpace1))

        assertThrows(IllegalArgumentException::class.java) {
            FinalInventorySnapshotDomainRules.validateScopeCoverage(snapshot, duplicate, setOf(storageSpace1))
        }
    }

    @Test
    fun `empty final entry freezes zero values without fabricated business metadata`() {
        val entry = emptyEntry(1, storageSpace1)
        assertEquals(true, entry.isEmpty)
        assertEquals(null, entry.commoditySnapshot)
        assertEquals(null, entry.ownershipClientSnapshot)
        assertEquals(0, entry.measurementsSnapshot.size)
    }

    @Test
    fun `non empty final entry requires commodity ownership measurements and calculation result`() {
        assertThrows(IllegalArgumentException::class.java) {
            nonEmptyEntry(1, storageSpace1).copy(commoditySnapshot = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            nonEmptyEntry(1, storageSpace1).copy(measurementsSnapshot = emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            nonEmptyEntry(1, storageSpace1).copy(
                calculationMetadata = calculationMetadata(calculationResultId = null)
            )
        }
    }

    @Test
    fun `third party snapshot freezes client id code and name`() {
        assertThrows(IllegalArgumentException::class.java) {
            FrozenOwnershipClientSnapshot(Ownership.THIRD_PARTY, clientId, null, "Client")
        }
        val frozen = FrozenOwnershipClientSnapshot(
            Ownership.THIRD_PARTY,
            clientId,
            "CL-001",
            "Example Client"
        )
        assertEquals("CL-001", frozen.clientCode)
    }

    @Test
    fun `resolved issue reference requires frozen resolution metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            FrozenIssueReference(
                IssueId(uuid(30)),
                IssueCategory.MEASUREMENT_PROBLEM,
                IssueStatus.RESOLVED,
                null,
                null,
                null,
                null
            )
        }
    }

    @Test
    fun `photo and note evidence references freeze correct metadata`() {
        val photo = FrozenEvidenceReference(
            EvidenceId(uuid(40)),
            EvidenceType.PHOTO,
            null,
            "a".repeat(64),
            "org/campaign/evidence/photo.jpg"
        )
        val note = FrozenEvidenceReference(
            EvidenceId(uuid(41)),
            EvidenceType.NOTE,
            "Observed condition",
            null,
            null
        )
        assertEquals(EvidenceType.PHOTO, photo.evidenceType)
        assertEquals(EvidenceType.NOTE, note.evidenceType)
    }

    private fun snapshot(
        snapshotSchemaVersion: String = "final-snapshot-v1",
        calculationCompatibilityVersions: Set<String> = setOf("engine-v2", "aggregation-v1")
    ) = FinalInventorySnapshot(
        finalSnapshotId,
        organizationId,
        campaignId,
        campaignSnapshotId,
        closedAt,
        userId,
        snapshotSchemaVersion,
        calculationCompatibilityVersions,
        closedAt,
        userId
    )

    private fun nonEmptyEntry(index: Int, storageSpaceId: StorageSpaceId) = FinalInventorySnapshotEntry(
        snapshotEntryId = SnapshotEntryId(uuid(100 + index)),
        finalSnapshotId = finalSnapshotId,
        storageSpaceId = storageSpaceId,
        hierarchySnapshot = hierarchy(),
        geometrySnapshot = geometry(),
        commoditySnapshot = FrozenCommoditySnapshot(
            commodityId,
            commodityVersionId,
            "COMMON_WHEAT",
            "Common Wheat"
        ),
        ownershipClientSnapshot = FrozenOwnershipClientSnapshot(Ownership.OWNED, null, null, null),
        isEmpty = false,
        measurementsSnapshot = listOf(measurement()),
        operationalValues = FrozenOperationalValues(
            CanonicalDecimal.from("1.000000"),
            CanonicalDecimal.from("9.000000"),
            CanonicalDecimal.from("780.000000"),
            CanonicalDecimal.from("100.000000"),
            CanonicalDecimal.from("78000.000000")
        ),
        calculationMetadata = calculationMetadata(),
        validationMetadata = validationMetadata(),
        issueReferences = emptyList(),
        evidenceReferences = emptyList()
    )

    private fun emptyEntry(index: Int, storageSpaceId: StorageSpaceId) = FinalInventorySnapshotEntry(
        snapshotEntryId = SnapshotEntryId(uuid(200 + index)),
        finalSnapshotId = finalSnapshotId,
        storageSpaceId = storageSpaceId,
        hierarchySnapshot = hierarchy(),
        geometrySnapshot = geometry(),
        commoditySnapshot = null,
        ownershipClientSnapshot = null,
        isEmpty = true,
        measurementsSnapshot = emptyList(),
        operationalValues = FrozenOperationalValues(
            null,
            null,
            null,
            CanonicalDecimal.from("0.000000"),
            CanonicalDecimal.from("0.000000")
        ),
        calculationMetadata = calculationMetadata(calculationResultId = null),
        validationMetadata = validationMetadata(),
        issueReferences = emptyList(),
        evidenceReferences = emptyList()
    )

    private fun hierarchy() = FrozenHierarchySnapshot(
        locationId,
        "LOC-001",
        "Main Location",
        storageUnitId,
        "UNIT-001",
        "Unit A",
        "SPACE-001",
        "Space A"
    )

    private fun geometry() = FrozenGeometrySnapshot(
        geometryProfileId,
        geometryVersionId,
        "CYL_TRUNC_CONE_V1",
        "F01-v1",
        mapOf("diameterM" to CanonicalDecimal.from("10.000000")),
        CanonicalDecimal.from("10.000000"),
        CanonicalDecimal.from("785.398163")
    )

    private fun measurement() = FrozenMeasurementSnapshot(
        1,
        CanonicalDecimal.from("1.000000"),
        "m",
        CanonicalDecimal.from("1.000000"),
        MeasurementDatum("TOP_DATUM", CanonicalDecimal.from("10.000000")),
        geometryVersionId
    )

    private fun calculationMetadata(
        calculationResultId: CalculationResultId? = this.calculationResultId
    ) = FrozenCalculationMetadata(
        calculationResultId,
        "engine-v2",
        "MEAN_ULLAGE",
        1,
        if (calculationResultId == null) null else "F01-v1",
        if (calculationResultId == null) null else "b".repeat(64),
        "ROUND_HALF_UP"
    )

    private fun validationMetadata() = FrozenValidationMetadata(
        revisionId,
        3,
        Instant.parse("2026-10-18T07:00:00Z"),
        userId
    )

    private fun uuid(seed: Int): String = "%08d-0000-4000-8000-%012d".format(seed, seed)
}
