-- SiloMath V6 PostgreSQL tenant-isolation assertions.
-- IMP-DB-002
--
-- Verifies the physical tenant-key model only.
-- Authorization policy, FK/composite-FK enforcement, uniqueness, CHECKs and
-- indexes remain in their dedicated tasks.

do $$
declare
    v record;
begin
    --------------------------------------------------------------------
    -- DIRECT TENANT KEYS
    --------------------------------------------------------------------
    -- These aggregate roots/projections are Organization-owned directly and
    -- therefore require a non-null UUID organization_id.

    for v in
        select *
        from (
            values
                ('organization_memberships'),
                ('locations'),
                ('storage_units'),
                ('storage_spaces'),
                ('clients'),
                ('geometry_profiles'),
                ('inventory_campaigns'),
                ('campaign_snapshots'),
                ('offline_working_set_manifests'),
                ('stocktaking_records'),
                ('issues'),
                ('evidence'),
                ('final_inventory_snapshots'),
                ('audit_events'),
                ('notifications'),
                ('organization_configurations')
        ) as required(table_name)
    loop
        if to_regclass('silomath.' || v.table_name) is null then
            raise exception
                'ASSERTION FAILED: tenant-owned table silomath.% does not exist',
                v.table_name;
        end if;

        if not exists (
            select 1
            from information_schema.columns c
            where c.table_schema = 'silomath'
              and c.table_name = v.table_name
              and c.column_name = 'organization_id'
              and c.data_type = 'uuid'
              and c.is_nullable = 'NO'
        ) then
            raise exception
                'ASSERTION FAILED: silomath.% requires non-null UUID organization_id tenant key',
                v.table_name;
        end if;
    end loop;

    --------------------------------------------------------------------
    -- IMMUTABLE PARENT-CHAIN TENANT LINEAGE
    --------------------------------------------------------------------
    -- Doc10 permits child ownership through a provably immutable parent chain.
    -- DB002 requires the lineage key itself to be present and non-null.
    -- Referential/composite-FK enforcement is deliberately deferred to DB003.

    for v in
        select *
        from (
            values
                ('geometry_profile_versions', 'geometry_profile_id'),
                ('campaign_scopes', 'campaign_id'),
                ('campaign_team_assignments', 'campaign_id'),
                ('campaign_snapshot_scope_items', 'campaign_snapshot_id'),
                ('campaign_snapshot_team_items', 'campaign_snapshot_id'),
                ('campaign_snapshot_commodity_items', 'campaign_snapshot_id'),
                ('stocktaking_revisions', 'stocktaking_record_id'),
                ('measurements', 'stocktaking_revision_id'),
                ('calculation_results', 'stocktaking_revision_id'),
                ('issue_resolutions', 'issue_id'),
                ('issue_status_events', 'issue_id'),
                ('final_inventory_snapshot_entries', 'final_snapshot_id')
        ) as required(table_name, parent_key)
    loop
        if to_regclass('silomath.' || v.table_name) is null then
            raise exception
                'ASSERTION FAILED: tenant-lineage child table silomath.% does not exist',
                v.table_name;
        end if;

        if not exists (
            select 1
            from information_schema.columns c
            where c.table_schema = 'silomath'
              and c.table_name = v.table_name
              and c.column_name = v.parent_key
              and c.data_type = 'uuid'
              and c.is_nullable = 'NO'
        ) then
            raise exception
                'ASSERTION FAILED: silomath.%.% must be a non-null UUID tenant-lineage parent key',
                v.table_name,
                v.parent_key;
        end if;
    end loop;

    raise notice 'V6 DB002 TENANT ISOLATION KEY ASSERTIONS PASSED';
end;
$$;
