-- SiloMath V6 APPROVED/FROZEN — canonical PostgreSQL baseline
-- Generated from the cleaned live `silomath` catalog on 2026-11-02.
-- Superseded migration history and deprecated RPC overloads are intentionally excluded.
-- Canonical seed includes only frozen geometry and commodity master data; user/org bootstrap data is intentionally excluded.

BEGIN;

-- Required extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA "extensions";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA "extensions";

-- Application schema
CREATE SCHEMA IF NOT EXISTS silomath AUTHORIZATION postgres;
ALTER SCHEMA silomath OWNER TO postgres;
REVOKE ALL ON SCHEMA silomath FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA silomath TO postgres;

-- Sequence(s)
CREATE SEQUENCE "silomath"."sync_change_stream_change_sequence_seq" AS bigint INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 NO CYCLE;
ALTER SEQUENCE "silomath"."sync_change_stream_change_sequence_seq" OWNER TO postgres;

-- Tables
CREATE TABLE "silomath"."api_idempotency_records" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "actor_user_id" uuid NOT NULL,
    "organization_id" uuid,
    "operation_code" text NOT NULL,
    "idempotency_key" text NOT NULL,
    "request_fingerprint" text NOT NULL,
    "state" text DEFAULT 'IN_PROGRESS'::text NOT NULL,
    "response_status" integer,
    "response_body" jsonb,
    "created_at" timestamp with time zone DEFAULT now() NOT NULL,
    "completed_at" timestamp with time zone
);
ALTER TABLE "silomath"."api_idempotency_records" OWNER TO postgres;

CREATE TABLE "silomath"."audit_events" (
    "audit_event_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "campaign_id" uuid,
    "actor_user_id" uuid,
    "action" text NOT NULL,
    "entity_type" text NOT NULL,
    "entity_id" uuid NOT NULL,
    "occurred_at" timestamp with time zone NOT NULL,
    "before_data" jsonb,
    "after_data" jsonb,
    "change_metadata" jsonb,
    "correlation_id" text NOT NULL,
    "source" text NOT NULL,
    "system_actor" text
);
ALTER TABLE "silomath"."audit_events" OWNER TO postgres;

CREATE TABLE "silomath"."calculation_results" (
    "calculation_result_id" uuid NOT NULL,
    "stocktaking_revision_id" uuid NOT NULL,
    "surface_model_version" text NOT NULL,
    "aggregation_method" text NOT NULL,
    "aggregation_rule_version" integer NOT NULL,
    "input_schema_version" text NOT NULL,
    "engine_version" text NOT NULL,
    "normalization_version" text NOT NULL,
    "formula_code" text,
    "formula_version" text,
    "geometry_family_id" uuid,
    "geometry_profile_version_id" uuid,
    "frozen_calculation_context" jsonb NOT NULL,
    "datum_metadata" jsonb,
    "geometry_inputs_snapshot" jsonb,
    "ordered_normalized_readings" jsonb,
    "geometry_origin" text,
    "measurement_reference_height_m" numeric(20,6),
    "maximum_fill_height_m" numeric(20,6),
    "max_capacity_m3" numeric(20,6),
    "effective_ullage_m" numeric(20,6),
    "effective_fill_height_m" numeric(20,6),
    "product_volume_m3" numeric(20,6) NOT NULL,
    "entered_density_value" numeric(20,6),
    "entered_density_unit" text,
    "actual_bulk_density_kg_m3" numeric(20,6),
    "physical_quantity_kg" numeric(20,6) NOT NULL,
    "input_fingerprint" text NOT NULL,
    "rounding_policy" text NOT NULL,
    "calculated_at" timestamp with time zone NOT NULL,
    "calculated_by" uuid,
    "calculation_source" text NOT NULL
);
ALTER TABLE "silomath"."calculation_results" OWNER TO postgres;

CREATE TABLE "silomath"."campaign_scopes" (
    "campaign_scope_id" uuid NOT NULL,
    "campaign_id" uuid NOT NULL,
    "storage_space_id" uuid NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL
);
ALTER TABLE "silomath"."campaign_scopes" OWNER TO postgres;

CREATE TABLE "silomath"."campaign_snapshot_commodity_items" (
    "snapshot_commodity_item_id" uuid NOT NULL,
    "campaign_snapshot_id" uuid NOT NULL,
    "commodity_id" uuid,
    "commodity_version_id" uuid
);
ALTER TABLE "silomath"."campaign_snapshot_commodity_items" OWNER TO postgres;

CREATE TABLE "silomath"."campaign_snapshot_scope_items" (
    "snapshot_scope_item_id" uuid NOT NULL,
    "campaign_snapshot_id" uuid NOT NULL,
    "storage_space_id" uuid NOT NULL,
    "geometry_profile_id" uuid NOT NULL,
    "geometry_profile_version_id" uuid NOT NULL,
    "location_id" uuid,
    "location_code" text,
    "location_name" text,
    "storage_unit_id" uuid,
    "storage_unit_code" text,
    "storage_unit_name" text,
    "storage_space_code" text,
    "geometry_family_id" uuid,
    "geometry_family_code" text,
    "geometry_family_name" text,
    "geometry_profile_code" text,
    "geometry_profile_name" text,
    "geometry_formula_version" text,
    "geometry_dimensions" jsonb,
    "measurement_reference_height_m" numeric(20,6),
    "maximum_fill_height_m" numeric(20,6),
    "max_capacity_m3" numeric(20,6)
);
ALTER TABLE "silomath"."campaign_snapshot_scope_items" OWNER TO postgres;

CREATE TABLE "silomath"."campaign_snapshot_team_items" (
    "snapshot_team_item_id" uuid NOT NULL,
    "campaign_snapshot_id" uuid NOT NULL,
    "user_id" uuid NOT NULL,
    "organization_membership_id" uuid,
    "role_at_assignment" text,
    "user_full_name" text,
    "user_email" text
);
ALTER TABLE "silomath"."campaign_snapshot_team_items" OWNER TO postgres;

CREATE TABLE "silomath"."campaign_snapshots" (
    "campaign_snapshot_id" uuid NOT NULL,
    "campaign_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "campaign_code" text NOT NULL,
    "campaign_name" text NOT NULL,
    "inventory_type" text NOT NULL,
    "start_date" date NOT NULL,
    "planned_end_date" date NOT NULL,
    "schema_version" text NOT NULL,
    "calculation_engine_version" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "organization_name" text,
    "organization_code" text,
    "snapshot_schema_version" text,
    "calculation_schema_versions" jsonb
);
ALTER TABLE "silomath"."campaign_snapshots" OWNER TO postgres;

CREATE TABLE "silomath"."campaign_team_assignments" (
    "assignment_id" uuid NOT NULL,
    "campaign_id" uuid NOT NULL,
    "user_id" uuid NOT NULL,
    "status" text NOT NULL,
    "assigned_at" timestamp with time zone NOT NULL,
    "assigned_by" uuid NOT NULL,
    "removed_at" timestamp with time zone,
    "removed_by" uuid,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."campaign_team_assignments" OWNER TO postgres;

CREATE TABLE "silomath"."clients" (
    "client_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "name" text NOT NULL,
    "code" text NOT NULL,
    "tax_id" text,
    "notes" text,
    "lifecycle_status" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."clients" OWNER TO postgres;

CREATE TABLE "silomath"."commodities" (
    "commodity_id" uuid NOT NULL,
    "code" text NOT NULL,
    "current_version_id" uuid NOT NULL,
    "lifecycle_status" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL
);
ALTER TABLE "silomath"."commodities" OWNER TO postgres;

CREATE TABLE "silomath"."commodity_versions" (
    "commodity_version_id" uuid NOT NULL,
    "commodity_id" uuid NOT NULL,
    "version_number" integer NOT NULL,
    "display_name" text NOT NULL,
    "category" text NOT NULL,
    "reference_test_weight_value" numeric(20,6),
    "reference_test_weight_unit" text,
    "moisture_reference" text,
    "impurities_reference" text,
    "schema_version" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "definition_metadata" jsonb
);
ALTER TABLE "silomath"."commodity_versions" OWNER TO postgres;

CREATE TABLE "silomath"."device_installations" (
    "device_installation_id" uuid NOT NULL,
    "user_id" uuid,
    "created_at" timestamp with time zone NOT NULL,
    "last_authorized_at" timestamp with time zone,
    "revoked_at" timestamp with time zone
);
ALTER TABLE "silomath"."device_installations" OWNER TO postgres;

CREATE TABLE "silomath"."evidence" (
    "evidence_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "campaign_id" uuid NOT NULL,
    "stocktaking_record_id" uuid,
    "issue_id" uuid,
    "stocktaking_revision_id" uuid,
    "evidence_type" text NOT NULL,
    "note_text" text,
    "local_private_ref" text,
    "remote_object_ref" text,
    "sha256" text,
    "mime_type" text,
    "byte_size" bigint,
    "pixel_width" integer,
    "pixel_height" integer,
    "sync_state" text NOT NULL,
    "removed_at" timestamp with time zone,
    "removed_by" uuid,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."evidence" OWNER TO postgres;

CREATE TABLE "silomath"."final_inventory_snapshot_entries" (
    "entry_id" uuid NOT NULL,
    "final_snapshot_id" uuid NOT NULL,
    "storage_space_id" uuid NOT NULL,
    "hierarchy_snapshot" jsonb NOT NULL,
    "geometry_snapshot" jsonb NOT NULL,
    "commodity_snapshot" jsonb,
    "ownership_client_snapshot" jsonb,
    "is_empty" boolean NOT NULL,
    "measurements_snapshot" jsonb,
    "operational_values" jsonb NOT NULL,
    "calculation_metadata" jsonb NOT NULL,
    "validation_metadata" jsonb NOT NULL,
    "issue_references_resolution_summary" jsonb NOT NULL,
    "evidence_references" jsonb NOT NULL
);
ALTER TABLE "silomath"."final_inventory_snapshot_entries" OWNER TO postgres;

CREATE TABLE "silomath"."final_inventory_snapshots" (
    "final_snapshot_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "campaign_id" uuid NOT NULL,
    "campaign_snapshot_id" uuid NOT NULL,
    "closed_at" timestamp with time zone NOT NULL,
    "closed_by" uuid NOT NULL,
    "snapshot_schema_version" text NOT NULL,
    "calculation_compatibility_versions" jsonb NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL
);
ALTER TABLE "silomath"."final_inventory_snapshots" OWNER TO postgres;

CREATE TABLE "silomath"."geometry_families" (
    "geometry_family_id" uuid NOT NULL,
    "family_code" text NOT NULL,
    "name" text NOT NULL,
    "formula_version" text NOT NULL,
    "parameter_contract" text NOT NULL
);
ALTER TABLE "silomath"."geometry_families" OWNER TO postgres;

CREATE TABLE "silomath"."geometry_profile_versions" (
    "geometry_profile_version_id" uuid NOT NULL,
    "geometry_profile_id" uuid NOT NULL,
    "version_number" integer NOT NULL,
    "geometry_family_id" uuid NOT NULL,
    "diameter_m" numeric(20,6),
    "cylinder_height_m" numeric(20,6),
    "hopper_height_m" numeric(20,6),
    "outlet_diameter_m" numeric(20,6),
    "side_length_m" numeric(20,6),
    "length_m" numeric(20,6),
    "width_m" numeric(20,6),
    "body_height_m" numeric(20,6),
    "height_m" numeric(20,6),
    "verified_area_m2" numeric(20,6),
    "area_provenance" text,
    "measurement_reference_height_m" numeric(20,6) NOT NULL,
    "maximum_fill_height_m" numeric(20,6) NOT NULL,
    "geometry_origin" text NOT NULL,
    "max_capacity_m3" numeric(20,6) NOT NULL,
    "formula_version" text NOT NULL,
    "schema_version" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "has_constant_cross_section" boolean
);
ALTER TABLE "silomath"."geometry_profile_versions" OWNER TO postgres;

CREATE TABLE "silomath"."geometry_profiles" (
    "geometry_profile_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "name" text NOT NULL,
    "code" text NOT NULL,
    "geometry_family_id" uuid NOT NULL,
    "lifecycle_status" text NOT NULL,
    "current_version_id" uuid NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."geometry_profiles" OWNER TO postgres;

CREATE TABLE "silomath"."inventory_campaigns" (
    "campaign_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "code" text NOT NULL,
    "campaign_year" integer NOT NULL,
    "sequence" integer NOT NULL,
    "name" text NOT NULL,
    "inventory_type" text NOT NULL,
    "start_date" date NOT NULL,
    "planned_end_date" date NOT NULL,
    "status" text NOT NULL,
    "launched_at" timestamp with time zone NOT NULL,
    "launched_by" uuid NOT NULL,
    "campaign_snapshot_id" uuid,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL,
    "completed_at" timestamp with time zone,
    "completed_by" uuid
);
ALTER TABLE "silomath"."inventory_campaigns" OWNER TO postgres;

CREATE TABLE "silomath"."issue_resolutions" (
    "issue_resolution_id" uuid NOT NULL,
    "issue_id" uuid NOT NULL,
    "resolution_reason" text NOT NULL,
    "resolution_context" text NOT NULL,
    "resolved_at" timestamp with time zone NOT NULL,
    "resolved_by" uuid NOT NULL,
    "audit_event_id" uuid NOT NULL
);
ALTER TABLE "silomath"."issue_resolutions" OWNER TO postgres;

CREATE TABLE "silomath"."issue_status_events" (
    "issue_status_event_id" uuid NOT NULL,
    "issue_id" uuid NOT NULL,
    "from_status" text,
    "to_status" text NOT NULL,
    "reason" text,
    "changed_at" timestamp with time zone NOT NULL,
    "changed_by" uuid NOT NULL,
    "audit_event_id" uuid NOT NULL
);
ALTER TABLE "silomath"."issue_status_events" OWNER TO postgres;

CREATE TABLE "silomath"."issues" (
    "issue_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "campaign_id" uuid NOT NULL,
    "stocktaking_record_id" uuid NOT NULL,
    "category" text NOT NULL,
    "description" text NOT NULL,
    "status" text NOT NULL,
    "reported_at" timestamp with time zone NOT NULL,
    "reported_by" uuid NOT NULL,
    "resolved_at" timestamp with time zone,
    "resolved_by" uuid,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."issues" OWNER TO postgres;

CREATE TABLE "silomath"."locations" (
    "location_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "name" text NOT NULL,
    "code" text NOT NULL,
    "address" text,
    "city" text,
    "country" text,
    "notes" text,
    "latitude_deg" numeric(9,6),
    "longitude_deg" numeric(10,6),
    "lifecycle_status" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."locations" OWNER TO postgres;

CREATE TABLE "silomath"."measurements" (
    "measurement_id" uuid NOT NULL,
    "stocktaking_revision_id" uuid NOT NULL,
    "sequence_number" integer NOT NULL,
    "raw_value" numeric(20,6) NOT NULL,
    "raw_unit" text NOT NULL,
    "normalized_ullage_m" numeric(20,6) NOT NULL,
    "datum_metadata" jsonb NOT NULL,
    "geometry_profile_version_id" uuid NOT NULL,
    "captured_at" timestamp with time zone NOT NULL,
    "captured_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."measurements" OWNER TO postgres;

CREATE TABLE "silomath"."notifications" (
    "notification_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "recipient_user_id" uuid NOT NULL,
    "notification_type" text NOT NULL,
    "title" text NOT NULL,
    "body" text NOT NULL,
    "context" jsonb,
    "created_at" timestamp with time zone NOT NULL,
    "read_at" timestamp with time zone,
    "state" text GENERATED ALWAYS AS (
CASE
    WHEN (read_at IS NULL) THEN 'UNREAD'::text
    ELSE 'READ'::text
END) STORED
);
ALTER TABLE "silomath"."notifications" OWNER TO postgres;

CREATE TABLE "silomath"."offline_working_set_manifests" (
    "manifest_id" uuid NOT NULL,
    "campaign_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "device_installation_id" uuid NOT NULL,
    "package_version" text NOT NULL,
    "content_manifest" jsonb NOT NULL,
    "integrity_hash" text NOT NULL,
    "integrity_algorithm" text NOT NULL,
    "verification_metadata" jsonb,
    "generated_at" timestamp with time zone NOT NULL,
    "downloaded_at" timestamp with time zone,
    "verified_at" timestamp with time zone,
    "is_current" boolean DEFAULT false NOT NULL
);
ALTER TABLE "silomath"."offline_working_set_manifests" OWNER TO postgres;

CREATE TABLE "silomath"."organization_configurations" (
    "organization_configuration_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "configuration_key" text NOT NULL,
    "value_type" text NOT NULL,
    "boolean_value" boolean,
    "integer_value" bigint,
    "decimal_value" numeric(20,6),
    "text_value" text,
    "enum_value" text,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."organization_configurations" OWNER TO postgres;

CREATE TABLE "silomath"."organization_memberships" (
    "membership_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "user_id" uuid NOT NULL,
    "role" text NOT NULL,
    "lifecycle_status" text NOT NULL,
    "disabled_at" timestamp with time zone,
    "disabled_by" uuid,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."organization_memberships" OWNER TO postgres;

CREATE TABLE "silomath"."organizations" (
    "organization_id" uuid NOT NULL,
    "name" text NOT NULL,
    "code" text NOT NULL,
    "address" text,
    "city" text,
    "country" text,
    "lifecycle_status" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."organizations" OWNER TO postgres;

CREATE TABLE "silomath"."stocktaking_records" (
    "stocktaking_record_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "campaign_id" uuid NOT NULL,
    "storage_space_id" uuid NOT NULL,
    "current_revision_id" uuid,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."stocktaking_records" OWNER TO postgres;

CREATE TABLE "silomath"."stocktaking_revisions" (
    "stocktaking_revision_id" uuid NOT NULL,
    "stocktaking_record_id" uuid NOT NULL,
    "revision_number" integer NOT NULL,
    "revision_state" text NOT NULL,
    "based_on_revision_id" uuid,
    "is_empty" boolean NOT NULL,
    "commodity_version_id" uuid,
    "actual_bulk_density_kg_m3" numeric(20,6),
    "ownership" text,
    "client_id" uuid,
    "notes" text,
    "calculation_result_id" uuid,
    "validated_at" timestamp with time zone,
    "validated_by" uuid,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."stocktaking_revisions" OWNER TO postgres;

CREATE TABLE "silomath"."storage_spaces" (
    "storage_space_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "storage_unit_id" uuid NOT NULL,
    "code" text NOT NULL,
    "description" text,
    "geometry_profile_id" uuid NOT NULL,
    "lifecycle_status" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."storage_spaces" OWNER TO postgres;

CREATE TABLE "silomath"."storage_units" (
    "storage_unit_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "location_id" uuid NOT NULL,
    "name" text NOT NULL,
    "code" text NOT NULL,
    "description" text,
    "lifecycle_status" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."storage_units" OWNER TO postgres;

CREATE TABLE "silomath"."sync_change_stream" (
    "change_sequence" bigint DEFAULT nextval('silomath.sync_change_stream_change_sequence_seq'::regclass) NOT NULL,
    "organization_id" uuid NOT NULL,
    "campaign_id" uuid,
    "entity_type" text NOT NULL,
    "entity_id" uuid NOT NULL,
    "entity_version" bigint NOT NULL,
    "change_type" text NOT NULL,
    "server_changed_at" timestamp with time zone DEFAULT now() NOT NULL,
    "authoritative_data" jsonb NOT NULL
);
ALTER TABLE "silomath"."sync_change_stream" OWNER TO postgres;

CREATE TABLE "silomath"."sync_conflicts" (
    "sync_conflict_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "sync_operation_id" uuid NOT NULL,
    "entity_type" text NOT NULL,
    "entity_id" uuid NOT NULL,
    "local_attempt" jsonb NOT NULL,
    "server_value" jsonb NOT NULL,
    "base_entity_version" bigint,
    "server_entity_version" bigint,
    "detected_at" timestamp with time zone NOT NULL,
    "status" text NOT NULL,
    "resolution_type" text,
    "resolved_at" timestamp with time zone,
    "resolved_by" uuid,
    "server_version" bigint
);
ALTER TABLE "silomath"."sync_conflicts" OWNER TO postgres;

CREATE TABLE "silomath"."sync_operation_dependencies" (
    "dependency_id" uuid NOT NULL,
    "parent_operation_id" uuid NOT NULL,
    "dependent_operation_id" uuid NOT NULL,
    "dependency_type" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL
);
ALTER TABLE "silomath"."sync_operation_dependencies" OWNER TO postgres;

CREATE TABLE "silomath"."sync_operations" (
    "sync_operation_id" uuid NOT NULL,
    "organization_id" uuid NOT NULL,
    "campaign_id" uuid,
    "entity_type" text NOT NULL,
    "entity_id" uuid NOT NULL,
    "operation_type" text NOT NULL,
    "payload" jsonb NOT NULL,
    "base_entity_version" bigint,
    "state" text NOT NULL,
    "idempotency_key" text NOT NULL,
    "attempt_count" bigint NOT NULL,
    "next_attempt_at" timestamp with time zone,
    "last_attempt_at" timestamp with time zone,
    "last_error_code" text,
    "correlation_id" text,
    "event_at" timestamp with time zone NOT NULL,
    "acknowledged_at" timestamp with time zone,
    "authoritative_entity_version" bigint,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL
);
ALTER TABLE "silomath"."sync_operations" OWNER TO postgres;

CREATE TABLE "silomath"."user_preferences" (
    "user_preference_id" uuid NOT NULL,
    "user_id" uuid NOT NULL,
    "theme" text NOT NULL,
    "notification_preferences" jsonb,
    "updated_at" timestamp with time zone NOT NULL
);
ALTER TABLE "silomath"."user_preferences" OWNER TO postgres;

CREATE TABLE "silomath"."users" (
    "user_id" uuid NOT NULL,
    "full_name" text NOT NULL,
    "email" text NOT NULL,
    "email_normalized" text NOT NULL,
    "lifecycle_status" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "created_by" uuid,
    "updated_at" timestamp with time zone NOT NULL,
    "updated_by" uuid NOT NULL,
    "entity_version" bigint DEFAULT 1 NOT NULL
);
ALTER TABLE "silomath"."users" OWNER TO postgres;

-- Sequence ownership
ALTER SEQUENCE "silomath"."sync_change_stream_change_sequence_seq" OWNED BY "silomath"."sync_change_stream"."change_sequence";

-- Primary, unique and check constraints
ALTER TABLE ONLY "silomath"."api_idempotency_records" ADD CONSTRAINT "api_idempotency_records_completion_chk" CHECK (state = 'IN_PROGRESS'::text AND response_status IS NULL AND response_body IS NULL AND completed_at IS NULL OR state = 'COMPLETED'::text AND response_status IS NOT NULL AND response_body IS NOT NULL AND completed_at IS NOT NULL);
ALTER TABLE ONLY "silomath"."api_idempotency_records" ADD CONSTRAINT "api_idempotency_records_fingerprint_chk" CHECK (btrim(request_fingerprint) <> ''::text);
ALTER TABLE ONLY "silomath"."api_idempotency_records" ADD CONSTRAINT "api_idempotency_records_key_chk" CHECK (btrim(idempotency_key) <> ''::text);
ALTER TABLE ONLY "silomath"."api_idempotency_records" ADD CONSTRAINT "api_idempotency_records_operation_code_chk" CHECK (btrim(operation_code) <> ''::text);
ALTER TABLE ONLY "silomath"."api_idempotency_records" ADD CONSTRAINT "api_idempotency_records_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "silomath"."api_idempotency_records" ADD CONSTRAINT "api_idempotency_records_state_chk" CHECK (state = ANY (ARRAY['IN_PROGRESS'::text, 'COMPLETED'::text]));
ALTER TABLE ONLY "silomath"."audit_events" ADD CONSTRAINT "audit_events_actor_system_check" CHECK (actor_user_id IS NOT NULL OR source = 'SYSTEM'::text AND system_actor IS NOT NULL AND btrim(system_actor) <> ''::text);
ALTER TABLE ONLY "silomath"."audit_events" ADD CONSTRAINT "audit_events_pkey" PRIMARY KEY (audit_event_id);
ALTER TABLE ONLY "silomath"."audit_events" ADD CONSTRAINT "audit_events_text_check" CHECK (btrim(action) <> ''::text AND btrim(entity_type) <> ''::text AND btrim(correlation_id) <> ''::text AND btrim(source) <> ''::text);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_aggregation_method_check" CHECK (aggregation_method = 'MEAN_ULLAGE'::text);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_aggregation_rule_version_check" CHECK (aggregation_rule_version = 1);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_density_check" CHECK (actual_bulk_density_kg_m3 IS NULL OR actual_bulk_density_kg_m3 > 0::numeric);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_fd73_version_tuple_check" CHECK (formula_version = '1'::text AND input_schema_version = '1'::text AND engine_version = '2.0'::text AND normalization_version = '1'::text AND surface_model_version = 'LEVEL_BASED_V1'::text);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_fill_range_check" CHECK (effective_fill_height_m IS NULL OR effective_fill_height_m >= 0::numeric AND maximum_fill_height_m IS NOT NULL AND effective_fill_height_m <= maximum_fill_height_m);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_fingerprint_check" CHECK (input_fingerprint ~ '^[0-9a-f]{64}$'::text);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_pkey" PRIMARY KEY (calculation_result_id);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_quantity_check" CHECK (physical_quantity_kg >= 0::numeric);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_surface_model_check" CHECK (surface_model_version = 'LEVEL_BASED_V1'::text);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_text_check" CHECK (btrim(input_schema_version) <> ''::text AND btrim(engine_version) <> ''::text AND btrim(normalization_version) <> ''::text AND btrim(rounding_policy) <> ''::text AND btrim(calculation_source) <> ''::text);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_ullage_range_check" CHECK (effective_ullage_m IS NULL OR effective_ullage_m >= 0::numeric AND measurement_reference_height_m IS NOT NULL AND effective_ullage_m <= measurement_reference_height_m);
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "calculation_results_volume_check" CHECK (product_volume_m3 >= 0::numeric);
ALTER TABLE ONLY "silomath"."campaign_scopes" ADD CONSTRAINT "campaign_scopes_campaign_space_unique" UNIQUE (campaign_id, storage_space_id);
ALTER TABLE ONLY "silomath"."campaign_scopes" ADD CONSTRAINT "campaign_scopes_pkey" PRIMARY KEY (campaign_scope_id);
ALTER TABLE ONLY "silomath"."campaign_snapshot_commodity_items" ADD CONSTRAINT "campaign_snapshot_commodity_items_pkey" PRIMARY KEY (snapshot_commodity_item_id);
ALTER TABLE ONLY "silomath"."campaign_snapshot_scope_items" ADD CONSTRAINT "campaign_snapshot_scope_items_pkey" PRIMARY KEY (snapshot_scope_item_id);
ALTER TABLE ONLY "silomath"."campaign_snapshot_team_items" ADD CONSTRAINT "campaign_snapshot_team_items_pkey" PRIMARY KEY (snapshot_team_item_id);
ALTER TABLE ONLY "silomath"."campaign_snapshots" ADD CONSTRAINT "campaign_snapshots_campaign_id_key" UNIQUE (campaign_id);
ALTER TABLE ONLY "silomath"."campaign_snapshots" ADD CONSTRAINT "campaign_snapshots_inventory_type_check" CHECK (inventory_type = ANY (ARRAY['ANNUAL'::text, 'MONTHLY'::text, 'CONTROL'::text]));
ALTER TABLE ONLY "silomath"."campaign_snapshots" ADD CONSTRAINT "campaign_snapshots_pkey" PRIMARY KEY (campaign_snapshot_id);
ALTER TABLE ONLY "silomath"."campaign_team_assignments" ADD CONSTRAINT "campaign_team_assignments_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."campaign_team_assignments" ADD CONSTRAINT "campaign_team_assignments_pkey" PRIMARY KEY (assignment_id);
ALTER TABLE ONLY "silomath"."campaign_team_assignments" ADD CONSTRAINT "campaign_team_assignments_removed_pair_check" CHECK (status = 'ACTIVE'::text AND removed_at IS NULL AND removed_by IS NULL OR status = 'REMOVED'::text AND removed_at IS NOT NULL AND removed_by IS NOT NULL);
ALTER TABLE ONLY "silomath"."campaign_team_assignments" ADD CONSTRAINT "campaign_team_assignments_status_check" CHECK (status = ANY (ARRAY['ACTIVE'::text, 'REMOVED'::text]));
ALTER TABLE ONLY "silomath"."clients" ADD CONSTRAINT "clients_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."clients" ADD CONSTRAINT "clients_lifecycle_status_check" CHECK (lifecycle_status = ANY (ARRAY['ACTIVE'::text, 'INACTIVE'::text]));
ALTER TABLE ONLY "silomath"."clients" ADD CONSTRAINT "clients_pkey" PRIMARY KEY (client_id);
ALTER TABLE ONLY "silomath"."clients" ADD CONSTRAINT "clients_text_check" CHECK (btrim(name) <> ''::text AND btrim(code) <> ''::text);
ALTER TABLE ONLY "silomath"."commodities" ADD CONSTRAINT "commodities_code_check" CHECK (btrim(code) <> ''::text);
ALTER TABLE ONLY "silomath"."commodities" ADD CONSTRAINT "commodities_lifecycle_status_check" CHECK (lifecycle_status = ANY (ARRAY['ACTIVE'::text, 'INACTIVE'::text]));
ALTER TABLE ONLY "silomath"."commodities" ADD CONSTRAINT "commodities_pkey" PRIMARY KEY (commodity_id);
ALTER TABLE ONLY "silomath"."commodity_versions" ADD CONSTRAINT "commodity_versions_number_check" CHECK (version_number >= 1);
ALTER TABLE ONLY "silomath"."commodity_versions" ADD CONSTRAINT "commodity_versions_pkey" PRIMARY KEY (commodity_version_id);
ALTER TABLE ONLY "silomath"."commodity_versions" ADD CONSTRAINT "commodity_versions_reference_weight_pair_check" CHECK (reference_test_weight_value IS NULL AND reference_test_weight_unit IS NULL OR reference_test_weight_value > 0::numeric AND reference_test_weight_unit IS NOT NULL AND btrim(reference_test_weight_unit) <> ''::text);
ALTER TABLE ONLY "silomath"."commodity_versions" ADD CONSTRAINT "commodity_versions_text_check" CHECK (btrim(display_name) <> ''::text AND btrim(category) <> ''::text AND btrim(schema_version) <> ''::text);
ALTER TABLE ONLY "silomath"."device_installations" ADD CONSTRAINT "device_installations_pkey" PRIMARY KEY (device_installation_id);
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "evidence_discriminator_check" CHECK (evidence_type = 'NOTE'::text AND note_text IS NOT NULL AND btrim(note_text) <> ''::text AND char_length(note_text) <= 2000 AND local_private_ref IS NULL AND remote_object_ref IS NULL AND sha256 IS NULL AND mime_type IS NULL AND byte_size IS NULL AND pixel_width IS NULL AND pixel_height IS NULL AND sync_state <> 'UPLOADING'::text OR evidence_type = 'PHOTO'::text AND note_text IS NULL AND (local_private_ref IS NOT NULL OR remote_object_ref IS NOT NULL) AND sha256 ~ '^[0-9a-f]{64}$'::text AND mime_type = 'image/jpeg'::text AND byte_size >= 1 AND byte_size <= 3000000 AND pixel_width > 0 AND pixel_height > 0 AND GREATEST(pixel_width, pixel_height) <= 2048 AND (remote_object_ref IS NULL OR POSITION(('://'::text) IN (remote_object_ref)) = 0) AND ((sync_state = ANY (ARRAY['LOCAL_PENDING'::text, 'UPLOADING'::text, 'SYNC_ERROR'::text])) AND local_private_ref IS NOT NULL OR sync_state = 'SYNCED'::text AND remote_object_ref IS NOT NULL));
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "evidence_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "evidence_issue_revision_check" CHECK (issue_id IS NULL OR stocktaking_revision_id IS NULL);
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "evidence_owner_xor_check" CHECK ((stocktaking_record_id IS NOT NULL) <> (issue_id IS NOT NULL));
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "evidence_pkey" PRIMARY KEY (evidence_id);
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "evidence_removed_pair_check" CHECK ((removed_at IS NULL) = (removed_by IS NULL));
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "evidence_sync_state_check" CHECK (sync_state = ANY (ARRAY['LOCAL_PENDING'::text, 'UPLOADING'::text, 'SYNCED'::text, 'SYNC_ERROR'::text]));
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "evidence_type_check" CHECK (evidence_type = ANY (ARRAY['PHOTO'::text, 'NOTE'::text]));
ALTER TABLE ONLY "silomath"."final_inventory_snapshot_entries" ADD CONSTRAINT "final_inventory_snapshot_entries_pkey" PRIMARY KEY (entry_id);
ALTER TABLE ONLY "silomath"."final_inventory_snapshots" ADD CONSTRAINT "final_inventory_snapshots_pkey" PRIMARY KEY (final_snapshot_id);
ALTER TABLE ONLY "silomath"."geometry_families" ADD CONSTRAINT "geometry_families_code_check" CHECK (family_code = ANY (ARRAY['CYL_TRUNC_CONE_V1'::text, 'CYL_FLAT_V1'::text, 'SQUARE_FLAT_V1'::text, 'RECT_FLAT_V1'::text, 'SQUARE_PYRAMID_HOPPER_V1'::text, 'RECT_PYRAMID_HOPPER_V1'::text, 'REG_HEX_FLAT_V1'::text, 'STAR_VERIFIED_AREA_V1'::text, 'POCKET_VERIFIED_AREA_V1'::text, 'PERIMETER_VERIFIED_AREA_V1'::text]));
ALTER TABLE ONLY "silomath"."geometry_families" ADD CONSTRAINT "geometry_families_pkey" PRIMARY KEY (geometry_family_id);
ALTER TABLE ONLY "silomath"."geometry_families" ADD CONSTRAINT "geometry_families_text_check" CHECK (btrim(name) <> ''::text AND btrim(formula_version) <> ''::text AND btrim(parameter_contract) <> ''::text);
ALTER TABLE ONLY "silomath"."geometry_profile_versions" ADD CONSTRAINT "geometry_profile_versions_capacity_check" CHECK (max_capacity_m3 > 0::numeric);
ALTER TABLE ONLY "silomath"."geometry_profile_versions" ADD CONSTRAINT "geometry_profile_versions_max_fill_height_check" CHECK (maximum_fill_height_m > 0::numeric);
ALTER TABLE ONLY "silomath"."geometry_profile_versions" ADD CONSTRAINT "geometry_profile_versions_number_check" CHECK (version_number >= 1);
ALTER TABLE ONLY "silomath"."geometry_profile_versions" ADD CONSTRAINT "geometry_profile_versions_pkey" PRIMARY KEY (geometry_profile_version_id);
ALTER TABLE ONLY "silomath"."geometry_profile_versions" ADD CONSTRAINT "geometry_profile_versions_reference_above_fill_check" CHECK (measurement_reference_height_m >= maximum_fill_height_m);
ALTER TABLE ONLY "silomath"."geometry_profile_versions" ADD CONSTRAINT "geometry_profile_versions_reference_height_check" CHECK (measurement_reference_height_m > 0::numeric);
ALTER TABLE ONLY "silomath"."geometry_profile_versions" ADD CONSTRAINT "geometry_profile_versions_text_check" CHECK (btrim(geometry_origin) <> ''::text AND btrim(formula_version) <> ''::text AND btrim(schema_version) <> ''::text);
ALTER TABLE ONLY "silomath"."geometry_profiles" ADD CONSTRAINT "geometry_profiles_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."geometry_profiles" ADD CONSTRAINT "geometry_profiles_lifecycle_status_check" CHECK (lifecycle_status = ANY (ARRAY['ACTIVE'::text, 'INACTIVE'::text]));
ALTER TABLE ONLY "silomath"."geometry_profiles" ADD CONSTRAINT "geometry_profiles_pkey" PRIMARY KEY (geometry_profile_id);
ALTER TABLE ONLY "silomath"."geometry_profiles" ADD CONSTRAINT "geometry_profiles_text_check" CHECK (btrim(name) <> ''::text AND btrim(code) <> ''::text);
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_code_format_check" CHECK (code ~ '^INV-[0-9]{4}-[0-9]{4}$'::text);
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_date_range_check" CHECK (planned_end_date >= start_date);
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_inventory_type_check" CHECK (inventory_type = ANY (ARRAY['ANNUAL'::text, 'MONTHLY'::text, 'CONTROL'::text]));
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_name_nonblank_check" CHECK (btrim(name) <> ''::text);
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_org_code_unique" UNIQUE (organization_id, code);
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_org_year_sequence_unique" UNIQUE (organization_id, campaign_year, sequence);
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_pkey" PRIMARY KEY (campaign_id);
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_sequence_check" CHECK (sequence >= 1);
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_status_check" CHECK (status = ANY (ARRAY['ACTIVE'::text, 'COMPLETED'::text]));
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_year_check" CHECK (campaign_year >= 2000);
ALTER TABLE ONLY "silomath"."issue_resolutions" ADD CONSTRAINT "issue_resolutions_context_check" CHECK (resolution_context = ANY (ARRAY['NORMAL'::text, 'CLOSE_FOLLOW_UP_ACCEPTANCE'::text]));
ALTER TABLE ONLY "silomath"."issue_resolutions" ADD CONSTRAINT "issue_resolutions_pkey" PRIMARY KEY (issue_resolution_id);
ALTER TABLE ONLY "silomath"."issue_resolutions" ADD CONSTRAINT "issue_resolutions_reason_check" CHECK (btrim(resolution_reason) <> ''::text);
ALTER TABLE ONLY "silomath"."issue_status_events" ADD CONSTRAINT "issue_status_events_pkey" PRIMARY KEY (issue_status_event_id);
ALTER TABLE ONLY "silomath"."issue_status_events" ADD CONSTRAINT "issue_status_events_reason_check" CHECK (from_status IS NULL OR reason IS NOT NULL AND btrim(reason) <> ''::text);
ALTER TABLE ONLY "silomath"."issue_status_events" ADD CONSTRAINT "issue_status_events_status_check" CHECK (from_status IS NULL AND to_status = 'OPEN'::text OR from_status = 'OPEN'::text AND to_status = 'RESOLVED'::text OR from_status = 'RESOLVED'::text AND to_status = 'OPEN'::text);
ALTER TABLE ONLY "silomath"."issues" ADD CONSTRAINT "issues_category_check" CHECK (category = ANY (ARRAY['MEASUREMENT_PROBLEM'::text, 'ACCESS_SAFETY'::text, 'GEOMETRY_DISCREPANCY'::text, 'COMMODITY_UNCERTAINTY'::text, 'STORAGE_SPACE_CONDITION'::text, 'OTHER'::text]));
ALTER TABLE ONLY "silomath"."issues" ADD CONSTRAINT "issues_description_check" CHECK (btrim(description) <> ''::text);
ALTER TABLE ONLY "silomath"."issues" ADD CONSTRAINT "issues_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."issues" ADD CONSTRAINT "issues_pkey" PRIMARY KEY (issue_id);
ALTER TABLE ONLY "silomath"."issues" ADD CONSTRAINT "issues_resolution_pair_check" CHECK (status = 'OPEN'::text AND resolved_at IS NULL AND resolved_by IS NULL OR status = 'RESOLVED'::text AND resolved_at IS NOT NULL AND resolved_by IS NOT NULL AND resolved_at >= reported_at);
ALTER TABLE ONLY "silomath"."issues" ADD CONSTRAINT "issues_status_check" CHECK (status = ANY (ARRAY['OPEN'::text, 'RESOLVED'::text]));
ALTER TABLE ONLY "silomath"."locations" ADD CONSTRAINT "locations_coordinate_pair_check" CHECK ((latitude_deg IS NULL) = (longitude_deg IS NULL));
ALTER TABLE ONLY "silomath"."locations" ADD CONSTRAINT "locations_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."locations" ADD CONSTRAINT "locations_latitude_range_check" CHECK (latitude_deg IS NULL OR latitude_deg >= '-90'::integer::numeric AND latitude_deg <= 90::numeric);
ALTER TABLE ONLY "silomath"."locations" ADD CONSTRAINT "locations_lifecycle_status_check" CHECK (lifecycle_status = ANY (ARRAY['ACTIVE'::text, 'INACTIVE'::text]));
ALTER TABLE ONLY "silomath"."locations" ADD CONSTRAINT "locations_longitude_range_check" CHECK (longitude_deg IS NULL OR longitude_deg >= '-180'::integer::numeric AND longitude_deg <= 180::numeric);
ALTER TABLE ONLY "silomath"."locations" ADD CONSTRAINT "locations_pkey" PRIMARY KEY (location_id);
ALTER TABLE ONLY "silomath"."locations" ADD CONSTRAINT "locations_text_check" CHECK (btrim(name) <> ''::text AND btrim(code) <> ''::text);
ALTER TABLE ONLY "silomath"."measurements" ADD CONSTRAINT "measurements_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."measurements" ADD CONSTRAINT "measurements_normalized_ullage_check" CHECK (normalized_ullage_m >= 0::numeric);
ALTER TABLE ONLY "silomath"."measurements" ADD CONSTRAINT "measurements_pkey" PRIMARY KEY (measurement_id);
ALTER TABLE ONLY "silomath"."measurements" ADD CONSTRAINT "measurements_raw_unit_check" CHECK (btrim(raw_unit) <> ''::text);
ALTER TABLE ONLY "silomath"."measurements" ADD CONSTRAINT "measurements_raw_value_check" CHECK (raw_value >= 0::numeric);
ALTER TABLE ONLY "silomath"."measurements" ADD CONSTRAINT "measurements_sequence_check" CHECK (sequence_number >= 1 AND sequence_number <= 5);
ALTER TABLE ONLY "silomath"."notifications" ADD CONSTRAINT "notifications_pkey" PRIMARY KEY (notification_id);
ALTER TABLE ONLY "silomath"."notifications" ADD CONSTRAINT "notifications_read_time_check" CHECK (read_at IS NULL OR read_at >= created_at);
ALTER TABLE ONLY "silomath"."notifications" ADD CONSTRAINT "notifications_text_check" CHECK (btrim(title) <> ''::text AND btrim(body) <> ''::text);
ALTER TABLE ONLY "silomath"."notifications" ADD CONSTRAINT "notifications_type_check" CHECK (notification_type = ANY (ARRAY['SYNC_ERROR'::text, 'SYNC_CONFLICT'::text, 'CAMPAIGN_ASSIGNMENT'::text, 'ISSUE_REQUIRES_ATTENTION'::text, 'CAMPAIGN_COMPLETED'::text]));
ALTER TABLE ONLY "silomath"."offline_working_set_manifests" ADD CONSTRAINT "offline_working_set_manifests_pkey" PRIMARY KEY (manifest_id);
ALTER TABLE ONLY "silomath"."organization_configurations" ADD CONSTRAINT "organization_configurations_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."organization_configurations" ADD CONSTRAINT "organization_configurations_key_check" CHECK (btrim(configuration_key) <> ''::text);
ALTER TABLE ONLY "silomath"."organization_configurations" ADD CONSTRAINT "organization_configurations_pkey" PRIMARY KEY (organization_configuration_id);
ALTER TABLE ONLY "silomath"."organization_configurations" ADD CONSTRAINT "organization_configurations_type_check" CHECK (value_type = ANY (ARRAY['BOOLEAN'::text, 'INTEGER'::text, 'DECIMAL'::text, 'TEXT'::text, 'ENUM'::text]));
ALTER TABLE ONLY "silomath"."organization_configurations" ADD CONSTRAINT "organization_configurations_typed_value_check" CHECK (value_type = 'BOOLEAN'::text AND boolean_value IS NOT NULL AND integer_value IS NULL AND decimal_value IS NULL AND text_value IS NULL AND enum_value IS NULL OR value_type = 'INTEGER'::text AND boolean_value IS NULL AND integer_value IS NOT NULL AND decimal_value IS NULL AND text_value IS NULL AND enum_value IS NULL OR value_type = 'DECIMAL'::text AND boolean_value IS NULL AND integer_value IS NULL AND decimal_value IS NOT NULL AND text_value IS NULL AND enum_value IS NULL OR value_type = 'TEXT'::text AND boolean_value IS NULL AND integer_value IS NULL AND decimal_value IS NULL AND text_value IS NOT NULL AND enum_value IS NULL OR value_type = 'ENUM'::text AND boolean_value IS NULL AND integer_value IS NULL AND decimal_value IS NULL AND text_value IS NULL AND enum_value IS NOT NULL);
ALTER TABLE ONLY "silomath"."organization_memberships" ADD CONSTRAINT "organization_memberships_disabled_pair_check" CHECK (lifecycle_status = 'ACTIVE'::text AND disabled_at IS NULL AND disabled_by IS NULL OR lifecycle_status = 'DISABLED'::text AND disabled_at IS NOT NULL AND disabled_by IS NOT NULL);
ALTER TABLE ONLY "silomath"."organization_memberships" ADD CONSTRAINT "organization_memberships_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."organization_memberships" ADD CONSTRAINT "organization_memberships_lifecycle_status_check" CHECK (lifecycle_status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text]));
ALTER TABLE ONLY "silomath"."organization_memberships" ADD CONSTRAINT "organization_memberships_pkey" PRIMARY KEY (membership_id);
ALTER TABLE ONLY "silomath"."organization_memberships" ADD CONSTRAINT "organization_memberships_role_check" CHECK (role = ANY (ARRAY['ADMIN'::text, 'STANDARD'::text]));
ALTER TABLE ONLY "silomath"."organizations" ADD CONSTRAINT "organizations_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."organizations" ADD CONSTRAINT "organizations_lifecycle_status_check" CHECK (lifecycle_status = ANY (ARRAY['ACTIVE'::text, 'INACTIVE'::text]));
ALTER TABLE ONLY "silomath"."organizations" ADD CONSTRAINT "organizations_pkey" PRIMARY KEY (organization_id);
ALTER TABLE ONLY "silomath"."organizations" ADD CONSTRAINT "organizations_text_check" CHECK (btrim(name) <> ''::text AND btrim(code) <> ''::text);
ALTER TABLE ONLY "silomath"."stocktaking_records" ADD CONSTRAINT "stocktaking_records_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."stocktaking_records" ADD CONSTRAINT "stocktaking_records_pkey" PRIMARY KEY (stocktaking_record_id);
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_client_semantics_check" CHECK (ownership IS NULL OR ownership = 'OWNED'::text AND client_id IS NULL OR ownership = 'THIRD_PARTY'::text AND client_id IS NOT NULL);
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_empty_semantics_check" CHECK (is_empty AND commodity_version_id IS NULL AND actual_bulk_density_kg_m3 IS NULL AND ownership IS NULL AND client_id IS NULL AND calculation_result_id IS NULL OR NOT is_empty AND commodity_version_id IS NOT NULL AND actual_bulk_density_kg_m3 > 0::numeric AND ownership IS NOT NULL);
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_no_self_base_check" CHECK (based_on_revision_id IS NULL OR based_on_revision_id <> stocktaking_revision_id);
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_number_check" CHECK (revision_number >= 1);
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_ownership_check" CHECK (ownership IS NULL OR (ownership = ANY (ARRAY['OWNED'::text, 'THIRD_PARTY'::text])));
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_pkey" PRIMARY KEY (stocktaking_revision_id);
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_state_check" CHECK (revision_state = ANY (ARRAY['WORKING'::text, 'VALIDATED'::text]));
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_validated_result_check" CHECK (revision_state <> 'VALIDATED'::text OR is_empty OR calculation_result_id IS NOT NULL);
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "stocktaking_revisions_validation_pair_check" CHECK (revision_state = 'WORKING'::text AND validated_at IS NULL AND validated_by IS NULL OR revision_state = 'VALIDATED'::text AND validated_at IS NOT NULL AND validated_by IS NOT NULL);
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "storage_spaces_canonical_code_md005" CHECK (char_length(code) >= 1 AND code = upper(btrim(code)));
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "storage_spaces_code_check" CHECK (btrim(code) <> ''::text);
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "storage_spaces_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "storage_spaces_lifecycle_status_check" CHECK (lifecycle_status = ANY (ARRAY['ACTIVE'::text, 'INACTIVE'::text]));
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "storage_spaces_pkey" PRIMARY KEY (storage_space_id);
ALTER TABLE ONLY "silomath"."storage_units" ADD CONSTRAINT "storage_units_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."storage_units" ADD CONSTRAINT "storage_units_field_limits_v6" CHECK (char_length(name) >= 1 AND char_length(name) <= 150 AND name = btrim(name) AND char_length(code) >= 1 AND char_length(code) <= 50 AND code = upper(btrim(code)) AND (description IS NULL OR char_length(description) <= 500));
ALTER TABLE ONLY "silomath"."storage_units" ADD CONSTRAINT "storage_units_lifecycle_status_check" CHECK (lifecycle_status = ANY (ARRAY['ACTIVE'::text, 'INACTIVE'::text]));
ALTER TABLE ONLY "silomath"."storage_units" ADD CONSTRAINT "storage_units_pkey" PRIMARY KEY (storage_unit_id);
ALTER TABLE ONLY "silomath"."storage_units" ADD CONSTRAINT "storage_units_text_check" CHECK (btrim(name) <> ''::text AND btrim(code) <> ''::text);
ALTER TABLE ONLY "silomath"."sync_change_stream" ADD CONSTRAINT "sync_change_stream_change_type_check" CHECK (change_type = ANY (ARRAY['UPSERT'::text, 'DELETE'::text]));
ALTER TABLE ONLY "silomath"."sync_change_stream" ADD CONSTRAINT "sync_change_stream_entity_type_check" CHECK (btrim(entity_type) <> ''::text);
ALTER TABLE ONLY "silomath"."sync_change_stream" ADD CONSTRAINT "sync_change_stream_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."sync_change_stream" ADD CONSTRAINT "sync_change_stream_pkey" PRIMARY KEY (change_sequence);
ALTER TABLE ONLY "silomath"."sync_conflicts" ADD CONSTRAINT "sync_conflicts_base_version_check" CHECK (base_entity_version IS NULL OR base_entity_version >= 1);
ALTER TABLE ONLY "silomath"."sync_conflicts" ADD CONSTRAINT "sync_conflicts_pkey" PRIMARY KEY (sync_conflict_id);
ALTER TABLE ONLY "silomath"."sync_conflicts" ADD CONSTRAINT "sync_conflicts_resolution_state_check" CHECK (status = 'OPEN'::text AND resolution_type IS NULL AND resolved_at IS NULL AND resolved_by IS NULL OR status = 'RESOLVED'::text AND resolution_type IS NOT NULL AND resolved_at IS NOT NULL AND resolved_by IS NOT NULL AND resolved_at >= detected_at);
ALTER TABLE ONLY "silomath"."sync_conflicts" ADD CONSTRAINT "sync_conflicts_resolution_type_check" CHECK (resolution_type IS NULL OR (resolution_type = ANY (ARRAY['RELOAD_AND_REENTER'::text, 'DISCARD_LOCAL_ATTEMPT'::text, 'NO_ACTION_REQUIRED'::text])));
ALTER TABLE ONLY "silomath"."sync_conflicts" ADD CONSTRAINT "sync_conflicts_server_version_check" CHECK (server_entity_version IS NULL OR server_entity_version >= 1);
ALTER TABLE ONLY "silomath"."sync_conflicts" ADD CONSTRAINT "sync_conflicts_status_check" CHECK (status = ANY (ARRAY['OPEN'::text, 'RESOLVED'::text]));
ALTER TABLE ONLY "silomath"."sync_operation_dependencies" ADD CONSTRAINT "sync_dependencies_no_self_check" CHECK (parent_operation_id <> dependent_operation_id);
ALTER TABLE ONLY "silomath"."sync_operation_dependencies" ADD CONSTRAINT "sync_dependencies_type_check" CHECK (dependency_type = 'REQUIRED_BEFORE'::text);
ALTER TABLE ONLY "silomath"."sync_operation_dependencies" ADD CONSTRAINT "sync_operation_dependencies_pkey" PRIMARY KEY (dependency_id);
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_acknowledgement_check" CHECK ((state = 'ACKNOWLEDGED'::text) = (acknowledged_at IS NOT NULL));
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_attempt_count_check" CHECK (attempt_count >= 0);
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_authoritative_version_check" CHECK (authoritative_entity_version IS NULL OR state = 'ACKNOWLEDGED'::text AND authoritative_entity_version >= 1);
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_base_version_check" CHECK (base_entity_version IS NULL OR base_entity_version >= 1);
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_last_attempt_check" CHECK (last_attempt_at IS NULL OR attempt_count > 0);
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_next_attempt_check" CHECK (next_attempt_at IS NULL OR state = 'RETRYABLE_ERROR'::text);
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_pkey" PRIMARY KEY (sync_operation_id);
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_state_check" CHECK (state = ANY (ARRAY['PENDING'::text, 'IN_FLIGHT'::text, 'ACKNOWLEDGED'::text, 'RETRYABLE_ERROR'::text, 'CONFLICT'::text, 'PERMANENT_ERROR'::text]));
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_text_check" CHECK (btrim(entity_type) <> ''::text AND btrim(operation_type) <> ''::text AND btrim(idempotency_key) <> ''::text);
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "sync_operations_updated_time_check" CHECK (updated_at >= created_at);
ALTER TABLE ONLY "silomath"."user_preferences" ADD CONSTRAINT "user_preferences_pkey" PRIMARY KEY (user_preference_id);
ALTER TABLE ONLY "silomath"."users" ADD CONSTRAINT "users_entity_version_check" CHECK (entity_version >= 1);
ALTER TABLE ONLY "silomath"."users" ADD CONSTRAINT "users_lifecycle_status_check" CHECK (lifecycle_status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text]));
ALTER TABLE ONLY "silomath"."users" ADD CONSTRAINT "users_pkey" PRIMARY KEY (user_id);
ALTER TABLE ONLY "silomath"."users" ADD CONSTRAINT "users_text_check" CHECK (btrim(full_name) <> ''::text AND btrim(email) <> ''::text AND btrim(email_normalized) <> ''::text);

-- Foreign-key constraints
ALTER TABLE ONLY "silomath"."audit_events" ADD CONSTRAINT "fk_audit_events_campaign_id" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."audit_events" ADD CONSTRAINT "fk_audit_events_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "fk_calculation_results_geometry_family_id" FOREIGN KEY (geometry_family_id) REFERENCES silomath.geometry_families(geometry_family_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "fk_calculation_results_geometry_profile_version_id" FOREIGN KEY (geometry_profile_version_id) REFERENCES silomath.geometry_profile_versions(geometry_profile_version_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."calculation_results" ADD CONSTRAINT "fk_calculation_results_stocktaking_revision_id" FOREIGN KEY (stocktaking_revision_id) REFERENCES silomath.stocktaking_revisions(stocktaking_revision_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_scopes" ADD CONSTRAINT "campaign_scopes_campaign_fkey" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT;
ALTER TABLE ONLY "silomath"."campaign_scopes" ADD CONSTRAINT "fk_campaign_scopes_storage_space_id" FOREIGN KEY (storage_space_id) REFERENCES silomath.storage_spaces(storage_space_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_commodity_items" ADD CONSTRAINT "fk_campaign_snapshot_commodity_items_campaign_snapshot_id" FOREIGN KEY (campaign_snapshot_id) REFERENCES silomath.campaign_snapshots(campaign_snapshot_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_commodity_items" ADD CONSTRAINT "fk_campaign_snapshot_commodity_items_commodity_id" FOREIGN KEY (commodity_id) REFERENCES silomath.commodities(commodity_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_commodity_items" ADD CONSTRAINT "fk_campaign_snapshot_commodity_items_commodity_version_id" FOREIGN KEY (commodity_version_id) REFERENCES silomath.commodity_versions(commodity_version_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_scope_items" ADD CONSTRAINT "fk_campaign_snapshot_scope_items_campaign_snapshot_id" FOREIGN KEY (campaign_snapshot_id) REFERENCES silomath.campaign_snapshots(campaign_snapshot_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_scope_items" ADD CONSTRAINT "fk_campaign_snapshot_scope_items_geometry_family_id" FOREIGN KEY (geometry_family_id) REFERENCES silomath.geometry_families(geometry_family_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_scope_items" ADD CONSTRAINT "fk_campaign_snapshot_scope_items_geometry_profile_id" FOREIGN KEY (geometry_profile_id) REFERENCES silomath.geometry_profiles(geometry_profile_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_scope_items" ADD CONSTRAINT "fk_campaign_snapshot_scope_items_geometry_profile_version_id" FOREIGN KEY (geometry_profile_version_id) REFERENCES silomath.geometry_profile_versions(geometry_profile_version_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_scope_items" ADD CONSTRAINT "fk_campaign_snapshot_scope_items_location_id" FOREIGN KEY (location_id) REFERENCES silomath.locations(location_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_scope_items" ADD CONSTRAINT "fk_campaign_snapshot_scope_items_storage_space_id" FOREIGN KEY (storage_space_id) REFERENCES silomath.storage_spaces(storage_space_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_scope_items" ADD CONSTRAINT "fk_campaign_snapshot_scope_items_storage_unit_id" FOREIGN KEY (storage_unit_id) REFERENCES silomath.storage_units(storage_unit_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_team_items" ADD CONSTRAINT "fk_campaign_snapshot_team_items_campaign_snapshot_id" FOREIGN KEY (campaign_snapshot_id) REFERENCES silomath.campaign_snapshots(campaign_snapshot_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_team_items" ADD CONSTRAINT "fk_campaign_snapshot_team_items_organization_membership_id" FOREIGN KEY (organization_membership_id) REFERENCES silomath.organization_memberships(membership_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshot_team_items" ADD CONSTRAINT "fk_campaign_snapshot_team_items_user_id" FOREIGN KEY (user_id) REFERENCES silomath.users(user_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_snapshots" ADD CONSTRAINT "campaign_snapshots_campaign_fkey" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT;
ALTER TABLE ONLY "silomath"."campaign_snapshots" ADD CONSTRAINT "fk_campaign_snapshots_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."campaign_team_assignments" ADD CONSTRAINT "campaign_team_assignments_campaign_fkey" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT;
ALTER TABLE ONLY "silomath"."campaign_team_assignments" ADD CONSTRAINT "fk_campaign_team_assignments_user_id" FOREIGN KEY (user_id) REFERENCES silomath.users(user_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."clients" ADD CONSTRAINT "fk_clients_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."commodities" ADD CONSTRAINT "fk_commodities_current_version_id" FOREIGN KEY (current_version_id) REFERENCES silomath.commodity_versions(commodity_version_id) ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE ONLY "silomath"."commodity_versions" ADD CONSTRAINT "fk_commodity_versions_commodity_id" FOREIGN KEY (commodity_id) REFERENCES silomath.commodities(commodity_id) ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED NOT VALID;
ALTER TABLE ONLY "silomath"."device_installations" ADD CONSTRAINT "fk_device_installations_user_id" FOREIGN KEY (user_id) REFERENCES silomath.users(user_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "fk_evidence_campaign_id" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "fk_evidence_issue_id" FOREIGN KEY (issue_id) REFERENCES silomath.issues(issue_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "fk_evidence_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "fk_evidence_stocktaking_record_id" FOREIGN KEY (stocktaking_record_id) REFERENCES silomath.stocktaking_records(stocktaking_record_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."evidence" ADD CONSTRAINT "fk_evidence_stocktaking_revision_id" FOREIGN KEY (stocktaking_revision_id) REFERENCES silomath.stocktaking_revisions(stocktaking_revision_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."final_inventory_snapshot_entries" ADD CONSTRAINT "fk_final_inventory_snapshot_entries_final_snapshot_id" FOREIGN KEY (final_snapshot_id) REFERENCES silomath.final_inventory_snapshots(final_snapshot_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."final_inventory_snapshot_entries" ADD CONSTRAINT "fk_final_inventory_snapshot_entries_storage_space_id" FOREIGN KEY (storage_space_id) REFERENCES silomath.storage_spaces(storage_space_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."final_inventory_snapshots" ADD CONSTRAINT "fk_final_inventory_snapshots_campaign_id" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."final_inventory_snapshots" ADD CONSTRAINT "fk_final_inventory_snapshots_campaign_snapshot_id" FOREIGN KEY (campaign_snapshot_id) REFERENCES silomath.campaign_snapshots(campaign_snapshot_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."final_inventory_snapshots" ADD CONSTRAINT "fk_final_inventory_snapshots_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."geometry_profile_versions" ADD CONSTRAINT "fk_geometry_profile_versions_geometry_family_id" FOREIGN KEY (geometry_family_id) REFERENCES silomath.geometry_families(geometry_family_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."geometry_profile_versions" ADD CONSTRAINT "fk_geometry_profile_versions_geometry_profile_id" FOREIGN KEY (geometry_profile_id) REFERENCES silomath.geometry_profiles(geometry_profile_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."geometry_profiles" ADD CONSTRAINT "fk_geometry_profiles_current_version_id" FOREIGN KEY (current_version_id) REFERENCES silomath.geometry_profile_versions(geometry_profile_version_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."geometry_profiles" ADD CONSTRAINT "fk_geometry_profiles_geometry_family_id" FOREIGN KEY (geometry_family_id) REFERENCES silomath.geometry_families(geometry_family_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."geometry_profiles" ADD CONSTRAINT "fk_geometry_profiles_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "fk_inventory_campaigns_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_completed_by_fkey" FOREIGN KEY (completed_by) REFERENCES silomath.users(user_id) ON DELETE RESTRICT;
ALTER TABLE ONLY "silomath"."inventory_campaigns" ADD CONSTRAINT "inventory_campaigns_snapshot_fkey" FOREIGN KEY (campaign_snapshot_id) REFERENCES silomath.campaign_snapshots(campaign_snapshot_id) ON DELETE RESTRICT;
ALTER TABLE ONLY "silomath"."issue_resolutions" ADD CONSTRAINT "fk_issue_resolutions_audit_event_id" FOREIGN KEY (audit_event_id) REFERENCES silomath.audit_events(audit_event_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."issue_resolutions" ADD CONSTRAINT "fk_issue_resolutions_issue_id" FOREIGN KEY (issue_id) REFERENCES silomath.issues(issue_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."issue_status_events" ADD CONSTRAINT "fk_issue_status_events_audit_event_id" FOREIGN KEY (audit_event_id) REFERENCES silomath.audit_events(audit_event_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."issue_status_events" ADD CONSTRAINT "fk_issue_status_events_issue_id" FOREIGN KEY (issue_id) REFERENCES silomath.issues(issue_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."issues" ADD CONSTRAINT "fk_issues_campaign_id" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."issues" ADD CONSTRAINT "fk_issues_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."issues" ADD CONSTRAINT "fk_issues_stocktaking_record_id" FOREIGN KEY (stocktaking_record_id) REFERENCES silomath.stocktaking_records(stocktaking_record_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."locations" ADD CONSTRAINT "fk_locations_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."measurements" ADD CONSTRAINT "fk_measurements_geometry_profile_version_id" FOREIGN KEY (geometry_profile_version_id) REFERENCES silomath.geometry_profile_versions(geometry_profile_version_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."measurements" ADD CONSTRAINT "fk_measurements_stocktaking_revision_id" FOREIGN KEY (stocktaking_revision_id) REFERENCES silomath.stocktaking_revisions(stocktaking_revision_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."notifications" ADD CONSTRAINT "fk_notifications_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."notifications" ADD CONSTRAINT "fk_notifications_recipient_user_id" FOREIGN KEY (recipient_user_id) REFERENCES silomath.users(user_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."offline_working_set_manifests" ADD CONSTRAINT "fk_offline_working_set_manifests_campaign_id" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."offline_working_set_manifests" ADD CONSTRAINT "fk_offline_working_set_manifests_device_installation_id" FOREIGN KEY (device_installation_id) REFERENCES silomath.device_installations(device_installation_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."offline_working_set_manifests" ADD CONSTRAINT "fk_offline_working_set_manifests_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."organization_configurations" ADD CONSTRAINT "fk_organization_configurations_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."organization_memberships" ADD CONSTRAINT "fk_organization_memberships_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."organization_memberships" ADD CONSTRAINT "fk_organization_memberships_user_id" FOREIGN KEY (user_id) REFERENCES silomath.users(user_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."stocktaking_records" ADD CONSTRAINT "fk_stocktaking_records_campaign_id" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."stocktaking_records" ADD CONSTRAINT "fk_stocktaking_records_current_revision_id" FOREIGN KEY (current_revision_id) REFERENCES silomath.stocktaking_revisions(stocktaking_revision_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."stocktaking_records" ADD CONSTRAINT "fk_stocktaking_records_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."stocktaking_records" ADD CONSTRAINT "fk_stocktaking_records_storage_space_id" FOREIGN KEY (storage_space_id) REFERENCES silomath.storage_spaces(storage_space_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "fk_stocktaking_revisions_based_on_revision_id" FOREIGN KEY (based_on_revision_id) REFERENCES silomath.stocktaking_revisions(stocktaking_revision_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "fk_stocktaking_revisions_calculation_result_id" FOREIGN KEY (calculation_result_id) REFERENCES silomath.calculation_results(calculation_result_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "fk_stocktaking_revisions_client_id" FOREIGN KEY (client_id) REFERENCES silomath.clients(client_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "fk_stocktaking_revisions_commodity_version_id" FOREIGN KEY (commodity_version_id) REFERENCES silomath.commodity_versions(commodity_version_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."stocktaking_revisions" ADD CONSTRAINT "fk_stocktaking_revisions_stocktaking_record_id" FOREIGN KEY (stocktaking_record_id) REFERENCES silomath.stocktaking_records(stocktaking_record_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "fk_storage_spaces_geometry_organization_md005" FOREIGN KEY (geometry_profile_id, organization_id) REFERENCES silomath.geometry_profiles(geometry_profile_id, organization_id) ON DELETE RESTRICT;
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "fk_storage_spaces_geometry_profile_id" FOREIGN KEY (geometry_profile_id) REFERENCES silomath.geometry_profiles(geometry_profile_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "fk_storage_spaces_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "fk_storage_spaces_storage_unit_id" FOREIGN KEY (storage_unit_id) REFERENCES silomath.storage_units(storage_unit_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."storage_spaces" ADD CONSTRAINT "fk_storage_spaces_unit_organization_md005" FOREIGN KEY (storage_unit_id, organization_id) REFERENCES silomath.storage_units(storage_unit_id, organization_id) ON DELETE RESTRICT;
ALTER TABLE ONLY "silomath"."storage_units" ADD CONSTRAINT "fk_storage_units_location_id" FOREIGN KEY (location_id) REFERENCES silomath.locations(location_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."storage_units" ADD CONSTRAINT "fk_storage_units_location_organization_md003" FOREIGN KEY (location_id, organization_id) REFERENCES silomath.locations(location_id, organization_id) ON DELETE RESTRICT;
ALTER TABLE ONLY "silomath"."storage_units" ADD CONSTRAINT "fk_storage_units_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."sync_conflicts" ADD CONSTRAINT "fk_sync_conflicts_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."sync_conflicts" ADD CONSTRAINT "fk_sync_conflicts_sync_operation_id" FOREIGN KEY (sync_operation_id) REFERENCES silomath.sync_operations(sync_operation_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."sync_operation_dependencies" ADD CONSTRAINT "fk_sync_operation_dependencies_dependent_operation_id" FOREIGN KEY (dependent_operation_id) REFERENCES silomath.sync_operations(sync_operation_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."sync_operation_dependencies" ADD CONSTRAINT "fk_sync_operation_dependencies_parent_operation_id" FOREIGN KEY (parent_operation_id) REFERENCES silomath.sync_operations(sync_operation_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "fk_sync_operations_campaign_id" FOREIGN KEY (campaign_id) REFERENCES silomath.inventory_campaigns(campaign_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."sync_operations" ADD CONSTRAINT "fk_sync_operations_organization_id" FOREIGN KEY (organization_id) REFERENCES silomath.organizations(organization_id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ONLY "silomath"."user_preferences" ADD CONSTRAINT "fk_user_preferences_user_id" FOREIGN KEY (user_id) REFERENCES silomath.users(user_id) ON DELETE RESTRICT NOT VALID;

-- Secondary / partial indexes
CREATE INDEX api_idempotency_records_created_at_idx ON silomath.api_idempotency_records USING btree (created_at);
CREATE UNIQUE INDEX api_idempotency_records_scope_uq ON silomath.api_idempotency_records USING btree (actor_user_id, COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid), operation_code, idempotency_key);
CREATE INDEX idx_audit_events_entity_occurred_at_desc ON silomath.audit_events USING btree (entity_type, entity_id, occurred_at DESC);
CREATE INDEX idx_audit_events_org_occurred_at_desc ON silomath.audit_events USING btree (organization_id, occurred_at DESC);
CREATE INDEX idx_calculation_results_revision_calculated_at ON silomath.calculation_results USING btree (stocktaking_revision_id, calculated_at);
CREATE INDEX campaign_scopes_campaign_idx ON silomath.campaign_scopes USING btree (campaign_id);
CREATE INDEX campaign_team_assignments_campaign_idx ON silomath.campaign_team_assignments USING btree (campaign_id);
CREATE UNIQUE INDEX campaign_team_assignments_one_active_user_idx ON silomath.campaign_team_assignments USING btree (campaign_id, user_id) WHERE (status = 'ACTIVE'::text);
CREATE INDEX idx_clients_org_lifecycle_code ON silomath.clients USING btree (organization_id, lifecycle_status, code);
CREATE UNIQUE INDEX uq_clients_org_code ON silomath.clients USING btree (organization_id, code);
CREATE UNIQUE INDEX uq_commodities_code ON silomath.commodities USING btree (code);
CREATE UNIQUE INDEX uq_commodity_versions_commodity_version ON silomath.commodity_versions USING btree (commodity_id, version_number);
CREATE INDEX idx_evidence_issue_type ON silomath.evidence USING btree (issue_id, evidence_type);
CREATE INDEX idx_evidence_record_revision_type ON silomath.evidence USING btree (stocktaking_record_id, stocktaking_revision_id, evidence_type);
CREATE INDEX idx_evidence_sync_state ON silomath.evidence USING btree (sync_state);
CREATE UNIQUE INDEX uq_final_snapshot_entries_snapshot_space ON silomath.final_inventory_snapshot_entries USING btree (final_snapshot_id, storage_space_id);
CREATE INDEX idx_final_snapshots_org_closed_at ON silomath.final_inventory_snapshots USING btree (organization_id, closed_at);
CREATE UNIQUE INDEX uq_final_inventory_snapshots_campaign ON silomath.final_inventory_snapshots USING btree (campaign_id);
CREATE UNIQUE INDEX uq_geometry_families_family_code ON silomath.geometry_families USING btree (family_code);
CREATE UNIQUE INDEX uq_geometry_profile_versions_profile_version ON silomath.geometry_profile_versions USING btree (geometry_profile_id, version_number);
CREATE INDEX idx_geometry_profiles_org_lifecycle_code ON silomath.geometry_profiles USING btree (organization_id, lifecycle_status, code);
CREATE UNIQUE INDEX uq_geometry_profiles_identity_organization_md005 ON silomath.geometry_profiles USING btree (geometry_profile_id, organization_id);
CREATE UNIQUE INDEX uq_geometry_profiles_org_code ON silomath.geometry_profiles USING btree (organization_id, code);
CREATE INDEX idx_inventory_campaigns_org_status_start_date ON silomath.inventory_campaigns USING btree (organization_id, status, start_date);
CREATE INDEX idx_inventory_campaigns_org_year_code ON silomath.inventory_campaigns USING btree (organization_id, campaign_year, code);
CREATE INDEX inventory_campaigns_org_status_idx ON silomath.inventory_campaigns USING btree (organization_id, status);
CREATE INDEX inventory_campaigns_org_type_idx ON silomath.inventory_campaigns USING btree (organization_id, inventory_type);
CREATE INDEX idx_issues_record_status ON silomath.issues USING btree (stocktaking_record_id, status);
CREATE INDEX idx_locations_org_coordinates ON silomath.locations USING btree (organization_id, latitude_deg, longitude_deg) WHERE ((latitude_deg IS NOT NULL) AND (longitude_deg IS NOT NULL));
CREATE INDEX idx_locations_org_lifecycle_code ON silomath.locations USING btree (organization_id, lifecycle_status, code);
CREATE UNIQUE INDEX uq_locations_identity_organization_md003 ON silomath.locations USING btree (location_id, organization_id);
CREATE UNIQUE INDEX uq_locations_org_code ON silomath.locations USING btree (organization_id, code);
CREATE UNIQUE INDEX uq_measurements_revision_sequence ON silomath.measurements USING btree (stocktaking_revision_id, sequence_number);
CREATE INDEX idx_notifications_recipient_read_created_desc ON silomath.notifications USING btree (recipient_user_id, read_at, created_at DESC);
CREATE UNIQUE INDEX uq_offline_manifests_campaign_device_version ON silomath.offline_working_set_manifests USING btree (campaign_id, device_installation_id, package_version);
CREATE UNIQUE INDEX uq_offline_manifests_one_current ON silomath.offline_working_set_manifests USING btree (campaign_id, device_installation_id) WHERE is_current;
CREATE UNIQUE INDEX uq_organization_configurations_org_key ON silomath.organization_configurations USING btree (organization_id, configuration_key);
CREATE UNIQUE INDEX uq_organization_memberships_org_user ON silomath.organization_memberships USING btree (organization_id, user_id);
CREATE UNIQUE INDEX uq_organizations_code ON silomath.organizations USING btree (code);
CREATE UNIQUE INDEX uq_stocktaking_records_campaign_space ON silomath.stocktaking_records USING btree (campaign_id, storage_space_id);
CREATE INDEX idx_stocktaking_revisions_root_revision_desc ON silomath.stocktaking_revisions USING btree (stocktaking_record_id, revision_number DESC);
CREATE UNIQUE INDEX uq_stocktaking_revisions_root_revision ON silomath.stocktaking_revisions USING btree (stocktaking_record_id, revision_number);
CREATE INDEX idx_storage_spaces_unit_lifecycle_code ON silomath.storage_spaces USING btree (storage_unit_id, lifecycle_status, code);
CREATE UNIQUE INDEX uq_storage_spaces_unit_code ON silomath.storage_spaces USING btree (storage_unit_id, code);
CREATE INDEX idx_storage_units_location_lifecycle_code ON silomath.storage_units USING btree (location_id, lifecycle_status, code);
CREATE UNIQUE INDEX uq_storage_units_identity_organization_md005 ON silomath.storage_units USING btree (storage_unit_id, organization_id);
CREATE UNIQUE INDEX uq_storage_units_location_code ON silomath.storage_units USING btree (location_id, code);
CREATE INDEX ix_sync_change_stream_org_campaign_sequence ON silomath.sync_change_stream USING btree (organization_id, campaign_id, change_sequence);
CREATE INDEX ix_sync_change_stream_org_sequence ON silomath.sync_change_stream USING btree (organization_id, change_sequence);
CREATE INDEX idx_sync_dependencies_dependent ON silomath.sync_operation_dependencies USING btree (dependent_operation_id);
CREATE UNIQUE INDEX uq_sync_dependencies_parent_dependent ON silomath.sync_operation_dependencies USING btree (parent_operation_id, dependent_operation_id);
CREATE INDEX idx_sync_operations_entity ON silomath.sync_operations USING btree (entity_type, entity_id);
CREATE INDEX idx_sync_operations_state_created_at ON silomath.sync_operations USING btree (state, created_at);
CREATE UNIQUE INDEX uq_sync_operations_idempotency_key ON silomath.sync_operations USING btree (idempotency_key);
CREATE UNIQUE INDEX uq_user_preferences_user ON silomath.user_preferences USING btree (user_id);
CREATE UNIQUE INDEX uq_users_email_normalized ON silomath.users USING btree (email_normalized);

-- Canonical functions / RPC
CREATE OR REPLACE FUNCTION silomath.assert_expected_entity_version(p_expected_entity_version bigint, p_actual_entity_version bigint)
 RETURNS void
 LANGUAGE plpgsql
 SET search_path TO ''
AS $function$
begin
    if p_expected_entity_version is null then
        raise exception 'expected entity version is required'
            using errcode = '22023';
    end if;

    if p_actual_entity_version is null then
        raise exception 'actual entity version is required'
            using errcode = '22023';
    end if;

    if p_expected_entity_version <> p_actual_entity_version then
        raise exception 'entity version conflict'
            using errcode = '40001';
    end if;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."assert_expected_entity_version"(p_expected_entity_version bigint, p_actual_entity_version bigint) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."assert_expected_entity_version"(p_expected_entity_version bigint, p_actual_entity_version bigint) TO postgres;

CREATE OR REPLACE FUNCTION silomath.campaign_team_to_json(p silomath.campaign_team_assignments)
 RETURNS jsonb
 LANGUAGE sql
 STABLE
 SET search_path TO ''
AS $function$
select jsonb_build_object('id',p.assignment_id,'campaignId',p.campaign_id,
 'userId',p.user_id,'organizationMembershipId',m.membership_id,'assignmentStatus',p.status,
 'assignedAt',p.assigned_at,'assignedBy',p.assigned_by,
 'removedAt',p.removed_at,'removedBy',p.removed_by,'createdAt',p.created_at,
 'createdBy',p.created_by,'updatedAt',p.updated_at,'updatedBy',p.updated_by,'entityVersion',p.entity_version)
from silomath.inventory_campaigns c join silomath.organization_memberships m
 on m.organization_id=c.organization_id and m.user_id=p.user_id where c.campaign_id=p.campaign_id;
$function$;
REVOKE ALL ON FUNCTION "silomath"."campaign_team_to_json"(p silomath.campaign_team_assignments) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."campaign_team_to_json"(p silomath.campaign_team_assignments) TO postgres;

CREATE OR REPLACE FUNCTION silomath.claim_idempotency_key(p_actor_user_id uuid, p_organization_id uuid, p_operation_code text, p_idempotency_key text, p_request_fingerprint text)
 RETURNS TABLE(disposition text, response_status integer, response_body jsonb)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_record silomath.api_idempotency_records%rowtype;
begin
    if p_actor_user_id is null then
        raise exception 'actor user id is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_operation_code), '') is null then
        raise exception 'operation code is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_idempotency_key), '') is null then
        raise exception 'idempotency key is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_request_fingerprint), '') is null then
        raise exception 'request fingerprint is required'
            using errcode = '22023';
    end if;

    insert into silomath.api_idempotency_records (
        actor_user_id,
        organization_id,
        operation_code,
        idempotency_key,
        request_fingerprint
    )
    values (
        p_actor_user_id,
        p_organization_id,
        btrim(p_operation_code),
        btrim(p_idempotency_key),
        btrim(p_request_fingerprint)
    )
    on conflict (
        actor_user_id,
        (coalesce(organization_id, '00000000-0000-0000-0000-000000000000'::uuid)),
        operation_code,
        idempotency_key
    )
    do nothing;

    select *
    into v_record
    from silomath.api_idempotency_records r
    where r.actor_user_id = p_actor_user_id
      and r.organization_id is not distinct from p_organization_id
      and r.operation_code = btrim(p_operation_code)
      and r.idempotency_key = btrim(p_idempotency_key)
    for update;

    if v_record.request_fingerprint <> btrim(p_request_fingerprint) then
        raise exception 'idempotency key payload conflict'
            using errcode = '23505';
    end if;

    if v_record.state = 'COMPLETED' then
        return query
        select
            'REPLAY'::text,
            v_record.response_status,
            v_record.response_body;
        return;
    end if;

    return query
    select
        'EXECUTE'::text,
        null::integer,
        null::jsonb;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."claim_idempotency_key"(p_actor_user_id uuid, p_organization_id uuid, p_operation_code text, p_idempotency_key text, p_request_fingerprint text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."claim_idempotency_key"(p_actor_user_id uuid, p_organization_id uuid, p_operation_code text, p_idempotency_key text, p_request_fingerprint text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.client_to_json(p silomath.clients)
 RETURNS jsonb
 LANGUAGE sql
 STABLE
 SET search_path TO ''
AS $function$
select jsonb_build_object(
 'id',p.client_id,'organizationId',p.organization_id,'name',p.name,
 'code',p.code,'registrationTaxId',p.tax_id,'notes',p.notes,
 'lifecycleStatus',p.lifecycle_status,'createdAt',p.created_at,
 'createdBy',p.created_by,'updatedAt',p.updated_at,'updatedBy',p.updated_by,
 'entityVersion',p.entity_version);
$function$;
REVOKE ALL ON FUNCTION "silomath"."client_to_json"(p silomath.clients) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."client_to_json"(p silomath.clients) TO postgres;

CREATE OR REPLACE FUNCTION silomath.complete_idempotency_key(p_actor_user_id uuid, p_organization_id uuid, p_operation_code text, p_idempotency_key text, p_request_fingerprint text, p_response_status integer, p_response_body jsonb)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_updated integer;
begin
    if p_response_status is null then
        raise exception 'response status is required'
            using errcode = '22023';
    end if;

    if p_response_body is null then
        raise exception 'response body is required'
            using errcode = '22023';
    end if;

    update silomath.api_idempotency_records r
    set state = 'COMPLETED',
        response_status = p_response_status,
        response_body = p_response_body,
        completed_at = now()
    where r.actor_user_id = p_actor_user_id
      and r.organization_id is not distinct from p_organization_id
      and r.operation_code = btrim(p_operation_code)
      and r.idempotency_key = btrim(p_idempotency_key)
      and r.request_fingerprint = btrim(p_request_fingerprint)
      and r.state = 'IN_PROGRESS';

    get diagnostics v_updated = row_count;

    if v_updated <> 1 then
        raise exception 'idempotency claim not found or already completed'
            using errcode = 'P0001';
    end if;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."complete_idempotency_key"(p_actor_user_id uuid, p_organization_id uuid, p_operation_code text, p_idempotency_key text, p_request_fingerprint text, p_response_status integer, p_response_body jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."complete_idempotency_key"(p_actor_user_id uuid, p_organization_id uuid, p_operation_code text, p_idempotency_key text, p_request_fingerprint text, p_response_status integer, p_response_body jsonb) TO postgres;

CREATE OR REPLACE FUNCTION silomath.enforce_active_campaign_write()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO ''
AS $function$
declare campaign uuid; campaign_status text;
begin
 if tg_table_name='stocktaking_revisions' then
 select campaign_id into campaign from silomath.stocktaking_records
 where stocktaking_record_id=new.stocktaking_record_id;
 else campaign:=new.campaign_id;
 end if;
 select status into campaign_status from silomath.inventory_campaigns
 where campaign_id=campaign for share;
 if campaign_status is distinct from 'ACTIVE' then
 raise exception 'completed Campaign is immutable' using errcode='55000'; end if;
 return new;
end $function$;
REVOKE ALL ON FUNCTION "silomath"."enforce_active_campaign_write"() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."enforce_active_campaign_write"() TO postgres;

CREATE OR REPLACE FUNCTION silomath.enforce_entity_version_increment()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'pg_catalog'
AS $function$
begin
    if new.entity_version is distinct from old.entity_version then
        raise exception 'entity_version is server-controlled for %.%', tg_table_schema, tg_table_name
            using errcode = '22023';
    end if;

    new.entity_version := old.entity_version + 1;
    return new;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."enforce_entity_version_increment"() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."enforce_entity_version_increment"() TO postgres;

CREATE OR REPLACE FUNCTION silomath.evidence_to_json(p_evidence silomath.evidence)
 RETURNS jsonb
 LANGUAGE sql
 STABLE
 SET search_path TO ''
AS $function$
  select jsonb_build_object(
    'evidenceId', p_evidence.evidence_id,
    'organizationId', p_evidence.organization_id,
    'campaignId', p_evidence.campaign_id,
    'stocktakingRecordId', p_evidence.stocktaking_record_id,
    'issueId', p_evidence.issue_id,
    'stocktakingRevisionId', p_evidence.stocktaking_revision_id,
    'evidenceType', p_evidence.evidence_type,
    'noteText', p_evidence.note_text,
    'remoteObjectRef', p_evidence.remote_object_ref,
    'sha256', p_evidence.sha256,
    'mimeType', p_evidence.mime_type,
    'byteSize', p_evidence.byte_size,
    'pixelWidth', p_evidence.pixel_width,
    'pixelHeight', p_evidence.pixel_height,
    'syncState', p_evidence.sync_state,
    'removedAt', p_evidence.removed_at,
    'removedBy', p_evidence.removed_by,
    'createdAt', p_evidence.created_at,
    'createdBy', p_evidence.created_by,
    'entityVersion', p_evidence.entity_version
  );
$function$;
REVOKE ALL ON FUNCTION "silomath"."evidence_to_json"(p_evidence silomath.evidence) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."evidence_to_json"(p_evidence silomath.evidence) TO postgres;

CREATE OR REPLACE FUNCTION silomath.geometry_version_to_json(p silomath.geometry_profile_versions)
 RETURNS jsonb
 LANGUAGE sql
 STABLE
 SET search_path TO ''
AS $function$
select jsonb_build_object('geometryProfileVersionId',p.geometry_profile_version_id,
 'geometryProfileId',p.geometry_profile_id,'versionNumber',p.version_number,'geometryFamilyId',p.geometry_family_id,
 'dimensions',jsonb_strip_nulls(jsonb_build_object('diameterM',p.diameter_m,'cylinderHeightM',p.cylinder_height_m,
 'hopperHeightM',p.hopper_height_m,'outletDiameterM',p.outlet_diameter_m,'sideM',p.side_length_m,
 'lengthM',p.length_m,'widthM',p.width_m,'bodyHeightM',p.body_height_m,'heightM',p.height_m,'areaM2',p.verified_area_m2)),
 'areaProvenance',case when left(ltrim(p.area_provenance),1) in ('{','[') then p.area_provenance::jsonb else to_jsonb(p.area_provenance) end,'hasConstantCrossSection',p.has_constant_cross_section,'measurementReferenceHeightM',p.measurement_reference_height_m,
 'maximumFillHeightM',p.maximum_fill_height_m,'geometryOrigin',p.geometry_origin,'maxCapacityM3',p.max_capacity_m3,
 'formulaVersion',p.formula_version,'schemaVersion',p.schema_version,'createdAt',p.created_at,'createdBy',p.created_by);
$function$;
REVOKE ALL ON FUNCTION "silomath"."geometry_version_to_json"(p silomath.geometry_profile_versions) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."geometry_version_to_json"(p silomath.geometry_profile_versions) TO postgres;

CREATE OR REPLACE FUNCTION silomath.guard_storage_space_identity_history()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'pg_catalog'
AS $function$
begin
    if new.storage_space_id is distinct from old.storage_space_id
        or new.organization_id is distinct from old.organization_id
        or new.storage_unit_id is distinct from old.storage_unit_id
        or new.created_at is distinct from old.created_at
        or new.created_by is distinct from old.created_by then
        raise exception 'Storage Space identity, parent and creation provenance are immutable' using errcode='55000';
    end if;
    if (new.code is distinct from old.code or new.geometry_profile_id is distinct from old.geometry_profile_id)
        and (exists(select 1 from silomath.campaign_scopes where storage_space_id=old.storage_space_id)
          or exists(select 1 from silomath.campaign_snapshot_scope_items where storage_space_id=old.storage_space_id)
          or exists(select 1 from silomath.stocktaking_records where storage_space_id=old.storage_space_id)) then
        raise exception 'Historically referenced Storage Space Code and Geometry Profile are stable' using errcode='55000';
    end if;
    return new;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."guard_storage_space_identity_history"() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."guard_storage_space_identity_history"() TO postgres;

CREATE OR REPLACE FUNCTION silomath.guard_storage_unit_identity_history()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'pg_catalog'
AS $function$
begin
    if new.storage_unit_id is distinct from old.storage_unit_id
        or new.organization_id is distinct from old.organization_id
        or new.location_id is distinct from old.location_id
        or new.created_at is distinct from old.created_at
        or new.created_by is distinct from old.created_by then
        raise exception 'Storage Unit identity, parent and creation provenance are immutable'
            using errcode='55000';
    end if;
    if new.code is distinct from old.code and (exists (
        select 1 from silomath.campaign_snapshot_scope_items s where s.storage_unit_id=old.storage_unit_id
    ) or exists (
        select 1 from silomath.campaign_scopes c
        join silomath.storage_spaces s on s.storage_space_id=c.storage_space_id
        where s.storage_unit_id=old.storage_unit_id
    ) or exists (
        select 1 from silomath.stocktaking_records r
        join silomath.storage_spaces s on s.storage_space_id=r.storage_space_id
        where s.storage_unit_id=old.storage_unit_id
    )) then
        raise exception 'Historically referenced Storage Unit Code is stable' using errcode='55000';
    end if;
    return new;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."guard_storage_unit_identity_history"() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."guard_storage_unit_identity_history"() TO postgres;

CREATE OR REPLACE FUNCTION silomath.has_active_organization_membership(p_organization_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select
        auth.uid() is not null
        and exists (
            select 1
            from silomath.users u
            join silomath.organization_memberships om
              on om.user_id = u.user_id
            join silomath.organizations o
              on o.organization_id = om.organization_id
            where u.user_id = auth.uid()
              and u.lifecycle_status = 'ACTIVE'
              and om.organization_id = p_organization_id
              and om.lifecycle_status = 'ACTIVE'
              and o.lifecycle_status = 'ACTIVE'
        );
$function$;
REVOKE ALL ON FUNCTION "silomath"."has_active_organization_membership"(p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."has_active_organization_membership"(p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.is_active_organization_admin(p_organization_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select
        auth.uid() is not null
        and exists (
            select 1
            from silomath.users u
            join silomath.organization_memberships om
              on om.user_id = u.user_id
            join silomath.organizations o
              on o.organization_id = om.organization_id
            where u.user_id = auth.uid()
              and u.lifecycle_status = 'ACTIVE'
              and om.organization_id = p_organization_id
              and om.lifecycle_status = 'ACTIVE'
              and om.role = 'ADMIN'
              and o.lifecycle_status = 'ACTIVE'
        );
$function$;
REVOKE ALL ON FUNCTION "silomath"."is_active_organization_admin"(p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."is_active_organization_admin"(p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.is_active_user()
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select
        auth.uid() is not null
        and exists (
            select 1
            from silomath.users u
            where u.user_id = auth.uid()
              and u.lifecycle_status = 'ACTIVE'
        );
$function$;
REVOKE ALL ON FUNCTION "silomath"."is_active_user"() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."is_active_user"() TO postgres;

CREATE OR REPLACE FUNCTION silomath.issue_to_json(p_issue silomath.issues)
 RETURNS jsonb
 LANGUAGE sql
 STABLE
 SET search_path TO ''
AS $function$
  select jsonb_build_object(
    'issueId', p_issue.issue_id,
    'organizationId', p_issue.organization_id,
    'campaignId', p_issue.campaign_id,
    'stocktakingRecordId', p_issue.stocktaking_record_id,
    'category', p_issue.category,
    'description', p_issue.description,
    'status', p_issue.status,
    'reportedAt', p_issue.reported_at,
    'reportedBy', p_issue.reported_by,
    'resolvedAt', p_issue.resolved_at,
    'resolvedBy', p_issue.resolved_by,
    'entityVersion', p_issue.entity_version
  );
$function$;
REVOKE ALL ON FUNCTION "silomath"."issue_to_json"(p_issue silomath.issues) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."issue_to_json"(p_issue silomath.issues) TO postgres;

CREATE OR REPLACE FUNCTION silomath.location_actor_is_active_admin(p_actor_user_id uuid, p_organization_id uuid)
 RETURNS boolean
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select exists (
        select 1
        from silomath.users u
        join silomath.organization_memberships m
          on m.user_id = u.user_id
        where u.user_id = p_actor_user_id
          and u.lifecycle_status = 'ACTIVE'
          and m.organization_id = p_organization_id
          and m.lifecycle_status = 'ACTIVE'
          and m.role = 'ADMIN'
    );
$function$;
REVOKE ALL ON FUNCTION "silomath"."location_actor_is_active_admin"(p_actor_user_id uuid, p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."location_actor_is_active_admin"(p_actor_user_id uuid, p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.location_actor_is_active_member(p_actor_user_id uuid, p_organization_id uuid)
 RETURNS boolean
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select exists (
        select 1
        from silomath.users u
        join silomath.organization_memberships m
          on m.user_id = u.user_id
        where u.user_id = p_actor_user_id
          and u.lifecycle_status = 'ACTIVE'
          and m.organization_id = p_organization_id
          and m.lifecycle_status = 'ACTIVE'
    );
$function$;
REVOKE ALL ON FUNCTION "silomath"."location_actor_is_active_member"(p_actor_user_id uuid, p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."location_actor_is_active_member"(p_actor_user_id uuid, p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.notification_to_json(p silomath.notifications)
 RETURNS jsonb
 LANGUAGE sql
 STABLE
 SET search_path TO ''
AS $function$
select jsonb_build_object('notificationId',p.notification_id,'organizationId',p.organization_id,
 'type',p.notification_type,'title',p.title,'message',p.body,'contextualReference',p.context,
 'createdAt',p.created_at,'readAt',p.read_at,'state',p.state);
$function$;
REVOKE ALL ON FUNCTION "silomath"."notification_to_json"(p silomath.notifications) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."notification_to_json"(p silomath.notifications) TO postgres;

CREATE OR REPLACE FUNCTION silomath.offline_package_actor_authorized(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select
        exists (
            select 1
            from silomath.users u
            join silomath.organization_memberships m
              on m.user_id = u.user_id
            where u.user_id = p_actor_user_id
              and u.lifecycle_status = 'ACTIVE'
              and m.organization_id = p_organization_id
              and m.lifecycle_status = 'ACTIVE'
        )
        and exists (
            select 1
            from silomath.inventory_campaigns c
            where c.campaign_id = p_campaign_id
              and c.organization_id = p_organization_id
        )
        and exists (
            select 1
            from silomath.device_installations d
            where d.device_installation_id = p_device_installation_id
              and d.user_id = p_actor_user_id
              and d.revoked_at is null
        );
$function$;
REVOKE ALL ON FUNCTION "silomath"."offline_package_actor_authorized"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."offline_package_actor_authorized"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.reject_audit_history_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'pg_catalog'
AS $function$
begin
    raise exception '% on append-only audit table %.% is prohibited', tg_op, tg_table_schema, tg_table_name
        using errcode = '55000';
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."reject_audit_history_mutation"() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."reject_audit_history_mutation"() TO postgres;

CREATE OR REPLACE FUNCTION silomath.reject_immutable_snapshot_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'pg_catalog'
AS $function$
begin
    raise exception '% on immutable snapshot table %.% is prohibited', tg_op, tg_table_schema, tg_table_name
        using errcode = '55000';
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."reject_immutable_snapshot_mutation"() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."reject_immutable_snapshot_mutation"() TO postgres;

CREATE OR REPLACE FUNCTION silomath.stocktaking_actor_is_team_member(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select
        p_actor_user_id is not null
        and exists (
            select 1
            from silomath.users u
            join silomath.organization_memberships om
              on om.user_id = u.user_id
             and om.organization_id = p_organization_id
            join silomath.organizations o
              on o.organization_id = p_organization_id
            join silomath.inventory_campaigns c
              on c.campaign_id = p_campaign_id
             and c.organization_id = p_organization_id
            join silomath.campaign_team_assignments ta
              on ta.campaign_id = c.campaign_id
             and ta.user_id = p_actor_user_id
            where u.user_id = p_actor_user_id
              and u.lifecycle_status = 'ACTIVE'
              and om.lifecycle_status = 'ACTIVE'
              and o.lifecycle_status = 'ACTIVE'
              and c.status = 'ACTIVE'
              and ta.status = 'ACTIVE'
        );
$function$;
REVOKE ALL ON FUNCTION "silomath"."stocktaking_actor_is_team_member"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."stocktaking_actor_is_team_member"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_actor_is_active_admin(p_actor_user_id uuid, p_organization_id uuid)
 RETURNS boolean
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select exists (
        select 1
        from silomath.users u
        join silomath.organization_memberships m
          on m.user_id = u.user_id
        where u.user_id = p_actor_user_id
          and u.lifecycle_status = 'ACTIVE'
          and m.organization_id = p_organization_id
          and m.lifecycle_status = 'ACTIVE'
          and m.role = 'ADMIN'
    );
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_actor_is_active_admin"(p_actor_user_id uuid, p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_actor_is_active_admin"(p_actor_user_id uuid, p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_actor_is_active_member(p_actor_user_id uuid, p_organization_id uuid)
 RETURNS boolean
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select exists (
        select 1
        from silomath.users u
        join silomath.organization_memberships m
          on m.user_id = u.user_id
        where u.user_id = p_actor_user_id
          and u.lifecycle_status = 'ACTIVE'
          and m.organization_id = p_organization_id
          and m.lifecycle_status = 'ACTIVE'
    );
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_actor_is_active_member"(p_actor_user_id uuid, p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_actor_is_active_member"(p_actor_user_id uuid, p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_unit_actor_is_active_admin(p_actor_user_id uuid, p_organization_id uuid)
 RETURNS boolean
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select exists (
        select 1
        from silomath.users u
        join silomath.organization_memberships m
          on m.user_id = u.user_id
        where u.user_id = p_actor_user_id
          and u.lifecycle_status = 'ACTIVE'
          and m.organization_id = p_organization_id
          and m.lifecycle_status = 'ACTIVE'
          and m.role = 'ADMIN'
    );
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_unit_actor_is_active_admin"(p_actor_user_id uuid, p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_unit_actor_is_active_admin"(p_actor_user_id uuid, p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_unit_actor_is_active_member(p_actor_user_id uuid, p_organization_id uuid)
 RETURNS boolean
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select exists (
        select 1
        from silomath.users u
        join silomath.organization_memberships m
          on m.user_id = u.user_id
        where u.user_id = p_actor_user_id
          and u.lifecycle_status = 'ACTIVE'
          and m.organization_id = p_organization_id
          and m.lifecycle_status = 'ACTIVE'
    );
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_unit_actor_is_active_member"(p_actor_user_id uuid, p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_unit_actor_is_active_member"(p_actor_user_id uuid, p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.sync_actor_is_active_member(p_actor_user_id uuid, p_organization_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select
        p_actor_user_id is not null
        and exists (
            select 1
            from silomath.users u
            join silomath.organization_memberships om
              on om.user_id = u.user_id
            join silomath.organizations o
              on o.organization_id = om.organization_id
            where u.user_id = p_actor_user_id
              and u.lifecycle_status = 'ACTIVE'
              and om.organization_id = p_organization_id
              and om.lifecycle_status = 'ACTIVE'
              and o.lifecycle_status = 'ACTIVE'
        );
$function$;
REVOKE ALL ON FUNCTION "silomath"."sync_actor_is_active_member"(p_actor_user_id uuid, p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."sync_actor_is_active_member"(p_actor_user_id uuid, p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.sync_emit_change(p_organization_id uuid, p_campaign_id uuid, p_entity_type text, p_entity_id uuid, p_entity_version bigint, p_change_type text, p_authoritative_data jsonb)
 RETURNS bigint
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_sequence bigint;
begin
    if p_organization_id is null
       or p_entity_id is null
       or nullif(btrim(p_entity_type), '') is null
       or p_entity_version is null
       or p_entity_version < 1
       or p_change_type not in ('UPSERT', 'DELETE')
       or p_authoritative_data is null then
        raise exception 'invalid sync change'
            using errcode = '22023';
    end if;

    insert into silomath.sync_change_stream (
        organization_id,
        campaign_id,
        entity_type,
        entity_id,
        entity_version,
        change_type,
        server_changed_at,
        authoritative_data
    )
    values (
        p_organization_id,
        p_campaign_id,
        btrim(p_entity_type),
        p_entity_id,
        p_entity_version,
        p_change_type,
        now(),
        p_authoritative_data
    )
    returning change_sequence into v_sequence;

    return v_sequence;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."sync_emit_change"(p_organization_id uuid, p_campaign_id uuid, p_entity_type text, p_entity_id uuid, p_entity_version bigint, p_change_type text, p_authoritative_data jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."sync_emit_change"(p_organization_id uuid, p_campaign_id uuid, p_entity_type text, p_entity_id uuid, p_entity_version bigint, p_change_type text, p_authoritative_data jsonb) TO postgres;

CREATE OR REPLACE FUNCTION silomath.sync_operation_result(p_operation silomath.sync_operations)
 RETURNS jsonb
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select jsonb_build_object(
        'syncOperationId', p_operation.sync_operation_id,
        'outcome',
            case p_operation.state
                when 'ACKNOWLEDGED' then 'ACKNOWLEDGED'
                when 'CONFLICT' then 'CONFLICT'
                when 'PERMANENT_ERROR' then 'PERMANENT_ERROR'
                else 'RETRYABLE_ERROR'
            end,
        'entityVersion', p_operation.authoritative_entity_version,
        'error',
            case
                when p_operation.state in ('ACKNOWLEDGED','CONFLICT')
                    then null
                else jsonb_build_object(
                    'code', coalesce(p_operation.last_error_code, 'SYNC_RETRY_REQUIRED')
                )
            end,
        'conflict',
            case
                when p_operation.state = 'CONFLICT' then (
                    select jsonb_build_object(
                        'syncConflictId', sc.sync_conflict_id,
                        'entityType', sc.entity_type,
                        'entityId', sc.entity_id,
                        'localAttempt', sc.local_attempt,
                        'serverValue', sc.server_value,
                        'baseEntityVersion', sc.base_entity_version,
                        'serverEntityVersion', sc.server_entity_version,
                        'status', sc.status
                    )
                    from silomath.sync_conflicts sc
                    where sc.sync_operation_id = p_operation.sync_operation_id
                      and sc.status = 'OPEN'
                    order by sc.detected_at desc
                    limit 1
                )
                else null
            end
    );
$function$;
REVOKE ALL ON FUNCTION "silomath"."sync_operation_result"(p_operation silomath.sync_operations) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."sync_operation_result"(p_operation silomath.sync_operations) TO postgres;

CREATE OR REPLACE FUNCTION silomath.user_preferences_get(p_actor uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_row silomath.user_preferences%rowtype;
begin
 if not exists(select 1 from silomath.users where user_id=p_actor and lifecycle_status='ACTIVE') then
 raise exception 'active user required' using errcode='42501'; end if;
 select * into v_row from silomath.user_preferences where user_id=p_actor;
 return jsonb_build_object('userId',p_actor,'theme',coalesce(v_row.theme,'SYSTEM'),
 'notificationPreferences',v_row.notification_preferences,'updatedAt',v_row.updated_at);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."user_preferences_get"(p_actor uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."user_preferences_get"(p_actor uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.audit_report_dataset_get(p_actor uuid, p_org uuid, p_request jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare rows jsonb; filters jsonb:=coalesce(p_request->'filters','{}');
begin
 if not silomath.location_actor_is_active_admin(p_actor,p_org) then
 raise exception 'Audit Trail requires Admin' using errcode='42501'; end if;
 if exists(select 1 from jsonb_object_keys(filters) k where k not in
 ('actorUserId','action','entityType','campaignId','entityId','from','to')) then
 raise exception 'unsupported Audit filter' using errcode='22023'; end if;
 select coalesce(jsonb_agg(jsonb_build_object('auditEventId',a.audit_event_id,
 'occurredAt',a.occurred_at,'actorUserId',a.actor_user_id,'action',a.action,
 'entityType',a.entity_type,'entityId',a.entity_id,'campaignId',a.campaign_id,
 'beforeData',a.before_data,'afterData',a.after_data,'changeMetadata',a.change_metadata,
 'source',a.source,'systemActor',a.system_actor) order by a.occurred_at desc,a.audit_event_id desc),'[]')
 into rows from silomath.audit_events a where organization_id=p_org
 and (filters->>'actorUserId' is null or a.actor_user_id=(filters->>'actorUserId')::uuid)
 and (filters->>'action' is null or a.action=filters->>'action')
 and (filters->>'entityType' is null or a.entity_type=filters->>'entityType')
 and (filters->>'campaignId' is null or a.campaign_id=(filters->>'campaignId')::uuid)
 and (filters->>'entityId' is null or a.entity_id=(filters->>'entityId')::uuid)
 and (filters->>'from' is null or a.occurred_at>=(filters->>'from')::timestamptz)
 and (filters->>'to' is null or a.occurred_at<=(filters->>'to')::timestamptz);
 return jsonb_build_object('organizationId',p_org,'reportType','AUDIT_TRAIL',
 'generatedAt',now(),'generatedBy',p_actor,'filters',filters,'rows',rows);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."audit_report_dataset_get"(p_actor uuid, p_org uuid, p_request jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."audit_report_dataset_get"(p_actor uuid, p_org uuid, p_request jsonb) TO postgres;

CREATE OR REPLACE FUNCTION silomath.campaign_close_readiness(p_actor uuid, p_org uuid, p_campaign uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare c silomath.inventory_campaigns%rowtype; todo bigint; open_issues bigint; pending_evidence bigint; pending_sync bigint; scope_count bigint;
begin
 if not silomath.location_actor_is_active_member(p_actor,p_org) then
 raise exception 'campaign access forbidden' using errcode='42501'; end if;
 select * into c from silomath.inventory_campaigns where campaign_id=p_campaign and organization_id=p_org;
 if not found then raise exception 'campaign not found' using errcode='P0002'; end if;
 select count(*),count(*) filter(where v.stocktaking_revision_id is null or v.revision_state<>'VALIDATED')
 into scope_count,todo from silomath.campaign_scopes s
 left join silomath.stocktaking_records r on r.campaign_id=s.campaign_id and r.storage_space_id=s.storage_space_id
 left join silomath.stocktaking_revisions v on v.stocktaking_revision_id=r.current_revision_id
 where s.campaign_id=p_campaign;
 select count(*) into open_issues from silomath.issues where campaign_id=p_campaign and status='OPEN';
 select count(*) into pending_evidence from silomath.evidence where campaign_id=p_campaign
 and removed_at is null and evidence_type='PHOTO' and
 (sync_state<>'SYNCED' or remote_object_ref is null);
 select count(*) into pending_sync from silomath.sync_operations where campaign_id=p_campaign
 and state<>'ACKNOWLEDGED';
 return jsonb_build_object('campaignId',p_campaign,'campaignStatus',c.status,
 'scopeCount',scope_count,'toDoCount',todo,'openIssueCount',open_issues,
 'pendingEvidenceCount',pending_evidence,'pendingCriticalSyncCount',pending_sync,
 'canClose',c.status='ACTIVE' and scope_count>0 and todo=0 and open_issues=0 and pending_evidence=0 and pending_sync=0);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."campaign_close_readiness"(p_actor uuid, p_org uuid, p_campaign uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."campaign_close_readiness"(p_actor uuid, p_org uuid, p_campaign uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.campaign_save_launch_command(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text, p_name text, p_inventory_type text, p_start_date date, p_planned_end_date date, p_scope_storage_space_ids uuid[], p_team_user_ids uuid[], p_snapshot_schema_version text, p_calculation_engine_version text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_campaign_id uuid := gen_random_uuid();
    v_snapshot_id uuid := gen_random_uuid();
    v_now timestamptz := now();
    v_campaign_year integer;
    v_sequence integer;
    v_code text;
    v_body jsonb;
    v_org silomath.organizations%rowtype;
    v_space_id uuid;
    v_team_user_id uuid;
    v_scope_count integer;
    v_team_count integer;
    v_entity_version bigint;
begin
    if not silomath.location_actor_is_active_admin(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'campaign launch forbidden'
            using errcode = '42501';
    end if;

    if nullif(btrim(p_name), '') is null then
        raise exception 'campaign name is required'
            using errcode = '22023';
    end if;

    if p_inventory_type not in ('ANNUAL', 'MONTHLY', 'CONTROL') then
        raise exception 'invalid inventory type'
            using errcode = '22023';
    end if;

    if p_start_date is null then
        raise exception 'campaign start date is required'
            using errcode = '22023';
    end if;

    if p_planned_end_date is null then
        raise exception 'campaign planned end date is required'
            using errcode = '22023';
    end if;

    if p_planned_end_date < p_start_date then
        raise exception 'planned end date cannot precede start date'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_snapshot_schema_version), '') is null then
        raise exception 'snapshot schema version is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_calculation_engine_version), '') is null then
        raise exception 'calculation engine version is required'
            using errcode = '22023';
    end if;

    v_scope_count := coalesce(array_length(p_scope_storage_space_ids, 1), 0);
    v_team_count := coalesce(array_length(p_team_user_ids, 1), 0);

    if v_scope_count < 1 then
        raise exception 'campaign scope must contain at least one storage space'
            using errcode = '22023';
    end if;

    if v_team_count < 1 then
        raise exception 'campaign team must contain at least one member'
            using errcode = '22023';
    end if;

    if (
        select count(distinct x)
        from unnest(p_scope_storage_space_ids) x
    ) <> v_scope_count then
        raise exception 'campaign scope contains duplicate storage spaces'
            using errcode = '22023';
    end if;

    if (
        select count(distinct x)
        from unnest(p_team_user_ids) x
    ) <> v_team_count then
        raise exception 'campaign team contains duplicate users'
            using errcode = '22023';
    end if;

    select *
    into v_org
    from silomath.organizations
    where organization_id = p_organization_id
    for update;

    if not found or v_org.lifecycle_status <> 'ACTIVE' then
        raise exception 'organization not found or inactive'
            using errcode = 'P0002';
    end if;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /campaigns:launch',
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    foreach v_space_id in array p_scope_storage_space_ids loop
        if not exists (
            select 1
            from silomath.storage_spaces ss
            join silomath.storage_units su
              on su.storage_unit_id = ss.storage_unit_id
             and su.organization_id = ss.organization_id
            join silomath.locations l
              on l.location_id = su.location_id
             and l.organization_id = su.organization_id
            join silomath.geometry_profiles gp
              on gp.geometry_profile_id = ss.geometry_profile_id
             and gp.organization_id = ss.organization_id
            join silomath.geometry_profile_versions gpv
              on gpv.geometry_profile_version_id = gp.current_version_id
            where ss.storage_space_id = v_space_id
              and ss.organization_id = p_organization_id
              and ss.lifecycle_status = 'ACTIVE'
              and su.lifecycle_status = 'ACTIVE'
              and l.lifecycle_status = 'ACTIVE'
              and gp.lifecycle_status = 'ACTIVE'
        ) then
            raise exception 'campaign scope contains ineligible storage space'
                using errcode = '22023';
        end if;
    end loop;

    foreach v_team_user_id in array p_team_user_ids loop
        if not exists (
            select 1
            from silomath.users u
            join silomath.organization_memberships m
              on m.user_id = u.user_id
            where u.user_id = v_team_user_id
              and u.lifecycle_status = 'ACTIVE'
              and m.organization_id = p_organization_id
              and m.lifecycle_status = 'ACTIVE'
        ) then
            raise exception 'campaign team contains ineligible user'
                using errcode = '22023';
        end if;
    end loop;

    /*
     * The Organization row is already locked FOR UPDATE, serializing Campaign
     * Code allocation for this Organization. The code year is the UTC
     * calendar year of the authoritative launch instant.
     */
    v_campaign_year :=
        extract(year from (v_now at time zone 'UTC'))::integer;

    select coalesce(max(ic.sequence), 0) + 1
    into v_sequence
    from silomath.inventory_campaigns ic
    where ic.organization_id = p_organization_id
      and ic.campaign_year = v_campaign_year;

    if v_sequence > 9999 then
        raise exception 'campaign sequence exhausted for calendar year'
            using errcode = '22023';
    end if;

    v_code :=
        'INV-'
        || v_campaign_year::text
        || '-'
        || lpad(v_sequence::text, 4, '0');

    insert into silomath.inventory_campaigns (
        campaign_id,
        organization_id,
        code,
        campaign_year,
        sequence,
        name,
        inventory_type,
        start_date,
        planned_end_date,
        status,
        launched_at,
        launched_by,
        campaign_snapshot_id,
        created_at,
        created_by,
        updated_at,
        updated_by,
        entity_version
    )
    values (
        v_campaign_id,
        p_organization_id,
        v_code,
        v_campaign_year,
        v_sequence,
        btrim(p_name),
        p_inventory_type,
        p_start_date,
        p_planned_end_date,
        'ACTIVE',
        v_now,
        p_actor_user_id,
        null,
        v_now,
        p_actor_user_id,
        v_now,
        p_actor_user_id,
        1
    );

    insert into silomath.campaign_scopes (
        campaign_scope_id,
        campaign_id,
        storage_space_id,
        created_at,
        created_by
    )
    select
        gen_random_uuid(),
        v_campaign_id,
        x.storage_space_id,
        v_now,
        p_actor_user_id
    from (
        select distinct unnest(p_scope_storage_space_ids) as storage_space_id
    ) x;

    insert into silomath.campaign_team_assignments (
        assignment_id,
        campaign_id,
        user_id,
        status,
        assigned_at,
        assigned_by,
        removed_at,
        removed_by,
        created_at,
        created_by,
        updated_at,
        updated_by,
        entity_version
    )
    select
        gen_random_uuid(),
        v_campaign_id,
        x.user_id,
        'ACTIVE',
        v_now,
        p_actor_user_id,
        null,
        null,
        v_now,
        p_actor_user_id,
        v_now,
        p_actor_user_id,
        1
    from (
        select distinct unnest(p_team_user_ids) as user_id
    ) x;

    insert into silomath.campaign_snapshots (
        campaign_snapshot_id,
        campaign_id,
        organization_id,
        campaign_code,
        campaign_name,
        inventory_type,
        start_date,
        planned_end_date,
        schema_version,
        calculation_engine_version,
        created_at,
        created_by,
        organization_name,
        organization_code,
        snapshot_schema_version,
        calculation_schema_versions
    )
    values (
        v_snapshot_id,
        v_campaign_id,
        p_organization_id,
        v_code,
        btrim(p_name),
        p_inventory_type,
        p_start_date,
        p_planned_end_date,
        btrim(p_snapshot_schema_version),
        btrim(p_calculation_engine_version),
        v_now,
        p_actor_user_id,
        v_org.name,
        v_org.code,
        btrim(p_snapshot_schema_version),
        (
            select coalesce(
                jsonb_object_agg(
                    gf.family_code,
                    jsonb_build_object(
                        'formulaVersion', gf.formula_version,
                        'geometryProfileVersionSchemaVersions',
                        x.schema_versions
                    )
                ),
                '{}'::jsonb
            )
            from (
                select
                    gpv.geometry_family_id,
                    jsonb_agg(distinct gpv.schema_version)
                        filter (where gpv.schema_version is not null)
                        as schema_versions
                from silomath.storage_spaces ss
                join silomath.geometry_profiles gp
                  on gp.geometry_profile_id = ss.geometry_profile_id
                 and gp.organization_id = ss.organization_id
                join silomath.geometry_profile_versions gpv
                  on gpv.geometry_profile_version_id = gp.current_version_id
                where ss.storage_space_id = any(p_scope_storage_space_ids)
                  and ss.organization_id = p_organization_id
                group by gpv.geometry_family_id
            ) x
            join silomath.geometry_families gf
              on gf.geometry_family_id = x.geometry_family_id
        )
    );

    insert into silomath.campaign_snapshot_scope_items (
        snapshot_scope_item_id,
        campaign_snapshot_id,
        storage_space_id,
        geometry_profile_id,
        geometry_profile_version_id,
        location_id,
        location_code,
        location_name,
        storage_unit_id,
        storage_unit_code,
        storage_unit_name,
        storage_space_code,
        geometry_family_id,
        geometry_family_code,
        geometry_family_name,
        geometry_profile_code,
        geometry_profile_name,
        geometry_formula_version,
        geometry_dimensions,
        measurement_reference_height_m,
        maximum_fill_height_m,
        max_capacity_m3
    )
    select
        gen_random_uuid(),
        v_snapshot_id,
        ss.storage_space_id,
        gp.geometry_profile_id,
        gpv.geometry_profile_version_id,
        l.location_id,
        l.code,
        l.name,
        su.storage_unit_id,
        su.code,
        su.name,
        ss.code,
        gf.geometry_family_id,
        gf.family_code,
        gf.name,
        gp.code,
        gp.name,
        gf.formula_version,
        jsonb_strip_nulls(
            jsonb_build_object(
                'diameterM', gpv.diameter_m,
                'cylinderHeightM', gpv.cylinder_height_m,
                'hopperHeightM', gpv.hopper_height_m,
                'outletDiameterM', gpv.outlet_diameter_m,
                'sideLengthM', gpv.side_length_m,
                'lengthM', gpv.length_m,
                'widthM', gpv.width_m,
                'bodyHeightM', gpv.body_height_m,
                'heightM', gpv.height_m,
                'verifiedAreaM2', gpv.verified_area_m2,
                'areaProvenance', gpv.area_provenance,
                'geometryOrigin', gpv.geometry_origin,
                'schemaVersion', gpv.schema_version,
                'formulaVersion', gpv.formula_version
            )
        ),
        gpv.measurement_reference_height_m,
        gpv.maximum_fill_height_m,
        gpv.max_capacity_m3
    from silomath.storage_spaces ss
    join silomath.storage_units su
      on su.storage_unit_id = ss.storage_unit_id
     and su.organization_id = ss.organization_id
    join silomath.locations l
      on l.location_id = su.location_id
     and l.organization_id = su.organization_id
    join silomath.geometry_profiles gp
      on gp.geometry_profile_id = ss.geometry_profile_id
     and gp.organization_id = ss.organization_id
    join silomath.geometry_profile_versions gpv
      on gpv.geometry_profile_version_id = gp.current_version_id
    join silomath.geometry_families gf
      on gf.geometry_family_id = gpv.geometry_family_id
    where ss.organization_id = p_organization_id
      and ss.storage_space_id = any(p_scope_storage_space_ids);

    insert into silomath.campaign_snapshot_commodity_items (
        snapshot_commodity_item_id,
        campaign_snapshot_id,
        commodity_id,
        commodity_version_id
    )
    select
        gen_random_uuid(),
        v_snapshot_id,
        c.commodity_id,
        c.current_version_id
    from silomath.commodities c
    join silomath.commodity_versions cv
      on cv.commodity_version_id = c.current_version_id
     and cv.commodity_id = c.commodity_id
    where c.lifecycle_status = 'ACTIVE'
    order by c.code, c.commodity_id;

    insert into silomath.campaign_snapshot_team_items (
        snapshot_team_item_id,
        campaign_snapshot_id,
        user_id,
        organization_membership_id,
        role_at_assignment,
        user_full_name,
        user_email
    )
    select
        gen_random_uuid(),
        v_snapshot_id,
        u.user_id,
        m.membership_id,
        m.role,
        u.full_name,
        u.email
    from silomath.users u
    join silomath.organization_memberships m
      on m.user_id = u.user_id
    where m.organization_id = p_organization_id
      and m.lifecycle_status = 'ACTIVE'
      and u.lifecycle_status = 'ACTIVE'
      and u.user_id = any(p_team_user_ids);

    update silomath.inventory_campaigns
    set campaign_snapshot_id = v_snapshot_id,
        updated_at = v_now,
        updated_by = p_actor_user_id
    where campaign_id = v_campaign_id
    returning entity_version into v_entity_version;

    insert into silomath.audit_events (
        audit_event_id,
        organization_id,
        campaign_id,
        actor_user_id,
        action,
        entity_type,
        entity_id,
        occurred_at,
        before_data,
        after_data,
        change_metadata,
        correlation_id,
        source,
        system_actor
    )
    values (
        gen_random_uuid(),
        p_organization_id,
        v_campaign_id,
        p_actor_user_id,
        'SAVE_AND_LAUNCH',
        'InventoryCampaign',
        v_campaign_id,
        v_now,
        null,
        jsonb_build_object(
            'campaignId', v_campaign_id,
            'campaignCode', v_code,
            'status', 'ACTIVE',
            'scopeCount', v_scope_count,
            'teamCount', v_team_count,
            'campaignSnapshotId', v_snapshot_id
        ),
        jsonb_build_object(
            'operation', 'Save & Launch'
        ),
        coalesce(nullif(btrim(p_correlation_id), ''), gen_random_uuid()::text),
        'API_V1',
        null
    );

    v_body := jsonb_build_object(
        'id', v_campaign_id,
        'organizationId', p_organization_id,
        'campaignCode', v_code,
        'name', btrim(p_name),
        'inventoryType', p_inventory_type,
        'startDate', p_start_date,
        'plannedEndDate', p_planned_end_date,
        'status', 'ACTIVE',
        'launchedAt', v_now,
        'launchedBy', p_actor_user_id,
        'completedAt', null,
        'completedBy', null,
        'campaignSnapshotId', v_snapshot_id,
        'createdAt', v_now,
        'createdBy', p_actor_user_id,
        'updatedAt', v_now,
        'updatedBy', p_actor_user_id,
        'entityVersion', v_entity_version
    );

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /campaigns:launch',
        p_idempotency_key,
        p_request_fingerprint,
        201,
        v_body
    );

    return jsonb_build_object(
        'status', 201,
        'body', v_body
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."campaign_save_launch_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text, p_name text, p_inventory_type text, p_start_date date, p_planned_end_date date, p_scope_storage_space_ids uuid[], p_team_user_ids uuid[], p_snapshot_schema_version text, p_calculation_engine_version text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."campaign_save_launch_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text, p_name text, p_inventory_type text, p_start_date date, p_planned_end_date date, p_scope_storage_space_ids uuid[], p_team_user_ids uuid[], p_snapshot_schema_version text, p_calculation_engine_version text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.campaign_snapshot_get(p_actor uuid, p_org uuid, p_campaign uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare s silomath.campaign_snapshots%rowtype; v_scope jsonb; v_geometry jsonb; v_team jsonb; v_commodities jsonb;
begin
 if not silomath.location_actor_is_active_member(p_actor,p_org) then
 raise exception 'campaign snapshot forbidden' using errcode='42501'; end if;
 select * into s from silomath.campaign_snapshots where organization_id=p_org and campaign_id=p_campaign;
 if not found then raise exception 'campaign snapshot not found' using errcode='P0002'; end if;
 select coalesce(jsonb_agg(jsonb_build_object(
 'storageSpaceId',x.storage_space_id,'storageSpaceCode',x.storage_space_code,
 'locationId',x.location_id,'locationCode',x.location_code,'locationName',x.location_name,
 'storageUnitId',x.storage_unit_id,'storageUnitCode',x.storage_unit_code,'storageUnitName',x.storage_unit_name)
 order by x.storage_space_id),'[]'::jsonb) into v_scope
 from silomath.campaign_snapshot_scope_items x where campaign_snapshot_id=s.campaign_snapshot_id;
 select coalesce(jsonb_agg(jsonb_build_object(
 'storageSpaceId',x.storage_space_id,'geometryFamilyId',x.geometry_family_id,
 'geometryFamilyCode',x.geometry_family_code,'geometryFamilyName',x.geometry_family_name,
 'geometryProfileId',x.geometry_profile_id,'geometryProfileVersionId',x.geometry_profile_version_id,
 'geometryProfileCode',x.geometry_profile_code,'geometryProfileName',x.geometry_profile_name,
 'formulaVersion',x.geometry_formula_version,'dimensions',x.geometry_dimensions,
 'measurementReferenceHeightM',x.measurement_reference_height_m,
 'maximumFillHeightM',x.maximum_fill_height_m,'maxCapacityM3',x.max_capacity_m3)
 order by x.storage_space_id),'[]'::jsonb) into v_geometry
 from silomath.campaign_snapshot_scope_items x where campaign_snapshot_id=s.campaign_snapshot_id;
 select coalesce(jsonb_agg(jsonb_build_object('userId',x.user_id,
 'organizationMembershipId',x.organization_membership_id,'roleAtAssignment',x.role_at_assignment,
 'fullName',x.user_full_name,'email',x.user_email) order by x.user_id),'[]'::jsonb)
 into v_team from silomath.campaign_snapshot_team_items x where campaign_snapshot_id=s.campaign_snapshot_id;
 select coalesce(jsonb_agg(jsonb_build_object('commodityId',x.commodity_id,
 'commodityVersionId',x.commodity_version_id) order by x.commodity_id),'[]'::jsonb)
 into v_commodities from silomath.campaign_snapshot_commodity_items x where campaign_snapshot_id=s.campaign_snapshot_id;
 return jsonb_build_object('campaignSnapshotId',s.campaign_snapshot_id,'campaignId',s.campaign_id,
 'organizationSnapshot',jsonb_build_object('organizationId',s.organization_id,'name',s.organization_name,'code',s.organization_code),
 'campaignConfiguration',jsonb_build_object('campaignCode',s.campaign_code,'name',s.campaign_name,
 'inventoryType',s.inventory_type,'startDate',s.start_date,'plannedEndDate',s.planned_end_date),
 'scopeSnapshot',v_scope,'geometrySnapshot',v_geometry,'teamSnapshot',v_team,
 'commodityCompatibilitySnapshot',v_commodities,'calculationSchemaVersions',s.calculation_schema_versions,
 'snapshotSchemaVersion',coalesce(s.snapshot_schema_version,s.schema_version),
 'createdAt',s.created_at,'createdBy',s.created_by);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."campaign_snapshot_get"(p_actor uuid, p_org uuid, p_campaign uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."campaign_snapshot_get"(p_actor uuid, p_org uuid, p_campaign uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.campaign_team_mutate_command(p_actor uuid, p_org uuid, p_campaign uuid, p_user uuid, p_remove boolean, p_expected bigint, p_key text, p_fingerprint text, p_correlation text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_claim record; v_campaign silomath.inventory_campaigns%rowtype;
 v_before silomath.campaign_team_assignments%rowtype;
 v_after silomath.campaign_team_assignments%rowtype;
 v_body jsonb; v_operation text:=case when p_remove then 'CAMPAIGN_TEAM_REMOVE' else 'CAMPAIGN_TEAM_ADD' end;
begin
 if not silomath.location_actor_is_active_admin(p_actor,p_org) then
 raise exception 'team administration forbidden' using errcode='42501'; end if;
 select * into v_claim from silomath.claim_idempotency_key(p_actor,p_org,v_operation,p_key,p_fingerprint);
 if v_claim.disposition='REPLAY' then
 return jsonb_build_object('status',v_claim.response_status,'body',v_claim.response_body); end if;
 select * into v_campaign from silomath.inventory_campaigns
 where campaign_id=p_campaign and organization_id=p_org for update;
 if not found then raise exception 'campaign not found' using errcode='P0002'; end if;
 if v_campaign.status<>'ACTIVE' then raise exception 'campaign completed' using errcode='55000'; end if;
 if p_remove then
 select * into v_before from silomath.campaign_team_assignments
 where campaign_id=p_campaign and user_id=p_user and status='ACTIVE' for update;
 if not found then raise exception 'active assignment not found' using errcode='P0002'; end if;
 perform silomath.assert_expected_entity_version(p_expected,v_before.entity_version);
 update silomath.campaign_team_assignments set status='REMOVED',removed_at=now(),removed_by=p_actor,
 updated_at=now(),updated_by=p_actor where assignment_id=v_before.assignment_id returning * into v_after;
 else
 if not silomath.location_actor_is_active_member(p_user,p_org) then
 raise exception 'team target requires active membership' using errcode='42501'; end if;
 insert into silomath.campaign_team_assignments(assignment_id,campaign_id,user_id,status,
 assigned_at,assigned_by,created_at,created_by,updated_at,updated_by,entity_version)
 values(gen_random_uuid(),p_campaign,p_user,'ACTIVE',now(),p_actor,now(),p_actor,now(),p_actor,1)
 returning * into v_after;
 insert into silomath.notifications(notification_id,organization_id,recipient_user_id,
 notification_type,title,body,context,created_at)
 values(gen_random_uuid(),p_org,p_user,'CAMPAIGN_ASSIGNMENT','Campaign assignment',
 'You have been assigned to a Campaign.',jsonb_build_object('campaignId',p_campaign),now());
 end if;
 v_body:=silomath.campaign_team_to_json(v_after);
 insert into silomath.audit_events(audit_event_id,organization_id,campaign_id,actor_user_id,
 action,entity_type,entity_id,occurred_at,before_data,after_data,correlation_id,source)
 values(gen_random_uuid(),p_org,p_campaign,p_actor,v_operation,'CAMPAIGN_TEAM_ASSIGNMENT',
 v_after.assignment_id,now(),case when p_remove then silomath.campaign_team_to_json(v_before) else null end,
 v_body,p_correlation,'API');
 perform silomath.sync_emit_change(p_org,p_campaign,'CAMPAIGN_TEAM_ASSIGNMENT',
 v_after.assignment_id,v_after.entity_version,'UPSERT',v_body);
 perform silomath.complete_idempotency_key(p_actor,p_org,v_operation,p_key,p_fingerprint,
 case when p_remove then 200 else 201 end,v_body);
 return jsonb_build_object('status',case when p_remove then 200 else 201 end,'body',v_body);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."campaign_team_mutate_command"(p_actor uuid, p_org uuid, p_campaign uuid, p_user uuid, p_remove boolean, p_expected bigint, p_key text, p_fingerprint text, p_correlation text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."campaign_team_mutate_command"(p_actor uuid, p_org uuid, p_campaign uuid, p_user uuid, p_remove boolean, p_expected bigint, p_key text, p_fingerprint text, p_correlation text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.client_mutate_command(p_actor uuid, p_org uuid, p_id uuid, p_patch jsonb, p_expected bigint, p_key text, p_fingerprint text, p_correlation text, p_create boolean)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
 v_claim record;
 v_before silomath.clients%rowtype;
 v_after silomath.clients%rowtype;
 v_name text; v_code text; v_tax text; v_notes text; v_status text;
 v_operation text := case when p_create then 'CLIENT_CREATE' else 'CLIENT_UPDATE' end;
 v_body jsonb;
begin
 if not silomath.location_actor_is_active_admin(p_actor,p_org) then
  raise exception 'client mutation forbidden' using errcode='42501';
 end if;
 if p_patch is null or jsonb_typeof(p_patch)<>'object'
 or exists(select 1 from jsonb_object_keys(p_patch) k where k not in
 ('name','code','registrationTaxId','notes','lifecycleStatus')) then
  raise exception 'invalid Client fields' using errcode='22023';
 end if;
 select * into v_claim from silomath.claim_idempotency_key(
 p_actor,p_org,v_operation,p_key,p_fingerprint);
 if v_claim.disposition='REPLAY' then
  return jsonb_build_object('status',v_claim.response_status,'body',v_claim.response_body);
 end if;
 if not p_create then
  select * into v_before from silomath.clients
  where client_id=p_id and organization_id=p_org for update;
  if not found then raise exception 'client not found' using errcode='P0002'; end if;
  perform silomath.assert_expected_entity_version(p_expected,v_before.entity_version);
 end if;
 v_name:=case when p_patch?'name' then btrim(p_patch->>'name') else v_before.name end;
 v_code:=case when p_patch?'code' then upper(btrim(p_patch->>'code')) else v_before.code end;
 v_tax:=case when p_patch?'registrationTaxId' then nullif(btrim(p_patch->>'registrationTaxId'),'') else v_before.tax_id end;
 v_notes:=case when p_patch?'notes' then nullif(btrim(p_patch->>'notes'),'') else v_before.notes end;
 v_status:=case when p_patch?'lifecycleStatus' then p_patch->>'lifecycleStatus'
 else case when p_create then 'ACTIVE' else v_before.lifecycle_status end end;
 if v_name is null or length(v_name) not between 1 and 150
 or v_code is null or length(v_code) not between 1 and 50
 or length(v_tax)>50 or length(v_notes)>500
 or v_status is null or v_status not in ('ACTIVE','INACTIVE') then
  raise exception 'invalid Client fields' using errcode='22023';
 end if;
 if not p_create and v_code<>v_before.code and exists(
 select 1 from silomath.stocktaking_revisions where client_id=p_id) then
  raise exception 'historically referenced Client Code is stable' using errcode='55000';
 end if;
 if p_create then
  insert into silomath.clients(client_id,organization_id,name,code,tax_id,notes,
   lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version)
  values(p_id,p_org,v_name,v_code,v_tax,v_notes,v_status,now(),p_actor,now(),p_actor,1)
  returning * into v_after;
 else
  update silomath.clients set name=v_name,code=v_code,tax_id=v_tax,notes=v_notes,
   lifecycle_status=v_status,updated_at=now(),updated_by=p_actor
  where client_id=p_id returning * into v_after;
 end if;
 v_body:=silomath.client_to_json(v_after);
 insert into silomath.audit_events(audit_event_id,organization_id,actor_user_id,
 action,entity_type,entity_id,occurred_at,before_data,after_data,correlation_id,source)
 values(gen_random_uuid(),p_org,p_actor,v_operation,'CLIENT',p_id,now(),
 case when p_create then null else silomath.client_to_json(v_before) end,
 v_body,p_correlation,'API');
 perform silomath.sync_emit_change(p_org,null,'CLIENT',p_id,v_after.entity_version,'UPSERT',v_body);
 perform silomath.complete_idempotency_key(p_actor,p_org,v_operation,p_key,p_fingerprint,
 case when p_create then 201 else 200 end,v_body);
 return jsonb_build_object('status',case when p_create then 201 else 200 end,'body',v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."client_mutate_command"(p_actor uuid, p_org uuid, p_id uuid, p_patch jsonb, p_expected bigint, p_key text, p_fingerprint text, p_correlation text, p_create boolean) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."client_mutate_command"(p_actor uuid, p_org uuid, p_id uuid, p_patch jsonb, p_expected bigint, p_key text, p_fingerprint text, p_correlation text, p_create boolean) TO postgres;

CREATE OR REPLACE FUNCTION silomath.evidence_create_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_evidence_id uuid, p_stocktaking_record_id uuid, p_issue_id uuid, p_stocktaking_revision_id uuid, p_evidence_type text, p_note_text text, p_local_private_ref text, p_sha256 text, p_mime_type text, p_byte_size bigint, p_pixel_width integer, p_pixel_height integer, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_claim record;
  v_evidence silomath.evidence%rowtype;
  v_now timestamptz := now();
  v_body jsonb;
  v_owner_record_id uuid;
  v_owner_revision_id uuid;
begin
  if not silomath.stocktaking_actor_is_team_member(
    p_actor_user_id, p_organization_id, p_campaign_id
  ) then
    raise exception 'EVIDENCE_NOT_TEAM_MEMBER' using errcode = '42501';
  end if;

  if p_evidence_id is null
     or ((p_stocktaking_record_id is not null) = (p_issue_id is not null))
     or p_evidence_type not in ('PHOTO','NOTE') then
    raise exception 'invalid evidence owner or type' using errcode = '22023';
  end if;

  if p_issue_id is not null then
    select i.stocktaking_record_id into v_owner_record_id
    from silomath.issues i
    where i.issue_id = p_issue_id
      and i.organization_id = p_organization_id
      and i.campaign_id = p_campaign_id;
    if v_owner_record_id is null or p_stocktaking_revision_id is not null then
      raise exception 'invalid issue evidence owner' using errcode = '22023';
    end if;
  else
    select sr.stocktaking_record_id, sr.current_revision_id
    into v_owner_record_id, v_owner_revision_id
    from silomath.stocktaking_records sr
    join silomath.stocktaking_revisions rev
      on rev.stocktaking_revision_id = sr.current_revision_id
     and rev.stocktaking_record_id = sr.stocktaking_record_id
     and rev.revision_state = 'WORKING'
    where sr.stocktaking_record_id = p_stocktaking_record_id
      and sr.organization_id = p_organization_id
      and sr.campaign_id = p_campaign_id;
    if v_owner_record_id is null then
      raise exception 'evidence owner not found' using errcode = 'P0002';
    end if;
    if p_stocktaking_revision_id is not null
       and p_stocktaking_revision_id <> v_owner_revision_id then
      raise exception 'EVIDENCE_IMMUTABLE' using errcode = 'P0001';
    end if;
  end if;

  if p_evidence_type = 'NOTE' then
    if nullif(btrim(p_note_text), '') is null
       or char_length(p_note_text) > 2000
       or p_local_private_ref is not null
       or p_sha256 is not null
       or p_mime_type is not null
       or p_byte_size is not null
       or p_pixel_width is not null
       or p_pixel_height is not null then
      raise exception 'invalid note evidence' using errcode = '22023';
    end if;
  else
    if p_note_text is not null
       or nullif(btrim(p_local_private_ref), '') is null
       or p_local_private_ref like '%://%'
       or left(p_local_private_ref, 1) = '/'
       or p_sha256 !~ '^[0-9a-f]{64}$'
       or p_mime_type <> 'image/jpeg'
       or p_byte_size not between 1 and 3000000
       or p_pixel_width < 1 or p_pixel_height < 1
       or greatest(p_pixel_width, p_pixel_height) > 2048 then
      raise exception 'invalid photo evidence' using errcode = '22023';
    end if;

    if p_issue_id is not null then
      if (
        select count(*)
        from silomath.evidence e
        where e.issue_id = p_issue_id
          and e.evidence_type = 'PHOTO'
          and e.removed_at is null
      ) >= 5 then
        raise exception 'EVIDENCE_PHOTO_LIMIT' using errcode = 'P0001';
      end if;
    elsif (
      select count(*)
      from silomath.evidence e
      where e.stocktaking_record_id = p_stocktaking_record_id
        and e.issue_id is null
        and e.evidence_type = 'PHOTO'
        and e.removed_at is null
        and (
          e.stocktaking_revision_id = coalesce(p_stocktaking_revision_id, v_owner_revision_id)
          or (e.stocktaking_revision_id is null and p_stocktaking_revision_id is null)
        )
    ) >= 5 then
      raise exception 'EVIDENCE_PHOTO_LIMIT' using errcode = 'P0001';
    end if;
  end if;

  select * into v_claim
  from silomath.claim_idempotency_key(
    p_actor_user_id, p_organization_id, 'EVIDENCE_CREATE',
    p_idempotency_key, p_request_fingerprint
  );
  if v_claim.disposition = 'REPLAY' then
    return jsonb_build_object('status', v_claim.response_status, 'body', v_claim.response_body);
  end if;

  insert into silomath.evidence (
    evidence_id, organization_id, campaign_id,
    stocktaking_record_id, issue_id, stocktaking_revision_id,
    evidence_type, note_text, local_private_ref, remote_object_ref,
    sha256, mime_type, byte_size, pixel_width, pixel_height,
    sync_state, removed_at, removed_by, created_at, created_by, entity_version
  ) values (
    p_evidence_id, p_organization_id, p_campaign_id,
    p_stocktaking_record_id, p_issue_id, p_stocktaking_revision_id,
    p_evidence_type,
    case when p_evidence_type = 'NOTE' then btrim(p_note_text) else null end,
    p_local_private_ref, null, p_sha256, p_mime_type,
    p_byte_size, p_pixel_width, p_pixel_height,
    'LOCAL_PENDING', null, null, v_now, p_actor_user_id, 1
  ) returning * into v_evidence;

  v_body := silomath.evidence_to_json(v_evidence);
  insert into silomath.audit_events (
    audit_event_id, organization_id, campaign_id, actor_user_id,
    action, entity_type, entity_id, occurred_at, before_data, after_data,
    change_metadata, correlation_id, source, system_actor
  ) values (
    gen_random_uuid(), p_organization_id, p_campaign_id, p_actor_user_id,
    'EVIDENCE_CREATED', 'EVIDENCE', p_evidence_id, v_now,
    null, v_body, null, p_correlation_id, 'API', null
  );

  perform silomath.sync_emit_change(
    p_organization_id, p_campaign_id, 'EVIDENCE', p_evidence_id,
    v_evidence.entity_version, 'UPSERT', v_body
  );
  perform silomath.complete_idempotency_key(
    p_actor_user_id, p_organization_id, 'EVIDENCE_CREATE',
    p_idempotency_key, p_request_fingerprint, 201, v_body
  );
  return jsonb_build_object('status', 201, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."evidence_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_evidence_id uuid, p_stocktaking_record_id uuid, p_issue_id uuid, p_stocktaking_revision_id uuid, p_evidence_type text, p_note_text text, p_local_private_ref text, p_sha256 text, p_mime_type text, p_byte_size bigint, p_pixel_width integer, p_pixel_height integer, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."evidence_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_evidence_id uuid, p_stocktaking_record_id uuid, p_issue_id uuid, p_stocktaking_revision_id uuid, p_evidence_type text, p_note_text text, p_local_private_ref text, p_sha256 text, p_mime_type text, p_byte_size bigint, p_pixel_width integer, p_pixel_height integer, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.evidence_finalize_upload_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_evidence_id uuid, p_remote_object_ref text, p_sha256 text, p_byte_size bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_claim record;
  v_before silomath.evidence%rowtype;
  v_after silomath.evidence%rowtype;
  v_now timestamptz := now();
  v_body jsonb;
begin
  if not silomath.stocktaking_actor_is_team_member(
    p_actor_user_id, p_organization_id, p_campaign_id
  ) then
    raise exception 'EVIDENCE_NOT_TEAM_MEMBER' using errcode = '42501';
  end if;

  select * into v_before
  from silomath.evidence
  where evidence_id = p_evidence_id
    and organization_id = p_organization_id
    and campaign_id = p_campaign_id
    and evidence_type = 'PHOTO'
    and removed_at is null
  for update;
  if not found then
    raise exception 'evidence not found' using errcode = 'P0002';
  end if;

  if v_before.sha256 <> p_sha256 or v_before.byte_size <> p_byte_size then
    raise exception 'EVIDENCE_HASH_MISMATCH' using errcode = '22023';
  end if;
  if nullif(btrim(p_remote_object_ref), '') is null
     or p_remote_object_ref like '%://%' then
    raise exception 'invalid remote object reference' using errcode = '22023';
  end if;

  select * into v_claim
  from silomath.claim_idempotency_key(
    p_actor_user_id, p_organization_id, 'EVIDENCE_UPLOAD_FINALIZE',
    p_idempotency_key, p_request_fingerprint
  );
  if v_claim.disposition = 'REPLAY' then
    return jsonb_build_object('status', v_claim.response_status, 'body', v_claim.response_body);
  end if;

  update silomath.evidence
  set remote_object_ref = btrim(p_remote_object_ref),
      sync_state = 'SYNCED'
  where evidence_id = p_evidence_id
  returning * into v_after;

  v_body := silomath.evidence_to_json(v_after);
  insert into silomath.audit_events (
    audit_event_id, organization_id, campaign_id, actor_user_id,
    action, entity_type, entity_id, occurred_at, before_data, after_data,
    change_metadata, correlation_id, source, system_actor
  ) values (
    gen_random_uuid(), p_organization_id, p_campaign_id, p_actor_user_id,
    'EVIDENCE_UPLOAD_VERIFIED', 'EVIDENCE', p_evidence_id, v_now,
    silomath.evidence_to_json(v_before), v_body,
    jsonb_build_object('sha256', p_sha256, 'byteSize', p_byte_size),
    p_correlation_id, 'API', null
  );
  perform silomath.sync_emit_change(
    p_organization_id, p_campaign_id, 'EVIDENCE', p_evidence_id,
    v_after.entity_version, 'UPSERT', v_body
  );
  perform silomath.complete_idempotency_key(
    p_actor_user_id, p_organization_id, 'EVIDENCE_UPLOAD_FINALIZE',
    p_idempotency_key, p_request_fingerprint, 200, v_body
  );
  return jsonb_build_object('status', 200, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."evidence_finalize_upload_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_evidence_id uuid, p_remote_object_ref text, p_sha256 text, p_byte_size bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."evidence_finalize_upload_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_evidence_id uuid, p_remote_object_ref text, p_sha256 text, p_byte_size bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.evidence_remove_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_evidence_id uuid, p_expected_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_claim record;
  v_before silomath.evidence%rowtype;
  v_after silomath.evidence%rowtype;
  v_now timestamptz := now();
  v_body jsonb;
begin
  if not silomath.stocktaking_actor_is_team_member(
    p_actor_user_id, p_organization_id, p_campaign_id
  ) then
    raise exception 'EVIDENCE_NOT_TEAM_MEMBER' using errcode = '42501';
  end if;

  select * into v_claim
  from silomath.claim_idempotency_key(
    p_actor_user_id, p_organization_id, 'EVIDENCE_REMOVE',
    p_idempotency_key, p_request_fingerprint
  );
  if v_claim.disposition = 'REPLAY' then
    return jsonb_build_object('status', v_claim.response_status, 'body', v_claim.response_body);
  end if;

  select * into v_before
  from silomath.evidence
  where evidence_id = p_evidence_id
    and organization_id = p_organization_id
    and campaign_id = p_campaign_id
    and removed_at is null
  for update;
  if not found then
    raise exception 'evidence not found' using errcode = 'P0002';
  end if;

  perform silomath.assert_expected_entity_version(
    p_expected_entity_version, v_before.entity_version
  );

  if v_before.stocktaking_revision_id is not null and exists (
    select 1 from silomath.stocktaking_revisions sr
    where sr.stocktaking_revision_id = v_before.stocktaking_revision_id
      and sr.revision_state = 'VALIDATED'
  ) then
    raise exception 'EVIDENCE_IMMUTABLE' using errcode = 'P0001';
  end if;

  update silomath.evidence
  set removed_at = v_now,
      removed_by = p_actor_user_id
  where evidence_id = p_evidence_id
  returning * into v_after;

  v_body := silomath.evidence_to_json(v_after);
  insert into silomath.audit_events (
    audit_event_id, organization_id, campaign_id, actor_user_id,
    action, entity_type, entity_id, occurred_at, before_data, after_data,
    change_metadata, correlation_id, source, system_actor
  ) values (
    gen_random_uuid(), p_organization_id, p_campaign_id, p_actor_user_id,
    'EVIDENCE_REMOVED', 'EVIDENCE', p_evidence_id, v_now,
    silomath.evidence_to_json(v_before), v_body, null,
    p_correlation_id, 'API', null
  );
  perform silomath.sync_emit_change(
    p_organization_id, p_campaign_id, 'EVIDENCE', p_evidence_id,
    v_after.entity_version, 'UPSERT', v_body
  );
  perform silomath.complete_idempotency_key(
    p_actor_user_id, p_organization_id, 'EVIDENCE_REMOVE',
    p_idempotency_key, p_request_fingerprint, 200, v_body
  );
  return jsonb_build_object('status', 200, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."evidence_remove_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_evidence_id uuid, p_expected_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."evidence_remove_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_evidence_id uuid, p_expected_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.final_inventory_snapshot_get(p_actor uuid, p_org uuid, p_campaign uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare s silomath.final_inventory_snapshots%rowtype; entries jsonb;
begin
 if not silomath.location_actor_is_active_member(p_actor,p_org) then
 raise exception 'final snapshot access forbidden' using errcode='42501'; end if;
 select * into s from silomath.final_inventory_snapshots where organization_id=p_org and campaign_id=p_campaign;
 if not found then raise exception 'final snapshot not found' using errcode='P0002'; end if;
 select coalesce(jsonb_agg(jsonb_build_object(
 'snapshotEntryId',e.entry_id,'finalSnapshotId',e.final_snapshot_id,'storageSpaceId',e.storage_space_id,
 'hierarchySnapshot',e.hierarchy_snapshot,'geometrySnapshot',e.geometry_snapshot,
 'commoditySnapshot',e.commodity_snapshot,'ownershipClientSnapshot',e.ownership_client_snapshot,
 'isEmpty',e.is_empty,'measurementsSnapshot',e.measurements_snapshot,
 'operationalValues',e.operational_values,'calculationMetadata',e.calculation_metadata,
 'validationMetadata',e.validation_metadata,'issueReferencesResolutionSummary',e.issue_references_resolution_summary,
 'evidenceReferences',e.evidence_references) order by e.storage_space_id),'[]'::jsonb)
 into entries from silomath.final_inventory_snapshot_entries e where final_snapshot_id=s.final_snapshot_id;
 return jsonb_build_object('finalSnapshotId',s.final_snapshot_id,'organizationId',s.organization_id,
 'campaignId',s.campaign_id,'campaignSnapshotId',s.campaign_snapshot_id,
 'closedAt',s.closed_at,'closedBy',s.closed_by,'snapshotSchemaVersion',s.snapshot_schema_version,
 'calculationCompatibilityVersions',s.calculation_compatibility_versions,
 'createdAt',s.created_at,'createdBy',s.created_by,'entries',entries);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."final_inventory_snapshot_get"(p_actor uuid, p_org uuid, p_campaign uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."final_inventory_snapshot_get"(p_actor uuid, p_org uuid, p_campaign uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.geometry_profile_to_json(p silomath.geometry_profiles)
 RETURNS jsonb
 LANGUAGE sql
 STABLE
 SET search_path TO ''
AS $function$
select jsonb_build_object('id',p.geometry_profile_id,'organizationId',p.organization_id,'name',p.name,'code',p.code,
 'geometryFamilyId',p.geometry_family_id,'lifecycleStatus',p.lifecycle_status,'currentVersionId',p.current_version_id,
 'currentVersion',silomath.geometry_version_to_json(v),'createdAt',p.created_at,'createdBy',p.created_by,
 'updatedAt',p.updated_at,'updatedBy',p.updated_by,'entityVersion',p.entity_version)
 from silomath.geometry_profile_versions v where v.geometry_profile_version_id=p.current_version_id;
$function$;
REVOKE ALL ON FUNCTION "silomath"."geometry_profile_to_json"(p silomath.geometry_profiles) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."geometry_profile_to_json"(p silomath.geometry_profiles) TO postgres;

CREATE OR REPLACE FUNCTION silomath.inventory_history_compare(p_actor uuid, p_org uuid, p_request jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare periods jsonb; changes jsonb; ids uuid[]; expected integer;
 filters jsonb:=coalesce(p_request->'filters','{}');
begin
 if exists(select 1 from jsonb_object_keys(filters) k where k not in ('commodityId','locationId','ownership','clientId')) then
 raise exception 'unsupported History filter' using errcode='22023'; end if;
 if not silomath.location_actor_is_active_member(p_actor,p_org) then
 raise exception 'history forbidden' using errcode='42501'; end if;
 if jsonb_typeof(p_request->'campaignIds') is distinct from 'array'
 or jsonb_array_length(p_request->'campaignIds')<2 then
 raise exception 'history requires 2..N Completed Campaigns' using errcode='22023'; end if;
 select array_agg(value::uuid),count(distinct value) into ids,expected
 from jsonb_array_elements_text(p_request->'campaignIds');
 if expected<>cardinality(ids) or expected<>(
 select count(*) from silomath.inventory_campaigns c join silomath.final_inventory_snapshots f using(campaign_id)
 where c.campaign_id=any(ids) and c.organization_id=p_org and c.status='COMPLETED') then
 raise exception 'authorized Completed Campaign snapshots required' using errcode='22023'; end if;
 select jsonb_agg(jsonb_build_object('campaignId',f.campaign_id,'finalSnapshotId',f.final_snapshot_id,
 'closedAt',f.closed_at) order by f.closed_at,f.campaign_id) into periods
 from silomath.final_inventory_snapshots f where f.campaign_id=any(ids) and f.organization_id=p_org;
 with ordered as (
 select f.*,lag(f.final_snapshot_id) over(order by f.closed_at,f.campaign_id) previous_id,
 lag(f.campaign_id) over(order by f.closed_at,f.campaign_id) previous_campaign
 from silomath.final_inventory_snapshots f where f.campaign_id=any(ids) and f.organization_id=p_org
 ), pairs as (
 select o.campaign_id,o.previous_campaign,o.closed_at,coalesce(new.storage_space_id,old.storage_space_id) space_id,
 old.entry_id old_id,new.entry_id new_id,
 old.hierarchy_snapshot old_hierarchy,new.hierarchy_snapshot new_hierarchy,
 old.commodity_snapshot old_commodity,new.commodity_snapshot new_commodity,
 old.ownership_client_snapshot old_owner,new.ownership_client_snapshot new_owner,
 (old.operational_values->>'physicalQuantityKg')::numeric old_quantity,
 (new.operational_values->>'physicalQuantityKg')::numeric new_quantity
 from ordered o cross join lateral (
 select storage_space_id from silomath.final_inventory_snapshot_entries where final_snapshot_id=o.previous_id
 union select storage_space_id from silomath.final_inventory_snapshot_entries where final_snapshot_id=o.final_snapshot_id
 ) spaces
 left join silomath.final_inventory_snapshot_entries old
 on old.final_snapshot_id=o.previous_id and old.storage_space_id=spaces.storage_space_id
 left join silomath.final_inventory_snapshot_entries new
 on new.final_snapshot_id=o.final_snapshot_id and new.storage_space_id=spaces.storage_space_id
 where o.previous_id is not null
 ), classified as (
 select *,case when old_id is null then 'SCOPE_ADDED' when new_id is null then 'SCOPE_REMOVED'
 when old_hierarchy->>'locationId' is distinct from new_hierarchy->>'locationId'
 or old_commodity->>'commodity_id' is distinct from new_commodity->>'commodity_id'
 or old_owner->>'ownership' is distinct from new_owner->>'ownership'
 then 'CONTEXT_CHANGED' else 'COMPARABLE' end classification from pairs
 )
 select coalesce(jsonb_agg(jsonb_build_object('previousCampaignId',previous_campaign,
 'campaignId',campaign_id,'storageSpaceId',space_id,'classification',classification,
 'previousPhysicalQuantityKg',old_quantity::text,'physicalQuantityKg',new_quantity::text,
 'absoluteChangeKg',case when classification='COMPARABLE' then (new_quantity-old_quantity)::text else null end,
 'percentageChange',case when classification='COMPARABLE' and old_quantity>0
 then ((new_quantity-old_quantity)/old_quantity*100)::text else null end)
 order by closed_at,campaign_id,space_id),'[]') into changes from classified
 where (
 (filters->>'locationId' is null or old_hierarchy->>'locationId'=filters->>'locationId')
 and (filters->>'commodityId' is null or old_commodity->>'commodity_id'=filters->>'commodityId')
 and (filters->>'ownership' is null or old_owner->>'ownership'=filters->>'ownership')
 and (filters->>'clientId' is null or old_owner#>>'{client,client_id}'=filters->>'clientId')
 ) or (
 (filters->>'locationId' is null or new_hierarchy->>'locationId'=filters->>'locationId')
 and (filters->>'commodityId' is null or new_commodity->>'commodity_id'=filters->>'commodityId')
 and (filters->>'ownership' is null or new_owner->>'ownership'=filters->>'ownership')
 and (filters->>'clientId' is null or new_owner#>>'{client,client_id}'=filters->>'clientId')
 );
 return jsonb_build_object('organizationId',p_org,'periods',periods,'changes',changes,
 'generatedAt',now(),'generatedBy',p_actor);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."inventory_history_compare"(p_actor uuid, p_org uuid, p_request jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."inventory_history_compare"(p_actor uuid, p_org uuid, p_request jsonb) TO postgres;

CREATE OR REPLACE FUNCTION silomath.issue_create_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_issue_id uuid, p_category text, p_description text, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_claim record;
  v_issue silomath.issues%rowtype;
  v_audit_event_id uuid := gen_random_uuid();
  v_now timestamptz := now();
  v_body jsonb;
begin
  if not silomath.stocktaking_actor_is_team_member(
    p_actor_user_id, p_organization_id, p_campaign_id
  ) then
    raise exception 'ISSUE_NOT_TEAM_MEMBER' using errcode = '42501';
  end if;

  if p_issue_id is null
     or p_stocktaking_record_id is null
     or p_category not in (
       'MEASUREMENT_PROBLEM','ACCESS_SAFETY','GEOMETRY_DISCREPANCY',
       'COMMODITY_UNCERTAINTY','STORAGE_SPACE_CONDITION','OTHER'
     )
     or nullif(btrim(p_description), '') is null then
    raise exception 'invalid issue input' using errcode = '22023';
  end if;

  if not exists (
    select 1
    from silomath.stocktaking_records sr
    where sr.stocktaking_record_id = p_stocktaking_record_id
      and sr.organization_id = p_organization_id
      and sr.campaign_id = p_campaign_id
  ) then
    raise exception 'issue owner not found' using errcode = 'P0002';
  end if;

  select * into v_claim
  from silomath.claim_idempotency_key(
    p_actor_user_id, p_organization_id, 'ISSUE_CREATE',
    p_idempotency_key, p_request_fingerprint
  );
  if v_claim.disposition = 'REPLAY' then
    return jsonb_build_object('status', v_claim.response_status, 'body', v_claim.response_body);
  end if;

  insert into silomath.issues (
    issue_id, organization_id, campaign_id, stocktaking_record_id,
    category, description, status, reported_at, reported_by,
    resolved_at, resolved_by, entity_version
  ) values (
    p_issue_id, p_organization_id, p_campaign_id, p_stocktaking_record_id,
    p_category, btrim(p_description), 'OPEN', v_now, p_actor_user_id,
    null, null, 1
  ) returning * into v_issue;

  insert into silomath.audit_events (
    audit_event_id, organization_id, campaign_id, actor_user_id,
    action, entity_type, entity_id, occurred_at, before_data, after_data,
    change_metadata, correlation_id, source, system_actor
  ) values (
    v_audit_event_id, p_organization_id, p_campaign_id, p_actor_user_id,
    'ISSUE_CREATED', 'ISSUE', p_issue_id, v_now, null,
    silomath.issue_to_json(v_issue), null, p_correlation_id, 'API', null
  );

  insert into silomath.issue_status_events (
    issue_status_event_id, issue_id, from_status, to_status, reason,
    changed_at, changed_by, audit_event_id
  ) values (
    gen_random_uuid(), p_issue_id, null, 'OPEN', null,
    v_now, p_actor_user_id, v_audit_event_id
  );

  v_body := silomath.issue_to_json(v_issue);
  perform silomath.sync_emit_change(
    p_organization_id, p_campaign_id, 'ISSUE', p_issue_id,
    v_issue.entity_version, 'UPSERT', v_body
  );
  perform silomath.complete_idempotency_key(
    p_actor_user_id, p_organization_id, 'ISSUE_CREATE',
    p_idempotency_key, p_request_fingerprint, 201, v_body
  );
  return jsonb_build_object('status', 201, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."issue_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_issue_id uuid, p_category text, p_description text, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."issue_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_issue_id uuid, p_category text, p_description text, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.issue_transition_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_issue_id uuid, p_target_status text, p_reason text, p_resolution_context text, p_expected_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_claim record;
  v_before silomath.issues%rowtype;
  v_after silomath.issues%rowtype;
  v_audit_event_id uuid := gen_random_uuid();
  v_now timestamptz := now();
  v_operation text;
  v_body jsonb;
begin
  if p_target_status not in ('OPEN','RESOLVED')
     or nullif(btrim(p_reason), '') is null then
    raise exception 'invalid issue transition' using errcode = '22023';
  end if;

  if p_target_status = 'RESOLVED'
     and p_resolution_context = 'CLOSE_FOLLOW_UP_ACCEPTANCE' then
    if not silomath.location_actor_is_active_admin(
      p_actor_user_id, p_organization_id
    ) then
      raise exception 'issue close follow-up forbidden' using errcode = '42501';
    end if;
  elsif not silomath.stocktaking_actor_is_team_member(
    p_actor_user_id, p_organization_id, p_campaign_id
  ) then
    raise exception 'issue mutation forbidden' using errcode = '42501';
  end if;

  if p_target_status = 'RESOLVED' then
    if p_resolution_context not in ('NORMAL','CLOSE_FOLLOW_UP_ACCEPTANCE') then
      raise exception 'invalid issue resolution context' using errcode = '22023';
    end if;
    v_operation := case
      when p_resolution_context = 'CLOSE_FOLLOW_UP_ACCEPTANCE'
        then 'ISSUE_ACCEPT_FOLLOW_UP'
      else 'ISSUE_RESOLVE'
    end;
  else
    if p_resolution_context is not null then
      raise exception 'reopen has no resolution context' using errcode = '22023';
    end if;
    v_operation := 'ISSUE_REOPEN';
  end if;

  select * into v_claim
  from silomath.claim_idempotency_key(
    p_actor_user_id, p_organization_id, v_operation,
    p_idempotency_key, p_request_fingerprint
  );
  if v_claim.disposition = 'REPLAY' then
    return jsonb_build_object('status', v_claim.response_status, 'body', v_claim.response_body);
  end if;

  select i.* into v_before
  from silomath.issues i
  join silomath.inventory_campaigns c
    on c.campaign_id = i.campaign_id
   and c.organization_id = i.organization_id
  where i.issue_id = p_issue_id
    and i.organization_id = p_organization_id
    and i.campaign_id = p_campaign_id
    and c.status = 'ACTIVE'
  for update;

  if not found then
    raise exception 'issue not found or campaign completed' using errcode = 'P0002';
  end if;

  perform silomath.assert_expected_entity_version(
    p_expected_entity_version, v_before.entity_version
  );

  if v_before.status = p_target_status then
    raise exception 'ISSUE_ALREADY_STATE' using errcode = 'P0001';
  end if;

  if p_target_status = 'RESOLVED' then
    update silomath.issues
    set status = 'RESOLVED',
        resolved_at = v_now,
        resolved_by = p_actor_user_id
    where issue_id = p_issue_id
    returning * into v_after;
  else
    update silomath.issues
    set status = 'OPEN',
        resolved_at = null,
        resolved_by = null
    where issue_id = p_issue_id
    returning * into v_after;
  end if;

  insert into silomath.audit_events (
    audit_event_id, organization_id, campaign_id, actor_user_id,
    action, entity_type, entity_id, occurred_at, before_data, after_data,
    change_metadata, correlation_id, source, system_actor
  ) values (
    v_audit_event_id, p_organization_id, p_campaign_id, p_actor_user_id,
    v_operation, 'ISSUE', p_issue_id, v_now,
    silomath.issue_to_json(v_before), silomath.issue_to_json(v_after),
    jsonb_build_object('reason', btrim(p_reason), 'resolutionContext', p_resolution_context),
    p_correlation_id, 'API', null
  );

  if p_target_status = 'RESOLVED' then
    insert into silomath.issue_resolutions (
      issue_resolution_id, issue_id, resolution_reason, resolution_context,
      resolved_at, resolved_by, audit_event_id
    ) values (
      gen_random_uuid(), p_issue_id, btrim(p_reason), p_resolution_context,
      v_now, p_actor_user_id, v_audit_event_id
    );
  end if;

  insert into silomath.issue_status_events (
    issue_status_event_id, issue_id, from_status, to_status, reason,
    changed_at, changed_by, audit_event_id
  ) values (
    gen_random_uuid(), p_issue_id, v_before.status, p_target_status,
    btrim(p_reason), v_now, p_actor_user_id, v_audit_event_id
  );

  v_body := silomath.issue_to_json(v_after);
  perform silomath.sync_emit_change(
    p_organization_id, p_campaign_id, 'ISSUE', p_issue_id,
    v_after.entity_version, 'UPSERT', v_body
  );
  perform silomath.complete_idempotency_key(
    p_actor_user_id, p_organization_id, v_operation,
    p_idempotency_key, p_request_fingerprint, 200, v_body
  );
  return jsonb_build_object('status', 200, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."issue_transition_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_issue_id uuid, p_target_status text, p_reason text, p_resolution_context text, p_expected_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."issue_transition_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_issue_id uuid, p_target_status text, p_reason text, p_resolution_context text, p_expected_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.location_create(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text)
 RETURNS silomath.locations
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_location silomath.locations%rowtype;
    v_now timestamptz := now();
begin
    if not silomath.location_actor_is_active_admin(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'location mutation forbidden'
            using errcode = '42501';
    end if;

    if p_location_id is null then
        raise exception 'location id is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_name), '') is null then
        raise exception 'location name is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_code), '') is null then
        raise exception 'location code is required'
            using errcode = '22023';
    end if;

    if p_lifecycle_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid location lifecycle status'
            using errcode = '22023';
    end if;

    if (p_latitude_deg is null) <> (p_longitude_deg is null) then
        raise exception 'latitude and longitude must both be present or both absent'
            using errcode = '22023';
    end if;

    insert into silomath.locations (
        location_id,
        organization_id,
        name,
        code,
        address,
        city,
        country,
        notes,
        latitude_deg,
        longitude_deg,
        lifecycle_status,
        created_at,
        created_by,
        updated_at,
        updated_by,
        entity_version
    )
    values (
        p_location_id,
        p_organization_id,
        btrim(p_name),
        upper(btrim(p_code)),
        nullif(btrim(p_address), ''),
        nullif(btrim(p_city), ''),
        nullif(btrim(p_country), ''),
        nullif(btrim(p_notes), ''),
        p_latitude_deg,
        p_longitude_deg,
        p_lifecycle_status,
        v_now,
        p_actor_user_id,
        v_now,
        p_actor_user_id,
        1
    )
    returning *
    into v_location;

    return v_location;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."location_create"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."location_create"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.location_get(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid)
 RETURNS silomath.locations
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_location silomath.locations%rowtype;
begin
    if not silomath.location_actor_is_active_member(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'location read forbidden'
            using errcode = '42501';
    end if;

    select *
    into v_location
    from silomath.locations
    where location_id = p_location_id
      and organization_id = p_organization_id;

    if not found then
        raise exception 'location not found'
            using errcode = 'P0002';
    end if;

    return v_location;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."location_get"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."location_get"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.location_list(p_actor_user_id uuid, p_organization_id uuid)
 RETURNS SETOF silomath.locations
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
begin
    if not silomath.location_actor_is_active_member(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'location read forbidden'
            using errcode = '42501';
    end if;

    return query
    select l.*
    from silomath.locations l
    where l.organization_id = p_organization_id
    order by l.code, l.location_id;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."location_list"(p_actor_user_id uuid, p_organization_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."location_list"(p_actor_user_id uuid, p_organization_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.location_list_page(p_actor_user_id uuid, p_organization_id uuid, p_search text DEFAULT NULL::text, p_status text DEFAULT NULL::text, p_after_code text DEFAULT NULL::text, p_after_id uuid DEFAULT NULL::uuid, p_limit integer DEFAULT 50)
 RETURNS SETOF silomath.locations
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_limit integer;
begin
    if not silomath.location_actor_is_active_member(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'location read forbidden'
            using errcode = '42501';
    end if;

    if p_status is not null
       and p_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid location lifecycle status'
            using errcode = '22023';
    end if;

    v_limit := greatest(1, least(coalesce(p_limit, 50), 200));

    return query
    select l.*
    from silomath.locations l
    where l.organization_id = p_organization_id
      and (
        p_status is null
        or l.lifecycle_status = p_status
      )
      and (
        nullif(btrim(p_search), '') is null
        or l.name ilike '%' || btrim(p_search) || '%'
        or l.code ilike '%' || btrim(p_search) || '%'
      )
      and (
        p_after_code is null
        or p_after_id is null
        or (l.code, l.location_id) > (p_after_code, p_after_id)
      )
    order by l.code, l.location_id
    limit v_limit + 1;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."location_list_page"(p_actor_user_id uuid, p_organization_id uuid, p_search text, p_status text, p_after_code text, p_after_id uuid, p_limit integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."location_list_page"(p_actor_user_id uuid, p_organization_id uuid, p_search text, p_status text, p_after_code text, p_after_id uuid, p_limit integer) TO postgres;

CREATE OR REPLACE FUNCTION silomath.location_patch(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid, p_expected_entity_version bigint, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text)
 RETURNS silomath.locations
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_existing silomath.locations%rowtype;
    v_updated silomath.locations%rowtype;
begin
    if not silomath.location_actor_is_active_admin(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'location mutation forbidden'
            using errcode = '42501';
    end if;

    select *
    into v_existing
    from silomath.locations
    where location_id = p_location_id
      and organization_id = p_organization_id
    for update;

    if not found then
        raise exception 'location not found'
            using errcode = 'P0002';
    end if;

    perform silomath.assert_expected_entity_version(
        p_expected_entity_version,
        v_existing.entity_version
    );

    if nullif(btrim(p_name), '') is null then
        raise exception 'location name is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_code), '') is null then
        raise exception 'location code is required'
            using errcode = '22023';
    end if;

    if p_lifecycle_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid location lifecycle status'
            using errcode = '22023';
    end if;

    if (p_latitude_deg is null) <> (p_longitude_deg is null) then
        raise exception 'latitude and longitude must both be present or both absent'
            using errcode = '22023';
    end if;

    update silomath.locations
    set name = btrim(p_name),
        code = upper(btrim(p_code)),
        address = nullif(btrim(p_address), ''),
        city = nullif(btrim(p_city), ''),
        country = nullif(btrim(p_country), ''),
        notes = nullif(btrim(p_notes), ''),
        latitude_deg = p_latitude_deg,
        longitude_deg = p_longitude_deg,
        lifecycle_status = p_lifecycle_status,
        updated_at = now(),
        updated_by = p_actor_user_id
    where location_id = p_location_id
      and organization_id = p_organization_id
    returning *
    into v_updated;

    return v_updated;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."location_patch"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid, p_expected_entity_version bigint, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."location_patch"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid, p_expected_entity_version bigint, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.notification_mark_read_command(p_actor uuid, p_id uuid, p_key text, p_fingerprint text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_row silomath.notifications%rowtype; claim record; result jsonb;
begin
 if not exists(select 1 from silomath.users where user_id=p_actor and lifecycle_status='ACTIVE') then
 raise exception 'active user required' using errcode='42501'; end if;
 select * into claim from silomath.claim_idempotency_key(p_actor,null,'NOTIFICATION_MARK_READ',p_key,p_fingerprint);
 if claim.disposition='REPLAY' then return claim.response_body; end if;
 update silomath.notifications set read_at=coalesce(read_at,now())
 where notification_id=p_id and recipient_user_id=p_actor
 and created_at>=now()-interval '12 months' returning * into v_row;
 if not found then raise exception 'notification not found' using errcode='P0002'; end if;
 result:=silomath.notification_to_json(v_row);
 perform silomath.complete_idempotency_key(p_actor,null,'NOTIFICATION_MARK_READ',p_key,p_fingerprint,200,result);
 return result;
end $function$;
REVOKE ALL ON FUNCTION "silomath"."notification_mark_read_command"(p_actor uuid, p_id uuid, p_key text, p_fingerprint text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."notification_mark_read_command"(p_actor uuid, p_id uuid, p_key text, p_fingerprint text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.notifications_mark_all_read_command(p_actor uuid, p_key text, p_fingerprint text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare claim record; result jsonb:=jsonb_build_object('accepted',true);
begin
 if not exists(select 1 from silomath.users where user_id=p_actor and lifecycle_status='ACTIVE') then
 raise exception 'active user required' using errcode='42501'; end if;
 select * into claim from silomath.claim_idempotency_key(p_actor,null,'NOTIFICATION_MARK_ALL_READ',p_key,p_fingerprint);
 if claim.disposition='REPLAY' then return claim.response_body; end if;
 update silomath.notifications set read_at=now() where recipient_user_id=p_actor
 and read_at is null and created_at>=now()-interval '12 months';
 perform silomath.complete_idempotency_key(p_actor,null,'NOTIFICATION_MARK_ALL_READ',p_key,p_fingerprint,200,result);
 return result;
end $function$;
REVOKE ALL ON FUNCTION "silomath"."notifications_mark_all_read_command"(p_actor uuid, p_key text, p_fingerprint text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."notifications_mark_all_read_command"(p_actor uuid, p_key text, p_fingerprint text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.offline_package_content(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_campaign silomath.inventory_campaigns%rowtype;
begin
    if not silomath.offline_package_actor_authorized(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id,
        p_device_installation_id
    ) then
        raise exception 'offline package forbidden'
            using errcode = '42501';
    end if;

    select *
    into v_campaign
    from silomath.inventory_campaigns
    where campaign_id = p_campaign_id
      and organization_id = p_organization_id;

    if not found then
        raise exception 'campaign not found'
            using errcode = 'P0002';
    end if;

    return jsonb_build_object(
        'organizationId', p_organization_id,
        'campaignId', p_campaign_id,
        'deviceInstallationId', p_device_installation_id,

        'campaign',
        to_jsonb(v_campaign),

        'campaignSnapshot',
        (
            select to_jsonb(cs)
            from silomath.campaign_snapshots cs
            where cs.campaign_id = p_campaign_id
              and cs.organization_id = p_organization_id
        ),

        'scope',
        coalesce(
            (
                select jsonb_agg(to_jsonb(x) order by x.location_code, x.storage_unit_code, x.storage_space_code)
                from silomath.campaign_snapshot_scope_items x
                join silomath.campaign_snapshots cs
                  on cs.campaign_snapshot_id = x.campaign_snapshot_id
                where cs.campaign_id = p_campaign_id
                  and cs.organization_id = p_organization_id
            ),
            '[]'::jsonb
        ),

        'team',
        coalesce(
            (
                select jsonb_agg(to_jsonb(x) order by x.user_id)
                from silomath.campaign_snapshot_team_items x
                join silomath.campaign_snapshots cs
                  on cs.campaign_snapshot_id = x.campaign_snapshot_id
                where cs.campaign_id = p_campaign_id
                  and cs.organization_id = p_organization_id
            ),
            '[]'::jsonb
        ),

        'commodities',
        coalesce(
            (
                select jsonb_agg(
                    jsonb_build_object(
                        'commodityId', c.commodity_id,
                        'code', c.code,
                        'lifecycleStatus', c.lifecycle_status,
                        'commodityVersion', to_jsonb(cv)
                    )
                    order by c.code
                )
                from silomath.campaign_snapshot_commodity_items sci
                join silomath.campaign_snapshots cs
                  on cs.campaign_snapshot_id = sci.campaign_snapshot_id
                join silomath.commodities c
                  on c.commodity_id = sci.commodity_id
                join silomath.commodity_versions cv
                  on cv.commodity_version_id = sci.commodity_version_id
                where cs.campaign_id = p_campaign_id
                  and cs.organization_id = p_organization_id
            ),
            '[]'::jsonb
        ),

        'stocktakingRecords',
        coalesce(
            (
                select jsonb_agg(to_jsonb(sr) order by sr.storage_space_id)
                from silomath.stocktaking_records sr
                where sr.organization_id = p_organization_id
                  and sr.campaign_id = p_campaign_id
            ),
            '[]'::jsonb
        ),

        'stocktakingRevisions',
        coalesce(
            (
                select jsonb_agg(
                    to_jsonb(rv)
                    order by rv.stocktaking_record_id, rv.revision_number
                )
                from silomath.stocktaking_revisions rv
                join silomath.stocktaking_records sr
                  on sr.stocktaking_record_id = rv.stocktaking_record_id
                where sr.organization_id = p_organization_id
                  and sr.campaign_id = p_campaign_id
            ),
            '[]'::jsonb
        ),

        'measurements',
        coalesce(
            (
                select jsonb_agg(
                    to_jsonb(m)
                    order by m.stocktaking_revision_id, m.sequence_number
                )
                from silomath.measurements m
                join silomath.stocktaking_revisions rv
                  on rv.stocktaking_revision_id = m.stocktaking_revision_id
                join silomath.stocktaking_records sr
                  on sr.stocktaking_record_id = rv.stocktaking_record_id
                where sr.organization_id = p_organization_id
                  and sr.campaign_id = p_campaign_id
            ),
            '[]'::jsonb
        ),

        'calculationResults',
        coalesce(
            (
                select jsonb_agg(
                    to_jsonb(cr)
                    order by cr.stocktaking_revision_id
                )
                from silomath.calculation_results cr
                join silomath.stocktaking_revisions rv
                  on rv.stocktaking_revision_id = cr.stocktaking_revision_id
                join silomath.stocktaking_records sr
                  on sr.stocktaking_record_id = rv.stocktaking_record_id
                where sr.organization_id = p_organization_id
                  and sr.campaign_id = p_campaign_id
            ),
            '[]'::jsonb
        ),

        'clients',
        coalesce(
            (
                select jsonb_agg(to_jsonb(c) order by c.code)
                from silomath.clients c
                where c.organization_id = p_organization_id
                  and exists (
                      select 1
                      from silomath.stocktaking_revisions rv
                      join silomath.stocktaking_records sr
                        on sr.stocktaking_record_id = rv.stocktaking_record_id
                      where sr.organization_id = p_organization_id
                        and sr.campaign_id = p_campaign_id
                        and rv.client_id = c.client_id
                  )
            ),
            '[]'::jsonb
        ),

        'issues',
        coalesce(
            (
                select jsonb_agg(to_jsonb(i) order by i.reported_at, i.issue_id)
                from silomath.issues i
                where i.organization_id = p_organization_id
                  and i.campaign_id = p_campaign_id
            ),
            '[]'::jsonb
        ),

        'issueStatusEvents',
        coalesce(
            (
                select jsonb_agg(to_jsonb(e) order by e.changed_at, e.issue_status_event_id)
                from silomath.issue_status_events e
                join silomath.issues i
                  on i.issue_id = e.issue_id
                where i.organization_id = p_organization_id
                  and i.campaign_id = p_campaign_id
            ),
            '[]'::jsonb
        ),

        'issueResolutions',
        coalesce(
            (
                select jsonb_agg(to_jsonb(r) order by r.resolved_at, r.issue_resolution_id)
                from silomath.issue_resolutions r
                join silomath.issues i
                  on i.issue_id = r.issue_id
                where i.organization_id = p_organization_id
                  and i.campaign_id = p_campaign_id
            ),
            '[]'::jsonb
        ),

        'evidence',
        coalesce(
            (
                select jsonb_agg(to_jsonb(e) order by e.created_at, e.evidence_id)
                from silomath.evidence e
                where e.organization_id = p_organization_id
                  and e.campaign_id = p_campaign_id
            ),
            '[]'::jsonb
        )
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."offline_package_content"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."offline_package_content"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.organization_membership_patch_command(p_actor_user_id uuid, p_organization_id uuid, p_membership_id uuid, p_role text, p_lifecycle_status text, p_expected_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_before silomath.organization_memberships%rowtype;
    v_after silomath.organization_memberships%rowtype;
    v_role text;
    v_lifecycle text;
    v_body jsonb;
begin
    if not exists (
        select 1
        from silomath.users u
        join silomath.organization_memberships actor_membership
          on actor_membership.user_id = u.user_id
        join silomath.organizations o
          on o.organization_id = actor_membership.organization_id
        where u.user_id = p_actor_user_id
          and u.lifecycle_status = 'ACTIVE'
          and actor_membership.organization_id = p_organization_id
          and actor_membership.lifecycle_status = 'ACTIVE'
          and actor_membership.role = 'ADMIN'
          and o.lifecycle_status = 'ACTIVE'
    ) then
        raise exception 'organization membership mutation forbidden'
            using errcode = '42501';
    end if;

    v_role := case when p_role is null then null else upper(btrim(p_role)) end;
    v_lifecycle := case when p_lifecycle_status is null then null else upper(btrim(p_lifecycle_status)) end;

    if v_role is null and v_lifecycle is null then
        raise exception 'role or lifecycle status is required'
            using errcode = '22023';
    end if;

    if v_role is not null and v_role not in ('ADMIN', 'STANDARD') then
        raise exception 'invalid organization membership role'
            using errcode = '22023';
    end if;

    if v_lifecycle is not null and v_lifecycle not in ('ACTIVE', 'DISABLED') then
        raise exception 'invalid organization membership lifecycle status'
            using errcode = '22023';
    end if;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'PATCH /memberships/{membershipId}',
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    select *
    into v_before
    from silomath.organization_memberships m
    where m.membership_id = p_membership_id
      and m.organization_id = p_organization_id
    for update;

    if not found then
        raise exception 'organization membership not found'
            using errcode = 'P0002';
    end if;

    perform silomath.assert_expected_entity_version(
        p_expected_entity_version,
        v_before.entity_version
    );

    if v_before.role = 'ADMIN'
       and v_before.lifecycle_status = 'ACTIVE'
       and (
           coalesce(v_role, v_before.role) <> 'ADMIN'
           or coalesce(v_lifecycle, v_before.lifecycle_status) <> 'ACTIVE'
       )
       and not exists (
           select 1
           from silomath.organization_memberships other_admin
           join silomath.users other_user
             on other_user.user_id = other_admin.user_id
           where other_admin.organization_id = p_organization_id
             and other_admin.membership_id <> p_membership_id
             and other_admin.role = 'ADMIN'
             and other_admin.lifecycle_status = 'ACTIVE'
             and other_user.lifecycle_status = 'ACTIVE'
       ) then
        raise exception 'last active admin cannot be disabled or demoted'
            using errcode = '23514';
    end if;

    update silomath.organization_memberships
    set
        role = coalesce(v_role, role),
        lifecycle_status = coalesce(v_lifecycle, lifecycle_status),
        disabled_at = case
            when coalesce(v_lifecycle, lifecycle_status) = 'DISABLED'
                then coalesce(disabled_at, now())
            else null
        end,
        disabled_by = case
            when coalesce(v_lifecycle, lifecycle_status) = 'DISABLED'
                then coalesce(disabled_by, p_actor_user_id)
            else null
        end,
        updated_at = now(),
        updated_by = p_actor_user_id
    where membership_id = p_membership_id
      and organization_id = p_organization_id
    returning * into v_after;

    insert into silomath.audit_events (
        audit_event_id,
        organization_id,
        campaign_id,
        actor_user_id,
        action,
        entity_type,
        entity_id,
        occurred_at,
        before_data,
        after_data,
        change_metadata,
        correlation_id,
        source,
        system_actor
    )
    values (
        gen_random_uuid(),
        p_organization_id,
        null,
        p_actor_user_id,
        'MEMBERSHIP_UPDATE',
        'OrganizationMembership',
        p_membership_id,
        now(),
        to_jsonb(v_before),
        to_jsonb(v_after),
        jsonb_build_object('roleChanged', v_before.role <> v_after.role, 'lifecycleChanged', v_before.lifecycle_status <> v_after.lifecycle_status),
        coalesce(nullif(btrim(p_correlation_id), ''), gen_random_uuid()::text),
        'API_V1',
        null
    );

    perform silomath.sync_emit_change(
        p_organization_id,
        null,
        'OrganizationMembership',
        p_membership_id,
        v_after.entity_version,
        'UPSERT',
        to_jsonb(v_after)
    );

    v_body := jsonb_build_object(
        'membershipId', v_after.membership_id,
        'organizationId', v_after.organization_id,
        'userId', v_after.user_id,
        'role', v_after.role,
        'lifecycleStatus', v_after.lifecycle_status,
        'disabledAt', v_after.disabled_at,
        'disabledBy', v_after.disabled_by,
        'createdAt', v_after.created_at,
        'createdBy', v_after.created_by,
        'updatedAt', v_after.updated_at,
        'updatedBy', v_after.updated_by,
        'entityVersion', v_after.entity_version
    );

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'PATCH /memberships/{membershipId}',
        p_idempotency_key,
        p_request_fingerprint,
        200,
        v_body
    );

    return jsonb_build_object('status', 200, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."organization_membership_patch_command"(p_actor_user_id uuid, p_organization_id uuid, p_membership_id uuid, p_role text, p_lifecycle_status text, p_expected_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."organization_membership_patch_command"(p_actor_user_id uuid, p_organization_id uuid, p_membership_id uuid, p_role text, p_lifecycle_status text, p_expected_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_correlation_id text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.organization_patch_command(p_actor uuid, p_org uuid, p_patch jsonb, p_expected bigint, p_key text, p_fingerprint text, p_correlation text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
 v_claim record; v_before silomath.organizations%rowtype;
 v_after silomath.organizations%rowtype; v_body jsonb;
 v_name text; v_code text; v_address text; v_city text; v_country text; v_status text;
begin
 if not silomath.location_actor_is_active_admin(p_actor,p_org) then
 raise exception 'organization mutation forbidden' using errcode='42501'; end if;
 if p_patch is null or jsonb_typeof(p_patch)<>'object'
 or p_patch='{}'::jsonb or exists(select 1 from jsonb_object_keys(p_patch) k
 where k not in ('name','code','address','city','country','lifecycleStatus')) then
 raise exception 'invalid Organization fields' using errcode='22023'; end if;
 select * into v_claim from silomath.claim_idempotency_key(p_actor,p_org,
 'ORGANIZATION_UPDATE',p_key,p_fingerprint);
 if v_claim.disposition='REPLAY' then
 return jsonb_build_object('status',v_claim.response_status,'body',v_claim.response_body); end if;
 select * into v_before from silomath.organizations where organization_id=p_org for update;
 if not found then raise exception 'organization not found' using errcode='P0002'; end if;
 perform silomath.assert_expected_entity_version(p_expected,v_before.entity_version);
 v_name:=case when p_patch?'name' then btrim(p_patch->>'name') else v_before.name end;
 v_code:=case when p_patch?'code' then upper(btrim(p_patch->>'code')) else v_before.code end;
 v_address:=case when p_patch?'address' then nullif(btrim(p_patch->>'address'),'') else v_before.address end;
 v_city:=case when p_patch?'city' then nullif(btrim(p_patch->>'city'),'') else v_before.city end;
 v_country:=case when p_patch?'country' then p_patch->>'country' else v_before.country end;
 v_status:=case when p_patch?'lifecycleStatus' then p_patch->>'lifecycleStatus' else v_before.lifecycle_status end;
 if v_name is null or length(v_name) not between 1 and 150
 or v_code is null or length(v_code) not between 1 and 50
 or length(v_address)>250 or length(v_city)>100
 or v_status is null or v_status not in ('ACTIVE','INACTIVE') then
 raise exception 'invalid Organization fields' using errcode='22023'; end if;
 if p_patch?'country' and v_country is not null
 and not (v_country=any(array['AD','AE','AF','AG','AI','AL','AM','AO','AQ','AR','AS','AT','AU','AW','AX','AZ','BA','BB','BD','BE','BF','BG','BH','BI','BJ','BL','BM','BN','BO','BQ','BR','BS','BT','BV','BW','BY','BZ','CA','CC','CD','CF','CG','CH','CI','CK','CL','CM','CN','CO','CR','CU','CV','CW','CX','CY','CZ','DE','DJ','DK','DM','DO','DZ','EC','EE','EG','EH','ER','ES','ET','FI','FJ','FK','FM','FO','FR','GA','GB','GD','GE','GF','GG','GH','GI','GL','GM','GN','GP','GQ','GR','GS','GT','GU','GW','GY','HK','HM','HN','HR','HT','HU','ID','IE','IL','IM','IN','IO','IQ','IR','IS','IT','JE','JM','JO','JP','KE','KG','KH','KI','KM','KN','KP','KR','KW','KY','KZ','LA','LB','LC','LI','LK','LR','LS','LT','LU','LV','LY','MA','MC','MD','ME','MF','MG','MH','MK','ML','MM','MN','MO','MP','MQ','MR','MS','MT','MU','MV','MW','MX','MY','MZ','NA','NC','NE','NF','NG','NI','NL','NO','NP','NR','NU','NZ','OM','PA','PE','PF','PG','PH','PK','PL','PM','PN','PR','PS','PT','PW','PY','QA','RE','RO','RS','RU','RW','SA','SB','SC','SD','SE','SG','SH','SI','SJ','SK','SL','SM','SN','SO','SR','SS','ST','SV','SX','SY','SZ','TC','TD','TF','TG','TH','TJ','TK','TL','TM','TN','TO','TR','TT','TV','TW','TZ','UA','UG','UM','US','UY','UZ','VA','VC','VE','VG','VI','VN','VU','WF','WS','YE','YT','ZA','ZM','ZW']::text[])) then
 raise exception 'invalid Country code' using errcode='22023'; end if;
 if v_code<>v_before.code and exists(select 1 from silomath.inventory_campaigns where organization_id=p_org) then
 raise exception 'historically referenced Organization Code is stable' using errcode='55000'; end if;
 update silomath.organizations set name=v_name,code=v_code,address=v_address,
 city=v_city,country=v_country,lifecycle_status=v_status,updated_at=now(),updated_by=p_actor
 where organization_id=p_org returning * into v_after;
 v_body:=jsonb_build_object('id',v_after.organization_id,'name',v_after.name,
 'code',v_after.code,'address',v_after.address,'city',v_after.city,'country',v_after.country,
 'lifecycleStatus',v_after.lifecycle_status,'createdAt',v_after.created_at,
 'createdBy',v_after.created_by,'updatedAt',v_after.updated_at,'updatedBy',v_after.updated_by,
 'entityVersion',v_after.entity_version);
 insert into silomath.audit_events(audit_event_id,organization_id,actor_user_id,
 action,entity_type,entity_id,occurred_at,before_data,after_data,correlation_id,source)
 values(gen_random_uuid(),p_org,p_actor,'ORGANIZATION_UPDATE','ORGANIZATION',p_org,now(),
 to_jsonb(v_before),v_body,p_correlation,'API');
 perform silomath.sync_emit_change(p_org,null,'ORGANIZATION',p_org,v_after.entity_version,'UPSERT',v_body);
 perform silomath.complete_idempotency_key(p_actor,p_org,'ORGANIZATION_UPDATE',
 p_key,p_fingerprint,200,v_body);
 return jsonb_build_object('status',200,'body',v_body);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."organization_patch_command"(p_actor uuid, p_org uuid, p_patch jsonb, p_expected bigint, p_key text, p_fingerprint text, p_correlation text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."organization_patch_command"(p_actor uuid, p_org uuid, p_patch jsonb, p_expected bigint, p_key text, p_fingerprint text, p_correlation text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.report_export_record(p_actor uuid, p_org uuid, p_type text, p_metadata jsonb, p_correlation text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
begin
 if not silomath.location_actor_is_active_member(p_actor,p_org)
 or (p_type='AUDIT_TRAIL' and not silomath.location_actor_is_active_admin(p_actor,p_org)) then
 raise exception 'report export forbidden' using errcode='42501'; end if;
 if p_type not in ('PHYSICAL_INVENTORY','INVENTORY_BY_LOCATION','INVENTORY_BY_COMMODITY',
 'INVENTORY_BY_OWNERSHIP','STORAGE_SPACE_WORKSHEET','CAPACITY_REPORT','INVENTORY_HISTORY',
 'DIGITAL_INVENTORY_DOSSIER','AUDIT_TRAIL') then raise exception 'unknown report type' using errcode='22023'; end if;
 insert into silomath.audit_events(audit_event_id,organization_id,actor_user_id,
 action,entity_type,entity_id,occurred_at,change_metadata,correlation_id,source)
 values(gen_random_uuid(),p_org,p_actor,'REPORT_EXPORT','REPORT',p_org,now(),
 jsonb_build_object('reportType',p_type,'format','CSV','source',p_metadata),p_correlation,'API');
end $function$;
REVOKE ALL ON FUNCTION "silomath"."report_export_record"(p_actor uuid, p_org uuid, p_type text, p_metadata jsonb, p_correlation text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."report_export_record"(p_actor uuid, p_org uuid, p_type text, p_metadata jsonb, p_correlation text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.report_inventory_rows(p_actor uuid, p_org uuid, p_campaign uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare c silomath.inventory_campaigns%rowtype; result jsonb;
begin
 if not silomath.location_actor_is_active_member(p_actor,p_org) then
 raise exception 'report forbidden' using errcode='42501'; end if;
 select * into c from silomath.inventory_campaigns where organization_id=p_org and campaign_id=p_campaign;
 if not found then raise exception 'campaign not found' using errcode='P0002'; end if;
 if c.status='COMPLETED' then
 select coalesce(jsonb_agg(jsonb_build_object(
 'storageSpaceId',e.storage_space_id,'hierarchy',e.hierarchy_snapshot,'geometry',e.geometry_snapshot,
 'commodity',e.commodity_snapshot,'ownershipClient',e.ownership_client_snapshot,
 'isEmpty',e.is_empty,'measurements',e.measurements_snapshot,'operationalValues',e.operational_values,
 'calculationMetadata',e.calculation_metadata,'validationMetadata',e.validation_metadata,
 'issues',e.issue_references_resolution_summary,'evidence',e.evidence_references)
 order by e.storage_space_id),'[]') into result
 from silomath.final_inventory_snapshot_entries e join silomath.final_inventory_snapshots f
 on f.final_snapshot_id=e.final_snapshot_id where f.campaign_id=p_campaign and f.organization_id=p_org;
 if not exists(select 1 from silomath.final_inventory_snapshots where campaign_id=p_campaign) then
 raise exception 'final snapshot unavailable' using errcode='P0002'; end if;
 else
 select coalesce(jsonb_agg(jsonb_build_object(
 'storageSpaceId',sc.storage_space_id,
 'hierarchy',jsonb_build_object('locationId',sc.location_id,'locationCode',sc.location_code,'locationName',sc.location_name,
 'storageUnitId',sc.storage_unit_id,'storageUnitCode',sc.storage_unit_code,'storageUnitName',sc.storage_unit_name,
 'storageSpaceCode',sc.storage_space_code),
 'geometry',jsonb_build_object('geometryProfileVersionId',sc.geometry_profile_version_id,
 'geometryFamilyId',sc.geometry_family_id,'maxCapacityM3',sc.max_capacity_m3),
 'commodity',(select to_jsonb(cv) from silomath.commodity_versions cv where cv.commodity_version_id=v.commodity_version_id),
 'ownershipClient',case when v.is_empty then null else jsonb_build_object('ownership',v.ownership,'client',
 (select to_jsonb(cl) from silomath.clients cl where cl.client_id=v.client_id)) end,
 'isEmpty',v.is_empty,'measurements',(select coalesce(jsonb_agg(to_jsonb(m) order by sequence_number),'[]')
 from silomath.measurements m where m.stocktaking_revision_id=v.stocktaking_revision_id),
 'operationalValues',jsonb_build_object('productVolumeM3',case when v.is_empty then 0 else cr.product_volume_m3 end,
 'physicalQuantityKg',case when v.is_empty then 0 else cr.physical_quantity_kg end,
 'actualBulkDensityKgM3',v.actual_bulk_density_kg_m3),
 'calculationMetadata',to_jsonb(cr),'validationMetadata',jsonb_build_object('revisionState',v.revision_state,
 'validatedAt',v.validated_at,'validatedBy',v.validated_by))
 order by sc.storage_space_id),'[]') into result
 from silomath.campaign_snapshot_scope_items sc
 left join silomath.stocktaking_records r on r.campaign_id=p_campaign and r.storage_space_id=sc.storage_space_id
 left join silomath.stocktaking_revisions v on v.stocktaking_revision_id=r.current_revision_id
 left join silomath.calculation_results cr on cr.calculation_result_id=v.calculation_result_id
 where sc.campaign_snapshot_id=c.campaign_snapshot_id;
 end if;
 return result;
end $function$;
REVOKE ALL ON FUNCTION "silomath"."report_inventory_rows"(p_actor uuid, p_org uuid, p_campaign uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."report_inventory_rows"(p_actor uuid, p_org uuid, p_campaign uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.stocktaking_actor_is_admin_team_member(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
    select
        silomath.stocktaking_actor_is_team_member(
            p_actor_user_id,
            p_organization_id,
            p_campaign_id
        )
        and exists (
            select 1
            from silomath.organization_memberships om
            where om.organization_id = p_organization_id
              and om.user_id = p_actor_user_id
              and om.lifecycle_status = 'ACTIVE'
              and om.role = 'ADMIN'
        );
$function$;
REVOKE ALL ON FUNCTION "silomath"."stocktaking_actor_is_admin_team_member"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."stocktaking_actor_is_admin_team_member"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.stocktaking_mark_empty_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_notes text, p_idempotency_key text, p_request_fingerprint text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_record silomath.stocktaking_records%rowtype;
    v_revision silomath.stocktaking_revisions%rowtype;
    v_before_revision silomath.stocktaking_revisions%rowtype;
    v_claim record;
    v_response jsonb;
    v_now timestamptz := now();
begin
    if not silomath.stocktaking_actor_is_team_member(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id
    ) then
        raise exception 'STOCKTAKING_NOT_TEAM_MEMBER'
            using errcode = '42501';
    end if;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'STOCKTAKING_MARK_EMPTY',
        btrim(p_idempotency_key),
        btrim(p_request_fingerprint)
    );

    if v_claim.disposition = 'REPLAY' then
        return v_claim.response_body;
    end if;

    select *
    into v_record
    from silomath.stocktaking_records
    where stocktaking_record_id = p_stocktaking_record_id
      and organization_id = p_organization_id
      and campaign_id = p_campaign_id
    for update;

    if not found then
        raise exception 'stocktaking record not found'
            using errcode = 'P0002';
    end if;

    if v_record.current_revision_id is null then
        raise exception 'current revision missing'
            using errcode = '22023';
    end if;

    select *
    into v_revision
    from silomath.stocktaking_revisions
    where stocktaking_revision_id = v_record.current_revision_id
      and stocktaking_record_id = v_record.stocktaking_record_id
    for update;

    if not found then
        raise exception 'current revision not found'
            using errcode = 'P0002';
    end if;

    if v_revision.revision_state <> 'WORKING' then
        raise exception 'STOCKTAKING_REVISION_STALE'
            using errcode = '40001';
    end if;

    perform silomath.assert_expected_entity_version(
        p_expected_revision_entity_version,
        v_revision.entity_version
    );

    if exists (
        select 1
        from silomath.measurements m
        where m.stocktaking_revision_id = v_revision.stocktaking_revision_id
    ) then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023',
                  detail = 'mark empty requires zero measurements';
    end if;

    v_before_revision := v_revision;

    update silomath.stocktaking_revisions
    set
        revision_state = 'VALIDATED',
        is_empty = true,
        commodity_version_id = null,
        actual_bulk_density_kg_m3 = null,
        ownership = null,
        client_id = null,
        notes = p_notes,
        calculation_result_id = null,
        validated_at = v_now,
        validated_by = p_actor_user_id
    where stocktaking_revision_id = v_revision.stocktaking_revision_id
    returning * into v_revision;

    update silomath.stocktaking_records
    set
        updated_at = v_now,
        updated_by = p_actor_user_id
    where stocktaking_record_id = v_record.stocktaking_record_id
    returning * into v_record;

    insert into silomath.audit_events (
        audit_event_id,
        organization_id,
        campaign_id,
        actor_user_id,
        action,
        entity_type,
        entity_id,
        occurred_at,
        before_data,
        after_data,
        change_metadata,
        correlation_id,
        source,
        system_actor
    )
    values (
        gen_random_uuid(),
        p_organization_id,
        p_campaign_id,
        p_actor_user_id,
        'MARK_EMPTY',
        'StocktakingRevision',
        v_revision.stocktaking_revision_id,
        v_now,
        jsonb_build_object(
            'revisionState', v_before_revision.revision_state,
            'isEmpty', v_before_revision.is_empty,
            'entityVersion', v_before_revision.entity_version
        ),
        jsonb_build_object(
            'revisionState', 'VALIDATED',
            'isEmpty', true,
            'productVolumeM3', 0,
            'physicalQuantityKg', 0,
            'entityVersion', v_revision.entity_version
        ),
        null,
        gen_random_uuid()::text,
        'SERVER',
        null
    );

    perform silomath.sync_emit_change(
        p_organization_id,
        p_campaign_id,
        'StocktakingRecord',
        v_record.stocktaking_record_id,
        v_record.entity_version,
        'UPSERT',
        jsonb_build_object(
            'stocktakingRecordId', v_record.stocktaking_record_id,
            'currentRevisionId', v_revision.stocktaking_revision_id,
            'revisionState', 'VALIDATED',
            'isEmpty', true
        )
    );

    v_response := jsonb_build_object(
        'stocktakingRevisionId', v_revision.stocktaking_revision_id,
        'stocktakingRecordId', v_revision.stocktaking_record_id,
        'revisionNumber', v_revision.revision_number,
        'revisionState', v_revision.revision_state,
        'isEmpty', v_revision.is_empty,
        'validatedAt', v_revision.validated_at,
        'validatedBy', v_revision.validated_by,
        'entityVersion', v_revision.entity_version
    );

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'STOCKTAKING_MARK_EMPTY',
        btrim(p_idempotency_key),
        btrim(p_request_fingerprint),
        200,
        v_response
    );

    return v_response;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."stocktaking_mark_empty_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_notes text, p_idempotency_key text, p_request_fingerprint text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."stocktaking_mark_empty_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_notes text, p_idempotency_key text, p_request_fingerprint text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.stocktaking_record_create_working(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_storage_space_id uuid, p_stocktaking_revision_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_now timestamptz := now();
begin
    if not silomath.stocktaking_actor_is_team_member(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id
    ) then
        raise exception 'stocktaking mutation forbidden'
            using errcode = '42501';
    end if;

    if p_stocktaking_record_id is null
       or p_storage_space_id is null
       or p_stocktaking_revision_id is null then
        raise exception 'stocktaking identifiers required'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.campaign_scopes cs
        where cs.campaign_id = p_campaign_id
          and cs.storage_space_id = p_storage_space_id
    ) then
        raise exception 'storage space outside campaign scope'
            using errcode = '22023';
    end if;

    if exists (
        select 1
        from silomath.stocktaking_records sr
        where sr.campaign_id = p_campaign_id
          and sr.storage_space_id = p_storage_space_id
    ) then
        raise exception 'stocktaking record already exists'
            using errcode = '23505';
    end if;

    insert into silomath.stocktaking_records (
        stocktaking_record_id,
        organization_id,
        campaign_id,
        storage_space_id,
        current_revision_id,
        created_at,
        created_by,
        updated_at,
        updated_by,
        entity_version
    )
    values (
        p_stocktaking_record_id,
        p_organization_id,
        p_campaign_id,
        p_storage_space_id,
        null,
        v_now,
        p_actor_user_id,
        v_now,
        p_actor_user_id,
        1
    );

    /*
     * A newly-created WORKING revision is initially represented as empty
     * working input. Mark Empty remains an explicit command and validation
     * state is not inferred from this value.
     */
    insert into silomath.stocktaking_revisions (
        stocktaking_revision_id,
        stocktaking_record_id,
        revision_number,
        revision_state,
        based_on_revision_id,
        is_empty,
        commodity_version_id,
        actual_bulk_density_kg_m3,
        ownership,
        client_id,
        notes,
        calculation_result_id,
        validated_at,
        validated_by,
        created_at,
        created_by,
        entity_version
    )
    values (
        p_stocktaking_revision_id,
        p_stocktaking_record_id,
        1,
        'WORKING',
        null,
        true,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        v_now,
        p_actor_user_id,
        1
    );

    update silomath.stocktaking_records
    set
        current_revision_id = p_stocktaking_revision_id,
        updated_at = v_now,
        updated_by = p_actor_user_id
    where stocktaking_record_id = p_stocktaking_record_id;

    return jsonb_build_object(
        'stocktakingRecordId', p_stocktaking_record_id,
        'stocktakingRevisionId', p_stocktaking_revision_id,
        'revisionNumber', 1,
        'revisionState', 'WORKING'
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."stocktaking_record_create_working"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_storage_space_id uuid, p_stocktaking_revision_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."stocktaking_record_create_working"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_storage_space_id uuid, p_stocktaking_revision_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.stocktaking_validate_nonempty_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_authoritative_calculation jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_record silomath.stocktaking_records%rowtype;
    v_revision silomath.stocktaking_revisions%rowtype;
    v_calculation silomath.calculation_results%rowtype;
    v_scope silomath.campaign_snapshot_scope_items%rowtype;
    v_measurement_count integer;
    v_claim record;
    v_response jsonb;
    v_now timestamptz := now();

    v_input_fingerprint text;
    v_formula_code text;
    v_formula_version text;
    v_geometry_family_id uuid;
    v_geometry_profile_version_id uuid;
begin
    if not silomath.stocktaking_actor_is_team_member(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id
    ) then
        raise exception 'STOCKTAKING_NOT_TEAM_MEMBER'
            using errcode = '42501';
    end if;

    if p_idempotency_key is null
       or nullif(btrim(p_idempotency_key), '') is null then
        raise exception 'idempotency key required'
            using errcode = '22023';
    end if;

    -- Frozen API contract: idempotencyKey is UUID.
    perform btrim(p_idempotency_key)::uuid;

    if nullif(btrim(p_request_fingerprint), '') is null then
        raise exception 'request fingerprint required'
            using errcode = '22023';
    end if;

    if p_authoritative_calculation is null
       or jsonb_typeof(p_authoritative_calculation) <> 'object' then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023';
    end if;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'STOCKTAKING_VALIDATE_NONEMPTY',
        btrim(p_idempotency_key),
        btrim(p_request_fingerprint)
    );

    if v_claim.disposition = 'REPLAY' then
        return v_claim.response_body;
    end if;

    select *
    into v_record
    from silomath.stocktaking_records
    where stocktaking_record_id = p_stocktaking_record_id
      and organization_id = p_organization_id
      and campaign_id = p_campaign_id
    for update;

    if not found then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = 'P0002';
    end if;

    select *
    into v_revision
    from silomath.stocktaking_revisions
    where stocktaking_revision_id = v_record.current_revision_id
      and stocktaking_record_id = v_record.stocktaking_record_id
    for update;

    if not found then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = 'P0002';
    end if;

    if v_revision.revision_state <> 'WORKING' then
        raise exception 'STOCKTAKING_REVISION_STALE'
            using errcode = '40001';
    end if;

    if v_revision.entity_version <> p_expected_revision_entity_version then
        raise exception 'STOCKTAKING_REVISION_STALE'
            using errcode = '40001';
    end if;

    if v_revision.is_empty
       or v_revision.commodity_version_id is null
       or v_revision.actual_bulk_density_kg_m3 is null
       or v_revision.actual_bulk_density_kg_m3 <= 0
       or v_revision.ownership not in ('OWNED','THIRD_PARTY')
       or (v_revision.ownership = 'THIRD_PARTY' and v_revision.client_id is null)
       or (v_revision.ownership = 'OWNED' and v_revision.client_id is not null)
       or v_revision.calculation_result_id is null then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023';
    end if;

    select count(*)
    into v_measurement_count
    from silomath.measurements m
    where m.stocktaking_revision_id = v_revision.stocktaking_revision_id;

    if v_measurement_count < 1 or v_measurement_count > 5 then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023';
    end if;

    if exists (
        select 1
        from silomath.measurements m
        where m.stocktaking_revision_id = v_revision.stocktaking_revision_id
          and (
              m.sequence_number < 1
              or m.sequence_number > 5
              or m.raw_value < 0
              or m.normalized_ullage_m < 0
          )
    ) then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023';
    end if;

    select cssi.*
    into v_scope
    from silomath.campaign_snapshot_scope_items cssi
    join silomath.campaign_snapshots cs
      on cs.campaign_snapshot_id = cssi.campaign_snapshot_id
    where cs.campaign_id = p_campaign_id
      and cs.organization_id = p_organization_id
      and cssi.storage_space_id = v_record.storage_space_id;

    if not found then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = 'P0002';
    end if;

    if exists (
        select 1
        from silomath.measurements m
        where m.stocktaking_revision_id = v_revision.stocktaking_revision_id
          and m.geometry_profile_version_id <> v_scope.geometry_profile_version_id
    ) then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023';
    end if;

    select *
    into v_calculation
    from silomath.calculation_results cr
    where cr.calculation_result_id = v_revision.calculation_result_id
      and cr.stocktaking_revision_id = v_revision.stocktaking_revision_id
    for update;

    if not found then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = 'P0002';
    end if;

    v_input_fingerprint :=
        lower(nullif(btrim(p_authoritative_calculation->>'inputFingerprint'), ''));

    v_formula_code :=
        nullif(btrim(p_authoritative_calculation->>'formulaCode'), '');

    v_formula_version :=
        nullif(btrim(p_authoritative_calculation->>'formulaVersion'), '');

    v_geometry_family_id :=
        nullif(p_authoritative_calculation->>'geometryFamilyId','')::uuid;

    v_geometry_profile_version_id :=
        nullif(p_authoritative_calculation->>'geometryProfileVersionId','')::uuid;

    if v_input_fingerprint is null
       or v_input_fingerprint !~ '^[0-9a-f]{64}$'
       or v_formula_code <> v_scope.geometry_family_code
       or v_formula_version <> v_scope.geometry_formula_version
       or v_geometry_family_id is distinct from v_scope.geometry_family_id
       or v_geometry_profile_version_id is distinct from v_scope.geometry_profile_version_id
       or p_authoritative_calculation->>'surfaceModelVersion' <> 'LEVEL_BASED_V1'
       or p_authoritative_calculation->>'aggregationMethod' <> 'MEAN_ULLAGE'
       or nullif(p_authoritative_calculation->>'aggregationRuleVersion','')::integer <> 1
       or p_authoritative_calculation->>'inputSchemaVersion' <> '1'
       or p_authoritative_calculation->>'engineVersion' <> '2.0'
       or p_authoritative_calculation->>'normalizationVersion' <> '1'
       or p_authoritative_calculation->>'roundingPolicy' <> 'ROUND_HALF_UP' then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023';
    end if;

    /*
     * Replace the provisional WORKING CalculationResult with the
     * server-authoritative calculation in the same transaction that validates
     * the revision. After VALIDATED, existing immutability guards apply.
     */
    update silomath.calculation_results
    set
        surface_model_version = p_authoritative_calculation->>'surfaceModelVersion',
        aggregation_method = p_authoritative_calculation->>'aggregationMethod',
        aggregation_rule_version = (p_authoritative_calculation->>'aggregationRuleVersion')::integer,
        input_schema_version = p_authoritative_calculation->>'inputSchemaVersion',
        engine_version = p_authoritative_calculation->>'engineVersion',
        normalization_version = p_authoritative_calculation->>'normalizationVersion',
        formula_code = v_formula_code,
        formula_version = v_formula_version,
        geometry_family_id = v_geometry_family_id,
        geometry_profile_version_id = v_geometry_profile_version_id,
        frozen_calculation_context =
            coalesce(p_authoritative_calculation->'frozenCalculationContext', '{}'::jsonb),
        datum_metadata = p_authoritative_calculation->'datumMetadata',
        geometry_inputs_snapshot = p_authoritative_calculation->'geometryInputsSnapshot',
        ordered_normalized_readings = p_authoritative_calculation->'orderedNormalizedReadings',
        geometry_origin = p_authoritative_calculation->>'geometryOrigin',
        measurement_reference_height_m =
            (p_authoritative_calculation->>'measurementReferenceHeightM')::numeric,
        maximum_fill_height_m =
            (p_authoritative_calculation->>'maximumFillHeightM')::numeric,
        max_capacity_m3 =
            (p_authoritative_calculation->>'maxCapacityM3')::numeric,
        effective_ullage_m =
            (p_authoritative_calculation->>'effectiveUllageM')::numeric,
        effective_fill_height_m =
            (p_authoritative_calculation->>'effectiveFillHeightM')::numeric,
        product_volume_m3 =
            (p_authoritative_calculation->>'productVolumeM3')::numeric,
        entered_density_value =
            (p_authoritative_calculation->>'enteredDensityValue')::numeric,
        entered_density_unit =
            p_authoritative_calculation->>'enteredDensityUnit',
        actual_bulk_density_kg_m3 =
            (p_authoritative_calculation->>'actualBulkDensityKgM3')::numeric,
        physical_quantity_kg =
            (p_authoritative_calculation->>'physicalQuantityKg')::numeric,
        input_fingerprint = v_input_fingerprint,
        rounding_policy = p_authoritative_calculation->>'roundingPolicy',
        calculated_at = v_now,
        calculated_by = p_actor_user_id,
        calculation_source = 'SERVER'
    where calculation_result_id = v_calculation.calculation_result_id
      and stocktaking_revision_id = v_revision.stocktaking_revision_id
    returning * into v_calculation;

    if v_calculation.actual_bulk_density_kg_m3
       <> v_revision.actual_bulk_density_kg_m3 then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023';
    end if;

    if exists (
        select 1
        from silomath.evidence e
        where e.organization_id = p_organization_id
          and e.campaign_id = p_campaign_id
          and e.stocktaking_record_id = v_record.stocktaking_record_id
          and e.issue_id is null
          and e.removed_at is null
          and e.stocktaking_revision_id is not null
          and e.stocktaking_revision_id <> v_revision.stocktaking_revision_id
    ) then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023';
    end if;

    if (
        select count(*)
        from silomath.evidence e
        where e.organization_id = p_organization_id
          and e.campaign_id = p_campaign_id
          and e.stocktaking_record_id = v_record.stocktaking_record_id
          and e.issue_id is null
          and e.evidence_type = 'PHOTO'
          and e.removed_at is null
          and (e.stocktaking_revision_id is null
               or e.stocktaking_revision_id = v_revision.stocktaking_revision_id)
    ) > 5 then
        raise exception 'STOCKTAKING_VALIDATION_FAILED'
            using errcode = '22023';
    end if;

    update silomath.evidence
    set stocktaking_revision_id = v_revision.stocktaking_revision_id
    where organization_id = p_organization_id
      and campaign_id = p_campaign_id
      and stocktaking_record_id = v_record.stocktaking_record_id
      and issue_id is null
      and removed_at is null
      and stocktaking_revision_id is null;

    update silomath.stocktaking_revisions
    set
        revision_state = 'VALIDATED',
        validated_at = v_now,
        validated_by = p_actor_user_id
    where stocktaking_revision_id = v_revision.stocktaking_revision_id
    returning * into v_revision;

    update silomath.stocktaking_records
    set
        updated_at = v_now,
        updated_by = p_actor_user_id
    where stocktaking_record_id = v_record.stocktaking_record_id
    returning * into v_record;

    insert into silomath.audit_events (
        audit_event_id,
        organization_id,
        campaign_id,
        actor_user_id,
        action,
        entity_type,
        entity_id,
        occurred_at,
        before_data,
        after_data,
        change_metadata,
        correlation_id,
        source,
        system_actor
    )
    values (
        gen_random_uuid(),
        p_organization_id,
        p_campaign_id,
        p_actor_user_id,
        'VALIDATE_STOCKTAKING',
        'StocktakingRevision',
        v_revision.stocktaking_revision_id,
        v_now,
        jsonb_build_object('revisionState','WORKING'),
        jsonb_build_object(
            'revisionState','VALIDATED',
            'isEmpty',false,
            'calculationResultId',v_calculation.calculation_result_id,
            'inputFingerprint',v_calculation.input_fingerprint
        ),
        null,
        gen_random_uuid()::text,
        'SERVER',
        null
    );

    perform silomath.sync_emit_change(
        p_organization_id,
        p_campaign_id,
        'StocktakingRecord',
        v_record.stocktaking_record_id,
        v_record.entity_version,
        'UPSERT',
        jsonb_build_object(
            'stocktakingRecordId',v_record.stocktaking_record_id,
            'currentRevisionId',v_revision.stocktaking_revision_id,
            'revisionState','VALIDATED',
            'isEmpty',false,
            'calculationResultId',v_calculation.calculation_result_id
        )
    );

    v_response := jsonb_build_object(
        'stocktakingRecordId',v_record.stocktaking_record_id,
        'stocktakingRevisionId',v_revision.stocktaking_revision_id,
        'revisionNumber',v_revision.revision_number,
        'revisionState',v_revision.revision_state,
        'isEmpty',false,
        'calculationResultId',v_calculation.calculation_result_id,
        'inputFingerprint',v_calculation.input_fingerprint,
        'productVolumeM3',v_calculation.product_volume_m3,
        'physicalQuantityKg',v_calculation.physical_quantity_kg,
        'validatedAt',v_revision.validated_at,
        'validatedBy',v_revision.validated_by,
        'entityVersion',v_revision.entity_version
    );

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'STOCKTAKING_VALIDATE_NONEMPTY',
        btrim(p_idempotency_key),
        btrim(p_request_fingerprint),
        200,
        v_response
    );

    return v_response;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."stocktaking_validate_nonempty_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_authoritative_calculation jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."stocktaking_validate_nonempty_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_idempotency_key text, p_request_fingerprint text, p_authoritative_calculation jsonb) TO postgres;

CREATE OR REPLACE FUNCTION silomath.stocktaking_working_upsert_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_commodity_version_id uuid, p_actual_bulk_density_kg_m3 numeric, p_ownership text, p_client_id uuid, p_notes text, p_measurements jsonb, p_calculation_result jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_record silomath.stocktaking_records%rowtype;
    v_revision silomath.stocktaking_revisions%rowtype;
    v_scope silomath.campaign_snapshot_scope_items%rowtype;
    v_measurement jsonb;
    v_measurement_count integer;
    v_sequence integer;
    v_geometry_profile_version_id uuid;
    v_expected_geometry_profile_version_id uuid;

    v_calculation_result_id uuid;
    v_geometry_family_id uuid;
    v_surface_model_version text;
    v_aggregation_method text;
    v_aggregation_rule_version integer;
    v_input_schema_version text;
    v_engine_version text;
    v_normalization_version text;
    v_formula_code text;
    v_formula_version text;
    v_input_fingerprint text;
begin
    if not silomath.stocktaking_actor_is_team_member(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id
    ) then
        raise exception 'stocktaking mutation forbidden'
            using errcode = '42501';
    end if;

    select *
    into v_record
    from silomath.stocktaking_records
    where stocktaking_record_id = p_stocktaking_record_id
      and organization_id = p_organization_id
      and campaign_id = p_campaign_id
    for update;

    if not found then
        raise exception 'stocktaking record not found'
            using errcode = 'P0002';
    end if;

    select *
    into v_revision
    from silomath.stocktaking_revisions
    where stocktaking_revision_id = v_record.current_revision_id
      and stocktaking_record_id = v_record.stocktaking_record_id
    for update;

    if not found then
        raise exception 'current revision not found'
            using errcode = 'P0002';
    end if;

    if v_revision.revision_state <> 'WORKING' then
        raise exception 'validated revision is immutable'
            using errcode = '40001';
    end if;

    perform silomath.assert_expected_entity_version(p_expected_revision_entity_version, v_revision.entity_version);

    if p_commodity_version_id is null then
        raise exception 'commodity version required'
            using errcode = '22023';
    end if;

    if p_actual_bulk_density_kg_m3 is null
       or p_actual_bulk_density_kg_m3 <= 0 then
        raise exception 'actual bulk density must be positive'
            using errcode = '22023';
    end if;

    if p_ownership not in ('OWNED', 'THIRD_PARTY') then
        raise exception 'invalid ownership'
            using errcode = '22023';
    end if;

    if p_ownership = 'THIRD_PARTY' and p_client_id is null then
        raise exception 'client required for third party ownership'
            using errcode = '22023';
    end if;

    if p_ownership = 'OWNED' and p_client_id is not null then
        raise exception 'client must be absent for owned stock'
            using errcode = '22023';
    end if;

    if p_client_id is not null
       and not exists (
           select 1
           from silomath.clients c
           where c.client_id = p_client_id
             and c.organization_id = p_organization_id
             and c.lifecycle_status = 'ACTIVE'
       ) then
        raise exception 'client not available in organization'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.commodity_versions cv
        join silomath.commodities c
          on c.commodity_id = cv.commodity_id
        where cv.commodity_version_id = p_commodity_version_id
          and c.lifecycle_status = 'ACTIVE'
    ) then
        raise exception 'commodity version not available'
            using errcode = '22023';
    end if;

    if p_measurements is null
       or jsonb_typeof(p_measurements) <> 'array' then
        raise exception 'measurements must be an array'
            using errcode = '22023';
    end if;

    v_measurement_count := jsonb_array_length(p_measurements);

    if v_measurement_count < 1 or v_measurement_count > 5 then
        raise exception 'measurement count must be between 1 and 5'
            using errcode = '22023';
    end if;

    select cssi.*
    into v_scope
    from silomath.campaign_snapshot_scope_items cssi
    join silomath.campaign_snapshots cs
      on cs.campaign_snapshot_id = cssi.campaign_snapshot_id
    where cs.campaign_id = p_campaign_id
      and cs.organization_id = p_organization_id
      and cssi.storage_space_id = v_record.storage_space_id;

    if not found then
        raise exception 'campaign snapshot scope item not found'
            using errcode = 'P0002';
    end if;

    v_expected_geometry_profile_version_id :=
        v_scope.geometry_profile_version_id;

    /*
     * Validate ordered measurement contract before replacing current WORKING
     * measurements.
     */
    v_sequence := 0;

    for v_measurement in
        select value
        from jsonb_array_elements(p_measurements)
    loop
        v_sequence := v_sequence + 1;

        if nullif(v_measurement->>'measurementId','') is null
           or nullif(v_measurement->>'sequenceNumber','') is null
           or nullif(v_measurement->>'rawValue','') is null
           or nullif(btrim(v_measurement->>'rawUnit'),'') is null
           or nullif(v_measurement->>'normalizedUllageM','') is null
           or v_measurement->'datumMetadata' is null
           or nullif(v_measurement->>'geometryProfileVersionId','') is null
           or nullif(v_measurement->>'capturedAt','') is null
           or nullif(v_measurement->>'capturedBy','') is null then
            raise exception 'malformed measurement'
                using errcode = '22023';
        end if;

        if (v_measurement->>'sequenceNumber')::integer <> v_sequence then
            raise exception 'measurement sequence must be contiguous from 1'
                using errcode = '22023';
        end if;

        v_geometry_profile_version_id :=
            (v_measurement->>'geometryProfileVersionId')::uuid;

        if v_geometry_profile_version_id
           <> v_expected_geometry_profile_version_id then
            raise exception 'geometry profile version mismatch'
                using errcode = '22023';
        end if;

        if (v_measurement->>'normalizedUllageM')::numeric < 0 then
            raise exception 'normalized ullage cannot be negative'
                using errcode = '22023';
        end if;
    end loop;

    if p_calculation_result is null
       or jsonb_typeof(p_calculation_result) <> 'object' then
        raise exception 'calculation result required'
            using errcode = '22023';
    end if;

    v_calculation_result_id :=
        nullif(p_calculation_result->>'calculationResultId','')::uuid;
    v_geometry_family_id :=
        nullif(p_calculation_result->>'geometryFamilyId','')::uuid;
    v_geometry_profile_version_id :=
        nullif(p_calculation_result->>'geometryProfileVersionId','')::uuid;
    v_surface_model_version :=
        nullif(btrim(p_calculation_result->>'surfaceModelVersion'),'');
    v_aggregation_method :=
        nullif(btrim(p_calculation_result->>'aggregationMethod'),'');
    v_aggregation_rule_version :=
        nullif(p_calculation_result->>'aggregationRuleVersion','')::integer;
    v_input_schema_version :=
        nullif(btrim(p_calculation_result->>'inputSchemaVersion'),'');
    v_engine_version :=
        nullif(btrim(p_calculation_result->>'engineVersion'),'');
    v_normalization_version :=
        nullif(btrim(p_calculation_result->>'normalizationVersion'),'');
    v_formula_code :=
        nullif(btrim(p_calculation_result->>'formulaCode'),'');
    v_formula_version :=
        nullif(btrim(p_calculation_result->>'formulaVersion'),'');
    v_input_fingerprint :=
        lower(nullif(btrim(p_calculation_result->>'inputFingerprint'),''));

    if v_calculation_result_id is null
       or v_geometry_family_id is null
       or v_geometry_profile_version_id is null
       or v_surface_model_version <> 'LEVEL_BASED_V1'
       or v_aggregation_method <> 'MEAN_ULLAGE'
       or v_aggregation_rule_version <> 1
       or v_input_schema_version <> '1'
       or v_engine_version <> '2.0'
       or v_normalization_version <> '1'
       or v_formula_version <> '1'
       or v_formula_code <> v_scope.geometry_family_code
       or v_formula_version <> v_scope.geometry_formula_version
       or v_input_fingerprint is null
       or v_input_fingerprint !~ '^[0-9a-f]{64}$' then
        raise exception 'invalid calculation metadata'
            using errcode = '22023';
    end if;

    if v_geometry_profile_version_id
       <> v_expected_geometry_profile_version_id
       or v_geometry_family_id is distinct from v_scope.geometry_family_id then
        raise exception 'calculation geometry mismatch'
            using errcode = '22023';
    end if;

    /*
     * WORKING input replacement is allowed. A prior calculation becomes stale
     * and is not mutated; the revision is rebound only to the newly submitted
     * calculation result.
     */
    delete from silomath.measurements
    where stocktaking_revision_id = v_revision.stocktaking_revision_id;

    for v_measurement in
        select value
        from jsonb_array_elements(p_measurements)
    loop
        insert into silomath.measurements (
            measurement_id,
            stocktaking_revision_id,
            sequence_number,
            raw_value,
            raw_unit,
            normalized_ullage_m,
            datum_metadata,
            geometry_profile_version_id,
            captured_at,
            captured_by,
            entity_version
        )
        values (
            (v_measurement->>'measurementId')::uuid,
            v_revision.stocktaking_revision_id,
            (v_measurement->>'sequenceNumber')::integer,
            (v_measurement->>'rawValue')::numeric,
            v_measurement->>'rawUnit',
            (v_measurement->>'normalizedUllageM')::numeric,
            v_measurement->'datumMetadata',
            (v_measurement->>'geometryProfileVersionId')::uuid,
            (v_measurement->>'capturedAt')::timestamptz,
            (v_measurement->>'capturedBy')::uuid,
            1
        );
    end loop;

    insert into silomath.calculation_results (
        calculation_result_id,
        stocktaking_revision_id,
        surface_model_version,
        aggregation_method,
        aggregation_rule_version,
        input_schema_version,
        engine_version,
        normalization_version,
        formula_code,
        formula_version,
        geometry_family_id,
        geometry_profile_version_id,
        frozen_calculation_context,
        datum_metadata,
        geometry_inputs_snapshot,
        ordered_normalized_readings,
        geometry_origin,
        measurement_reference_height_m,
        maximum_fill_height_m,
        max_capacity_m3,
        effective_ullage_m,
        effective_fill_height_m,
        product_volume_m3,
        entered_density_value,
        entered_density_unit,
        actual_bulk_density_kg_m3,
        physical_quantity_kg,
        input_fingerprint,
        rounding_policy,
        calculated_at,
        calculated_by,
        calculation_source
    )
    values (
        v_calculation_result_id,
        v_revision.stocktaking_revision_id,
        v_surface_model_version,
        v_aggregation_method,
        v_aggregation_rule_version,
        v_input_schema_version,
        v_engine_version,
        v_normalization_version,
        v_formula_code,
        v_formula_version,
        v_geometry_family_id,
        v_geometry_profile_version_id,
        coalesce(p_calculation_result->'frozenCalculationContext','{}'::jsonb),
        p_calculation_result->'datumMetadata',
        p_calculation_result->'geometryInputsSnapshot',
        p_calculation_result->'orderedNormalizedReadings',
        p_calculation_result->>'geometryOrigin',
        nullif(p_calculation_result->>'measurementReferenceHeightM','')::numeric,
        nullif(p_calculation_result->>'maximumFillHeightM','')::numeric,
        nullif(p_calculation_result->>'maxCapacityM3','')::numeric,
        nullif(p_calculation_result->>'effectiveUllageM','')::numeric,
        nullif(p_calculation_result->>'effectiveFillHeightM','')::numeric,
        (p_calculation_result->>'productVolumeM3')::numeric,
        coalesce(
            nullif(p_calculation_result->>'enteredDensityValue','')::numeric,
            p_actual_bulk_density_kg_m3
        ),
        coalesce(
            nullif(p_calculation_result->>'enteredDensityUnit',''),
            'kg/m3'
        ),
        p_actual_bulk_density_kg_m3,
        (p_calculation_result->>'physicalQuantityKg')::numeric,
        v_input_fingerprint,
        p_calculation_result->>'roundingPolicy',
        (p_calculation_result->>'calculatedAt')::timestamptz,
        nullif(p_calculation_result->>'calculatedBy','')::uuid,
        p_calculation_result->>'calculationSource'
    );

    update silomath.stocktaking_revisions
    set
        is_empty = false,
        commodity_version_id = p_commodity_version_id,
        actual_bulk_density_kg_m3 = p_actual_bulk_density_kg_m3,
        ownership = p_ownership,
        client_id = p_client_id,
        notes = p_notes,
        calculation_result_id = v_calculation_result_id
    where stocktaking_revision_id = v_revision.stocktaking_revision_id
    returning * into v_revision;

    update silomath.stocktaking_records
    set
        updated_at = now(),
        updated_by = p_actor_user_id
    where stocktaking_record_id = v_record.stocktaking_record_id
    returning * into v_record;

    return jsonb_build_object(
        'stocktakingRecordId', v_record.stocktaking_record_id,
        'stocktakingRevisionId', v_revision.stocktaking_revision_id,
        'revisionNumber', v_revision.revision_number,
        'revisionState', v_revision.revision_state,
        'isEmpty', false,
        'calculationResultId', v_calculation_result_id,
        'entityVersion', v_revision.entity_version
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."stocktaking_working_upsert_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_commodity_version_id uuid, p_actual_bulk_density_kg_m3 numeric, p_ownership text, p_client_id uuid, p_notes text, p_measurements jsonb, p_calculation_result jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."stocktaking_working_upsert_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_commodity_version_id uuid, p_actual_bulk_density_kg_m3 numeric, p_ownership text, p_client_id uuid, p_notes text, p_measurements jsonb, p_calculation_result jsonb) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_create(p_actor_user_id uuid, p_organization_id uuid, p_storage_space_id uuid, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text)
 RETURNS silomath.storage_spaces
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_storage_space silomath.storage_spaces%rowtype;
    v_now timestamptz := now();
begin
    if not silomath.storage_space_actor_is_active_admin(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage space mutation forbidden'
            using errcode = '42501';
    end if;

    if p_storage_space_id is null then
        raise exception 'storage space id is required'
            using errcode = '22023';
    end if;

    if p_storage_unit_id is null then
        raise exception 'storage unit id is required'
            using errcode = '22023';
    end if;

    if p_geometry_profile_id is null then
        raise exception 'geometry profile id is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_code), '') is null then
        raise exception 'storage space code is required'
            using errcode = '22023';
    end if;

    if p_lifecycle_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid storage space lifecycle status'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.storage_units su
        where su.storage_unit_id = p_storage_unit_id
          and su.organization_id = p_organization_id
    ) then
        raise exception 'storage unit not found'
            using errcode = 'P0002';
    end if;

    if not exists (
        select 1
        from silomath.geometry_profiles gp
        where gp.geometry_profile_id = p_geometry_profile_id
          and gp.organization_id = p_organization_id
    ) then
        raise exception 'geometry profile not found'
            using errcode = 'P0002';
    end if;

    insert into silomath.storage_spaces (
        storage_space_id,
        organization_id,
        storage_unit_id,
        code,
        description,
        geometry_profile_id,
        lifecycle_status,
        created_at,
        created_by,
        updated_at,
        updated_by,
        entity_version
    )
    values (
        p_storage_space_id,
        p_organization_id,
        p_storage_unit_id,
        upper(btrim(p_code)),
        nullif(btrim(p_description), ''),
        p_geometry_profile_id,
        p_lifecycle_status,
        v_now,
        p_actor_user_id,
        v_now,
        p_actor_user_id,
        1
    )
    returning *
    into v_storage_space;

    return v_storage_space;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_create"(p_actor_user_id uuid, p_organization_id uuid, p_storage_space_id uuid, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_create"(p_actor_user_id uuid, p_organization_id uuid, p_storage_space_id uuid, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_get(p_actor_user_id uuid, p_organization_id uuid, p_storage_space_id uuid)
 RETURNS silomath.storage_spaces
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_storage_space silomath.storage_spaces%rowtype;
begin
    if not silomath.storage_space_actor_is_active_member(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage space read forbidden'
            using errcode = '42501';
    end if;

    select *
    into v_storage_space
    from silomath.storage_spaces
    where storage_space_id = p_storage_space_id
      and organization_id = p_organization_id;

    if not found then
        raise exception 'storage space not found'
            using errcode = 'P0002';
    end if;

    return v_storage_space;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_get"(p_actor_user_id uuid, p_organization_id uuid, p_storage_space_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_get"(p_actor_user_id uuid, p_organization_id uuid, p_storage_space_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_list(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid DEFAULT NULL::uuid)
 RETURNS SETOF silomath.storage_spaces
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
begin
    if not silomath.storage_space_actor_is_active_member(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage space read forbidden'
            using errcode = '42501';
    end if;

    return query
    select ss.*
    from silomath.storage_spaces ss
    where ss.organization_id = p_organization_id
      and (p_storage_unit_id is null or ss.storage_unit_id = p_storage_unit_id)
    order by ss.code, ss.storage_space_id;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_list"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_list"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_list_page(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_status text DEFAULT NULL::text, p_after_code text DEFAULT NULL::text, p_after_id uuid DEFAULT NULL::uuid, p_limit integer DEFAULT 50)
 RETURNS SETOF silomath.storage_spaces
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_limit integer;
begin
    if not silomath.storage_space_actor_is_active_member(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage space read forbidden'
            using errcode = '42501';
    end if;

    if p_storage_unit_id is null then
        raise exception 'storage unit id is required'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.storage_units su
        where su.storage_unit_id = p_storage_unit_id
          and su.organization_id = p_organization_id
    ) then
        raise exception 'storage unit not found'
            using errcode = 'P0002';
    end if;

    if p_status is not null
       and p_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid storage space lifecycle status'
            using errcode = '22023';
    end if;

    v_limit := greatest(1, least(coalesce(p_limit, 50), 200));

    return query
    select ss.*
    from silomath.storage_spaces ss
    where ss.organization_id = p_organization_id
      and ss.storage_unit_id = p_storage_unit_id
      and (
        p_status is null
        or ss.lifecycle_status = p_status
      )
      and (
        p_after_code is null
        or p_after_id is null
        or (ss.code, ss.storage_space_id) > (p_after_code, p_after_id)
      )
    order by ss.code, ss.storage_space_id
    limit v_limit + 1;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_list_page"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_status text, p_after_code text, p_after_id uuid, p_limit integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_list_page"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_status text, p_after_code text, p_after_id uuid, p_limit integer) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_patch(p_actor_user_id uuid, p_organization_id uuid, p_storage_space_id uuid, p_expected_entity_version bigint, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text)
 RETURNS silomath.storage_spaces
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_existing silomath.storage_spaces%rowtype;
    v_updated silomath.storage_spaces%rowtype;
begin
    if not silomath.storage_space_actor_is_active_admin(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage space mutation forbidden'
            using errcode = '42501';
    end if;

    select *
    into v_existing
    from silomath.storage_spaces
    where storage_space_id = p_storage_space_id
      and organization_id = p_organization_id
    for update;

    if not found then
        raise exception 'storage space not found'
            using errcode = 'P0002';
    end if;

    perform silomath.assert_expected_entity_version(
        p_expected_entity_version,
        v_existing.entity_version
    );

    if p_storage_unit_id is null then
        raise exception 'storage unit id is required'
            using errcode = '22023';
    end if;

    if p_geometry_profile_id is null then
        raise exception 'geometry profile id is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_code), '') is null then
        raise exception 'storage space code is required'
            using errcode = '22023';
    end if;

    if p_lifecycle_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid storage space lifecycle status'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.storage_units su
        where su.storage_unit_id = p_storage_unit_id
          and su.organization_id = p_organization_id
    ) then
        raise exception 'storage unit not found'
            using errcode = 'P0002';
    end if;

    if not exists (
        select 1
        from silomath.geometry_profiles gp
        where gp.geometry_profile_id = p_geometry_profile_id
          and gp.organization_id = p_organization_id
    ) then
        raise exception 'geometry profile not found'
            using errcode = 'P0002';
    end if;

    update silomath.storage_spaces
    set storage_unit_id = p_storage_unit_id,
        code = upper(btrim(p_code)),
        description = nullif(btrim(p_description), ''),
        geometry_profile_id = p_geometry_profile_id,
        lifecycle_status = p_lifecycle_status,
        updated_at = now(),
        updated_by = p_actor_user_id
    where storage_space_id = p_storage_space_id
      and organization_id = p_organization_id
    returning *
    into v_updated;

    return v_updated;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_patch"(p_actor_user_id uuid, p_organization_id uuid, p_storage_space_id uuid, p_expected_entity_version bigint, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_patch"(p_actor_user_id uuid, p_organization_id uuid, p_storage_space_id uuid, p_expected_entity_version bigint, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_unit_create(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text)
 RETURNS silomath.storage_units
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_storage_unit silomath.storage_units%rowtype;
    v_now timestamptz := now();
begin
    if not silomath.storage_unit_actor_is_active_admin(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage unit mutation forbidden'
            using errcode = '42501';
    end if;

    if p_storage_unit_id is null then
        raise exception 'storage unit id is required'
            using errcode = '22023';
    end if;

    if p_location_id is null then
        raise exception 'location id is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_name), '') is null then
        raise exception 'storage unit name is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_code), '') is null then
        raise exception 'storage unit code is required'
            using errcode = '22023';
    end if;

    if p_lifecycle_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid storage unit lifecycle status'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.locations l
        where l.location_id = p_location_id
          and l.organization_id = p_organization_id
    ) then
        raise exception 'location not found'
            using errcode = 'P0002';
    end if;

    insert into silomath.storage_units (
        storage_unit_id,
        organization_id,
        location_id,
        name,
        code,
        description,
        lifecycle_status,
        created_at,
        created_by,
        updated_at,
        updated_by,
        entity_version
    )
    values (
        p_storage_unit_id,
        p_organization_id,
        p_location_id,
        btrim(p_name),
        upper(btrim(p_code)),
        nullif(btrim(p_description), ''),
        p_lifecycle_status,
        v_now,
        p_actor_user_id,
        v_now,
        p_actor_user_id,
        1
    )
    returning *
    into v_storage_unit;

    return v_storage_unit;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_unit_create"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_unit_create"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_unit_get(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid)
 RETURNS silomath.storage_units
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_storage_unit silomath.storage_units%rowtype;
begin
    if not silomath.storage_unit_actor_is_active_member(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage unit read forbidden'
            using errcode = '42501';
    end if;

    select *
    into v_storage_unit
    from silomath.storage_units
    where storage_unit_id = p_storage_unit_id
      and organization_id = p_organization_id;

    if not found then
        raise exception 'storage unit not found'
            using errcode = 'P0002';
    end if;

    return v_storage_unit;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_unit_get"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_unit_get"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_unit_list(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid DEFAULT NULL::uuid)
 RETURNS SETOF silomath.storage_units
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
begin
    if not silomath.storage_unit_actor_is_active_member(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage unit read forbidden'
            using errcode = '42501';
    end if;

    return query
    select su.*
    from silomath.storage_units su
    where su.organization_id = p_organization_id
      and (p_location_id is null or su.location_id = p_location_id)
    order by su.code, su.storage_unit_id;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_unit_list"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_unit_list"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_unit_list_page(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid, p_search text DEFAULT NULL::text, p_status text DEFAULT NULL::text, p_after_code text DEFAULT NULL::text, p_after_id uuid DEFAULT NULL::uuid, p_limit integer DEFAULT 50)
 RETURNS SETOF silomath.storage_units
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_limit integer;
begin
    if not silomath.storage_unit_actor_is_active_member(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage unit read forbidden'
            using errcode = '42501';
    end if;

    if p_location_id is null then
        raise exception 'location id is required'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.locations l
        where l.location_id = p_location_id
          and l.organization_id = p_organization_id
    ) then
        raise exception 'location not found'
            using errcode = 'P0002';
    end if;

    if p_status is not null
       and p_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid storage unit lifecycle status'
            using errcode = '22023';
    end if;

    v_limit := greatest(1, least(coalesce(p_limit, 50), 200));

    return query
    select su.*
    from silomath.storage_units su
    where su.organization_id = p_organization_id
      and su.location_id = p_location_id
      and (
        p_status is null
        or su.lifecycle_status = p_status
      )
      and (
        nullif(btrim(p_search), '') is null
        or su.name ilike '%' || btrim(p_search) || '%'
        or su.code ilike '%' || btrim(p_search) || '%'
      )
      and (
        p_after_code is null
        or p_after_id is null
        or (su.code, su.storage_unit_id) > (p_after_code, p_after_id)
      )
    order by su.code, su.storage_unit_id
    limit v_limit + 1;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_unit_list_page"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid, p_search text, p_status text, p_after_code text, p_after_id uuid, p_limit integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_unit_list_page"(p_actor_user_id uuid, p_organization_id uuid, p_location_id uuid, p_search text, p_status text, p_after_code text, p_after_id uuid, p_limit integer) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_unit_patch(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_expected_entity_version bigint, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text)
 RETURNS silomath.storage_units
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_existing silomath.storage_units%rowtype;
    v_updated silomath.storage_units%rowtype;
begin
    if not silomath.storage_unit_actor_is_active_admin(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage unit mutation forbidden'
            using errcode = '42501';
    end if;

    select *
    into v_existing
    from silomath.storage_units
    where storage_unit_id = p_storage_unit_id
      and organization_id = p_organization_id
    for update;

    if not found then
        raise exception 'storage unit not found'
            using errcode = 'P0002';
    end if;

    perform silomath.assert_expected_entity_version(
        p_expected_entity_version,
        v_existing.entity_version
    );

    if p_location_id is null then
        raise exception 'location id is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_name), '') is null then
        raise exception 'storage unit name is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_code), '') is null then
        raise exception 'storage unit code is required'
            using errcode = '22023';
    end if;

    if p_lifecycle_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid storage unit lifecycle status'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.locations l
        where l.location_id = p_location_id
          and l.organization_id = p_organization_id
    ) then
        raise exception 'location not found'
            using errcode = 'P0002';
    end if;

    update silomath.storage_units
    set location_id = p_location_id,
        name = btrim(p_name),
        code = upper(btrim(p_code)),
        description = nullif(btrim(p_description), ''),
        lifecycle_status = p_lifecycle_status,
        updated_at = now(),
        updated_by = p_actor_user_id
    where storage_unit_id = p_storage_unit_id
      and organization_id = p_organization_id
    returning *
    into v_updated;

    return v_updated;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_unit_patch"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_expected_entity_version bigint, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_unit_patch"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_expected_entity_version bigint, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.sync_pull_page(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid DEFAULT NULL::uuid, p_checkpoint bigint DEFAULT NULL::bigint, p_limit integer DEFAULT 200)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_limit integer;
    v_checkpoint bigint := coalesce(p_checkpoint, 0);
    v_next_checkpoint bigint;
    v_changes jsonb;
    v_has_more boolean;
begin
    if not silomath.sync_actor_is_active_member(p_actor_user_id, p_organization_id) then
        raise exception 'sync pull forbidden'
            using errcode = '42501';
    end if;

    if p_actor_user_id is null then
        raise exception 'actor required'
            using errcode = '22023';
    end if;

    v_limit := greatest(1, least(coalesce(p_limit, 200), 500));

    with page as (
        select
            s.change_sequence,
            s.entity_type,
            s.entity_id,
            s.entity_version,
            s.change_type,
            s.server_changed_at,
            s.authoritative_data
        from silomath.sync_change_stream s
        where s.organization_id = p_organization_id
          and s.change_sequence > v_checkpoint
          and (p_campaign_id is null or s.campaign_id = p_campaign_id)
        order by s.change_sequence
        limit v_limit
    )
    select
        coalesce(
            jsonb_agg(
                jsonb_build_object(
                    'entityType', p.entity_type,
                    'entityId', p.entity_id,
                    'entityVersion', p.entity_version,
                    'changeType', p.change_type,
                    'serverChangedAt', p.server_changed_at,
                    'data', p.authoritative_data
                )
                order by p.change_sequence
            ),
            '[]'::jsonb
        ),
        max(p.change_sequence)
    into v_changes, v_next_checkpoint
    from page p;

    v_next_checkpoint := coalesce(v_next_checkpoint, v_checkpoint);

    select exists (
        select 1
        from silomath.sync_change_stream s
        where s.organization_id = p_organization_id
          and s.change_sequence > v_next_checkpoint
          and (p_campaign_id is null or s.campaign_id = p_campaign_id)
    )
    into v_has_more;

    return jsonb_build_object(
        'changes', v_changes,
        'nextCheckpoint', v_next_checkpoint::text,
        'hasMore', v_has_more
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."sync_pull_page"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_checkpoint bigint, p_limit integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."sync_pull_page"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_checkpoint bigint, p_limit integer) TO postgres;

CREATE OR REPLACE FUNCTION silomath.sync_server_status(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid DEFAULT NULL::uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_pending bigint;
    v_failed bigint;
    v_conflicts bigint;
begin
    if not silomath.sync_actor_is_active_member(p_actor_user_id, p_organization_id) then
        raise exception 'sync status forbidden'
            using errcode = '42501';
    end if;

    if p_actor_user_id is null then
        raise exception 'actor required'
            using errcode = '22023';
    end if;

    select
        count(*) filter (
            where state in ('PENDING', 'IN_FLIGHT', 'RETRYABLE_ERROR')
        ),
        count(*) filter (
            where state = 'PERMANENT_ERROR'
        )
    into v_pending, v_failed
    from silomath.sync_operations
    where organization_id = p_organization_id
      and (p_campaign_id is null or campaign_id = p_campaign_id);

    select count(*)
    into v_conflicts
    from silomath.sync_conflicts sc
    join silomath.sync_operations so
      on so.sync_operation_id = sc.sync_operation_id
    where sc.organization_id = p_organization_id
      and sc.status = 'OPEN'
      and (p_campaign_id is null or so.campaign_id = p_campaign_id);

    return jsonb_build_object(
        'pendingCount', coalesce(v_pending, 0),
        'failedCount', coalesce(v_failed, 0),
        'conflictCount', coalesce(v_conflicts, 0),
        'serverTimeUtc', now()
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."sync_server_status"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."sync_server_status"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.user_preferences_patch_command(p_actor uuid, p_patch jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
begin
 if not exists(select 1 from silomath.users where user_id=p_actor and lifecycle_status='ACTIVE') then
 raise exception 'active user required' using errcode='42501'; end if;
 if p_patch is null or jsonb_typeof(p_patch)<>'object' or p_patch='{}'::jsonb
 or exists(select 1 from jsonb_object_keys(p_patch) k where k<>'theme')
 or p_patch->>'theme' is null or p_patch->>'theme' not in ('SYSTEM','LIGHT','DARK') then
 raise exception 'only approved Theme preference may be changed' using errcode='22023'; end if;
 insert into silomath.user_preferences(user_preference_id,user_id,theme,notification_preferences,updated_at)
 values(gen_random_uuid(),p_actor,p_patch->>'theme',null,now())
 on conflict(user_id) do update set theme=excluded.theme,
 updated_at=case when silomath.user_preferences.theme=excluded.theme
 then silomath.user_preferences.updated_at else excluded.updated_at end;
 return silomath.user_preferences_get(p_actor);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."user_preferences_patch_command"(p_actor uuid, p_patch jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."user_preferences_patch_command"(p_actor uuid, p_patch jsonb) TO postgres;

CREATE OR REPLACE FUNCTION silomath.campaign_close_command(p_actor uuid, p_org uuid, p_campaign uuid, p_expected bigint, p_key text, p_fingerprint text, p_correlation text, p_schema text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare claim record; c silomath.inventory_campaigns%rowtype; s silomath.campaign_snapshots%rowtype;
 readiness jsonb; result jsonb; snapshot_id uuid:=gen_random_uuid(); closed timestamptz:=now();
 item record; v silomath.stocktaking_revisions%rowtype; calc silomath.calculation_results%rowtype;
 count_entries bigint; version bigint;
begin
 if not silomath.location_actor_is_active_admin(p_actor,p_org) then
 raise exception 'close requires active Admin' using errcode='42501'; end if;
 if nullif(btrim(p_schema),'') is null then raise exception 'snapshot compatibility unavailable' using errcode='22023'; end if;
 select * into claim from silomath.claim_idempotency_key(p_actor,p_org,'CAMPAIGN_CLOSE',p_key,p_fingerprint);
 if claim.disposition='REPLAY' then return jsonb_build_object('status',claim.response_status,'body',claim.response_body); end if;
 select * into c from silomath.inventory_campaigns where campaign_id=p_campaign and organization_id=p_org for update;
 if not found then raise exception 'campaign not found' using errcode='P0002'; end if;
 perform silomath.assert_expected_entity_version(p_expected,c.entity_version);
 -- Serialize against current aggregate writes, retaining validated versions.
 perform 1 from silomath.stocktaking_records where campaign_id=p_campaign order by stocktaking_record_id for update;
 perform 1 from silomath.stocktaking_revisions locked_revision join silomath.stocktaking_records locked_record
 on locked_record.stocktaking_record_id=locked_revision.stocktaking_record_id
 where locked_record.campaign_id=p_campaign order by locked_revision.stocktaking_revision_id for update of locked_revision;
 perform 1 from silomath.issues where campaign_id=p_campaign for update;
 perform 1 from silomath.evidence where campaign_id=p_campaign for update;
 readiness:=silomath.campaign_close_readiness(p_actor,p_org,p_campaign);
 if not (readiness->>'canClose')::boolean then raise exception 'CLOSE_GUARDS_FAILED' using errcode='P0001',detail=readiness::text; end if;
 select * into s from silomath.campaign_snapshots where campaign_snapshot_id=c.campaign_snapshot_id;
 insert into silomath.final_inventory_snapshots(final_snapshot_id,organization_id,campaign_id,campaign_snapshot_id,
 closed_at,closed_by,snapshot_schema_version,calculation_compatibility_versions,created_at,created_by)
 values(snapshot_id,p_org,p_campaign,c.campaign_snapshot_id,closed,p_actor,p_schema,
 coalesce(s.calculation_schema_versions,'{}'::jsonb),closed,p_actor);
 for item in select sc.*,r.current_revision_id,r.stocktaking_record_id
 from silomath.campaign_snapshot_scope_items sc join silomath.stocktaking_records r
 on r.campaign_id=p_campaign and r.storage_space_id=sc.storage_space_id
 where sc.campaign_snapshot_id=c.campaign_snapshot_id
 loop
 select * into v from silomath.stocktaking_revisions where stocktaking_revision_id=item.current_revision_id;
 select * into calc from silomath.calculation_results where calculation_result_id=v.calculation_result_id;
 if not v.is_empty and (calc.calculation_result_id is null or v.commodity_version_id is null
 or v.actual_bulk_density_kg_m3 is null or v.actual_bulk_density_kg_m3<=0
 or v.ownership is null or v.ownership not in ('OWNED','THIRD_PARTY')
 or (v.ownership='THIRD_PARTY' and v.client_id is null)
 or calc.product_volume_m3<0 or calc.physical_quantity_kg<0
 or calc.geometry_profile_version_id is distinct from item.geometry_profile_version_id
 or nullif(btrim(calc.input_fingerprint),'') is null
 or (select count(*) from silomath.measurements where stocktaking_revision_id=v.stocktaking_revision_id) not between 1 and 5
 or exists(select 1 from silomath.measurements m where m.stocktaking_revision_id=v.stocktaking_revision_id
 and m.geometry_profile_version_id is distinct from item.geometry_profile_version_id)
 or not exists(select 1 from silomath.campaign_snapshot_commodity_items ci
 where ci.campaign_snapshot_id=c.campaign_snapshot_id and ci.commodity_version_id=v.commodity_version_id)) then
 raise exception 'CLOSE_GUARDS_FAILED' using errcode='P0001'; end if;
 insert into silomath.final_inventory_snapshot_entries(entry_id,final_snapshot_id,storage_space_id,
 hierarchy_snapshot,geometry_snapshot,commodity_snapshot,ownership_client_snapshot,is_empty,
 measurements_snapshot,operational_values,calculation_metadata,validation_metadata,
 issue_references_resolution_summary,evidence_references)
 values(gen_random_uuid(),snapshot_id,item.storage_space_id,
 jsonb_build_object('organizationId',p_org,'organizationName',s.organization_name,'organizationCode',s.organization_code,
 'locationId',item.location_id,'locationCode',item.location_code,'locationName',item.location_name,
 'storageUnitId',item.storage_unit_id,'storageUnitCode',item.storage_unit_code,'storageUnitName',item.storage_unit_name,
 'storageSpaceId',item.storage_space_id,'storageSpaceCode',item.storage_space_code),
 jsonb_build_object('geometryFamilyId',item.geometry_family_id,'geometryFamilyCode',item.geometry_family_code,
 'geometryProfileId',item.geometry_profile_id,'geometryProfileVersionId',item.geometry_profile_version_id,
 'geometryProfileCode',item.geometry_profile_code,'geometryProfileName',item.geometry_profile_name,
 'formulaVersion',item.geometry_formula_version,'dimensions',item.geometry_dimensions,
 'measurementReferenceHeightM',item.measurement_reference_height_m,'maximumFillHeightM',item.maximum_fill_height_m,
 'maxCapacityM3',item.max_capacity_m3),
 case when v.is_empty then null else (select to_jsonb(cv) from silomath.commodity_versions cv where commodity_version_id=v.commodity_version_id) end,
 case when v.is_empty then null else jsonb_build_object('ownership',v.ownership,'client',
 (select to_jsonb(cl) from silomath.clients cl where client_id=v.client_id)) end,
 v.is_empty,(select coalesce(jsonb_agg(to_jsonb(m) order by sequence_number),'[]') from silomath.measurements m
 where stocktaking_revision_id=v.stocktaking_revision_id),
 jsonb_build_object('productVolumeM3',case when v.is_empty then 0 else calc.product_volume_m3 end,
 'physicalQuantityKg',case when v.is_empty then 0 else calc.physical_quantity_kg end,
 'actualBulkDensityKgM3',case when v.is_empty then null else v.actual_bulk_density_kg_m3 end),
 case when v.is_empty then coalesce(s.calculation_schema_versions,'{}') else to_jsonb(calc) end,
 jsonb_build_object('stocktakingRecordId',item.stocktaking_record_id,'stocktakingRevisionId',v.stocktaking_revision_id,
 'revisionNumber',v.revision_number,'validatedAt',v.validated_at,'validatedBy',v.validated_by),
 (select coalesce(jsonb_agg(jsonb_build_object('issue',silomath.issue_to_json(i),
 'resolutions',(select coalesce(jsonb_agg(to_jsonb(ir)),'[]') from silomath.issue_resolutions ir where ir.issue_id=i.issue_id))),'[]')
 from silomath.issues i where i.stocktaking_record_id=item.stocktaking_record_id),
 (select coalesce(jsonb_agg(silomath.evidence_to_json(e)),'[]') from silomath.evidence e
 where e.campaign_id=p_campaign and e.removed_at is null and
 (e.stocktaking_revision_id=v.stocktaking_revision_id or e.issue_id in
 (select issue_id from silomath.issues where stocktaking_record_id=item.stocktaking_record_id))));
 end loop;
 select count(*) into count_entries from silomath.final_inventory_snapshot_entries where final_snapshot_id=snapshot_id;
 if count_entries<>(readiness->>'scopeCount')::bigint then raise exception 'CLOSE_GUARDS_FAILED' using errcode='P0001'; end if;
 update silomath.inventory_campaigns set status='COMPLETED',completed_at=closed,completed_by=p_actor,
 updated_at=closed,updated_by=p_actor where campaign_id=p_campaign returning entity_version into version;
 result:=silomath.final_inventory_snapshot_get(p_actor,p_org,p_campaign);
 insert into silomath.audit_events(audit_event_id,organization_id,campaign_id,actor_user_id,
 action,entity_type,entity_id,occurred_at,before_data,after_data,correlation_id,source)
 values(gen_random_uuid(),p_org,p_campaign,p_actor,'CAMPAIGN_CLOSE','CAMPAIGN',p_campaign,closed,
 to_jsonb(c),jsonb_build_object('status','COMPLETED','finalSnapshotId',snapshot_id),p_correlation,'API');
 perform silomath.sync_emit_change(p_org,p_campaign,'CAMPAIGN',p_campaign,version,'UPSERT',
 jsonb_build_object('campaignId',p_campaign,'status','COMPLETED','finalSnapshotId',snapshot_id,'entityVersion',version));
 perform silomath.complete_idempotency_key(p_actor,p_org,'CAMPAIGN_CLOSE',p_key,p_fingerprint,200,result);
 return jsonb_build_object('status',200,'body',result);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."campaign_close_command"(p_actor uuid, p_org uuid, p_campaign uuid, p_expected bigint, p_key text, p_fingerprint text, p_correlation text, p_schema text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."campaign_close_command"(p_actor uuid, p_org uuid, p_campaign uuid, p_expected bigint, p_key text, p_fingerprint text, p_correlation text, p_schema text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.geometry_profile_mutate_command(p_actor uuid, p_org uuid, p_id uuid, p_patch jsonb, p_capacity numeric, p_schema text, p_expected bigint, p_key text, p_fingerprint text, p_correlation text, p_create boolean)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
<<geometry_values>>
declare claim record; old silomath.geometry_profiles%rowtype; updated silomath.geometry_profiles%rowtype;
 version silomath.geometry_profile_versions%rowtype; new_version uuid:=gen_random_uuid();
 family uuid; name text; code text; lifecycle text; d jsonb; body jsonb;
 operation text:=case when p_create then 'GEOMETRY_PROFILE_CREATE' else 'GEOMETRY_PROFILE_UPDATE' end;
 calculation_changed boolean;
begin
 if not silomath.location_actor_is_active_admin(p_actor,p_org) then raise exception 'geometry Admin required' using errcode='42501'; end if;
 select * into claim from silomath.claim_idempotency_key(p_actor,p_org,operation,p_key,p_fingerprint);
 if claim.disposition='REPLAY' then return jsonb_build_object('status',claim.response_status,'body',claim.response_body); end if;
 if not p_create then
 select * into old from silomath.geometry_profiles where geometry_profile_id=p_id and organization_id=p_org for update;
 if not found then raise exception 'geometry profile not found' using errcode='P0002'; end if;
 perform silomath.assert_expected_entity_version(p_expected,old.entity_version);
 select * into version from silomath.geometry_profile_versions where geometry_profile_version_id=old.current_version_id;
 end if;
 name:=case when p_patch?'name' then btrim(p_patch->>'name') else old.name end;
 code:=case when p_patch?'code' then upper(btrim(p_patch->>'code')) else old.code end;
 lifecycle:=coalesce(p_patch->>'lifecycleStatus',case when p_create then 'ACTIVE' else old.lifecycle_status end);
 family:=coalesce((p_patch->>'geometryFamilyId')::uuid,old.geometry_family_id);
 if name is null or length(name) not between 1 and 150 or code is null or length(code) not between 1 and 50
 or lifecycle not in ('ACTIVE','INACTIVE') or not exists(select 1 from silomath.geometry_families where geometry_family_id=family) then
 raise exception 'invalid geometry profile fields' using errcode='22023'; end if;
 if not p_create and code<>old.code and exists(select 1 from silomath.campaign_snapshot_scope_items where geometry_profile_id=p_id) then
 raise exception 'historically referenced Geometry Code is stable' using errcode='55000'; end if;
 calculation_changed:=p_create or p_patch?'dimensions' or p_patch?'geometryFamilyId'
 or p_patch?'measurementReferenceHeightM' or p_patch?'maximumFillHeightM' or p_patch?'geometryOrigin' or p_patch?'areaProvenance' or p_patch?'hasConstantCrossSection';
 if calculation_changed then
 if p_capacity is null or p_capacity<=0 or nullif(btrim(p_schema),'') is null or jsonb_typeof(p_patch->'dimensions') is distinct from 'object'
 then raise exception 'authoritative geometry calculation required' using errcode='22023'; end if;
 d:=p_patch->'dimensions';
 if p_create then
 -- One SQL statement resolves both immediate circular foreign keys.
 with profile as (
 insert into silomath.geometry_profiles(geometry_profile_id,organization_id,name,code,geometry_family_id,lifecycle_status,
 current_version_id,created_at,created_by,updated_at,updated_by,entity_version)
 values(p_id,p_org,name,code,family,lifecycle,new_version,now(),p_actor,now(),p_actor,1) returning geometry_profile_id
 )
 insert into silomath.geometry_profile_versions(geometry_profile_version_id,geometry_profile_id,version_number,geometry_family_id,
 diameter_m,cylinder_height_m,hopper_height_m,outlet_diameter_m,side_length_m,length_m,width_m,body_height_m,height_m,
 verified_area_m2,area_provenance,measurement_reference_height_m,maximum_fill_height_m,geometry_origin,max_capacity_m3,
 formula_version,schema_version,created_at,created_by,has_constant_cross_section)
 select new_version,profile.geometry_profile_id,1,family,
 (d->>'diameterM')::numeric,(d->>'cylinderHeightM')::numeric,(d->>'hopperHeightM')::numeric,(d->>'outletDiameterM')::numeric,
 (d->>'sideM')::numeric,(d->>'lengthM')::numeric,(d->>'widthM')::numeric,(d->>'bodyHeightM')::numeric,(d->>'heightM')::numeric,
 (d->>'areaM2')::numeric,p_patch->>'areaProvenance',(p_patch->>'measurementReferenceHeightM')::numeric,
 (p_patch->>'maximumFillHeightM')::numeric,p_patch->>'geometryOrigin',p_capacity,'1',p_schema,now(),p_actor,(p_patch->>'hasConstantCrossSection')::boolean from profile;
 else
 insert into silomath.geometry_profile_versions(geometry_profile_version_id,geometry_profile_id,version_number,geometry_family_id,
 diameter_m,cylinder_height_m,hopper_height_m,outlet_diameter_m,side_length_m,length_m,width_m,body_height_m,height_m,
 verified_area_m2,area_provenance,measurement_reference_height_m,maximum_fill_height_m,geometry_origin,max_capacity_m3,
 formula_version,schema_version,created_at,created_by,has_constant_cross_section)
 values(new_version,p_id,version.version_number+1,family,
 (d->>'diameterM')::numeric,(d->>'cylinderHeightM')::numeric,(d->>'hopperHeightM')::numeric,(d->>'outletDiameterM')::numeric,
 (d->>'sideM')::numeric,(d->>'lengthM')::numeric,(d->>'widthM')::numeric,(d->>'bodyHeightM')::numeric,(d->>'heightM')::numeric,
 (d->>'areaM2')::numeric,p_patch->>'areaProvenance',(p_patch->>'measurementReferenceHeightM')::numeric,
 (p_patch->>'maximumFillHeightM')::numeric,p_patch->>'geometryOrigin',p_capacity,'1',p_schema,now(),p_actor,(p_patch->>'hasConstantCrossSection')::boolean);
 end if;
 else new_version:=old.current_version_id;
 end if;
 if not p_create then
 update silomath.geometry_profiles set name=geometry_values.name,code=geometry_values.code,
 geometry_family_id=family,lifecycle_status=lifecycle,current_version_id=new_version,updated_at=now(),updated_by=p_actor
 where geometry_profile_id=p_id returning * into updated;
 else select * into updated from silomath.geometry_profiles where geometry_profile_id=p_id;
 end if;
 body:=silomath.geometry_profile_to_json(updated);
 insert into silomath.audit_events(audit_event_id,organization_id,actor_user_id,action,entity_type,entity_id,
 occurred_at,before_data,after_data,correlation_id,source)
 values(gen_random_uuid(),p_org,p_actor,operation,'GEOMETRY_PROFILE',p_id,now(),
 case when p_create then null else silomath.geometry_profile_to_json(old) end,body,p_correlation,'API');
 perform silomath.sync_emit_change(p_org,null,'GEOMETRY_PROFILE',p_id,updated.entity_version,'UPSERT',body);
 perform silomath.complete_idempotency_key(p_actor,p_org,operation,p_key,p_fingerprint,case when p_create then 201 else 200 end,body);
 return jsonb_build_object('status',case when p_create then 201 else 200 end,'body',body);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."geometry_profile_mutate_command"(p_actor uuid, p_org uuid, p_id uuid, p_patch jsonb, p_capacity numeric, p_schema text, p_expected bigint, p_key text, p_fingerprint text, p_correlation text, p_create boolean) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."geometry_profile_mutate_command"(p_actor uuid, p_org uuid, p_id uuid, p_patch jsonb, p_capacity numeric, p_schema text, p_expected bigint, p_key text, p_fingerprint text, p_correlation text, p_create boolean) TO postgres;

CREATE OR REPLACE FUNCTION silomath.inventory_dossier_get(p_actor uuid, p_org uuid, p_campaign uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare final jsonb; launch jsonb;
begin
 final:=silomath.final_inventory_snapshot_get(p_actor,p_org,p_campaign);
 launch:=silomath.campaign_snapshot_get(p_actor,p_org,p_campaign);
 return jsonb_build_object('organizationId',p_org,'campaignId',p_campaign,
 'finalSnapshot',final,'campaignSnapshot',launch,'generatedAt',now(),'generatedBy',p_actor);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."inventory_dossier_get"(p_actor uuid, p_org uuid, p_campaign uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."inventory_dossier_get"(p_actor uuid, p_org uuid, p_campaign uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.location_create_command(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_location_id uuid, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_location silomath.locations%rowtype;
    v_body jsonb;
begin
    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /locations',
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    v_location := silomath.location_create(
        p_actor_user_id,
        p_organization_id,
        p_location_id,
        p_name,
        p_code,
        p_address,
        p_city,
        p_country,
        p_notes,
        p_latitude_deg,
        p_longitude_deg,
        p_lifecycle_status
    );

    v_body := to_jsonb(v_location);

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /locations',
        p_idempotency_key,
        p_request_fingerprint,
        201,
        v_body
    );

    return jsonb_build_object(
        'status', 201,
        'body', v_body
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."location_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_location_id uuid, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."location_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_location_id uuid, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.location_patch_command(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_location_id uuid, p_expected_entity_version bigint, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_location silomath.locations%rowtype;
    v_body jsonb;
    v_operation_code text;
begin
    v_operation_code := 'PATCH /locations/' || p_location_id::text;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        v_operation_code,
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    v_location := silomath.location_patch(
        p_actor_user_id,
        p_organization_id,
        p_location_id,
        p_expected_entity_version,
        p_name,
        p_code,
        p_address,
        p_city,
        p_country,
        p_notes,
        p_latitude_deg,
        p_longitude_deg,
        p_lifecycle_status
    );

    v_body := to_jsonb(v_location);

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        v_operation_code,
        p_idempotency_key,
        p_request_fingerprint,
        200,
        v_body
    );

    return jsonb_build_object(
        'status', 200,
        'body', v_body
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."location_patch_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_location_id uuid, p_expected_entity_version bigint, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."location_patch_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_location_id uuid, p_expected_entity_version bigint, p_name text, p_code text, p_address text, p_city text, p_country text, p_notes text, p_latitude_deg numeric, p_longitude_deg numeric, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.offline_manifest_get(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_content jsonb;
    v_content_text text;
    v_hash text;
    v_existing silomath.offline_working_set_manifests%rowtype;
    v_manifest_id uuid;
    v_package_version text;
    v_generated_at timestamptz := now();
    v_content_manifest jsonb;
begin
    v_content := silomath.offline_package_content(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id,
        p_device_installation_id
    );

    /*
     * jsonb::text is the exact canonical byte representation returned by the
     * package RPC. SHA-256 is therefore computed over those exact UTF-8 bytes.
     */
    v_content_text := v_content::text;

    v_hash := encode(
        extensions.digest(
            convert_to(v_content_text, 'UTF8'),
            'sha256'
        ),
        'hex'
    );

    v_content_manifest := jsonb_build_object(
        'organizationId', p_organization_id,
        'campaignId', p_campaign_id,
        'deviceInstallationId', p_device_installation_id,
        'counts', jsonb_build_object(
            'scope', jsonb_array_length(v_content->'scope'),
            'team', jsonb_array_length(v_content->'team'),
            'commodities', jsonb_array_length(v_content->'commodities'),
            'stocktakingRecords', jsonb_array_length(v_content->'stocktakingRecords'),
            'stocktakingRevisions', jsonb_array_length(v_content->'stocktakingRevisions'),
            'measurements', jsonb_array_length(v_content->'measurements'),
            'calculationResults', jsonb_array_length(v_content->'calculationResults'),
            'clients', jsonb_array_length(v_content->'clients'),
            'issues', jsonb_array_length(v_content->'issues'),
            'issueStatusEvents', jsonb_array_length(v_content->'issueStatusEvents'),
            'issueResolutions', jsonb_array_length(v_content->'issueResolutions'),
            'evidence', jsonb_array_length(v_content->'evidence')
        ),
        'compatibility', jsonb_build_object(
            'campaignSnapshotSchemaVersion',
                v_content #>> '{campaignSnapshot,snapshot_schema_version}',
            'calculationEngineVersion',
                v_content #>> '{campaignSnapshot,calculation_engine_version}'
        ),
        'integrity', jsonb_build_object(
            'algorithm', 'SHA-256',
            'contentHash', v_hash
        )
    );

    /*
     * Serialize manifest generation for this Campaign├ùDeviceInstallation.
     */
    perform 1
    from silomath.device_installations d
    where d.device_installation_id = p_device_installation_id
    for update;

    select *
    into v_existing
    from silomath.offline_working_set_manifests m
    where m.campaign_id = p_campaign_id
      and m.organization_id = p_organization_id
      and m.device_installation_id = p_device_installation_id
      and m.is_current
    for update;

    if found and v_existing.integrity_hash = v_hash then
        return jsonb_build_object(
            'manifestId', v_existing.manifest_id,
            'organizationId', v_existing.organization_id,
            'campaignId', v_existing.campaign_id,
            'deviceInstallationId', v_existing.device_installation_id,
            'packageVersion', v_existing.package_version,
            'contentManifest', v_existing.content_manifest,
            'integrityHash', v_existing.integrity_hash,
            'integrityAlgorithm', v_existing.integrity_algorithm,
            'generatedAt', v_existing.generated_at
        );
    end if;

    update silomath.offline_working_set_manifests
    set is_current = false
    where campaign_id = p_campaign_id
      and organization_id = p_organization_id
      and device_installation_id = p_device_installation_id
      and is_current;

    v_manifest_id := gen_random_uuid();
    v_package_version := gen_random_uuid()::text;

    insert into silomath.offline_working_set_manifests (
        manifest_id,
        campaign_id,
        organization_id,
        device_installation_id,
        package_version,
        content_manifest,
        integrity_hash,
        integrity_algorithm,
        verification_metadata,
        generated_at,
        downloaded_at,
        verified_at,
        is_current
    )
    values (
        v_manifest_id,
        p_campaign_id,
        p_organization_id,
        p_device_installation_id,
        v_package_version,
        v_content_manifest,
        v_hash,
        'SHA-256',
        null,
        v_generated_at,
        null,
        null,
        true
    );

    return jsonb_build_object(
        'manifestId', v_manifest_id,
        'organizationId', p_organization_id,
        'campaignId', p_campaign_id,
        'deviceInstallationId', p_device_installation_id,
        'packageVersion', v_package_version,
        'contentManifest', v_content_manifest,
        'integrityHash', v_hash,
        'integrityAlgorithm', 'SHA-256',
        'generatedAt', v_generated_at
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."offline_manifest_get"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."offline_manifest_get"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.offline_package_get(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid, p_package_version text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_manifest silomath.offline_working_set_manifests%rowtype;
    v_content jsonb;
    v_content_text text;
    v_hash text;
begin
    if not silomath.offline_package_actor_authorized(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id,
        p_device_installation_id
    ) then
        raise exception 'offline package forbidden'
            using errcode = '42501';
    end if;

    if nullif(btrim(p_package_version), '') is null then
        raise exception 'package version is required'
            using errcode = '22023';
    end if;

    select *
    into v_manifest
    from silomath.offline_working_set_manifests m
    where m.organization_id = p_organization_id
      and m.campaign_id = p_campaign_id
      and m.device_installation_id = p_device_installation_id
      and m.package_version = p_package_version
      and m.is_current;

    if not found then
        raise exception 'offline package version not found or superseded'
            using errcode = 'P0002';
    end if;

    v_content := silomath.offline_package_content(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id,
        p_device_installation_id
    );

    v_content_text := v_content::text;

    v_hash := encode(
        extensions.digest(
            convert_to(v_content_text, 'UTF8'),
            'sha256'
        ),
        'hex'
    );

    if v_hash <> v_manifest.integrity_hash then
        raise exception 'offline package version superseded'
            using errcode = '40001';
    end if;

    return jsonb_build_object(
        'manifestId', v_manifest.manifest_id,
        'organizationId', v_manifest.organization_id,
        'campaignId', v_manifest.campaign_id,
        'deviceInstallationId', v_manifest.device_installation_id,
        'packageVersion', v_manifest.package_version,
        'integrityHash', v_manifest.integrity_hash,
        'integrityAlgorithm', v_manifest.integrity_algorithm,
        'generatedAt', v_manifest.generated_at,
        'content', v_content_text
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."offline_package_get"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid, p_package_version text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."offline_package_get"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_device_installation_id uuid, p_package_version text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.report_dataset_get(p_actor uuid, p_org uuid, p_type text, p_request jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare c silomath.inventory_campaigns%rowtype; rows jsonb; output jsonb; totals jsonb;
 campaign uuid; source_id uuid;
begin
 if p_type not in ('PHYSICAL_INVENTORY','INVENTORY_BY_LOCATION','INVENTORY_BY_COMMODITY',
 'INVENTORY_BY_OWNERSHIP','STORAGE_SPACE_WORKSHEET','CAPACITY_REPORT') then
 raise exception 'specialized report contract required' using errcode='22023'; end if;
 if p_request is null or jsonb_typeof(p_request)<>'object' then
 raise exception 'report request required' using errcode='22023'; end if;
 campaign:=(p_request->>'campaignId')::uuid;
 rows:=silomath.report_inventory_rows(p_actor,p_org,campaign);
 select * into c from silomath.inventory_campaigns where campaign_id=campaign and organization_id=p_org;
 select final_snapshot_id into source_id from silomath.final_inventory_snapshots where campaign_id=campaign;
 if exists(select 1 from jsonb_object_keys(coalesce(p_request->'filters','{}')) k where
 k not in ('locationId','storageUnitId','storageSpaceId','commodityId','ownership','clientId','isEmpty')) then
 raise exception 'unsupported report filter' using errcode='22023'; end if;
 select coalesce(jsonb_agg(x order by x->>'storageSpaceId'),'[]') into rows from jsonb_array_elements(rows) x
 where (p_request#>>'{filters,locationId}' is null or x#>>'{hierarchy,locationId}'=p_request#>>'{filters,locationId}')
 and (p_request#>>'{filters,storageUnitId}' is null or x#>>'{hierarchy,storageUnitId}'=p_request#>>'{filters,storageUnitId}')
 and (p_request#>>'{filters,storageSpaceId}' is null or x->>'storageSpaceId'=p_request#>>'{filters,storageSpaceId}')
 and (p_request#>>'{filters,commodityId}' is null or x#>>'{commodity,commodity_id}'=p_request#>>'{filters,commodityId}')
 and (p_request#>>'{filters,ownership}' is null or x#>>'{ownershipClient,ownership}'=p_request#>>'{filters,ownership}')
 and (p_request#>>'{filters,clientId}' is null or x#>>'{ownershipClient,client,client_id}'=p_request#>>'{filters,clientId}')
 and (p_request#>>'{filters,isEmpty}' is null or x->>'isEmpty'=p_request#>>'{filters,isEmpty}');
 select jsonb_build_object('productVolumeM3',sum((x#>>'{operationalValues,productVolumeM3}')::numeric)::text,
 'physicalQuantityKg',sum((x#>>'{operationalValues,physicalQuantityKg}')::numeric)::text,
 'storageSpaceCount',count(*),
 'utilizationPercent',case when sum((x#>>'{geometry,maxCapacityM3}')::numeric) filter
 (where (x#>>'{geometry,maxCapacityM3}')::numeric>0 and x#>>'{operationalValues,productVolumeM3}' is not null)>0
 then (sum((x#>>'{operationalValues,productVolumeM3}')::numeric) filter
 (where (x#>>'{geometry,maxCapacityM3}')::numeric>0)/
 sum((x#>>'{geometry,maxCapacityM3}')::numeric) filter
 (where (x#>>'{geometry,maxCapacityM3}')::numeric>0 and x#>>'{operationalValues,productVolumeM3}' is not null)*100)::text else null end)
 into totals from jsonb_array_elements(rows) x;
 if p_type in ('INVENTORY_BY_LOCATION','INVENTORY_BY_COMMODITY','INVENTORY_BY_OWNERSHIP') then
 select coalesce(jsonb_agg(g order by g->>'groupKey'),'[]') into output from (
 select jsonb_build_object('groupKey',group_key,'storageSpaceCount',count(*),
 'location',case when p_type='INVENTORY_BY_LOCATION' then jsonb_agg(x->'hierarchy')->0 else null end,
 'commodity',case when p_type in ('INVENTORY_BY_LOCATION','INVENTORY_BY_COMMODITY') then jsonb_agg(x->'commodity')->0 else null end,
 'ownershipClient',case when p_type='INVENTORY_BY_OWNERSHIP' then jsonb_agg(x->'ownershipClient')->0 else null end,
 'locationCount',count(distinct x#>>'{hierarchy,locationId}'),
 'productVolumeM3',sum((x#>>'{operationalValues,productVolumeM3}')::numeric)::text,
 'physicalQuantityKg',sum((x#>>'{operationalValues,physicalQuantityKg}')::numeric)::text) g
 from (select x,case p_type
 when 'INVENTORY_BY_LOCATION' then coalesce(x#>>'{hierarchy,locationId}','')||':'||coalesce(x#>>'{commodity,commodity_id}','EMPTY')
 when 'INVENTORY_BY_COMMODITY' then coalesce(x#>>'{commodity,commodity_id}','EMPTY')
 else coalesce(x#>>'{ownershipClient,ownership}','EMPTY')||':'||coalesce(x#>>'{ownershipClient,client,client_id}','') end group_key
 from jsonb_array_elements(rows) x) data group by group_key) groups;
 elsif p_type='CAPACITY_REPORT' then
 select coalesce(jsonb_agg(x||jsonb_build_object('utilizationPercent',
 case when (x#>>'{geometry,maxCapacityM3}')::numeric>0 then
 ((x#>>'{operationalValues,productVolumeM3}')::numeric/(x#>>'{geometry,maxCapacityM3}')::numeric*100)::text else null end)
 order by x->>'storageSpaceId'),'[]') into output from jsonb_array_elements(rows) x;
 else output:=rows; end if;
 return jsonb_build_object('organizationId',p_org,'reportType',p_type,'campaignId',campaign,
 'finalSnapshotId',source_id,'sourceState',case when c.status='COMPLETED' then 'FINAL' else 'NON_FINAL' end,
 'generatedAt',now(),'generatedBy',p_actor,'filters',coalesce(p_request->'filters','{}'),
 'rows',output,'totals',totals);
end $function$;
REVOKE ALL ON FUNCTION "silomath"."report_dataset_get"(p_actor uuid, p_org uuid, p_type text, p_request jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."report_dataset_get"(p_actor uuid, p_org uuid, p_type text, p_request jsonb) TO postgres;

CREATE OR REPLACE FUNCTION silomath.stocktaking_reset_validation_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_new_stocktaking_revision_id uuid, p_idempotency_key text, p_request_fingerprint text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_record silomath.stocktaking_records%rowtype;
    v_prior silomath.stocktaking_revisions%rowtype;
    v_new silomath.stocktaking_revisions%rowtype;
    v_claim record;
    v_response jsonb;
    v_next_revision_number integer;
    v_now timestamptz := now();
begin
    if not silomath.stocktaking_actor_is_admin_team_member(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id
    ) then
        raise exception 'STOCKTAKING_NOT_TEAM_MEMBER'
            using errcode = '42501';
    end if;

    if p_new_stocktaking_revision_id is null then
        raise exception 'new stocktaking revision id required'
            using errcode = '22023';
    end if;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'STOCKTAKING_RESET_VALIDATION',
        btrim(p_idempotency_key),
        btrim(p_request_fingerprint)
    );

    if v_claim.disposition = 'REPLAY' then
        return v_claim.response_body;
    end if;

    select *
    into v_record
    from silomath.stocktaking_records
    where stocktaking_record_id = p_stocktaking_record_id
      and organization_id = p_organization_id
      and campaign_id = p_campaign_id
    for update;

    if not found then
        raise exception 'stocktaking record not found'
            using errcode = 'P0002';
    end if;

    select *
    into v_prior
    from silomath.stocktaking_revisions
    where stocktaking_revision_id = v_record.current_revision_id
      and stocktaking_record_id = v_record.stocktaking_record_id
    for update;

    if not found then
        raise exception 'current revision not found'
            using errcode = 'P0002';
    end if;

    if v_prior.revision_state <> 'VALIDATED' then
        raise exception 'STOCKTAKING_REVISION_STALE'
            using errcode = '40001';
    end if;

    perform silomath.assert_expected_entity_version(
        p_expected_revision_entity_version,
        v_prior.entity_version
    );

    select coalesce(max(r.revision_number), 0) + 1
    into v_next_revision_number
    from silomath.stocktaking_revisions r
    where r.stocktaking_record_id = v_record.stocktaking_record_id;

    insert into silomath.stocktaking_revisions (
        stocktaking_revision_id,
        stocktaking_record_id,
        revision_number,
        revision_state,
        based_on_revision_id,
        is_empty,
        commodity_version_id,
        actual_bulk_density_kg_m3,
        ownership,
        client_id,
        notes,
        calculation_result_id,
        validated_at,
        validated_by,
        created_at,
        created_by,
        entity_version
    )
    values (
        p_new_stocktaking_revision_id,
        v_record.stocktaking_record_id,
        v_next_revision_number,
        'WORKING',
        v_prior.stocktaking_revision_id,
        v_prior.is_empty,
        v_prior.commodity_version_id,
        v_prior.actual_bulk_density_kg_m3,
        v_prior.ownership,
        v_prior.client_id,
        v_prior.notes,
        null,
        null,
        null,
        v_now,
        p_actor_user_id,
        1
    )
    returning * into v_new;

    if not v_prior.is_empty then
        insert into silomath.measurements (
            measurement_id,
            stocktaking_revision_id,
            sequence_number,
            raw_value,
            raw_unit,
            normalized_ullage_m,
            datum_metadata,
            geometry_profile_version_id,
            captured_at,
            captured_by,
            entity_version
        )
        select
            gen_random_uuid(),
            v_new.stocktaking_revision_id,
            m.sequence_number,
            m.raw_value,
            m.raw_unit,
            m.normalized_ullage_m,
            m.datum_metadata,
            m.geometry_profile_version_id,
            m.captured_at,
            m.captured_by,
            1
        from silomath.measurements m
        where m.stocktaking_revision_id = v_prior.stocktaking_revision_id
        order by m.sequence_number;
    end if;

    update silomath.stocktaking_records
    set
        current_revision_id = v_new.stocktaking_revision_id,
        updated_at = v_now,
        updated_by = p_actor_user_id
    where stocktaking_record_id = v_record.stocktaking_record_id
    returning * into v_record;

    insert into silomath.audit_events (
        audit_event_id,
        organization_id,
        campaign_id,
        actor_user_id,
        action,
        entity_type,
        entity_id,
        occurred_at,
        before_data,
        after_data,
        change_metadata,
        correlation_id,
        source,
        system_actor
    )
    values (
        gen_random_uuid(),
        p_organization_id,
        p_campaign_id,
        p_actor_user_id,
        'RESET_VALIDATION',
        'StocktakingRecord',
        v_record.stocktaking_record_id,
        v_now,
        jsonb_build_object(
            'validatedRevisionId', v_prior.stocktaking_revision_id,
            'revisionNumber', v_prior.revision_number,
            'entityVersion', v_prior.entity_version
        ),
        jsonb_build_object(
            'workingRevisionId', v_new.stocktaking_revision_id,
            'revisionNumber', v_new.revision_number,
            'basedOnRevisionId', v_prior.stocktaking_revision_id,
            'recordEntityVersion', v_record.entity_version
        ),
        null,
        gen_random_uuid()::text,
        'SERVER',
        null
    );

    perform silomath.sync_emit_change(
        p_organization_id,
        p_campaign_id,
        'StocktakingRecord',
        v_record.stocktaking_record_id,
        v_record.entity_version,
        'UPSERT',
        jsonb_build_object(
            'stocktakingRecordId', v_record.stocktaking_record_id,
            'currentRevisionId', v_new.stocktaking_revision_id,
            'revisionState', 'WORKING',
            'basedOnRevisionId', v_prior.stocktaking_revision_id
        )
    );

    v_response := jsonb_build_object(
        'stocktakingRevisionId', v_new.stocktaking_revision_id,
        'stocktakingRecordId', v_new.stocktaking_record_id,
        'revisionNumber', v_new.revision_number,
        'revisionState', v_new.revision_state,
        'basedOnRevisionId', v_new.based_on_revision_id,
        'isEmpty', v_new.is_empty,
        'entityVersion', v_new.entity_version
    );

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'STOCKTAKING_RESET_VALIDATION',
        btrim(p_idempotency_key),
        btrim(p_request_fingerprint),
        200,
        v_response
    );

    return v_response;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."stocktaking_reset_validation_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_new_stocktaking_revision_id uuid, p_idempotency_key text, p_request_fingerprint text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."stocktaking_reset_validation_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_new_stocktaking_revision_id uuid, p_idempotency_key text, p_request_fingerprint text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.stocktaking_working_upsert_idempotent_command(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_commodity_version_id uuid, p_actual_bulk_density_kg_m3 numeric, p_ownership text, p_client_id uuid, p_notes text, p_measurements jsonb, p_calculation_result jsonb, p_idempotency_key text, p_request_fingerprint text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_response jsonb;
begin
    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'STOCKTAKING_WORKING_UPSERT',
        btrim(p_idempotency_key),
        btrim(p_request_fingerprint)
    );

    if v_claim.disposition = 'REPLAY' then
        return v_claim.response_body;
    end if;

    v_response := silomath.stocktaking_working_upsert_command(
        p_actor_user_id,
        p_organization_id,
        p_campaign_id,
        p_stocktaking_record_id,
        p_expected_revision_entity_version,
        p_commodity_version_id,
        p_actual_bulk_density_kg_m3,
        p_ownership,
        p_client_id,
        p_notes,
        p_measurements,
        p_calculation_result
    );

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'STOCKTAKING_WORKING_UPSERT',
        btrim(p_idempotency_key),
        btrim(p_request_fingerprint),
        200,
        v_response
    );

    return v_response;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."stocktaking_working_upsert_idempotent_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_commodity_version_id uuid, p_actual_bulk_density_kg_m3 numeric, p_ownership text, p_client_id uuid, p_notes text, p_measurements jsonb, p_calculation_result jsonb, p_idempotency_key text, p_request_fingerprint text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."stocktaking_working_upsert_idempotent_command"(p_actor_user_id uuid, p_organization_id uuid, p_campaign_id uuid, p_stocktaking_record_id uuid, p_expected_revision_entity_version bigint, p_commodity_version_id uuid, p_actual_bulk_density_kg_m3 numeric, p_ownership text, p_client_id uuid, p_notes text, p_measurements jsonb, p_calculation_result jsonb, p_idempotency_key text, p_request_fingerprint text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_batch_create_command(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_idempotency_key text, p_request_fingerprint text, p_code_prefix text, p_start_number integer, p_end_number integer, p_number_width integer, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_code text;
    v_created jsonb := '[]'::jsonb;
    v_item silomath.storage_spaces%rowtype;
    v_body jsonb;
    v_i integer;
begin
    if not silomath.storage_space_actor_is_active_admin(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage space batch mutation forbidden'
            using errcode = '42501';
    end if;

    if p_storage_unit_id is null then
        raise exception 'storage unit id is required'
            using errcode = '22023';
    end if;

    if p_geometry_profile_id is null then
        raise exception 'geometry profile id is required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_code_prefix), '') is null then
        raise exception 'storage space code prefix is required'
            using errcode = '22023';
    end if;

    if p_start_number is null
       or p_end_number is null
       or p_start_number < 0
       or p_end_number < p_start_number then
        raise exception 'invalid storage space batch range'
            using errcode = '22023';
    end if;

    if p_number_width is null
       or p_number_width < 1
       or p_number_width > 9 then
        raise exception 'invalid storage space batch number width'
            using errcode = '22023';
    end if;

    if p_lifecycle_status not in ('ACTIVE', 'INACTIVE') then
        raise exception 'invalid storage space lifecycle status'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.storage_units su
        where su.storage_unit_id = p_storage_unit_id
          and su.organization_id = p_organization_id
    ) then
        raise exception 'storage unit not found'
            using errcode = 'P0002';
    end if;

    if not exists (
        select 1
        from silomath.geometry_profiles gp
        where gp.geometry_profile_id = p_geometry_profile_id
          and gp.organization_id = p_organization_id
    ) then
        raise exception 'geometry profile not found'
            using errcode = 'P0002';
    end if;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /storage-spaces:batch-create',
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    for v_i in p_start_number..p_end_number loop
        v_code :=
            upper(btrim(p_code_prefix))
            || lpad(v_i::text, p_number_width, '0');

        if exists (
            select 1
            from silomath.storage_spaces ss
            where ss.storage_unit_id = p_storage_unit_id
              and ss.code = v_code
        ) then
            raise exception 'storage space batch code already exists: %', v_code
                using errcode = '23505';
        end if;
    end loop;

    for v_i in p_start_number..p_end_number loop
        v_code :=
            upper(btrim(p_code_prefix))
            || lpad(v_i::text, p_number_width, '0');

        v_item := silomath.storage_space_create(
            p_actor_user_id,
            p_organization_id,
            gen_random_uuid(),
            p_storage_unit_id,
            v_code,
            p_description,
            p_geometry_profile_id,
            p_lifecycle_status
        );

        v_created := v_created || jsonb_build_array(to_jsonb(v_item));
    end loop;

    v_body := jsonb_build_object(
        'items', v_created,
        'createdCount', jsonb_array_length(v_created)
    );

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /storage-spaces:batch-create',
        p_idempotency_key,
        p_request_fingerprint,
        201,
        v_body
    );

    return jsonb_build_object(
        'status', 201,
        'body', v_body
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_batch_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_idempotency_key text, p_request_fingerprint text, p_code_prefix text, p_start_number integer, p_end_number integer, p_number_width integer, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_batch_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_idempotency_key text, p_request_fingerprint text, p_code_prefix text, p_start_number integer, p_end_number integer, p_number_width integer, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_batch_create_items_command(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_idempotency_key text, p_request_fingerprint text, p_items jsonb, p_correlation_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_item jsonb;
    v_created jsonb := '[]'::jsonb;
    v_row silomath.storage_spaces%rowtype;
    v_code text;
    v_description text;
    v_geometry_profile_id uuid;
    v_lifecycle_status text;
    v_body jsonb;
begin
    if not silomath.storage_space_actor_is_active_admin(
        p_actor_user_id,
        p_organization_id
    ) then
        raise exception 'storage space batch mutation forbidden'
            using errcode = '42501';
    end if;

    if p_items is null
       or jsonb_typeof(p_items) <> 'array'
       or jsonb_array_length(p_items) < 1 then
        raise exception 'storage space batch items are required'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.storage_units su
        join silomath.locations l
          on l.location_id = su.location_id
         and l.organization_id = su.organization_id
        where su.storage_unit_id = p_storage_unit_id
          and su.organization_id = p_organization_id
    ) then
        raise exception 'storage unit not found'
            using errcode = 'P0002';
    end if;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /storage-spaces:batch-create',
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    if exists (
        select 1
        from (
            select upper(btrim(value->>'code')) as code, count(*) as code_count
            from jsonb_array_elements(p_items)
            group by upper(btrim(value->>'code'))
        ) duplicate_codes
        where duplicate_codes.code is null
           or duplicate_codes.code = ''
           or duplicate_codes.code_count > 1
    ) then
        raise exception 'storage space batch contains invalid or duplicate codes'
            using errcode = '22023';
    end if;

    for v_item in select value from jsonb_array_elements(p_items)
    loop
        if (v_item - array['code','description','geometryProfileId','lifecycleStatus']::text[]) <> '{}'::jsonb then
            raise exception 'storage space batch item contains unsupported fields'
                using errcode = '22023';
        end if;

        begin
            v_code := upper(btrim(v_item->>'code'));
            v_description := nullif(btrim(v_item->>'description'), '');
            v_geometry_profile_id := nullif(v_item->>'geometryProfileId', '')::uuid;
            v_lifecycle_status := coalesce(nullif(upper(btrim(v_item->>'lifecycleStatus')), ''), 'ACTIVE');
        exception when others then
            raise exception 'invalid storage space batch item'
                using errcode = '22023';
        end;

        if v_code is null or v_code = '' or v_geometry_profile_id is null
           or v_lifecycle_status not in ('ACTIVE', 'INACTIVE') then
            raise exception 'invalid storage space batch item'
                using errcode = '22023';
        end if;

        if not exists (
            select 1
            from silomath.geometry_profiles gp
            where gp.geometry_profile_id = v_geometry_profile_id
              and gp.organization_id = p_organization_id
        ) then
            raise exception 'geometry profile not found'
                using errcode = 'P0002';
        end if;

        if exists (
            select 1
            from silomath.storage_spaces ss
            where ss.storage_unit_id = p_storage_unit_id
              and ss.code = v_code
        ) then
            raise exception 'storage space batch code already exists: %', v_code
                using errcode = '23505';
        end if;
    end loop;

    for v_item in select value from jsonb_array_elements(p_items)
    loop
        v_code := upper(btrim(v_item->>'code'));
        v_description := nullif(btrim(v_item->>'description'), '');
        v_geometry_profile_id := (v_item->>'geometryProfileId')::uuid;
        v_lifecycle_status := coalesce(nullif(upper(btrim(v_item->>'lifecycleStatus')), ''), 'ACTIVE');

        v_row := silomath.storage_space_create(
            p_actor_user_id,
            p_organization_id,
            gen_random_uuid(),
            p_storage_unit_id,
            v_code,
            v_description,
            v_geometry_profile_id,
            v_lifecycle_status
        );

        insert into silomath.audit_events (
            audit_event_id, organization_id, campaign_id, actor_user_id,
            action, entity_type, entity_id, occurred_at,
            before_data, after_data, change_metadata,
            correlation_id, source, system_actor
        )
        values (
            gen_random_uuid(), p_organization_id, null, p_actor_user_id,
            'BATCH_CREATE', 'StorageSpace', v_row.storage_space_id, now(),
            null, to_jsonb(v_row),
            jsonb_build_object('batch', true, 'storageUnitId', p_storage_unit_id),
            coalesce(nullif(btrim(p_correlation_id), ''), gen_random_uuid()::text),
            'API_V1', null
        );

        perform silomath.sync_emit_change(
            p_organization_id,
            null,
            'StorageSpace',
            v_row.storage_space_id,
            v_row.entity_version,
            'UPSERT',
            to_jsonb(v_row)
        );

        v_created := v_created || jsonb_build_array(to_jsonb(v_row));
    end loop;

    v_body := jsonb_build_object('items', v_created);

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /storage-spaces:batch-create',
        p_idempotency_key,
        p_request_fingerprint,
        201,
        v_body
    );

    return jsonb_build_object('status', 201, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_batch_create_items_command"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_idempotency_key text, p_request_fingerprint text, p_items jsonb, p_correlation_id text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_batch_create_items_command"(p_actor_user_id uuid, p_organization_id uuid, p_storage_unit_id uuid, p_idempotency_key text, p_request_fingerprint text, p_items jsonb, p_correlation_id text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_create_command(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_space_id uuid, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_storage_space silomath.storage_spaces%rowtype;
    v_body jsonb;
begin
    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /storage-spaces',
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    v_storage_space := silomath.storage_space_create(
        p_actor_user_id,
        p_organization_id,
        p_storage_space_id,
        p_storage_unit_id,
        p_code,
        p_description,
        p_geometry_profile_id,
        p_lifecycle_status
    );

    v_body := to_jsonb(v_storage_space);

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /storage-spaces',
        p_idempotency_key,
        p_request_fingerprint,
        201,
        v_body
    );

    return jsonb_build_object('status', 201, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_space_id uuid, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_space_id uuid, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_space_patch_command(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_space_id uuid, p_expected_entity_version bigint, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_storage_space silomath.storage_spaces%rowtype;
    v_body jsonb;
    v_operation_code text;
begin
    v_operation_code := 'PATCH /storage-spaces/' || p_storage_space_id::text;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        v_operation_code,
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    v_storage_space := silomath.storage_space_patch(
        p_actor_user_id,
        p_organization_id,
        p_storage_space_id,
        p_expected_entity_version,
        p_storage_unit_id,
        p_code,
        p_description,
        p_geometry_profile_id,
        p_lifecycle_status
    );

    v_body := to_jsonb(v_storage_space);

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        v_operation_code,
        p_idempotency_key,
        p_request_fingerprint,
        200,
        v_body
    );

    return jsonb_build_object('status', 200, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_space_patch_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_space_id uuid, p_expected_entity_version bigint, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_space_patch_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_space_id uuid, p_expected_entity_version bigint, p_storage_unit_id uuid, p_code text, p_description text, p_geometry_profile_id uuid, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_unit_create_command(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_unit_id uuid, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_storage_unit silomath.storage_units%rowtype;
    v_body jsonb;
begin
    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /storage-units',
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    v_storage_unit := silomath.storage_unit_create(
        p_actor_user_id,
        p_organization_id,
        p_storage_unit_id,
        p_location_id,
        p_name,
        p_code,
        p_description,
        p_lifecycle_status
    );

    v_body := to_jsonb(v_storage_unit);

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        'POST /storage-units',
        p_idempotency_key,
        p_request_fingerprint,
        201,
        v_body
    );

    return jsonb_build_object('status', 201, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_unit_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_unit_id uuid, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_unit_create_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_unit_id uuid, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.storage_unit_patch_command(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_unit_id uuid, p_expected_entity_version bigint, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_claim record;
    v_storage_unit silomath.storage_units%rowtype;
    v_body jsonb;
    v_operation_code text;
begin
    v_operation_code := 'PATCH /storage-units/' || p_storage_unit_id::text;

    select *
    into v_claim
    from silomath.claim_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        v_operation_code,
        p_idempotency_key,
        p_request_fingerprint
    );

    if v_claim.disposition = 'REPLAY' then
        return jsonb_build_object(
            'status', v_claim.response_status,
            'body', v_claim.response_body
        );
    end if;

    v_storage_unit := silomath.storage_unit_patch(
        p_actor_user_id,
        p_organization_id,
        p_storage_unit_id,
        p_expected_entity_version,
        p_location_id,
        p_name,
        p_code,
        p_description,
        p_lifecycle_status
    );

    v_body := to_jsonb(v_storage_unit);

    perform silomath.complete_idempotency_key(
        p_actor_user_id,
        p_organization_id,
        v_operation_code,
        p_idempotency_key,
        p_request_fingerprint,
        200,
        v_body
    );

    return jsonb_build_object('status', 200, 'body', v_body);
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."storage_unit_patch_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_unit_id uuid, p_expected_entity_version bigint, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."storage_unit_patch_command"(p_actor_user_id uuid, p_organization_id uuid, p_idempotency_key text, p_request_fingerprint text, p_storage_unit_id uuid, p_expected_entity_version bigint, p_location_id uuid, p_name text, p_code text, p_description text, p_lifecycle_status text) TO postgres;

CREATE OR REPLACE FUNCTION silomath.sync_apply_operation(p_actor_user_id uuid, p_sync_operation_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_op silomath.sync_operations%rowtype;
    v_result jsonb;
    v_body jsonb;
    v_request_fingerprint text;
    v_entity_version bigint;
    v_server_value jsonb;
    v_server_version bigint;
    v_parent_id uuid;
    v_sqlstate text;
    v_error_code text;
begin
    select *
    into v_op
    from silomath.sync_operations
    where sync_operation_id = p_sync_operation_id
    for update;

    if not found then
        raise exception 'sync operation not found'
            using errcode = 'P0002';
    end if;

    if v_op.state in ('ACKNOWLEDGED', 'CONFLICT', 'PERMANENT_ERROR') then
        return silomath.sync_operation_result(v_op);
    end if;

    if p_actor_user_id is null
       or not silomath.sync_actor_is_active_member(
           p_actor_user_id,
           v_op.organization_id
       ) then
        update silomath.sync_operations
        set state = 'PERMANENT_ERROR',
            attempt_count = attempt_count + 1,
            last_attempt_at = now(),
            next_attempt_at = null,
            last_error_code = 'SYNC_FORBIDDEN',
            updated_at = now()
        where sync_operation_id = p_sync_operation_id
        returning * into v_op;

        return silomath.sync_operation_result(v_op);
    end if;

    /*
     * The sync operation identity and payload are immutable in the dispatcher.
     * The fingerprint is deterministic and intentionally independent of retry time.
     */
    v_request_fingerprint := encode(
        extensions.digest(
            jsonb_build_object(
                'syncOperationId', v_op.sync_operation_id,
                'organizationId', v_op.organization_id,
                'campaignId', v_op.campaign_id,
                'entityType', v_op.entity_type,
                'entityId', v_op.entity_id,
                'operationType', v_op.operation_type,
                'payload', v_op.payload,
                'baseEntityVersion', v_op.base_entity_version,
                'eventAtUtc', v_op.event_at
            )::text,
            'sha256'
        ),
        'hex'
    );

    update silomath.sync_operations
    set state = 'IN_FLIGHT',
        attempt_count = attempt_count + 1,
        last_attempt_at = now(),
        next_attempt_at = null,
        last_error_code = null,
        updated_at = now()
    where sync_operation_id = p_sync_operation_id
    returning * into v_op;

    begin
        if v_op.entity_type = 'LOCATION'
           and v_op.operation_type = 'CREATE' then

            select silomath.location_create_command(
                p_actor_user_id,
                v_op.organization_id,
                v_op.idempotency_key,
                v_request_fingerprint,
                v_op.entity_id,
                v_op.payload->>'name',
                v_op.payload->>'code',
                v_op.payload->>'address',
                v_op.payload->>'city',
                v_op.payload->>'country',
                v_op.payload->>'notes',
                nullif(v_op.payload->>'latitude', '')::numeric,
                nullif(v_op.payload->>'longitude', '')::numeric,
                coalesce(nullif(v_op.payload->>'lifecycleStatus', ''), 'ACTIVE')
            )
            into v_result;

        elsif v_op.entity_type = 'LOCATION'
              and v_op.operation_type = 'PATCH' then

            if (v_op.payload ? 'latitude') <> (v_op.payload ? 'longitude') then
                raise exception 'latitude and longitude must be supplied together'
                    using errcode = '22023';
            end if;

            select to_jsonb(l), l.entity_version
            into v_server_value, v_server_version
            from silomath.locations l
            where l.location_id = v_op.entity_id
              and l.organization_id = v_op.organization_id
            for update;

            if not found then
                raise exception 'location not found'
                    using errcode = 'P0002';
            end if;

            select silomath.location_patch_command(
                p_actor_user_id,
                v_op.organization_id,
                v_op.idempotency_key,
                v_request_fingerprint,
                v_op.entity_id,
                v_op.base_entity_version,
                case when v_op.payload ? 'name'
                    then v_op.payload->>'name'
                    else v_server_value->>'name'
                end,
                case when v_op.payload ? 'code'
                    then v_op.payload->>'code'
                    else v_server_value->>'code'
                end,
                case when v_op.payload ? 'address'
                    then v_op.payload->>'address'
                    else v_server_value->>'address'
                end,
                case when v_op.payload ? 'city'
                    then v_op.payload->>'city'
                    else v_server_value->>'city'
                end,
                case when v_op.payload ? 'country'
                    then v_op.payload->>'country'
                    else v_server_value->>'country'
                end,
                case when v_op.payload ? 'notes'
                    then v_op.payload->>'notes'
                    else v_server_value->>'notes'
                end,
                case when v_op.payload ? 'latitude'
                    then nullif(v_op.payload->>'latitude', '')::numeric
                    else nullif(v_server_value->>'latitude_deg', '')::numeric
                end,
                case when v_op.payload ? 'longitude'
                    then nullif(v_op.payload->>'longitude', '')::numeric
                    else nullif(v_server_value->>'longitude_deg', '')::numeric
                end,
                case when v_op.payload ? 'lifecycleStatus'
                    then v_op.payload->>'lifecycleStatus'
                    else v_server_value->>'lifecycle_status'
                end
            )
            into v_result;

        elsif v_op.entity_type = 'STORAGE_UNIT'
              and v_op.operation_type = 'CREATE' then

            v_parent_id := nullif(v_op.payload->>'locationId', '')::uuid;

            select silomath.storage_unit_create_command(
                p_actor_user_id,
                v_op.organization_id,
                v_op.idempotency_key,
                v_request_fingerprint,
                v_op.entity_id,
                v_parent_id,
                v_op.payload->>'name',
                v_op.payload->>'code',
                v_op.payload->>'description',
                coalesce(nullif(v_op.payload->>'lifecycleStatus', ''), 'ACTIVE')
            )
            into v_result;

        elsif v_op.entity_type = 'STORAGE_UNIT'
              and v_op.operation_type = 'PATCH' then

            if v_op.payload ? 'locationId' then
                raise exception 'storage unit parent location is immutable'
                    using errcode = '22023';
            end if;

            select to_jsonb(su), su.entity_version, su.location_id
            into v_server_value, v_server_version, v_parent_id
            from silomath.storage_units su
            where su.storage_unit_id = v_op.entity_id
              and su.organization_id = v_op.organization_id
            for update;

            if not found then
                raise exception 'storage unit not found'
                    using errcode = 'P0002';
            end if;

            select silomath.storage_unit_patch_command(
                p_actor_user_id,
                v_op.organization_id,
                v_op.idempotency_key,
                v_request_fingerprint,
                v_op.entity_id,
                v_op.base_entity_version,
                v_parent_id,
                case when v_op.payload ? 'name'
                    then v_op.payload->>'name'
                    else v_server_value->>'name'
                end,
                case when v_op.payload ? 'code'
                    then v_op.payload->>'code'
                    else v_server_value->>'code'
                end,
                case when v_op.payload ? 'description'
                    then v_op.payload->>'description'
                    else v_server_value->>'description'
                end,
                case when v_op.payload ? 'lifecycleStatus'
                    then v_op.payload->>'lifecycleStatus'
                    else v_server_value->>'lifecycle_status'
                end
            )
            into v_result;

        elsif v_op.entity_type = 'STORAGE_SPACE'
              and v_op.operation_type = 'CREATE' then

            v_parent_id := nullif(v_op.payload->>'storageUnitId', '')::uuid;

            select silomath.storage_space_create_command(
                p_actor_user_id,
                v_op.organization_id,
                v_op.idempotency_key,
                v_request_fingerprint,
                v_op.entity_id,
                v_parent_id,
                v_op.payload->>'code',
                v_op.payload->>'description',
                nullif(v_op.payload->>'geometryProfileId', '')::uuid,
                coalesce(nullif(v_op.payload->>'lifecycleStatus', ''), 'ACTIVE')
            )
            into v_result;

        elsif v_op.entity_type = 'STORAGE_SPACE'
              and v_op.operation_type = 'PATCH' then

            if v_op.payload ? 'storageUnitId' then
                raise exception 'storage space parent storage unit is immutable'
                    using errcode = '22023';
            end if;

            select to_jsonb(ss), ss.entity_version, ss.storage_unit_id
            into v_server_value, v_server_version, v_parent_id
            from silomath.storage_spaces ss
            where ss.storage_space_id = v_op.entity_id
              and ss.organization_id = v_op.organization_id
            for update;

            if not found then
                raise exception 'storage space not found'
                    using errcode = 'P0002';
            end if;

            select silomath.storage_space_patch_command(
                p_actor_user_id,
                v_op.organization_id,
                v_op.idempotency_key,
                v_request_fingerprint,
                v_op.entity_id,
                v_op.base_entity_version,
                v_parent_id,
                case when v_op.payload ? 'code'
                    then v_op.payload->>'code'
                    else v_server_value->>'code'
                end,
                case when v_op.payload ? 'description'
                    then v_op.payload->>'description'
                    else v_server_value->>'description'
                end,
                case when v_op.payload ? 'geometryProfileId'
                    then nullif(v_op.payload->>'geometryProfileId', '')::uuid
                    else nullif(v_server_value->>'geometry_profile_id', '')::uuid
                end,
                case when v_op.payload ? 'lifecycleStatus'
                    then v_op.payload->>'lifecycleStatus'
                    else v_server_value->>'lifecycle_status'
                end
            )
            into v_result;

        else
            update silomath.sync_operations
            set state = 'PERMANENT_ERROR',
                next_attempt_at = null,
                last_error_code = 'SYNC_OPERATION_UNSUPPORTED',
                updated_at = now()
            where sync_operation_id = p_sync_operation_id
            returning * into v_op;

            return silomath.sync_operation_result(v_op);
        end if;

        v_body := v_result->'body';
        v_entity_version := nullif(v_body->>'entity_version', '')::bigint;

        if v_entity_version is null then
            raise exception 'authoritative entity version missing'
                using errcode = 'XX000';
        end if;

        update silomath.sync_operations
        set state = 'ACKNOWLEDGED',
            authoritative_entity_version = v_entity_version,
            acknowledged_at = now(),
            next_attempt_at = null,
            last_error_code = null,
            updated_at = now()
        where sync_operation_id = p_sync_operation_id
        returning * into v_op;

        return silomath.sync_operation_result(v_op);

    exception
        when sqlstate '40001' then
            /*
             * Canonical optimistic-concurrency conflict.
             * The subtransaction above has already rolled back the failed domain command,
             * including any idempotency-claim side effects.
             */
            if v_op.entity_type = 'LOCATION' then
                select to_jsonb(l), l.entity_version
                into v_server_value, v_server_version
                from silomath.locations l
                where l.location_id = v_op.entity_id
                  and l.organization_id = v_op.organization_id;
            elsif v_op.entity_type = 'STORAGE_UNIT' then
                select to_jsonb(su), su.entity_version
                into v_server_value, v_server_version
                from silomath.storage_units su
                where su.storage_unit_id = v_op.entity_id
                  and su.organization_id = v_op.organization_id;
            elsif v_op.entity_type = 'STORAGE_SPACE' then
                select to_jsonb(ss), ss.entity_version
                into v_server_value, v_server_version
                from silomath.storage_spaces ss
                where ss.storage_space_id = v_op.entity_id
                  and ss.organization_id = v_op.organization_id;
            end if;

            if not exists (
                select 1
                from silomath.sync_conflicts sc
                where sc.sync_operation_id = p_sync_operation_id
                  and sc.status = 'OPEN'
            ) then
                insert into silomath.sync_conflicts (
                    sync_conflict_id,
                    organization_id,
                    sync_operation_id,
                    entity_type,
                    entity_id,
                    local_attempt,
                    server_value,
                    base_entity_version,
                    server_entity_version,
                    detected_at,
                    status
                )
                values (
                    gen_random_uuid(),
                    v_op.organization_id,
                    v_op.sync_operation_id,
                    v_op.entity_type,
                    v_op.entity_id,
                    v_op.payload,
                    coalesce(v_server_value, '{}'::jsonb),
                    v_op.base_entity_version,
                    v_server_version,
                    now(),
                    'OPEN'
                );
            end if;

            update silomath.sync_operations
            set state = 'CONFLICT',
                authoritative_entity_version = null,
                next_attempt_at = null,
                last_error_code = null,
                updated_at = now()
            where sync_operation_id = p_sync_operation_id
            returning * into v_op;

            return silomath.sync_operation_result(v_op);

        when sqlstate '42501'
          or sqlstate '22023'
          or sqlstate '22P02'
          or sqlstate 'P0002'
          or sqlstate '23502'
          or sqlstate '23503'
          or sqlstate '23505'
          or sqlstate '23514'
          or sqlstate '23P01' then

            get stacked diagnostics v_sqlstate = returned_sqlstate;
            v_error_code := case v_sqlstate
                when '42501' then 'SYNC_FORBIDDEN'
                when 'P0002' then 'SYNC_RESOURCE_NOT_FOUND'
                when '23505' then 'SYNC_CONFLICTING_IDENTITY'
                else 'SYNC_VALIDATION_FAILED'
            end;

            update silomath.sync_operations
            set state = 'PERMANENT_ERROR',
                next_attempt_at = null,
                last_error_code = v_error_code,
                updated_at = now()
            where sync_operation_id = p_sync_operation_id
            returning * into v_op;

            return silomath.sync_operation_result(v_op);

        when others then
            /*
             * Unknown/server-side failures remain retryable.
             * Domain writes performed inside the protected block are rolled back by
             * PL/pgSQL exception subtransaction semantics before this handler executes.
             */
            update silomath.sync_operations
            set state = 'RETRYABLE_ERROR',
                next_attempt_at = now(),
                last_error_code = 'SYNC_RETRY_REQUIRED',
                updated_at = now()
            where sync_operation_id = p_sync_operation_id
            returning * into v_op;

            return silomath.sync_operation_result(v_op);
    end;
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."sync_apply_operation"(p_actor_user_id uuid, p_sync_operation_id uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."sync_apply_operation"(p_actor_user_id uuid, p_sync_operation_id uuid) TO postgres;

CREATE OR REPLACE FUNCTION silomath.sync_batch_dispatch(p_actor_user_id uuid, p_device_installation_id uuid, p_client_version text, p_checkpoint_context text, p_operations jsonb, p_correlation_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
    v_count integer;
    v_item jsonb;
    v_results jsonb := '[]'::jsonb;
    v_existing silomath.sync_operations%rowtype;
    v_created silomath.sync_operations%rowtype;

    v_operation_id uuid;
    v_idempotency_key text;
    v_organization_id uuid;
    v_campaign_id uuid;
    v_entity_type text;
    v_entity_id uuid;
    v_operation_type text;
    v_payload jsonb;
    v_base_version bigint;
    v_event_at timestamptz;
    v_dependencies jsonb;

    v_dependency_text text;
    v_dependency_id uuid;
    v_dependency_ok boolean;
    v_fifo_blocked boolean;
    v_request_mismatch boolean;
    v_result jsonb;
    v_correlation_id text := coalesce(
        nullif(btrim(p_correlation_id), ''),
        gen_random_uuid()::text
    );
begin
    if p_actor_user_id is null then
        raise exception 'actor required'
            using errcode = '22023';
    end if;

    if p_device_installation_id is null then
        raise exception 'deviceInstallationId required'
            using errcode = '22023';
    end if;

    if nullif(btrim(p_client_version), '') is null then
        raise exception 'clientVersion required'
            using errcode = '22023';
    end if;

    if p_operations is null
       or jsonb_typeof(p_operations) <> 'array' then
        raise exception 'operations must be an array'
            using errcode = '22023';
    end if;

    v_count := jsonb_array_length(p_operations);

    if v_count < 1 or v_count > 100 then
        raise exception 'sync batch size must be between 1 and 100'
            using errcode = '22023';
    end if;

    if not exists (
        select 1
        from silomath.device_installations d
        where d.device_installation_id = p_device_installation_id
          and d.user_id = p_actor_user_id
          and d.revoked_at is null
    ) then
        raise exception 'device installation forbidden'
            using errcode = '42501';
    end if;

    for v_item in
        select value
        from jsonb_array_elements(p_operations)
    loop
        begin
            v_operation_id := nullif(v_item->>'syncOperationId','')::uuid;
            v_idempotency_key := nullif(btrim(v_item->>'idempotencyKey'),'');
            v_organization_id := nullif(v_item->>'organizationId','')::uuid;
            v_campaign_id := nullif(v_item->>'campaignId','')::uuid;
            v_entity_type := nullif(btrim(v_item->>'entityType'),'');
            v_entity_id := nullif(v_item->>'entityId','')::uuid;
            v_operation_type := nullif(btrim(v_item->>'operationType'),'');
            v_payload := coalesce(v_item->'payload', '{}'::jsonb);
            v_base_version := nullif(v_item->>'baseEntityVersion','')::bigint;
            v_event_at := coalesce(
                nullif(v_item->>'eventAtUtc','')::timestamptz,
                now()
            );
            v_dependencies := coalesce(v_item->'dependencyIds', '[]'::jsonb);
        exception
            when others then
                raise exception 'malformed sync operation'
                    using errcode = '22023';
        end;

        if v_operation_id is null
           or v_idempotency_key is null
           or v_organization_id is null
           or v_entity_type is null
           or v_entity_id is null
           or v_operation_type is null
           or jsonb_typeof(v_payload) <> 'object'
           or jsonb_typeof(v_dependencies) <> 'array' then
            raise exception 'required sync operation field missing'
                using errcode = '22023';
        end if;

        -- Frozen contract: idempotencyKey is UUID.
        perform v_idempotency_key::uuid;

        if not silomath.sync_actor_is_active_member(
            p_actor_user_id,
            v_organization_id
        ) then
            v_results := v_results || jsonb_build_array(
                jsonb_build_object(
                    'syncOperationId', v_operation_id,
                    'outcome', 'PERMANENT_ERROR',
                    'entityVersion', null,
                    'error', jsonb_build_object(
                        'code', 'SYNC_FORBIDDEN'
                    ),
                    'conflict', null
                )
            );
            continue;
        end if;

        if v_campaign_id is not null
           and not exists (
               select 1
               from silomath.inventory_campaigns c
               where c.campaign_id = v_campaign_id
                 and c.organization_id = v_organization_id
           ) then
            v_results := v_results || jsonb_build_array(
                jsonb_build_object(
                    'syncOperationId', v_operation_id,
                    'outcome', 'PERMANENT_ERROR',
                    'entityVersion', null,
                    'error', jsonb_build_object(
                        'code', 'CAMPAIGN_NOT_FOUND'
                    ),
                    'conflict', null
                )
            );
            continue;
        end if;

        /*
         * Idempotent replay.
         * Same key + materially identical immutable request => original result.
         * Same key + different request => explicit error.
         */
        select *
        into v_existing
        from silomath.sync_operations so
        where so.idempotency_key = v_idempotency_key
        for update;

        if found then
            v_request_mismatch :=
                v_existing.sync_operation_id <> v_operation_id
                or v_existing.organization_id <> v_organization_id
                or v_existing.campaign_id is distinct from v_campaign_id
                or v_existing.entity_type <> v_entity_type
                or v_existing.entity_id <> v_entity_id
                or v_existing.operation_type <> v_operation_type
                or v_existing.payload <> v_payload
                or v_existing.base_entity_version is distinct from v_base_version
                or v_existing.event_at <> v_event_at;

            if v_request_mismatch then
                v_results := v_results || jsonb_build_array(
                    jsonb_build_object(
                        'syncOperationId', v_operation_id,
                        'outcome', 'PERMANENT_ERROR',
                        'entityVersion', null,
                        'error', jsonb_build_object(
                            'code', 'IDEMPOTENCY_KEY_REUSE_MISMATCH'
                        ),
                        'conflict', null
                    )
                );
                continue;
            end if;

            /*
             * Retryable operations may be attempted again with identical
             * identity and payload. Terminal outcomes are replayed verbatim.
             */
            if v_existing.state = 'RETRYABLE_ERROR' then
                v_result := silomath.sync_apply_operation(
                    p_actor_user_id,
                    v_existing.sync_operation_id
                );
            else
                v_result := silomath.sync_operation_result(v_existing);
            end if;

            v_results := v_results || jsonb_build_array(v_result);
            continue;
        end if;

        /*
         * Explicit dependency ordering.
         * Every dependency must already be ACKNOWLEDGED. Because this loop is
         * ordered, an earlier operation in the same batch can satisfy it.
         */
        v_dependency_ok := true;

        for v_dependency_text in
            select jsonb_array_elements_text(v_dependencies)
        loop
            begin
                v_dependency_id := v_dependency_text::uuid;
            exception
                when others then
                    v_dependency_ok := false;
                    exit;
            end;

            if not exists (
                select 1
                from silomath.sync_operations dep
                where dep.sync_operation_id = v_dependency_id
                  and dep.state = 'ACKNOWLEDGED'
            ) then
                v_dependency_ok := false;
                exit;
            end if;
        end loop;

        if not v_dependency_ok then
            v_results := v_results || jsonb_build_array(
                jsonb_build_object(
                    'syncOperationId', v_operation_id,
                    'outcome', 'RETRYABLE_ERROR',
                    'entityVersion', null,
                    'error', jsonb_build_object(
                        'code', 'SYNC_DEPENDENCY_NOT_ACKNOWLEDGED'
                    ),
                    'conflict', null
                )
            );
            continue;
        end if;

        /*
         * Same-entity FIFO: unresolved older work for the entity blocks the
         * newer mutation. No silent coalescing.
         */
        select exists (
            select 1
            from silomath.sync_operations older
            where older.organization_id = v_organization_id
              and older.entity_type = v_entity_type
              and older.entity_id = v_entity_id
              and older.state in (
                  'PENDING',
                  'IN_FLIGHT',
                  'RETRYABLE_ERROR',
                  'CONFLICT'
              )
        )
        into v_fifo_blocked;

        if v_fifo_blocked then
            v_results := v_results || jsonb_build_array(
                jsonb_build_object(
                    'syncOperationId', v_operation_id,
                    'outcome', 'RETRYABLE_ERROR',
                    'entityVersion', null,
                    'error', jsonb_build_object(
                        'code', 'SYNC_ENTITY_FIFO_BLOCKED'
                    ),
                    'conflict', null
                )
            );
            continue;
        end if;

        insert into silomath.sync_operations (
            sync_operation_id,
            organization_id,
            campaign_id,
            entity_type,
            entity_id,
            operation_type,
            payload,
            base_entity_version,
            state,
            idempotency_key,
            attempt_count,
            next_attempt_at,
            last_attempt_at,
            last_error_code,
            correlation_id,
            event_at,
            acknowledged_at,
            authoritative_entity_version,
            created_at,
            updated_at
        )
        values (
            v_operation_id,
            v_organization_id,
            v_campaign_id,
            v_entity_type,
            v_entity_id,
            v_operation_type,
            v_payload,
            v_base_version,
            'PENDING',
            v_idempotency_key,
            0,
            null,
            null,
            null,
            v_correlation_id,
            v_event_at,
            null,
            null,
            now(),
            now()
        )
        returning * into v_created;

        for v_dependency_text in
            select jsonb_array_elements_text(v_dependencies)
        loop
            v_dependency_id := v_dependency_text::uuid;

            insert into silomath.sync_operation_dependencies (
                dependency_id,
                parent_operation_id,
                dependent_operation_id,
                dependency_type,
                created_at
            )
            values (
                gen_random_uuid(),
                v_dependency_id,
                v_operation_id,
                'REQUIRED_BEFORE',
                now()
            );
        end loop;

        v_result := silomath.sync_apply_operation(
            p_actor_user_id,
            v_operation_id
        );

        v_results := v_results || jsonb_build_array(v_result);
    end loop;

    return jsonb_build_object(
        'results', v_results,
        'serverTimeUtc', now(),
        'correlationId', v_correlation_id
    );
end;
$function$;
REVOKE ALL ON FUNCTION "silomath"."sync_batch_dispatch"(p_actor_user_id uuid, p_device_installation_id uuid, p_client_version text, p_checkpoint_context text, p_operations jsonb, p_correlation_id text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION "silomath"."sync_batch_dispatch"(p_actor_user_id uuid, p_device_installation_id uuid, p_client_version text, p_checkpoint_context text, p_operations jsonb, p_correlation_id text) TO postgres;

-- Triggers
CREATE TRIGGER trg_audit_events_append_only_rows BEFORE DELETE OR UPDATE ON silomath.audit_events FOR EACH ROW EXECUTE FUNCTION silomath.reject_audit_history_mutation();
CREATE TRIGGER trg_audit_events_append_only_truncate BEFORE TRUNCATE ON silomath.audit_events FOR EACH STATEMENT EXECUTE FUNCTION silomath.reject_audit_history_mutation();
CREATE TRIGGER trg_campaign_snapshot_commodity_items_immutable BEFORE DELETE OR UPDATE ON silomath.campaign_snapshot_commodity_items FOR EACH ROW EXECUTE FUNCTION silomath.reject_immutable_snapshot_mutation();
CREATE TRIGGER trg_campaign_snapshot_scope_items_immutable BEFORE DELETE OR UPDATE ON silomath.campaign_snapshot_scope_items FOR EACH ROW EXECUTE FUNCTION silomath.reject_immutable_snapshot_mutation();
CREATE TRIGGER trg_campaign_snapshot_team_items_immutable BEFORE DELETE OR UPDATE ON silomath.campaign_snapshot_team_items FOR EACH ROW EXECUTE FUNCTION silomath.reject_immutable_snapshot_mutation();
CREATE TRIGGER trg_campaign_snapshots_immutable BEFORE DELETE OR UPDATE ON silomath.campaign_snapshots FOR EACH ROW EXECUTE FUNCTION silomath.reject_immutable_snapshot_mutation();
CREATE TRIGGER trg_campaign_team_assignments_entity_version BEFORE UPDATE ON silomath.campaign_team_assignments FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_clients_entity_version BEFORE UPDATE ON silomath.clients FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER evidence_active_campaign BEFORE INSERT OR UPDATE ON silomath.evidence FOR EACH ROW EXECUTE FUNCTION silomath.enforce_active_campaign_write();
CREATE TRIGGER trg_evidence_entity_version BEFORE UPDATE ON silomath.evidence FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_final_inventory_snapshot_entries_immutable BEFORE DELETE OR UPDATE ON silomath.final_inventory_snapshot_entries FOR EACH ROW EXECUTE FUNCTION silomath.reject_immutable_snapshot_mutation();
CREATE TRIGGER trg_final_inventory_snapshots_immutable BEFORE DELETE OR UPDATE ON silomath.final_inventory_snapshots FOR EACH ROW EXECUTE FUNCTION silomath.reject_immutable_snapshot_mutation();
CREATE TRIGGER trg_geometry_profiles_entity_version BEFORE UPDATE ON silomath.geometry_profiles FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_inventory_campaigns_entity_version BEFORE UPDATE ON silomath.inventory_campaigns FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER issues_active_campaign BEFORE INSERT OR UPDATE ON silomath.issues FOR EACH ROW EXECUTE FUNCTION silomath.enforce_active_campaign_write();
CREATE TRIGGER trg_issues_entity_version BEFORE UPDATE ON silomath.issues FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_locations_entity_version BEFORE UPDATE ON silomath.locations FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_measurements_entity_version BEFORE UPDATE ON silomath.measurements FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_organization_configurations_entity_version BEFORE UPDATE ON silomath.organization_configurations FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_organization_memberships_entity_version BEFORE UPDATE ON silomath.organization_memberships FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_organizations_entity_version BEFORE UPDATE ON silomath.organizations FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER stocktaking_records_active_campaign BEFORE INSERT OR UPDATE ON silomath.stocktaking_records FOR EACH ROW EXECUTE FUNCTION silomath.enforce_active_campaign_write();
CREATE TRIGGER trg_stocktaking_records_entity_version BEFORE UPDATE ON silomath.stocktaking_records FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER stocktaking_revisions_active_campaign BEFORE INSERT OR UPDATE ON silomath.stocktaking_revisions FOR EACH ROW EXECUTE FUNCTION silomath.enforce_active_campaign_write();
CREATE TRIGGER trg_stocktaking_revisions_entity_version BEFORE UPDATE ON silomath.stocktaking_revisions FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_storage_spaces_entity_version BEFORE UPDATE ON silomath.storage_spaces FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_storage_spaces_identity_history BEFORE UPDATE ON silomath.storage_spaces FOR EACH ROW EXECUTE FUNCTION silomath.guard_storage_space_identity_history();
CREATE TRIGGER trg_storage_units_entity_version BEFORE UPDATE ON silomath.storage_units FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();
CREATE TRIGGER trg_storage_units_identity_history BEFORE UPDATE ON silomath.storage_units FOR EACH ROW EXECUTE FUNCTION silomath.guard_storage_unit_identity_history();
CREATE TRIGGER trg_users_entity_version BEFORE UPDATE ON silomath.users FOR EACH ROW EXECUTE FUNCTION silomath.enforce_entity_version_increment();

-- Row-level security
ALTER TABLE "silomath"."api_idempotency_records" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."api_idempotency_records" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."audit_events" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."audit_events" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."calculation_results" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."calculation_results" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_scopes" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_scopes" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_snapshot_commodity_items" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_snapshot_commodity_items" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_snapshot_scope_items" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_snapshot_scope_items" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_snapshot_team_items" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_snapshot_team_items" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_snapshots" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_snapshots" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_team_assignments" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."campaign_team_assignments" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."clients" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."clients" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."commodities" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."commodities" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."commodity_versions" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."commodity_versions" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."device_installations" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."device_installations" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."evidence" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."evidence" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."final_inventory_snapshot_entries" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."final_inventory_snapshot_entries" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."final_inventory_snapshots" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."final_inventory_snapshots" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."geometry_families" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."geometry_families" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."geometry_profile_versions" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."geometry_profile_versions" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."geometry_profiles" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."geometry_profiles" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."inventory_campaigns" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."inventory_campaigns" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."issue_resolutions" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."issue_resolutions" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."issue_status_events" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."issue_status_events" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."issues" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."issues" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."locations" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."locations" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."measurements" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."measurements" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."notifications" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."notifications" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."offline_working_set_manifests" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."offline_working_set_manifests" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."organization_configurations" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."organization_configurations" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."organization_memberships" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."organization_memberships" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."organizations" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."organizations" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."stocktaking_records" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."stocktaking_records" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."stocktaking_revisions" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."stocktaking_revisions" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."storage_spaces" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."storage_spaces" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."storage_units" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."storage_units" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."sync_change_stream" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."sync_change_stream" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."sync_conflicts" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."sync_conflicts" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."sync_operation_dependencies" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."sync_operation_dependencies" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."sync_operations" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."sync_operations" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."user_preferences" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."user_preferences" NO FORCE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."users" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "silomath"."users" NO FORCE ROW LEVEL SECURITY;

-- Explicit table privileges
GRANT INSERT ON TABLE "silomath"."api_idempotency_records" TO "service_role";
GRANT SELECT ON TABLE "silomath"."api_idempotency_records" TO "service_role";
GRANT UPDATE ON TABLE "silomath"."api_idempotency_records" TO "service_role";

-- Frozen canonical master-data seed
SET CONSTRAINTS ALL DEFERRED;
INSERT INTO "silomath"."geometry_families" ("geometry_family_id", "family_code", "name", "formula_version", "parameter_contract") VALUES
    ('e9ffe9ed-5486-4b71-82e2-5b1e7bc0dae4'::uuid, 'CYL_TRUNC_CONE_V1', 'Cylindrical Silo with Truncated Cone Hopper', '1', 'D,Hcyl,Hhop,d'),
    ('d66d5fa9-a8c0-48b3-949e-b55a0ac82a9b'::uuid, 'CYL_FLAT_V1', 'Cylindrical Silo with Flat Bottom', '1', 'D,H'),
    ('bb349182-64a3-44dc-a4ef-4244b2028da1'::uuid, 'SQUARE_FLAT_V1', 'Square Silo with Flat Bottom', '1', 'a,H'),
    ('ee751a22-f927-4f52-bfcb-a39337b2c3ab'::uuid, 'RECT_FLAT_V1', 'Rectangular Silo with Flat Bottom', '1', 'L,W,H'),
    ('8571405d-9fe3-4207-b0d5-0ca2978a574d'::uuid, 'SQUARE_PYRAMID_HOPPER_V1', 'Square Silo with Pyramidal Hopper', '1', 'a,Hbody,Hhop'),
    ('9421f6a0-7545-4f21-9d4d-170ec27908a8'::uuid, 'RECT_PYRAMID_HOPPER_V1', 'Rectangular Silo with Pyramidal Hopper', '1', 'L,W,Hbody,Hhop'),
    ('356fdebc-643c-47c8-8cf2-290297cddbe9'::uuid, 'REG_HEX_FLAT_V1', 'Regular Hexagonal Silo with Flat Bottom', '1', 's,H'),
    ('3f8fc3f7-d41d-450e-ba67-25ab6199177e'::uuid, 'STAR_VERIFIED_AREA_V1', 'Star-Shaped Storage with Verified Area', '1', 'A,H,provenance'),
    ('ccccb8df-7ada-4678-bb8c-bb8953b3abc0'::uuid, 'POCKET_VERIFIED_AREA_V1', 'Pocket Storage with Verified Area', '1', 'A,H,provenance'),
    ('42acc8df-bf73-446b-97a9-d70b71fc1b55'::uuid, 'PERIMETER_VERIFIED_AREA_V1', 'Perimeter-Based Storage with Verified Area', '1', 'A,H,provenance');

INSERT INTO "silomath"."commodities" ("commodity_id", "code", "current_version_id", "lifecycle_status", "created_at", "created_by") VALUES
    ('818a3f72-919a-4457-88f8-b2c5fef47531'::uuid, 'COMMON_WHEAT', '1fb56a69-52c5-4a0b-864f-e0c01f10d87b'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('dbb5f501-29e0-49a9-afba-5b0c1c96a295'::uuid, 'DURUM_WHEAT', '6c7ab258-c783-4d81-abd4-d45500f88d77'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('11d9b077-df2a-4f81-891d-c09f6cf91caf'::uuid, 'SPELT', '55142479-dd2f-4760-83d4-6f02f68554ac'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('1d045c4d-70b1-42f4-88a2-ddfbc8172af3'::uuid, 'CORN_MAIZE', '5c148b1b-7737-49d5-b8d5-71ae7c363be5'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('5afd2b82-5703-42ef-adfa-3260527ffca9'::uuid, 'BARLEY', '04917623-6fa5-425c-9151-2ff24cacbe55'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('c4f0d150-153a-4cd9-9617-b758b051c267'::uuid, 'MALTING_BARLEY', 'e60dc77b-6653-4ae2-89e7-d68584ed54f1'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('d31e40b5-ab9d-4dde-87fb-481dc55ace45'::uuid, 'OATS', '118c0d9f-0eda-46de-b122-203986c31b1a'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('45fe2b69-ad66-41b4-b06d-e3fc0ddb95c7'::uuid, 'RYE', 'cb0c8c74-a3ff-4628-90b3-88a4ac98fc5e'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('b4236fd4-2cff-4d1c-952c-31830126e604'::uuid, 'TRITICALE', '728f6d1a-229a-4779-b708-74089c7316ff'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('756a7299-a9c0-4e37-956b-43dcb021e8dd'::uuid, 'SORGHUM', 'ecc245ae-544d-46a6-9388-acfcf63a210b'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('b5b55183-e463-4f0d-8b62-35956732b972'::uuid, 'MILLET', '4c39236b-6425-49dc-8bfc-07e1253c0c79'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('657a47d7-b3e9-4e4c-a7fe-14584a674614'::uuid, 'RICE', '363efb87-d6a0-4126-827b-781272f17c17'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('bbc673c0-1c1c-4c3f-9748-79efc9637831'::uuid, 'BUCKWHEAT', 'dabe5350-ad80-493d-ae4c-26c85023378d'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('cd468a62-2c8c-483e-8188-4acb9dffa7d3'::uuid, 'SUNFLOWER_SEED', '0fb44791-2ae9-4ce6-a5f4-f0f53781d843'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('a7f2e55a-fac7-419d-ae4a-d3c45850a461'::uuid, 'RAPESEED_CANOLA', '251abaf9-df15-4497-b724-a342a44fa141'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('650eb434-b293-439c-951d-80b8503f5dc9'::uuid, 'SOYBEAN', '69b4e43d-8cbb-4f49-a947-977dc03c598d'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('c9407477-c671-43d2-82eb-ca10657c8a2b'::uuid, 'MUSTARD_SEED', 'eb4dea2f-a312-416a-8fbc-b1777b664406'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('4babc1c2-c711-4145-a2f2-b9c509634216'::uuid, 'LINSEED_FLAXSEED', '3f6d4198-46c9-43bb-9fe9-6fdaadaac250'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('f0874f28-2ff8-4899-8ace-5b1ef2438d23'::uuid, 'FIELD_PEAS', '8c97ca3e-8cd9-42eb-8d84-4259c7ed6637'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('3654d63e-0168-45ba-b719-022c0cdf60b8'::uuid, 'FIELD_FABA_BEANS', '9485741c-fc69-4487-a864-78a187345e6a'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('8aad8011-ba17-4be4-b0b5-28bb32c3c5f6'::uuid, 'LUPINS', '18be4453-5412-43ed-bcde-d0e2eb5627bc'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('ff48e528-4b59-4e8d-a326-d04620693bed'::uuid, 'DRY_BEANS', 'b94b7a10-2c03-4df6-b906-2ad763a7cfca'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('983e65d7-d055-400a-8628-9f3e5a878a2e'::uuid, 'CHICKPEAS', '170d215b-2ad3-4af4-af8f-84090c7083fb'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid),
    ('d12134e2-c48b-4468-9f0c-ff4c30016cfd'::uuid, 'LENTILS', 'bfbfaf71-6ffd-40c1-9425-2064b299fb7a'::uuid, 'ACTIVE', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid);

INSERT INTO "silomath"."commodity_versions" ("commodity_version_id", "commodity_id", "version_number", "display_name", "category", "reference_test_weight_value", "reference_test_weight_unit", "moisture_reference", "impurities_reference", "schema_version", "created_at", "created_by", "definition_metadata") VALUES
    ('1fb56a69-52c5-4a0b-864f-e0c01f10d87b'::uuid, '818a3f72-919a-4457-88f8-b2c5fef47531'::uuid, 1, 'Common Wheat', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('6c7ab258-c783-4d81-abd4-d45500f88d77'::uuid, 'dbb5f501-29e0-49a9-afba-5b0c1c96a295'::uuid, 1, 'Durum Wheat', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('55142479-dd2f-4760-83d4-6f02f68554ac'::uuid, '11d9b077-df2a-4f81-891d-c09f6cf91caf'::uuid, 1, 'Spelt', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('5c148b1b-7737-49d5-b8d5-71ae7c363be5'::uuid, '1d045c4d-70b1-42f4-88a2-ddfbc8172af3'::uuid, 1, 'Corn/Maize', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('04917623-6fa5-425c-9151-2ff24cacbe55'::uuid, '5afd2b82-5703-42ef-adfa-3260527ffca9'::uuid, 1, 'Barley', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('e60dc77b-6653-4ae2-89e7-d68584ed54f1'::uuid, 'c4f0d150-153a-4cd9-9617-b758b051c267'::uuid, 1, 'Malting Barley', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('118c0d9f-0eda-46de-b122-203986c31b1a'::uuid, 'd31e40b5-ab9d-4dde-87fb-481dc55ace45'::uuid, 1, 'Oats', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('cb0c8c74-a3ff-4628-90b3-88a4ac98fc5e'::uuid, '45fe2b69-ad66-41b4-b06d-e3fc0ddb95c7'::uuid, 1, 'Rye', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('728f6d1a-229a-4779-b708-74089c7316ff'::uuid, 'b4236fd4-2cff-4d1c-952c-31830126e604'::uuid, 1, 'Triticale', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('ecc245ae-544d-46a6-9388-acfcf63a210b'::uuid, '756a7299-a9c0-4e37-956b-43dcb021e8dd'::uuid, 1, 'Sorghum', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('4c39236b-6425-49dc-8bfc-07e1253c0c79'::uuid, 'b5b55183-e463-4f0d-8b62-35956732b972'::uuid, 1, 'Millet', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('363efb87-d6a0-4126-827b-781272f17c17'::uuid, '657a47d7-b3e9-4e4c-a7fe-14584a674614'::uuid, 1, 'Rice', 'Cereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('dabe5350-ad80-493d-ae4c-26c85023378d'::uuid, 'bbc673c0-1c1c-4c3f-9748-79efc9637831'::uuid, 1, 'Buckwheat', 'Pseudocereal', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('0fb44791-2ae9-4ce6-a5f4-f0f53781d843'::uuid, 'cd468a62-2c8c-483e-8188-4acb9dffa7d3'::uuid, 1, 'Sunflower Seed', 'Oilseed', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('251abaf9-df15-4497-b724-a342a44fa141'::uuid, 'a7f2e55a-fac7-419d-ae4a-d3c45850a461'::uuid, 1, 'Rapeseed/Canola', 'Oilseed', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('69b4e43d-8cbb-4f49-a947-977dc03c598d'::uuid, '650eb434-b293-439c-951d-80b8503f5dc9'::uuid, 1, 'Soybean', 'Oilseed', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('eb4dea2f-a312-416a-8fbc-b1777b664406'::uuid, 'c9407477-c671-43d2-82eb-ca10657c8a2b'::uuid, 1, 'Mustard Seed', 'Oilseed', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('3f6d4198-46c9-43bb-9fe9-6fdaadaac250'::uuid, '4babc1c2-c711-4145-a2f2-b9c509634216'::uuid, 1, 'Linseed/Flaxseed', 'Oilseed', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('8c97ca3e-8cd9-42eb-8d84-4259c7ed6637'::uuid, 'f0874f28-2ff8-4899-8ace-5b1ef2438d23'::uuid, 1, 'Field Peas', 'Pulse', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('9485741c-fc69-4487-a864-78a187345e6a'::uuid, '3654d63e-0168-45ba-b719-022c0cdf60b8'::uuid, 1, 'Field/Faba Beans', 'Pulse', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('18be4453-5412-43ed-bcde-d0e2eb5627bc'::uuid, '8aad8011-ba17-4be4-b0b5-28bb32c3c5f6'::uuid, 1, 'Lupins', 'Pulse', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('b94b7a10-2c03-4df6-b906-2ad763a7cfca'::uuid, 'ff48e528-4b59-4e8d-a326-d04620693bed'::uuid, 1, 'Dry Beans', 'Pulse', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('170d215b-2ad3-4af4-af8f-84090c7083fb'::uuid, '983e65d7-d055-400a-8628-9f3e5a878a2e'::uuid, 1, 'Chickpeas', 'Pulse', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL),
    ('bfbfaf71-6ffd-40c1-9425-2064b299fb7a'::uuid, 'd12134e2-c48b-4468-9f0c-ff4c30016cfd'::uuid, 1, 'Lentils', 'Pulse', NULL, NULL, NULL, NULL, '1', '2026-10-31T16:35:25.420873+00:00'::timestamptz, '00000000-0000-0000-0000-000000000001'::uuid, NULL);

SELECT pg_catalog.setval('silomath.sync_change_stream_change_sequence_seq', 1, false);

COMMIT;

-- End of SiloMath V6 canonical baseline.
