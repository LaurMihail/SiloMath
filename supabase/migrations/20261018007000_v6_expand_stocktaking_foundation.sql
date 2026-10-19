-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical StocktakingRecord / StocktakingRevision foundation.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--
-- Additive only:
--   * StocktakingRecord is the stable Campaign x StorageSpace aggregate root;
--   * current_revision_id is the controlled pointer to the current revision;
--   * StocktakingRevision owns revision state and stocktaking business inputs;
--   * validated revisions are immutable;
--   * Reset Validation creates a new WORKING revision rather than rewriting history;
--   * classification is derived and is not persisted on StocktakingRecord;
--   * FK completion remains IMP-DB-003;
--   * unique/check/index hardening remains IMP-DB-004..006.

create schema if not exists silomath;

create table if not exists silomath.stocktaking_records (
    stocktaking_record_id uuid primary key,
    organization_id uuid not null,
    campaign_id uuid not null,
    storage_space_id uuid not null,

    current_revision_id uuid null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1
);

create table if not exists silomath.stocktaking_revisions (
    stocktaking_revision_id uuid primary key,
    stocktaking_record_id uuid not null,
    revision_number integer not null,

    revision_state text not null,
    based_on_revision_id uuid null,

    is_empty boolean not null,

    commodity_version_id uuid null,
    actual_bulk_density_kg_m3 numeric(20,6) null,
    ownership text null,
    client_id uuid null,

    notes text null,

    calculation_result_id uuid null,

    validated_at timestamptz null,
    validated_by uuid null,

    created_at timestamptz not null,
    created_by uuid not null,

    entity_version bigint not null default 1
);
