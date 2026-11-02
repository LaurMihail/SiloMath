-- FD76 Supabase-only final parity: membership mutation behavior and privilege assertions.

begin;

do $$
declare
    v_admin uuid := '76000000-0000-0000-0000-000000000001';
    v_member uuid := '76000000-0000-0000-0000-000000000002';
    v_standard uuid := '76000000-0000-0000-0000-000000000003';
    v_org uuid := '76000000-0000-0000-0000-000000000010';
    v_other_org uuid := '76000000-0000-0000-0000-000000000011';
    v_admin_membership uuid := '76000000-0000-0000-0000-000000000020';
    v_member_membership uuid := '76000000-0000-0000-0000-000000000021';
    v_standard_membership uuid := '76000000-0000-0000-0000-000000000022';
    v_result jsonb;
    v_replay jsonb;
    v_batch_result jsonb;
    v_batch_replay jsonb;
    v_failed boolean;
begin
    if has_function_privilege('anon', 'silomath.organization_membership_patch_command(uuid,uuid,uuid,text,text,bigint,text,text,text)', 'EXECUTE')
       or has_function_privilege('authenticated', 'silomath.organization_membership_patch_command(uuid,uuid,uuid,text,text,bigint,text,text,text)', 'EXECUTE')
       or has_function_privilege('service_role', 'silomath.organization_membership_patch_command(uuid,uuid,uuid,text,text,bigint,text,text,text)', 'EXECUTE') then
        raise exception 'membership command must remain postgres-only';
    end if;

    if not has_function_privilege('postgres', 'silomath.organization_membership_patch_command(uuid,uuid,uuid,text,text,bigint,text,text,text)', 'EXECUTE') then
        raise exception 'postgres must execute membership command';
    end if;

    if has_function_privilege('anon', 'silomath.storage_space_batch_create_items_command(uuid,uuid,uuid,text,text,jsonb,text)', 'EXECUTE')
       or has_function_privilege('authenticated', 'silomath.storage_space_batch_create_items_command(uuid,uuid,uuid,text,text,jsonb,text)', 'EXECUTE')
       or has_function_privilege('service_role', 'silomath.storage_space_batch_create_items_command(uuid,uuid,uuid,text,text,jsonb,text)', 'EXECUTE') then
        raise exception 'storage space batch command must remain postgres-only';
    end if;

    insert into silomath.users (
        user_id, full_name, email, email_normalized, lifecycle_status,
        created_at, created_by, updated_at, updated_by, entity_version
    ) values
        (v_admin, 'Parity Admin', 'parity-admin@example.invalid', 'parity-admin@example.invalid', 'ACTIVE', now(), null, now(), v_admin, 1),
        (v_member, 'Parity Member', 'parity-member@example.invalid', 'parity-member@example.invalid', 'ACTIVE', now(), null, now(), v_admin, 1),
        (v_standard, 'Parity Standard', 'parity-standard@example.invalid', 'parity-standard@example.invalid', 'ACTIVE', now(), null, now(), v_admin, 1);

    insert into silomath.organizations (
        organization_id, name, code, address, city, country, lifecycle_status,
        created_at, created_by, updated_at, updated_by, entity_version
    ) values
        (v_org, 'Parity Organization', 'PARITY', null, null, null, 'ACTIVE', now(), v_admin, now(), v_admin, 1),
        (v_other_org, 'Other Organization', 'PARITY-OTHER', null, null, null, 'ACTIVE', now(), v_admin, now(), v_admin, 1);

    insert into silomath.organization_memberships (
        membership_id, organization_id, user_id, role, lifecycle_status,
        disabled_at, disabled_by, created_at, created_by, updated_at, updated_by, entity_version
    ) values
        (v_admin_membership, v_org, v_admin, 'ADMIN', 'ACTIVE', null, null, now(), v_admin, now(), v_admin, 1),
        (v_member_membership, v_org, v_member, 'STANDARD', 'ACTIVE', null, null, now(), v_admin, now(), v_admin, 1),
        (v_standard_membership, v_org, v_standard, 'STANDARD', 'ACTIVE', null, null, now(), v_admin, now(), v_admin, 1);

    select silomath.organization_membership_patch_command(
        v_admin, v_org, v_member_membership, 'ADMIN', null, 1,
        '76000000-0000-0000-0000-000000000101', 'membership-promote-v1', 'fd76-parity'
    ) into v_result;

    if v_result #>> '{body,role}' <> 'ADMIN'
       or (v_result #>> '{body,entityVersion}')::bigint <> 2 then
        raise exception 'membership patch response or entity version mismatch';
    end if;

    select silomath.organization_membership_patch_command(
        v_admin, v_org, v_member_membership, 'ADMIN', null, 1,
        '76000000-0000-0000-0000-000000000101', 'membership-promote-v1', 'fd76-parity'
    ) into v_replay;

    if v_replay <> v_result then
        raise exception 'membership exact replay mismatch';
    end if;

    v_failed := false;
    begin
        perform silomath.organization_membership_patch_command(
            v_admin, v_org, v_member_membership, 'STANDARD', null, 1,
            '76000000-0000-0000-0000-000000000102', 'membership-stale-v1', 'fd76-parity'
        );
    exception when sqlstate '40001' then
        v_failed := true;
    end;
    if not v_failed then
        raise exception 'stale membership entity version was accepted';
    end if;

    v_failed := false;
    begin
        perform silomath.organization_membership_patch_command(
            v_standard, v_org, v_member_membership, 'STANDARD', null, 2,
            '76000000-0000-0000-0000-000000000103', 'membership-standard-v1', 'fd76-parity'
        );
    exception when sqlstate '42501' then
        v_failed := true;
    end;
    if not v_failed then
        raise exception 'STANDARD membership mutation was accepted';
    end if;

    v_failed := false;
    begin
        perform silomath.organization_membership_patch_command(
            v_admin, v_other_org, v_member_membership, 'STANDARD', null, 2,
            '76000000-0000-0000-0000-000000000104', 'membership-cross-tenant-v1', 'fd76-parity'
        );
    exception when sqlstate '42501' then
        v_failed := true;
    end;
    if not v_failed then
        raise exception 'cross-tenant membership mutation was accepted';
    end if;

    perform silomath.organization_membership_patch_command(
        v_admin, v_org, v_member_membership, 'STANDARD', null, 2,
        '76000000-0000-0000-0000-000000000105', 'membership-demote-v2', 'fd76-parity'
    );

    v_failed := false;
    begin
        perform silomath.organization_membership_patch_command(
            v_admin, v_org, v_admin_membership, 'STANDARD', null, 1,
            '76000000-0000-0000-0000-000000000106', 'membership-last-admin-v1', 'fd76-parity'
        );
    exception when sqlstate '23514' then
        v_failed := true;
    end;
    if not v_failed then
        raise exception 'last active administrator demotion was accepted';
    end if;

    if not exists (
        select 1 from silomath.audit_events
        where entity_type = 'OrganizationMembership'
          and entity_id = v_member_membership
          and action = 'MEMBERSHIP_UPDATE'
    ) then
        raise exception 'membership mutation audit event missing';
    end if;

    if not exists (
        select 1 from silomath.sync_change_stream
        where organization_id = v_org
          and entity_type = 'OrganizationMembership'
          and entity_id = v_member_membership
          and entity_version = 2
    ) then
        raise exception 'membership mutation sync emission missing';
    end if;

    -- The MD003 fixture supplies a canonical Organization -> Location ->
    -- Storage Unit plus a valid Geometry Profile/Version. The batch test
    -- remains synthetic and rolls back with this transaction.
    select silomath.storage_space_batch_create_items_command(
        '90000000-0000-0000-0000-000000000002',
        '90000000-0000-0000-0000-000000000001',
        '90000000-0000-0000-0000-000000000004',
        '76000000-0000-0000-0000-000000000201',
        'batch-valid-v1',
        jsonb_build_array(
            jsonb_build_object(
                'code', 'BATCH-A',
                'description', 'Atomic A',
                'geometryProfileId', '90000000-0000-0000-0000-000000000007',
                'lifecycleStatus', 'ACTIVE'
            ),
            jsonb_build_object(
                'code', 'BATCH-B',
                'description', 'Atomic B',
                'geometryProfileId', '90000000-0000-0000-0000-000000000007',
                'lifecycleStatus', 'INACTIVE'
            )
        ),
        'fd76-parity'
    ) into v_batch_result;

    if jsonb_array_length(v_batch_result #> '{body,items}') <> 2
       or (
           select count(*) from silomath.storage_spaces
           where storage_unit_id = '90000000-0000-0000-0000-000000000004'
             and code in ('BATCH-A', 'BATCH-B')
       ) <> 2 then
        raise exception 'valid storage space batch did not commit atomically';
    end if;

    select silomath.storage_space_batch_create_items_command(
        '90000000-0000-0000-0000-000000000002',
        '90000000-0000-0000-0000-000000000001',
        '90000000-0000-0000-0000-000000000004',
        '76000000-0000-0000-0000-000000000201',
        'batch-valid-v1',
        jsonb_build_array(
            jsonb_build_object(
                'code', 'BATCH-A',
                'description', 'Atomic A',
                'geometryProfileId', '90000000-0000-0000-0000-000000000007',
                'lifecycleStatus', 'ACTIVE'
            ),
            jsonb_build_object(
                'code', 'BATCH-B',
                'description', 'Atomic B',
                'geometryProfileId', '90000000-0000-0000-0000-000000000007',
                'lifecycleStatus', 'INACTIVE'
            )
        ),
        'fd76-parity'
    ) into v_batch_replay;

    if v_batch_replay <> v_batch_result then
        raise exception 'storage space batch exact replay mismatch';
    end if;

    v_failed := false;
    begin
        perform silomath.storage_space_batch_create_items_command(
            '90000000-0000-0000-0000-000000000002',
            '90000000-0000-0000-0000-000000000001',
            '90000000-0000-0000-0000-000000000004',
            '76000000-0000-0000-0000-000000000201',
            'batch-different-v2',
            jsonb_build_array(jsonb_build_object(
                'code', 'BATCH-E',
                'geometryProfileId', '90000000-0000-0000-0000-000000000007'
            )),
            'fd76-parity'
        );
    exception when sqlstate '23505' then
        v_failed := true;
    end;
    if not v_failed then
        raise exception 'storage space batch idempotency mismatch was accepted';
    end if;

    v_failed := false;
    begin
        perform silomath.storage_space_batch_create_items_command(
            '90000000-0000-0000-0000-000000000002',
            '90000000-0000-0000-0000-000000000001',
            '90000000-0000-0000-0000-000000000004',
            '76000000-0000-0000-0000-000000000202',
            'batch-duplicate-v1',
            jsonb_build_array(
                jsonb_build_object(
                    'code', 'BATCH-C',
                    'geometryProfileId', '90000000-0000-0000-0000-000000000007'
                ),
                jsonb_build_object(
                    'code', 'BATCH-C',
                    'geometryProfileId', '90000000-0000-0000-0000-000000000007'
                )
            ),
            'fd76-parity'
        );
    exception when sqlstate '22023' then
        v_failed := true;
    end;
    if not v_failed or exists (
        select 1 from silomath.storage_spaces
        where storage_unit_id = '90000000-0000-0000-0000-000000000004'
          and code = 'BATCH-C'
    ) then
        raise exception 'duplicate storage space batch was not rolled back atomically';
    end if;

    v_failed := false;
    begin
        perform silomath.storage_space_batch_create_items_command(
            v_standard,
            '90000000-0000-0000-0000-000000000001',
            '90000000-0000-0000-0000-000000000004',
            '76000000-0000-0000-0000-000000000203',
            'batch-standard-v1',
            jsonb_build_array(jsonb_build_object(
                'code', 'BATCH-D',
                'geometryProfileId', '90000000-0000-0000-0000-000000000007'
            )),
            'fd76-parity'
        );
    exception when sqlstate '42501' then
        v_failed := true;
    end;
    if not v_failed then
        raise exception 'STANDARD user storage space batch was accepted';
    end if;

    if (
        select count(*) from silomath.audit_events
        where entity_type = 'StorageSpace'
          and action = 'BATCH_CREATE'
          and entity_id in (
              select storage_space_id from silomath.storage_spaces
              where storage_unit_id = '90000000-0000-0000-0000-000000000004'
                and code in ('BATCH-A', 'BATCH-B')
          )
    ) <> 2 then
        raise exception 'storage space batch audit emission mismatch';
    end if;

    if (
        select count(*) from silomath.sync_change_stream
        where organization_id = '90000000-0000-0000-0000-000000000001'
          and entity_type = 'StorageSpace'
          and entity_id in (
              select storage_space_id from silomath.storage_spaces
              where storage_unit_id = '90000000-0000-0000-0000-000000000004'
                and code in ('BATCH-A', 'BATCH-B')
          )
    ) <> 2 then
        raise exception 'storage space batch sync emission mismatch';
    end if;
end;
$$;

rollback;
