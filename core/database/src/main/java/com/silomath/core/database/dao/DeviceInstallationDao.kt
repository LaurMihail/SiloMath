package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.silomath.core.database.entity.DeviceInstallationEntity

@Dao
interface DeviceInstallationDao {

    @Query("SELECT * FROM device_installations WHERE device_installation_id = :id LIMIT 1")
    suspend fun getById(id: String): DeviceInstallationEntity?

    @Upsert
    suspend fun upsert(installation: DeviceInstallationEntity)

    @Query("UPDATE device_installations SET user_id = :userId WHERE device_installation_id = :id")
    suspend fun associateUser(id: String, userId: String?)

    @Query("UPDATE device_installations SET last_authorized_at = :timestamp WHERE device_installation_id = :id")
    suspend fun updateLastAuthorizedAt(id: String, timestamp: Long)

    @Query("UPDATE device_installations SET revoked_at = :timestamp WHERE device_installation_id = :id")
    suspend fun markRevoked(id: String, timestamp: Long)
}
