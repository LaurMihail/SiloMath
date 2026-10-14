-- 1. Create public.campaigns table
create table public.campaigns (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations(id) on delete restrict,
    name text not null,
    status text not null check (status in ('Active', 'Completed')),
    revision bigint not null default 1 check (revision >= 1),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Trigger for campaigns update metadata
create trigger update_campaigns_sync_metadata
    before update on public.campaigns
    for each row
    execute function public.update_master_data_sync_metadata();

-- Enable RLS on campaigns
alter table public.campaigns enable row level security;

-- Policy: Organization members can select campaigns
create policy "Organization members can select campaigns"
    on public.campaigns
    for select
    to authenticated
    using (public.is_organization_member(organization_id));


-- 2. Create public.campaign_scope table
create table public.campaign_scope (
    id uuid primary key default gen_random_uuid(),
    campaign_id uuid not null references public.campaigns(id) on delete restrict,
    storage_space_id uuid not null references public.storage_spaces(id) on delete restrict,
    geometry_version_id uuid not null references public.geometry_versions(id) on delete restrict,
    unique (campaign_id, storage_space_id)
);

-- Enable RLS on campaign_scope
alter table public.campaign_scope enable row level security;

-- Policy: Organization members can select campaign_scope
create policy "Organization members can select campaign scope"
    on public.campaign_scope
    for select
    to authenticated
    using (
        exists (
            select 1 from public.campaigns c
            where c.id = campaign_scope.campaign_id
              and public.is_organization_member(c.organization_id)
        )
    );


-- 3. Create public.campaign_assignments table
create table public.campaign_assignments (
    id uuid primary key default gen_random_uuid(),
    campaign_id uuid not null references public.campaigns(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete restrict,
    role_at_assignment text not null check (role_at_assignment in ('Admin', 'Standard User')),
    unique (campaign_id, user_id)
);

-- Enable RLS on campaign_assignments
alter table public.campaign_assignments enable row level security;

-- Policy: Organization members can select campaign_assignments
create policy "Organization members can select campaign assignments"
    on public.campaign_assignments
    for select
    to authenticated
    using (
        exists (
            select 1 from public.campaigns c
            where c.id = campaign_assignments.campaign_id
              and public.is_organization_member(c.organization_id)
        )
    );


-- 4. Set exact table privileges
revoke all privileges on table public.campaigns from anon, authenticated;
revoke all privileges on table public.campaign_scope from anon, authenticated;
revoke all privileges on table public.campaign_assignments from anon, authenticated;

grant select on table public.campaigns to authenticated;
grant select on table public.campaign_scope to authenticated;
grant select on table public.campaign_assignments to authenticated;
