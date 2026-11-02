-- FD76-03B1 assertions for canonical user identity reconciliation.

do $$
begin
    -- Every legacy profile must have a canonical user.
    if exists (
        select 1
        from public.profiles p
        left join silomath.users su
          on su.user_id = p.id
        where su.user_id is null
    ) then
        raise exception 'FD76-03B1 legacy profile missing canonical user';
    end if;

    -- Canonical identity must preserve the authenticated user ID.
    if exists (
        select 1
        from public.profiles p
        join auth.users au
          on au.id = p.id
        join silomath.users su
          on su.user_id = p.id
        where su.user_id <> au.id
    ) then
        raise exception 'FD76-03B1 canonical user identity mismatch';
    end if;

    -- Email must come from Supabase Auth and be normalized deterministically.
    if exists (
        select 1
        from public.profiles p
        join auth.users au
          on au.id = p.id
        join silomath.users su
          on su.user_id = p.id
        where su.email <> btrim(au.email)
           or su.email_normalized <> lower(btrim(au.email))
    ) then
        raise exception 'FD76-03B1 canonical email provenance mismatch';
    end if;

    -- Display name must preserve the legacy profile value.
    if exists (
        select 1
        from public.profiles p
        join silomath.users su
          on su.user_id = p.id
        where su.full_name <> btrim(p.display_name)
    ) then
        raise exception 'FD76-03B1 canonical full_name provenance mismatch';
    end if;

    -- Reconciled users must be ACTIVE and start at entity_version 1.
    if exists (
        select 1
        from public.profiles p
        join silomath.users su
          on su.user_id = p.id
        where su.lifecycle_status <> 'ACTIVE'
           or su.entity_version <> 1
    ) then
        raise exception 'FD76-03B1 canonical lifecycle/entity version mismatch';
    end if;

    -- Legacy profile records must remain retained during EXPAND/MIGRATE.
    if not exists (
        select 1
        from public.profiles
    ) then
        raise exception 'FD76-03B1 legacy profile provenance lost';
    end if;
end;
$$;
