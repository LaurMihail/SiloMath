begin;
do $$
declare a uuid:='90000000-0000-0000-0000-000000000002';
 o uuid:='90000000-0000-0000-0000-000000000001';
 space uuid:='90000000-0000-0000-0000-000000000005';
 c uuid; record_id uuid:=gen_random_uuid(); revision uuid:=gen_random_uuid();
 r jsonb; replay jsonb; v bigint; k text:=gen_random_uuid()::text; failed boolean;
begin
 update silomath.inventory_campaigns set campaign_year=extract(year from now())::integer-1
 where campaign_id='90000000-0000-0000-0000-000000000009';
 r:=silomath.campaign_save_launch_command(a,o,gen_random_uuid()::text,'close-launch','close-test',
 'Close contract fixture','CONTROL',date '2026-10-31',date '2026-11-01',
 array[space],array[a],'CAMPAIGN_SNAPSHOT_V1','2.0');
 c:=(r->'body'->>'id')::uuid;
 select entity_version into v from silomath.inventory_campaigns where campaign_id=c;
 failed:=false;
 begin
 perform silomath.campaign_close_command(a,o,c,v,k,'close-fingerprint','close-test','FINAL_TEST_V1');
 exception when raise_exception then
 if sqlerrm like '%CLOSE_GUARDS_FAILED%' then failed:=true; else raise; end if; end;
 if not failed or exists(select 1 from silomath.final_inventory_snapshots where campaign_id=c)
 or (select status from silomath.inventory_campaigns where campaign_id=c)<>'ACTIVE' then
 raise exception 'Failed Close left partial state'; end if;
 perform silomath.stocktaking_record_create_working(a,o,c,record_id,space,revision);
 perform silomath.stocktaking_mark_empty_command(a,o,c,record_id,1,null,
 gen_random_uuid()::text,'close-empty');
 r:=silomath.campaign_close_command(a,o,c,v,k,'close-fingerprint','close-test','FINAL_TEST_V1');
 replay:=silomath.campaign_close_command(a,o,c,v,k,'close-fingerprint','close-test','FINAL_TEST_V1');
 if replay<>r then raise exception 'Close replay changed result'; end if;
 if (select count(*) from silomath.final_inventory_snapshots where campaign_id=c)<>1 then
 raise exception 'Close snapshot cardinality failed'; end if;
 if jsonb_array_length(r->'body'->'entries')<>1 then raise exception 'Close entry coverage failed'; end if;
 if (select status from silomath.inventory_campaigns where campaign_id=c)<>'COMPLETED'
 or (select entity_version from silomath.inventory_campaigns where campaign_id=c)<>v+1 then
 raise exception 'Close transition/version failed'; end if;
 failed:=false;
 begin
 update silomath.stocktaking_records set updated_at=now() where stocktaking_record_id=record_id;
 exception when object_not_in_prerequisite_state then failed:=true; end;
 if not failed then raise exception 'Completed Stocktaking write accepted'; end if;
 raise notice 'FD76 CAMPAIGN CLOSE ASSERTIONS PASS';
end $$;
rollback;
