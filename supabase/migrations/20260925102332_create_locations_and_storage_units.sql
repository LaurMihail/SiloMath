create table public.locations (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null
        references public.organizations(id)
        on delete restrict,
    code text not null,
    name text not null,
    unique (organization_id, code)
);

create table public.storage_units (
    id uuid primary key default gen_random_uuid(),
    location_id uuid not null
        references public.locations(id)
        on delete restrict,
    code text not null,
    name text,
    type text not null default 'SILO'
        check (type = 'SILO'),
    unique (location_id, code)
);

alter table public.locations enable row level security;
alter table public.storage_units enable row level security;

create policy "Members can select locations"
on public.locations
for select
to authenticated
using (
    public.is_organization_member(organization_id)
);

create policy "Members can select storage units"
on public.storage_units
for select
to authenticated
using (
    exists (
        select 1
        from public.locations l
        where l.id = storage_units.location_id
          and public.is_organization_member(l.organization_id)
    )
);

grant select on public.locations to authenticated;
grant select on public.storage_units to authenticated;
