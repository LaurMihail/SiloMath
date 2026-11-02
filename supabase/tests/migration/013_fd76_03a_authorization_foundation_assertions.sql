-- FD76-03A structural assertions for canonical authorization foundation.

do $$
declare
    v_def text;
begin
    -- 1. Canonical helpers must exist.
    if to_regprocedure('silomath.is_active_user()') is null then
        raise exception 'FD76-03A missing silomath.is_active_user()';
    end if;

    if to_regprocedure('silomath.has_active_organization_membership(uuid)') is null then
        raise exception 'FD76-03A missing silomath.has_active_organization_membership(uuid)';
    end if;

    if to_regprocedure('silomath.is_active_organization_admin(uuid)') is null then
        raise exception 'FD76-03A missing silomath.is_active_organization_admin(uuid)';
    end if;

    -- 2. All helpers must be SECURITY DEFINER with empty search_path.
    if exists (
        select 1
        from pg_proc p
        join pg_namespace n on n.oid = p.pronamespace
        where n.nspname = 'silomath'
          and p.proname in (
              'is_active_user',
              'has_active_organization_membership',
              'is_active_organization_admin'
          )
          and (
              not p.prosecdef
              or p.proconfig is distinct from array['search_path=""']::text[]
          )
    ) then
        raise exception 'FD76-03A canonical authorization helper security configuration invalid';
    end if;

    -- 3. No client/runtime role may execute these private helpers directly.
    if has_function_privilege('anon', 'silomath.is_active_user()', 'EXECUTE')
       or has_function_privilege('authenticated', 'silomath.is_active_user()', 'EXECUTE')
       or has_function_privilege('service_role', 'silomath.is_active_user()', 'EXECUTE')
       or has_function_privilege('anon', 'silomath.has_active_organization_membership(uuid)', 'EXECUTE')
       or has_function_privilege('authenticated', 'silomath.has_active_organization_membership(uuid)', 'EXECUTE')
       or has_function_privilege('service_role', 'silomath.has_active_organization_membership(uuid)', 'EXECUTE')
       or has_function_privilege('anon', 'silomath.is_active_organization_admin(uuid)', 'EXECUTE')
       or has_function_privilege('authenticated', 'silomath.is_active_organization_admin(uuid)', 'EXECUTE')
       or has_function_privilege('service_role', 'silomath.is_active_organization_admin(uuid)', 'EXECUTE')
    then
        raise exception 'FD76-03A canonical authorization helper exposed to client/runtime role';
    end if;

    -- 4. Helpers must use canonical silomath authorization tables.
    select pg_get_functiondef('silomath.has_active_organization_membership(uuid)'::regprocedure)
      into v_def;

    if v_def not like '%silomath.users%'
       or v_def not like '%silomath.organization_memberships%'
       or v_def not like '%silomath.organizations%'
    then
        raise exception 'FD76-03A membership helper does not use canonical authorization tables';
    end if;

    select pg_get_functiondef('silomath.is_active_organization_admin(uuid)'::regprocedure)
      into v_def;

    if v_def not like '%silomath.users%'
       or v_def not like '%silomath.organization_memberships%'
       or v_def not like '%silomath.organizations%'
       or v_def not like '%om.role = ''ADMIN''%'
    then
        raise exception 'FD76-03A admin helper does not enforce canonical active admin authorization';
    end if;

    -- 5. Legacy membership authority must not be referenced by the new helpers.
    if exists (
        select 1
        from pg_proc p
        join pg_namespace n on n.oid = p.pronamespace
        where n.nspname = 'silomath'
          and p.proname in (
              'is_active_user',
              'has_active_organization_membership',
              'is_active_organization_admin'
          )
          and pg_get_functiondef(p.oid) like '%public.organization_members%'
    ) then
        raise exception 'FD76-03A canonical authorization helper references legacy public.organization_members';
    end if;
end;
$$;
