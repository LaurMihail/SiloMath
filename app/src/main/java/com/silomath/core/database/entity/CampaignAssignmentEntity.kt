package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "campaign_assignments",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaign_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("campaign_id"),
        Index(value = ["campaign_id", "user_id"], unique = true)
    ]
)
data class CampaignAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "campaign_id") val campaignId: Long,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "role_at_assignment") val roleAtAssignment: String
)
