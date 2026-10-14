package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "organization_members",
    primaryKeys = ["organization_id", "user_id"],
    foreignKeys = [
        ForeignKey(
            entity = OrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["organization_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["organization_id"]),
        Index(value = ["user_id"])
    ]
)
data class OrganizationMemberEntity(
    @ColumnInfo(name = "organization_id") val organizationId: Long,
    @ColumnInfo(name = "user_id") val userId: String,
    val role: String
)
