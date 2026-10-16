package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transitional Storage Unit Room entity.
 *
 * Migration stage: EXPAND.
 *
 * Legacy Long identity/FK fields remain until dependent persistence is
 * migrated to canonical UUID identifiers.
 */
@Entity(
    tableName = "storage_units",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("location_id"),
        Index(value = ["location_id", "code"], unique = true),
        Index(value = ["storage_unit_id"], unique = true),
        Index("canonical_location_id"),
        Index("canonical_organization_id")
    ]
)
data class StorageUnitEntity(

    // Legacy migration shim.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "location_id")
    val locationId: Long,

    val remoteId: String? = null,
    val code: String,
    val name: String? = null,
    val type: String? = null,
    val lifecycleStatus: String = "ACTIVE",
    val remoteRevision: Long? = null,

    // Canonical V6 expand fields.
    @ColumnInfo(name = "storage_unit_id")
    val storageUnitId: String? = null,

    @ColumnInfo(name = "canonical_location_id")
    val canonicalLocationId: String? = null,

    @ColumnInfo(name = "canonical_organization_id")
    val canonicalOrganizationId: String? = null,

    val description: String? = null,

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
