package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transitional Storage Space Room entity.
 *
 * Migration stage: EXPAND.
 *
 * Legacy Long identity/FKs and legacy geometry configuration remain until
 * dependent persistence is migrated to canonical UUID identifiers.
 */
@Entity(
    tableName = "storage_spaces",
    foreignKeys = [
        ForeignKey(
            entity = StorageUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["storage_unit_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GeometryProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["geometry_profile_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = GeometryVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["geometry_version_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("storage_unit_id"),
        Index("geometry_profile_id"),
        Index("geometry_version_id"),
        Index(value = ["storage_unit_id", "code"], unique = true),
        Index(value = ["storage_space_id"], unique = true),
        Index("canonical_storage_unit_id"),
        Index("canonical_organization_id"),
        Index("canonical_geometry_profile_id")
    ]
)
data class StorageSpaceEntity(

    // Legacy migration shim.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val code: String,

    @ColumnInfo(name = "storage_unit_id")
    val storageUnitId: Long,

    @ColumnInfo(name = "geometry_profile_id")
    val geometryProfileId: Long,

    val remoteId: String? = null,

    @ColumnInfo(name = "geometry_version_id")
    val geometryVersionId: Long? = null,

    @ColumnInfo(name = "configured_dimensions")
    val configuredDimensions: String? = null,

    val lifecycleStatus: String = "ACTIVE",
    val remoteRevision: Long? = null,

    // Canonical V6 expand fields.
    @ColumnInfo(name = "storage_space_id")
    val storageSpaceId: String? = null,

    @ColumnInfo(name = "canonical_storage_unit_id")
    val canonicalStorageUnitId: String? = null,

    @ColumnInfo(name = "canonical_organization_id")
    val canonicalOrganizationId: String? = null,

    @ColumnInfo(name = "canonical_geometry_profile_id")
    val canonicalGeometryProfileId: String? = null,

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
