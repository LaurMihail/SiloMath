begin;

do $$
declare
  v_actor uuid := '90000000-0000-0000-0000-000000000002';
  v_org uuid := '90000000-0000-0000-0000-000000000001';
  v_space uuid := '90000000-0000-0000-0000-000000000005';
  v_campaign uuid;
  v_record uuid := gen_random_uuid();
  v_revision uuid := gen_random_uuid();
  v_issue uuid := gen_random_uuid();
  v_note uuid := gen_random_uuid();
  v_photo uuid := gen_random_uuid();
  v_result jsonb;
  v_replay jsonb;
  v_audit_before integer;
  v_sync_before integer;
  v_i integer;
  v_failed boolean;
  v_resolve_key text := gen_random_uuid()::text;
  v_remove_key text := gen_random_uuid()::text;
begin
  update silomath.inventory_campaigns
  set campaign_year = extract(year from (now() at time zone 'UTC'))::integer - 1
  where campaign_id = '90000000-0000-0000-0000-000000000009';

  v_result := silomath.campaign_save_launch_command(
    v_actor, v_org, gen_random_uuid()::text,
    'fd76-issue-evidence-campaign', 'fd76-issue-evidence-test',
    'FD76 Issue Evidence Contract', 'CONTROL',
    date '2026-10-31', date '2026-11-01',
    array[v_space], array[v_actor],
    'CAMPAIGN_SNAPSHOT_V1', '2.0'
  );
  v_campaign := (v_result->'body'->>'id')::uuid;

  perform silomath.stocktaking_record_create_working(
    v_actor, v_org, v_campaign, v_record, v_space, v_revision
  );

  select count(*) into v_audit_before from silomath.audit_events;
  select count(*) into v_sync_before from silomath.sync_change_stream;

  v_result := silomath.issue_create_command(
    v_actor, v_org, v_campaign, v_record, v_issue,
    'OTHER', 'Contract assertion issue',
    gen_random_uuid()::text, 'issue-create-fingerprint', 'fd76-issue-evidence-test'
  );
  if (v_result->>'status')::integer <> 201
     or v_result->'body'->>'issueId' <> v_issue::text
     or v_result->'body'->>'status' <> 'OPEN'
     or (v_result->'body'->>'entityVersion')::bigint <> 1 then
    raise exception 'Issue create response is not canonical: %', v_result;
  end if;

  -- Exact replay uses the same key/fingerprint and returns the same result.
  declare
    v_replay_key text;
  begin
    select r.idempotency_key into v_replay_key
    from silomath.api_idempotency_records r
    where r.actor_user_id = v_actor
      and r.organization_id = v_org
      and r.operation_code = 'ISSUE_CREATE'
      and r.response_body->>'issueId' = v_issue::text;

    v_replay := silomath.issue_create_command(
      v_actor, v_org, v_campaign, v_record, v_issue,
      'OTHER', 'Contract assertion issue',
      v_replay_key, 'issue-create-fingerprint', 'fd76-issue-evidence-test'
    );
  end;

  if v_replay <> v_result or
     (select count(*) from silomath.issues where issue_id = v_issue) <> 1 then
    raise exception 'Issue exact replay duplicated or changed result';
  end if;

  v_result := silomath.issue_transition_command(
    v_actor, v_org, v_campaign, v_issue, 'RESOLVED',
    'Resolved explicitly', 'NORMAL', 1,
    v_resolve_key, 'issue-resolve-fingerprint',
    'fd76-issue-evidence-test'
  );
  if v_result->'body'->>'status' <> 'RESOLVED'
     or (v_result->'body'->>'entityVersion')::bigint <> 2 then
    raise exception 'Issue resolve failed: %', v_result;
  end if;

  v_replay := silomath.issue_transition_command(
    v_actor, v_org, v_campaign, v_issue, 'RESOLVED',
    'Resolved explicitly', 'NORMAL', 1,
    v_resolve_key, 'issue-resolve-fingerprint',
    'fd76-issue-evidence-test'
  );
  if v_replay <> v_result
     or (select entity_version from silomath.issues where issue_id = v_issue) <> 2 then
    raise exception 'Issue resolve exact replay changed result or entityVersion';
  end if;

  v_failed := false;
  begin
    perform silomath.issue_transition_command(
      v_actor, v_org, v_campaign, v_issue, 'OPEN',
      'Stale reopen', null, 1,
      gen_random_uuid()::text, 'issue-stale-fingerprint',
      'fd76-issue-evidence-test'
    );
  exception when serialization_failure then
    v_failed := true;
  end;
  if not v_failed then
    raise exception 'Issue stale entityVersion was not rejected';
  end if;

  v_result := silomath.issue_transition_command(
    v_actor, v_org, v_campaign, v_issue, 'OPEN',
    'Reopened explicitly', null, 2,
    gen_random_uuid()::text, 'issue-reopen-fingerprint',
    'fd76-issue-evidence-test'
  );
  if v_result->'body'->>'status' <> 'OPEN'
     or (v_result->'body'->>'entityVersion')::bigint <> 3 then
    raise exception 'Issue reopen failed: %', v_result;
  end if;

  v_result := silomath.evidence_create_command(
    v_actor, v_org, v_campaign, v_note, v_record, null, null,
    'NOTE', 'Canonical note', null, null, null, null, null, null,
    gen_random_uuid()::text, 'evidence-note-fingerprint',
    'fd76-issue-evidence-test'
  );
  if v_result->'body'->>'evidenceType' <> 'NOTE'
     or v_result->'body'->>'syncState' <> 'LOCAL_PENDING' then
    raise exception 'NOTE Evidence create failed: %', v_result;
  end if;

  v_result := silomath.evidence_create_command(
    v_actor, v_org, v_campaign, v_photo, v_record, null, v_revision,
    'PHOTO', null, 'private/assertion-photo.jpg',
    repeat('a', 64), 'image/jpeg', 4, 2, 2,
    gen_random_uuid()::text, 'evidence-photo-fingerprint',
    'fd76-issue-evidence-test'
  );
  if v_result->'body'->>'syncState' <> 'LOCAL_PENDING' then
    raise exception 'PHOTO Evidence metadata create failed: %', v_result;
  end if;

  v_result := silomath.evidence_finalize_upload_command(
    v_actor, v_org, v_campaign, v_photo,
    v_org::text || '/' || v_campaign::text || '/' || v_photo::text || '/' || repeat('a', 64) || '.jpg',
    repeat('a', 64), 4,
    gen_random_uuid()::text, 'evidence-finalize-fingerprint',
    'fd76-issue-evidence-test'
  );
  if v_result->'body'->>'syncState' <> 'SYNCED'
     or (v_result->'body'->>'entityVersion')::bigint <> 2 then
    raise exception 'PHOTO Evidence finalize failed: %', v_result;
  end if;

  for v_i in 2..5 loop
    perform silomath.evidence_create_command(
      v_actor, v_org, v_campaign, gen_random_uuid(), v_record, null, v_revision,
      'PHOTO', null, 'private/assertion-' || v_i || '.jpg',
      lpad(v_i::text, 64, 'b'), 'image/jpeg', 4, 2, 2,
      gen_random_uuid()::text, 'evidence-limit-' || v_i,
      'fd76-issue-evidence-test'
    );
  end loop;

  v_failed := false;
  begin
    perform silomath.evidence_create_command(
      v_actor, v_org, v_campaign, gen_random_uuid(), v_record, null, v_revision,
      'PHOTO', null, 'private/assertion-sixth.jpg',
      repeat('c', 64), 'image/jpeg', 4, 2, 2,
      gen_random_uuid()::text, 'evidence-limit-sixth',
      'fd76-issue-evidence-test'
    );
  exception when raise_exception then
    if sqlerrm like '%EVIDENCE_PHOTO_LIMIT%' then v_failed := true; else raise; end if;
  end;
  if not v_failed then
    raise exception 'Sixth active PHOTO was not rejected';
  end if;

  v_result := silomath.evidence_remove_command(
    v_actor, v_org, v_campaign, v_note, 1,
    v_remove_key, 'evidence-remove-fingerprint',
    'fd76-issue-evidence-test'
  );
  if v_result->'body'->>'removedAt' is null
     or (v_result->'body'->>'entityVersion')::bigint <> 2 then
    raise exception 'Evidence logical removal failed: %', v_result;
  end if;

  v_replay := silomath.evidence_remove_command(
    v_actor, v_org, v_campaign, v_note, 1,
    v_remove_key, 'evidence-remove-fingerprint',
    'fd76-issue-evidence-test'
  );
  if v_replay <> v_result
     or (select entity_version from silomath.evidence where evidence_id = v_note) <> 2 then
    raise exception 'Evidence removal exact replay changed result or entityVersion';
  end if;

  if (select count(*) from silomath.audit_events) <= v_audit_before
     or (select count(*) from silomath.sync_change_stream) <= v_sync_before then
    raise exception 'Issue/Evidence audit or sync emission missing';
  end if;

  raise notice 'FD76 ISSUE/EVIDENCE ASSERTIONS PASSED';
end;
$$;

rollback;
