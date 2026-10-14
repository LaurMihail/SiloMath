-- Create commodities table (global shared catalog)
create table public.commodities (
    id uuid primary key default gen_random_uuid(),
    code text not null unique,
    name text not null,
    reference_density numeric null check (reference_density is null or reference_density >= 0),
    density_unit text not null,
    min_density numeric null check (min_density is null or min_density >= 0),
    max_density numeric null check (max_density is null or max_density >= 0),
    validation_parameters jsonb null,
    lifecycle_status text not null default 'ACTIVE' check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
    revision bigint not null default 1 check (revision >= 1),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (
        min_density is null
        or max_density is null
        or min_density <= max_density
    )
);

-- Enable RLS on commodities
alter table public.commodities enable row level security;

-- Policy: Authenticated users can select commodities
create policy "Authenticated users can select commodities"
    on public.commodities
    for select
    to authenticated
    using (true);


-- Create clients table (organization-scoped)
create table public.clients (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations(id) on delete restrict,
    name text not null,
    lifecycle_status text not null default 'ACTIVE' check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
    revision bigint not null default 1 check (revision >= 1),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Enable RLS on clients
alter table public.clients enable row level security;

-- Policy: Organization members can select clients
create policy "Organization members can select clients"
    on public.clients
    for select
    to authenticated
    using (public.is_organization_member(organization_id));


-- Register update triggers for sync metadata
create trigger update_commodities_sync_metadata
    before update on public.commodities
    for each row
    execute function public.update_master_data_sync_metadata();

create trigger update_clients_sync_metadata
    before update on public.clients
    for each row
    execute function public.update_master_data_sync_metadata();
