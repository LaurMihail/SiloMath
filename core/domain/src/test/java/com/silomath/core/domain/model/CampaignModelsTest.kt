package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.CampaignScopeId
import com.silomath.core.model.CampaignSnapshotId
import com.silomath.core.model.CampaignTeamAssignmentId
import com.silomath.core.model.CanonicalDecimal
import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import com.silomath.core.model.GeometryProfileId
import com.silomath.core.model.GeometryProfileVersionId
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.OrganizationMembershipId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.StorageUnitId
import com.silomath.core.model.UserId
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignModelsTest {
    private val campaignId = CampaignId("11111111-1111-4111-8111-111111111111")
    private val organizationId = OrganizationId("22222222-2222-4222-8222-222222222222")
    private val campaignSnapshotId = CampaignSnapshotId("33333333-3333-4333-8333-333333333333")
    private val campaignScopeId = CampaignScopeId("44444444-4444-4444-8444-444444444444")
    private val assignmentId = CampaignTeamAssignmentId("55555555-5555-4555-8555-555555555555")
    private val userId = UserId("66666666-6666-4666-8666-666666666666")
    private val secondUserId = UserId("77777777-7777-4777-8777-777777777777")
    private val membershipId = OrganizationMembershipId("88888888-8888-4888-8888-888888888888")
    private val storageSpaceId = StorageSpaceId("99999999-9999-4999-8999-999999999999")
    private val storageUnitId = StorageUnitId("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
    private val locationId = LocationId("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
    private val geometryProfileId = GeometryProfileId("cccccccc-cccc-4ccc-8ccc-cccccccccccc")
    private val geometryVersionId = GeometryProfileVersionId("dddddddd-dddd-4ddd-8ddd-dddddddddddd")
    private val commodityId = CommodityId("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee")
    private val commodityVersionId = CommodityVersionId("ffffffff-ffff-4fff-8fff-ffffffffffff")
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val launchedAt = Instant.parse("2026-01-02T00:00:00Z")
    private val completedAt = Instant.parse("2026-01-10T00:00:00Z")

    @Test
    fun `campaign lifecycle is exactly ACTIVE to COMPLETED`() {
        assertEquals(listOf("ACTIVE", "COMPLETED"), CampaignStatus.entries.map { it.name })
        assertTrue(campaign().canTransitionTo(CampaignStatus.COMPLETED))
        assertFalse(campaign(status = CampaignStatus.COMPLETED, completedAt = completedAt, completedBy = userId)
            .canTransitionTo(CampaignStatus.ACTIVE))
    }

    @Test
    fun `active and completed metadata invariants are enforced`() {
        assertThrows(IllegalArgumentException::class.java) {
            campaign(status = CampaignStatus.ACTIVE, completedAt = completedAt, completedBy = userId)
        }
        assertThrows(IllegalArgumentException::class.java) {
            campaign(status = CampaignStatus.COMPLETED)
        }
    }

    @Test
    fun `team history preserves explicit removal metadata`() {
        val active = assignment(CampaignTeamAssignmentStatus.ACTIVE)
        assertEquals(null, active.removedAt)
        val removed = assignment(CampaignTeamAssignmentStatus.REMOVED, completedAt, userId)
        assertEquals(completedAt, removed.removedAt)
        assertThrows(IllegalArgumentException::class.java) {
            assignment(CampaignTeamAssignmentStatus.REMOVED)
        }
    }

    @Test
    fun `save and launch requires exact nonempty unique scope and team`() {
        val valid = launchInput()
        assertEquals(1, valid.scopeStorageSpaceIds.size)
        assertEquals(1, valid.team.size)
        assertThrows(IllegalArgumentException::class.java) { launchInput(scope = emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            launchInput(scope = listOf(storageSpaceId, storageSpaceId))
        }
        assertThrows(IllegalArgumentException::class.java) { launchInput(team = emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            launchInput(team = listOf(SaveAndLaunchTeamMember(secondUserId), SaveAndLaunchTeamMember(secondUserId)))
        }
    }

    @Test
    fun `campaign snapshot freezes organization scope team commodity and compatibility versions`() {
        val snapshot = snapshot()
        assertEquals(organizationId, snapshot.organizationId)
        assertEquals("ORG", snapshot.organizationSnapshot.code)
        assertEquals("SPACE-01", snapshot.scopeSnapshot.single().storageSpaceCode)
        assertEquals(OrganizationRole.STANDARD, snapshot.teamSnapshot.single().organizationRole)
        assertEquals(commodityVersionId, snapshot.commodityCompatibilitySnapshot.single().commodityVersionId)
        assertEquals(setOf("CALC_SCHEMA_V1"), snapshot.calculationSchemaVersions)
        assertEquals("ENGINE_V1", snapshot.calculationEngineVersion)
    }

    @Test
    fun `campaign snapshot rejects duplicate identities and missing compatibility`() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(scope = listOf(scopeEntry(), scopeEntry()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(team = listOf(teamEntry(), teamEntry()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(commodities = emptyList())
        }
    }

    @Test
    fun `campaign team mutation control requires optimistic version and idempotency`() {
        CampaignTeamMutationControl(1L, "idem-1")
        assertThrows(IllegalArgumentException::class.java) {
            CampaignTeamMutationControl(0L, "idem-1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CampaignTeamMutationControl(1L, " ")
        }
    }

    private fun campaign(
        status: CampaignStatus = CampaignStatus.ACTIVE,
        completedAt: Instant? = null,
        completedBy: UserId? = null
    ) = InventoryCampaign(
        campaignId, organizationId, "INV-2026-0001", "Annual Inventory",
        CampaignInventoryType.ANNUAL, LocalDate.parse("2026-01-02"), LocalDate.parse("2026-01-31"),
        status, launchedAt, userId, completedAt, completedBy, campaignSnapshotId,
        createdAt, userId, launchedAt, userId, 1L
    )

    private fun assignment(
        status: CampaignTeamAssignmentStatus,
        removedAt: Instant? = null,
        removedBy: UserId? = null
    ) = CampaignTeamAssignment(
        assignmentId, campaignId, secondUserId, membershipId, status, launchedAt, userId,
        removedAt, removedBy, createdAt, userId, removedAt ?: launchedAt, userId, 1L
    )

    private fun launchInput(
        scope: List<StorageSpaceId> = listOf(storageSpaceId),
        team: List<SaveAndLaunchTeamMember> = listOf(SaveAndLaunchTeamMember(secondUserId))
    ) = SaveAndLaunchCampaignInput(
        organizationId, "Annual Inventory", CampaignInventoryType.ANNUAL,
        LocalDate.parse("2026-01-02"), LocalDate.parse("2026-01-31"),
        scope, team, "launch-1"
    )

    private fun scopeEntry() = CampaignScopeSnapshotEntry(
        storageSpaceId = storageSpaceId,
        storageSpaceCode = "SPACE-01",
        storageUnitId = storageUnitId,
        storageUnitCode = "UNIT-01",
        storageUnitName = "Unit 01",
        locationId = locationId,
        locationCode = "LOC-01",
        locationName = "Location 01",
        geometryProfileId = geometryProfileId,
        geometryProfileVersionId = geometryVersionId,
        geometryProfileCode = "GP-01",
        geometryProfileName = "Profile 01",
        geometryFamilyCode = "F01",
        geometryFamilyName = "Family 01",
        geometryFormulaVersion = "1",
        geometryDimensions = mapOf("diameter_m" to CanonicalDecimal.from("10")),
        measurementReferenceHeightM = CanonicalDecimal.from("20"),
        maximumFillHeightM = CanonicalDecimal.from("19"),
        maxCapacityM3 = CanonicalDecimal.from("1000")
    )

    private fun teamEntry() = CampaignTeamSnapshotEntry(
        userId = secondUserId,
        organizationMembershipId = membershipId,
        userFullName = "Field User",
        userEmail = "field@example.com",
        organizationRole = OrganizationRole.STANDARD
    )

    private fun snapshot(
        scope: List<CampaignScopeSnapshotEntry> = listOf(scopeEntry()),
        team: List<CampaignTeamSnapshotEntry> = listOf(teamEntry()),
        commodities: List<CampaignCommodityCompatibilitySnapshotEntry> = listOf(
            CampaignCommodityCompatibilitySnapshotEntry(commodityId, commodityVersionId)
        )
    ) = CampaignSnapshot(
        campaignSnapshotId = campaignSnapshotId,
        campaignId = campaignId,
        organizationSnapshot = CampaignOrganizationSnapshot(
            organizationId, "Organization", "ORG", null, "Braila", "RO"
        ),
        campaignConfiguration = CampaignConfigurationSnapshot(
            "INV-2026-0001", "Annual Inventory", CampaignInventoryType.ANNUAL,
            LocalDate.parse("2026-01-02"), LocalDate.parse("2026-01-31")
        ),
        scopeSnapshot = scope,
        commodityCompatibilitySnapshot = commodities,
        teamSnapshot = team,
        calculationSchemaVersions = setOf("CALC_SCHEMA_V1"),
        calculationEngineVersion = "ENGINE_V1",
        createdAt = launchedAt,
        createdBy = userId,
        snapshotSchemaVersion = "SNAPSHOT_V1"
    )
}
