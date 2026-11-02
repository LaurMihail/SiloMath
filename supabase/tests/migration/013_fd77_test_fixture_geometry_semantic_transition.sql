-- Test helper / transition fixture for geometry family semantic identifiers during migration replay
-- Reconciles legacy F02 family code to CYL_FLAT_V1 before canonical geometry seed migration (20261031145858).
begin;

alter table silomath.geometry_families
    drop constraint if exists geometry_families_code_check;

update silomath.geometry_families
set family_code = 'CYL_FLAT_V1',
    name = 'Cylindrical Silo with Flat Bottom',
    formula_version = '1',
    parameter_contract = 'D,H'
where geometry_family_id = '90000000-0000-0000-0000-000000000006'
  and family_code = 'F02';

commit;
