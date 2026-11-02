\pset pager off
\pset format unaligned
\pset tuples_only on

do $$
begin
    if to_regclass('public.organization_invitations') is not null then
        raise exception 'Legacy invitation table remains present';
    end if;

    if to_regprocedure(
        'public.finalize_existing_user_invitation(uuid,uuid,uuid,text)'
    ) is not null then
        raise exception 'Legacy invitation finalization function remains present';
    end if;

    if to_regprocedure(
        'public.create_existing_user_membership_invitation(uuid,uuid,text,text,text,uuid)'
    ) is not null then
        raise exception 'Legacy existing-user invitation function remains present';
    end if;

    if to_regprocedure(
        'public.accept_new_user_invitation(uuid,uuid)'
    ) is not null then
        raise exception 'Legacy invitation acceptance function remains present';
    end if;

    if to_regprocedure('public.get_auth_user_id_by_email(text)') is not null then
        raise exception 'Legacy Auth email lookup function remains present';
    end if;

    if to_regprocedure(
        'silomath.organization_membership_patch_command(uuid,uuid,uuid,text,text,bigint,text,text,text)'
    ) is null then
        raise exception 'Canonical OrganizationMembership command is missing';
    end if;

    raise notice 'FD76 INVITATION RETIREMENT ASSERTIONS PASS';
end
$$;