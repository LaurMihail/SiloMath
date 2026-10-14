package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "organizations",
    indices = [
        Index(value = ["organization_id"], unique = true),
        Index(value = ["code"], unique = true)
    ]
)
data class OrganizationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "organization_id")
    val organizationId: String? = null,

    val name: String,

    val code: String? = null,

    val address: String? = null,

    val city: String? = null,

    val country: String? = null,

    @ColumnInfo(name = "lifecycle_status", defaultValue = "ACTIVE")
    val lifecycleStatus: String = "ACTIVE",

    @ColumnInfo(name = "created_at")
    val createdAt: Long? = null,

    @ColumnInfo(name = "created_by")
    val createdBy: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null,

    @ColumnInfo(name = "updated_by")
    val updatedBy: String? = null,

    @ColumnInfo(name = "entity_version")
    val entityVersion: Long? = null
)
