begin;
do $$
declare a uuid:='90000000-0000-0000-0000-000000000002';
 o uuid:='90000000-0000-0000-0000-000000000001';
 family uuid:='90000000-0000-0000-0000-000000000006';
 id uuid:=gen_random_uuid(); k text:=gen_random_uuid()::text; r jsonb; replay jsonb; first_version uuid; failed boolean;
 patch jsonb;
begin
 patch:=jsonb_build_object('name','Geometry contract','code',' GEOMETRY-TEST ','geometryFamilyId',family,
 'dimensions',jsonb_build_object('diameterM','2','heightM','3'),
 'measurementReferenceHeightM','3','maximumFillHeightM','3','geometryOrigin','BASE');
 r:=silomath.geometry_profile_mutate_command(a,o,id,patch,9.424778,'GEOMETRY_TEST_V1',null,k,'geometry-create','geometry-test',true);
 replay:=silomath.geometry_profile_mutate_command(a,o,id,patch,9.424778,'GEOMETRY_TEST_V1',null,k,'geometry-create','geometry-test',true);
 if r<>replay or r->'body'->>'code'<>'GEOMETRY-TEST' then raise exception 'Geometry create/replay failed'; end if;
 first_version:=(r->'body'->>'currentVersionId')::uuid;
 patch:=patch||'{"dimensions":{"diameterM":"2","heightM":"4"},"measurementReferenceHeightM":"4","maximumFillHeightM":"4"}';
 r:=silomath.geometry_profile_mutate_command(a,o,id,patch,12.566371,'GEOMETRY_TEST_V1',1,
 gen_random_uuid()::text,'geometry-update','geometry-test',false);
 if r->'body'->>'entityVersion'<>'2' or
 (select count(*) from silomath.geometry_profile_versions where geometry_profile_id=id)<>2
 or (select height_m from silomath.geometry_profile_versions where geometry_profile_version_id=first_version)<>3 then
 raise exception 'Geometry version immutability failed'; end if;
 failed:=false;
 begin
 perform silomath.geometry_profile_mutate_command(gen_random_uuid(),o,id,'{"name":"Forged"}',null,null,2,
 gen_random_uuid()::text,'forged','geometry-test',false);
 exception when insufficient_privilege then failed:=true; end;
 if not failed then raise exception 'Unauthorized Geometry mutation accepted'; end if;
 if jsonb_typeof(silomath.geometry_version_to_json(
   jsonb_populate_record(null::silomath.geometry_profile_versions,
     jsonb_build_object('area_provenance','{"approvedReference":"Certified drawing A-101","verification":{"method":"ENGINEERING_CALCULATION"}}'))
 )->'areaProvenance')<>'object' then
 raise exception 'Structured area provenance was not preserved as JSON'; end if;
 if jsonb_typeof(silomath.geometry_version_to_json(
   jsonb_populate_record(null::silomath.geometry_profile_versions,
     jsonb_build_object('area_provenance','Certified drawing A-101'))
 )->'areaProvenance')<>'string' then
 raise exception 'Legacy scalar area provenance was not preserved as string'; end if;
 raise notice 'FD76 GEOMETRY PROFILE ASSERTIONS PASS';
end $$;
rollback;
