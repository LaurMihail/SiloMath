-- 1. Create public.inventory_snapshots
create table public.inventory_snapshots (
    id uuid primary key default gen_random_uuid(),
    campaign_id uuid not null references public.campaigns(id) on delete restrict,
    captured_at timestamptz not null default now(),
    metadata_json jsonb not null,
    unique (campaign_id)
);

-- Immutability triggers for inventory_snapshots
create or replace function public.prevent_inventory_snapshots_mutation()
returns trigger
language plpgsql
as $$
begin
    if TG_OP = 'UPDATE' then
        raise exception 'Inventory snapshots are immutable';
    elsif TG_OP = 'DELETE' then
        raise exception 'Inventory snapshots are immutable';
    end if;
    return null;
end;
$$;

create trigger prevent_inventory_snapshots_update_delete
    before update or delete on public.inventory_snapshots
    for each row
    execute function public.prevent_inventory_snapshots_mutation();

-- Enable RLS on inventory_snapshots
alter table public.inventory_snapshots enable row level security;

-- Policy: Select inventory_snapshots if organization member
create policy "Organization members can select inventory snapshots"
    on public.inventory_snapshots
    for select
    to authenticated
    using (
        exists (
            select 1 from public.campaigns c
            where c.id = inventory_snapshots.campaign_id
              and public.is_organization_member(c.organization_id)
        )
    );


-- 2. Create public.inventory_snapshot_items
create table public.inventory_snapshot_items (
    id uuid primary key default gen_random_uuid(),
    snapshot_id uuid not null references public.inventory_snapshots(id) on delete cascade,
    storage_space_code text not null,
    captured_geometry_version_id uuid not null references public.geometry_versions(id) on delete restrict,
    commodity_name text null,
    density numeric null check (density is null or density >= 0),
    ownership text not null check (ownership in ('Owned', 'Third-Party')),
    client_name text null,
    final_volume numeric not null check (final_volume >= 0),
    final_mass numeric not null check (final_mass >= 0),
    engine_version text not null,
    metadata_json jsonb not null
);

create index index_inventory_snapshot_items_snapshot_id on public.inventory_snapshot_items (snapshot_id);

-- Immutability triggers for inventory_snapshot_items
create or replace function public.prevent_inventory_snapshot_items_mutation()
returns trigger
language plpgsql
as $$
begin
    if TG_OP = 'UPDATE' then
        raise exception 'Inventory snapshot items are immutable';
    elsif TG_OP = 'DELETE' then
        raise exception 'Inventory snapshot items are immutable';
    end if;
    return null;
end;
$$;

create trigger prevent_inventory_snapshot_items_update_delete
    before update or delete on public.inventory_snapshot_items
    for each row
    execute function public.prevent_inventory_snapshot_items_mutation();

-- Enable RLS on inventory_snapshot_items
alter table public.inventory_snapshot_items enable row level security;

-- Policy: Select inventory_snapshot_items if organization member
create policy "Organization members can select inventory snapshot items"
    on public.inventory_snapshot_items
    for select
    to authenticated
    using (
        exists (
            select 1 from public.inventory_snapshots s
            join public.campaigns c on c.id = s.campaign_id
            where s.id = inventory_snapshot_items.snapshot_id
              and public.is_organization_member(c.organization_id)
        )
    );


-- 3. Set exact table privileges
revoke all privileges on table public.inventory_snapshots from anon, authenticated;
revoke all privileges on table public.inventory_snapshot_items from anon, authenticated;

grant select on table public.inventory_snapshots to authenticated;
grant select on table public.inventory_snapshot_items to authenticated;
