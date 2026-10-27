package com.silomath.core.database.di

import android.content.Context
import com.silomath.core.database.offline.CanonicalOfflineGuards
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE organizations ADD COLUMN lifecycle_status TEXT NOT NULL DEFAULT 'ACTIVE'")
        db.execSQL("ALTER TABLE organizations ADD COLUMN remoteRevision INTEGER")
        db.execSQL("ALTER TABLE locations ADD COLUMN lifecycle_status TEXT NOT NULL DEFAULT 'ACTIVE'")
        db.execSQL("ALTER TABLE locations ADD COLUMN remoteRevision INTEGER")
        db.execSQL("ALTER TABLE storage_units ADD COLUMN lifecycle_status TEXT NOT NULL DEFAULT 'ACTIVE'")
        db.execSQL("ALTER TABLE storage_units ADD COLUMN remoteRevision INTEGER")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `storage_spaces` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `storage_unit_id` INTEGER NOT NULL,
                `code` TEXT NOT NULL,
                `geometry_profile_id` INTEGER NOT NULL,
                `geometry_version_id` INTEGER NOT NULL,
                `configured_dimensions` TEXT NOT NULL,
                `remoteId` TEXT,
                `lifecycle_status` TEXT NOT NULL DEFAULT 'ACTIVE',
                `remoteRevision` INTEGER,
                FOREIGN KEY(`storage_unit_id`) REFERENCES `storage_units`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`geometry_profile_id`) REFERENCES `geometry_profiles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`geometry_version_id`) REFERENCES `geometry_versions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_storage_spaces_storage_unit_id_code` ON `storage_spaces` (`storage_unit_id`, `code`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_storage_spaces_geometry_profile_id` ON `storage_spaces` (`geometry_profile_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_storage_spaces_geometry_version_id` ON `storage_spaces` (`geometry_version_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_storage_spaces_remoteId` ON `storage_spaces` (`remoteId`)")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `commodities` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `code` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `reference_density` REAL,
                `density_unit` TEXT NOT NULL,
                `min_density` REAL,
                `max_density` REAL,
                `validation_parameters` TEXT,
                `remoteId` TEXT,
                `lifecycle_status` TEXT NOT NULL DEFAULT 'ACTIVE',
                `remoteRevision` INTEGER
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_commodities_code` ON `commodities` (`code`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_commodities_remoteId` ON `commodities` (`remoteId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `clients` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `organization_id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `remoteId` TEXT,
                `lifecycle_status` TEXT NOT NULL DEFAULT 'ACTIVE',
                `remoteRevision` INTEGER,
                FOREIGN KEY(`organization_id`) REFERENCES `organizations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_clients_organization_id` ON `clients` (`organization_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_clients_remoteId` ON `clients` (`remoteId`)")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `campaigns` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `organization_id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `remoteId` TEXT,
                `remoteRevision` INTEGER,
                FOREIGN KEY(`organization_id`) REFERENCES `organizations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_campaigns_organization_id` ON `campaigns` (`organization_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_campaigns_remoteId` ON `campaigns` (`remoteId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `campaign_scope` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `campaign_id` INTEGER NOT NULL,
                `storage_space_id` INTEGER NOT NULL,
                `geometry_version_id` INTEGER NOT NULL,
                `remoteId` TEXT,
                FOREIGN KEY(`campaign_id`) REFERENCES `campaigns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`storage_space_id`) REFERENCES `storage_spaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`geometry_version_id`) REFERENCES `geometry_versions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_campaign_scope_campaign_id_storage_space_id` ON `campaign_scope` (`campaign_id`, `storage_space_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_campaign_scope_storage_space_id` ON `campaign_scope` (`storage_space_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_campaign_scope_geometry_version_id` ON `campaign_scope` (`geometry_version_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_campaign_scope_remoteId` ON `campaign_scope` (`remoteId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `campaign_assignments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `campaign_id` INTEGER NOT NULL,
                `user_id` TEXT NOT NULL,
                `role_at_assignment` TEXT NOT NULL,
                `remoteId` TEXT,
                FOREIGN KEY(`campaign_id`) REFERENCES `campaigns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_campaign_assignments_campaign_id` ON `campaign_assignments` (`campaign_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_campaign_assignments_remoteId` ON `campaign_assignments` (`remoteId`)")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stocktaking_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `campaign_id` INTEGER NOT NULL,
                `storage_space_id` INTEGER NOT NULL,
                `commodity_id` INTEGER,
                `density` REAL,
                `ownership` TEXT NOT NULL,
                `client_id` INTEGER,
                `status` TEXT NOT NULL,
                `remoteId` TEXT,
                `remoteRevision` INTEGER,
                FOREIGN KEY(`campaign_id`) REFERENCES `campaigns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`storage_space_id`) REFERENCES `storage_spaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`commodity_id`) REFERENCES `commodities`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`client_id`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stocktaking_records_campaign_id_storage_space_id` ON `stocktaking_records` (`campaign_id`, `storage_space_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stocktaking_records_storage_space_id` ON `stocktaking_records` (`storage_space_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stocktaking_records_commodity_id` ON `stocktaking_records` (`commodity_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stocktaking_records_client_id` ON `stocktaking_records` (`client_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stocktaking_records_remoteId` ON `stocktaking_records` (`remoteId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `measurements` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `stocktaking_record_id` INTEGER NOT NULL,
                `value` REAL NOT NULL,
                `measured_at` INTEGER NOT NULL,
                `remoteId` TEXT,
                FOREIGN KEY(`stocktaking_record_id`) REFERENCES `stocktaking_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_measurements_stocktaking_record_id` ON `measurements` (`stocktaking_record_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_measurements_remoteId` ON `measurements` (`remoteId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `calculation_results` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `stocktaking_record_id` INTEGER NOT NULL,
                `volume` REAL NOT NULL,
                `mass` REAL NOT NULL,
                `engine_version` TEXT NOT NULL,
                `metadata_json` TEXT NOT NULL,
                `remoteId` TEXT,
                FOREIGN KEY(`stocktaking_record_id`) REFERENCES `stocktaking_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calculation_results_stocktaking_record_id` ON `calculation_results` (`stocktaking_record_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_calculation_results_remoteId` ON `calculation_results` (`remoteId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `issues` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `stocktaking_record_id` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `reported_at` INTEGER NOT NULL,
                FOREIGN KEY(`stocktaking_record_id`) REFERENCES `stocktaking_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_issues_stocktaking_record_id` ON `issues` (`stocktaking_record_id`)")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE issues ADD COLUMN remoteId TEXT")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_issues_remoteId` ON `issues` (`remoteId`)")
    }
}

internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursor = db.query(
            "SELECT COUNT(*) FROM inventory_snapshot_items " +
            "WHERE captured_geometry_version_id NOT IN (SELECT id FROM geometry_versions)"
        )
        if (cursor.moveToFirst()) {
            val orphanCount = cursor.getInt(0)
            cursor.close()
            if (orphanCount > 0) {
                throw IllegalStateException("Migration 7->8 failed: inventory_snapshot_items contains $orphanCount orphaned captured_geometry_version_id values that cannot satisfy Foreign Key constraint")
            }
        } else {
            cursor.close()
        }

        db.execSQL("ALTER TABLE inventory_snapshots ADD COLUMN remoteId TEXT")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_inventory_snapshots_remoteId` ON `inventory_snapshots` (`remoteId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `inventory_snapshot_items_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `snapshot_id` INTEGER NOT NULL,
                `storage_space_code` TEXT NOT NULL,
                `captured_geometry_version_id` INTEGER NOT NULL,
                `commodity_name` TEXT,
                `density` REAL,
                `ownership` TEXT NOT NULL,
                `client_name` TEXT,
                `final_volume` REAL NOT NULL,
                `final_mass` REAL NOT NULL,
                `engine_version` TEXT NOT NULL,
                `metadata_json` TEXT NOT NULL,
                `remoteId` TEXT,
                FOREIGN KEY(`snapshot_id`) REFERENCES `inventory_snapshots`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`captured_geometry_version_id`) REFERENCES `geometry_versions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `inventory_snapshot_items_new` (
                `id`, `snapshot_id`, `storage_space_code`, `captured_geometry_version_id`,
                `commodity_name`, `density`, `ownership`, `client_name`,
                `final_volume`, `final_mass`, `engine_version`, `metadata_json`, `remoteId`
            )
            SELECT 
                `id`,
                `snapshot_id`,
                `storage_space_code`,
                `captured_geometry_version_id`,
                `commodity_name`,
                `density`,
                `ownership`,
                `client_name`,
                `final_volume`,
                `final_mass`,
                `engine_version`,
                `metadata_json`,
                NULL
            FROM `inventory_snapshot_items`
        """)

        db.execSQL("DROP TABLE `inventory_snapshot_items`")
        db.execSQL("ALTER TABLE `inventory_snapshot_items_new` RENAME TO `inventory_snapshot_items`")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_snapshot_items_snapshot_id` ON `inventory_snapshot_items` (`snapshot_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_snapshot_items_captured_geometry_version_id` ON `inventory_snapshot_items` (`captured_geometry_version_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_inventory_snapshot_items_remoteId` ON `inventory_snapshot_items` (`remoteId`)")
    }
}

internal val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `organization_members` (
                `organization_id` INTEGER NOT NULL,
                `user_id` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                PRIMARY KEY(`organization_id`, `user_id`),
                FOREIGN KEY(`organization_id`) REFERENCES `organizations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_organization_members_organization_id` ON `organization_members` (`organization_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_organization_members_user_id` ON `organization_members` (`user_id`)")
    }
}

internal val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sync_operations` (
                `sync_operation_id` TEXT NOT NULL,
                `idempotency_key` TEXT NOT NULL,
                `organization_id` TEXT NOT NULL,
                `campaign_id` TEXT,
                `entity_type` TEXT NOT NULL,
                `entity_id` TEXT NOT NULL,
                `operation_type` TEXT NOT NULL,
                `base_entity_version` INTEGER NOT NULL,
                `event_at` INTEGER NOT NULL,
                `payload_json` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `attempt_count` INTEGER NOT NULL,
                `retry_metadata` TEXT,
                `last_error` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`sync_operation_id`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `device_installations` (
                `installation_uuid` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`installation_uuid`)
            )
        """.trimIndent())
    }
}

internal val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `users` (
                `user_id` TEXT NOT NULL,
                `full_name` TEXT NOT NULL,
                `email` TEXT NOT NULL,
                `email_normalized` TEXT NOT NULL,
                `lifecycle_status` TEXT NOT NULL DEFAULT 'ACTIVE',
                `created_at` INTEGER NOT NULL,
                `created_by` TEXT,
                `updated_at` INTEGER NOT NULL,
                `updated_by` TEXT NOT NULL,
                `entity_version` INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(`user_id`)
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email_normalized` ON `users` (`email_normalized`)")
    }
}

internal val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `device_installations_new` (
                `device_installation_id` TEXT NOT NULL,
                `user_id` TEXT,
                `created_at` INTEGER NOT NULL,
                `last_authorized_at` INTEGER,
                `revoked_at` INTEGER,
                `updated_at` INTEGER NOT NULL,
                `entity_version` INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(`device_installation_id`),
                FOREIGN KEY(`user_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `device_installations_new` (
                `device_installation_id`, `created_at`, `updated_at`, `entity_version`
            )
            SELECT 
                installation_uuid,
                created_at,
                created_at,
                1
            FROM `device_installations`
        """)

        db.execSQL("DROP TABLE IF EXISTS `device_installations`")
        db.execSQL("ALTER TABLE `device_installations_new` RENAME TO `device_installations`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_device_installations_user_id` ON `device_installations` (`user_id`)")
    }
}

internal fun createOrganizationTriggers(db: SupportSQLiteDatabase) {
    db.execSQL("""
        CREATE TRIGGER IF NOT EXISTS `check_org_constraints_insert`
        BEFORE INSERT ON `organizations`
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.organization_id IS NOT NULL AND (length(NEW.organization_id) != 36 OR NOT (NEW.organization_id GLOB '[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]'))
                THEN RAISE(ABORT, 'Invalid canonical organization_id UUID')
                WHEN NEW.name != trim(NEW.name) OR length(NEW.name) NOT BETWEEN 1 AND 150
                THEN RAISE(ABORT, 'Invalid organization name')
                WHEN NEW.code IS NOT NULL AND (length(NEW.code) > 50 OR NEW.code != upper(trim(NEW.code)) OR length(trim(NEW.code)) < 1)
                THEN RAISE(ABORT, 'Invalid organization code')
                WHEN NEW.address IS NOT NULL AND length(NEW.address) > 250
                THEN RAISE(ABORT, 'Invalid organization address')
                WHEN NEW.city IS NOT NULL AND length(NEW.city) > 100
                THEN RAISE(ABORT, 'Invalid organization city')
                WHEN NEW.lifecycle_status NOT IN ('ACTIVE', 'INACTIVE')
                THEN RAISE(ABORT, 'Invalid organization lifecycle_status')
                WHEN NEW.entity_version IS NOT NULL AND NEW.entity_version < 1
                THEN RAISE(ABORT, 'Invalid organization entity_version')
            END;
        END;
    """.trimIndent())

    db.execSQL("""
        CREATE TRIGGER IF NOT EXISTS `check_org_constraints_update`
        BEFORE UPDATE ON `organizations`
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.organization_id IS NOT NULL AND (length(NEW.organization_id) != 36 OR NOT (NEW.organization_id GLOB '[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]'))
                THEN RAISE(ABORT, 'Invalid canonical organization_id UUID')
                WHEN NEW.name != trim(NEW.name) OR length(NEW.name) NOT BETWEEN 1 AND 150
                THEN RAISE(ABORT, 'Invalid organization name')
                WHEN NEW.code IS NOT NULL AND (length(NEW.code) > 50 OR NEW.code != upper(trim(NEW.code)) OR length(trim(NEW.code)) < 1)
                THEN RAISE(ABORT, 'Invalid organization code')
                WHEN NEW.address IS NOT NULL AND length(NEW.address) > 250
                THEN RAISE(ABORT, 'Invalid organization address')
                WHEN NEW.city IS NOT NULL AND length(NEW.city) > 100
                THEN RAISE(ABORT, 'Invalid organization city')
                WHEN NEW.lifecycle_status NOT IN ('ACTIVE', 'INACTIVE')
                THEN RAISE(ABORT, 'Invalid organization lifecycle_status')
                WHEN NEW.entity_version IS NOT NULL AND NEW.entity_version < 1
                THEN RAISE(ABORT, 'Invalid organization entity_version')
            END;
        END;
    """.trimIndent())
}

internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `organizations_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `organization_id` TEXT,
                `name` TEXT NOT NULL,
                `code` TEXT,
                `address` TEXT,
                `city` TEXT,
                `country` TEXT,
                `lifecycle_status` TEXT NOT NULL DEFAULT 'ACTIVE',
                `created_at` INTEGER,
                `created_by` TEXT,
                `updated_at` INTEGER,
                `updated_by` TEXT,
                `entity_version` INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `organizations_new` (
                `id`, `organization_id`, `name`, `lifecycle_status`, `entity_version`
            )
            SELECT 
                `id`,
                CASE WHEN `remoteId` IS NOT NULL AND length(`remoteId`) = 36 AND `remoteId` GLOB '[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]' THEN lower(`remoteId`) ELSE NULL END,
                trim(`name`),
                `lifecycleStatus`,
                CASE WHEN `remoteRevision` IS NOT NULL AND `remoteRevision` >= 1 THEN `remoteRevision` ELSE NULL END
            FROM `organizations`
        """.trimIndent())

        db.execSQL("DROP TABLE IF EXISTS `organizations`")
        db.execSQL("ALTER TABLE `organizations_new` RENAME TO `organizations`")

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_organizations_organization_id` ON `organizations` (`organization_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_organizations_code` ON `organizations` (`code`)")

        createOrganizationTriggers(db)
    }
}

internal val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `organization_memberships` (
                `legacy_organization_id` INTEGER NOT NULL,
                `organization_id` TEXT,
                `user_id` TEXT NOT NULL,
                `legacy_role` TEXT NOT NULL,
                `role` TEXT,
                `membership_id` TEXT,
                `lifecycle_status` TEXT,
                `created_at` INTEGER,
                `created_by` TEXT,
                `updated_at` INTEGER,
                `updated_by` TEXT,
                `entity_version` INTEGER,
                PRIMARY KEY(`legacy_organization_id`, `user_id`),
                FOREIGN KEY(`legacy_organization_id`) REFERENCES `organizations`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`organization_id`) REFERENCES `organizations`(`organization_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `organization_memberships` (
                `legacy_organization_id`, `organization_id`, `user_id`, `legacy_role`, `role`, `membership_id`, `lifecycle_status`, `created_at`, `created_by`, `updated_at`, `updated_by`, `entity_version`
            )
            SELECT 
                m.`organization_id`,
                o.`organization_id`,
                m.`user_id`,
                m.`role`,
                CASE m.`role`
                    WHEN 'Admin' THEN 'ADMIN'
                    WHEN 'Standard' THEN 'STANDARD'
                    WHEN 'Standard User' THEN 'STANDARD'
                    ELSE NULL
                END,
                NULL,
                NULL,
                NULL,
                NULL,
                NULL,
                NULL,
                NULL
            FROM `organization_members` m
            LEFT JOIN `organizations` o ON m.`organization_id` = o.`id`
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_organization_memberships_legacy_organization_id` ON `organization_memberships` (`legacy_organization_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_organization_memberships_organization_id` ON `organization_memberships` (`organization_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_organization_memberships_user_id` ON `organization_memberships` (`user_id`)")
    }
}

internal val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `locations_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `organization_id` INTEGER NOT NULL,
                `remoteId` TEXT,
                `code` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `lifecycleStatus` TEXT NOT NULL,
                `remoteRevision` INTEGER,
                `location_id` TEXT,
                `canonical_organization_id` TEXT,
                `address` TEXT,
                `city` TEXT,
                `country` TEXT,
                `notes` TEXT,
                `created_at` INTEGER,
                `created_by` TEXT,
                `updated_at` INTEGER,
                `updated_by` TEXT,
                `entity_version` INTEGER,
                FOREIGN KEY(`organization_id`) REFERENCES `organizations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `locations_new` (
                `id`, `organization_id`, `remoteId`, `code`, `name`, `lifecycleStatus`, `remoteRevision`,
                `location_id`, `canonical_organization_id`, `address`, `city`, `country`, `notes`,
                `created_at`, `created_by`, `updated_at`, `updated_by`, `entity_version`
            )
            SELECT 
                l.`id`,
                l.`organization_id`,
                l.`remoteId`,
                l.`code`,
                l.`name`,
                l.`lifecycleStatus`,
                l.`remoteRevision`,
                CASE WHEN l.`remoteId` IS NOT NULL AND length(l.`remoteId`) = 36 AND l.`remoteId` GLOB '[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f]-[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]' THEN lower(l.`remoteId`) ELSE NULL END,
                o.`organization_id`,
                NULL,
                NULL,
                NULL,
                NULL,
                NULL,
                NULL,
                NULL,
                NULL,
                NULL
            FROM `locations` l
            LEFT JOIN `organizations` o ON l.`organization_id` = o.`id`
        """.trimIndent())

        db.execSQL("DROP TABLE IF EXISTS `locations`")
        db.execSQL("ALTER TABLE `locations_new` RENAME TO `locations`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_organization_id` ON `locations` (`organization_id`)")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SiloMathDatabase {
        return Room.databaseBuilder(
            context,
            SiloMathDatabase::class.java,
            "silomath_db"
        )
        .addMigrations(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
            MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
            MIGRATION_14_15
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                CanonicalOfflineGuards.install(db)
                createOrganizationTriggers(db)
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                createOrganizationTriggers(db)
            }
        })
        .build()
    }

    @Provides
    fun provideUserDao(db: SiloMathDatabase): UserDao = db.userDao()

    @Provides
    fun provideDeviceInstallationDao(db: SiloMathDatabase): DeviceInstallationDao = db.deviceInstallationDao()

    @Provides
    fun provideOrganizationDao(db: SiloMathDatabase): OrganizationDao = db.organizationDao()

    @Provides
    fun provideOrganizationMemberDao(db: SiloMathDatabase): OrganizationMemberDao = db.organizationMemberDao()

    @Provides
    fun provideLocationDao(db: SiloMathDatabase): LocationDao = db.locationDao()

    @Provides
    fun provideStorageDao(db: SiloMathDatabase): StorageDao = db.storageDao()

    @Provides
    fun provideGeometryDao(db: SiloMathDatabase): GeometryDao = db.geometryDao()

    @Provides
    fun provideCommodityDao(db: SiloMathDatabase): CommodityDao = db.commodityDao()

    @Provides
    fun provideClientDao(db: SiloMathDatabase): ClientDao = db.clientDao()

    @Provides
    fun provideCampaignDao(db: SiloMathDatabase): CampaignDao = db.campaignDao()

    @Provides
    fun provideStocktakingDao(db: SiloMathDatabase): StocktakingDao = db.stocktakingDao()

    @Provides
    fun provideInventoryDao(db: SiloMathDatabase): InventoryDao = db.inventoryDao()

    @Provides
    fun provideSyncDao(db: SiloMathDatabase): SyncDao = db.syncDao()

    @Provides
    fun provideSyncOperationDao(db: SiloMathDatabase): SyncOperationDao = db.syncOperationDao()

    @Provides
    fun provideAuditDao(db: SiloMathDatabase): AuditDao = db.auditDao()
}
