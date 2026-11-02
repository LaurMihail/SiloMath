begin;
do $$
declare
 a uuid:='90000000-0000-0000-0000-000000000002';
 o uuid:='90000000-0000-0000-0000-000000000001';
 c uuid:='90000000-0000-0000-0000-000000000009';
 k text:=gen_random_uuid()::text; r jsonb; replay jsonb;
 v bigint; snapshots bigint; failed boolean;
begin
 select count(*) into snapshots from silomath.campaign_snapshot_team_items;
 -- The retained canonical fixture has no Team assignment. Create it through
 -- the same authoritative command before testing removal/history.
 perform silomath.campaign_team_mutate_command(a,o,c,a,false,null,
 gen_random_uuid()::text,'team-initial-add','team-test');
 select entity_version into v from silomath.campaign_team_assignments
 where campaign_id=c and user_id=a and status='ACTIVE';
 r:=silomath.campaign_team_mutate_command(a,o,c,a,true,v,k,'team-remove','team-test');
 replay:=silomath.campaign_team_mutate_command(a,o,c,a,true,v,k,'team-remove','team-test');
 if r<>replay or r->'body'->>'assignmentStatus'<>'REMOVED' then raise exception 'Team remove replay failed'; end if;
 if silomath.stocktaking_actor_is_team_member(a,o,c) then raise exception 'Removed Team retained authority'; end if;
 r:=silomath.campaign_team_mutate_command(a,o,c,a,false,null,
 gen_random_uuid()::text,'team-add','team-test');
 if r->'body'->>'assignmentStatus'<>'ACTIVE' then raise exception 'Team add failed'; end if;
 if not silomath.stocktaking_actor_is_team_member(a,o,c) then raise exception 'Team add has no authority'; end if;
 if (select count(*) from silomath.campaign_snapshot_team_items)<>snapshots then
 raise exception 'Team mutation rewrote launch snapshot'; end if;
 if (select count(*) from silomath.campaign_team_assignments where campaign_id=c and user_id=a)<2 then
 raise exception 'Team assignment history lost'; end if;
 failed:=false;
 begin
 perform silomath.campaign_team_mutate_command(gen_random_uuid(),o,c,a,false,null,
 gen_random_uuid()::text,'forged','team-test');
 exception when insufficient_privilege then failed:=true; end;
 if not failed then raise exception 'Forged Team admin accepted'; end if;
 if not exists(select 1 from silomath.audit_events where campaign_id=c and action='CAMPAIGN_TEAM_ADD')
 or not exists(select 1 from silomath.sync_change_stream where campaign_id=c and entity_type='CAMPAIGN_TEAM_ASSIGNMENT') then
 raise exception 'Team audit/sync missing'; end if;
 raise notice 'FD76 CAMPAIGN TEAM ASSERTIONS PASS';
end $$;
rollback;
