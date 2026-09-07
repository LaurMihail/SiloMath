package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.CommodityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommodityDao {
    @Query("SELECT * FROM commodities")
    fun getAllCommodities(): Flow<List<CommodityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommodity(commodity: CommodityEntity): Long
}
