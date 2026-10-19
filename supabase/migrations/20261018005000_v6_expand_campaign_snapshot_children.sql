-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical CampaignSnapshot child collections.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   11_API_SPECIFICATION
--
-- Additive only:
--   * CampaignSnapshot remains immutable launch-time context;
--   * scope/team collections are explicit child rows, not opaque business JSON;
--   * later mutable master/team changes must not rewrite snapshot rows;
--   * detailed frozen display/hierarchy/profile/version payload expansion remains
--     a separate canonical mapping step where the baseline does not prescribe
--     individual physical column names;
--   * FK completion remains IMP-DB-003;
--   * unique/check/index hardening remains IMP-DB-004..006.

create schema if not exists silomath;

create table if not exists silomath.campaign_snapshot_scope_items (
    snapshot_scope_item_id uuid primary key,
    campaign_snapshot_id uuid not null,

    storage_space_id uuid not null,
    geometry_profile_id uuid not null,
    geometry_profile_version_id uuid not null
);

create table if not exists silomath.campaign_snapshot_team_items (
    snapshot_team_item_id uuid primary key,
    campaign_snapshot_id uuid not null,

    user_id uuid not null
);
