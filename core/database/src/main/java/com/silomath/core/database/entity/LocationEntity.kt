package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    foreignKeys = [
        ForeignKey(
            entity = OrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["organization_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("organization_id"),
        Index(value = ["organization_id", "code"], unique = true)
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "organization_id") val organizationId: Long,
    val remoteId: String? = null,
    val code: String,
    val name: String,
    val lifecycleStatus: String = "ACTIVE",
    val remoteRevision: Long? = null,
    @ColumnInfo(name = "location_id") val locationId: String? = null,
    @ColumnInfo(name = "canonical_organization_id") val canonicalOrganizationId: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long? = null,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long? = null,
    @ColumnInfo(name = "updated_by") val updatedBy: String? = null,
    @ColumnInfo(name = "entity_version") val entityVersion: Long? = null
)
