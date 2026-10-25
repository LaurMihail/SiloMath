-- SiloMath V6
-- IMP-DB-005
-- Executable assertions for required row-level CHECK constraints.

DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT * FROM (VALUES
            ('users','users_lifecycle_status_check'),
            ('users','users_entity_version_check'),
            ('organizations','organizations_lifecycle_status_check'),
            ('organization_memberships','organization_memberships_role_check'),
            ('organization_memberships','organization_memberships_disabled_pair_check'),
            ('locations','locations_coordinate_pair_check'),
            ('locations','locations_latitude_range_check'),
            ('locations','locations_longitude_range_check'),
            ('storage_units','storage_units_lifecycle_status_check'),
            ('storage_spaces','storage_spaces_lifecycle_status_check'),
            ('clients','clients_lifecycle_status_check'),
            ('geometry_families','geometry_families_code_check'),
            ('geometry_profiles','geometry_profiles_lifecycle_status_check'),
            ('geometry_profile_versions','geometry_profile_versions_number_check'),
            ('geometry_profile_versions','geometry_profile_versions_reference_above_fill_check'),
            ('geometry_profile_versions','geometry_profile_versions_capacity_check'),
            ('commodities','commodities_lifecycle_status_check'),
            ('commodity_versions','commodity_versions_number_check'),
            ('commodity_versions','commodity_versions_reference_weight_pair_check'),
            ('stocktaking_records','stocktaking_records_entity_version_check'),
            ('stocktaking_revisions','stocktaking_revisions_state_check'),
            ('stocktaking_revisions','stocktaking_revisions_no_self_base_check'),
            ('stocktaking_revisions','stocktaking_revisions_validation_pair_check'),
            ('stocktaking_revisions','stocktaking_revisions_empty_semantics_check'),
            ('stocktaking_revisions','stocktaking_revisions_client_semantics_check'),
            ('stocktaking_revisions','stocktaking_revisions_validated_result_check'),
            ('measurements','measurements_sequence_check'),
            ('measurements','measurements_raw_value_check'),
            ('measurements','measurements_normalized_ullage_check'),
            ('issues','issues_category_check'),
            ('issues','issues_status_check'),
            ('issues','issues_resolution_pair_check'),
            ('issue_resolutions','issue_resolutions_context_check'),
            ('issue_status_events','issue_status_events_status_check'),
            ('evidence','evidence_owner_xor_check'),
            ('evidence','evidence_issue_revision_check'),
            ('evidence','evidence_type_check'),
            ('evidence','evidence_sync_state_check'),
            ('evidence','evidence_discriminator_check'),
            ('sync_operations','sync_operations_state_check'),
            ('sync_operations','sync_operations_acknowledgement_check'),
            ('sync_operation_dependencies','sync_dependencies_type_check'),
            ('sync_operation_dependencies','sync_dependencies_no_self_check'),
            ('sync_conflicts','sync_conflicts_status_check'),
            ('sync_conflicts','sync_conflicts_resolution_state_check'),
            ('notifications','notifications_type_check'),
            ('organization_configurations','organization_configurations_type_check'),
            ('organization_configurations','organization_configurations_typed_value_check')
        ) AS x(table_name, constraint_name)
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint c
            JOIN pg_class t ON t.oid = c.conrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE n.nspname = 'silomath'
              AND t.relname = r.table_name
              AND c.conname = r.constraint_name
              AND c.contype = 'c'
        ) THEN
            RAISE EXCEPTION 'DB005 missing CHECK %.%', r.table_name, r.constraint_name;
        END IF;
    END LOOP;
END;
$$;

DO $$
BEGIN
    RAISE NOTICE 'V6 DB005 CHECK CONSTRAINT ASSERTIONS PASSED';
END;
$$;
