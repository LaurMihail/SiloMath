package com.silomath.core.database.migration

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * SiloMath V6 Campaign persistence migration.
 *
 * Stage: MIGRATE.
 *
 * Schema columns were introduced during versions 16 and 17.
 * This migration backfills canonical UUID-based references where the
 * corresponding authoritative legacy/remote identity already exists.
 *
 * No synthetic business identifiers, timestamps, Campaign Codes,
 * membership identities, snapshot references or audit metadata are invented.
 */
class CampaignCanonicalBackfill17To18 : AutoMigrationSpec {

    override fun onPostMigrate(db: SupportSQLiteDatabase) {

        // ---------------------------------------------------------------------
        // InventoryCampaign
        // ---------------------------------------------------------------------

        db.execSQL(
            """
            UPDATE campaigns
            SET campaign_id = lower(remoteId)
            WHERE campaign_id IS NULL
              AND remoteId IS NOT NULL
              AND length(remoteId) = 36
              AND remoteId GLOB
                  '[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE campaigns
            SET canonical_organization_id = (
                SELECT o.organization_id
                FROM organizations o
                WHERE o.id = campaigns.organization_id
                LIMIT 1
            )
            WHERE canonical_organization_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM organizations o
                  WHERE o.id = campaigns.organization_id
                    AND o.organization_id IS NOT NULL
              )
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE campaigns
            SET canonical_status =
                CASE upper(trim(status))
                    WHEN 'ACTIVE' THEN 'ACTIVE'
                    WHEN 'COMPLETED' THEN 'COMPLETED'
                    ELSE canonical_status
                END
            WHERE canonical_status IS NULL
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE campaigns
            SET entity_version = remote_revision
            WHERE entity_version IS NULL
              AND remote_revision IS NOT NULL
              AND remote_revision >= 1
            """.trimIndent()
        )

        // ---------------------------------------------------------------------
        // CampaignScope
        // ---------------------------------------------------------------------

        db.execSQL(
            """
            UPDATE campaign_scope
            SET campaign_scope_id = lower(remoteId)
            WHERE campaign_scope_id IS NULL
              AND remoteId IS NOT NULL
              AND length(remoteId) = 36
              AND remoteId GLOB
                  '[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE campaign_scope
            SET canonical_campaign_id = (
                SELECT c.campaign_id
                FROM campaigns c
                WHERE c.id = campaign_scope.campaign_id
                LIMIT 1
            )
            WHERE canonical_campaign_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM campaigns c
                  WHERE c.id = campaign_scope.campaign_id
                    AND c.campaign_id IS NOT NULL
              )
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE campaign_scope
            SET canonical_storage_space_id = (
                SELECT lower(s.remoteId)
                FROM storage_spaces s
                WHERE s.id = campaign_scope.storage_space_id
                  AND s.remoteId IS NOT NULL
                  AND length(s.remoteId) = 36
                LIMIT 1
            )
            WHERE canonical_storage_space_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM storage_spaces s
                  WHERE s.id = campaign_scope.storage_space_id
                    AND s.remoteId IS NOT NULL
                    AND length(s.remoteId) = 36
              )
            """.trimIndent()
        )

        // snapshot_hierarchy_ref is deliberately NOT synthesized.
        // It must come from the authoritative CampaignSnapshot migration.

        // ---------------------------------------------------------------------
        // CampaignTeamAssignment
        // ---------------------------------------------------------------------

        db.execSQL(
            """
            UPDATE campaign_assignments
            SET assignment_id = lower(remoteId)
            WHERE assignment_id IS NULL
              AND remoteId IS NOT NULL
              AND length(remoteId) = 36
              AND remoteId GLOB
                  '[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE campaign_assignments
            SET canonical_campaign_id = (
                SELECT c.campaign_id
                FROM campaigns c
                WHERE c.id = campaign_assignments.campaign_id
                LIMIT 1
            )
            WHERE canonical_campaign_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM campaigns c
                  WHERE c.id = campaign_assignments.campaign_id
                    AND c.campaign_id IS NOT NULL
              )
            """.trimIndent()
        )

        /*
         * Legacy campaign_assignments represented current team membership only.
         * Therefore an existing legacy row maps to the current ACTIVE projection.
         * No REMOVED history is invented because the legacy model did not retain it.
         */
        db.execSQL(
            """
            UPDATE campaign_assignments
            SET assignment_status = 'ACTIVE'
            WHERE assignment_status IS NULL
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE campaign_assignments
            SET organization_membership_id = (
                SELECT om.membership_id
                FROM organization_memberships om
                JOIN campaigns c
                  ON c.id = campaign_assignments.campaign_id
                WHERE om.legacy_organization_id = c.organization_id
                  AND om.user_id = campaign_assignments.user_id
                  AND om.membership_id IS NOT NULL
                LIMIT 1
            )
            WHERE organization_membership_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM organization_memberships om
                  JOIN campaigns c
                    ON c.id = campaign_assignments.campaign_id
                  WHERE om.legacy_organization_id = c.organization_id
                    AND om.user_id = campaign_assignments.user_id
                    AND om.membership_id IS NOT NULL
              )
            """.trimIndent()
        )

        /*
         * assigned_at / assigned_by / created metadata / entity_version are
         * deliberately left NULL when the legacy store has no authoritative
         * value. They must be refreshed/backfilled from authoritative server data.
         */
    }
}
