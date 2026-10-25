-- SiloMath V6
-- IMP-DB-006
-- PostgreSQL index strategy hardening for frozen query families.
--
-- Governing baseline:
--   10_DATABASE_SCHEMA section 8
--
-- Unique indexes created by DB004 already cover several exact lookup intents;
-- this migration adds only the remaining navigation/filter/worker/history indexes.

create index if not exists idx_locations_org_lifecycle_code
    on silomath.locations (organization_id, lifecycle_status, code);

create index if not exists idx_clients_org_lifecycle_code
    on silomath.clients (organization_id, lifecycle_status, code);

create index if not exists idx_geometry_profiles_org_lifecycle_code
    on silomath.geometry_profiles (organization_id, lifecycle_status, code);

create index if not exists idx_locations_org_coordinates
    on silomath.locations (organization_id, latitude_deg, longitude_deg)
    where latitude_deg is not null and longitude_deg is not null;

create index if not exists idx_storage_units_location_lifecycle_code
    on silomath.storage_units (location_id, lifecycle_status, code);

create index if not exists idx_storage_spaces_unit_lifecycle_code
    on silomath.storage_spaces (storage_unit_id, lifecycle_status, code);

create index if not exists idx_inventory_campaigns_org_status_start_date
    on silomath.inventory_campaigns (organization_id, status, start_date);

create index if not exists idx_inventory_campaigns_org_year_code
    on silomath.inventory_campaigns (organization_id, campaign_year, code);

create index if not exists idx_stocktaking_revisions_root_revision_desc
    on silomath.stocktaking_revisions (stocktaking_record_id, revision_number desc);

create index if not exists idx_issues_record_status
    on silomath.issues (stocktaking_record_id, status);

create index if not exists idx_calculation_results_revision_calculated_at
    on silomath.calculation_results (stocktaking_revision_id, calculated_at);

create index if not exists idx_evidence_record_revision_type
    on silomath.evidence (stocktaking_record_id, stocktaking_revision_id, evidence_type);

create index if not exists idx_evidence_issue_type
    on silomath.evidence (issue_id, evidence_type);

create index if not exists idx_evidence_sync_state
    on silomath.evidence (sync_state);

create index if not exists idx_final_snapshots_org_closed_at
    on silomath.final_inventory_snapshots (organization_id, closed_at);

create index if not exists idx_sync_operations_state_created_at
    on silomath.sync_operations (state, created_at);

create index if not exists idx_sync_operations_entity
    on silomath.sync_operations (entity_type, entity_id);

create index if not exists idx_sync_dependencies_dependent
    on silomath.sync_operation_dependencies (dependent_operation_id);

create index if not exists idx_audit_events_org_occurred_at_desc
    on silomath.audit_events (organization_id, occurred_at desc);

create index if not exists idx_audit_events_entity_occurred_at_desc
    on silomath.audit_events (entity_type, entity_id, occurred_at desc);

create index if not exists idx_notifications_recipient_read_created_desc
    on silomath.notifications (recipient_user_id, read_at, created_at desc);
