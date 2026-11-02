-- Test-only infrastructure shim for a NEW isolated PostgreSQL cluster without GoTrue.
-- Not a historical Auth fixture, not a Production migration, not an Auth behavioral test.
-- Columns are exactly those consumed by retained migration fixtures/functions.
do $$ begin
    if not exists(select 1 from pg_roles where rolname='anon') then create role anon; end if;
    if not exists(select 1 from pg_roles where rolname='authenticated') then create role authenticated; end if;
    if not exists(select 1 from pg_roles where rolname='service_role') then create role service_role; end if;
end $$;
create schema extensions;
create extension pgcrypto with schema extensions;
create schema auth;
create table auth.users (
    id uuid primary key, instance_id uuid, aud text, role text, email text,
    encrypted_password text, email_confirmed_at timestamptz, created_at timestamptz, updated_at timestamptz
);
-- Legacy Supabase profile relation consumed by FD76-03B1 reconciliation.
-- This is a structural native-test compatibility object, not a V6 table.
create table public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text,
    created_at timestamptz,
    updated_at timestamptz
);
create function auth.uid() returns uuid language sql stable as $$
    select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid;
$$;
