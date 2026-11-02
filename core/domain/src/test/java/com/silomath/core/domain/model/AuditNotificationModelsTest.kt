package com.silomath.core.domain.model

import com.silomath.core.model.AuditEventId
import com.silomath.core.model.CampaignId
import com.silomath.core.model.NotificationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.UserId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditNotificationModelsTest {

    private val organizationId = OrganizationId("11111111-1111-1111-1111-111111111111")
    private val userId = UserId("22222222-2222-2222-2222-222222222222")
    private val campaignId = CampaignId("33333333-3333-3333-3333-333333333333")
    private val eventId = AuditEventId("44444444-4444-4444-4444-444444444444")
    private val notificationId = NotificationId("55555555-5555-5555-5555-555555555555")
    private val entityId = "66666666-6666-6666-6666-666666666666"
    private val createdAt = Instant.parse("2026-10-18T10:00:00Z")

    @Test
    fun notificationTaxonomyIsExactlyFrozenSet() {
        assertEquals(
            setOf(
                "SYNC_ERROR",
                "SYNC_CONFLICT",
                "CAMPAIGN_ASSIGNMENT",
                "ISSUE_REQUIRES_ATTENTION",
                "CAMPAIGN_COMPLETED"
            ),
            NotificationType.entries.map { it.name }.toSet()
        )
    }

    @Test
    fun humanAuditEventAllowsHumanActorAndNonSystemSource() {
        val event = humanAuditEvent()

        assertEquals(userId, event.actorUserId)
        assertEquals("ANDROID", event.source)
    }

    @Test
    fun systemAuditEventRequiresSystemSourceAndSystemActor() {
        assertThrows(IllegalArgumentException::class.java) {
            AuditEvent(
                eventId = eventId,
                organizationId = organizationId,
                actorUserId = null,
                action = "CAMPAIGN_CLOSE",
                entityType = "InventoryCampaign",
                entityId = campaignId.value,
                timestampUtc = createdAt,
                campaignId = campaignId,
                changeMetadata = null,
                correlationId = "corr-1",
                source = "SERVER",
                systemActor = "close-worker"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            AuditEvent(
                eventId = eventId,
                organizationId = organizationId,
                actorUserId = null,
                action = "CAMPAIGN_CLOSE",
                entityType = "InventoryCampaign",
                entityId = campaignId.value,
                timestampUtc = createdAt,
                campaignId = campaignId,
                changeMetadata = null,
                correlationId = "corr-1",
                source = AuditEvent.SYSTEM_SOURCE,
                systemActor = null
            )
        }
    }

    @Test
    fun auditMetadataRejectsSecretOrBinaryFieldNames() {
        assertThrows(IllegalArgumentException::class.java) {
            AuditChangeMetadata(after = mapOf("accessToken" to "secret"))
        }

        assertThrows(IllegalArgumentException::class.java) {
            AuditChangeMetadata(changes = mapOf("photo_bytes" to "..."))
        }
    }

    @Test
    fun auditEventRejectsEmptyMetadataContainer() {
        assertThrows(IllegalArgumentException::class.java) {
            humanAuditEvent(changeMetadata = AuditChangeMetadata())
        }
    }

    @Test
    fun notificationStartsUnreadAndMarkReadIsIdempotent() {
        val notification = notification()
        val readAt = createdAt.plusSeconds(60)

        assertTrue(notification.isUnread)

        val read = notification.markRead(readAt)
        assertFalse(read.isUnread)
        assertEquals(readAt, read.readAt)

        val secondRead = read.markRead(createdAt.plusSeconds(120))
        assertSame(read, secondRead)
        assertEquals(readAt, secondRead.readAt)
    }

    @Test
    fun markAllReadMarksOnlyUnreadNotificationsWithoutRewritingExistingReadAt() {
        val existingReadAt = createdAt.plusSeconds(10)
        val existingRead = notification().copy(readAt = existingReadAt)
        val unread = notification(
            notificationId = NotificationId("77777777-7777-7777-7777-777777777777")
        )
        val markAt = createdAt.plusSeconds(120)

        val result = markAllNotificationsRead(listOf(existingRead, unread), markAt)

        assertEquals(existingReadAt, result[0].readAt)
        assertEquals(markAt, result[1].readAt)
    }

    @Test
    fun notificationContextRequiresStableUuidIdentity() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificationContextReference(
                entityType = "Issue",
                entityId = "ISS-42"
            )
        }
    }

    @Test
    fun notificationRejectsReadTimestampBeforeCreation() {
        assertThrows(IllegalArgumentException::class.java) {
            notification(readAt = createdAt.minusSeconds(1))
        }
    }

    private fun humanAuditEvent(
        changeMetadata: AuditChangeMetadata? = null
    ): AuditEvent = AuditEvent(
        eventId = eventId,
        organizationId = organizationId,
        actorUserId = userId,
        action = "ISSUE_RESOLVE",
        entityType = "Issue",
        entityId = entityId,
        timestampUtc = createdAt,
        campaignId = campaignId,
        changeMetadata = changeMetadata,
        correlationId = "corr-123",
        source = "ANDROID",
        systemActor = null
    )

    private fun notification(
        notificationId: NotificationId = this.notificationId,
        readAt: Instant? = null
    ): Notification = Notification(
        notificationId = notificationId,
        organizationId = organizationId,
        userId = userId,
        type = NotificationType.ISSUE_REQUIRES_ATTENTION,
        title = "Issue requires attention",
        message = "Review the issue before campaign close.",
        contextualReference = NotificationContextReference(
            entityType = "Issue",
            entityId = entityId
        ),
        createdAt = createdAt,
        readAt = readAt
    )
}
