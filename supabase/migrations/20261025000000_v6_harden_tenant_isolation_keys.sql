-- SiloMath V6
-- IMP-DB-002
-- Tenant-isolation key hardening.
--
-- Governing baseline:
--   04_USER_ROLES_PERMISSIONS
--   10_DATABASE_SCHEMA
--   17_SECURITY_AUDIT_PRIVACY
--
-- Scope:
--   * preserve direct organization_id keys on tenant-owned aggregate roots;
--   * preserve tenant lineage for child records through required immutable parent keys;
--   * do not add FK/composite-FK enforcement here (IMP-DB-003);
--   * do not add uniqueness, CHECK or index hardening here (IMP-DB-004..006);
--   * do not treat identifiers as authorization credentials.

-- CampaignSnapshot Commodity compatibility rows are tenant-owned through
-- CampaignSnapshot. A nullable parent key would break that tenant lineage.
do $$
begin
    if exists (
        select 1
        from silomath.campaign_snapshot_commodity_items
        where campaign_snapshot_id is null
    ) then
        raise exception
            'DB002 migration blocked: campaign_snapshot_commodity_items contains rows without campaign_snapshot_id';
    end if;
end;
$$;

alter table silomath.campaign_snapshot_commodity_items
    alter column campaign_snapshot_id set not null;

comment on column silomath.campaign_snapshot_commodity_items.campaign_snapshot_id is
    'Required tenant-lineage parent key to CampaignSnapshot. Referential enforcement is completed by IMP-DB-003.';
