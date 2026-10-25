package com.silomath.core.domain.repository

import com.silomath.core.domain.model.Measurement
import com.silomath.core.domain.model.StocktakingRecord
import com.silomath.core.domain.model.StocktakingRevision
import com.silomath.core.model.CampaignId
import com.silomath.core.model.StocktakingRecordId
import com.silomath.core.model.StocktakingRevisionId
import kotlinx.coroutines.flow.Flow

/**
 * Canonical local-first Stocktaking domain boundary.
 *
 * Persistence entities and legacy numeric identifiers are intentionally excluded.
 * Implementations must persist supported local mutations durably and participate in
 * the atomic business-mutation + SyncOperation transaction defined by DB012.
 */
interface StocktakingRepository {
    fun observeRecords(campaignId: CampaignId): Flow<List<StocktakingRecord>>

    fun observeRecord(stocktakingRecordId: StocktakingRecordId): Flow<StocktakingRecord?>

    fun observeRevision(stocktakingRevisionId: StocktakingRevisionId): Flow<StocktakingRevision?>

    fun observeMeasurements(stocktakingRevisionId: StocktakingRevisionId): Flow<List<Measurement>>

    suspend fun saveWorkingRevision(
        revision: StocktakingRevision,
        measurements: List<Measurement>
    ): Result<StocktakingRevision>

    suspend fun validateRevision(
        revision: StocktakingRevision,
        measurements: List<Measurement>
    ): Result<StocktakingRevision>

    suspend fun markEmpty(
        revision: StocktakingRevision
    ): Result<StocktakingRevision>

    suspend fun resetValidation(
        validatedRevision: StocktakingRevision,
        newWorkingRevision: StocktakingRevision
    ): Result<StocktakingRevision>
}
