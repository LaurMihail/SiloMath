-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical synchronized UserPreference persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--
-- Additive only:
--   * UserPreference is user-scoped, not Organization-scoped;
--   * exactly one logical synchronized preference set exists per User;
--   * theme is restricted by the frozen SYSTEM/LIGHT/DARK value domain;
--   * notification_preferences may contain only actually configurable
--     NotificationType preferences;
--   * mandatory/non-configurable notification behavior cannot be invented here;
--   * presentation-only local settings may remain DataStore-owned;
--   * user uniqueness remains IMP-DB-004;
--   * value-domain/structured preference checks remain IMP-DB-005;
--   * FK completion remains IMP-DB-003.

create schema if not exists silomath;

create table if not exists silomath.user_preferences (
    user_preference_id uuid primary key,

    user_id uuid not null,

    theme text not null,
    notification_preferences jsonb null,

    updated_at timestamptz not null
);
