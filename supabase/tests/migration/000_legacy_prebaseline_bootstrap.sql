-- SiloMath PostgreSQL migration regression test support.
--
-- GAP-PG-MIG-003:
-- The tracked legacy Supabase migration history is not independently
-- replayable from an empty database because foundational tenant objects
-- predate the earliest retained migration.
--
-- This file is NOT a historical Production migration and MUST NOT be
-- deployed as part of the application migration chain.
--
-- It reconstructs only the minimum proven pre-migration legacy baseline
-- required to replay the retained 202609 migration history in an isolated
-- migration test database.
--
-- Proven missing prebaseline objects:
--   public.organizations
--   public.organization_members
--   public.is_organization_member(uuid)
--
-- No V6 canonical silomath.* structure is introduced here.

create table if not exists public.organizations (
    id uuid primary key,
    name text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.organization_members (
    organization_id uuid not null
        references public.organizations(id)
        on delete cascade,

    user_id uuid not null
        references auth.users(id)
        on delete cascade,

    role text not null
        check (role in ('Admin', 'Standard')),

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    primary key (organization_id, user_id)
);

create or replace function public.is_organization_member(
    p_organization_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.organization_members om
        where om.organization_id = p_organization_id
          and om.user_id = auth.uid()
    );
$$;

revoke all
on function public.is_organization_member(uuid)
from public;

grant execute
on function public.is_organization_member(uuid)
to authenticated;

comment on table public.organizations is
    'TEST-ONLY reconstruction of missing pre-202609 legacy baseline for PostgreSQL migration rehearsal.';

comment on table public.organization_members is
    'TEST-ONLY reconstruction of missing pre-202609 legacy baseline for PostgreSQL migration rehearsal.';

comment on function public.is_organization_member(uuid) is
    'TEST-ONLY reconstruction of missing pre-202609 legacy authorization helper for PostgreSQL migration rehearsal.';
