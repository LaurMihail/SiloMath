package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "organization_memberships",
    primaryKeys = ["legacy_organization_id", "user_id"],
    foreignKeys = [
        ForeignKey(
            entity = OrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["legacy_organization_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = OrganizationEntity::class,
            parentColumns = ["organization_id"],
            childColumns = ["organization_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["legacy_organization_id"]),
        Index(value = ["organization_id"]),
        Index(value = ["user_id"])
    ]
)
data class OrganizationMembershipEntity(
    @ColumnInfo(name = "legacy_organization_id")
    val legacyOrganizationId: Long,

    @ColumnInfo(name = "organization_id")
    val organizationId: String? = null,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "legacy_role")
    val legacyRole: String,

    val role: String? = null,

    @ColumnInfo(name = "membership_id")
    val membershipId: String? = null,

    @ColumnInfo(name = "lifecycle_status")
    val lifecycleStatus: String? = null,

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
