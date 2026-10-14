package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_trail")
data class AuditTrailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: String,
    val action: String,
    val entity: String,
    @ColumnInfo(name = "prev_state") val prevState: String?,
    @ColumnInfo(name = "new_state") val newState: String?,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String
)
