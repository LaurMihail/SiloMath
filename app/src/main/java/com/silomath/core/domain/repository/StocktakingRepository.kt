package com.silomath.core.domain.repository

import com.silomath.core.database.entity.MeasurementEntity
import com.silomath.core.database.entity.StocktakingRecordEntity
import kotlinx.coroutines.flow.Flow

interface StocktakingRepository {
    fun getRecords(campaignId: Long): Flow<List<StocktakingRecordEntity>>
    fun getMeasurements(recordId: Long): Flow<List<MeasurementEntity>>
    suspend fun addMeasurement(recordId: Long, value: Double) // Enforce 5-measurement rule
    suspend fun updateRecord(record: StocktakingRecordEntity)
    suspend fun submitRecord(recordId: Long)
}
