-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical SyncOperationDependency persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   12_OFFLINE_SYNC_SPECIFICATION
--
-- Additive only:
--   * SyncOperationDependency is an immutable operational dependency edge;
--   * dependency_type is frozen to REQUIRED_BEFORE;
--   * parent and dependent operations must be distinct;
--   * dependency uniqueness remains IMP-DB-004;
--   * dependency_type and no-self-dependency checks remain IMP-DB-005;
--   * FK completion remains IMP-DB-003;
--   * dependent-operation lookup index remains IMP-DB-006.

create schema if not exists silomath;

create table if not exists silomath.sync_operation_dependencies (
    dependency_id uuid primary key,

    parent_operation_id uuid not null,
    dependent_operation_id uuid not null,

    dependency_type text not null,

    created_at timestamptz not null
);
