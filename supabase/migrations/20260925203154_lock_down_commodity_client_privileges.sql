revoke all privileges on table public.commodities from anon;
revoke all privileges on table public.clients from anon;

revoke all privileges on table public.commodities from authenticated;
revoke all privileges on table public.clients from authenticated;

grant select on table public.commodities to authenticated;
grant select on table public.clients to authenticated;
