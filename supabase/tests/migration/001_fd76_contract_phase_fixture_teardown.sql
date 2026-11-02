-- FD-76 / CLEAN V6 native replay support.
-- TEST-ONLY fixture transition from EXPAND/MIGRATE to CONTRACT.
--
-- Historical preservation is asserted before this file runs.
-- Production is never modified by this fixture.
-- TRUNCATE ... CASCADE is acceptable here because this database is an isolated,
-- disposable synthetic PostgreSQL cluster owned by the native test runner.

begin;

truncate table
    public.calculation_results,
    public.issues,
    public.measurements,
    public.inventory_snapshot_items,
    public.inventory_snapshots,
    public.stocktaking_records,
    public.campaign_assignments,
    public.campaign_scope,
    public.campaign_launch_snapshot_items,
    public.campaign_launch_team_snapshot,
    public.campaign_launch_snapshots,
    public.campaigns,
    public.clients,
    public.commodities,
    public.storage_spaces,
    public.geometry_versions,
    public.geometry_profiles,
    public.geometry_families,
    public.storage_units,
    public.locations,
    public.organization_members,
    public.organizations,
    public.profiles
restart identity cascade;

commit;
