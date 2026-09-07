package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "campaign_scope",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaign_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = StorageSpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["storage_space_id"],
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
        Index("campaign_id"),
        Index("storage_space_id"),
        Index("geometry_version_id"),
        Index(value = ["campaign_id", "storage_space_id"], unique = true)
    ]
)
data class CampaignScopeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "campaign_id") val campaignId: Long,
    @ColumnInfo(name = "storage_space_id") val storageSpaceId: Long,
    @ColumnInfo(name = "geometry_version_id") val geometryVersionId: Long
)
