-- SiloMath PostgreSQL migration test
-- Representative populated LEGACY fixture.
--
-- This fixture runs after the legacy migration boundary and BEFORE the
-- SiloMath V6 canonical PostgreSQL migrations.
--
-- It deliberately creates a valid legacy Campaign by invoking the
-- historical public.save_and_launch_campaign(...) RPC rather than
-- bypassing its atomic launch/snapshot contract.

do $$
declare
    v_org_id uuid := '11111111-1111-1111-1111-111111111111';
    v_user_id uuid := '22222222-2222-2222-2222-222222222222';

    v_location_id uuid := '33333333-3333-3333-3333-333333333333';
    v_unit_id uuid := '44444444-4444-4444-4444-444444444444';
    v_space_id uuid := '55555555-5555-5555-5555-555555555555';

    v_geometry_family_id uuid := '66666666-6666-6666-6666-666666666661';
    v_geometry_profile_id uuid := '66666666-6666-6666-6666-666666666662';
    v_geometry_version_id uuid := '66666666-6666-6666-6666-666666666663';

    v_campaign_id uuid;
    v_snapshot_id uuid;
    v_reused boolean;

    v_team jsonb;
begin
    --------------------------------------------------------------------
    -- AUTH USER
    --------------------------------------------------------------------

    insert into auth.users (
        id,
        instance_id,
        aud,
        role,
        email,
        encrypted_password,
        email_confirmed_at,
        created_at,
        updated_at
    )
    values (
        v_user_id,
        '00000000-0000-0000-0000-000000000000',
        'authenticated',
        'authenticated',
        'migration-fixture@silomath.local',
        '',
        now(),
        now(),
        now()
    )
    on conflict (id) do nothing;

    --------------------------------------------------------------------
    -- LEGACY ORGANIZATION + ADMIN MEMBERSHIP
    --------------------------------------------------------------------

    insert into public.organizations (
        id,
        name
    )
    values (
        v_org_id,
        'Migration Fixture Organization'
    )
    on conflict (id) do nothing;

    insert into public.organization_members (
        organization_id,
        user_id,
        role
    )
    values (
        v_org_id,
        v_user_id,
        'Admin'
    )
    on conflict do nothing;

    --------------------------------------------------------------------
    -- LEGACY GEOMETRY GRAPH
    --------------------------------------------------------------------

    insert into public.geometry_families (
        id,
        name
    )
    values (
        v_geometry_family_id,
        'Migration Fixture Geometry Family'
    )
    on conflict (id) do nothing;

    insert into public.geometry_profiles (
        id,
        geometry_family_id,
        profile_code,
        name
    )
    values (
        v_geometry_profile_id,
        v_geometry_family_id,
        'MIGRATION_PROFILE',
        'Migration Fixture Geometry Profile'
    )
    on conflict (id) do nothing;

    insert into public.geometry_versions (
        id,
        geometry_profile_id,
        version_number,
        fixed_parameters,
        required_dimensions_schema,
        measurement_schema,
        supported_units,
        calculation_method,
        validation_constraints,
        capacity_rules
    )
    values (
        v_geometry_version_id,
        v_geometry_profile_id,
        1,
        jsonb_build_object(
            'diameter_m', 10,
            'height_m', 20
        ),
        jsonb_build_object(
            'diameter_m', 'required',
            'height_m', 'required'
        ),
        jsonb_build_object(
            'measurement_type', 'ULLAGE'
        ),
        jsonb_build_array(
            'm'
        ),
        'MIGRATION_FIXTURE_V1',
        jsonb_build_object(
            'diameter_positive', true,
            'height_positive', true
        ),
        jsonb_build_object(
            'fixture', true
        )
    )
    on conflict (id) do nothing;

    --------------------------------------------------------------------
    -- LEGACY HIERARCHY
    --------------------------------------------------------------------

    insert into public.locations (
        id,
        organization_id,
        code,
        name
    )
    values (
        v_location_id,
        v_org_id,
        'LOC-MIG',
        'Migration Location'
    )
    on conflict (id) do nothing;

    insert into public.storage_units (
        id,
        location_id,
        code,
        name,
        type
    )
    values (
        v_unit_id,
        v_location_id,
        'UNIT-MIG',
        'Migration Unit',
        'SILO'
    )
    on conflict (id) do nothing;

    insert into public.storage_spaces (
        id,
        storage_unit_id,
        code,
        geometry_profile_id,
        geometry_version_id,
        configured_dimensions
    )
    values (
        v_space_id,
        v_unit_id,
        'SPACE-MIG',
        v_geometry_profile_id,
        v_geometry_version_id,
        jsonb_build_object(
            'diameter_m', 10,
            'height_m', 20
        )
    )
    on conflict (id) do nothing;

    --------------------------------------------------------------------
    -- EMULATE AUTHENTICATED LEGACY RPC CALLER
    --------------------------------------------------------------------

    perform set_config(
        'request.jwt.claim.sub',
        v_user_id::text,
        true
    );

    perform set_config(
        'request.jwt.claim.role',
        'authenticated',
        true
    );

    perform set_config(
        'request.jwt.claims',
        jsonb_build_object(
            'sub', v_user_id::text,
            'role', 'authenticated'
        )::text,
        true
    );

    --------------------------------------------------------------------
    -- LEGACY CAMPAIGN TEAM
    --
    -- Historical RPC requires the launching Admin to belong to the team.
    -- That is a legacy rule and is intentionally respected by this
    -- pre-V6 fixture so that the source state is valid under its own
    -- historical contract.
    --------------------------------------------------------------------

    v_team := jsonb_build_array(
        jsonb_build_object(
            'user_id', v_user_id::text,
            'role_at_assignment', 'Admin'
        )
    );

    --------------------------------------------------------------------
    -- VALID LEGACY SAVE & LAUNCH
    --------------------------------------------------------------------

    select
        result.campaign_id,
        result.launch_snapshot_id,
        result.reused
    into
        v_campaign_id,
        v_snapshot_id,
        v_reused
    from public.save_and_launch_campaign(
        p_organization_id := v_org_id,
        p_name := 'Migration Fixture Campaign',
        p_code := 'INV-2026-0001',
        p_inventory_type := 'Annual',
        p_start_date := date '2026-10-01',
        p_planned_end_date := date '2026-10-31',
        p_scope_storage_space_ids := array[v_space_id],
        p_team := v_team,
        p_idempotency_key := 'migration-fixture-idempotency'
    ) as result;

    --------------------------------------------------------------------
    -- FIXTURE PRECONDITION ASSERTIONS
    --------------------------------------------------------------------

    if v_campaign_id is null then
        raise exception
            'LEGACY FIXTURE FAILED: save_and_launch_campaign returned null campaign_id';
    end if;

    if v_snapshot_id is null then
        raise exception
            'LEGACY FIXTURE FAILED: save_and_launch_campaign returned null launch_snapshot_id';
    end if;

    if not exists (
        select 1
        from public.campaigns c
        where c.id = v_campaign_id
          and c.organization_id = v_org_id
          and c.code = 'INV-2026-0001'
          and c.name = 'Migration Fixture Campaign'
          and c.inventory_type = 'Annual'
          and c.status = 'Active'
          and c.launch_snapshot_id = v_snapshot_id
    ) then
        raise exception
            'LEGACY FIXTURE FAILED: valid launched Campaign was not persisted';
    end if;

    if not exists (
        select 1
        from public.campaign_scope cs
        where cs.campaign_id = v_campaign_id
          and cs.storage_space_id = v_space_id
          and cs.geometry_version_id = v_geometry_version_id
    ) then
        raise exception
            'LEGACY FIXTURE FAILED: Campaign scope was not persisted';
    end if;

    if not exists (
        select 1
        from public.campaign_assignments ca
        where ca.campaign_id = v_campaign_id
          and ca.user_id = v_user_id
          and ca.role_at_assignment = 'Admin'
    ) then
        raise exception
            'LEGACY FIXTURE FAILED: Campaign assignment was not persisted';
    end if;

    if not exists (
        select 1
        from public.campaign_launch_snapshots cls
        where cls.id = v_snapshot_id
          and cls.campaign_id = v_campaign_id
          and cls.organization_id = v_org_id
          and cls.launched_by_user_id = v_user_id
    ) then
        raise exception
            'LEGACY FIXTURE FAILED: Campaign launch snapshot was not persisted';
    end if;

    if not exists (
        select 1
        from public.campaign_launch_snapshot_items item
        where item.snapshot_id = v_snapshot_id
          and item.storage_space_id = v_space_id
          and item.geometry_profile_id = v_geometry_profile_id
          and item.geometry_version_id = v_geometry_version_id
    ) then
        raise exception
            'LEGACY FIXTURE FAILED: Campaign launch snapshot item was not persisted';
    end if;

    if not exists (
        select 1
        from public.campaign_launch_team_snapshot team_item
        where team_item.snapshot_id = v_snapshot_id
          and team_item.user_id = v_user_id
          and team_item.role_at_assignment = 'Admin'
    ) then
        raise exception
            'LEGACY FIXTURE FAILED: Campaign launch team snapshot was not persisted';
    end if;

    raise notice
        'LEGACY FIXTURE READY: campaign_id=%, snapshot_id=%, reused=%',
        v_campaign_id,
        v_snapshot_id,
        v_reused;
end;
$$;
