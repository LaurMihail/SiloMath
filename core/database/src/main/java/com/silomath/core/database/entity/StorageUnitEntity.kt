package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "storage_units",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("location_id")]
)
data class StorageUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "location_id") val locationId: Long,
    val remoteId: String? = null,
    val code: String,
    val name: String? = null,
    val type: String = "SILO",
    val lifecycleStatus: String = "ACTIVE",
    val remoteRevision: Long? = null
)
