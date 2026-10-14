package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "commodities",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class CommodityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val code: String,
    val name: String,
    @ColumnInfo(name = "reference_density") val referenceDensity: Double? = null,
    @ColumnInfo(name = "density_unit") val densityUnit: String,
    @ColumnInfo(name = "min_density") val minDensity: Double? = null,
    @ColumnInfo(name = "max_density") val maxDensity: Double? = null,
    @ColumnInfo(name = "validation_parameters") val validationParameters: String? = null,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String = "ACTIVE",
    @ColumnInfo(name = "remote_revision") val remoteRevision: Long? = null
)
