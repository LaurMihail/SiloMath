# GATE-M5 - audited dependency assessment

## Current execution-order approval

The user explicitly approved temporary decoupling of standalone pure Kotlin calculation implementation from CAM-008. CAM-008 remains incomplete and required for real integration. Standalone numerical kernel implementation and current verification/boundaries are recorded in IMP-CALC-independent-numerical-kernel.md. The approved exception supersedes the execution-order blocker below; it does not close CALC end-to-end tasks or GATE-M5.

## Historical assessment before this approval

OFF-006 is PASS at its staged-import boundary, committed as 761fa4500979aa8fa8085bcf54dd7225c65e2394. This assessment does not reopen OFF-006 or make calculation compatibility its prerequisite. No READY/activation changes.

## Execution graph from the dependency-audited backlog

The source workbook is read-only and supplies task ordering/dependencies, not functional authority. Functional authority is 01_PRODUCT_BASELINE, 02_DECISION_REGISTER and Doc13 (APPROVED/FROZEN), with campaign frozen-context details in Docs09/10 and accepted IADR-0002.

| Task | Audited dependencies | Assessment |
|---|---|---|
| IMP-CAM-008 | IMP-CAM-007 | PARTIAL - canonical builder missing |
| IMP-CALC-001 | IMP-CAM-008 | BLOCKED_TECHNICAL - CAM-008 prerequisite unsatisfied |
| IMP-CALC-002 | IMP-CALC-001 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-003 | IMP-CALC-001;IMP-CALC-002 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-004 | IMP-CALC-001;IMP-CALC-003 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-005 | IMP-CALC-001;IMP-CALC-004 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-006 | IMP-CALC-001;IMP-CALC-005 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-007 | IMP-CALC-001;IMP-CALC-006 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-008 | IMP-CALC-001;IMP-CALC-007 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-009 | IMP-CALC-001;IMP-CALC-008 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-010 | IMP-CALC-001;IMP-CALC-009 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-011 | IMP-CALC-001;IMP-CALC-010 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-012 | IMP-CALC-001;IMP-CALC-011 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-013 | IMP-CALC-001;IMP-CALC-012 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-014 | IMP-CALC-001;IMP-CALC-013 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-015 | IMP-CALC-001;IMP-CALC-014 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-016 | IMP-CALC-001;IMP-CALC-015 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-017 | IMP-CALC-001;IMP-CALC-016 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-018 | IMP-CALC-001;IMP-CALC-017 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-019 | IMP-CALC-001;IMP-CALC-018 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-020 | IMP-CALC-005;IMP-CALC-006;IMP-CALC-007;IMP-CALC-008;IMP-CALC-009;IMP-CALC-010;IMP-CALC-011;IMP-CALC-012;IMP-CALC-013;IMP-CALC-014;IMP-CALC-017 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-021 | IMP-CALC-001;IMP-CALC-020 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-022 | IMP-CALC-020 | NOT IMPLEMENTED - predecessors not complete |
| IMP-CALC-023 | IMP-CALC-020 | NOT IMPLEMENTED - predecessors not complete |
| GATE-M5 | IMP-CALC-019;IMP-CALC-021;IMP-CALC-022;IMP-CALC-023 | NOT DONE - four terminal branches not green |

GATE-M5 terminal branches are CALC-019, CALC-021, CALC-022 and CALC-023. Golden vectors alone do not close the milestone. The initial linear CALC-001 through CALC-018 branch splits into stale-result detection (019) and the 020 golden-vector branch leading to 021/022/023. All ten family tasks, normalization, aggregation, fill, quantity, precision and fingerprint are included transitively.

## First unsatisfied audited prerequisite

CALC-001 explicitly depends on CAM-008 (Campaign Snapshot Builder). CAM-008 depends on CAM-007. Current evidence does not support marking CAM-008 PASS:

