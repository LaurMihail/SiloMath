alter table public.geometry_versions
add constraint geometry_versions_id_profile_unique
unique (id, geometry_profile_id);

create table public.storage_spaces (
    id uuid primary key default gen_random_uuid(),
    storage_unit_id uuid not null
        references public.storage_units(id)
        on delete restrict,
    code text not null,
    geometry_profile_id uuid not null
        references public.geometry_profiles(id)
        on delete restrict,
    geometry_version_id uuid not null,
    configured_dimensions jsonb not null,
    unique (storage_unit_id, code),
    constraint storage_spaces_geometry_version_profile_fk
        foreign key (geometry_version_id, geometry_profile_id)
        references public.geometry_versions(id, geometry_profile_id)
        on delete restrict
);

alter table public.storage_spaces enable row level security;

create policy "Members can select storage spaces"
on public.storage_spaces
for select
to authenticated
using (
    exists (
        select 1
        from public.storage_units su
        join public.locations l
          on l.id = su.location_id
        where su.id = storage_spaces.storage_unit_id
          and public.is_organization_member(l.organization_id)
    )
);

grant select on public.storage_spaces to authenticated;
