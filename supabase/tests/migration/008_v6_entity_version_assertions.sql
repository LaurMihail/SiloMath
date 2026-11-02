-- SiloMath V6
-- IMP-DB-007
-- Executable assertions for canonical entityVersion persistence.

DO $$
DECLARE
    v_table_name text;
    v_trigger_name text;
    v_data_type text;
    v_is_nullable text;
    v_column_default text;
BEGIN
    FOREACH v_table_name IN ARRAY ARRAY[
        'users',
        'organizations',
        'organization_memberships',
        'locations',
        'storage_units',
        'storage_spaces',
        'clients',
        'geometry_profiles',
        'inventory_campaigns',
        'campaign_team_assignments',
        'stocktaking_records',
        'stocktaking_revisions',
        'measurements',
        'issues',
        'organization_configurations'
    ] LOOP
        SELECT c.data_type, c.is_nullable, c.column_default
          INTO v_data_type, v_is_nullable, v_column_default
        FROM information_schema.columns c
        WHERE c.table_schema = 'silomath'
          AND c.table_name = v_table_name
          AND c.column_name = 'entity_version';

        IF v_data_type IS NULL THEN
            RAISE EXCEPTION 'DB007 missing entity_version on silomath.%', v_table_name;
        END IF;

        IF v_data_type <> 'bigint' THEN
            RAISE EXCEPTION 'DB007 entity_version must be BIGINT on silomath.% (found %)', v_table_name, v_data_type;
        END IF;

        IF v_is_nullable <> 'NO' THEN
            RAISE EXCEPTION 'DB007 entity_version must be NOT NULL on silomath.%', v_table_name;
        END IF;

        IF v_column_default IS NULL OR replace(v_column_default, '::bigint', '') <> '1' THEN
            RAISE EXCEPTION 'DB007 entity_version must default to 1 on silomath.% (found %)', v_table_name, v_column_default;
        END IF;

        v_trigger_name := format('trg_%s_entity_version', v_table_name);

        IF NOT EXISTS (
            SELECT 1
            FROM pg_trigger tg
            JOIN pg_class t ON t.oid = tg.tgrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            JOIN pg_proc p ON p.oid = tg.tgfoid
            JOIN pg_namespace pn ON pn.oid = p.pronamespace
            WHERE n.nspname = 'silomath'
              AND t.relname = v_table_name
              AND tg.tgname = v_trigger_name
              AND NOT tg.tgisinternal
              AND pn.nspname = 'silomath'
              AND p.proname = 'enforce_entity_version_increment'
        ) THEN
            RAISE EXCEPTION 'DB007 missing authoritative entity_version trigger on silomath.%', v_table_name;
        END IF;
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'silomath'
          AND p.proname = 'enforce_entity_version_increment'
    ) THEN
        RAISE EXCEPTION 'DB007 missing silomath.enforce_entity_version_increment()';
    END IF;

    RAISE NOTICE 'V6 DB007 ENTITY VERSION ASSERTIONS PASSED';
END;
$$;
