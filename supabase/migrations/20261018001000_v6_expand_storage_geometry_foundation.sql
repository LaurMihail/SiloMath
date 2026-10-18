-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical Storage / Geometry foundation tables.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   13_CALCULATION_ENGINE_SPECIFICATION
--
-- Additive only:
--   * no legacy public.* table is dropped or rewritten;
--   * no legacy data is fabricated;
--   * tenant/RLS hardening remains IMP-DB-002;
--   * FK completion remains IMP-DB-003;
--   * unique/check/index hardening remains IMP-DB-004..006;
--   * geometry_profile_versions are expanded separately against the exact
--     frozen F01-F10 parameter/version contract.

create schema if not exists silomath;

create table if not exists silomath.storage_units (
    storage_unit_id uuid primary key,
    organization_id uuid not null,
    location_id uuid not null,

    name text not null,
    code text not null,
    description text null,

    lifecycle_status text not null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1
);

create table if not exists silomath.geometry_families (
    geometry_family_id uuid primary key,

    family_code text not null,
    name text not null,
    formula_version text not null,
    parameter_contract text not null
);

create table if not exists silomath.geometry_profiles (
    geometry_profile_id uuid primary key,
    organization_id uuid not null,

    name text not null,
    code text not null,

    geometry_family_id uuid not null,
    lifecycle_status text not null,
    current_version_id uuid not null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1
);
