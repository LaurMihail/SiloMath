-- SiloMath V6 CampaignSnapshot Commodity compatibility expansion.
--
-- Governing contract:
--   02_DECISION_REGISTER FD-72
--   09_DATA_MODEL CampaignSnapshot commodityCompatibilitySnapshot
--   10_DATABASE_SCHEMA DB-D43
--   CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001
--
-- DB001 scope:
--   Expand the canonical physical table only.
--
-- Deferred to dedicated database-hardening tasks:
--   FK enforcement
--   uniqueness enforcement
--   additional CHECK constraints
--   indexes
--   immutable-row enforcement
--
-- Existing historical CampaignSnapshots are not backfilled because the
-- legacy schema does not contain authoritative launch-time Commodity
-- compatibility provenance. No compatibility data is fabricated.

create table if not exists silomath.campaign_snapshot_commodity_items (
    snapshot_commodity_item_id uuid primary key,
    campaign_snapshot_id uuid null,
    commodity_id uuid null,
    commodity_version_id uuid null
);

comment on table silomath.campaign_snapshot_commodity_items is
    'Immutable CampaignSnapshot launch-time Commodity compatibility child collection. Structural foundation only; referential, uniqueness, indexing and immutability hardening are handled by dedicated database-hardening tasks.';

comment on column silomath.campaign_snapshot_commodity_items.campaign_snapshot_id is
    'Parent CampaignSnapshot identity. FK enforcement is deferred to the dedicated referential-integrity task.';

comment on column silomath.campaign_snapshot_commodity_items.commodity_id is
    'Stable Commodity identity captured for launch-time compatibility.';

comment on column silomath.campaign_snapshot_commodity_items.commodity_version_id is
    'Immutable CommodityVersion identity captured for launch-time compatibility.';
