package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.silomath.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE user_id = :userId LIMIT 1")
    suspend fun getById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email_normalized = :emailNormalized LIMIT 1")
    suspend fun getByEmailNormalized(emailNormalized: String): UserEntity?

    @Query("SELECT * FROM users")
    fun observeAll(): Flow<List<UserEntity>>

    @Upsert
    suspend fun upsert(user: UserEntity)
}
