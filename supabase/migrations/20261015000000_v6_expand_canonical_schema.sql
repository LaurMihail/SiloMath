-- SiloMath V6 canonical PostgreSQL expansion boundary.
--
-- Governing contract:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   11_API_SPECIFICATION
--   20_PRODUCTION_OPERATIONS
--
-- Migration strategy:
--   EXPAND -> MIGRATE -> CONTRACT
--
-- This migration is deliberately additive.
-- It does not drop or rewrite legacy public.* campaign structures.
-- Legacy data migration/backfill and later contraction are separate,
-- explicitly validated migrations.

create schema if not exists silomath;

create table if not exists silomath.inventory_campaigns (
    campaign_id uuid primary key,
    organization_id uuid not null,

    code text not null,
    campaign_year integer not null,
    sequence integer not null,

    name text not null,
    inventory_type text not null,
    start_date date not null,
    planned_end_date date not null,
    status text not null,

    launched_at timestamptz not null,
    launched_by uuid not null,

    campaign_snapshot_id uuid null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1,

    constraint inventory_campaigns_name_nonblank_check
        check (btrim(name) <> ''),

    constraint inventory_campaigns_code_format_check
        check (code ~ '^INV-[0-9]{4}-[0-9]{4}$'),

    constraint inventory_campaigns_inventory_type_check
        check (inventory_type in ('ANNUAL', 'MONTHLY', 'CONTROL')),

    constraint inventory_campaigns_status_check
        check (status in ('ACTIVE', 'COMPLETED')),

    constraint inventory_campaigns_date_range_check
        check (planned_end_date >= start_date),

    constraint inventory_campaigns_year_check
        check (campaign_year >= 2000),

    constraint inventory_campaigns_sequence_check
        check (sequence >= 1),

    constraint inventory_campaigns_entity_version_check
        check (entity_version >= 1),

    constraint inventory_campaigns_org_year_sequence_unique
        unique (organization_id, campaign_year, sequence),

    constraint inventory_campaigns_org_code_unique
        unique (organization_id, code)
);

create index if not exists inventory_campaigns_org_status_idx
    on silomath.inventory_campaigns (
        organization_id,
        status
    );

create index if not exists inventory_campaigns_org_type_idx
    on silomath.inventory_campaigns (
        organization_id,
        inventory_type
    );

create table if not exists silomath.campaign_scopes (
    campaign_scope_id uuid primary key,
    campaign_id uuid not null,
    storage_space_id uuid not null,

    created_at timestamptz not null,
    created_by uuid not null,

    constraint campaign_scopes_campaign_space_unique
        unique (campaign_id, storage_space_id),

    constraint campaign_scopes_campaign_fkey
        foreign key (campaign_id)
        references silomath.inventory_campaigns(campaign_id)
        on delete restrict
);

create index if not exists campaign_scopes_campaign_idx
    on silomath.campaign_scopes(campaign_id);

create table if not exists silomath.campaign_team_assignments (
    assignment_id uuid primary key,
    campaign_id uuid not null,
    user_id uuid not null,

    status text not null,

    assigned_at timestamptz not null,
    assigned_by uuid not null,

    removed_at timestamptz null,
    removed_by uuid null,

    created_at timestamptz not null,
    created_by uuid not null,
    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null default 1,

    constraint campaign_team_assignments_status_check
        check (status in ('ACTIVE', 'REMOVED')),

    constraint campaign_team_assignments_entity_version_check
        check (entity_version >= 1),

    constraint campaign_team_assignments_removed_pair_check
        check (
            (
                status = 'ACTIVE'
                and removed_at is null
                and removed_by is null
            )
            or
            (
                status = 'REMOVED'
                and removed_at is not null
                and removed_by is not null
            )
        ),

    constraint campaign_team_assignments_campaign_fkey
        foreign key (campaign_id)
        references silomath.inventory_campaigns(campaign_id)
        on delete restrict
);

create unique index if not exists campaign_team_assignments_one_active_user_idx
    on silomath.campaign_team_assignments (
        campaign_id,
        user_id
    )
    where status = 'ACTIVE';

create index if not exists campaign_team_assignments_campaign_idx
    on silomath.campaign_team_assignments(campaign_id);

-- Campaign Snapshot is introduced as canonical V6 storage here.
-- Child frozen-context tables are expanded separately once exact
-- existing legacy-to-canonical mappings are migration-tested.

create table if not exists silomath.campaign_snapshots (
    campaign_snapshot_id uuid primary key,
    campaign_id uuid not null unique,
    organization_id uuid not null,

    campaign_code text not null,
    campaign_name text not null,
    inventory_type text not null,
    start_date date not null,
    planned_end_date date not null,

    schema_version text not null,
    calculation_engine_version text not null,

    created_at timestamptz not null,
    created_by uuid not null,

    constraint campaign_snapshots_inventory_type_check
        check (inventory_type in ('ANNUAL', 'MONTHLY', 'CONTROL')),

    constraint campaign_snapshots_campaign_fkey
        foreign key (campaign_id)
        references silomath.inventory_campaigns(campaign_id)
        on delete restrict
);

alter table silomath.inventory_campaigns
    add constraint inventory_campaigns_snapshot_fkey
    foreign key (campaign_snapshot_id)
    references silomath.campaign_snapshots(campaign_snapshot_id)
    on delete restrict;
