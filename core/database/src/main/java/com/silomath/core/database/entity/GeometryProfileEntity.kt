package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transitional Geometry Profile Room entity.
 *
 * DOM003 adds only the canonical identity bridge required by
 * StorageSpace. Full Geometry Domain alignment belongs to IMP-DOM-004.
 */
@Entity(
    tableName = "geometry_profiles",
    foreignKeys = [
        ForeignKey(
            entity = GeometryFamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["geometry_family_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("geometry_family_id"),
        Index(value = ["geometry_profile_id"], unique = true)
    ]
)
data class GeometryProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "geometry_family_id")
    val geometryFamilyId: Long,

    val remoteId: String? = null,

    @ColumnInfo(name = "profile_code")
    val profileCode: String,

    val name: String,
    val lifecycleStatus: String = "ACTIVE",
    val remoteRevision: Long? = null,

    @ColumnInfo(name = "geometry_profile_id")
    val canonicalGeometryProfileId: String? = null
)
