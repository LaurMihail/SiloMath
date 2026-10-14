-- 1. Precheck existing campaigns count
do $$
declare
    existing_count integer;
begin
    select count(*) into existing_count from public.campaigns;
    if existing_count > 0 then
        raise exception 'Migration failed: % existing campaigns found without required launch fields', existing_count;
    end if;
end;
$$;

-- 2. Create public.campaign_launch_snapshots
create table public.campaign_launch_snapshots (
    id uuid primary key default gen_random_uuid(),
    campaign_id uuid not null references public.campaigns(id) on delete restrict,
    organization_id uuid not null references public.organizations(id) on delete restrict,
    organization_name text not null,
    launched_by_user_id uuid not null references auth.users(id) on delete restrict,
    launched_at timestamptz not null,
    unique (campaign_id),
    unique (id, campaign_id)
);

-- 3. Create public.campaign_launch_snapshot_items
create table public.campaign_launch_snapshot_items (
    id uuid primary key default gen_random_uuid(),
    snapshot_id uuid not null references public.campaign_launch_snapshots(id) on delete cascade,
    location_id uuid not null references public.locations(id) on delete restrict,
    location_code text not null,
    location_name text not null,
    storage_unit_id uuid not null references public.storage_units(id) on delete restrict,
    storage_unit_code text not null,
    storage_unit_name text null,
    storage_unit_type text not null,
    storage_space_id uuid not null references public.storage_spaces(id) on delete restrict,
    storage_space_code text not null,
    geometry_family_id uuid not null references public.geometry_families(id) on delete restrict,
    geometry_family_name text not null,
    geometry_profile_id uuid not null references public.geometry_profiles(id) on delete restrict,
    geometry_profile_code text not null,
    geometry_profile_name text not null,
    geometry_version_id uuid not null references public.geometry_versions(id) on delete restrict,
    configured_dimensions jsonb not null,
    unique (snapshot_id, storage_space_id)
);

