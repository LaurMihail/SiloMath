begin;
do $$
declare
 a uuid:='90000000-0000-0000-0000-000000000002';
 o uuid:='90000000-0000-0000-0000-000000000001';
 c uuid:=gen_random_uuid(); k text:=gen_random_uuid()::text;
 r jsonb; replay jsonb; failed boolean; n bigint;
begin
 r:=silomath.client_mutate_command(a,o,c,
 '{"name":"Contract Client","code":" test-client ","registrationTaxId":null}'::jsonb,
 null,k,'create-fingerprint','client-test',true);
 if r->'body'->>'code'<>'TEST-CLIENT' or r->>'status'<>'201' then
 raise exception 'Client create/normalization failed'; end if;
 replay:=silomath.client_mutate_command(a,o,c,
 '{"name":"Contract Client","code":" test-client ","registrationTaxId":null}'::jsonb,
 null,k,'create-fingerprint','client-test',true);
 if replay<>r then raise exception 'Client replay differs'; end if;
 failed:=false;
 begin
 perform silomath.client_mutate_command(a,o,c,'{}',null,k,'different','client-test',true);
 exception when others then if sqlstate='23505' or sqlerrm like '%IDEMPOTENCY%' then failed:=true; else raise; end if;
 end;
 if not failed then raise exception 'Idempotency mismatch accepted'; end if;
 r:=silomath.client_mutate_command(a,o,c,'{"lifecycleStatus":"INACTIVE"}',1,
 gen_random_uuid()::text,'update-fingerprint','client-test',false);
 if r->'body'->>'entityVersion'<>'2' or r->'body'->>'lifecycleStatus'<>'INACTIVE' then
 raise exception 'Client update failed'; end if;
 failed:=false;
 begin
 perform silomath.client_mutate_command(a,o,c,'{"name":"Stale"}',1,
 gen_random_uuid()::text,'stale','client-test',false);
 exception when serialization_failure then failed:=true; end;
 if not failed then raise exception 'Stale Client accepted'; end if;
 failed:=false;
 begin
 perform silomath.client_mutate_command(gen_random_uuid(),o,c,'{"name":"Forged"}',2,
 gen_random_uuid()::text,'forged','client-test',false);
 exception when insufficient_privilege then failed:=true; end;
 if not failed then raise exception 'Unauthorized Client mutation accepted'; end if;
 failed:=false;
 begin
 perform silomath.client_mutate_command(a,o,gen_random_uuid(),'{"name":"","code":"X"}',null,
 gen_random_uuid()::text,'invalid','client-test',true);
 exception when invalid_parameter_value then failed:=true; end;
 if not failed then raise exception 'Invalid Client accepted'; end if;
 select count(*) into n from silomath.audit_events where entity_id=c;
 if n<>2 then raise exception 'Client audit count %',n; end if;
 select count(*) into n from silomath.sync_change_stream where entity_id=c;
 if n<>2 then raise exception 'Client sync count %',n; end if;
 raise notice 'FD76 CLIENT ASSERTIONS PASS';
end $$;
rollback;
