package com.silomath.core.data.repository

import com.silomath.core.domain.model.Measurement
import com.silomath.core.domain.model.StocktakingRecord
import com.silomath.core.domain.model.StocktakingRevision
import com.silomath.core.domain.repository.StocktakingRepository
import com.silomath.core.model.CampaignId
import com.silomath.core.model.StocktakingRecordId
import com.silomath.core.model.StocktakingRevisionId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OfflineStocktakingRepository @Inject constructor() : StocktakingRepository {

    override fun observeRecords(campaignId: CampaignId): Flow<List<StocktakingRecord>> = unsupportedRead()
    override fun observeRecord(stocktakingRecordId: StocktakingRecordId): Flow<StocktakingRecord?> = unsupportedRead()
    override fun observeRevision(stocktakingRevisionId: StocktakingRevisionId): Flow<StocktakingRevision?> = unsupportedRead()
    override fun observeMeasurements(stocktakingRevisionId: StocktakingRevisionId): Flow<List<Measurement>> = unsupportedRead()

    override suspend fun saveWorkingRevision(
        revision: StocktakingRevision,
        measurements: List<Measurement>
    ): Result<StocktakingRevision> = unsupportedMutation()

    override suspend fun validateRevision(
        revision: StocktakingRevision,
        measurements: List<Measurement>
    ): Result<StocktakingRevision> = unsupportedMutation()

    override suspend fun markEmpty(
        revision: StocktakingRevision
    ): Result<StocktakingRevision> = unsupportedMutation()

    override suspend fun resetValidation(
        validatedRevision: StocktakingRevision,
        newWorkingRevision: StocktakingRevision
    ): Result<StocktakingRevision> = unsupportedMutation()

    private fun <T> unsupportedRead(): Flow<T> = flow {
        throw UnsupportedOperationException(
            "Canonical Stocktaking Room mapping belongs to DB010/DB011 and M7"
        )
    }

    private fun <T> unsupportedMutation(): Result<T> = Result.failure(
        UnsupportedOperationException(
            "Canonical local-first Stocktaking mutation persistence requires DB012 and M7 implementation"
        )
    )
}
