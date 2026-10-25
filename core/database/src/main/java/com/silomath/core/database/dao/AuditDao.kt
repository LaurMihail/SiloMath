package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.silomath.core.database.entity.AuditTrailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_trail ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<AuditTrailEntity>>

    // Local audit projection is append-only as well: duplicate primary keys fail
    // closed instead of replacing historical rows. Canonical AuditEvent remains
    // server-authoritative per the frozen V6 persistence contract.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: AuditTrailEntity): Long
}
