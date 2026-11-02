-- FD76 Campaign Save & Launch authoritative command assertions.
-- Reuses the canonical MD003 synthetic hierarchy fixture and rolls back every change.

begin;

do $$
declare
    v_actor uuid := '90000000-0000-0000-0000-000000000002';
    v_org uuid := '90000000-0000-0000-0000-000000000001';
    v_space uuid := '90000000-0000-0000-0000-000000000005';
    v_result jsonb;
    v_replay jsonb;
    v_body jsonb;
    v_campaign_id uuid;
    v_snapshot_id uuid;
    v_persisted_version bigint;
    v_active_commodity_count bigint;
begin
    if has_function_privilege(
        'anon',
        'silomath.campaign_save_launch_command(uuid,uuid,text,text,text,text,text,date,date,uuid[],uuid[],text,text)',
        'EXECUTE'
    ) or has_function_privilege(
        'authenticated',
        'silomath.campaign_save_launch_command(uuid,uuid,text,text,text,text,text,date,date,uuid[],uuid[],text,text)',
        'EXECUTE'
    ) or has_function_privilege(
        'service_role',
        'silomath.campaign_save_launch_command(uuid,uuid,text,text,text,text,text,date,date,uuid[],uuid[],text,text)',
        'EXECUTE'
    ) then
        raise exception 'campaign launch command must remain postgres-only';
    end if;

    if not has_function_privilege(
        'postgres',
        'silomath.campaign_save_launch_command(uuid,uuid,text,text,text,text,text,date,date,uuid[],uuid[],text,text)',
        'EXECUTE'
    ) then
        raise exception 'postgres must execute campaign launch command';
    end if;

    -- The historical fixture intentionally owns sequence 9999 for 2026.
    -- Move only its test-local allocation year out of the authoritative current
    -- UTC year so this rollback-only assertion can exercise sequence allocation.
    update silomath.inventory_campaigns
    set campaign_year = extract(year from (now() at time zone 'UTC'))::integer - 1
    where campaign_id = '90000000-0000-0000-0000-000000000009';

    select silomath.campaign_save_launch_command(
        v_actor,
        v_org,
        '77000000-0000-0000-0000-000000000001',
        'campaign-launch-contract-v1',
        'fd76-campaign-launch',
        'FD76 Campaign Launch Contract',
        'CONTROL',
        date '2026-10-31',
        date '2026-11-01',
        array[v_space],
        array[v_actor],
        'CAMPAIGN_SNAPSHOT_V1',
        '2.0'
    ) into v_result;

    if (v_result ->> 'status')::integer <> 201 then
        raise exception 'campaign launch did not return HTTP-equivalent status 201';
    end if;

    v_body := v_result -> 'body';
    v_campaign_id := (v_body ->> 'id')::uuid;
    v_snapshot_id := (v_body ->> 'campaignSnapshotId')::uuid;

    if (
        select array_agg(key order by key)
        from jsonb_object_keys(v_body) key
    ) <> array[
        'campaignCode', 'campaignSnapshotId', 'completedAt', 'completedBy',
        'createdAt', 'createdBy', 'entityVersion', 'id', 'inventoryType',
        'launchedAt', 'launchedBy', 'name', 'organizationId',
        'plannedEndDate', 'startDate', 'status', 'updatedAt', 'updatedBy'
    ]::text[] then
        raise exception 'campaign launch response is not the canonical CampaignResponse shape';
    end if;

    select entity_version
    into v_persisted_version
    from silomath.inventory_campaigns
    where campaign_id = v_campaign_id;

    if v_persisted_version is null
       or (v_body ->> 'entityVersion')::bigint <> v_persisted_version then
        raise exception 'campaign launch response entityVersion is not the persisted server version';
    end if;

    select silomath.campaign_save_launch_command(
        v_actor,
        v_org,
        '77000000-0000-0000-0000-000000000001',
        'campaign-launch-contract-v1',
        'fd76-campaign-launch-replay',
        'FD76 Campaign Launch Contract',
        'CONTROL',
        date '2026-10-31',
        date '2026-11-01',
        array[v_space],
        array[v_actor],
        'CAMPAIGN_SNAPSHOT_V1',
        '2.0'
    ) into v_replay;

    if v_replay <> v_result then
        raise exception 'campaign launch exact replay did not preserve canonical response';
    end if;

    if (select entity_version from silomath.inventory_campaigns where campaign_id = v_campaign_id)
       <> v_persisted_version then
        raise exception 'campaign launch replay incremented entityVersion';
    end if;

    if (
        select count(*)
        from silomath.inventory_campaigns
        where organization_id = v_org
          and name = 'FD76 Campaign Launch Contract'
    ) <> 1 then
        raise exception 'campaign launch replay created a duplicate Campaign';
    end if;

    if not exists (
        select 1
        from silomath.campaign_snapshots
        where campaign_snapshot_id = v_snapshot_id
          and campaign_id = v_campaign_id
          and organization_id = v_org
    ) then
        raise exception 'campaign launch snapshot was not persisted';
    end if;

    if (
        select count(*)
        from silomath.campaign_snapshot_scope_items
        where campaign_snapshot_id = v_snapshot_id
          and storage_space_id = v_space
    ) <> 1 then
        raise exception 'campaign launch scope snapshot was not persisted exactly once';
    end if;

    if (
        select count(*)
        from silomath.campaign_snapshot_team_items
        where campaign_snapshot_id = v_snapshot_id
          and user_id = v_actor
    ) <> 1 then
        raise exception 'campaign launch team snapshot was not persisted exactly once';
    end if;

    select count(*)
    into v_active_commodity_count
    from silomath.commodities c
    join silomath.commodity_versions cv
      on cv.commodity_version_id = c.current_version_id
     and cv.commodity_id = c.commodity_id
    where c.lifecycle_status = 'ACTIVE';

    if (
        select count(*)
        from silomath.campaign_snapshot_commodity_items
        where campaign_snapshot_id = v_snapshot_id
    ) <> v_active_commodity_count then
        raise exception 'campaign launch commodity compatibility snapshot is incomplete';
    end if;

    if not exists (
        select 1
        from silomath.audit_events
        where organization_id = v_org
          and campaign_id = v_campaign_id
          and entity_type = 'InventoryCampaign'
          and entity_id = v_campaign_id
          and action = 'SAVE_AND_LAUNCH'
    ) then
        raise exception 'campaign launch audit event was not emitted';
    end if;
end;
$$;

rollback;
