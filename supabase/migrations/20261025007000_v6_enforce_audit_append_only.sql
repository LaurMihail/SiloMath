-- SiloMath V6
-- IMP-DB-009
-- Authoritative AuditEvent append-only persistence hardening.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   17_SECURITY_AUDIT_PRIVACY
--
-- audit_events is authoritative history. Application paths may INSERT new
-- events but must never UPDATE, DELETE or TRUNCATE existing audit history.

create or replace function silomath.reject_audit_history_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception '% on append-only audit table %.% is prohibited', tg_op, tg_table_schema, tg_table_name
        using errcode = '55000';
end;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger tg
        JOIN pg_class t ON t.oid = tg.tgrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'silomath'
          AND t.relname = 'audit_events'
          AND tg.tgname = 'trg_audit_events_append_only_rows'
          AND NOT tg.tgisinternal
    ) THEN
        create trigger trg_audit_events_append_only_rows
            before update or delete on silomath.audit_events
            for each row
            execute function silomath.reject_audit_history_mutation();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger tg
        JOIN pg_class t ON t.oid = tg.tgrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'silomath'
          AND t.relname = 'audit_events'
          AND tg.tgname = 'trg_audit_events_append_only_truncate'
          AND NOT tg.tgisinternal
    ) THEN
        create trigger trg_audit_events_append_only_truncate
            before truncate on silomath.audit_events
            for each statement
            execute function silomath.reject_audit_history_mutation();
    END IF;
END;
$$;
