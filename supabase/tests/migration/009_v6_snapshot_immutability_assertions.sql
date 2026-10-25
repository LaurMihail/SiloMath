-- SiloMath V6
-- IMP-DB-008
-- Executable assertions for immutable snapshot persistence.

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
            JOIN pg_proc p ON p.oid = tg.tgfoid
            JOIN pg_namespace pn ON pn.oid = p.pronamespace
            WHERE n.nspname = 'silomath'
              AND t.relname = table_name
              AND tg.tgname = trigger_name
              AND NOT tg.tgisinternal
              AND pn.nspname = 'silomath'
              AND p.proname = 'reject_immutable_snapshot_mutation'
        ) THEN
            RAISE EXCEPTION 'DB008 missing immutable snapshot trigger on silomath.%', table_name;
        END IF;
    END LOOP;

    RAISE NOTICE 'V6 DB008 SNAPSHOT IMMUTABILITY ASSERTIONS PASSED';
END;
$$;
