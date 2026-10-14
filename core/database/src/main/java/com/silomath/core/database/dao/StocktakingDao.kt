package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.CalculationResultEntity
import com.silomath.core.database.entity.IssueEntity
import com.silomath.core.database.entity.MeasurementEntity
import com.silomath.core.database.entity.StocktakingRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StocktakingDao {
    @Query("SELECT * FROM stocktaking_records WHERE campaign_id = :campaignId")
    fun getRecordsByCampaign(campaignId: Long): Flow<List<StocktakingRecordEntity>>

    @Query("SELECT * FROM stocktaking_records WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getRecordByRemoteId(remoteId: String): StocktakingRecordEntity?

    @Query("SELECT * FROM measurements WHERE stocktaking_record_id = :recordId")
    fun getMeasurementsByRecord(recordId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getMeasurementByRemoteId(remoteId: String): MeasurementEntity?

    @Query("SELECT * FROM calculation_results WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getCalculationResultByRemoteId(remoteId: String): CalculationResultEntity?

    @Query("SELECT * FROM issues WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getIssueByRemoteId(remoteId: String): IssueEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: StocktakingRecordEntity): Long

    @Update
    suspend fun updateRecord(record: StocktakingRecordEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeasurement(measurement: MeasurementEntity): Long

    @Update
    suspend fun updateMeasurement(measurement: MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCalculationResult(result: CalculationResultEntity): Long

    @Update
    suspend fun updateCalculationResult(result: CalculationResultEntity)

    @Query("SELECT * FROM calculation_results WHERE stocktaking_record_id = :recordId")
    suspend fun getCalculationResult(recordId: Long): CalculationResultEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIssue(issue: IssueEntity): Long

    @Update
    suspend fun updateIssue(issue: IssueEntity)

    @Query("SELECT * FROM issues WHERE stocktaking_record_id = :recordId")
    fun getIssuesByRecord(recordId: Long): Flow<List<IssueEntity>>

    @Query("DELETE FROM measurements WHERE stocktaking_record_id = :recordId")
    suspend fun clearMeasurements(recordId: Long)

    @Query("SELECT COUNT(*) FROM measurements WHERE stocktaking_record_id = :recordId")
    suspend fun getMeasurementCount(recordId: Long): Int

    @Transaction
    suspend fun addMeasurementTransactional(recordId: Long, value: Double) {
        val count = getMeasurementCount(recordId)
        if (count >= 5) {
            throw IllegalArgumentException("Maximum 5 measurements per record exceeded")
        }
        insertMeasurement(MeasurementEntity(stocktakingRecordId = recordId, value = value))
    }
}
