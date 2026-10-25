package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.EvidenceId
import com.silomath.core.model.IssueId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.StocktakingRecordId
import com.silomath.core.model.StocktakingRevisionId
import com.silomath.core.model.UserId
import java.time.Instant

enum class EvidenceType {
    PHOTO,
    NOTE
}

enum class EvidenceSyncState {
    LOCAL_PENDING,
    UPLOADING,
    SYNCED,
    SYNC_ERROR
}

data class Evidence(
    val evidenceId: EvidenceId,
    val organizationId: OrganizationId,
    val campaignId: CampaignId,
    val stocktakingRecordId: StocktakingRecordId?,
    val issueId: IssueId?,
    val stocktakingRevisionId: StocktakingRevisionId?,
    val evidenceType: EvidenceType,
    val noteText: String?,
    val localPrivateRef: String?,
    val remoteObjectRef: String?,
    val sha256: String?,
    val mimeType: String?,
    val byteSize: Long?,
    val pixelWidth: Int?,
    val pixelHeight: Int?,
    val syncState: EvidenceSyncState,
    val removedAt: Instant?,
    val removedBy: UserId?,
    val createdAt: Instant,
    val createdBy: UserId
) {
    init {
        require((stocktakingRecordId != null) xor (issueId != null)) {
            "Evidence must have exactly one principal owner: StocktakingRecord XOR Issue"
        }
        if (issueId != null) {
            require(stocktakingRevisionId == null) {
                "Issue-owned Evidence must not bind to a StocktakingRevision"
            }
        }
        require((removedAt == null) == (removedBy == null)) {
            "Evidence logical removal requires removedAt and removedBy together"
        }

        when (evidenceType) {
            EvidenceType.NOTE -> validateNote()
            EvidenceType.PHOTO -> validatePhoto()
        }
    }

    val isActive: Boolean
        get() = removedAt == null

    private fun validateNote() {
        require(noteText != null && noteText.isNotBlank()) {
            "NOTE Evidence requires non-empty noteText"
        }
        require(noteText == noteText.trim()) {
            "NOTE Evidence noteText must be trimmed"
        }
        require(noteText.length <= MAX_NOTE_CHARACTERS) {
            "NOTE Evidence noteText must be <= $MAX_NOTE_CHARACTERS Unicode characters"
        }
        require(localPrivateRef == null) { "NOTE Evidence must not have localPrivateRef" }
        require(remoteObjectRef == null) { "NOTE Evidence must not have remoteObjectRef" }
        require(sha256 == null) { "NOTE Evidence must not have sha256" }
        require(mimeType == null) { "NOTE Evidence must not have mimeType" }
        require(byteSize == null) { "NOTE Evidence must not have byteSize" }
        require(pixelWidth == null && pixelHeight == null) {
            "NOTE Evidence must not have image dimensions"
        }
        require(syncState != EvidenceSyncState.UPLOADING) {
            "NOTE Evidence does not enter the binary UPLOADING phase"
        }
    }

    private fun validatePhoto() {
        require(noteText == null) { "PHOTO Evidence must not have noteText" }
        require(localPrivateRef != null || remoteObjectRef != null) {
            "PHOTO Evidence requires a durable local or remote binary reference"
        }
        require(localPrivateRef == null || localPrivateRef.isNotBlank()) {
            "PHOTO localPrivateRef must be opaque and non-blank"
        }
        require(localPrivateRef == null || !localPrivateRef.contains("://")) {
            "PHOTO localPrivateRef must be an opaque app-private reference, not a URI"
        }
        require(localPrivateRef == null || !localPrivateRef.startsWith("/")) {
            "PHOTO localPrivateRef must not expose an absolute filesystem path"
        }
        require(remoteObjectRef == null || remoteObjectRef.isNotBlank()) {
            "PHOTO remoteObjectRef must be opaque and non-blank"
        }
        require(remoteObjectRef == null || !remoteObjectRef.contains("://")) {
            "PHOTO remoteObjectRef must be an opaque private reference, not a public URL"
        }
        require(sha256 != null && SHA_256_REGEX.matches(sha256)) {
            "PHOTO Evidence requires lowercase SHA-256 over canonical JPEG bytes"
        }
        require(mimeType == CANONICAL_PHOTO_MIME_TYPE) {
            "PHOTO Evidence mimeType must be image/jpeg"
        }
        require(byteSize != null && byteSize in 1..MAX_PHOTO_BYTES) {
            "PHOTO Evidence byteSize must be within the 3 MB hard maximum"
        }
        require(pixelWidth != null && pixelHeight != null && pixelWidth > 0 && pixelHeight > 0) {
            "PHOTO Evidence requires positive canonical JPEG dimensions"
        }
        require(maxOf(pixelWidth, pixelHeight) <= MAX_PHOTO_LONG_EDGE_PX) {
            "PHOTO Evidence long edge must be <= $MAX_PHOTO_LONG_EDGE_PX px"
        }

        when (syncState) {
            EvidenceSyncState.LOCAL_PENDING,
            EvidenceSyncState.UPLOADING,
            EvidenceSyncState.SYNC_ERROR -> require(localPrivateRef != null) {
                "Unsynchronized PHOTO Evidence requires its durable local private binary"
            }

            EvidenceSyncState.SYNCED -> require(remoteObjectRef != null) {
                "SYNCED PHOTO Evidence requires an opaque remoteObjectRef"
            }
        }
    }

    companion object {
        const val MAX_NOTE_CHARACTERS = 2000
        const val MAX_PHOTO_COUNT = 5
        const val MAX_PHOTO_LONG_EDGE_PX = 2048
        const val MAX_PHOTO_BYTES = 3_000_000L
        const val CANONICAL_PHOTO_MIME_TYPE = "image/jpeg"
        private val SHA_256_REGEX = Regex("^[0-9a-f]{64}$")
    }
}

