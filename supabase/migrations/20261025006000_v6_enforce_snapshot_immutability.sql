-- SiloMath V6
-- IMP-DB-008
-- Immutable CampaignSnapshot / FinalInventorySnapshot persistence hardening.
--
-- Governing baseline:
--   05_WORKFLOWS_STATE_MACHINES
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--
-- Snapshot rows are append-once historical truth. Later master-data, team,
-- Commodity, geometry or Campaign changes must never rewrite or delete them.
-- This migration does not fabricate missing historical launch provenance.

create or replace function silomath.reject_immutable_snapshot_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception '% on immutable snapshot table %.% is prohibited', tg_op, tg_table_schema, tg_table_name
        using errcode = '55000';
end;
$$;

DO $$
DECLARE
    table_name text;
    trigger_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'campaign_snapshots',
        'campaign_snapshot_scope_items',
        'campaign_snapshot_team_items',
        'campaign_snapshot_commodity_items',
        'final_inventory_snapshots',
        'final_inventory_snapshot_entries'
    ] LOOP
        trigger_name := format('trg_%s_immutable', table_name);

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
                'create trigger %I before update or delete on silomath.%I for each row execute function silomath.reject_immutable_snapshot_mutation()',
                trigger_name,
                table_name
            );
        END IF;
    END LOOP;
END;
$$;
