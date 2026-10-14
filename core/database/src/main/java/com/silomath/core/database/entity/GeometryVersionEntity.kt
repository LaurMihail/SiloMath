package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "geometry_versions",
    foreignKeys = [
        ForeignKey(
            entity = GeometryProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["geometry_profile_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("geometry_profile_id"),
        Index(value = ["geometry_profile_id", "version_number"], unique = true)
    ]
)
data class GeometryVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "geometry_profile_id") val geometryProfileId: Long,
    val remoteId: String? = null,
    @ColumnInfo(name = "version_number") val versionNumber: Int,
    @ColumnInfo(name = "fixed_parameters") val fixedParameters: String,
    @ColumnInfo(name = "required_dimensions_schema") val requiredDimensionsSchema: String,
    @ColumnInfo(name = "measurement_schema") val measurementSchema: String,
    @ColumnInfo(name = "supported_units") val supportedUnits: String,
    @ColumnInfo(name = "calculation_method") val calculationMethod: String,
    @ColumnInfo(name = "validation_constraints") val validationConstraints: String,
    @ColumnInfo(name = "capacity_rules") val capacityRules: String? = null
)
