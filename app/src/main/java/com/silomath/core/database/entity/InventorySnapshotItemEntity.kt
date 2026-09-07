package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_snapshot_items",
    foreignKeys = [
        ForeignKey(
            entity = InventorySnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshot_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("snapshot_id")]
)
data class InventorySnapshotItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "snapshot_id") val snapshotId: Long,
    @ColumnInfo(name = "storage_space_code") val storageSpaceCode: String,
    @ColumnInfo(name = "captured_geometry_version_id") val capturedGeometryVersionId: Long,
    @ColumnInfo(name = "commodity_name") val commodityName: String?,
    val density: Double?,
    val ownership: String,
    @ColumnInfo(name = "client_name") val clientName: String?,
    @ColumnInfo(name = "final_volume") val finalVolume: Double,
    @ColumnInfo(name = "final_mass") val finalMass: Double,
    @ColumnInfo(name = "engine_version") val engineVersion: String,
    @ColumnInfo(name = "metadata_json") val metadataJson: String
)
