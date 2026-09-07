package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey @ColumnInfo(name = "client_operation_uuid") val clientOperationUuid: String,
    @ColumnInfo(name = "op_type") val opType: String,
    @ColumnInfo(name = "target_ref") val targetRef: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    val status: String,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
