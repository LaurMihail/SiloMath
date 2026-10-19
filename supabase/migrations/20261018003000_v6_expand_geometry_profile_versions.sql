-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical immutable GeometryProfileVersion persistence.
--
-- Governing baseline:
--   06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   13_CALCULATION_ENGINE_SPECIFICATION
--
-- Additive only:
--   * no legacy public.* table is dropped or rewritten;
--   * no historical data is fabricated;
--   * family parameters are explicit physical columns, not opaque business JSON;
--   * FK completion remains IMP-DB-003;
--   * unique/check/index hardening remains IMP-DB-004..006;
--   * family-specific parameter validation remains enforced by the frozen
--     domain/calculation contract and later database hardening tasks.

create schema if not exists silomath;

create table if not exists silomath.geometry_profile_versions (
    geometry_profile_version_id uuid primary key,
    geometry_profile_id uuid not null,
    version_number integer not null,
    geometry_family_id uuid not null,

    diameter_m numeric(20,6) null,
    cylinder_height_m numeric(20,6) null,
    hopper_height_m numeric(20,6) null,
    outlet_diameter_m numeric(20,6) null,

    side_length_m numeric(20,6) null,
    length_m numeric(20,6) null,
    width_m numeric(20,6) null,
    body_height_m numeric(20,6) null,
    height_m numeric(20,6) null,

    verified_area_m2 numeric(20,6) null,
    area_provenance text null,

    measurement_reference_height_m numeric(20,6) not null,
    maximum_fill_height_m numeric(20,6) not null,
    geometry_origin text not null,

    max_capacity_m3 numeric(20,6) not null,

    formula_version text not null,
    schema_version text not null,

    created_at timestamptz not null,
    created_by uuid not null
);
