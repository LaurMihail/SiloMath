package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "issues",
    foreignKeys = [
        ForeignKey(
            entity = StocktakingRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["stocktaking_record_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("stocktaking_record_id")]
)
data class IssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stocktaking_record_id") val stocktakingRecordId: Long,
    val description: String,
    val reportedAt: Long = System.currentTimeMillis()
)
