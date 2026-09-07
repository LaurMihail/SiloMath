package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.GeometryFamilyEntity
import com.silomath.core.database.entity.GeometryProfileEntity
import com.silomath.core.database.entity.GeometryVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeometryDao {
    @Query("SELECT * FROM geometry_families")
    fun getAllFamilies(): Flow<List<GeometryFamilyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamily(family: GeometryFamilyEntity): Long

    @Query("SELECT * FROM geometry_profiles")
    fun getAllProfiles(): Flow<List<GeometryProfileEntity>>

    @Query("SELECT * FROM geometry_versions WHERE geometry_profile_id = :profileId ORDER BY version_number DESC")
    fun getVersionsByProfile(profileId: Long): Flow<List<GeometryVersionEntity>>

    @Query("SELECT MAX(version_number) FROM geometry_versions WHERE geometry_profile_id = :profileId")
    suspend fun getMaxVersionNumber(profileId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GeometryProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVersion(version: GeometryVersionEntity): Long
}
