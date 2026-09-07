package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = StocktakingRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["stocktaking_record_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stocktaking_record_id")]
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stocktaking_record_id") val stocktakingRecordId: Long,
    val value: Double,
    val timestamp: Long = System.currentTimeMillis()
)
