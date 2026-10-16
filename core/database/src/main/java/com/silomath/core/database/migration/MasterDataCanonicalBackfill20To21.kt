package com.silomath.core.database.migration

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * SiloMath V6 Master Data persistence migration.
 *
 * Stage: MIGRATE.
 *
 * Backfills canonical UUID identities/references only when an authoritative
 * legacy/remote identity already exists. No synthetic UUIDs, business codes,
 * descriptions, audit metadata or timestamps are invented.
 */
class MasterDataCanonicalBackfill20To21 : AutoMigrationSpec {

    override fun onPostMigrate(db: SupportSQLiteDatabase) {

        // GeometryProfile identity bridge required by StorageSpace.
        db.execSQL(
            """
            UPDATE geometry_profiles
            SET geometry_profile_id = lower(remoteId)
            WHERE geometry_profile_id IS NULL
              AND remoteId IS NOT NULL
              AND length(remoteId) = 36
              AND remoteId GLOB
                  '[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'
            """.trimIndent()
        )

        // StorageUnit.
        db.execSQL(
            """
            UPDATE storage_units
            SET storage_unit_id = lower(remoteId)
            WHERE storage_unit_id IS NULL
              AND remoteId IS NOT NULL
              AND length(remoteId) = 36
              AND remoteId GLOB
                  '[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE storage_units
            SET canonical_location_id = (
                SELECT l.location_id
                FROM locations l
                WHERE l.id = storage_units.location_id
                LIMIT 1
            )
            WHERE canonical_location_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM locations l
                  WHERE l.id = storage_units.location_id
                    AND l.location_id IS NOT NULL
              )
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE storage_units
            SET canonical_organization_id = (
                SELECT l.canonical_organization_id
                FROM locations l
                WHERE l.id = storage_units.location_id
                LIMIT 1
            )
            WHERE canonical_organization_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM locations l
                  WHERE l.id = storage_units.location_id
                    AND l.canonical_organization_id IS NOT NULL
              )
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE storage_units
            SET entity_version = remoteRevision
            WHERE entity_version IS NULL
              AND remoteRevision IS NOT NULL
              AND remoteRevision >= 1
            """.trimIndent()
        )

        // StorageSpace.
        db.execSQL(
            """
            UPDATE storage_spaces
            SET storage_space_id = lower(remoteId)
            WHERE storage_space_id IS NULL
              AND remoteId IS NOT NULL
              AND length(remoteId) = 36
              AND remoteId GLOB
                  '[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE storage_spaces
            SET canonical_storage_unit_id = (
                SELECT su.storage_unit_id
                FROM storage_units su
                WHERE su.id = storage_spaces.storage_unit_id
                LIMIT 1
            )
            WHERE canonical_storage_unit_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM storage_units su
                  WHERE su.id = storage_spaces.storage_unit_id
                    AND su.storage_unit_id IS NOT NULL
              )
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE storage_spaces
            SET canonical_organization_id = (
                SELECT su.canonical_organization_id
                FROM storage_units su
                WHERE su.id = storage_spaces.storage_unit_id
                LIMIT 1
            )
            WHERE canonical_organization_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM storage_units su
                  WHERE su.id = storage_spaces.storage_unit_id
                    AND su.canonical_organization_id IS NOT NULL
              )
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE storage_spaces
            SET canonical_geometry_profile_id = (
                SELECT gp.geometry_profile_id
                FROM geometry_profiles gp
                WHERE gp.id = storage_spaces.geometry_profile_id
                LIMIT 1
            )
            WHERE canonical_geometry_profile_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM geometry_profiles gp
                  WHERE gp.id = storage_spaces.geometry_profile_id
                    AND gp.geometry_profile_id IS NOT NULL
              )
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE storage_spaces
            SET entity_version = remoteRevision
            WHERE entity_version IS NULL
              AND remoteRevision IS NOT NULL
              AND remoteRevision >= 1
            """.trimIndent()
        )

        // Client.
        db.execSQL(
            """
            UPDATE clients
            SET client_id = lower(remoteId)
            WHERE client_id IS NULL
              AND remoteId IS NOT NULL
              AND length(remoteId) = 36
              AND remoteId GLOB
                  '[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]-[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE clients
            SET canonical_organization_id = (
                SELECT o.organization_id
                FROM organizations o
                WHERE o.id = clients.organization_id
                LIMIT 1
            )
            WHERE canonical_organization_id IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM organizations o
                  WHERE o.id = clients.organization_id
                    AND o.organization_id IS NOT NULL
              )
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE clients
            SET entity_version = remote_revision
            WHERE entity_version IS NULL
              AND remote_revision IS NOT NULL
              AND remote_revision >= 1
            """.trimIndent()
        )

        // Audit metadata and missing business fields deliberately remain NULL
        // when the legacy store has no authoritative value.
    }
}
