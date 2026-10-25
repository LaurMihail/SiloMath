-- SiloMath V6
-- IMP-DB-007
-- Canonical entityVersion persistence and server-controlled increment hardening.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA (DB-D09)
--   12_OFFLINE_SYNC_SPECIFICATION
--
-- Scope:
--   * apply only to entities explicitly versioned by the frozen logical model;
--   * entity_version is BIGINT NOT NULL, starts at 1;
--   * ordinary UPDATE increments the authoritative version exactly once;
--   * callers may not assign entity_version directly;
--   * stale base-version rejection remains an API/mutation precondition concern
--     using baseEntityVersion; this migration does not invent a database-side
--     conflict protocol or weaken the frozen sync contract.

create or replace function silomath.enforce_entity_version_increment()
returns trigger
language plpgsql
as $$
begin
    if new.entity_version is distinct from old.entity_version then
        raise exception 'entity_version is server-controlled for %.%', tg_table_schema, tg_table_name
            using errcode = '22023';
    end if;

    new.entity_version := old.entity_version + 1;
    return new;
end;
$$;

DO $$
DECLARE
    table_name text;
    trigger_name text;
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
        EXECUTE format(
            'alter table silomath.%I alter column entity_version set default 1',
            table_name
        );

        EXECUTE format(
            'alter table silomath.%I alter column entity_version set not null',
            table_name
        );

        trigger_name := format('trg_%s_entity_version', table_name);

        IF NOT EXISTS (
            SELECT 1
            FROM pg_trigger tg
            JOIN pg_class t ON t.oid = tg.tgrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE n.nspname = 'silomath'
              AND t.relname = table_name
              AND tg.tgname = trigger_name
              AND NOT tg.tgisinternal
        ) THEN
            EXECUTE format(
                'create trigger %I before update on silomath.%I for each row execute function silomath.enforce_entity_version_increment()',
                trigger_name,
                table_name
            );
        END IF;
    END LOOP;
END;
$$;
