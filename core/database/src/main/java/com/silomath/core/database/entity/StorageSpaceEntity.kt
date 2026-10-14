package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
        Index(value = ["storage_unit_id", "code"], unique = true)
    ]
)
data class StorageSpaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    @ColumnInfo(name = "storage_unit_id") val storageUnitId: Long,
    @ColumnInfo(name = "geometry_profile_id") val geometryProfileId: Long,
    val remoteId: String? = null,
    @ColumnInfo(name = "geometry_version_id") val geometryVersionId: Long,
    @ColumnInfo(name = "configured_dimensions") val configuredDimensions: String,
    val lifecycleStatus: String = "ACTIVE",
    val remoteRevision: Long? = null
)
