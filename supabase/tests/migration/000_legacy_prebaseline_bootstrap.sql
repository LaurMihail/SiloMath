-- SiloMath PostgreSQL migration regression test support.
--
-- GAP-PG-MIG-003:
-- The tracked legacy Supabase migration history is not independently
-- replayable from an empty database because public.organizations and
-- public.organization_members predate the earliest retained migration.
--
-- This file is NOT a historical Production migration and MUST NOT be
-- deployed as part of the application migration chain.
--
-- It reconstructs only the minimum proven pre-migration legacy baseline
-- required to replay the retained 202609 migration history in an isolated
-- migration test database.
--
-- Source evidence:
-- - retained migrations reference public.organizations and
--   public.organization_members before either table is created;
-- - legacy functions insert organization_id/user_id/role into
--   public.organization_members;
-- - the representative legacy fixture inserts id/name into organizations;
-- - later legacy metadata migration expects organizations.updated_at to
--   already exist because its trigger writes NEW.updated_at.
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

comment on table public.organizations is
    'TEST-ONLY reconstruction of missing pre-202609 legacy baseline for PostgreSQL migration rehearsal.';

comment on table public.organization_members is
    'TEST-ONLY reconstruction of missing pre-202609 legacy baseline for PostgreSQL migration rehearsal.';
