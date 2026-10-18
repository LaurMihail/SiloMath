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

enum class IssueCategory {
    MEASUREMENT_PROBLEM,
    ACCESS_SAFETY,
    GEOMETRY_DISCREPANCY,
    COMMODITY_UNCERTAINTY,
    STORAGE_SPACE_CONDITION,
    OTHER
}

enum class IssueStatus {
    OPEN,
    RESOLVED
}

enum class IssueResolutionContext {
    NORMAL,
    CLOSE_FOLLOW_UP_ACCEPTANCE
}

data class Issue(
    val issueId: IssueId,
    val organizationId: OrganizationId,
    val campaignId: CampaignId,
    val stocktakingRecordId: StocktakingRecordId,
    val issueCategory: IssueCategory,
    val description: String,
    val status: IssueStatus,
    val reportedAt: Instant,
    val reportedBy: UserId,
    val resolvedAt: Instant?,
    val resolvedBy: UserId?,
    val entityVersion: Long
) {
    init {
        require(description.isNotBlank()) {
            "Issue description is mandatory for every category"
        }
        require(description == description.trim()) {
            "Issue description must be trimmed"
        }
        require(entityVersion >= 1L) {
            "Issue entityVersion must be >= 1"
        }

        when (status) {
            IssueStatus.OPEN -> {
                require(resolvedAt == null && resolvedBy == null) {
                    "OPEN Issue must not expose an effective resolution summary"
                }
            }

            IssueStatus.RESOLVED -> {
                require(resolvedAt != null && resolvedBy != null) {
                    "RESOLVED Issue requires resolvedAt and resolvedBy"
                }
                require(!resolvedAt.isBefore(reportedAt)) {
                    "Issue resolvedAt must not be before reportedAt"
                }
            }
        }
    }
}

data class IssueResolution(
    val issueResolutionId: IssueResolutionId,
    val issueId: IssueId,
    val resolutionReason: String,
    val resolutionContext: IssueResolutionContext,
    val resolvedAt: Instant,
    val resolvedBy: UserId,
    val auditEventId: AuditEventId
) {
    init {
        require(resolutionReason.isNotBlank()) {
            "IssueResolution requires an explicit reason"
        }
        require(resolutionReason == resolutionReason.trim()) {
            "IssueResolution reason must be trimmed"
        }
    }
}

data class IssueStatusEvent(
    val issueStatusEventId: IssueStatusEventId,
    val issueId: IssueId,
    val fromStatus: IssueStatus?,
    val toStatus: IssueStatus,
    val reason: String?,
    val changedAt: Instant,
    val changedBy: UserId,
    val auditEventId: AuditEventId
) {
    init {
        if (fromStatus == null) {
            require(toStatus == IssueStatus.OPEN) {
                "Initial IssueStatusEvent must create OPEN status"
            }
            require(reason == null || reason.isNotBlank()) {
                "Initial IssueStatusEvent reason, when present, must not be blank"
            }
        } else {
            require(fromStatus != toStatus) {
                "IssueStatusEvent must represent an actual status transition"
            }
            require(reason != null && reason.isNotBlank()) {
                "Issue resolution/reopen transition requires an explicit reason"
            }
        }
        require(reason == null || reason == reason.trim()) {
            "IssueStatusEvent reason must be trimmed"
        }
    }
}

object IssueDomainRules {

    fun validateStatusTransition(
        event: IssueStatusEvent,
        campaignStatus: CampaignStatus
    ) {
        when (event.fromStatus) {
            null -> require(event.toStatus == IssueStatus.OPEN) {
                "Issue creation must produce OPEN"
            }

            IssueStatus.OPEN -> require(event.toStatus == IssueStatus.RESOLVED) {
                "OPEN Issue may transition only to RESOLVED"
            }

            IssueStatus.RESOLVED -> {
                require(event.toStatus == IssueStatus.OPEN) {
                    "RESOLVED Issue may transition only to OPEN"
                }
                require(campaignStatus == CampaignStatus.ACTIVE) {
                    "RESOLVED Issue may reopen only before Campaign completion"
                }
            }
        }
    }

    fun validateResolutionMutation(
        issueBefore: Issue,
        issueAfter: Issue,
        resolution: IssueResolution,
        statusEvent: IssueStatusEvent,
        campaignStatus: CampaignStatus
    ) {
        require(issueBefore.issueId == issueAfter.issueId) {
            "Issue identity must be stable across resolution"
        }
        require(issueBefore.status == IssueStatus.OPEN && issueAfter.status == IssueStatus.RESOLVED) {
            "Issue resolution must project OPEN to RESOLVED"
        }
        require(resolution.issueId == issueBefore.issueId) {
            "IssueResolution must belong to the resolved Issue"
        }
        require(statusEvent.issueId == issueBefore.issueId) {
            "IssueStatusEvent must belong to the resolved Issue"
        }
        require(statusEvent.fromStatus == IssueStatus.OPEN && statusEvent.toStatus == IssueStatus.RESOLVED) {
            "Issue resolution requires an OPEN to RESOLVED status event"
        }
        require(issueAfter.resolvedAt == resolution.resolvedAt) {
            "Issue current projection resolvedAt must match latest IssueResolution"
        }
        require(issueAfter.resolvedBy == resolution.resolvedBy) {
            "Issue current projection resolvedBy must match latest IssueResolution"
        }
        require(statusEvent.changedAt == resolution.resolvedAt) {
            "Resolution status event timestamp must match IssueResolution"
        }
        require(statusEvent.changedBy == resolution.resolvedBy) {
            "Resolution status event actor must match IssueResolution"
        }
        require(statusEvent.auditEventId == resolution.auditEventId) {
            "IssueResolution and status transition must link the same authoritative audit event"
        }
        require(!resolution.resolvedAt.isBefore(issueBefore.reportedAt)) {
            "Issue resolution must not predate Issue reporting"
        }
        validateStatusTransition(statusEvent, campaignStatus)
    }

    fun validateReopenMutation(
        issueBefore: Issue,
        issueAfter: Issue,
        statusEvent: IssueStatusEvent,
        campaignStatus: CampaignStatus
    ) {
        require(issueBefore.issueId == issueAfter.issueId) {
            "Issue identity must be stable across reopen"
        }
        require(issueBefore.status == IssueStatus.RESOLVED && issueAfter.status == IssueStatus.OPEN) {
            "Issue reopen must project RESOLVED to OPEN"
        }
        require(issueAfter.resolvedAt == null && issueAfter.resolvedBy == null) {
            "Reopened Issue must clear only the effective resolution summary"
        }
        require(statusEvent.issueId == issueBefore.issueId) {
            "IssueStatusEvent must belong to the reopened Issue"
        }
        require(statusEvent.fromStatus == IssueStatus.RESOLVED && statusEvent.toStatus == IssueStatus.OPEN) {
            "Issue reopen requires a RESOLVED to OPEN status event"
        }
        require(!statusEvent.changedAt.isBefore(issueBefore.reportedAt)) {
            "Issue reopen must not predate Issue reporting"
        }
        validateStatusTransition(statusEvent, campaignStatus)
    }
}
