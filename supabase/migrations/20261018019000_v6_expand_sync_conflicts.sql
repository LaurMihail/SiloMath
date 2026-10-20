-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical SyncConflict persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   12_OFFLINE_SYNC_SPECIFICATION
--
-- Additive only:
--   * SyncConflict preserves explicit local/server conflict context;
--   * local_attempt and server_value are structured operational conflict envelopes;
--   * conflict records preserve tenant, originating operation and target entity context;
--   * lifecycle is OPEN / RESOLVED;
--   * approved resolution types are RELOAD_AND_REENTER /
--     DISCARD_LOCAL_ATTEMPT / NO_ACTION_REQUIRED;
--   * FORCE_OVERWRITE and AUTO_MERGE do not exist;
--   * conflict_class persistence is intentionally not introduced here because
--     Doc12 defines the classification taxonomy while Doc09/Doc10 do not map
--     a canonical physical conflict_class field;
--   * FK completion remains IMP-DB-003;
--   * lifecycle/value-domain consistency checks remain IMP-DB-005;
--   * indexes remain IMP-DB-006.

create schema if not exists silomath;

create table if not exists silomath.sync_conflicts (
    sync_conflict_id uuid primary key,

    organization_id uuid not null,
    sync_operation_id uuid not null,

    entity_type text not null,
    entity_id uuid not null,

    local_attempt jsonb not null,
    server_value jsonb not null,

    base_entity_version bigint null,
    server_entity_version bigint null,

    detected_at timestamptz not null,
    status text not null,

    resolution_type text null,
    resolved_at timestamptz null,
    resolved_by uuid null
);