-- 4. Create public.campaign_launch_team_snapshot
create table public.campaign_launch_team_snapshot (
    id uuid primary key default gen_random_uuid(),
    snapshot_id uuid not null references public.campaign_launch_snapshots(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete restrict,
    role_at_assignment text not null check (role_at_assignment in ('Admin', 'Standard User')),
    unique (snapshot_id, user_id)
);

-- 5. Extend public.campaigns table
alter table public.campaigns
    add column code text not null,
    add column inventory_type text not null,
    add column start_date date not null,
    add column planned_end_date date not null,
    add column launched_by uuid not null references auth.users(id) on delete restrict,
    add column launched_at timestamptz not null,
    add column launch_snapshot_id uuid null,
    add column idempotency_key text not null,
    add column request_fingerprint text not null,
    add constraint campaigns_name_nonblank_check check (btrim(name) <> ''),
    add constraint campaigns_code_nonblank_check check (btrim(code) <> ''),
    add constraint campaigns_inventory_type_check check (inventory_type in ('Annual', 'Monthly', 'Control', 'Handover / Takeover')),
    add constraint campaigns_date_range_check check (planned_end_date >= start_date),
    add constraint campaigns_org_code_unique unique (organization_id, code),
    add constraint campaigns_org_idempotency_key_unique unique (organization_id, idempotency_key),
    add constraint campaigns_launch_snapshot_id_campaign_id_unique unique (launch_snapshot_id, id),
    add constraint campaigns_same_campaign_snapshot_fkey foreign key (launch_snapshot_id, id) references public.campaign_launch_snapshots(id, campaign_id) on delete restrict;

-- 6. Deferred constraint trigger ensuring Active/Completed campaigns must have launch_snapshot_id at COMMIT
create or replace function public.enforce_active_campaign_launch_snapshot()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.status in ('Active', 'Completed') and new.launch_snapshot_id is null then
        raise exception 'Active or Completed campaign requires a non-null launch_snapshot_id';
    end if;
    return new;
end;
$$;

create constraint trigger enforce_active_campaign_launch_snapshot_trigger
    after insert or update on public.campaigns
    deferrable initially deferred
    for each row
    execute function public.enforce_active_campaign_launch_snapshot();

-- 7. Write-once protection for campaigns.launch_snapshot_id
create or replace function public.prevent_launch_snapshot_id_mutation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if old.launch_snapshot_id is not null and new.launch_snapshot_id is distinct from old.launch_snapshot_id then
        raise exception 'launch_snapshot_id is write-once and cannot be changed or set to null';
    end if;
    return new;
end;
$$;

create trigger prevent_launch_snapshot_id_mutation_trigger
    before update on public.campaigns
    for each row
    execute function public.prevent_launch_snapshot_id_mutation();

-- 8. Immutability protection for campaign_launch_snapshots, items, and team snapshot
create or replace function public.prevent_campaign_launch_snapshots_mutation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    raise exception 'Launch campaign snapshots are immutable';
end;
$$;

create trigger prevent_campaign_launch_snapshots_update_delete
    before update or delete on public.campaign_launch_snapshots
    for each row
    execute function public.prevent_campaign_launch_snapshots_mutation();

create or replace function public.enforce_launch_snapshot_child_mutation_protection()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_campaign_launch_snapshot_id uuid;
begin
    if TG_OP in ('UPDATE', 'DELETE') then
        raise exception 'Launch campaign snapshot items are immutable';
    elsif TG_OP = 'INSERT' then
        select launch_snapshot_id into v_campaign_launch_snapshot_id
        from public.campaigns c
        join public.campaign_launch_snapshots cls on cls.campaign_id = c.id
        where cls.id = new.snapshot_id;

        if v_campaign_launch_snapshot_id is not null then
            raise exception 'Cannot add items or team to launch snapshot after campaign launch finalization';
        end if;
    end if;

    return new;
end;
$$;

create trigger prevent_launch_snapshot_items_mutation_trigger
    before insert or update or delete on public.campaign_launch_snapshot_items
    for each row
    execute function public.enforce_launch_snapshot_child_mutation_protection();

create trigger prevent_launch_team_snapshot_mutation_trigger
    before insert or update or delete on public.campaign_launch_team_snapshot
    for each row
    execute function public.enforce_launch_snapshot_child_mutation_protection();

-- 9. RLS on new launch snapshot tables
alter table public.campaign_launch_snapshots enable row level security;
alter table public.campaign_launch_snapshot_items enable row level security;
alter table public.campaign_launch_team_snapshot enable row level security;

create policy "Organization members can select campaign launch snapshots"
    on public.campaign_launch_snapshots
    for select
    to authenticated
    using (public.is_organization_member(organization_id));

create policy "Organization members can select campaign launch snapshot items"
    on public.campaign_launch_snapshot_items
    for select
    to authenticated
    using (
        exists (
            select 1 from public.campaign_launch_snapshots cls
            where cls.id = campaign_launch_snapshot_items.snapshot_id
              and public.is_organization_member(cls.organization_id)
        )
    );

create policy "Organization members can select campaign launch team snapshot"
    on public.campaign_launch_team_snapshot
    for select
    to authenticated
    using (
        exists (
            select 1 from public.campaign_launch_snapshots cls
            where cls.id = campaign_launch_team_snapshot.snapshot_id
              and public.is_organization_member(cls.organization_id)
        )
    );

-- 10. Table Privileges (SELECT only for authenticated, none for anon)
revoke all privileges on table public.campaign_launch_snapshots from anon, authenticated;
revoke all privileges on table public.campaign_launch_snapshot_items from anon, authenticated;
revoke all privileges on table public.campaign_launch_team_snapshot from anon, authenticated;

grant select on table public.campaign_launch_snapshots to authenticated;
grant select on table public.campaign_launch_snapshot_items to authenticated;
grant select on table public.campaign_launch_team_snapshot to authenticated;
