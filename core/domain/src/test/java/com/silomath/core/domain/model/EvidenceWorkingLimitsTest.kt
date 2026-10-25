package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.EvidenceId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.StocktakingRecordId
import com.silomath.core.model.StocktakingRevisionId
import com.silomath.core.model.UserId
import java.time.Instant
import org.junit.Assert.assertThrows
import org.junit.Test

class EvidenceWorkingLimitsTest {

    private val organizationId = OrganizationId("11111111-1111-4111-8111-111111111111")
    private val campaignId = CampaignId("22222222-2222-4222-8222-222222222222")
    private val recordId = StocktakingRecordId("33333333-3333-4333-8333-333333333333")
    private val currentRevisionId = StocktakingRevisionId("44444444-4444-4444-8444-444444444444")
    private val priorRevisionId = StocktakingRevisionId("55555555-5555-4555-8555-555555555555")
    private val userId = UserId("66666666-6666-4666-8666-666666666666")
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val hash = "a".repeat(64)

    @Test
    fun `unbound working photos still enforce five photo limit`() {
        val five = (1..5).map { photo(it, revisionId = null) }
        EvidenceDomainRules.validatePhotoLimits(five)

        assertThrows(IllegalArgumentException::class.java) {
            EvidenceDomainRules.validatePhotoLimits(five + photo(6, revisionId = null))
        }
    }

    @Test
    fun `current working revision budget includes bound and not yet bound photos`() {
        val evidence = listOf(
            photo(1, currentRevisionId),
            photo(2, currentRevisionId),
            photo(3, revisionId = null),
            photo(4, revisionId = null),
            photo(5, revisionId = null),
            photo(20, priorRevisionId)
        )
        EvidenceDomainRules.validateWorkingStocktakingPhotoLimit(
            stocktakingRecordId = recordId,
            currentWorkingRevisionId = currentRevisionId,
            evidence = evidence
        )

        assertThrows(IllegalArgumentException::class.java) {
            EvidenceDomainRules.validateWorkingStocktakingPhotoLimit(
                stocktakingRecordId = recordId,
                currentWorkingRevisionId = currentRevisionId,
                evidence = evidence + photo(6, revisionId = null)
            )
        }
    }

    @Test
    fun `local private reference is opaque rather than public uri or absolute path`() {
        assertThrows(IllegalArgumentException::class.java) {
            photo(1, localPrivateRef = "file:///data/user/0/com.silomath/evidence.jpg")
        }
        assertThrows(IllegalArgumentException::class.java) {
            photo(2, localPrivateRef = "/sdcard/DCIM/evidence.jpg")
        }
    }

    private fun photo(
        index: Int,
        revisionId: StocktakingRevisionId? = currentRevisionId,
        localPrivateRef: String = "private/evidence-$index.jpg"
    ) = Evidence(
        evidenceId = EvidenceId("99999999-9999-4999-8999-${index.toString().padStart(12, '0')}"),
        organizationId = organizationId,
        campaignId = campaignId,
        stocktakingRecordId = recordId,
        issueId = null,
        stocktakingRevisionId = revisionId,
        evidenceType = EvidenceType.PHOTO,
        noteText = null,
        localPrivateRef = localPrivateRef,
        remoteObjectRef = null,
        sha256 = hash,
        mimeType = Evidence.CANONICAL_PHOTO_MIME_TYPE,
        byteSize = 1_000_000L,
        pixelWidth = 1600,
        pixelHeight = 1200,
        syncState = EvidenceSyncState.LOCAL_PENDING,
        removedAt = null,
        removedBy = null,
        createdAt = createdAt,
        createdBy = userId
    )
}
