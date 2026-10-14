package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
        Index(value = ["remoteId"], unique = true)
    ]
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "organization_id") val organizationId: Long,
    val remoteId: String? = null,
    val name: String,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String = "ACTIVE",
    @ColumnInfo(name = "remote_revision") val remoteRevision: Long? = null
)
