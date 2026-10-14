package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.silomath.core.model.SyncOperationState

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey
    @ColumnInfo(name = "sync_operation_id")
    val syncOperationId: String,

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,

    @ColumnInfo(name = "organization_id")
    val organizationId: String,

    @ColumnInfo(name = "campaign_id")
    val campaignId: String? = null,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "operation_type")
    val operationType: String,

    @ColumnInfo(name = "base_entity_version")
    val baseEntityVersion: Long = 1,

    @ColumnInfo(name = "event_at")
    val eventAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "payload_json")
    val payloadJson: String,

    @ColumnInfo(name = "state")
    val state: SyncOperationState = SyncOperationState.PENDING,

    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,

    @ColumnInfo(name = "retry_metadata")
    val retryMetadata: String? = null,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
