-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical Issue lifecycle and history persistence.
--
-- Governing baseline:
--   05_WORKFLOWS_STATE_MACHINES
--   06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--
-- Additive only:
--   * Issue current projection uses exactly OPEN / RESOLVED;
--   * resolution is explicit and never inferred from validation or Mark Empty;
--   * IssueResolution and IssueStatusEvent are append-only history;
--   * resolved_at / resolved_by on Issue represent only the latest effective
--     resolution summary and may become null again after reopen;
--   * audit_event_id links are persisted now; FK completion remains IMP-DB-003;
--   * enum/check/index hardening remains IMP-DB-004..006.

create schema if not exists silomath;

create table if not exists silomath.issues (
    issue_id uuid primary key,
    organization_id uuid not null,
    campaign_id uuid not null,
    stocktaking_record_id uuid not null,

    category text not null,
    description text not null,
    status text not null,

    reported_at timestamptz not null,
    reported_by uuid not null,

    resolved_at timestamptz null,
    resolved_by uuid null,

    entity_version bigint not null default 1
);

create table if not exists silomath.issue_resolutions (
    issue_resolution_id uuid primary key,
    issue_id uuid not null,

    resolution_reason text not null,
    resolution_context text not null,

    resolved_at timestamptz not null,
    resolved_by uuid not null,

    audit_event_id uuid not null
);

create table if not exists silomath.issue_status_events (
    issue_status_event_id uuid primary key,
    issue_id uuid not null,

    from_status text null,
    to_status text not null,
    reason text null,

    changed_at timestamptz not null,
    changed_by uuid not null,

    audit_event_id uuid not null
);
