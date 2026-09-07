package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calculation_results",
    foreignKeys = [
        ForeignKey(
            entity = StocktakingRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["stocktaking_record_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["stocktaking_record_id"], unique = true)
    ]
)
data class CalculationResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stocktaking_record_id") val stocktakingRecordId: Long,
    val volume: Double,
    val mass: Double,
    @ColumnInfo(name = "engine_version") val engineVersion: String,
    @ColumnInfo(name = "metadata_json") val metadataJson: String
)
