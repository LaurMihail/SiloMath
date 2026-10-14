alter table public.organizations
add column if not exists lifecycle_status text not null default 'ACTIVE' check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
add column if not exists revision bigint not null default 1 check (revision >= 1);

alter table public.locations
add column if not exists lifecycle_status text not null default 'ACTIVE' check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
add column if not exists revision bigint not null default 1 check (revision >= 1),
add column if not exists created_at timestamptz not null default now(),
add column if not exists updated_at timestamptz not null default now();

alter table public.storage_units
add column if not exists lifecycle_status text not null default 'ACTIVE' check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
add column if not exists revision bigint not null default 1 check (revision >= 1),
add column if not exists created_at timestamptz not null default now(),
add column if not exists updated_at timestamptz not null default now();

alter table public.storage_spaces
add column if not exists lifecycle_status text not null default 'ACTIVE' check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
add column if not exists revision bigint not null default 1 check (revision >= 1),
add column if not exists created_at timestamptz not null default now(),
add column if not exists updated_at timestamptz not null default now();

alter table public.geometry_families
add column if not exists lifecycle_status text not null default 'ACTIVE' check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
add column if not exists revision bigint not null default 1 check (revision >= 1),
add column if not exists created_at timestamptz not null default now(),
add column if not exists updated_at timestamptz not null default now();

alter table public.geometry_profiles
add column if not exists lifecycle_status text not null default 'ACTIVE' check (lifecycle_status in ('ACTIVE', 'INACTIVE')),
add column if not exists revision bigint not null default 1 check (revision >= 1),
add column if not exists created_at timestamptz not null default now(),
add column if not exists updated_at timestamptz not null default now();

alter table public.geometry_versions
add column if not exists created_at timestamptz not null default now();

create or replace function public.update_master_data_sync_metadata()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    new.revision = old.revision + 1;
    return new;
end;
$$;

create trigger update_organizations_sync_metadata
before update on public.organizations
for each row
execute function public.update_master_data_sync_metadata();

create trigger update_locations_sync_metadata
before update on public.locations
for each row
execute function public.update_master_data_sync_metadata();

create trigger update_storage_units_sync_metadata
before update on public.storage_units
for each row
execute function public.update_master_data_sync_metadata();

create trigger update_storage_spaces_sync_metadata
before update on public.storage_spaces
for each row
execute function public.update_master_data_sync_metadata();

create trigger update_geometry_families_sync_metadata
before update on public.geometry_families
for each row
execute function public.update_master_data_sync_metadata();

create trigger update_geometry_profiles_sync_metadata
before update on public.geometry_profiles
for each row
execute function public.update_master_data_sync_metadata();
