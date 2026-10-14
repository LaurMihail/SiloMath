package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transitional Campaign Scope Room entity.
 *
 * Migration stage: EXPAND.
 *
 * Legacy Long identities/FKs remain until dependent records and repositories
 * are migrated to canonical UUID identifiers.
 *
 * Final V6 target:
 * - campaign_scope_id UUID/TEXT primary key;
 * - campaign_id UUID/TEXT FK;
 * - storage_space_id UUID/TEXT FK;
 * - exact launch-time hierarchy context resolved through CampaignSnapshot;
 * - immutable after launch.
 */
@Entity(
    tableName = "campaign_scope",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaign_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = StorageSpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["storage_space_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = GeometryVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["geometry_version_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("campaign_id"),
        Index("storage_space_id"),
        Index("geometry_version_id"),
        Index(
            value = ["campaign_id", "storage_space_id"],
            unique = true
        ),
        Index(value = ["remoteId"], unique = true),

        Index(value = ["campaign_scope_id"], unique = true),
        Index("canonical_campaign_id"),
        Index("canonical_storage_space_id"),
        Index(
            value = [
                "canonical_campaign_id",
                "canonical_storage_space_id"
            ],
            unique = true
        )
    ]
)
data class CampaignScopeEntity(

    // -------------------------------------------------------------------------
    // LEGACY MIGRATION SHIM
    // -------------------------------------------------------------------------

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "campaign_id")
    val campaignId: Long,

    @ColumnInfo(name = "storage_space_id")
    val storageSpaceId: Long,

    /**
     * Legacy scope-level geometry reference.
     *
     * Nullable during migration because canonical V6 Scope does not own the
     * historical geometry identity. Launch-time reproducibility is preserved
     * through CampaignSnapshot / snapshotHierarchyRef.
     */
    @ColumnInfo(name = "geometry_version_id")
    val geometryVersionId: Long? = null,

    val remoteId: String? = null,

    // -------------------------------------------------------------------------
    // CANONICAL V6 EXPAND FIELDS
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "campaign_scope_id")
    val campaignScopeId: String? = null,

    @ColumnInfo(name = "canonical_campaign_id")
    val canonicalCampaignId: String? = null,

    @ColumnInfo(name = "canonical_storage_space_id")
    val canonicalStorageSpaceId: String? = null,

    /**
     * Reference to launch-time frozen hierarchy context.
     */
    @ColumnInfo(name = "snapshot_hierarchy_ref")
    val snapshotHierarchyRef: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long? = null,

    @ColumnInfo(name = "created_by")
    val createdBy: String? = null
)
