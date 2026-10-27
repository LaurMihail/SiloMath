package com.silomath.backend.offline

/** Required frozen fields; nullable server legacy-expansion rows cannot masquerade as complete V6 packages. */
internal object OfflinePackageFieldContract {
    val required = mapOf(
        "calculation_results" to setOf("calculation_result_id","stocktaking_revision_id","surface_model_version","aggregation_method","aggregation_rule_version","input_schema_version","engine_version","normalization_version","frozen_calculation_context","product_volume_m3","physical_quantity_kg","input_fingerprint","rounding_policy","calculated_at","calculation_source"),
        "campaign_scopes" to setOf("campaign_scope_id","campaign_id","storage_space_id","created_at","created_by"),
        "campaign_snapshot_commodity_items" to setOf("snapshot_commodity_item_id","campaign_snapshot_id","commodity_id","commodity_version_id"),
        "campaign_snapshot_scope_items" to setOf("snapshot_scope_item_id","campaign_snapshot_id","storage_space_id","geometry_profile_id","geometry_profile_version_id","location_id","location_code","location_name","storage_unit_id","storage_unit_code","storage_unit_name","storage_space_code","geometry_family_id","geometry_family_code","geometry_family_name","geometry_profile_code","geometry_profile_name","geometry_formula_version","geometry_dimensions","measurement_reference_height_m","maximum_fill_height_m","max_capacity_m3"),
        "campaign_snapshot_team_items" to setOf("snapshot_team_item_id","campaign_snapshot_id","user_id","organization_membership_id","role_at_assignment","user_full_name","user_email"),
        "campaign_snapshots" to setOf("campaign_snapshot_id","campaign_id","organization_id","campaign_code","campaign_name","inventory_type","start_date","planned_end_date","schema_version","calculation_engine_version","created_at","created_by","organization_name","organization_code","snapshot_schema_version","calculation_schema_versions"),
        "campaign_team_assignments" to setOf("assignment_id","campaign_id","user_id","status","assigned_at","assigned_by","created_at","created_by","updated_at","updated_by","entity_version"),
        "clients" to setOf("client_id","organization_id","name","code","lifecycle_status","created_at","created_by","updated_at","updated_by","entity_version"),
        "commodities" to setOf("commodity_id","code","current_version_id","lifecycle_status","created_at","created_by"),
        "commodity_versions" to setOf("commodity_version_id","commodity_id","version_number","display_name","category","schema_version","created_at","created_by"),
        "device_installations" to setOf("device_installation_id","created_at"),
        "evidence" to setOf("evidence_id","organization_id","campaign_id","evidence_type","sync_state","created_at","created_by","entity_version"),
        "geometry_families" to setOf("geometry_family_id","family_code","name","formula_version","parameter_contract"),
        "geometry_profile_versions" to setOf("geometry_profile_version_id","geometry_profile_id","version_number","geometry_family_id","measurement_reference_height_m","maximum_fill_height_m","geometry_origin","max_capacity_m3","formula_version","schema_version","created_at","created_by"),
        "geometry_profiles" to setOf("geometry_profile_id","organization_id","name","code","geometry_family_id","lifecycle_status","current_version_id","created_at","created_by","updated_at","updated_by","entity_version"),
        "inventory_campaigns" to setOf("campaign_id","organization_id","code","campaign_year","sequence","name","inventory_type","start_date","planned_end_date","status","launched_at","launched_by","created_at","created_by","updated_at","updated_by","entity_version"),
        "issue_resolutions" to setOf("issue_resolution_id","issue_id","resolution_reason","resolution_context","resolved_at","resolved_by","audit_event_id"),
        "issue_status_events" to setOf("issue_status_event_id","issue_id","to_status","changed_at","changed_by","audit_event_id"),
        "issues" to setOf("issue_id","organization_id","campaign_id","stocktaking_record_id","category","description","status","reported_at","reported_by","entity_version"),
        "locations" to setOf("location_id","organization_id","name","code","lifecycle_status","created_at","created_by","updated_at","updated_by","entity_version"),
        "measurements" to setOf("measurement_id","stocktaking_revision_id","sequence_number","raw_value","raw_unit","normalized_ullage_m","datum_metadata","geometry_profile_version_id","captured_at","captured_by","entity_version"),
        "organization_memberships" to setOf("membership_id","organization_id","user_id","role","lifecycle_status","created_at","created_by","updated_at","updated_by","entity_version"),
        "organizations" to setOf("organization_id","name","code","lifecycle_status","created_at","created_by","updated_at","updated_by","entity_version"),
        "stocktaking_records" to setOf("stocktaking_record_id","organization_id","campaign_id","storage_space_id","created_at","created_by","updated_at","updated_by","entity_version"),
        "stocktaking_revisions" to setOf("stocktaking_revision_id","stocktaking_record_id","revision_number","revision_state","is_empty","created_at","created_by","entity_version"),
        "storage_spaces" to setOf("storage_space_id","organization_id","storage_unit_id","code","geometry_profile_id","lifecycle_status","created_at","created_by","updated_at","updated_by","entity_version"),
        "storage_units" to setOf("storage_unit_id","organization_id","location_id","name","code","lifecycle_status","created_at","created_by","updated_at","updated_by","entity_version"),
        "users" to setOf("user_id","full_name","email","email_normalized","lifecycle_status","created_at","updated_at","updated_by","entity_version")
    )
}
