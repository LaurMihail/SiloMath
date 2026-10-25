-- SiloMath V6
-- IMP-DB-009
-- Executable assertions for authoritative append-only AuditEvent persistence.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger tg
        JOIN pg_class t ON t.oid = tg.tgrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        JOIN pg_proc p ON p.oid = tg.tgfoid
        JOIN pg_namespace pn ON pn.oid = p.pronamespace
        WHERE n.nspname = 'silomath'
          AND t.relname = 'audit_events'
          AND tg.tgname = 'trg_audit_events_append_only_rows'
          AND NOT tg.tgisinternal
          AND pn.nspname = 'silomath'
          AND p.proname = 'reject_audit_history_mutation'
    ) THEN
        RAISE EXCEPTION 'DB009 missing row-level append-only trigger on silomath.audit_events';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger tg
        JOIN pg_class t ON t.oid = tg.tgrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        JOIN pg_proc p ON p.oid = tg.tgfoid
        JOIN pg_namespace pn ON pn.oid = p.pronamespace
        WHERE n.nspname = 'silomath'
          AND t.relname = 'audit_events'
          AND tg.tgname = 'trg_audit_events_append_only_truncate'
          AND NOT tg.tgisinternal
          AND pn.nspname = 'silomath'
          AND p.proname = 'reject_audit_history_mutation'
    ) THEN
        RAISE EXCEPTION 'DB009 missing TRUNCATE guard on silomath.audit_events';
    END IF;

    RAISE NOTICE 'V6 DB009 AUDIT APPEND-ONLY ASSERTIONS PASSED';
END;
$$;
