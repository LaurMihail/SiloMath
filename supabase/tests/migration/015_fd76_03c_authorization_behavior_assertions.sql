-- FD76-03C behavioral assertions for canonical authorization helpers.

begin;

do $$
declare
    v_admin uuid := '71000000-0000-0000-0000-000000000001';
    v_standard uuid := '71000000-0000-0000-0000-000000000002';
    v_disabled_user uuid := '71000000-0000-0000-0000-000000000003';
    v_org_active uuid := '72000000-0000-0000-0000-000000000001';
    v_org_other uuid := '72000000-0000-0000-0000-000000000002';
    v_org_inactive uuid := '72000000-0000-0000-0000-000000000003';
begin
    insert into silomath.users (
        user_id, full_name, email, email_normalized, lifecycle_status,
        created_at, created_by, updated_at, updated_by, entity_version
    ) values
        (v_admin, 'FD76 Admin', 'fd76-admin@example.invalid', 'fd76-admin@example.invalid', 'ACTIVE', now(), null, now(), v_admin, 1),
        (v_standard, 'FD76 Standard', 'fd76-standard@example.invalid', 'fd76-standard@example.invalid', 'ACTIVE', now(), null, now(), v_standard, 1),
        (v_disabled_user, 'FD76 Disabled', 'fd76-disabled@example.invalid', 'fd76-disabled@example.invalid', 'DISABLED', now(), null, now(), v_disabled_user, 1);

    insert into silomath.organizations (
        organization_id, name, code, address, city, country, lifecycle_status,
        created_at, created_by, updated_at, updated_by, entity_version
    ) values
        (v_org_active, 'FD76 Active Org', 'FD76-ACTIVE', null, null, null, 'ACTIVE', now(), v_admin, now(), v_admin, 1),
        (v_org_other, 'FD76 Other Org', 'FD76-OTHER', null, null, null, 'ACTIVE', now(), v_admin, now(), v_admin, 1),
        (v_org_inactive, 'FD76 Inactive Org', 'FD76-INACTIVE', null, null, null, 'INACTIVE', now(), v_admin, now(), v_admin, 1);

    insert into silomath.organization_memberships (
        membership_id, organization_id, user_id, role, lifecycle_status,
        disabled_at, disabled_by,
        created_at, created_by, updated_at, updated_by, entity_version
    ) values
        ('73000000-0000-0000-0000-000000000001', v_org_active, v_admin, 'ADMIN', 'ACTIVE', null, null, now(), v_admin, now(), v_admin, 1),
        ('73000000-0000-0000-0000-000000000002', v_org_active, v_standard, 'STANDARD', 'ACTIVE', null, null, now(), v_admin, now(), v_admin, 1),
        ('73000000-0000-0000-0000-000000000003', v_org_active, v_disabled_user, 'ADMIN', 'ACTIVE', null, null, now(), v_admin, now(), v_admin, 1),
        ('73000000-0000-0000-0000-000000000004', v_org_other, v_admin, 'ADMIN', 'DISABLED', now(), v_admin, now(), v_admin, now(), v_admin, 1),
        ('73000000-0000-0000-0000-000000000005', v_org_inactive, v_admin, 'ADMIN', 'ACTIVE', null, null, now(), v_admin, now(), v_admin, 1);

    -- ACTIVE Admin in ACTIVE org.
    perform set_config('request.jwt.claim.sub', v_admin::text, true);
    perform set_config('request.jwt.claims', json_build_object('sub', v_admin::text, 'role', 'authenticated')::text, true);

    if not silomath.is_active_user() then
        raise exception 'FD76-03C active user unexpectedly rejected';
    end if;

    if not silomath.has_active_organization_membership(v_org_active) then
        raise exception 'FD76-03C active membership unexpectedly rejected';
    end if;

    if not silomath.is_active_organization_admin(v_org_active) then
        raise exception 'FD76-03C active admin unexpectedly rejected';
    end if;

    -- Cross-tenant / disabled membership must fail.
    if silomath.has_active_organization_membership(v_org_other) then
        raise exception 'FD76-03C disabled/cross-tenant membership unexpectedly allowed';
    end if;

    -- Inactive organization must fail.
    if silomath.has_active_organization_membership(v_org_inactive) then
        raise exception 'FD76-03C inactive organization unexpectedly allowed';
    end if;

    -- STANDARD user is a member but not Admin.
    perform set_config('request.jwt.claim.sub', v_standard::text, true);
    perform set_config('request.jwt.claims', json_build_object('sub', v_standard::text, 'role', 'authenticated')::text, true);

    if not silomath.has_active_organization_membership(v_org_active) then
        raise exception 'FD76-03C standard member unexpectedly rejected';
    end if;

    if silomath.is_active_organization_admin(v_org_active) then
        raise exception 'FD76-03C standard member unexpectedly authorized as admin';
    end if;

    -- Disabled user must fail even with an ACTIVE membership row.
    perform set_config('request.jwt.claim.sub', v_disabled_user::text, true);
    perform set_config('request.jwt.claims', json_build_object('sub', v_disabled_user::text, 'role', 'authenticated')::text, true);

    if silomath.is_active_user() then
        raise exception 'FD76-03C disabled user unexpectedly active';
    end if;

    if silomath.has_active_organization_membership(v_org_active) then
        raise exception 'FD76-03C disabled user unexpectedly authorized';
    end if;

    if silomath.is_active_organization_admin(v_org_active) then
        raise exception 'FD76-03C disabled user unexpectedly authorized as admin';
    end if;
end;
$$;

rollback;
