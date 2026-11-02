begin;
do $$
declare a uuid:='90000000-0000-0000-0000-000000000002';
 o uuid:='90000000-0000-0000-0000-000000000001';
 space uuid:='90000000-0000-0000-0000-000000000005';
 c uuid; records uuid; revision uuid; ids uuid[]:='{}'; r jsonb; first_result jsonb;
 v bigint; code text; n integer; failed boolean;
begin
 update silomath.inventory_campaigns set campaign_year=extract(year from now())::integer-1
 where campaign_id='90000000-0000-0000-0000-000000000009';
 for n in 1..2 loop
 r:=silomath.campaign_save_launch_command(a,o,gen_random_uuid()::text,'report-launch-'||n,'report-test',
 'Report contract fixture '||n,'CONTROL',date '2026-10-31',date '2026-11-01',
 array[space],array[a],'CAMPAIGN_SNAPSHOT_V1','2.0');
 c:=(r->'body'->>'id')::uuid; ids:=array_append(ids,c);
 records:=gen_random_uuid(); revision:=gen_random_uuid();
 perform silomath.stocktaking_record_create_working(a,o,c,records,space,revision);
 perform silomath.stocktaking_mark_empty_command(a,o,c,records,1,null,gen_random_uuid()::text,'report-empty-'||n);
 select entity_version into v from silomath.inventory_campaigns where campaign_id=c;
 perform silomath.campaign_close_command(a,o,c,v,gen_random_uuid()::text,'report-close-'||n,'report-test','FINAL_TEST_V1');
 end loop;
 foreach code in array array['PHYSICAL_INVENTORY','INVENTORY_BY_LOCATION','INVENTORY_BY_COMMODITY',
 'INVENTORY_BY_OWNERSHIP','STORAGE_SPACE_WORKSHEET','CAPACITY_REPORT'] loop
 r:=silomath.report_dataset_get(a,o,code,jsonb_build_object('campaignId',c));
 if r->>'sourceState'<>'FINAL' or r->'totals'->>'physicalQuantityKg'<>'0'
 or jsonb_array_length(r->'rows')<>1 then raise exception 'Report dataset failed: % %',code,r; end if;
 end loop;
 first_result:=silomath.report_dataset_get(a,o,'PHYSICAL_INVENTORY',jsonb_build_object('campaignId',c));
 update silomath.locations set name='Changed after Close' where location_id='90000000-0000-0000-0000-000000000003';
 r:=silomath.report_dataset_get(a,o,'PHYSICAL_INVENTORY',jsonb_build_object('campaignId',c));
 if r->'rows'<>first_result->'rows' then raise exception 'Report depends on mutable current master'; end if;
 r:=silomath.inventory_history_compare(a,o,jsonb_build_object('campaignIds',to_jsonb(ids)));
 if jsonb_array_length(r->'changes')<>1 or r#>>'{changes,0,classification}'<>'COMPARABLE'
 or r#>>'{changes,0,percentageChange}' is not null then raise exception 'History empty comparison failed %',r; end if;
 r:=silomath.inventory_dossier_get(a,o,c);
 if jsonb_array_length(r#>'{finalSnapshot,entries}')<>1 or r?'auditEvents' then
 raise exception 'Dossier shape or Audit boundary failed'; end if;
 failed:=false;
 begin
 perform silomath.report_dataset_get(gen_random_uuid(),o,'PHYSICAL_INVENTORY',jsonb_build_object('campaignId',c));
 exception when insufficient_privilege then failed:=true; end;
 if not failed then raise exception 'Unauthorized reporting accepted'; end if;
 r:=silomath.audit_report_dataset_get(a,o,'{}');
 if jsonb_typeof(r->'rows')<>'array' then raise exception 'Audit dataset failed'; end if;
 perform silomath.report_export_record(a,o,'PHYSICAL_INVENTORY',
 jsonb_build_object('campaignId',c,'finalSnapshotId',first_result->'finalSnapshotId'),'report-test');
 if not exists(select 1 from silomath.audit_events where organization_id=o and action='REPORT_EXPORT') then
 raise exception 'Successful export audit missing'; end if;
 raise notice 'FD76 REPORTING ASSERTIONS PASS';
end $$;
rollback;
