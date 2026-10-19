-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical immutable Final Inventory Snapshot persistence.
--
-- Governing baseline:
--   01_PRODUCT_BASELINE
--   02_DECISION_REGISTER
--   05_WORKFLOWS_STATE_MACHINES
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--
-- Additive only:
--   * Inventory Close is server-authoritative and online;
--   * one successful Close creates exactly one immutable FinalInventorySnapshot;
--   * exactly one immutable entry is created for every scoped Storage Space;
--   * completed history is rendered from frozen snapshot values, never by
--     reinterpreting current mutable master data;
--   * structured JSONB fields represent explicit frozen snapshot components,
--     not one opaque authoritative business object;
--   * snapshot/entry uniqueness remains IMP-DB-004;
--   * FK completion remains IMP-DB-003;
--   * immutable-write enforcement remains dedicated hardening.

create schema if not exists silomath;

create table if not exists silomath.final_inventory_snapshots (
    final_snapshot_id uuid primary key,

    organization_id uuid not null,
    campaign_id uuid not null,
    campaign_snapshot_id uuid not null,

    closed_at timestamptz not null,
    closed_by uuid not null,

    snapshot_schema_version text not null,
    calculation_compatibility_versions jsonb not null,

    created_at timestamptz not null,
    created_by uuid not null
);

create table if not exists silomath.final_inventory_snapshot_entries (
    entry_id uuid primary key,
    final_snapshot_id uuid not null,
    storage_space_id uuid not null,

    hierarchy_snapshot jsonb not null,
    geometry_snapshot jsonb not null,

    commodity_snapshot jsonb null,
    ownership_client_snapshot jsonb null,

    is_empty boolean not null,

    measurements_snapshot jsonb null,
    operational_values jsonb not null,
    calculation_metadata jsonb not null,
    validation_metadata jsonb not null,

    issue_references_resolution_summary jsonb not null,
    evidence_references jsonb not null
);
