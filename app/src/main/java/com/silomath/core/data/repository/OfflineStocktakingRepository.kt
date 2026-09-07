package com.silomath.core.data.repository

import com.silomath.core.database.dao.StocktakingDao
import com.silomath.core.database.entity.MeasurementEntity
import com.silomath.core.database.entity.StocktakingRecordEntity
import com.silomath.core.domain.repository.StocktakingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class OfflineStocktakingRepository @Inject constructor(
    private val stocktakingDao: StocktakingDao
) : StocktakingRepository {
    override fun getRecords(campaignId: Long): Flow<List<StocktakingRecordEntity>> = 
        stocktakingDao.getRecordsByCampaign(campaignId)

    override fun getMeasurements(recordId: Long): Flow<List<MeasurementEntity>> = 
        stocktakingDao.getMeasurementsByRecord(recordId)

    override suspend fun addMeasurement(recordId: Long, value: Double) {
        stocktakingDao.addMeasurementTransactional(recordId, value)
    }

    override suspend fun updateRecord(record: StocktakingRecordEntity) {
        require(record.ownership in listOf("Owned", "Third-Party")) { "Invalid ownership" }
        if (record.ownership == "Third-Party") {
            require(record.clientId != null) { "Client mandatory for Third-Party ownership" }
        }
        require(record.status in listOf("To Do", "Issues", "Validated")) { "Invalid status" }
        stocktakingDao.updateRecord(record)
    }

    override suspend fun submitRecord(recordId: Long) {
        // Logic to transition record status
    }
}
