package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stocktaking_records",
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
            entity = CommodityEntity::class,
            parentColumns = ["id"],
            childColumns = ["commodity_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("campaign_id"),
        Index("storage_space_id"),
        Index("commodity_id"),
        Index("client_id"),
        Index(value = ["campaign_id", "storage_space_id"], unique = true),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class StocktakingRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "campaign_id") val campaignId: Long,
    @ColumnInfo(name = "storage_space_id") val storageSpaceId: Long,
    @ColumnInfo(name = "commodity_id") val commodityId: Long?,
    val density: Double?,
    val ownership: String, // Owned/Third-Party
    @ColumnInfo(name = "client_id") val clientId: Long?,
    val status: String,
    val remoteId: String? = null,
    @ColumnInfo(name = "remote_revision") val remoteRevision: Long? = null
)