- supabase/migrations/20260928124803_create_atomic_save_and_launch_campaign_rpc.sql writes public.campaigns / campaign_scope / campaign_launch_snapshots / campaign_launch_snapshot_items / campaign_launch_team_snapshot using legacy metadata. It does not construct the complete canonical silomath CampaignSnapshot aggregate, frozen commodity compatibility and pinned calculation/schema provenance.
- supabase/migrations/20261018020000_v6_reconcile_campaign_snapshot_frozen_context.sql adds canonical snapshot context columns but explicitly describes structural expansion, not a complete snapshot builder.
- supabase/migrations/20261018023000_v6_expand_campaign_snapshot_commodity_items.sql provides a structural child table; historical compatibility is not invented/backfilled.
- supabase/migrations/20261015002000_v6_stage_legacy_snapshot_provenance.sql explicitly forbids synthesizing missing historical version identifiers or substituting current versions.
- core/network/.../api/CampaignApi.kt declares the launch REST client contract. The inspected current backend contains package serialization/read/authorization code, not a canonical campaign-launch snapshot builder.
- app/.../repository/OfflineCampaignRepository.kt returns UnsupportedOperationException for Save & Launch and snapshot reading. That is not evidence of an operational builder.
- The actual backend-generated OFF-006 test fixture proves transport/staging from prepared canonical data; it does not prove the launch-time snapshot builder exists.

The pure Kotlin calculation-engine currently has no source implementation. CanonicalDecimal in core:model is a persistence helper with boundary rounding, not the deterministic engine. Synthetic engine-v1/formula-v1/schema-v1 test strings cannot be promoted into Production supported versions.

## Boundary and next decision

To respect the user's instruction to follow audited dependencies, CALC-001 cannot be started as though CAM-008 were green. The prerequisite must be completed with its own audited predecessors, or an explicit execution-dependency decoupling must authorize implementing the standalone pure Kotlin Doc13 engine against frozen input contracts before CAM-008 completion, without falsely closing CAM-008 or GATE-M5. This is an ordering/dependency question, not a request for a frozen semantic change. No canonical builder, version name or compatibility capability has been fabricated.

No calculation source code or gate status has been changed. No CALC test has been represented as run. OFF-006 closure verification executed in this turn: 23 tests PASS and Development compilation PASS; git diff --check PASS. No push, Supabase execution/mutation, baseline edit, or package activation. Existing IDE changes are untouched. This blocked dependency assessment remains uncommitted.

## Approved version-contract resolution

CR-CALC-VERSION-CONTRACT-001 is now APPROVED by explicit user instruction. The former missing-identifier/aggregation wording boundary is resolved by controlled baseline update, FD-73 and a real exact-tuple Production registry. See CR-CALC-VERSION-CONTRACT-001.md for current verification and remaining integration boundaries. This supersedes earlier unapproved-version blocker statements, not the historical findings or CAM-008/GATE-M5 integration requirements.

## Current CALC component verification after the approved contract

All following component results come from implemented code, not from approval alone: canonical numerical API and SI input checks, exact mean/fill, F01-F10, density/quantity, rational/symbolic precision, approved tuple-to-handler registry, canonical fingerprint/stale detection and stored-input history reproduction are independently implemented and tested. Golden/boundary suite and backend numerical parity pass. Latest component total:63 client-engine tests plus4 backend verifier tests, no failures/skips. Details are in CR-CALC-VERSION-CONTRACT-001.md and the CALC-021/022 evidence files.

This does not complete the integrated CAM-008 -> pinned calculation context path. GATE-M5 remains OPEN. No previously unknown historical tuple is inferred or aliased to2.0. The earlier table is historical, not the current component status.

### CAM-008 boundary on returning from the independent engine

Doc09 CampaignSnapshot requires snapshotSchemaVersion (table near line390); FinalInventorySnapshot has its own snapshotSchemaVersion near line562. Neither field is calculation inputSchemaVersion. The approved calculation CR assigns inputSchemaVersion1 and the calculation tuple; it does NOT assign a Production CampaignSnapshot schema identifier or equate these distinct version fields.

The available21 official documents describe the snapshot schema version but do not assign its concrete current identifier. The inspected existing package test fixture uses the explicit test-only snapshot-v1 label. That is not Production authority. Current schema/provenance migrations explicitly prohibit inventing historical snapshot/calculation identifiers. No separate attachment was available to supply another approved snapshot version contract.

Therefore the Production snapshot-schema binding boundary is not filled with a guessed1 or snapshot-v1. CAM-008 remains incomplete (and its CAM-007 predecessor is not certified by these numerical tests). Required next input is an approved concrete CampaignSnapshot schema identifier/source and compatibility mapping, distinct from calculation inputSchemaVersion; a separate approved contract may provide it. The standalone mathematical and versioned calculation work remains valid/PASS; this does not reopen OFF-006 or the resolved calculation-version CR. No decision on this snapshot boundary or full campaign integration has been implemented by assumption.
