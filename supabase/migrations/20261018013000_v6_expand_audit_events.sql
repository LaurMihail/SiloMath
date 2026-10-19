-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical append-only AuditEvent persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   17_SECURITY_AUDIT_PRIVACY
--
-- Additive only:
--   * AuditEvent is authoritative append-only history;
--   * actor_user_id may be null only for SYSTEM actions, where source and
--     system_actor identify the authoritative system actor;
--   * structured before/after/change metadata is permitted as an audit envelope;
--   * secrets, credentials and Evidence binaries must never be stored here;
--   * no semantic update metadata or entity_version applies to append-only events;
--   * actor/system and value-domain checks remain IMP-DB-005;
--   * FK completion remains IMP-DB-003;
--   * index strategy remains IMP-DB-006;
--   * database append-only enforcement remains IMP-DB-009 hardening.

create schema if not exists silomath;

create table if not exists silomath.audit_events (
    audit_event_id uuid primary key,

    organization_id uuid not null,
    campaign_id uuid null,

    actor_user_id uuid null,

    action text not null,
    entity_type text not null,
    entity_id uuid not null,

    occurred_at timestamptz not null,

    before_data jsonb null,
    after_data jsonb null,
    change_metadata jsonb null,

    correlation_id text not null,

    source text not null,
    system_actor text null
);
