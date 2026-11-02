\pset pager off
\pset format unaligned
\pset tuples_only on

do $$
declare
    v_name text;
begin
    foreach v_name in array array[
        'calculation_results',
        'campaign_assignments',
        'campaign_launch_snapshot_items',
        'campaign_launch_snapshots',
        'campaign_launch_team_snapshot',
        'campaign_scope',
        'campaigns',
        'clients',
        'commodities',
        'geometry_families',
        'geometry_profiles',
        'geometry_versions',
        'inventory_snapshot_items',
        'inventory_snapshots',
        'issues',
        'locations',
        'measurements',
        'organization_members',
        'organizations',
        'profiles',
        'stocktaking_records',
        'storage_spaces',
        'storage_units'
    ]
    loop
        if to_regclass(format('public.%I', v_name)) is not null then
            raise exception 'Legacy public business table remains: %', v_name;
        end if;
    end loop;

    if exists (
        select 1
        from pg_proc p
        join pg_namespace n on n.oid = p.pronamespace
        where n.nspname = 'public'
          and p.proname in (
            'can_administer_user',
            'enforce_active_campaign_launch_snapshot',
            'enforce_campaign_scope_freeze',
            'enforce_campaign_scope_organization_consistency',
            'enforce_campaign_team_freeze',
            'enforce_launch_snapshot_child_mutation_protection',
            'enforce_max_measurements_per_record',
            'enforce_third_party_client_org_consistency',
            'enforce_validated_stocktaking_child_immutability',
            'enforce_validated_stocktaking_record_immutability',
            'is_organization_admin',
            'is_organization_member',
            'is_stocktaking_record_validated',
            'prevent_campaign_launch_snapshots_mutation',
            'prevent_campaign_org_mismatch_on_campaign_update',
            'prevent_campaign_org_mismatch_on_location_update',
            'prevent_campaign_org_mismatch_on_space_update',
            'prevent_campaign_org_mismatch_on_unit_update',
            'prevent_geometry_version_mutation',
            'prevent_inventory_snapshot_items_mutation',
            'prevent_inventory_snapshots_mutation',
            'prevent_launch_snapshot_id_mutation',
            'prevent_third_party_client_org_mismatch_on_client_update',
            'prevent_third_party_org_mismatch_on_campaign_update',
            'rls_auto_enable',
            'save_and_launch_campaign',
            'update_master_data_sync_metadata'
          )
    ) then
        raise exception 'Legacy public helper/guard functions remain';
    end if;

    if exists (select 1 from pg_event_trigger where evtname = 'ensure_rls') then
        raise exception 'Legacy ensure_rls event trigger remains';
    end if;

    if to_regclass('silomath.users') is null
       or to_regclass('silomath.organizations') is null
       or to_regclass('silomath.organization_memberships') is null
       or to_regclass('silomath.inventory_campaigns') is null then
        raise exception 'Canonical silomath foundation is incomplete after cleanup';
    end if;

    raise notice 'FD76 CLEAN V6 PUBLIC BUSINESS RUNTIME RETIREMENT ASSERTIONS PASS';
end
$$;