package com.silomath.core.domain.repository

import com.silomath.core.database.entity.CommodityEntity
import kotlinx.coroutines.flow.Flow

interface CommodityRepository {
    fun getCommodities(): Flow<List<CommodityEntity>>
    suspend fun saveCommodity(commodity: CommodityEntity)
}
