package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transitional Campaign Team Assignment Room entity.
 *
 * Migration stage: EXPAND.
 *
 * Legacy Long Campaign FK and uniqueness behavior remain temporarily so the
 * existing application continues to operate while canonical UUID-based
 * assignment data is introduced.
 *
 * Final V6 target:
 * - assignment_id UUID/TEXT primary key;
 * - campaign_id UUID/TEXT FK;
 * - OrganizationMembership reference;
 * - ACTIVE / REMOVED lifecycle with preserved history;
 * - canonical synchronization metadata.
 */
@Entity(
    tableName = "campaign_assignments",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaign_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("campaign_id"),
        Index(
            value = ["campaign_id", "user_id"],
            unique = true
        ),
        Index(value = ["remoteId"], unique = true),

        Index(value = ["assignment_id"], unique = true),
        Index("canonical_campaign_id"),
        Index("organization_membership_id"),
        Index("assignment_status")
    ]
)
data class CampaignAssignmentEntity(

    // -------------------------------------------------------------------------
    // LEGACY MIGRATION SHIM
    // -------------------------------------------------------------------------

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "campaign_id")
    val campaignId: Long,

    @ColumnInfo(name = "user_id")
    val userId: String,

    /**
     * Legacy role snapshot.
     *
     * Nullable during migration because canonical V6 authorization is based on
     * OrganizationMembership and Campaign Team assignment lifecycle.
     */
    @ColumnInfo(name = "role_at_assignment")
    val roleAtAssignment: String? = null,

    val remoteId: String? = null,

    // -------------------------------------------------------------------------
    // CANONICAL V6 EXPAND FIELDS
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "assignment_id")
    val assignmentId: String? = null,

    @ColumnInfo(name = "canonical_campaign_id")
    val canonicalCampaignId: String? = null,

    @ColumnInfo(name = "organization_membership_id")
    val organizationMembershipId: String? = null,

    /**
     * Canonical values: ACTIVE / REMOVED.
     */
    @ColumnInfo(name = "assignment_status")
    val assignmentStatus: String? = null,

    @ColumnInfo(name = "assigned_at")
    val assignedAt: Long? = null,

    @ColumnInfo(name = "assigned_by")
    val assignedBy: String? = null,

    @ColumnInfo(name = "removed_at")
    val removedAt: Long? = null,

    @ColumnInfo(name = "removed_by")
    val removedBy: String? = null,

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
