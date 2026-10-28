-- IMP-MD-005. Forward-only canonical Storage Space contract closure.
-- No legacy rewrite, deletion, numeric identity promotion or live-data backfill.
begin;

create unique index if not exists uq_storage_units_identity_organization_md005
    on silomath.storage_units(storage_unit_id,organization_id);
create unique index if not exists uq_geometry_profiles_identity_organization_md005
    on silomath.geometry_profiles(geometry_profile_id,organization_id);

alter table silomath.storage_spaces
    add constraint fk_storage_spaces_unit_organization_md005
    foreign key(storage_unit_id,organization_id)
    references silomath.storage_units(storage_unit_id,organization_id) on delete restrict not valid;
alter table silomath.storage_spaces validate constraint fk_storage_spaces_unit_organization_md005;

alter table silomath.storage_spaces
    add constraint fk_storage_spaces_geometry_organization_md005
    foreign key(geometry_profile_id,organization_id)
    references silomath.geometry_profiles(geometry_profile_id,organization_id) on delete restrict not valid;
alter table silomath.storage_spaces validate constraint fk_storage_spaces_geometry_organization_md005;

alter table silomath.storage_spaces
    add constraint storage_spaces_canonical_code_md005
    check (char_length(code)>=1 and code=upper(btrim(code))) not valid;
alter table silomath.storage_spaces validate constraint storage_spaces_canonical_code_md005;

create or replace function silomath.guard_storage_space_identity_history()
returns trigger language plpgsql as $$
begin
    if new.storage_space_id is distinct from old.storage_space_id
        or new.organization_id is distinct from old.organization_id
        or new.storage_unit_id is distinct from old.storage_unit_id
        or new.created_at is distinct from old.created_at
        or new.created_by is distinct from old.created_by then
        raise exception 'Storage Space identity, parent and creation provenance are immutable' using errcode='55000';
    end if;
    if (new.code is distinct from old.code or new.geometry_profile_id is distinct from old.geometry_profile_id)
        and (exists(select 1 from silomath.campaign_scopes where storage_space_id=old.storage_space_id)
          or exists(select 1 from silomath.campaign_snapshot_scope_items where storage_space_id=old.storage_space_id)
          or exists(select 1 from silomath.stocktaking_records where storage_space_id=old.storage_space_id)) then
        raise exception 'Historically referenced Storage Space Code and Geometry Profile are stable' using errcode='55000';
    end if;
    return new;
end;
$$;

create trigger trg_storage_spaces_identity_history
before update on silomath.storage_spaces
for each row execute function silomath.guard_storage_space_identity_history();

commit;
