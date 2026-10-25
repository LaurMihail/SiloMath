-- SiloMath V6
-- IMP-DB-005
-- PostgreSQL row-level CHECK hardening.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   12_OFFLINE_SYNC_SPECIFICATION
--   13_CALCULATION_ENGINE_SPECIFICATION
--   15_EVIDENCE_STORAGE_SPECIFICATION
--
-- Cross-row/cross-table invariants remain transactional/trigger/domain concerns;
-- this migration intentionally models only invariants valid as row CHECKs.

alter table silomath.users
    add constraint users_lifecycle_status_check check (lifecycle_status in ('ACTIVE', 'DISABLED')),
    add constraint users_text_check check (btrim(full_name) <> '' and btrim(email) <> '' and btrim(email_normalized) <> ''),
    add constraint users_entity_version_check check (entity_version >= 1);

alter table silomath.organizations
    add constraint organizations_lifecycle_status_check check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
    add constraint organizations_text_check check (btrim(name) <> '' and btrim(code) <> ''),
    add constraint organizations_entity_version_check check (entity_version >= 1);

alter table silomath.organization_memberships
    add constraint organization_memberships_role_check check (role in ('ADMIN', 'STANDARD')),
    add constraint organization_memberships_lifecycle_status_check check (lifecycle_status in ('ACTIVE', 'DISABLED')),
    add constraint organization_memberships_disabled_pair_check check (
        (lifecycle_status = 'ACTIVE' and disabled_at is null and disabled_by is null)
        or (lifecycle_status = 'DISABLED' and disabled_at is not null and disabled_by is not null)
    ),
    add constraint organization_memberships_entity_version_check check (entity_version >= 1);

alter table silomath.locations
    add constraint locations_lifecycle_status_check check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
    add constraint locations_text_check check (btrim(name) <> '' and btrim(code) <> ''),
    add constraint locations_coordinate_pair_check check ((latitude_deg is null) = (longitude_deg is null)),
    add constraint locations_latitude_range_check check (latitude_deg is null or latitude_deg between -90 and 90),
    add constraint locations_longitude_range_check check (longitude_deg is null or longitude_deg between -180 and 180),
    add constraint locations_entity_version_check check (entity_version >= 1);

alter table silomath.storage_units
    add constraint storage_units_lifecycle_status_check check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
    add constraint storage_units_text_check check (btrim(name) <> '' and btrim(code) <> ''),
    add constraint storage_units_entity_version_check check (entity_version >= 1);

alter table silomath.storage_spaces
    add constraint storage_spaces_lifecycle_status_check check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
    add constraint storage_spaces_code_check check (btrim(code) <> ''),
    add constraint storage_spaces_entity_version_check check (entity_version >= 1);

alter table silomath.clients
    add constraint clients_lifecycle_status_check check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
    add constraint clients_text_check check (btrim(name) <> '' and btrim(code) <> ''),
    add constraint clients_entity_version_check check (entity_version >= 1);

alter table silomath.geometry_families
    add constraint geometry_families_code_check check (family_code in ('F01','F02','F03','F04','F05','F06','F07','F08','F09','F10')),
    add constraint geometry_families_text_check check (btrim(name) <> '' and btrim(formula_version) <> '' and btrim(parameter_contract) <> '');

alter table silomath.geometry_profiles
    add constraint geometry_profiles_lifecycle_status_check check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
    add constraint geometry_profiles_text_check check (btrim(name) <> '' and btrim(code) <> ''),
    add constraint geometry_profiles_entity_version_check check (entity_version >= 1);

alter table silomath.geometry_profile_versions
    add constraint geometry_profile_versions_number_check check (version_number >= 1),
    add constraint geometry_profile_versions_reference_height_check check (measurement_reference_height_m > 0),
    add constraint geometry_profile_versions_max_fill_height_check check (maximum_fill_height_m > 0),
    add constraint geometry_profile_versions_reference_above_fill_check check (measurement_reference_height_m >= maximum_fill_height_m),
    add constraint geometry_profile_versions_capacity_check check (max_capacity_m3 > 0),
    add constraint geometry_profile_versions_text_check check (btrim(geometry_origin) <> '' and btrim(formula_version) <> '' and btrim(schema_version) <> '');

alter table silomath.commodities
    add constraint commodities_lifecycle_status_check check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
    add constraint commodities_code_check check (btrim(code) <> '');

