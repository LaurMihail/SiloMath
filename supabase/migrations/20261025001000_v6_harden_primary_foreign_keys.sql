-- SiloMath V6
-- IMP-DB-003
-- PostgreSQL primary/foreign-key hardening.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   17_SECURITY_AUDIT_PRIVACY
--
-- Strategy:
--   * canonical UUID primary keys already exist on foundation tables;
--   * add missing canonical single-column FKs with RESTRICT semantics;
--   * add as NOT VALID so retained legacy-derived rows are not destroyed or
--     fabricated merely to satisfy canonical referential history;
--   * PostgreSQL still enforces each NOT VALID FK for all new/updated rows;
--   * uniqueness/CHECK/index hardening remains IMP-DB-004..006.

do $$
declare
    r record;
begin
    for r in
        select *
        from (
            values
        ('organization_memberships', 'organization_id', 'organizations', 'organization_id', 'fk_organization_memberships_organization_id'),
        ('organization_memberships', 'user_id', 'users', 'user_id', 'fk_organization_memberships_user_id'),
        ('device_installations', 'user_id', 'users', 'user_id', 'fk_device_installations_user_id'),
        ('locations', 'organization_id', 'organizations', 'organization_id', 'fk_locations_organization_id'),
        ('storage_units', 'organization_id', 'organizations', 'organization_id', 'fk_storage_units_organization_id'),
        ('storage_units', 'location_id', 'locations', 'location_id', 'fk_storage_units_location_id'),
        ('storage_spaces', 'organization_id', 'organizations', 'organization_id', 'fk_storage_spaces_organization_id'),
        ('storage_spaces', 'storage_unit_id', 'storage_units', 'storage_unit_id', 'fk_storage_spaces_storage_unit_id'),
        ('storage_spaces', 'geometry_profile_id', 'geometry_profiles', 'geometry_profile_id', 'fk_storage_spaces_geometry_profile_id'),
        ('clients', 'organization_id', 'organizations', 'organization_id', 'fk_clients_organization_id'),
        ('geometry_profiles', 'organization_id', 'organizations', 'organization_id', 'fk_geometry_profiles_organization_id'),
        ('geometry_profiles', 'geometry_family_id', 'geometry_families', 'geometry_family_id', 'fk_geometry_profiles_geometry_family_id'),
        ('geometry_profiles', 'current_version_id', 'geometry_profile_versions', 'geometry_profile_version_id', 'fk_geometry_profiles_current_version_id'),
        ('geometry_profile_versions', 'geometry_profile_id', 'geometry_profiles', 'geometry_profile_id', 'fk_geometry_profile_versions_geometry_profile_id'),
        ('geometry_profile_versions', 'geometry_family_id', 'geometry_families', 'geometry_family_id', 'fk_geometry_profile_versions_geometry_family_id'),
        ('commodities', 'current_version_id', 'commodity_versions', 'commodity_version_id', 'fk_commodities_current_version_id'),
        ('commodity_versions', 'commodity_id', 'commodities', 'commodity_id', 'fk_commodity_versions_commodity_id'),
        ('inventory_campaigns', 'organization_id', 'organizations', 'organization_id', 'fk_inventory_campaigns_organization_id'),
        ('campaign_scopes', 'storage_space_id', 'storage_spaces', 'storage_space_id', 'fk_campaign_scopes_storage_space_id'),
        ('campaign_team_assignments', 'user_id', 'users', 'user_id', 'fk_campaign_team_assignments_user_id'),
        ('campaign_snapshots', 'organization_id', 'organizations', 'organization_id', 'fk_campaign_snapshots_organization_id'),
        ('campaign_snapshot_scope_items', 'campaign_snapshot_id', 'campaign_snapshots', 'campaign_snapshot_id', 'fk_campaign_snapshot_scope_items_campaign_snapshot_id'),
        ('campaign_snapshot_scope_items', 'location_id', 'locations', 'location_id', 'fk_campaign_snapshot_scope_items_location_id'),
        ('campaign_snapshot_scope_items', 'storage_unit_id', 'storage_units', 'storage_unit_id', 'fk_campaign_snapshot_scope_items_storage_unit_id'),
        ('campaign_snapshot_scope_items', 'storage_space_id', 'storage_spaces', 'storage_space_id', 'fk_campaign_snapshot_scope_items_storage_space_id'),
        ('campaign_snapshot_scope_items', 'geometry_family_id', 'geometry_families', 'geometry_family_id', 'fk_campaign_snapshot_scope_items_geometry_family_id'),
        ('campaign_snapshot_scope_items', 'geometry_profile_id', 'geometry_profiles', 'geometry_profile_id', 'fk_campaign_snapshot_scope_items_geometry_profile_id'),
        ('campaign_snapshot_scope_items', 'geometry_profile_version_id', 'geometry_profile_versions', 'geometry_profile_version_id', 'fk_campaign_snapshot_scope_items_geometry_profile_version_id'),
        ('campaign_snapshot_team_items', 'campaign_snapshot_id', 'campaign_snapshots', 'campaign_snapshot_id', 'fk_campaign_snapshot_team_items_campaign_snapshot_id'),
        ('campaign_snapshot_team_items', 'user_id', 'users', 'user_id', 'fk_campaign_snapshot_team_items_user_id'),
        ('campaign_snapshot_team_items', 'organization_membership_id', 'organization_memberships', 'membership_id', 'fk_campaign_snapshot_team_items_organization_membership_id'),
        ('campaign_snapshot_commodity_items', 'campaign_snapshot_id', 'campaign_snapshots', 'campaign_snapshot_id', 'fk_campaign_snapshot_commodity_items_campaign_snapshot_id'),
        ('campaign_snapshot_commodity_items', 'commodity_id', 'commodities', 'commodity_id', 'fk_campaign_snapshot_commodity_items_commodity_id'),
        ('campaign_snapshot_commodity_items', 'commodity_version_id', 'commodity_versions', 'commodity_version_id', 'fk_campaign_snapshot_commodity_items_commodity_version_id'),
        ('offline_working_set_manifests', 'campaign_id', 'inventory_campaigns', 'campaign_id', 'fk_offline_working_set_manifests_campaign_id'),
        ('offline_working_set_manifests', 'organization_id', 'organizations', 'organization_id', 'fk_offline_working_set_manifests_organization_id'),
        ('offline_working_set_manifests', 'device_installation_id', 'device_installations', 'device_installation_id', 'fk_offline_working_set_manifests_device_installation_id'),
        ('stocktaking_records', 'organization_id', 'organizations', 'organization_id', 'fk_stocktaking_records_organization_id'),
        ('stocktaking_records', 'campaign_id', 'inventory_campaigns', 'campaign_id', 'fk_stocktaking_records_campaign_id'),
        ('stocktaking_records', 'storage_space_id', 'storage_spaces', 'storage_space_id', 'fk_stocktaking_records_storage_space_id'),
        ('stocktaking_records', 'current_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id', 'fk_stocktaking_records_current_revision_id'),
        ('stocktaking_revisions', 'stocktaking_record_id', 'stocktaking_records', 'stocktaking_record_id', 'fk_stocktaking_revisions_stocktaking_record_id'),
        ('stocktaking_revisions', 'based_on_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id', 'fk_stocktaking_revisions_based_on_revision_id'),
        ('stocktaking_revisions', 'commodity_version_id', 'commodity_versions', 'commodity_version_id', 'fk_stocktaking_revisions_commodity_version_id'),
        ('stocktaking_revisions', 'client_id', 'clients', 'client_id', 'fk_stocktaking_revisions_client_id'),
        ('stocktaking_revisions', 'calculation_result_id', 'calculation_results', 'calculation_result_id', 'fk_stocktaking_revisions_calculation_result_id'),
        ('measurements', 'stocktaking_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id', 'fk_measurements_stocktaking_revision_id'),
        ('measurements', 'geometry_profile_version_id', 'geometry_profile_versions', 'geometry_profile_version_id', 'fk_measurements_geometry_profile_version_id'),
        ('calculation_results', 'stocktaking_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id', 'fk_calculation_results_stocktaking_revision_id'),
        ('calculation_results', 'geometry_family_id', 'geometry_families', 'geometry_family_id', 'fk_calculation_results_geometry_family_id'),
        ('calculation_results', 'geometry_profile_version_id', 'geometry_profile_versions', 'geometry_profile_version_id', 'fk_calculation_results_geometry_profile_version_id'),
        ('issues', 'organization_id', 'organizations', 'organization_id', 'fk_issues_organization_id'),
        ('issues', 'campaign_id', 'inventory_campaigns', 'campaign_id', 'fk_issues_campaign_id'),
        ('issues', 'stocktaking_record_id', 'stocktaking_records', 'stocktaking_record_id', 'fk_issues_stocktaking_record_id'),
        ('issue_resolutions', 'issue_id', 'issues', 'issue_id', 'fk_issue_resolutions_issue_id'),
        ('issue_resolutions', 'audit_event_id', 'audit_events', 'audit_event_id', 'fk_issue_resolutions_audit_event_id'),
        ('issue_status_events', 'issue_id', 'issues', 'issue_id', 'fk_issue_status_events_issue_id'),
        ('issue_status_events', 'audit_event_id', 'audit_events', 'audit_event_id', 'fk_issue_status_events_audit_event_id'),
        ('evidence', 'organization_id', 'organizations', 'organization_id', 'fk_evidence_organization_id'),
        ('evidence', 'campaign_id', 'inventory_campaigns', 'campaign_id', 'fk_evidence_campaign_id'),
        ('evidence', 'stocktaking_record_id', 'stocktaking_records', 'stocktaking_record_id', 'fk_evidence_stocktaking_record_id'),
        ('evidence', 'issue_id', 'issues', 'issue_id', 'fk_evidence_issue_id'),
        ('evidence', 'stocktaking_revision_id', 'stocktaking_revisions', 'stocktaking_revision_id', 'fk_evidence_stocktaking_revision_id'),
        ('final_inventory_snapshots', 'organization_id', 'organizations', 'organization_id', 'fk_final_inventory_snapshots_organization_id'),
        ('final_inventory_snapshots', 'campaign_id', 'inventory_campaigns', 'campaign_id', 'fk_final_inventory_snapshots_campaign_id'),
        ('final_inventory_snapshots', 'campaign_snapshot_id', 'campaign_snapshots', 'campaign_snapshot_id', 'fk_final_inventory_snapshots_campaign_snapshot_id'),
        ('final_inventory_snapshot_entries', 'final_snapshot_id', 'final_inventory_snapshots', 'final_snapshot_id', 'fk_final_inventory_snapshot_entries_final_snapshot_id'),
        ('final_inventory_snapshot_entries', 'storage_space_id', 'storage_spaces', 'storage_space_id', 'fk_final_inventory_snapshot_entries_storage_space_id'),
        ('audit_events', 'organization_id', 'organizations', 'organization_id', 'fk_audit_events_organization_id'),
        ('audit_events', 'campaign_id', 'inventory_campaigns', 'campaign_id', 'fk_audit_events_campaign_id'),
        ('notifications', 'organization_id', 'organizations', 'organization_id', 'fk_notifications_organization_id'),
        ('notifications', 'recipient_user_id', 'users', 'user_id', 'fk_notifications_recipient_user_id'),
        ('user_preferences', 'user_id', 'users', 'user_id', 'fk_user_preferences_user_id'),
        ('organization_configurations', 'organization_id', 'organizations', 'organization_id', 'fk_organization_configurations_organization_id'),
        ('sync_operations', 'organization_id', 'organizations', 'organization_id', 'fk_sync_operations_organization_id'),
        ('sync_operations', 'campaign_id', 'inventory_campaigns', 'campaign_id', 'fk_sync_operations_campaign_id'),
        ('sync_operation_dependencies', 'parent_operation_id', 'sync_operations', 'sync_operation_id', 'fk_sync_operation_dependencies_parent_operation_id'),
        ('sync_operation_dependencies', 'dependent_operation_id', 'sync_operations', 'sync_operation_id', 'fk_sync_operation_dependencies_dependent_operation_id'),
        ('sync_conflicts', 'organization_id', 'organizations', 'organization_id', 'fk_sync_conflicts_organization_id'),
        ('sync_conflicts', 'sync_operation_id', 'sync_operations', 'sync_operation_id', 'fk_sync_conflicts_sync_operation_id')
        ) as x(table_name, column_name, referenced_table, referenced_column, constraint_name)
    loop
        execute format(
            'alter table silomath.%I add constraint %I foreign key (%I) references silomath.%I(%I) on delete restrict not valid',
            r.table_name,
            r.constraint_name,
            r.column_name,
            r.referenced_table,
            r.referenced_column
        );
    end loop;
end;
$$;
