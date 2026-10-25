-- SiloMath V6
-- IMP-DB-005 supplement
-- Complete row-level CHECK hardening for CalculationResult and AuditEvent.
--
-- Governing baseline:
--   10_DATABASE_SCHEMA
--   13_CALCULATION_ENGINE_SPECIFICATION
--   17_SECURITY_AUDIT_PRIVACY

alter table silomath.calculation_results
    add constraint calculation_results_surface_model_check
        check (surface_model_version = 'LEVEL_BASED_V1'),
    add constraint calculation_results_aggregation_method_check
        check (aggregation_method = 'MEAN_ULLAGE_V1'),
    add constraint calculation_results_aggregation_rule_version_check
        check (aggregation_rule_version >= 1),
    add constraint calculation_results_volume_check
        check (product_volume_m3 >= 0),
    add constraint calculation_results_quantity_check
        check (physical_quantity_kg >= 0),
    add constraint calculation_results_density_check
        check (actual_bulk_density_kg_m3 is null or actual_bulk_density_kg_m3 > 0),
    add constraint calculation_results_fingerprint_check
        check (input_fingerprint ~ '^[0-9a-f]{64}$'),
    add constraint calculation_results_text_check
        check (
            btrim(input_schema_version) <> ''
            and btrim(engine_version) <> ''
            and btrim(normalization_version) <> ''
            and btrim(rounding_policy) <> ''
            and btrim(calculation_source) <> ''
        ),
    add constraint calculation_results_fill_range_check
        check (
            effective_fill_height_m is null
            or (
                effective_fill_height_m >= 0
                and maximum_fill_height_m is not null
                and effective_fill_height_m <= maximum_fill_height_m
            )
        ),
    add constraint calculation_results_ullage_range_check
        check (
            effective_ullage_m is null
            or (
                effective_ullage_m >= 0
                and measurement_reference_height_m is not null
                and effective_ullage_m <= measurement_reference_height_m
            )
        );

alter table silomath.audit_events
    add constraint audit_events_text_check
        check (
            btrim(action) <> ''
            and btrim(entity_type) <> ''
            and btrim(correlation_id) <> ''
            and btrim(source) <> ''
        ),
    add constraint audit_events_actor_system_check
        check (
            actor_user_id is not null
            or (
                source = 'SYSTEM'
                and system_actor is not null
                and btrim(system_actor) <> ''
            )
        );
