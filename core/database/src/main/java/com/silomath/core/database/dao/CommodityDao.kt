package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.CommodityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommodityDao {
    @Query("SELECT * FROM commodities")
    fun getAllCommodities(): Flow<List<CommodityEntity>>

    @Query("SELECT * FROM commodities WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getCommodityByRemoteId(remoteId: String): CommodityEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCommodity(commodity: CommodityEntity): Long

    @Update
    suspend fun updateCommodity(commodity: CommodityEntity)
}
