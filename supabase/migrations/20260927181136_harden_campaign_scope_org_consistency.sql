-- 1. Function for campaigns.organization_id UPDATE
create or replace function public.prevent_campaign_org_mismatch_on_campaign_update()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    invalid_count integer;
begin
    if old.organization_id is distinct from new.organization_id then
        select count(*) into invalid_count
        from public.campaign_scope cs
        join public.storage_spaces ss on ss.id = cs.storage_space_id
        join public.storage_units su on su.id = ss.storage_unit_id
        join public.locations l on l.id = su.location_id
        where cs.campaign_id = new.id
          and l.organization_id <> new.organization_id;

        if invalid_count > 0 then
            raise exception 'Cannot update campaign organization: violates campaign_scope organization consistency';
        end if;
    end if;
    return new;
end;
$$;

create trigger prevent_campaign_org_mismatch_trigger
    before update of organization_id on public.campaigns
    for each row
    execute function public.prevent_campaign_org_mismatch_on_campaign_update();


-- 2. Function for locations.organization_id UPDATE
create or replace function public.prevent_campaign_org_mismatch_on_location_update()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    invalid_count integer;
begin
    if old.organization_id is distinct from new.organization_id then
        select count(*) into invalid_count
        from public.campaign_scope cs
        join public.campaigns c on c.id = cs.campaign_id
        join public.storage_spaces ss on ss.id = cs.storage_space_id
        join public.storage_units su on su.id = ss.storage_unit_id
        where su.location_id = new.id
          and c.organization_id <> new.organization_id;

        if invalid_count > 0 then
            raise exception 'Cannot update location organization: violates campaign_scope organization consistency';
        end if;
    end if;
    return new;
end;
$$;

create trigger prevent_location_org_mismatch_trigger
    before update of organization_id on public.locations
    for each row
    execute function public.prevent_campaign_org_mismatch_on_location_update();


-- 3. Function for storage_units.location_id UPDATE
create or replace function public.prevent_campaign_org_mismatch_on_unit_update()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    invalid_count integer;
    new_org_id uuid;
begin
    if old.location_id is distinct from new.location_id then
        select organization_id into new_org_id
        from public.locations
        where id = new.location_id;

        select count(*) into invalid_count
        from public.campaign_scope cs
        join public.campaigns c on c.id = cs.campaign_id
        join public.storage_spaces ss on ss.id = cs.storage_space_id
        where ss.storage_unit_id = new.id
          and c.organization_id <> new_org_id;

        if invalid_count > 0 then
            raise exception 'Cannot update storage_unit location: violates campaign_scope organization consistency';
        end if;
    end if;
    return new;
end;
$$;

create trigger prevent_unit_org_mismatch_trigger
    before update of location_id on public.storage_units
    for each row
    execute function public.prevent_campaign_org_mismatch_on_unit_update();


-- 4. Function for storage_spaces.storage_unit_id UPDATE
create or replace function public.prevent_campaign_org_mismatch_on_space_update()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    invalid_count integer;
    new_org_id uuid;
begin
    if old.storage_unit_id is distinct from new.storage_unit_id then
        select l.organization_id into new_org_id
        from public.storage_units su
        join public.locations l on l.id = su.location_id
        where su.id = new.storage_unit_id;

        select count(*) into invalid_count
        from public.campaign_scope cs
        join public.campaigns c on c.id = cs.campaign_id
        where cs.storage_space_id = new.id
          and c.organization_id <> new_org_id;

        if invalid_count > 0 then
            raise exception 'Cannot update storage_space unit: violates campaign_scope organization consistency';
        end if;
    end if;
    return new;
end;
$$;

create trigger prevent_space_org_mismatch_trigger
    before update of storage_unit_id on public.storage_spaces
    for each row
    execute function public.prevent_campaign_org_mismatch_on_space_update();
