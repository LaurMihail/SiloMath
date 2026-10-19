-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical Commodity / CommodityVersion foundation.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   14_COMMODITY_SPECIFICATION
--
-- Additive only:
--   * Commodity is global platform-governed reference data;
--   * CommodityVersion rows are immutable historical definitions;
--   * display_name is version-owned per the frozen Commodity contract;
--   * no tenant-specific/custom Commodity capability is introduced;
--   * Reference Test Weight is reference-only and never substitutes Actual Bulk Density;
--   * controlled definitionMetadata registry persistence is expanded separately;
--   * FK completion remains IMP-DB-003;
--   * unique/check/index hardening remains IMP-DB-004..006.

create schema if not exists silomath;

create table if not exists silomath.commodities (
    commodity_id uuid primary key,
    code text not null,
    current_version_id uuid not null,
    lifecycle_status text not null,
    created_at timestamptz not null,
    created_by uuid not null
);

create table if not exists silomath.commodity_versions (
    commodity_version_id uuid primary key,
    commodity_id uuid not null,
    version_number integer not null,

    display_name text not null,
    category text not null,

    reference_test_weight_value numeric(20,6) null,
    reference_test_weight_unit text null,

    moisture_reference text null,
    impurities_reference text null,

    schema_version text not null,
    created_at timestamptz not null,
    created_by uuid not null
);
