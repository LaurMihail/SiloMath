-- SiloMath V6
-- IMP-DB-003 PostgreSQL PK/FK regression assertions.

do $$
declare
    r record;
begin
    if exists (
        select t.table_name
        from information_schema.tables t
        where t.table_schema = 'silomath'
          and t.table_type = 'BASE TABLE'
          and not exists (
              select 1
              from information_schema.table_constraints c
              where c.table_schema = t.table_schema
                and c.table_name = t.table_name
                and c.constraint_type = 'PRIMARY KEY'
          )
    ) then
        raise exception
            'ASSERTION FAILED: at least one silomath base table has no primary key';
    end if;

    for r in
        select *
        from (
            values
                ('organization_memberships', 'organization_id', 'organizations', 'organization_id'),
                ('organization_memberships', 'user_id', 'users', 'user_id'),
                ('device_installations', 'user_id', 'users', 'user_id'),
                ('locations', 'organization_id', 'organizations', 'organization_id'),
                ('storage_units', 'organization_id', 'organizations', 'organization_id'),
                ('storage_units', 'location_id', 'locations', 'location_id'),
                ('storage_spaces', 'organization_id', 'organizations', 'organization_id'),
                ('storage_spaces', 'storage_unit_id', 'storage_units', 'storage_unit_id'),
                ('storage_spaces', 'geometry_profile_id', 'geometry_profiles', 'geometry_profile_id'),
                ('clients', 'organization_id', 'organizations', 'organization_id'),
                ('geometry_profiles', 'organization_id', 'organizations', 'organization_id'),
                ('geometry_profiles', 'geometry_family_id', 'geometry_families', 'geometry_family_id'),
                ('geometry_profiles', 'current_version_id', 'geometry_profile_versions', 'geometry_profile_version_id'),
                ('geometry_profile_versions', 'geometry_profile_id', 'geometry_profiles', 'geometry_profile_id'),
                ('geometry_profile_versions', 'geometry_family_id', 'geometry_families', 'geometry_family_id'),
                ('commodities', 'current_version_id', 'commodity_versions', 'commodity_version_id'),
                ('commodity_versions', 'commodity_id', 'commodities', 'commodity_id'),
                ('inventory_campaigns', 'organization_id', 'organizations', 'organization_id'),
                ('inventory_campaigns', 'campaign_snapshot_id', 'campaign_snapshots', 'campaign_snapshot_id'),
                ('campaign_scopes', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('campaign_scopes', 'storage_space_id', 'storage_spaces', 'storage_space_id'),
                ('campaign_team_assignments', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('campaign_team_assignments', 'user_id', 'users', 'user_id'),
                ('campaign_snapshots', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('campaign_snapshots', 'organization_id', 'organizations', 'organization_id'),
                ('campaign_snapshot_scope_items', 'campaign_snapshot_id', 'campaign_snapshots', 'campaign_snapshot_id'),
                ('campaign_snapshot_scope_items', 'location_id', 'locations', 'location_id'),
                ('campaign_snapshot_scope_items', 'storage_unit_id', 'storage_units', 'storage_unit_id'),
                ('campaign_snapshot_scope_items', 'storage_space_id', 'storage_spaces', 'storage_space_id'),
                ('campaign_snapshot_scope_items', 'geometry_family_id', 'geometry_families', 'geometry_family_id'),
                ('campaign_snapshot_scope_items', 'geometry_profile_id', 'geometry_profiles', 'geometry_profile_id'),
                ('campaign_snapshot_scope_items', 'geometry_profile_version_id', 'geometry_profile_versions', 'geometry_profile_version_id'),
                ('campaign_snapshot_team_items', 'campaign_snapshot_id', 'campaign_snapshots', 'campaign_snapshot_id'),
                ('campaign_snapshot_team_items', 'user_id', 'users', 'user_id'),
                ('campaign_snapshot_team_items', 'organization_membership_id', 'organization_memberships', 'membership_id'),
                ('campaign_snapshot_commodity_items', 'campaign_snapshot_id', 'campaign_snapshots', 'campaign_snapshot_id'),
                ('campaign_snapshot_commodity_items', 'commodity_id', 'commodities', 'commodity_id'),
                ('campaign_snapshot_commodity_items', 'commodity_version_id', 'commodity_versions', 'commodity_version_id'),
                ('offline_working_set_manifests', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('offline_working_set_manifests', 'organization_id', 'organizations', 'organization_id'),
                ('offline_working_set_manifests', 'device_installation_id', 'device_installations', 'device_installation_id'),
                ('stocktaking_records', 'organization_id', 'organizations', 'organization_id'),
                ('stocktaking_records', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('stocktaking_records', 'storage_space_id', 'storage_spaces', 'storage_space_id'),
                ('stocktaking_records', 'current_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id'),
                ('stocktaking_revisions', 'stocktaking_record_id', 'stocktaking_records', 'stocktaking_record_id'),
                ('stocktaking_revisions', 'based_on_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id'),
                ('stocktaking_revisions', 'commodity_version_id', 'commodity_versions', 'commodity_version_id'),
                ('stocktaking_revisions', 'client_id', 'clients', 'client_id'),
                ('stocktaking_revisions', 'calculation_result_id', 'calculation_results', 'calculation_result_id'),
                ('measurements', 'stocktaking_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id'),
                ('measurements', 'geometry_profile_version_id', 'geometry_profile_versions', 'geometry_profile_version_id'),
                ('calculation_results', 'stocktaking_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id'),
                ('calculation_results', 'geometry_family_id', 'geometry_families', 'geometry_family_id'),
                ('calculation_results', 'geometry_profile_version_id', 'geometry_profile_versions', 'geometry_profile_version_id'),
                ('issues', 'organization_id', 'organizations', 'organization_id'),
                ('issues', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('issues', 'stocktaking_record_id', 'stocktaking_records', 'stocktaking_record_id'),
                ('issue_resolutions', 'issue_id', 'issues', 'issue_id'),
                ('issue_resolutions', 'audit_event_id', 'audit_events', 'audit_event_id'),
                ('issue_status_events', 'issue_id', 'issues', 'issue_id'),
                ('issue_status_events', 'audit_event_id', 'audit_events', 'audit_event_id'),
                ('evidence', 'organization_id', 'organizations', 'organization_id'),
                ('evidence', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('evidence', 'stocktaking_record_id', 'stocktaking_records', 'stocktaking_record_id'),
                ('evidence', 'issue_id', 'issues', 'issue_id'),
                ('evidence', 'stocktaking_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id'),
                ('final_inventory_snapshots', 'organization_id', 'organizations', 'organization_id'),
                ('final_inventory_snapshots', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('final_inventory_snapshots', 'campaign_snapshot_id', 'campaign_snapshots', 'campaign_snapshot_id'),
                ('final_inventory_snapshot_entries', 'final_snapshot_id', 'final_inventory_snapshots', 'final_snapshot_id'),
                ('final_inventory_snapshot_entries', 'storage_space_id', 'storage_spaces', 'storage_space_id'),
                ('audit_events', 'organization_id', 'organizations', 'organization_id'),
                ('audit_events', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('notifications', 'organization_id', 'organizations', 'organization_id'),
                ('notifications', 'recipient_user_id', 'users', 'user_id'),
                ('user_preferences', 'user_id', 'users', 'user_id'),
                ('organization_configurations', 'organization_id', 'organizations', 'organization_id'),
                ('sync_operations', 'organization_id', 'organizations', 'organization_id'),
                ('sync_operations', 'campaign_id', 'inventory_campaigns', 'campaign_id'),
                ('sync_operation_dependencies', 'parent_operation_id', 'sync_operations', 'sync_operation_id'),
                ('sync_operation_dependencies', 'dependent_operation_id', 'sync_operations', 'sync_operation_id'),
                ('sync_conflicts', 'organization_id', 'organizations', 'organization_id'),
                ('sync_conflicts', 'sync_operation_id', 'sync_operations', 'sync_operation_id')
        ) as x(table_name, column_name, referenced_table, referenced_column)
    loop
        if not exists (
            select 1
            from pg_constraint c
            join pg_attribute a
              on a.attrelid = c.conrelid
             and a.attnum = c.conkey[1]
            join pg_attribute ra
              on ra.attrelid = c.confrelid
             and ra.attnum = c.confkey[1]
            where c.contype = 'f'
              and c.conrelid = format('silomath.%I', r.table_name)::regclass
              and c.confrelid = format('silomath.%I', r.referenced_table)::regclass
              and cardinality(c.conkey) = 1
              and cardinality(c.confkey) = 1
              and a.attname = r.column_name
              and ra.attname = r.referenced_column
        ) then
            raise exception
                'ASSERTION FAILED: missing FK %.% -> %.%',
                r.table_name,
                r.column_name,
                r.referenced_table,
                r.referenced_column;
        end if;
    end loop;

    if exists (
        select 1
        from pg_constraint c
        where c.contype = 'f'
          and c.connamespace = 'silomath'::regnamespace
          and c.confdeltype in ('c', 'n', 'd')
    ) then
        raise exception
            'ASSERTION FAILED: silomath contains a CASCADE/SET NULL/SET DEFAULT foreign-key delete action';
    end if;
end;
$$;
