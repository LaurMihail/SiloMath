-- SiloMath V6
-- IMP-DB-001
-- EXPAND/MIGRATE phase: reconcile canonical SyncConflict server_version.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   12_OFFLINE_SYNC_SPECIFICATION
--
-- Forward-safe compatibility:
--   * Doc09 domain semantics use serverEntityVersion;
--   * Doc10 canonical physical name is server_version;
--   * existing server_entity_version is retained temporarily for compatibility;
--   * existing values are copied forward without reinterpretation;
--   * removal of the compatibility column is a later CONTRACT step;
--   * FK completion remains IMP-DB-003;
--   * lifecycle/value-domain checks remain IMP-DB-005;
--   * indexes remain IMP-DB-006.
--
-- This migration does not close the full IMP-DB-001 task.

alter table silomath.sync_conflicts
    add column if not exists server_version bigint null;

update silomath.sync_conflicts
set server_version = server_entity_version
where server_version is null
  and server_entity_version is not null;
