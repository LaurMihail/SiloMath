-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical OfflineWorkingSetManifest persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   12_OFFLINE_SYNC_SPECIFICATION
--
-- Additive only:
--   * manifest rows represent versioned Campaign x DeviceInstallation packages;
--   * content_manifest stores technical completeness counts/version/hash inventory;
--   * business entities remain persisted in their canonical tables, not inside
--     content_manifest as an opaque authoritative payload;
--   * package integrity uses cryptographic hash/verification metadata;
--   * V6 does not introduce a separate package-signing PKI;
--   * OFFLINE_READY remains derived and is not persisted as CampaignStatus;
--   * FK completion remains IMP-DB-003;
--   * unique/current/check/index hardening remains IMP-DB-004..006.

create schema if not exists silomath;

create table if not exists silomath.offline_working_set_manifests (
    manifest_id uuid primary key,

    campaign_id uuid not null,
    organization_id uuid not null,
    device_installation_id uuid not null,

    package_version text not null,

    content_manifest jsonb not null,

    integrity_hash text not null,
    integrity_algorithm text not null,
    verification_metadata jsonb null,

    generated_at timestamptz not null,
    downloaded_at timestamptz null,
    verified_at timestamptz null,

    is_current boolean not null default false
);
