package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index("geometry_family_id")]
)
data class GeometryProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "geometry_family_id") val geometryFamilyId: Long,
    val remoteId: String? = null,
    @ColumnInfo(name = "profile_code") val profileCode: String,
    val name: String,
    val lifecycleStatus: String = "ACTIVE",
    val remoteRevision: Long? = null
)
