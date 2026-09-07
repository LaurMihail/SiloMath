package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commodities")
data class CommodityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "hectoliter_weight") val hectoliterWeight: Double,
    @ColumnInfo(name = "moisture_ref") val moistureRef: Double,
    @ColumnInfo(name = "impurities_ref") val impuritiesRef: Double
)
