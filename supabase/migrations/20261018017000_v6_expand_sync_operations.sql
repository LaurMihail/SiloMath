-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical SyncOperation persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   12_OFFLINE_SYNC_SPECIFICATION
--
-- Additive only:
--   * SyncOperation is a durable transactional outbox operation;
--   * one logical mutation keeps one stable operation/idempotency identity;
--   * organization_id preserves tenant context;
--   * campaign_id is optional operational context;
--   * payload stores canonical serialized mutation intent, not business source-of-truth state;
--   * base_entity_version is nullable when the mutation target is not versioned;
--   * retry and acknowledgement metadata preserve crash-safe replay semantics;
--   * ACKNOWLEDGED is the only successful terminal state;
--   * value-domain and lifecycle checks remain IMP-DB-005;
--   * idempotency uniqueness remains IMP-DB-004;
--   * FK completion remains IMP-DB-003;
--   * worker indexes remain IMP-DB-006.

create schema if not exists silomath;

create table if not exists silomath.sync_operations (
    sync_operation_id uuid primary key,

    organization_id uuid not null,
    campaign_id uuid null,

    entity_type text not null,
    entity_id uuid not null,
    operation_type text not null,

    payload jsonb not null,

    base_entity_version bigint null,

    state text not null,
    idempotency_key text not null,

    attempt_count bigint not null,
    next_attempt_at timestamptz null,
    last_attempt_at timestamptz null,

    last_error_code text null,
    correlation_id text null,

    event_at timestamptz not null,
    acknowledged_at timestamptz null,
    authoritative_entity_version bigint null,

    created_at timestamptz not null,
    updated_at timestamptz not null
);
