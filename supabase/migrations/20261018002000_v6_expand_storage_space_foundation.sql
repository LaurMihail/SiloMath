-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical StorageSpace foundation.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--
-- Additive only:
--   * no legacy public.* table is dropped or rewritten;
--   * no legacy data is fabricated;
--   * tenant/RLS hardening remains IMP-DB-002;
--   * FK completion remains IMP-DB-003;
--   * unique/check/index hardening remains IMP-DB-004..006;
--   * max capacity is versioned geometry data and is not persisted here.

create schema if not exists silomath;

create table if not exists silomath.storage_spaces (
    storage_space_id uuid primary key,
    organization_id uuid not null,
    storage_unit_id uuid not null,

    code text not null,
    description text null,

    geometry_profile_id uuid not null,
    lifecycle_status text not null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1
);