alter table silomath.commodity_versions
    add constraint commodity_versions_number_check check (version_number >= 1),
    add constraint commodity_versions_text_check check (btrim(display_name) <> '' and btrim(category) <> '' and btrim(schema_version) <> ''),
    add constraint commodity_versions_reference_weight_pair_check check (
        (reference_test_weight_value is null and reference_test_weight_unit is null)
        or (reference_test_weight_value > 0 and reference_test_weight_unit is not null and btrim(reference_test_weight_unit) <> '')
    );

alter table silomath.stocktaking_records
    add constraint stocktaking_records_entity_version_check check (entity_version >= 1);

alter table silomath.stocktaking_revisions
    add constraint stocktaking_revisions_number_check check (revision_number >= 1),
    add constraint stocktaking_revisions_state_check check (revision_state in ('WORKING', 'VALIDATED')),
    add constraint stocktaking_revisions_no_self_base_check check (based_on_revision_id is null or based_on_revision_id <> stocktaking_revision_id),
    add constraint stocktaking_revisions_ownership_check check (ownership is null or ownership in ('OWNED', 'THIRD_PARTY')),
    add constraint stocktaking_revisions_entity_version_check check (entity_version >= 1),
    add constraint stocktaking_revisions_validation_pair_check check (
        (revision_state = 'WORKING' and validated_at is null and validated_by is null)
        or (revision_state = 'VALIDATED' and validated_at is not null and validated_by is not null)
    ),
    add constraint stocktaking_revisions_empty_semantics_check check (
        (is_empty and commodity_version_id is null and actual_bulk_density_kg_m3 is null and ownership is null and client_id is null and calculation_result_id is null)
        or
        (not is_empty and commodity_version_id is not null and actual_bulk_density_kg_m3 > 0 and ownership is not null)
    ),
    add constraint stocktaking_revisions_client_semantics_check check (
        ownership is null
        or (ownership = 'OWNED' and client_id is null)
        or (ownership = 'THIRD_PARTY' and client_id is not null)
    ),
    add constraint stocktaking_revisions_validated_result_check check (
        revision_state <> 'VALIDATED' or is_empty or calculation_result_id is not null
    );

alter table silomath.measurements
    add constraint measurements_sequence_check check (sequence_number between 1 and 5),
    add constraint measurements_raw_value_check check (raw_value >= 0),
    add constraint measurements_normalized_ullage_check check (normalized_ullage_m >= 0),
    add constraint measurements_raw_unit_check check (btrim(raw_unit) <> ''),
    add constraint measurements_entity_version_check check (entity_version >= 1);

alter table silomath.issues
    add constraint issues_category_check check (category in ('MEASUREMENT_PROBLEM','ACCESS_SAFETY','GEOMETRY_DISCREPANCY','COMMODITY_UNCERTAINTY','STORAGE_SPACE_CONDITION','OTHER')),
    add constraint issues_status_check check (status in ('OPEN','RESOLVED')),
    add constraint issues_description_check check (btrim(description) <> ''),
    add constraint issues_resolution_pair_check check (
        (status = 'OPEN' and resolved_at is null and resolved_by is null)
        or (status = 'RESOLVED' and resolved_at is not null and resolved_by is not null and resolved_at >= reported_at)
    ),
    add constraint issues_entity_version_check check (entity_version >= 1);

alter table silomath.issue_resolutions
    add constraint issue_resolutions_reason_check check (btrim(resolution_reason) <> ''),
    add constraint issue_resolutions_context_check check (resolution_context in ('NORMAL','CLOSE_FOLLOW_UP_ACCEPTANCE'));

alter table silomath.issue_status_events
    add constraint issue_status_events_status_check check (
        (from_status is null and to_status = 'OPEN')
        or (from_status = 'OPEN' and to_status = 'RESOLVED')
        or (from_status = 'RESOLVED' and to_status = 'OPEN')
    ),
    add constraint issue_status_events_reason_check check (from_status is null or (reason is not null and btrim(reason) <> ''));

alter table silomath.evidence
    add constraint evidence_owner_xor_check check ((stocktaking_record_id is not null) <> (issue_id is not null)),
    add constraint evidence_issue_revision_check check (issue_id is null or stocktaking_revision_id is null),
    add constraint evidence_type_check check (evidence_type in ('PHOTO','NOTE')),
    add constraint evidence_sync_state_check check (sync_state in ('LOCAL_PENDING','UPLOADING','SYNCED','SYNC_ERROR')),
    add constraint evidence_removed_pair_check check ((removed_at is null) = (removed_by is null)),
    add constraint evidence_entity_version_check check (entity_version >= 1),
    add constraint evidence_discriminator_check check (
        (evidence_type = 'NOTE'
            and note_text is not null and btrim(note_text) <> '' and char_length(note_text) <= 2000
            and local_private_ref is null and remote_object_ref is null and sha256 is null and mime_type is null
            and byte_size is null and pixel_width is null and pixel_height is null
            and sync_state <> 'UPLOADING')
        or
        (evidence_type = 'PHOTO'
            and note_text is null
            and (local_private_ref is not null or remote_object_ref is not null)
            and sha256 ~ '^[0-9a-f]{64}$'
            and mime_type = 'image/jpeg'
            and byte_size between 1 and 3000000
            and pixel_width > 0 and pixel_height > 0
            and greatest(pixel_width, pixel_height) <= 2048
            and (remote_object_ref is null or position('://' in remote_object_ref) = 0)
            and ((sync_state in ('LOCAL_PENDING','UPLOADING','SYNC_ERROR') and local_private_ref is not null)
                 or (sync_state = 'SYNCED' and remote_object_ref is not null)))
    );

