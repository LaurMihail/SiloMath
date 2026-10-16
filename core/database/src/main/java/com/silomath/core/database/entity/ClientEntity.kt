package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transitional Client Room entity.
 *
 * Migration stage: EXPAND.
 *
 * Legacy Long organization FK remains until dependent persistence is
 * migrated to canonical UUID identifiers.
 */
@Entity(
    tableName = "clients",
    foreignKeys = [
        ForeignKey(
            entity = OrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["organization_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("organization_id"),
        Index(value = ["organization_id", "code"], unique = true),
        Index(value = ["remoteId"], unique = true),
        Index(value = ["client_id"], unique = true),
        Index("canonical_organization_id")
    ]
)
data class ClientEntity(

    // Legacy migration shim.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "organization_id")
    val organizationId: Long,

    val remoteId: String? = null,
    val name: String,

    @ColumnInfo(name = "lifecycle_status")
    val lifecycleStatus: String = "ACTIVE",

    @ColumnInfo(name = "remote_revision")
    val remoteRevision: Long? = null,

    // Canonical V6 expand fields.
    @ColumnInfo(name = "client_id")
    val clientId: String? = null,

    @ColumnInfo(name = "canonical_organization_id")
    val canonicalOrganizationId: String? = null,

    val code: String? = null,

    @ColumnInfo(name = "registration_tax_id")
    val registrationTaxId: String? = null,

    val notes: String? = null,

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
