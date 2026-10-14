-- SiloMath V6
-- MIGRATE phase: controlled provenance staging for legacy CampaignSnapshot.
--
-- Frozen V6 requires exact historical schema/calculation compatibility
-- identifiers. Legacy campaign_launch_snapshots do not contain those
-- identifiers.
--
-- Therefore:
--   * no version value is synthesized;
--   * no current V6 version is substituted;
--   * canonical snapshot migration is allowed only after explicit
--     provenance has been supplied from authoritative deployment/history
--     evidence;
--   * legacy source rows remain untouched.

create table if not exists silomath.legacy_campaign_snapshot_provenance (
    legacy_snapshot_id uuid primary key,

    legacy_campaign_id uuid not null,

    snapshot_schema_version text null,

    engine_version text null,
    input_schema_version text null,
    normalization_version text null,
    surface_model_version text null,
    aggregation_method text null,
    aggregation_rule_version text null,

    provenance_source text null,
    provenance_recorded_at timestamptz null,
    provenance_recorded_by text null,

    migration_status text not null default 'NEEDS_PROVENANCE',

    constraint legacy_campaign_snapshot_provenance_status_check
        check (
            migration_status in (
                'NEEDS_PROVENANCE',
                'READY',
                'MIGRATED'
            )
        ),

    constraint legacy_campaign_snapshot_provenance_ready_check
        check (
            migration_status = 'NEEDS_PROVENANCE'
            or (
                snapshot_schema_version is not null
                and btrim(snapshot_schema_version) <> ''
                and engine_version is not null
                and btrim(engine_version) <> ''
                and input_schema_version is not null
                and btrim(input_schema_version) <> ''
                and normalization_version is not null
                and btrim(normalization_version) <> ''
                and surface_model_version is not null
                and btrim(surface_model_version) <> ''
                and aggregation_method is not null
                and btrim(aggregation_method) <> ''
                and aggregation_rule_version is not null
                and btrim(aggregation_rule_version) <> ''
                and provenance_source is not null
                and btrim(provenance_source) <> ''
                and provenance_recorded_at is not null
                and provenance_recorded_by is not null
                and btrim(provenance_recorded_by) <> ''
            )
        )
);

insert into silomath.legacy_campaign_snapshot_provenance (
    legacy_snapshot_id,
    legacy_campaign_id,
    migration_status
)
select
    cls.id,
    cls.campaign_id,
    'NEEDS_PROVENANCE'
from public.campaign_launch_snapshots cls
on conflict (legacy_snapshot_id) do nothing;

do $$
declare
    legacy_count integer;
    staged_count integer;
begin
    select count(*)
    into legacy_count
    from public.campaign_launch_snapshots;

    select count(*)
    into staged_count
    from silomath.legacy_campaign_snapshot_provenance;

    if staged_count < legacy_count then
        raise exception
            'V6 snapshot provenance staging failed: staged count % is lower than legacy snapshot count %',
            staged_count,
            legacy_count;
    end if;
end;
$$;

-- Deliberately no canonical CampaignSnapshot INSERT here.
--
-- A later migration may consume only rows with:
--
--     migration_status = 'READY'
--
-- and must fail rather than fabricate provenance for any remaining
-- NEEDS_PROVENANCE row.
