create table public.geometry_families (
    id uuid primary key default gen_random_uuid(),
    name text not null unique
);

create table public.geometry_profiles (
    id uuid primary key default gen_random_uuid(),
    geometry_family_id uuid not null
        references public.geometry_families(id)
        on delete restrict,
    profile_code text not null unique,
    name text not null
);

create table public.geometry_versions (
    id uuid primary key default gen_random_uuid(),
    geometry_profile_id uuid not null
        references public.geometry_profiles(id)
        on delete restrict,
    version_number integer not null
        check (version_number > 0),
    fixed_parameters jsonb not null,
    required_dimensions_schema jsonb not null,
    measurement_schema jsonb not null,
    supported_units jsonb not null,
    calculation_method text not null,
    validation_constraints jsonb not null,
    capacity_rules jsonb,
    unique (geometry_profile_id, version_number)
);

alter table public.geometry_families enable row level security;
alter table public.geometry_profiles enable row level security;
alter table public.geometry_versions enable row level security;

create policy "Authenticated users can select geometry families"
on public.geometry_families
for select
to authenticated
using (true);

create policy "Authenticated users can select geometry profiles"
on public.geometry_profiles
for select
to authenticated
using (true);

create policy "Authenticated users can select geometry versions"
on public.geometry_versions
for select
to authenticated
using (true);

grant select on public.geometry_families to authenticated;
grant select on public.geometry_profiles to authenticated;
grant select on public.geometry_versions to authenticated;

create or replace function public.prevent_geometry_version_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'Geometry versions are immutable and append-only';
end;
$$;

create trigger prevent_geometry_version_update_delete
before update or delete on public.geometry_versions
for each row
execute function public.prevent_geometry_version_mutation();