object EvidenceDomainRules {

    fun validateStocktakingRevisionBindingForValidation(
        evidence: Evidence,
        validatedRevisionId: StocktakingRevisionId
    ) {
        require(evidence.stocktakingRecordId != null && evidence.issueId == null) {
            "Only Stocktaking-owned Evidence binds to a StocktakingRevision"
        }
        require(evidence.stocktakingRevisionId == validatedRevisionId) {
            "Validated Stocktaking Evidence must be bound to the exact validated revision"
        }
        require(evidence.isActive) {
            "Removed working Evidence must not be frozen into validated history"
        }
    }

    fun validateLogicalRemoval(
        evidenceBefore: Evidence,
        evidenceAfter: Evidence,
        isFrozenHistorical: Boolean,
        campaignStatus: CampaignStatus
    ) {
        require(!isFrozenHistorical) {
            "Validated historical Evidence is immutable and cannot be removed"
        }
        require(campaignStatus == CampaignStatus.ACTIVE) {
            "COMPLETED Campaign Evidence is historical read-only"
        }
        require(evidenceBefore.evidenceId == evidenceAfter.evidenceId) {
            "Evidence identity must remain stable during logical removal"
        }
        require(evidenceBefore.isActive && !evidenceAfter.isActive) {
            "Evidence removal must transition active metadata to logical removal"
        }
        require(evidenceAfter.removedAt != null && evidenceAfter.removedBy != null) {
            "Evidence logical removal must record actor and timestamp"
        }
        require(contentIdentity(evidenceBefore) == contentIdentity(evidenceAfter)) {
            "Logical removal must not rewrite Evidence content, ownership, binding or integrity metadata"
        }
    }

    /**
     * Enforces the frozen five-PHOTO rule across already-bound historical revisions,
     * unbound working Stocktaking evidence, and Issue-owned evidence.
     *
     * Unbound working evidence is grouped by StocktakingRecord because V6 explicitly
     * allows stocktakingRevisionId to remain null until Validation binds the evidence
     * to the exact validated revision.
     */
    fun validatePhotoLimits(evidence: Collection<Evidence>) {
        val activePhotos = evidence.filter { it.evidenceType == EvidenceType.PHOTO && it.isActive }

        activePhotos
            .filter { it.stocktakingRecordId != null && it.stocktakingRevisionId != null }
            .groupBy { it.stocktakingRevisionId }
            .forEach { (revisionId, photos) ->
                require(photos.size <= Evidence.MAX_PHOTO_COUNT) {
                    "StocktakingRevision $revisionId exceeds the five-PHOTO limit"
                }
            }

        activePhotos
            .filter { it.stocktakingRecordId != null && it.stocktakingRevisionId == null }
            .groupBy { it.stocktakingRecordId }
            .forEach { (recordId, photos) ->
                require(photos.size <= Evidence.MAX_PHOTO_COUNT) {
                    "Working StocktakingRecord $recordId exceeds the five-PHOTO limit before revision binding"
                }
            }

        activePhotos
            .filter { it.issueId != null }
            .groupBy { it.issueId }
            .forEach { (issueId, photos) ->
                require(photos.size <= Evidence.MAX_PHOTO_COUNT) {
                    "Issue $issueId exceeds the five-PHOTO limit"
                }
            }
    }

    /**
     * Enforces the current working revision limit when the caller knows the current
     * revision identity. Unbound working photos and photos already bound to that
     * revision consume the same five-photo budget; prior historical revisions do not.
     */
    fun validateWorkingStocktakingPhotoLimit(
        stocktakingRecordId: StocktakingRecordId,
        currentWorkingRevisionId: StocktakingRevisionId,
        evidence: Collection<Evidence>
    ) {
        val workingPhotos = evidence.filter {
            it.isActive &&
                it.evidenceType == EvidenceType.PHOTO &&
                it.stocktakingRecordId == stocktakingRecordId &&
                it.issueId == null &&
                (it.stocktakingRevisionId == null || it.stocktakingRevisionId == currentWorkingRevisionId)
        }
        require(workingPhotos.size <= Evidence.MAX_PHOTO_COUNT) {
            "Current StocktakingRevision $currentWorkingRevisionId exceeds the five-PHOTO limit"
        }
    }

    private fun contentIdentity(evidence: Evidence): List<Any?> = listOf(
        evidence.organizationId,
        evidence.campaignId,
        evidence.stocktakingRecordId,
        evidence.issueId,
        evidence.stocktakingRevisionId,
        evidence.evidenceType,
        evidence.noteText,
        evidence.localPrivateRef,
        evidence.remoteObjectRef,
        evidence.sha256,
        evidence.mimeType,
        evidence.byteSize,
        evidence.pixelWidth,
        evidence.pixelHeight,
        evidence.createdAt,
        evidence.createdBy
    )
}
