# SiloMath V6 — Baseline Change & Refreeze Record

**Record date:** 2026-10-20  
**Disposition:** APPROVED AND APPLIED  
**Resulting baseline:** SiloMath V6 APPROVED / FROZEN — CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 baseline-change refreeze  

## 1. Approval basis

CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 was approved through the SiloMath baseline-change governance process before application to the frozen baseline.

## 2. Applied baseline change

CampaignSnapshot commodityCompatibilitySnapshot remains an immutable N-valued launch-time frozen collection.

Its canonical physical representation is campaign_snapshot_commodity_items with:

- snapshot_commodity_item_id UUID
- campaign_snapshot_id UUID
- commodity_id UUID
- commodity_version_id UUID

The collection preserves launch-time Commodity identity/version compatibility and is not represented as opaque JSON or reconstructed from later mutable Commodity state.

## 3. Direct normative updates

Substantive changes were applied to:

- 02_DECISION_REGISTER
- 09_DATA_MODEL
- 10_DATABASE_SCHEMA
- 18_TEST_STRATEGY_ACCEPTANCE

All 21 official baseline documents carry the 2026-10-20 refreeze marker.

No additional product behavior was approved.

## 4. Frozen invariants preserved

This refreeze does not change Commodity governance, Commodity selection, Stocktaking semantics, Campaign lifecycle, calculation behavior, authorization, offline/local-first authority, reporting semantics or Final Inventory Snapshot semantics.

Detailed FK, uniqueness, CHECK, index and immutability enforcement remain under existing database-hardening scope.

## 5. Traceability

FD-72 extends the final-decision namespace to FD-01–FD-72.

Requirement Registry tooling and generated Requirement Registry/RVM artifacts were updated accordingly.

FD-72 remains UNMAPPED until executable implementation verification is added with the implementation slice. No test evidence was invented.

## 6. Validation evidence

Refreeze validation completed with **114 PASS / 0 FAIL**.

**Refreeze status: FINAL / APPROVED / FROZEN.**
