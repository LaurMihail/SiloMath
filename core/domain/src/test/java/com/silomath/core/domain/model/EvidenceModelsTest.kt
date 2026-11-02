package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.EvidenceId
import com.silomath.core.model.IssueId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.StocktakingRecordId
import com.silomath.core.model.StocktakingRevisionId
import com.silomath.core.model.UserId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceModelsTest {

    private val organizationId = OrganizationId("11111111-1111-4111-8111-111111111111")
    private val campaignId = CampaignId("22222222-2222-4222-8222-222222222222")
    private val recordId = StocktakingRecordId("33333333-3333-4333-8333-333333333333")
    private val revisionId = StocktakingRevisionId("44444444-4444-4444-8444-444444444444")
    private val issueId = IssueId("55555555-5555-4555-8555-555555555555")
    private val userId = UserId("66666666-6666-4666-8666-666666666666")
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val removedAt = Instant.parse("2026-01-02T00:00:00Z")
    private val hash = "a".repeat(64)

    @Test
    fun `evidence types and sync states are exactly frozen values`() {
        assertEquals(listOf("PHOTO", "NOTE"), EvidenceType.entries.map { it.name })
        assertEquals(
            listOf("LOCAL_PENDING", "UPLOADING", "SYNCED", "SYNC_ERROR"),
            EvidenceSyncState.entries.map { it.name }
        )
    }

    @Test
    fun `evidence requires exactly one principal owner`() {
        assertThrows(IllegalArgumentException::class.java) {
            note(stocktakingRecordId = null, issueId = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            note(stocktakingRecordId = recordId, issueId = issueId)
        }
    }

    @Test
    fun `issue evidence never binds to stocktaking revision`() {
        assertThrows(IllegalArgumentException::class.java) {
            note(stocktakingRecordId = null, issueId = issueId, stocktakingRevisionId = revisionId)
        }
    }

    @Test
    fun `note requires trimmed non empty text no binary metadata and no uploading phase`() {
        assertThrows(IllegalArgumentException::class.java) { note(text = "   ") }
        assertThrows(IllegalArgumentException::class.java) { note(text = " note ") }
        assertThrows(IllegalArgumentException::class.java) { note(text = "n".repeat(2001)) }
        assertThrows(IllegalArgumentException::class.java) {
            note(syncState = EvidenceSyncState.UPLOADING)
        }

        val valid = note(text = "Observed condition")
        assertEquals("Observed condition", valid.noteText)
    }

    @Test
    fun `photo enforces canonical jpeg integrity and dimensions`() {
        assertThrows(IllegalArgumentException::class.java) { photo(sha256 = "ABC") }
        assertThrows(IllegalArgumentException::class.java) { photo(mimeType = "image/png") }
        assertThrows(IllegalArgumentException::class.java) { photo(byteSize = 3_000_001L) }
        assertThrows(IllegalArgumentException::class.java) { photo(pixelWidth = 2049) }
        assertThrows(IllegalArgumentException::class.java) {
            photo(remoteObjectRef = "https://public.example/evidence.jpg", syncState = EvidenceSyncState.SYNCED)
        }
    }

    @Test
    fun `pending uploading and sync error photo retain local durable bytes`() {
        listOf(
            EvidenceSyncState.LOCAL_PENDING,
            EvidenceSyncState.UPLOADING,
            EvidenceSyncState.SYNC_ERROR
        ).forEach { state ->
            assertThrows(IllegalArgumentException::class.java) {
                photo(localPrivateRef = null, remoteObjectRef = "org/campaign/evidence/$hash.jpg", syncState = state)
            }
        }
    }

    @Test
    fun `synced photo requires opaque remote object reference and may retain local cache`() {
        val evidence = photo(
            remoteObjectRef = "org/campaign/evidence/$hash.jpg",
            syncState = EvidenceSyncState.SYNCED
        )
        assertEquals(EvidenceSyncState.SYNCED, evidence.syncState)
    }

    @Test
    fun `validated stocktaking evidence binds to exact revision`() {
        val evidence = photo(stocktakingRevisionId = revisionId)
        EvidenceDomainRules.validateStocktakingRevisionBindingForValidation(evidence, revisionId)

        val otherRevision = StocktakingRevisionId("77777777-7777-4777-8777-777777777777")
        assertThrows(IllegalArgumentException::class.java) {
            EvidenceDomainRules.validateStocktakingRevisionBindingForValidation(evidence, otherRevision)
        }
    }

    @Test
    fun `logical removal records actor and time without rewriting content`() {
        val before = note()
        val after = before.copy(removedAt = removedAt, removedBy = userId)

        EvidenceDomainRules.validateLogicalRemoval(
            evidenceBefore = before,
            evidenceAfter = after,
            isFrozenHistorical = false,
            campaignStatus = CampaignStatus.ACTIVE
        )
        assertFalse(after.isActive)
    }

    @Test
    fun `validated or completed evidence cannot be logically removed`() {
        val before = note()
        val after = before.copy(removedAt = removedAt, removedBy = userId)

        assertThrows(IllegalArgumentException::class.java) {
            EvidenceDomainRules.validateLogicalRemoval(before, after, true, CampaignStatus.ACTIVE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EvidenceDomainRules.validateLogicalRemoval(before, after, false, CampaignStatus.COMPLETED)
        }
    }

    @Test
    fun `maximum five active photos per stocktaking revision`() {
        val five = (1..5).map { index -> photo(id = evidenceId(index), stocktakingRevisionId = revisionId) }
        EvidenceDomainRules.validatePhotoLimits(five)

        assertThrows(IllegalArgumentException::class.java) {
            EvidenceDomainRules.validatePhotoLimits(
                five + photo(id = evidenceId(6), stocktakingRevisionId = revisionId)
            )
        }
    }

    @Test
    fun `maximum five active photos per issue and note never consumes photo limit`() {
        val photos = (1..5).map { index ->
            photo(
                id = evidenceId(index),
                stocktakingRecordId = null,
                issueId = issueId,
                stocktakingRevisionId = null
            )
        }
        val notes = (6..12).map { index ->
            note(
                id = evidenceId(index),
                stocktakingRecordId = null,
                issueId = issueId,
                stocktakingRevisionId = null
            )
        }
        EvidenceDomainRules.validatePhotoLimits(photos + notes)
        assertTrue(notes.all { it.evidenceType == EvidenceType.NOTE })
    }

    private fun note(
        id: EvidenceId = evidenceId(20),
        text: String = "Observation",
        stocktakingRecordId: StocktakingRecordId? = recordId,
        issueId: IssueId? = null,
        stocktakingRevisionId: StocktakingRevisionId? = null,
        syncState: EvidenceSyncState = EvidenceSyncState.LOCAL_PENDING
    ) = Evidence(
        evidenceId = id,
        organizationId = organizationId,
        campaignId = campaignId,
        stocktakingRecordId = stocktakingRecordId,
        issueId = issueId,
        stocktakingRevisionId = stocktakingRevisionId,
        evidenceType = EvidenceType.NOTE,
        noteText = text,
        localPrivateRef = null,
        remoteObjectRef = null,
        sha256 = null,
        mimeType = null,
        byteSize = null,
        pixelWidth = null,
        pixelHeight = null,
        syncState = syncState,
        removedAt = null,
        removedBy = null,
        createdAt = createdAt,
        createdBy = userId
    )

    private fun photo(
        id: EvidenceId = evidenceId(30),
        stocktakingRecordId: StocktakingRecordId? = recordId,
        issueId: IssueId? = null,
        stocktakingRevisionId: StocktakingRevisionId? = revisionId,
        localPrivateRef: String? = "private/evidence.jpg",
        remoteObjectRef: String? = null,
        sha256: String = hash,
        mimeType: String = Evidence.CANONICAL_PHOTO_MIME_TYPE,
        byteSize: Long = 1_000_000L,
        pixelWidth: Int = 1600,
        pixelHeight: Int = 1200,
        syncState: EvidenceSyncState = EvidenceSyncState.LOCAL_PENDING
    ) = Evidence(
        evidenceId = id,
        organizationId = organizationId,
        campaignId = campaignId,
        stocktakingRecordId = stocktakingRecordId,
        issueId = issueId,
        stocktakingRevisionId = stocktakingRevisionId,
        evidenceType = EvidenceType.PHOTO,
        noteText = null,
        localPrivateRef = localPrivateRef,
        remoteObjectRef = remoteObjectRef,
        sha256 = sha256,
        mimeType = mimeType,
        byteSize = byteSize,
        pixelWidth = pixelWidth,
        pixelHeight = pixelHeight,
        syncState = syncState,
        removedAt = null,
        removedBy = null,
        createdAt = createdAt,
        createdBy = userId
    )

    private fun evidenceId(index: Int): EvidenceId {
        val suffix = index.toString().padStart(12, '0')
        return EvidenceId("99999999-9999-4999-8999-$suffix")
    }
}
