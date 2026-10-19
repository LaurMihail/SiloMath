-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical Measurement persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   13_CALCULATION_ENGINE_SPECIFICATION
--
-- Additive only:
--   * Measurement is owned by exactly one StocktakingRevision;
--   * V6 numerical field measurement semantics are ULLAGE;
--   * raw operator value/unit and canonical normalized ullage are both preserved;
--   * datum_metadata preserves the structured frozen datum/reference context;
--   * validated-revision measurements are immutable;
--   * FK completion remains IMP-DB-003;
--   * unique/sequence/check/index hardening remains IMP-DB-004..006.

create schema if not exists silomath;

create table if not exists silomath.measurements (
    measurement_id uuid primary key,
    stocktaking_revision_id uuid not null,

    sequence_number integer not null,

    raw_value numeric(20,6) not null,
    raw_unit text not null,
    normalized_ullage_m numeric(20,6) not null,

    datum_metadata jsonb not null,
    geometry_profile_version_id uuid not null,

    captured_at timestamptz not null,
    captured_by uuid not null,

    entity_version bigint not null default 1
);
