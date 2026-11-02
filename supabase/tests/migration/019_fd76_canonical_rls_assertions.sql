begin;

do $$
declare
  v_total integer;
  v_rls integer;
  v_direct_grants integer;
  v_client_policies integer;
begin
  select count(*), count(*) filter (where c.relrowsecurity)
  into v_total, v_rls
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
  where n.nspname = 'silomath'
    and c.relkind in ('r', 'p');

  if v_total = 0 or v_rls <> v_total then
    raise exception 'FD76 canonical RLS incomplete: %/% tables enabled', v_rls, v_total;
  end if;

  select count(*) into v_direct_grants
  from information_schema.role_table_grants
  where table_schema = 'silomath'
    and grantee in ('anon', 'authenticated');

  if v_direct_grants <> 0 then
    raise exception 'FD76 direct canonical table grants remain: %', v_direct_grants;
  end if;

  select count(*) into v_client_policies
  from pg_policies
  where schemaname = 'silomath'
    and (
      roles @> array['anon']::name[]
      or roles @> array['authenticated']::name[]
      or roles @> array['public']::name[]
    );

  if v_client_policies <> 0 then
    raise exception 'FD76 direct canonical client RLS policies remain: %', v_client_policies;
  end if;

  raise notice 'FD76 CANONICAL RLS ASSERTIONS PASSED: %/% tables enabled, client grants/policies=0',
    v_rls, v_total;
end;
$$;

rollback;
