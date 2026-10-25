-- SiloMath V6
-- IMP-DB-004
-- Executable assertions for the frozen PostgreSQL uniqueness contract.

DO $$
DECLARE
    r record;
    has_equivalent boolean;
BEGIN
    FOR r IN
        SELECT * FROM (VALUES
            ('users', 'email_normalized'),
            ('organizations', 'code'),
            ('organization_memberships', 'organization_id,user_id'),
            ('locations', 'organization_id,code'),
            ('storage_units', 'location_id,code'),
            ('storage_spaces', 'storage_unit_id,code'),
            ('clients', 'organization_id,code'),
            ('geometry_families', 'family_code'),
            ('geometry_profiles', 'organization_id,code'),
            ('geometry_profile_versions', 'geometry_profile_id,version_number'),
            ('commodity_versions', 'commodity_id,version_number'),
            ('inventory_campaigns', 'organization_id,campaign_year,sequence'),
            ('inventory_campaigns', 'organization_id,code'),
            ('campaign_scopes', 'campaign_id,storage_space_id'),
            ('campaign_snapshots', 'campaign_id'),
            ('stocktaking_records', 'campaign_id,storage_space_id'),
            ('stocktaking_revisions', 'stocktaking_record_id,revision_number'),
            ('measurements', 'stocktaking_revision_id,sequence_number'),
            ('final_inventory_snapshots', 'campaign_id'),
            ('final_inventory_snapshot_entries', 'final_snapshot_id,storage_space_id'),
            ('offline_working_set_manifests', 'campaign_id,device_installation_id,package_version'),
            ('user_preferences', 'user_id'),
            ('organization_configurations', 'organization_id,configuration_key'),
            ('sync_operations', 'idempotency_key'),
            ('sync_operation_dependencies', 'parent_operation_id,dependent_operation_id')
        ) AS x(table_name, columns_csv)
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
            RAISE EXCEPTION 'DB004 missing UNIQUE key silomath.%(%)', r.table_name, r.columns_csv;
        END IF;
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'silomath'
          AND tablename = 'campaign_team_assignments'
          AND indexname IN (
              'campaign_team_assignments_one_active_user_idx',
              'uq_campaign_team_assignments_one_active'
          )
          AND lower(indexdef) LIKE '%unique index%'
          AND lower(indexdef) LIKE '%(campaign_id, user_id)%'
          AND lower(indexdef) LIKE '%where (status = ''active''::text)%'
    ) THEN
        RAISE EXCEPTION 'DB004 missing partial UNIQUE ACTIVE(campaign_id,user_id) for campaign_team_assignments';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'silomath'
          AND tablename = 'offline_working_set_manifests'
          AND indexname = 'uq_offline_manifests_one_current'
          AND lower(indexdef) LIKE '%unique index%'
          AND lower(indexdef) LIKE '%(campaign_id, device_installation_id)%'
          AND lower(indexdef) LIKE '%where is_current%'
    ) THEN
        RAISE EXCEPTION 'DB004 missing one-current-manifest partial UNIQUE key';
    END IF;
END;
$$;

DO $$
BEGIN
    RAISE NOTICE 'V6 DB004 UNIQUE CONSTRAINT ASSERTIONS PASSED';
END;
$$;
