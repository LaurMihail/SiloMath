-- 1. Precheck existing stocktaking_records data for Third-Party organization consistency
do $$
declare
    invalid_count integer;
begin
    select count(*) into invalid_count
    from public.stocktaking_records sr
    join public.campaigns c on c.id = sr.campaign_id
    left join public.clients cl on cl.id = sr.client_id
    where sr.ownership = 'Third-Party'
      and (sr.client_id is null or cl.organization_id <> c.organization_id);

    if invalid_count > 0 then
        raise exception 'Migration failed: % Third-Party stocktaking_records have missing or cross-organization client_id', invalid_count;
    end if;
end;
$$;

-- 2. Create function to enforce Third-Party client organization consistency
create or replace function public.enforce_third_party_client_org_consistency()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    camp_org_id uuid;
    client_org_id uuid;
begin
    if new.ownership = 'Third-Party' then
        if new.client_id is null then
            raise exception 'Third-Party stocktaking record requires client_id';
        end if;

        select organization_id into camp_org_id
        from public.campaigns
        where id = new.campaign_id;

        select organization_id into client_org_id
        from public.clients
        where id = new.client_id;

        if camp_org_id is null or client_org_id is null or camp_org_id <> client_org_id then
            raise exception 'Third-Party client organization mismatch: client organization does not match campaign organization';
        end if;
    end if;

    return new;
end;
$$;

create trigger enforce_third_party_client_org_consistency_trigger
    before insert or update of ownership, client_id, campaign_id on public.stocktaking_records
    for each row
    execute function public.enforce_third_party_client_org_consistency();


-- 3. Protect campaigns.organization_id UPDATE against invalidating Third-Party relations
create or replace function public.prevent_third_party_org_mismatch_on_campaign_update()
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
        from public.stocktaking_records sr
        join public.clients cl on cl.id = sr.client_id
        where sr.campaign_id = new.id
          and sr.ownership = 'Third-Party'
          and cl.organization_id <> new.organization_id;

        if invalid_count > 0 then
            raise exception 'Cannot update campaign organization: violates Third-Party stocktaking client organization consistency';
        end if;
    end if;
    return new;
end;
$$;

create trigger prevent_third_party_campaign_org_mismatch_trigger
    before update of organization_id on public.campaigns
    for each row
    execute function public.prevent_third_party_org_mismatch_on_campaign_update();


-- 4. Protect clients.organization_id UPDATE against invalidating Third-Party relations
create or replace function public.prevent_third_party_client_org_mismatch_on_client_update()
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
        from public.stocktaking_records sr
        join public.campaigns c on c.id = sr.campaign_id
        where sr.client_id = new.id
          and sr.ownership = 'Third-Party'
          and c.organization_id <> new.organization_id;

        if invalid_count > 0 then
            raise exception 'Cannot update client organization: violates Third-Party stocktaking client organization consistency';
        end if;
    end if;
    return new;
end;
$$;

create trigger prevent_third_party_client_org_mismatch_trigger
    before update of organization_id on public.clients
    for each row
    execute function public.prevent_third_party_client_org_mismatch_on_client_update();
