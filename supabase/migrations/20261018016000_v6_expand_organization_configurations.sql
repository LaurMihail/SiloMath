-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical OrganizationConfiguration persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--
-- Additive only:
--   * OrganizationConfiguration is Organization-scoped;
--   * configuration_key must come from the platform-controlled approved registry;
--   * value_type is one of BOOLEAN / INTEGER / DECIMAL / TEXT / ENUM;
--   * values use dedicated typed scalar columns;
--   * arbitrary business JSON is prohibited;
--   * system-controlled product rules must never become editable configuration;
--   * Organization x configuration_key uniqueness remains IMP-DB-004;
--   * value_type / exactly-one-compatible-value enforcement remains IMP-DB-005;
--   * FK completion remains IMP-DB-003.

create schema if not exists silomath;

create table if not exists silomath.organization_configurations (
    organization_configuration_id uuid primary key,

    organization_id uuid not null,
    configuration_key text not null,
    value_type text not null,

    boolean_value boolean null,
    integer_value bigint null,
    decimal_value numeric(20,6) null,
    text_value text null,
    enum_value text null,

    updated_at timestamptz not null,
    updated_by uuid not null,
    entity_version bigint not null
);
