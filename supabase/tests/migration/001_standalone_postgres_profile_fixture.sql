-- Native PostgreSQL-only synthetic legacy profile fixture.
-- Required because FD76-03B1 reconciles public.profiles with auth.users.
-- This file is not a Production migration and is not a GoTrue behavior test.
insert into public.profiles (id, display_name, created_at, updated_at)
select
    u.id,
    'Native Migration Fixture User',
    coalesce(u.created_at, now()),
    coalesce(u.updated_at, u.created_at, now())
from auth.users u
on conflict (id) do nothing;
