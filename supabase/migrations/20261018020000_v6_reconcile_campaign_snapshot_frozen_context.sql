-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical CampaignSnapshot frozen-context reconciliation.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   11_API_SPECIFICATION
--
-- Additive only:
--   * CampaignSnapshot remains immutable launch-time context;
--   * organization and campaign configuration must remain historically readable;
--   * scope items preserve frozen hierarchy and geometry/profile/version context;
--   * team items preserve frozen membership/display/role context;
--   * calculation compatibility is represented as the required version set;
--   * existing schema_version / calculation_engine_version columns are retained
--     for rollout compatibility and are not reinterpreted by this migration;
--   * Commodity compatibility physical mapping remains open because Doc09
--     defines the semantic collection while Doc10 does not name its physical
--     child structure;
--   * FK completion remains IMP-DB-003;
--   * unique constraints remain IMP-DB-004;
--   * value-domain/CHECK hardening remains IMP-DB-005;
--   * indexes remain IMP-DB-006;
--   * immutable enforcement remains IMP-DB-008.
--
-- This migration does not close the full IMP-DB-001 task.

alter table silomath.campaign_snapshots
    add column if not exists organization_name text null,
    add column if not exists organization_code text null,
    add column if not exists snapshot_schema_version text null,
    add column if not exists calculation_schema_versions jsonb null;

alter table silomath.campaign_snapshot_scope_items
    add column if not exists location_id uuid null,
    add column if not exists location_code text null,
    add column if not exists location_name text null,

    add column if not exists storage_unit_id uuid null,
    add column if not exists storage_unit_code text null,
    add column if not exists storage_unit_name text null,

    add column if not exists storage_space_code text null,

    add column if not exists geometry_family_id uuid null,
    add column if not exists geometry_family_code text null,
    add column if not exists geometry_family_name text null,

    add column if not exists geometry_profile_code text null,
    add column if not exists geometry_profile_name text null,

    add column if not exists geometry_formula_version text null,
    add column if not exists geometry_dimensions jsonb null,
    add column if not exists measurement_reference_height_m numeric(20,6) null,
    add column if not exists maximum_fill_height_m numeric(20,6) null,
    add column if not exists max_capacity_m3 numeric(20,6) null;

alter table silomath.campaign_snapshot_team_items
    add column if not exists organization_membership_id uuid null,
    add column if not exists role_at_assignment text null,
    add column if not exists user_full_name text null,
    add column if not exists user_email text null;
