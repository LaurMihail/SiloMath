-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical CalculationResult reproducibility record.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   13_CALCULATION_ENGINE_SPECIFICATION
--
-- Additive only:
--   * CalculationResult is replaceable while its revision is WORKING and frozen
--     when the revision is VALIDATED;
--   * validated/final historical calculations are reproduced from frozen inputs
--     and pinned versions, never from current mutable master data;
--   * structured snapshots below are calculation-reproducibility records, not
--     authoritative replacements for canonical business entities;
--   * input_fingerprint stores the canonical SHA-256 normalized input fingerprint;
--   * Mark Empty uses the explicit workflow path: volume/quantity are zero while
--     geometry/measurement/density fields may be null where calculation is bypassed;
--   * FK completion remains IMP-DB-003;
--   * check/current/index hardening remains IMP-DB-004..006.

create schema if not exists silomath;

create table if not exists silomath.calculation_results (
    calculation_result_id uuid primary key,
    stocktaking_revision_id uuid not null,

    surface_model_version text not null,
    aggregation_method text not null,
    aggregation_rule_version integer not null,

    input_schema_version text not null,
    engine_version text not null,
    normalization_version text not null,

    formula_code text null,
    formula_version text null,

    geometry_family_id uuid null,
    geometry_profile_version_id uuid null,

    frozen_calculation_context jsonb not null,
    datum_metadata jsonb null,
    geometry_inputs_snapshot jsonb null,
    ordered_normalized_readings jsonb null,

    geometry_origin text null,
    measurement_reference_height_m numeric(20,6) null,
    maximum_fill_height_m numeric(20,6) null,
    max_capacity_m3 numeric(20,6) null,

    effective_ullage_m numeric(20,6) null,
    effective_fill_height_m numeric(20,6) null,

    product_volume_m3 numeric(20,6) not null,

    entered_density_value numeric(20,6) null,
    entered_density_unit text null,
    actual_bulk_density_kg_m3 numeric(20,6) null,

    physical_quantity_kg numeric(20,6) not null,

    input_fingerprint text not null,
    rounding_policy text not null,

    calculated_at timestamptz not null,
    calculated_by uuid null,
    calculation_source text not null
);
