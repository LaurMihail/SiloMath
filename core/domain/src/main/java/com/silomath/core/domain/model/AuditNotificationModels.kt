package com.silomath.core.domain.model

import com.silomath.core.model.AuditEventId
import com.silomath.core.model.CampaignId
import com.silomath.core.model.NotificationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.UserId
import java.time.Instant
import java.util.UUID

private val forbiddenAuditKeyFragments = setOf(
    "password",
    "token",
    "authorization",
    "credential",
    "secret",
    "private_key",
    "privatekey",
    "binary",
    "photo_bytes",
    "photobytes"
)

private fun requireStableCode(value: String, label: String) {
    require(value.isNotBlank()) { "$label must not be blank" }
    require(value == value.trim()) { "$label must be trimmed" }
}

private fun requireCanonicalUuidString(value: String, label: String) {
    require(value.isNotBlank()) { "$label must not be blank" }
    val parsed = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("$label must be a valid UUID: $value")
    }
    require(parsed.toString() == value) {
        "$label must use canonical lowercase UUID representation: $value"
    }
}

data class AuditChangeMetadata(
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val changes: Map<String, String?> = emptyMap()
) {
    init {
        val keys = before.keys + after.keys + changes.keys
        require(keys.all { it.isNotBlank() && it == it.trim() }) {
            "Audit metadata keys must be non-blank and trimmed"
        }
        require(keys.none(::containsForbiddenAuditKeyFragment)) {
            "Audit metadata must not contain secret, credential, token, authorization, password, or binary fields"
        }
    }

    val isEmpty: Boolean
        get() = before.isEmpty() && after.isEmpty() && changes.isEmpty()

    private fun containsForbiddenAuditKeyFragment(key: String): Boolean {
        val normalized = key.lowercase().replace('-', '_').replace(' ', '_')
        return forbiddenAuditKeyFragments.any(normalized::contains)
    }
}

data class AuditEvent(
    val eventId: AuditEventId,
    val organizationId: OrganizationId,
    val actorUserId: UserId?,
    val action: String,
    val entityType: String,
    val entityId: String,
    val timestampUtc: Instant,
    val campaignId: CampaignId?,
    val changeMetadata: AuditChangeMetadata?,
    val correlationId: String,
    val source: String,
    val systemActor: String?
) {
    init {
        requireStableCode(action, "AuditEvent action")
        requireStableCode(entityType, "AuditEvent entityType")
        requireCanonicalUuidString(entityId, "AuditEvent entityId")
        requireStableCode(correlationId, "AuditEvent correlationId")
        requireStableCode(source, "AuditEvent source")

        if (actorUserId == null) {
            require(source == SYSTEM_SOURCE) {
                "AuditEvent without actorUserId must use source=SYSTEM"
            }
            require(!systemActor.isNullOrBlank() && systemActor == systemActor.trim()) {
                "SYSTEM AuditEvent requires non-blank systemActor metadata"
            }
        }

        require(changeMetadata?.isEmpty != true) {
            "AuditEvent changeMetadata must be omitted when there is no relevant change metadata"
        }
    }

    companion object {
        const val SYSTEM_SOURCE: String = "SYSTEM"
    }
}

enum class NotificationType {
    SYNC_ERROR,
    SYNC_CONFLICT,
    CAMPAIGN_ASSIGNMENT,
    ISSUE_REQUIRES_ATTENTION,
    CAMPAIGN_COMPLETED
}

data class NotificationContextReference(
    val entityType: String,
    val entityId: String
) {
    init {
        requireStableCode(entityType, "Notification context entityType")
        requireCanonicalUuidString(entityId, "Notification context entityId")
    }
}

data class Notification(
    val notificationId: NotificationId,
    val organizationId: OrganizationId,
    val userId: UserId,
    val type: NotificationType,
    val title: String,
    val message: String,
    val contextualReference: NotificationContextReference?,
    val createdAt: Instant,
    val readAt: Instant?
) {
    init {
        require(title.isNotBlank() && title == title.trim()) {
            "Notification title must be non-blank and trimmed"
        }
        require(message.isNotBlank() && message == message.trim()) {
            "Notification message must be non-blank and trimmed"
        }
        require(readAt == null || !readAt.isBefore(createdAt)) {
            "Notification readAt cannot be before createdAt"
        }
    }

    val isUnread: Boolean
        get() = readAt == null

    fun markRead(at: Instant): Notification {
        require(!at.isBefore(createdAt)) {
            "Notification readAt cannot be before createdAt"
        }
        return if (readAt == null) copy(readAt = at) else this
    }
}

fun markAllNotificationsRead(
    notifications: List<Notification>,
    at: Instant
): List<Notification> = notifications.map { it.markRead(at) }
