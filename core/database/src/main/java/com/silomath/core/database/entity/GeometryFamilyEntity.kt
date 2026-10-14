package com.silomath.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geometry_families")
data class GeometryFamilyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val name: String,
    val lifecycleStatus: String = "ACTIVE",
    val remoteRevision: Long? = null
)
