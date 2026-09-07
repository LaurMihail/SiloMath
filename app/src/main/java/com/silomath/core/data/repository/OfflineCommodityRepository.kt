package com.silomath.core.data.repository

import com.silomath.core.database.dao.CommodityDao
import com.silomath.core.database.entity.CommodityEntity
import com.silomath.core.domain.repository.CommodityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineCommodityRepository @Inject constructor(
    private val commodityDao: CommodityDao
) : CommodityRepository {
    override fun getCommodities(): Flow<List<CommodityEntity>> = commodityDao.getAllCommodities()

    override suspend fun saveCommodity(commodity: CommodityEntity) {
        commodityDao.insertCommodity(commodity)
    }
}
