package com.silomath.core.model

import kotlinx.serialization.Serializable

enum class SyncOperationState {
    PENDING,
    IN_FLIGHT,
    ACKNOWLEDGED,
    RETRYABLE_ERROR,
    CONFLICT,
    PERMANENT_ERROR
}

@Serializable
data class SyncOperation(
    val syncOperationId: String,
    val idempotencyKey: String,
    val organizationId: String,
    val campaignId: String? = null,
    val entityType: String,
    val entityId: String,
    val operationType: String,
    val baseEntityVersion: Long = 1,
    val eventAt: Long = System.currentTimeMillis(),
    val payload: String,
    val state: SyncOperationState = SyncOperationState.PENDING,
    val attemptCount: Int = 0,
    val retryMetadata: String? = null,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
