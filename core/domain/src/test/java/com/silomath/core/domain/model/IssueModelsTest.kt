package com.silomath.core.domain.model

import com.silomath.core.model.AuditEventId
import com.silomath.core.model.CampaignId
import com.silomath.core.model.IssueId
import com.silomath.core.model.IssueResolutionId
import com.silomath.core.model.IssueStatusEventId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.StocktakingRecordId
import com.silomath.core.model.UserId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class IssueModelsTest {

    private val issueId = IssueId("11111111-1111-4111-8111-111111111111")
    private val organizationId = OrganizationId("22222222-2222-4222-8222-222222222222")
    private val campaignId = CampaignId("33333333-3333-4333-8333-333333333333")
    private val recordId = StocktakingRecordId("44444444-4444-4444-8444-444444444444")
    private val userId = UserId("55555555-5555-4555-8555-555555555555")
    private val resolutionId = IssueResolutionId("66666666-6666-4666-8666-666666666666")
    private val statusEventId = IssueStatusEventId("77777777-7777-4777-8777-777777777777")
    private val auditEventId = AuditEventId("88888888-8888-4888-8888-888888888888")
    private val reportedAt = Instant.parse("2026-01-01T00:00:00Z")
    private val changedAt = Instant.parse("2026-01-02T00:00:00Z")

    @Test
    fun `issue categories are exactly the frozen six`() {
        assertEquals(
            listOf(
                "MEASUREMENT_PROBLEM",
                "ACCESS_SAFETY",
                "GEOMETRY_DISCREPANCY",
                "COMMODITY_UNCERTAINTY",
                "STORAGE_SPACE_CONDITION",
                "OTHER"
            ),
            IssueCategory.entries.map { it.name }
        )
    }

    @Test
    fun `issue status is exactly open and resolved`() {
        assertEquals(listOf("OPEN", "RESOLVED"), IssueStatus.entries.map { it.name })
    }

    @Test
    fun `description is mandatory trimmed and entity version is positive`() {
        assertThrows(IllegalArgumentException::class.java) {
            openIssue(description = "   ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            openIssue(description = " Geometry mismatch ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            openIssue(entityVersion = 0L)
        }
    }

    @Test
    fun `open issue has no effective resolution summary`() {
        assertThrows(IllegalArgumentException::class.java) {
            Issue(
                issueId = issueId,
                organizationId = organizationId,
                campaignId = campaignId,
                stocktakingRecordId = recordId,
                issueCategory = IssueCategory.GEOMETRY_DISCREPANCY,
                description = "Geometry mismatch",
                status = IssueStatus.OPEN,
                reportedAt = reportedAt,
                reportedBy = userId,
                resolvedAt = changedAt,
                resolvedBy = userId,
                entityVersion = 2L
            )
        }
    }

    @Test
    fun `resolved issue requires effective resolution summary`() {
        assertThrows(IllegalArgumentException::class.java) {
            resolvedIssue(resolvedAt = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            resolvedIssue(resolvedBy = null)
        }
    }

    @Test
    fun `resolution reason is explicit and context does not create third status`() {
        assertEquals(
            listOf("NORMAL", "CLOSE_FOLLOW_UP_ACCEPTANCE"),
            IssueResolutionContext.entries.map { it.name }
        )
        assertThrows(IllegalArgumentException::class.java) {
            resolution(reason = "")
        }
    }

    @Test
    fun `initial status event creates open and later transitions require reason`() {
        IssueStatusEvent(
            issueStatusEventId = statusEventId,
            issueId = issueId,
            fromStatus = null,
            toStatus = IssueStatus.OPEN,
            reason = null,
            changedAt = reportedAt,
            changedBy = userId,
            auditEventId = auditEventId
        )

        assertThrows(IllegalArgumentException::class.java) {
            statusEvent(IssueStatus.OPEN, IssueStatus.RESOLVED, reason = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            statusEvent(IssueStatus.OPEN, IssueStatus.OPEN, reason = "No-op")
        }
    }

    @Test
    fun `open resolves only to resolved`() {
        val event = statusEvent(IssueStatus.OPEN, IssueStatus.RESOLVED, "Resolved after review")
        IssueDomainRules.validateStatusTransition(event, CampaignStatus.ACTIVE)
    }

    @Test
    fun `resolved may reopen only before campaign completion`() {
        val event = statusEvent(IssueStatus.RESOLVED, IssueStatus.OPEN, "Problem recurred")
        IssueDomainRules.validateStatusTransition(event, CampaignStatus.ACTIVE)

        assertThrows(IllegalArgumentException::class.java) {
            IssueDomainRules.validateStatusTransition(event, CampaignStatus.COMPLETED)
        }
    }

    @Test
    fun `resolution mutation keeps current projection event resolution and audit coherent`() {
        val before = openIssue()
        val resolution = resolution(reason = "Accepted correction")
        val event = statusEvent(
            IssueStatus.OPEN,
            IssueStatus.RESOLVED,
            reason = "Accepted correction"
        )
        val after = resolvedIssue()

        IssueDomainRules.validateResolutionMutation(
            issueBefore = before,
            issueAfter = after,
            resolution = resolution,
            statusEvent = event,
            campaignStatus = CampaignStatus.ACTIVE
        )
    }

    @Test
    fun `close follow up acceptance is explicit resolved action`() {
        val before = openIssue()
        val resolution = resolution(
            reason = "Accepted for follow-up after close",
            context = IssueResolutionContext.CLOSE_FOLLOW_UP_ACCEPTANCE
        )
        val event = statusEvent(
            IssueStatus.OPEN,
            IssueStatus.RESOLVED,
            reason = "Accepted for follow-up after close"
        )
        val after = resolvedIssue()

        IssueDomainRules.validateResolutionMutation(
            issueBefore = before,
            issueAfter = after,
            resolution = resolution,
            statusEvent = event,
            campaignStatus = CampaignStatus.ACTIVE
        )
        assertEquals(IssueStatus.RESOLVED, after.status)
    }

    @Test
    fun `reopen clears only current resolution summary and preserves history externally`() {
        val before = resolvedIssue()
        val after = openIssue(entityVersion = 3L)
        val event = statusEvent(IssueStatus.RESOLVED, IssueStatus.OPEN, "Problem recurred")

        IssueDomainRules.validateReopenMutation(
            issueBefore = before,
            issueAfter = after,
            statusEvent = event,
            campaignStatus = CampaignStatus.ACTIVE
        )
        assertNull(after.resolvedAt)
        assertNull(after.resolvedBy)
    }

    private fun openIssue(
        description: String = "Geometry mismatch",
        entityVersion: Long = 1L
    ) = Issue(
        issueId = issueId,
        organizationId = organizationId,
        campaignId = campaignId,
        stocktakingRecordId = recordId,
        issueCategory = IssueCategory.GEOMETRY_DISCREPANCY,
        description = description,
        status = IssueStatus.OPEN,
        reportedAt = reportedAt,
        reportedBy = userId,
        resolvedAt = null,
        resolvedBy = null,
        entityVersion = entityVersion
    )

    private fun resolvedIssue(
        resolvedAt: Instant? = changedAt,
        resolvedBy: UserId? = userId
    ) = Issue(
        issueId = issueId,
        organizationId = organizationId,
        campaignId = campaignId,
        stocktakingRecordId = recordId,
        issueCategory = IssueCategory.GEOMETRY_DISCREPANCY,
        description = "Geometry mismatch",
        status = IssueStatus.RESOLVED,
        reportedAt = reportedAt,
        reportedBy = userId,
        resolvedAt = resolvedAt,
        resolvedBy = resolvedBy,
        entityVersion = 2L
    )

    private fun resolution(
        reason: String,
        context: IssueResolutionContext = IssueResolutionContext.NORMAL
    ) = IssueResolution(
        issueResolutionId = resolutionId,
        issueId = issueId,
        resolutionReason = reason,
        resolutionContext = context,
        resolvedAt = changedAt,
        resolvedBy = userId,
        auditEventId = auditEventId
    )

    private fun statusEvent(
        from: IssueStatus,
        to: IssueStatus,
        reason: String?
    ) = IssueStatusEvent(
        issueStatusEventId = statusEventId,
        issueId = issueId,
        fromStatus = from,
        toStatus = to,
        reason = reason,
        changedAt = changedAt,
        changedBy = userId,
        auditEventId = auditEventId
    )
}
