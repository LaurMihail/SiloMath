package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email_normalized"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "email_normalized")
    val emailNormalized: String,

    @ColumnInfo(name = "lifecycle_status")
    val lifecycleStatus: String = "ACTIVE",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_by")
    val createdBy: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_by")
    val updatedBy: String,

    @ColumnInfo(name = "entity_version")
    val entityVersion: Long = 1
) {
    companion object {
        fun normalizeEmail(rawEmail: String): String {
            return rawEmail.trim().lowercase(java.util.Locale.ROOT)
        }
    }
}
