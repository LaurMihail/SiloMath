-- SiloMath V6
-- IMP-DB-004
-- PostgreSQL uniqueness hardening.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--
-- Scope:
--   * enforce only frozen business uniqueness;
--   * preserve DB005 CHECK and DB006 performance-index scope separation;
--   * fail closed if retained data violates a required unique key;
--   * do not rewrite or fabricate retained historical rows.

DO $$
DECLARE
    r record;
    has_equivalent boolean;
BEGIN
    FOR r IN
        SELECT * FROM (VALUES
            ('users', 'email_normalized', 'uq_users_email_normalized'),
            ('organizations', 'code', 'uq_organizations_code'),
            ('organization_memberships', 'organization_id,user_id', 'uq_organization_memberships_org_user'),
            ('locations', 'organization_id,code', 'uq_locations_org_code'),
            ('storage_units', 'location_id,code', 'uq_storage_units_location_code'),
            ('storage_spaces', 'storage_unit_id,code', 'uq_storage_spaces_unit_code'),
            ('clients', 'organization_id,code', 'uq_clients_org_code'),
            ('geometry_families', 'family_code', 'uq_geometry_families_family_code'),
            ('geometry_profiles', 'organization_id,code', 'uq_geometry_profiles_org_code'),
            ('geometry_profile_versions', 'geometry_profile_id,version_number', 'uq_geometry_profile_versions_profile_version'),
            ('commodity_versions', 'commodity_id,version_number', 'uq_commodity_versions_commodity_version'),
            ('inventory_campaigns', 'organization_id,campaign_year,sequence', 'uq_inventory_campaigns_org_year_sequence'),
            ('inventory_campaigns', 'organization_id,code', 'uq_inventory_campaigns_org_code'),
            ('campaign_scopes', 'campaign_id,storage_space_id', 'uq_campaign_scopes_campaign_space'),
            ('campaign_snapshots', 'campaign_id', 'uq_campaign_snapshots_campaign'),
            ('stocktaking_records', 'campaign_id,storage_space_id', 'uq_stocktaking_records_campaign_space'),
            ('stocktaking_revisions', 'stocktaking_record_id,revision_number', 'uq_stocktaking_revisions_root_revision'),
            ('measurements', 'stocktaking_revision_id,sequence_number', 'uq_measurements_revision_sequence'),
            ('final_inventory_snapshots', 'campaign_id', 'uq_final_inventory_snapshots_campaign'),
            ('final_inventory_snapshot_entries', 'final_snapshot_id,storage_space_id', 'uq_final_snapshot_entries_snapshot_space'),
            ('offline_working_set_manifests', 'campaign_id,device_installation_id,package_version', 'uq_offline_manifests_campaign_device_version'),
            ('user_preferences', 'user_id', 'uq_user_preferences_user'),
            ('organization_configurations', 'organization_id,configuration_key', 'uq_organization_configurations_org_key'),
            ('sync_operation_dependencies', 'parent_operation_id,dependent_operation_id', 'uq_sync_dependencies_parent_dependent')
        ) AS x(table_name, columns_csv, index_name)
    LOOP
        SELECT EXISTS (
            SELECT 1
            FROM pg_index i
            JOIN pg_class t ON t.oid = i.indrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            CROSS JOIN LATERAL (
                SELECT string_agg(a.attname, ',' ORDER BY u.ordinality) AS columns_csv
                FROM unnest(i.indkey) WITH ORDINALITY AS u(attnum, ordinality)
                JOIN pg_attribute a
                  ON a.attrelid = t.oid
                 AND a.attnum = u.attnum
                WHERE u.attnum > 0
            ) keys
            WHERE n.nspname = 'silomath'
              AND t.relname = r.table_name
              AND i.indisunique
              AND i.indpred IS NULL
              AND keys.columns_csv = r.columns_csv
        ) INTO has_equivalent;

        IF NOT has_equivalent THEN
            EXECUTE format(
                'create unique index %I on silomath.%I (%s)',
                r.index_name,
                r.table_name,
                (SELECT string_agg(format('%I', btrim(c)), ', ')
                 FROM unnest(string_to_array(r.columns_csv, ',')) c)
            );
        END IF;
    END LOOP;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'silomath'
          AND tablename = 'campaign_team_assignments'
          AND lower(indexdef) LIKE '%unique index%'
          AND lower(indexdef) LIKE '%(campaign_id, user_id)%'
          AND lower(indexdef) LIKE '%where (status = ''active''::text)%'
    ) THEN
        CREATE UNIQUE INDEX uq_campaign_team_assignments_one_active
            ON silomath.campaign_team_assignments (campaign_id, user_id)
            WHERE status = 'ACTIVE';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'silomath'
          AND tablename = 'offline_working_set_manifests'
          AND lower(indexdef) LIKE '%unique index%'
          AND lower(indexdef) LIKE '%(campaign_id, device_installation_id)%'
          AND lower(indexdef) LIKE '%where is_current%'
    ) THEN
        CREATE UNIQUE INDEX uq_offline_manifests_one_current
            ON silomath.offline_working_set_manifests (campaign_id, device_installation_id)
            WHERE is_current;
    END IF;
END;
$$;
