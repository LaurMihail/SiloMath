-- SiloMath V6
-- MIGRATE phase: legacy public Campaign core -> canonical silomath Campaign core.
--
-- This migration intentionally does NOT migrate CampaignSnapshot yet.
-- Legacy snapshot rows do not contain authoritative schema/calculation
-- compatibility provenance required by the V6 canonical snapshot contract.
--
-- No destructive legacy cleanup is performed here.

do $$
declare
    invalid_count integer;
begin
    select count(*)
    into invalid_count
    from public.campaigns
    where inventory_type not in (
        'Annual',
        'Monthly',
        'Control'
    );

    if invalid_count > 0 then
        raise exception
            'V6 migration blocked: % legacy campaigns contain unsupported inventory_type values',
            invalid_count;
    end if;
end;
$$;

do $$
declare
    invalid_count integer;
begin
    select count(*)
    into invalid_count
    from public.campaigns
    where status not in (
        'Active',
        'Completed'
    );

    if invalid_count > 0 then
        raise exception
            'V6 migration blocked: % legacy campaigns contain unsupported campaign status values',
            invalid_count;
    end if;
end;
$$;

do $$
declare
    invalid_count integer;
begin
    select count(*)
    into invalid_count
    from public.campaigns
    where code is null
       or code !~ '^INV-[0-9]{4}-[0-9]{4}$';

    if invalid_count > 0 then
        raise exception
            'V6 migration blocked: % legacy campaigns have non-canonical Campaign Code',
            invalid_count;
    end if;
end;
$$;

do $$
declare
    invalid_count integer;
begin
    select count(*)
    into invalid_count
    from public.campaigns
    where launched_at is null
       or launched_by is null;

    if invalid_count > 0 then
        raise exception
            'V6 migration blocked: % legacy campaigns lack authoritative launch actor/time',
            invalid_count;
    end if;
end;
$$;

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
select
    c.id,
    c.organization_id,
    c.code,
    substring(c.code from 5 for 4)::integer,
    substring(c.code from 10 for 4)::integer,
    c.name,
    case c.inventory_type
        when 'Annual' then 'ANNUAL'
        when 'Monthly' then 'MONTHLY'
        when 'Control' then 'CONTROL'
    end,
    c.start_date,
    c.planned_end_date,
    case c.status
        when 'Active' then 'ACTIVE'
        when 'Completed' then 'COMPLETED'
    end,
    c.launched_at,
    c.launched_by,
    null,
    c.launched_at,
    c.launched_by,
    c.launched_at,
    c.launched_by,
    1
from public.campaigns c
on conflict (campaign_id) do nothing;

do $$
declare
    legacy_count integer;
    canonical_count integer;
begin
    select count(*)
    into legacy_count
    from public.campaigns;

    select count(*)
    into canonical_count
    from silomath.inventory_campaigns;

    if canonical_count < legacy_count then
        raise exception
            'V6 migration verification failed: canonical campaign count % is lower than legacy count %',
            canonical_count,
            legacy_count;
    end if;
end;
$$;

insert into silomath.campaign_scopes (
    campaign_scope_id,
    campaign_id,
    storage_space_id,
    created_at,
    created_by
)
select
    gen_random_uuid(),
    cs.campaign_id,
    cs.storage_space_id,
    c.launched_at,
    c.launched_by
from public.campaign_scope cs
join public.campaigns c
  on c.id = cs.campaign_id
where not exists (
    select 1
    from silomath.campaign_scopes target
    where target.campaign_id = cs.campaign_id
      and target.storage_space_id = cs.storage_space_id
);

do $$
declare
    legacy_count integer;
    canonical_count integer;
begin
    select count(*)
    into legacy_count
    from public.campaign_scope;

    select count(*)
    into canonical_count
    from silomath.campaign_scopes;

    if canonical_count < legacy_count then
        raise exception
            'V6 migration verification failed: canonical campaign scope count % is lower than legacy count %',
            canonical_count,
            legacy_count;
    end if;
end;
$$;

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
    ca.campaign_id,
    ca.user_id,
    'ACTIVE',
    c.launched_at,
    c.launched_by,
    null,
    null,
    c.launched_at,
    c.launched_by,
    c.launched_at,
    c.launched_by,
    1
from public.campaign_assignments ca
join public.campaigns c
  on c.id = ca.campaign_id
where not exists (
    select 1
    from silomath.campaign_team_assignments target
    where target.campaign_id = ca.campaign_id
      and target.user_id = ca.user_id
      and target.status = 'ACTIVE'
);

do $$
declare
    legacy_count integer;
    canonical_count integer;
begin
    select count(*)
    into legacy_count
    from public.campaign_assignments;

    select count(*)
    into canonical_count
    from silomath.campaign_team_assignments
    where status = 'ACTIVE';

    if canonical_count < legacy_count then
        raise exception
            'V6 migration verification failed: canonical active team count % is lower than legacy assignment count %',
            canonical_count,
            legacy_count;
    end if;
end;
$$;

do $$
declare
    mismatch_count integer;
begin
    select count(*)
    into mismatch_count
    from public.campaigns legacy
    join silomath.inventory_campaigns canonical
      on canonical.campaign_id = legacy.id
    where canonical.organization_id <> legacy.organization_id
       or canonical.name <> legacy.name
       or canonical.code <> legacy.code
       or canonical.start_date <> legacy.start_date
       or canonical.planned_end_date <> legacy.planned_end_date
       or canonical.inventory_type <>
            case legacy.inventory_type
                when 'Annual' then 'ANNUAL'
                when 'Monthly' then 'MONTHLY'
                when 'Control' then 'CONTROL'
            end
       or canonical.status <>
            case legacy.status
                when 'Active' then 'ACTIVE'
                when 'Completed' then 'COMPLETED'
            end;

    if mismatch_count > 0 then
        raise exception
            'V6 migration verification failed: % canonical campaigns differ from legacy source values',
            mismatch_count;
    end if;
end;
$$;
