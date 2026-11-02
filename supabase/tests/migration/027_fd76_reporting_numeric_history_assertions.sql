begin;
do $$
declare
 a uuid:='90000000-0000-0000-0000-000000000002';
 o uuid:='90000000-0000-0000-0000-000000000001';
 space uuid:='90000000-0000-0000-0000-000000000005';
 second_space uuid:=gen_random_uuid(); third_space uuid:=gen_random_uuid();
 ids uuid[]:='{}'; scoped uuid[]; c uuid; snap uuid; launch uuid; item uuid;
 r jsonb; n integer; i integer; volume numeric; quantity numeric; capacity numeric;
begin
 insert into silomath.storage_spaces(storage_space_id,organization_id,storage_unit_id,code,geometry_profile_id,
 lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version)
 select second_space,organization_id,storage_unit_id,'REPORT-NUMERIC-2',geometry_profile_id,
 lifecycle_status,now(),a,now(),a,1 from silomath.storage_spaces where storage_space_id=space;
 insert into silomath.storage_spaces(storage_space_id,organization_id,storage_unit_id,code,geometry_profile_id,
 lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version)
 select third_space,organization_id,storage_unit_id,'REPORT-NUMERIC-3',geometry_profile_id,
 lifecycle_status,now(),a,now(),a,1 from silomath.storage_spaces where storage_space_id=space;
 update silomath.inventory_campaigns set campaign_year=extract(year from now())::integer-1
 where campaign_id='90000000-0000-0000-0000-000000000009';
 for n in 1..3 loop
 scoped:=case when n=1 then array[space,second_space] else array[space,third_space] end;
 r:=silomath.campaign_save_launch_command(a,o,gen_random_uuid()::text,'numeric-launch-'||n,'report-numeric-test',
 'Synthetic numeric report '||n,'CONTROL',date '2026-10-31',date '2026-11-01',
 scoped,array[a],'CAMPAIGN_SNAPSHOT_V1','2.0');
 c:=(r->'body'->>'id')::uuid; launch:=(r->'body'->>'campaignSnapshotId')::uuid;
 ids:=array_append(ids,c);snap:=gen_random_uuid();
 -- Test-only immutable snapshots isolate reporting arithmetic from calculation.
 insert into silomath.final_inventory_snapshots(final_snapshot_id,organization_id,campaign_id,campaign_snapshot_id,
 closed_at,closed_by,snapshot_schema_version,calculation_compatibility_versions,created_at,created_by)
 values(snap,o,c,launch,now()+n*interval '1 second',a,'FINAL_TEST_V1','{"engineVersion":"2.0"}',now(),a);
 for i in 1..2 loop
 item:=scoped[i];volume:=case when i=1 then 1 else 2 end;
 quantity:=case when i=1 then 10+n else 20 end;capacity:=case when i=1 then 2 else 8 end;
 insert into silomath.final_inventory_snapshot_entries(entry_id,final_snapshot_id,storage_space_id,
 hierarchy_snapshot,geometry_snapshot,commodity_snapshot,ownership_client_snapshot,is_empty,
 measurements_snapshot,operational_values,calculation_metadata,validation_metadata,issue_references_resolution_summary,evidence_references)
 values(gen_random_uuid(),snap,item,
 jsonb_build_object('locationId','90000000-0000-0000-0000-000000000003','storageSpaceCode','SYNTHETIC'),
 jsonb_build_object('maxCapacityM3',capacity),null,
 jsonb_build_object('ownership',case when n=3 and i=1 then 'THIRD_PARTY' else 'OWNED' end),false,'[]',
 jsonb_build_object('productVolumeM3',volume,'physicalQuantityKg',quantity),
 '{"engineVersion":"2.0"}','{}','[]','[]');
 end loop;
 update silomath.inventory_campaigns set status='COMPLETED',completed_at=now(),completed_by=a where campaign_id=c;
 end loop;
 r:=silomath.report_dataset_get(a,o,'CAPACITY_REPORT',jsonb_build_object('campaignId',ids[1]));
 if (r#>>'{totals,utilizationPercent}')::numeric<>30 then raise exception 'Capacity aggregate used arithmetic mean: %',r; end if;
 r:=silomath.report_dataset_get(a,o,'PHYSICAL_INVENTORY',jsonb_build_object('campaignId',ids[1]));
 if (r#>>'{totals,physicalQuantityKg}')::numeric<>31 then raise exception 'Canonical sum failed'; end if;
 r:=silomath.inventory_history_compare(a,o,jsonb_build_object('campaignIds',to_jsonb(ids)));
 if not exists(select 1 from jsonb_array_elements(r->'changes') x where x->>'classification'='SCOPE_ADDED')
 or not exists(select 1 from jsonb_array_elements(r->'changes') x where x->>'classification'='SCOPE_REMOVED')
 or not exists(select 1 from jsonb_array_elements(r->'changes') x where x->>'classification'='CONTEXT_CHANGED')
 or not exists(select 1 from jsonb_array_elements(r->'changes') x where x->>'classification'='COMPARABLE'
 and (x->>'absoluteChangeKg')::numeric=1) then raise exception 'History classification/metric failed %',r; end if;
 raise notice 'FD76 REPORT NUMERIC/HISTORY ASSERTIONS PASS';
end $$;
rollback;
