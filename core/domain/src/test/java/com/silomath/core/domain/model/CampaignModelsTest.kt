package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.CampaignScopeId
import com.silomath.core.model.CampaignSnapshotId
import com.silomath.core.model.CampaignTeamAssignmentId
import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import com.silomath.core.model.GeometryProfileVersionId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.OrganizationMembershipId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.UserId
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    private val secondStorageSpaceId = StorageSpaceId("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
    private val geometryVersionId = GeometryProfileVersionId("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
    private val commodityId = CommodityId("cccccccc-cccc-4ccc-8ccc-cccccccccccc")
    private val commodityVersionId = CommodityVersionId("dddddddd-dddd-4ddd-8ddd-dddddddddddd")

    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val launchedAt = Instant.parse("2026-01-02T00:00:00Z")
    private val completedAt = Instant.parse("2026-01-10T00:00:00Z")

    @Test
    fun `campaign status is exactly active and completed`() {
        assertEquals(listOf("ACTIVE", "COMPLETED"), CampaignStatus.entries.map { it.name })
    }

    @Test
    fun `active campaign rejects completion metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            campaign(status = CampaignStatus.ACTIVE, completedAt = completedAt, completedBy = userId)
        }
    }

    @Test
    fun `completed campaign requires completion metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            campaign(status = CampaignStatus.COMPLETED, completedAt = null, completedBy = null)
        }
    }

    @Test
    fun `campaign rejects invalid code and date order`() {
        assertThrows(IllegalArgumentException::class.java) {
            campaign(campaignCode = "DRAFT-1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            campaign(
                startDate = LocalDate.parse("2026-02-01"),
                plannedEndDate = LocalDate.parse("2026-01-31")
            )
        }
    }

    @Test
    fun `scope requires immutable launch hierarchy reference`() {
        assertThrows(IllegalArgumentException::class.java) {
            CampaignScope(
                campaignScopeId = campaignScopeId,
                campaignId = campaignId,
                storageSpaceId = storageSpaceId,
                snapshotHierarchyRef = " ",
                createdAt = createdAt,
                createdBy = userId
            )
        }
    }

    @Test
    fun `active team assignment has no removal metadata`() {
        val assignment = assignment(CampaignTeamAssignmentStatus.ACTIVE)
        assertEquals(null, assignment.removedAt)
        assertEquals(null, assignment.removedBy)
    }

    @Test
    fun `removed team assignment requires preserved removal metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            assignment(CampaignTeamAssignmentStatus.REMOVED)
        }
        val removed = assignment(
            CampaignTeamAssignmentStatus.REMOVED,
            removedAt = completedAt,
            removedBy = userId
        )
        assertEquals(completedAt, removed.removedAt)
    }

    @Test
    fun `campaign snapshot requires non empty unique scope and launch team`() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(scope = emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(team = emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(
                scope = listOf(scopeEntry(), scopeEntry())
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(
                team = listOf(teamEntry(), teamEntry())
            )
        }
    }

    @Test
    fun `campaign snapshot geometry must belong to frozen scope`() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(
                geometry = listOf(
                    CampaignGeometrySnapshotEntry(
                        storageSpaceId = secondStorageSpaceId,
                        geometryProfileVersionId = geometryVersionId
                    )
                )
            )
        }
    }

    @Test
    fun `campaign snapshot preserves frozen compatibility context`() {
        val snapshot = snapshot()
        assertEquals(1, snapshot.scopeSnapshot.size)
        assertEquals(1, snapshot.geometrySnapshot.size)
        assertEquals(1, snapshot.commodityCompatibilitySnapshot.size)
        assertEquals(1, snapshot.teamSnapshot.size)
        assertEquals(setOf("ENGINE_V1", "SCHEMA_V1"), snapshot.calculationSchemaVersions)
    }

    private fun campaign(
        campaignCode: String = "INV-2026-0001",
        startDate: LocalDate = LocalDate.parse("2026-01-02"),
        plannedEndDate: LocalDate = LocalDate.parse("2026-01-31"),
        status: CampaignStatus = CampaignStatus.ACTIVE,
        completedAt: Instant? = null,
        completedBy: UserId? = null
    ) = InventoryCampaign(
        campaignId = campaignId,
        organizationId = organizationId,
        campaignCode = campaignCode,
        name = "Annual Inventory",
        inventoryType = CampaignInventoryType.ANNUAL,
        startDate = startDate,
        plannedEndDate = plannedEndDate,
        status = status,
        launchedAt = launchedAt,
        launchedBy = userId,
        completedAt = completedAt,
        completedBy = completedBy,
        campaignSnapshotId = campaignSnapshotId,
        createdAt = createdAt,
        createdBy = userId,
        updatedAt = launchedAt,
        updatedBy = userId,
        entityVersion = 1L
    )

    private fun assignment(
        status: CampaignTeamAssignmentStatus,
        removedAt: Instant? = null,
        removedBy: UserId? = null
    ) = CampaignTeamAssignment(
        assignmentId = assignmentId,
        campaignId = campaignId,
        userId = secondUserId,
        organizationMembershipId = membershipId,
        assignmentStatus = status,
        assignedAt = launchedAt,
        assignedBy = userId,
        removedAt = removedAt,
        removedBy = removedBy,
        createdAt = createdAt,
        createdBy = userId,
        updatedAt = removedAt ?: launchedAt,
        updatedBy = userId,
        entityVersion = 1L
    )

    private fun scopeEntry() = CampaignScopeSnapshotEntry(
        storageSpaceId = storageSpaceId,
        snapshotHierarchyRef = "ORG/LOC/UNIT/SPACE"
    )

    private fun teamEntry() = CampaignTeamSnapshotEntry(
        userId = secondUserId,
        organizationMembershipId = membershipId
    )

    private fun snapshot(
        scope: List<CampaignScopeSnapshotEntry> = listOf(scopeEntry()),
        geometry: List<CampaignGeometrySnapshotEntry> = listOf(
            CampaignGeometrySnapshotEntry(
                storageSpaceId = storageSpaceId,
                geometryProfileVersionId = geometryVersionId
            )
        ),
        team: List<CampaignTeamSnapshotEntry> = listOf(teamEntry())
    ) = CampaignSnapshot(
        campaignSnapshotId = campaignSnapshotId,
        campaignId = campaignId,
        organizationId = organizationId,
        campaignConfiguration = CampaignConfigurationSnapshot(
            campaignCode = "INV-2026-0001",
            name = "Annual Inventory",
            inventoryType = CampaignInventoryType.ANNUAL,
            startDate = LocalDate.parse("2026-01-02"),
            plannedEndDate = LocalDate.parse("2026-01-31")
        ),
        scopeSnapshot = scope,
        geometrySnapshot = geometry,
        commodityCompatibilitySnapshot = listOf(
            CampaignCommodityCompatibilitySnapshotEntry(
                commodityId = commodityId,
                commodityVersionId = commodityVersionId
            )
        ),
        teamSnapshot = team,
        calculationSchemaVersions = setOf("ENGINE_V1", "SCHEMA_V1"),
        createdAt = launchedAt,
        createdBy = userId,
        snapshotSchemaVersion = "1"
    )
}
