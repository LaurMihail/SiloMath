-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical Evidence metadata persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   12_OFFLINE_SYNC_SPECIFICATION
--   15_EVIDENCE_STORAGE_SPECIFICATION
--
-- Additive only:
--   * Evidence types are exactly PHOTO / NOTE;
--   * every Evidence has exactly one principal owner: StocktakingRecord XOR Issue;
--   * stocktaking_revision_id is conditional and freezes when Evidence enters
--     validated Stocktaking history;
--   * PHOTO binaries remain external to PostgreSQL/Room;
--   * database persistence stores only metadata, hashes and opaque references;
--   * logical working removal is represented by removed_at / removed_by;
--   * validated/final Evidence is immutable;
--   * owner/discriminator/photo-limit/check hardening remains IMP-DB-004..006;
--   * FK completion remains IMP-DB-003.

create schema if not exists silomath;

create table if not exists silomath.evidence (
    evidence_id uuid primary key,

    organization_id uuid not null,
    campaign_id uuid not null,

    stocktaking_record_id uuid null,
    issue_id uuid null,
    stocktaking_revision_id uuid null,

    evidence_type text not null,

    note_text text null,

    local_private_ref text null,
    remote_object_ref text null,

    sha256 text null,
    mime_type text null,
    byte_size bigint null,
    pixel_width integer null,
    pixel_height integer null,

    sync_state text not null,

    removed_at timestamptz null,
    removed_by uuid null,

    created_at timestamptz not null,
    created_by uuid not null,

    entity_version bigint not null default 1
);
