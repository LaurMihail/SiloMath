-- Create public.save_and_launch_campaign RPC function
create or replace function public.save_and_launch_campaign(
    p_organization_id uuid,
    p_name text,
    p_code text,
    p_inventory_type text,
    p_start_date date,
    p_planned_end_date date,
    p_scope_storage_space_ids uuid[],
    p_team jsonb,
    p_idempotency_key text
)
returns table (
    campaign_id uuid,
    launch_snapshot_id uuid,
    reused boolean
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_caller_id uuid;
    v_is_admin boolean := false;
    v_trimmed_name text := btrim(coalesce(p_name, ''));
    v_trimmed_code text := btrim(coalesce(p_code, ''));
    v_trimmed_idem_key text := btrim(coalesce(p_idempotency_key, ''));
    v_canonical_scope text;
    v_canonical_team text;
    v_canonical_string text;
    v_fingerprint text;
    v_existing_campaign_id uuid;
    v_existing_launch_snapshot_id uuid;
    v_existing_fingerprint text;
    v_scope_count integer;
    v_distinct_scope_count integer;
    v_valid_spaces_count integer;
    v_team_count integer;
    v_distinct_team_count integer;
    v_valid_members_count integer;
    v_launcher_in_team boolean := false;
    v_launched_at timestamptz := now();
    v_new_campaign_id uuid := gen_random_uuid();
    v_new_snapshot_id uuid := gen_random_uuid();
begin
    -- 1. Identify auth.uid()
    v_caller_id := auth.uid();
    if v_caller_id is null then
        raise exception 'Authentication required to save and launch campaign';
    end if;

    -- 2. Authorization: caller must be Admin in requested organization
    select exists (
        select 1 from public.organization_members
        where organization_id = p_organization_id
          and user_id = v_caller_id
          and role = 'Admin'
    ) into v_is_admin;

    if not v_is_admin then
        raise exception 'Save & Launch requires Admin role in requested organization';
    end if;

    -- 3. Input Validation
    if v_trimmed_name = '' then
        raise exception 'Campaign name cannot be blank';
    end if;

    if v_trimmed_code = '' then
        raise exception 'Campaign code cannot be blank';
    end if;

    if v_trimmed_idem_key = '' then
        raise exception 'Idempotency key cannot be blank';
    end if;

    if p_inventory_type not in ('Annual', 'Monthly', 'Control', 'Handover / Takeover') then
        raise exception 'Invalid inventory type: %', p_inventory_type;
    end if;

    if p_planned_end_date < p_start_date then
        raise exception 'Planned end date must be on or after start date';
    end if;

    -- Scope Validation
    v_scope_count := coalesce(cardinality(p_scope_storage_space_ids), 0);
    if v_scope_count = 0 then
        raise exception 'Campaign scope cannot be empty';
    end if;

    select count(distinct space_id) into v_distinct_scope_count
    from unnest(p_scope_storage_space_ids) as space_id;

    if v_distinct_scope_count <> v_scope_count then
        raise exception 'Duplicate storage space IDs found in campaign scope';
    end if;

    -- Verify every storage space belongs to requested organization
    select count(*) into v_valid_spaces_count
    from public.storage_spaces ss
    join public.storage_units su on su.id = ss.storage_unit_id
    join public.locations l on l.id = su.location_id
    where ss.id = any(p_scope_storage_space_ids)
      and l.organization_id = p_organization_id;

    if v_valid_spaces_count <> v_scope_count then
        raise exception 'One or more storage spaces do not belong to requested organization';
    end if;

    -- Team Validation
    if p_team is null or jsonb_typeof(p_team) <> 'array' or jsonb_array_length(p_team) = 0 then
        raise exception 'Campaign team cannot be empty';
    end if;

    v_team_count := jsonb_array_length(p_team);

    select count(distinct (item->>'user_id')::uuid) into v_distinct_team_count
    from jsonb_array_elements(p_team) as item;

    if v_distinct_team_count <> v_team_count then
        raise exception 'Duplicate user IDs found in campaign team';
    end if;

    -- Check all roles are valid
    if exists (
        select 1 from jsonb_array_elements(p_team) as item
        where coalesce(item->>'role_at_assignment', '') not in ('Admin', 'Standard User')
    ) then
        raise exception 'Invalid role in campaign team assignment';
    end if;

    -- Check every team user is an organization member
    select count(*) into v_valid_members_count
    from jsonb_array_elements(p_team) as item
    join public.organization_members om
      on om.organization_id = p_organization_id
     and om.user_id = (item->>'user_id')::uuid;

    if v_valid_members_count <> v_team_count then
        raise exception 'One or more team users are not members of requested organization';
    end if;

    -- Verify launching Admin is included in team
    select exists (
        select 1 from jsonb_array_elements(p_team) as item
        where (item->>'user_id')::uuid = v_caller_id
    ) into v_launcher_in_team;

    if not v_launcher_in_team then
        raise exception 'Launching Admin must be included in campaign team';
    end if;

    -- 4. Canonical Fingerprint Calculation
    select string_agg(space_id::text, ',' order by space_id)
    into v_canonical_scope
    from unnest(p_scope_storage_space_ids) as space_id;

    select string_agg((item->>'user_id') || ':' || (item->>'role_at_assignment'), ',' order by (item->>'user_id'))
    into v_canonical_team
    from jsonb_array_elements(p_team) as item;

    v_canonical_string := p_organization_id::text || '|' ||
                          v_trimmed_name || '|' ||
                          v_trimmed_code || '|' ||
                          p_inventory_type || '|' ||
                          p_start_date::text || '|' ||
                          p_planned_end_date::text || '|' ||
                          coalesce(v_canonical_scope, '') || '|' ||
                          coalesce(v_canonical_team, '');

    v_fingerprint := encode(extensions.digest(v_canonical_string::bytea, 'sha256'::text), 'hex');

    -- 5. Lock & Idempotency Check
    perform pg_advisory_xact_lock(hashtext('save_and_launch_campaign_' || p_organization_id::text || '_' || v_trimmed_idem_key));

    select c.id, c.launch_snapshot_id, c.request_fingerprint
    into v_existing_campaign_id, v_existing_launch_snapshot_id, v_existing_fingerprint
    from public.campaigns c
    where c.organization_id = p_organization_id
      and c.idempotency_key = v_trimmed_idem_key;

    if v_existing_campaign_id is not null then
        if v_existing_fingerprint = v_fingerprint then
            return query select v_existing_campaign_id, v_existing_launch_snapshot_id, true;
            return;
        else
            raise exception 'IDEMPOTENCY_CONFLICT: Idempotency key reused with different request payload';
        end if;
    end if;

    -- 6. Atomic Write Sequence
    -- a. INSERT campaigns as Active with launch_snapshot_id NULL
    insert into public.campaigns (
        id, organization_id, name, code, inventory_type, start_date, planned_end_date,
        launched_by, launched_at, status, idempotency_key, request_fingerprint
    ) values (
        v_new_campaign_id, p_organization_id, v_trimmed_name, v_trimmed_code, p_inventory_type,
        p_start_date, p_planned_end_date, v_caller_id, v_launched_at, 'Active',
        v_trimmed_idem_key, v_fingerprint
    );

    -- b. INSERT campaign_scope
    insert into public.campaign_scope (campaign_id, storage_space_id, geometry_version_id)
    select v_new_campaign_id, ss.id, ss.geometry_version_id
    from public.storage_spaces ss
    where ss.id = any(p_scope_storage_space_ids);

    -- c. INSERT campaign_assignments
    insert into public.campaign_assignments (campaign_id, user_id, role_at_assignment)
    select v_new_campaign_id, (item->>'user_id')::uuid, item->>'role_at_assignment'
    from jsonb_array_elements(p_team) as item;

    -- d. INSERT campaign_launch_snapshots
    insert into public.campaign_launch_snapshots (
        id, campaign_id, organization_id, organization_name, launched_by_user_id, launched_at
    )
    select v_new_snapshot_id, v_new_campaign_id, p_organization_id, org.name, v_caller_id, v_launched_at
    from public.organizations org
    where org.id = p_organization_id;

    -- e. INSERT campaign_launch_snapshot_items
    insert into public.campaign_launch_snapshot_items (
        snapshot_id, location_id, location_code, location_name,
        storage_unit_id, storage_unit_code, storage_unit_name, storage_unit_type,
        storage_space_id, storage_space_code, geometry_family_id, geometry_family_name,
        geometry_profile_id, geometry_profile_code, geometry_profile_name,
        geometry_version_id, configured_dimensions
    )
    select
        v_new_snapshot_id, l.id, l.code, l.name,
        su.id, su.code, su.name, su.type,
        ss.id, ss.code, gf.id, gf.name,
        gp.id, gp.profile_code, gp.name,
        ss.geometry_version_id, ss.configured_dimensions
    from public.storage_spaces ss
    join public.storage_units su on su.id = ss.storage_unit_id
    join public.locations l on l.id = su.location_id
    join public.geometry_profiles gp on gp.id = ss.geometry_profile_id
    join public.geometry_families gf on gf.id = gp.geometry_family_id
    where ss.id = any(p_scope_storage_space_ids);

    -- f. INSERT campaign_launch_team_snapshot
    insert into public.campaign_launch_team_snapshot (snapshot_id, user_id, role_at_assignment)
    select v_new_snapshot_id, (item->>'user_id')::uuid, item->>'role_at_assignment'
    from jsonb_array_elements(p_team) as item;

    -- g. UPDATE campaigns.launch_snapshot_id
    update public.campaigns set launch_snapshot_id = v_new_snapshot_id where id = v_new_campaign_id;

    -- h. Return results
    return query select v_new_campaign_id, v_new_snapshot_id, false;
end;
$$;

-- Privileges & Permissions
revoke execute on function public.save_and_launch_campaign(uuid, text, text, text, date, date, uuid[], jsonb, text) from public, anon;
grant execute on function public.save_and_launch_campaign(uuid, text, text, text, date, date, uuid[], jsonb, text) to authenticated;
