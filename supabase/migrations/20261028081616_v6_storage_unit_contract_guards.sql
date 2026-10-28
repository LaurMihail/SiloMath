-- IMP-MD-003. Forward-only Storage Unit invariant closure, V6 Docs09/10/11.
-- No UPDATE/backfill, legacy conversion, deletion, reassignment or schema contraction.
-- Invalid existing canonical data makes validation fail rather than being reinterpreted.
begin;
create unique index if not exists uq_locations_identity_organization_md003
    on silomath.locations(location_id,organization_id);
alter table silomath.storage_units
    add constraint fk_storage_units_location_organization_md003
    foreign key(location_id,organization_id)
    references silomath.locations(location_id,organization_id) on delete restrict not valid;
alter table silomath.storage_units validate constraint fk_storage_units_location_organization_md003;
alter table silomath.storage_units
    add constraint storage_units_field_limits_v6 check (
        char_length(name) between 1 and 150 and name=btrim(name)
        and char_length(code) between 1 and 50 and code=upper(btrim(code))
        and (description is null or char_length(description)<=500)
    ) not valid;
alter table silomath.storage_units validate constraint storage_units_field_limits_v6;

create or replace function silomath.guard_storage_unit_identity_history()
returns trigger language plpgsql as $$
begin
    if new.storage_unit_id is distinct from old.storage_unit_id
        or new.organization_id is distinct from old.organization_id
        or new.location_id is distinct from old.location_id
        or new.created_at is distinct from old.created_at
        or new.created_by is distinct from old.created_by then
        raise exception 'Storage Unit identity, parent and creation provenance are immutable'
            using errcode='55000';
    end if;
    if new.code is distinct from old.code and (exists (
        select 1 from silomath.campaign_snapshot_scope_items s where s.storage_unit_id=old.storage_unit_id
    ) or exists (
        select 1 from silomath.campaign_scopes c
        join silomath.storage_spaces s on s.storage_space_id=c.storage_space_id
        where s.storage_unit_id=old.storage_unit_id
    ) or exists (
        select 1 from silomath.stocktaking_records r
        join silomath.storage_spaces s on s.storage_space_id=r.storage_space_id
        where s.storage_unit_id=old.storage_unit_id
    )) then
        raise exception 'Historically referenced Storage Unit Code is stable' using errcode='55000';
    end if;
    return new;
end;
$$;
create trigger trg_storage_units_identity_history
before update on silomath.storage_units
for each row execute function silomath.guard_storage_unit_identity_history();
commit;
