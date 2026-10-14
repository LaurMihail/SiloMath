package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transitional Campaign Room entity.
 *
 * Migration stage: EXPAND.
 *
 * Legacy integer identity and legacy remote fields are intentionally preserved
 * until all dependent tables have migrated to canonical UUID foreign keys.
 *
 * Final V6 target:
 * - campaign_id UUID/TEXT is the canonical primary key;
 * - organization_id is the canonical Organization UUID FK;
 * - legacy id/remoteId/remoteRevision are removed only during CONTRACT.
 */
@Entity(
    tableName = "campaigns",
    foreignKeys = [
        ForeignKey(
            entity = OrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["organization_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("organization_id"),
        Index(value = ["remoteId"], unique = true),

        // V6 EXPAND columns.
        Index(value = ["campaign_id"], unique = true),
        Index("canonical_organization_id"),
        Index(
            value = ["canonical_organization_id", "campaign_code"],
            unique = true
        )
    ]
)
data class CampaignEntity(

    // -------------------------------------------------------------------------
    // LEGACY MIGRATION SHIM
    // -------------------------------------------------------------------------

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "organization_id")
    val organizationId: Long,

    val name: String,

    /**
     * Legacy value currently used by existing code ("Active"/"Completed").
     * Do not use this as the final V6 domain status.
     */
    val status: String,

    val remoteId: String? = null,

    @ColumnInfo(name = "remote_revision")
    val remoteRevision: Long? = null,

    // -------------------------------------------------------------------------
    // CANONICAL V6 EXPAND FIELDS
    // -------------------------------------------------------------------------

    /**
     * Canonical technical Campaign UUID.
     * Final Room PK after dependent FK migration is complete.
     */
    @ColumnInfo(name = "campaign_id")
    val campaignId: String? = null,

    /**
     * Canonical Organization UUID.
     *
     * Temporary name avoids colliding with the legacy INTEGER
     * organization_id column during Expand/Migrate.
     *
     * CONTRACT will make the canonical UUID column organization_id.
     */
    @ColumnInfo(name = "canonical_organization_id")
    val canonicalOrganizationId: String? = null,

    /**
     * Official server-generated INV-YYYY-NNNN Campaign Code.
     */
    @ColumnInfo(name = "campaign_code")
    val campaignCode: String? = null,

    @ColumnInfo(name = "campaign_year")
    val campaignYear: Int? = null,

    /**
     * Server-side sequence component used for official code allocation.
     */
    @ColumnInfo(name = "campaign_sequence")
    val campaignSequence: Int? = null,

    /**
     * ANNUAL / MONTHLY / CONTROL.
     */
    @ColumnInfo(name = "inventory_type")
    val inventoryType: String? = null,

    /**
     * ISO YYYY-MM-DD.
     */
    @ColumnInfo(name = "start_date")
    val startDate: String? = null,

    /**
     * ISO YYYY-MM-DD.
     */
    @ColumnInfo(name = "planned_end_date")
    val plannedEndDate: String? = null,

    /**
     * Canonical V6 status: ACTIVE / COMPLETED.
     *
     * Kept separate during migration because existing code still uses
     * the legacy status column representation.
     */
    @ColumnInfo(name = "canonical_status")
    val canonicalStatus: String? = null,

    @ColumnInfo(name = "launched_at")
    val launchedAt: Long? = null,

    @ColumnInfo(name = "launched_by")
    val launchedBy: String? = null,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "completed_by")
    val completedBy: String? = null,

    @ColumnInfo(name = "campaign_snapshot_id")
    val campaignSnapshotId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long? = null,

    @ColumnInfo(name = "created_by")
    val createdBy: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null,

    @ColumnInfo(name = "updated_by")
    val updatedBy: String? = null,

    @ColumnInfo(name = "entity_version")
    val entityVersion: Long? = null
)
