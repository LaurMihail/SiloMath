-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical PostgreSQL foundation tables.
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
--   * full unique/check/index hardening remains IMP-DB-004..006.

create schema if not exists silomath;

create table if not exists silomath.users (
    user_id uuid primary key,
    full_name text not null,
    email text not null,
    email_normalized text not null,
    lifecycle_status text not null,

    created_at timestamptz not null,
    created_by uuid null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1
);

create table if not exists silomath.organizations (
    organization_id uuid primary key,
    name text not null,
    code text not null,

    address text null,
    city text null,
    country text null,

    lifecycle_status text not null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1
);

create table if not exists silomath.organization_memberships (
    membership_id uuid primary key,
    organization_id uuid not null,
    user_id uuid not null,

    role text not null,
    lifecycle_status text not null,

    disabled_at timestamptz null,
    disabled_by uuid null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1
);

create table if not exists silomath.device_installations (
    device_installation_id uuid primary key,
    user_id uuid null,

    created_at timestamptz not null,
    last_authorized_at timestamptz null,
    revoked_at timestamptz null
);

create table if not exists silomath.locations (
    location_id uuid primary key,
    organization_id uuid not null,

    name text not null,
    code text not null,

    address text null,
    city text null,
    country text null,
    notes text null,

    latitude_deg numeric(9,6) null,
    longitude_deg numeric(10,6) null,

    lifecycle_status text not null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1
);

create table if not exists silomath.clients (
    client_id uuid primary key,
    organization_id uuid not null,

    name text not null,
    code text not null,
    tax_id text null,
    notes text null,

    lifecycle_status text not null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1
);