alter table silomath.sync_operations
    add constraint sync_operations_state_check check (state in ('PENDING','IN_FLIGHT','ACKNOWLEDGED','RETRYABLE_ERROR','CONFLICT','PERMANENT_ERROR')),
    add constraint sync_operations_text_check check (btrim(entity_type) <> '' and btrim(operation_type) <> '' and btrim(idempotency_key) <> ''),
    add constraint sync_operations_base_version_check check (base_entity_version is null or base_entity_version >= 1),
    add constraint sync_operations_attempt_count_check check (attempt_count >= 0),
    add constraint sync_operations_updated_time_check check (updated_at >= created_at),
    add constraint sync_operations_last_attempt_check check (last_attempt_at is null or attempt_count > 0),
    add constraint sync_operations_next_attempt_check check (next_attempt_at is null or state = 'RETRYABLE_ERROR'),
    add constraint sync_operations_acknowledgement_check check ((state = 'ACKNOWLEDGED') = (acknowledged_at is not null)),
    add constraint sync_operations_authoritative_version_check check (authoritative_entity_version is null or (state = 'ACKNOWLEDGED' and authoritative_entity_version >= 1));

alter table silomath.sync_operation_dependencies
    add constraint sync_dependencies_type_check check (dependency_type = 'REQUIRED_BEFORE'),
    add constraint sync_dependencies_no_self_check check (parent_operation_id <> dependent_operation_id);

alter table silomath.sync_conflicts
    add constraint sync_conflicts_status_check check (status in ('OPEN','RESOLVED')),
    add constraint sync_conflicts_base_version_check check (base_entity_version is null or base_entity_version >= 1),
    add constraint sync_conflicts_server_version_check check (server_entity_version is null or server_entity_version >= 1),
    add constraint sync_conflicts_resolution_type_check check (resolution_type is null or resolution_type in ('RELOAD_AND_REENTER','DISCARD_LOCAL_ATTEMPT','NO_ACTION_REQUIRED')),
    add constraint sync_conflicts_resolution_state_check check (
        (status = 'OPEN' and resolution_type is null and resolved_at is null and resolved_by is null)
        or
        (status = 'RESOLVED' and resolution_type is not null and resolved_at is not null and resolved_by is not null and resolved_at >= detected_at)
    );

alter table silomath.notifications
    add constraint notifications_type_check check (notification_type in ('SYNC_ERROR','SYNC_CONFLICT','CAMPAIGN_ASSIGNMENT','ISSUE_REQUIRES_ATTENTION','CAMPAIGN_COMPLETED')),
    add constraint notifications_text_check check (btrim(title) <> '' and btrim(body) <> ''),
    add constraint notifications_read_time_check check (read_at is null or read_at >= created_at);

alter table silomath.organization_configurations
    add constraint organization_configurations_type_check check (value_type in ('BOOLEAN','INTEGER','DECIMAL','TEXT','ENUM')),
    add constraint organization_configurations_key_check check (btrim(configuration_key) <> ''),
    add constraint organization_configurations_entity_version_check check (entity_version >= 1),
    add constraint organization_configurations_typed_value_check check (
        (value_type = 'BOOLEAN' and boolean_value is not null and integer_value is null and decimal_value is null and text_value is null and enum_value is null)
        or (value_type = 'INTEGER' and boolean_value is null and integer_value is not null and decimal_value is null and text_value is null and enum_value is null)
        or (value_type = 'DECIMAL' and boolean_value is null and integer_value is null and decimal_value is not null and text_value is null and enum_value is null)
        or (value_type = 'TEXT' and boolean_value is null and integer_value is null and decimal_value is null and text_value is not null and enum_value is null)
        or (value_type = 'ENUM' and boolean_value is null and integer_value is null and decimal_value is null and text_value is null and enum_value is not null)
    );
