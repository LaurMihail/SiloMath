-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical Notification persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--
-- Additive only:
--   * Notification is an in-app user projection and is not a source of truth;
--   * organization_id preserves tenant context;
--   * recipient_user_id identifies the notification recipient;
--   * read_at is the authoritative mutable read-state timestamp;
--   * state is derived from read_at and cannot diverge from it;
--   * context stores only the optional typed contextual/deep-link reference;
--   * navigation must re-authorize current target access/state;
--   * server retention is 12 months;
--   * notification type value-domain enforcement remains IMP-DB-005;
--   * FK completion remains IMP-DB-003;
--   * index strategy remains IMP-DB-006.

create schema if not exists silomath;

create table if not exists silomath.notifications (
    notification_id uuid primary key,

    organization_id uuid not null,
    recipient_user_id uuid not null,

    notification_type text not null,

    title text not null,
    body text not null,
    context jsonb null,

    created_at timestamptz not null,
    read_at timestamptz null,

    state text generated always as (
        case
            when read_at is null then 'UNREAD'
            else 'READ'
        end
    ) stored
);
