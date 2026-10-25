-- SiloMath V6
-- IMP-DB-006
-- Executable assertions for the frozen PostgreSQL query-family index strategy.

DO $$
DECLARE
    required_index text;
BEGIN
    FOREACH required_index IN ARRAY ARRAY[
        'idx_locations_org_lifecycle_code',
        'idx_clients_org_lifecycle_code',
        'idx_geometry_profiles_org_lifecycle_code',
        'idx_locations_org_coordinates',
        'idx_storage_units_location_lifecycle_code',
        'idx_storage_spaces_unit_lifecycle_code',
        'idx_inventory_campaigns_org_status_start_date',
        'idx_inventory_campaigns_org_year_code',
        'idx_stocktaking_revisions_root_revision_desc',
        'idx_issues_record_status',
        'idx_calculation_results_revision_calculated_at',
        'idx_evidence_record_revision_type',
        'idx_evidence_issue_type',
        'idx_evidence_sync_state',
        'idx_final_snapshots_org_closed_at',
        'idx_sync_operations_state_created_at',
        'idx_sync_operations_entity',
        'idx_sync_dependencies_dependent',
        'idx_audit_events_org_occurred_at_desc',
        'idx_audit_events_entity_occurred_at_desc',
        'idx_notifications_recipient_read_created_desc'
    ] LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM pg_indexes
            WHERE schemaname = 'silomath'
              AND indexname = required_index
        ) THEN
            RAISE EXCEPTION 'DB006 missing required index %', required_index;
        END IF;
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'silomath'
          AND indexname = 'idx_locations_org_coordinates'
          AND lower(indexdef) LIKE '%where%latitude_deg is not null%longitude_deg is not null%'
    ) THEN
        RAISE EXCEPTION 'DB006 Location coordinate index must be partial for persisted coordinates';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'silomath'
          AND indexname = 'idx_stocktaking_revisions_root_revision_desc'
          AND lower(indexdef) LIKE '%revision_number desc%'
    ) THEN
        RAISE EXCEPTION 'DB006 Stocktaking revision navigation index must order revision_number DESC';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'silomath'
          AND indexname = 'idx_audit_events_org_occurred_at_desc'
          AND lower(indexdef) LIKE '%occurred_at desc%'
    ) THEN
        RAISE EXCEPTION 'DB006 Audit organization history index must order occurred_at DESC';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'silomath'
          AND indexname = 'idx_notifications_recipient_read_created_desc'
          AND lower(indexdef) LIKE '%created_at desc%'
    ) THEN
        RAISE EXCEPTION 'DB006 Notification index must order created_at DESC';
    END IF;

    RAISE NOTICE 'V6 DB006 INDEX STRATEGY ASSERTIONS PASSED';
END;
$$;
