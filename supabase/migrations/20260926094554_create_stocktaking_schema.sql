-- 1. Create public.stocktaking_records
create table public.stocktaking_records (
    id uuid primary key default gen_random_uuid(),
    campaign_id uuid not null references public.campaigns(id) on delete restrict,
    storage_space_id uuid not null references public.storage_spaces(id) on delete restrict,
    commodity_id uuid null references public.commodities(id) on delete restrict,
    density numeric null check (density is null or density >= 0),
    ownership text not null check (ownership in ('Owned', 'Third-Party')),
    client_id uuid null references public.clients(id) on delete restrict,
    status text not null check (status in ('To Do', 'Issues', 'Validated')),
    revision bigint not null default 1 check (revision >= 1),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (campaign_id, storage_space_id),
    check (ownership <> 'Third-Party' or client_id is not null)
);

-- Trigger for stocktaking_records update metadata
create trigger update_stocktaking_records_sync_metadata
    before update on public.stocktaking_records
    for each row
    execute function public.update_master_data_sync_metadata();

-- Enable RLS on stocktaking_records
alter table public.stocktaking_records enable row level security;

-- Policy: Select stocktaking_records if organization member
create policy "Organization members can select stocktaking records"
    on public.stocktaking_records
    for select
    to authenticated
    using (
        exists (
            select 1 from public.campaigns c
            where c.id = stocktaking_records.campaign_id
              and public.is_organization_member(c.organization_id)
        )
    );


-- 2. Create public.measurements
create table public.measurements (
    id uuid primary key default gen_random_uuid(),
    stocktaking_record_id uuid not null references public.stocktaking_records(id) on delete cascade,
    value numeric not null,
    measured_at timestamptz not null default now()
);

create index index_measurements_stocktaking_record_id on public.measurements (stocktaking_record_id);

-- Enforce max 5 measurements per record trigger/function
create or replace function public.enforce_max_measurements_per_record()
returns trigger
language plpgsql
as $$
declare
    current_count integer;
begin
    select count(*) into current_count
    from public.measurements
    where stocktaking_record_id = new.stocktaking_record_id;

    if current_count >= 5 then
        raise exception 'Maximum 5 measurements per stocktaking record exceeded';
    end if;

    return new;
end;
$$;

create trigger enforce_max_measurements_trigger
    before insert on public.measurements
    for each row
    execute function public.enforce_max_measurements_per_record();

-- Enable RLS on measurements
alter table public.measurements enable row level security;

-- Policy: Select measurements if organization member
create policy "Organization members can select measurements"
    on public.measurements
    for select
    to authenticated
    using (
        exists (
            select 1 from public.stocktaking_records sr
            join public.campaigns c on c.id = sr.campaign_id
            where sr.id = measurements.stocktaking_record_id
              and public.is_organization_member(c.organization_id)
        )
    );


-- 3. Create public.calculation_results
create table public.calculation_results (
    id uuid primary key default gen_random_uuid(),
    stocktaking_record_id uuid not null references public.stocktaking_records(id) on delete restrict,
    volume numeric not null check (volume >= 0),
    mass numeric not null check (mass >= 0),
    engine_version text not null,
    metadata_json jsonb not null,
    unique (stocktaking_record_id)
);

-- Enable RLS on calculation_results
alter table public.calculation_results enable row level security;

-- Policy: Select calculation_results if organization member
create policy "Organization members can select calculation results"
    on public.calculation_results
    for select
    to authenticated
    using (
        exists (
            select 1 from public.stocktaking_records sr
            join public.campaigns c on c.id = sr.campaign_id
            where sr.id = calculation_results.stocktaking_record_id
              and public.is_organization_member(c.organization_id)
        )
    );


-- 4. Create public.issues
create table public.issues (
    id uuid primary key default gen_random_uuid(),
    stocktaking_record_id uuid not null references public.stocktaking_records(id) on delete restrict,
    description text not null,
    reported_at timestamptz not null default now()
);

create index index_issues_stocktaking_record_id on public.issues (stocktaking_record_id);

-- Enable RLS on issues
alter table public.issues enable row level security;

-- Policy: Select issues if organization member
create policy "Organization members can select issues"
    on public.issues
    for select
    to authenticated
    using (
        exists (
            select 1 from public.stocktaking_records sr
            join public.campaigns c on c.id = sr.campaign_id
            where sr.id = issues.stocktaking_record_id
              and public.is_organization_member(c.organization_id)
        )
    );


-- 5. Set exact table privileges
revoke all privileges on table public.stocktaking_records from anon, authenticated;
revoke all privileges on table public.measurements from anon, authenticated;
revoke all privileges on table public.calculation_results from anon, authenticated;
revoke all privileges on table public.issues from anon, authenticated;

grant select on table public.stocktaking_records to authenticated;
grant select on table public.measurements to authenticated;
grant select on table public.calculation_results to authenticated;
grant select on table public.issues to authenticated;
