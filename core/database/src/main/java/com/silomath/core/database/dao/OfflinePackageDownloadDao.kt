package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.silomath.core.database.entity.OfflinePackageDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflinePackageDownloadDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(download: OfflinePackageDownloadEntity)

    @Update
    suspend fun update(download: OfflinePackageDownloadEntity)

    @Query("SELECT * FROM offline_package_downloads WHERE manifest_id = :manifestId LIMIT 1")
    suspend fun get(manifestId: String): OfflinePackageDownloadEntity?

    @Query("SELECT * FROM offline_package_downloads WHERE authorized_user_id = :userId ORDER BY updated_at DESC")
    fun observeForUser(userId: String): Flow<List<OfflinePackageDownloadEntity>>

    @Query("SELECT * FROM offline_package_downloads WHERE state IN ('DOWNLOAD_PENDING','DOWNLOADING','VERIFYING') AND authorized_user_id = :userId")
    suspend fun recoverableForUser(userId: String): List<OfflinePackageDownloadEntity>
}
