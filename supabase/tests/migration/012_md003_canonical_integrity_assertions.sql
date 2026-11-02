-- IMP-MD-003 scoped assertions. Does not certify global legacy reconciliation.
do $$
begin
 if exists(select 1 from silomath.storage_units u left join silomath.locations l on l.location_id=u.location_id and l.organization_id=u.organization_id where l.location_id is null) then
  raise exception 'MD003 canonical StorageUnit parent integrity failed';
 end if;
 if not exists(select 1 from pg_constraint where conname='fk_storage_units_location_organization_md003' and conrelid='silomath.storage_units'::regclass and convalidated) then
  raise exception 'MD003 composite parent FK not validated';
 end if;
 if not exists(select 1 from pg_constraint where conname='storage_units_field_limits_v6' and conrelid='silomath.storage_units'::regclass and convalidated) then
  raise exception 'MD003 field constraints not validated';
 end if;
 if not exists(select 1 from silomath.storage_units where storage_unit_id='90000000-0000-0000-0000-000000000004' and code='REF' and name='Historical Unit' and entity_version=1) then
  raise exception 'Populated canonical Unit changed during migration';
 end if;
 if not exists(select 1 from public.storage_units where id='44444444-4444-4444-4444-444444444444' and code='UNIT-MIG') then
  raise exception 'Retained legacy Unit was lost';
 end if;
 if not exists(select 1 from silomath.inventory_campaigns where organization_id='11111111-1111-1111-1111-111111111111' and code='INV-2026-0001') then
  raise exception 'Legacy campaign was lost';
 end if;
 if exists(select 1 from silomath.organizations where organization_id='11111111-1111-1111-1111-111111111111') then
  raise exception 'Legacy Organization provenance was fabricated';
 end if;
 if not exists(select 1 from pg_constraint where conname='fk_inventory_campaigns_organization_id' and conrelid='silomath.inventory_campaigns'::regclass and not convalidated) then
  raise exception 'Legacy Campaign FK staging changed unexpectedly';
 end if;
end;
$$;
