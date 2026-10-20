-- SiloMath V6
-- IMP-DB-001
-- EXPAND phase: canonical CommodityVersion definitionMetadata persistence.
--
-- Governing baseline:
--   09_DATA_MODEL
--   10_DATABASE_SCHEMA
--   14_COMMODITY_SPECIFICATION
--
-- Additive only:
--   * CommodityVersion remains immutable platform-governed reference data;
--   * definition_metadata stores governed structured version metadata;
--   * JSONB is the physical representation only, not a customer-editable
--     extension point;
--   * accepted keys/types remain governed by the platform-controlled
--     schema registry;
--   * no tenant-specific/custom Commodity capability is introduced;
--   * registry enforcement is not invented by this DB foundation slice;
--   * FK completion remains IMP-DB-003;
--   * unique/check/index hardening remains IMP-DB-004..006;
--   * immutability enforcement remains IMP-DB-008.
--
-- This migration does not close the full IMP-DB-001 task.

alter table silomath.commodity_versions
    add column if not exists definition_metadata jsonb null;
