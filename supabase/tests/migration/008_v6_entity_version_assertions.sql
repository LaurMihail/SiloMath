-- SiloMath V6
-- IMP-DB-007
-- Executable assertions for canonical entityVersion persistence.

DO $$
DECLARE
    table_name text;
    trigger_name text;
    data_type text;
    is_nullable text;
    column_default text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
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
          INTO data_type, is_nullable, column_default
        FROM information_schema.columns c
        WHERE c.table_schema = 'silomath'
          AND c.table_name = table_name
          AND c.column_name = 'entity_version';

        IF data_type IS NULL THEN
            RAISE EXCEPTION 'DB007 missing entity_version on silomath.%', table_name;
        END IF;

        IF data_type <> 'bigint' THEN
            RAISE EXCEPTION 'DB007 entity_version must be BIGINT on silomath.% (found %)', table_name, data_type;
        END IF;

        IF is_nullable <> 'NO' THEN
            RAISE EXCEPTION 'DB007 entity_version must be NOT NULL on silomath.%', table_name;
        END IF;

        IF column_default IS NULL OR replace(column_default, '::bigint', '') <> '1' THEN
            RAISE EXCEPTION 'DB007 entity_version must default to 1 on silomath.% (found %)', table_name, column_default;
        END IF;

        trigger_name := format('trg_%s_entity_version', table_name);

        IF NOT EXISTS (
            SELECT 1
            FROM pg_trigger tg
            JOIN pg_class t ON t.oid = tg.tgrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            JOIN pg_proc p ON p.oid = tg.tgfoid
            JOIN pg_namespace pn ON pn.oid = p.pronamespace
            WHERE n.nspname = 'silomath'
              AND t.relname = table_name
              AND tg.tgname = trigger_name
              AND NOT tg.tgisinternal
              AND pn.nspname = 'silomath'
              AND p.proname = 'enforce_entity_version_increment'
        ) THEN
            RAISE EXCEPTION 'DB007 missing authoritative entity_version trigger on silomath.%', table_name;
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
