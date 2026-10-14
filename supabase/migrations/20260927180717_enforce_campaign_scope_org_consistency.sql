-- 1. Precheck existing data for organization inconsistency in campaign_scope
do $$
declare
    invalid_count integer;
begin
    select count(*) into invalid_count
    from public.campaign_scope cs
    join public.campaigns c on c.id = cs.campaign_id
    join public.storage_spaces ss on ss.id = cs.storage_space_id
    join public.storage_units su on su.id = ss.storage_unit_id
    join public.locations l on l.id = su.location_id
    where c.organization_id <> l.organization_id;

    if invalid_count > 0 then
        raise exception 'Migration failed: % campaign_scope rows link a campaign and storage_space belonging to different organizations', invalid_count;
    end if;
end;
$$;

-- 2. Create function to enforce campaign_scope organization consistency
create or replace function public.enforce_campaign_scope_organization_consistency()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    camp_org_id uuid;
    space_org_id uuid;
begin
    select organization_id into camp_org_id
    from public.campaigns
    where id = new.campaign_id;

    select l.organization_id into space_org_id
    from public.storage_spaces ss
    join public.storage_units su on su.id = ss.storage_unit_id
    join public.locations l on l.id = su.location_id
    where ss.id = new.storage_space_id;

    if camp_org_id is null or space_org_id is null or camp_org_id <> space_org_id then
        raise exception 'Campaign scope organization mismatch: storage_space organization does not match campaign organization';
    end if;

    return new;
end;
$$;

-- 3. Attach trigger to campaign_scope
create trigger enforce_campaign_scope_org_consistency_trigger
    before insert or update of campaign_id, storage_space_id on public.campaign_scope
    for each row
    execute function public.enforce_campaign_scope_organization_consistency();
