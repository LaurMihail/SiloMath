package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE organization_id = :organizationId")
    fun getClientsByOrganization(organizationId: Long): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE organization_id = :organizationId")
    suspend fun getClientsByOrganizationSync(organizationId: Long): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE client_id = :clientId LIMIT 1")
    suspend fun getClientByClientId(clientId: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getClientByRemoteId(remoteId: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)
}
