# Change Request: CR-CALC-VERSION-CONTRACT-001

## Governance metadata
- Title: Approve canonical Calculation Version Contract and aggregation metadata clarification
- Status: APPROVED
- Baseline: SILOMATH_V6
- Changes frozen V6 baseline: YES (bounded clarification / version contract)
- Owner: SiloMath Product Owner
- Requested by: Product Owner
- Created date: 2026-10-27

## Approval
- Decision: APPROVED by explicit user instruction in this conversation.
- Decision date: 2026-10-27
- Approved by: Product Owner.
- Approval source: the exact canonical decisions in the user message. No separate attachment file was accessible in synced sources; none is claimed to have been read.
- Superseded by: None.

## Affected frozen baseline
Documents: 01_PRODUCT_BASELINE, 02_DECISION_REGISTER, 07_ARCHITECTURE, 09_DATA_MODEL, 10_DATABASE_SCHEMA, 11_API_SPECIFICATION, 12_OFFLINE_SYNC_SPECIFICATION, 13_CALCULATION_ENGINE_SPECIFICATION, 16_REPORTING_SPECIFICATION, 18_TEST_STRATEGY_ACCEPTANCE, 19_RETENTION_POLICY, 20_PRODUCTION_OPERATIONS. All21 documents share the new refreeze revision marker; other document semantics are unchanged. Decisions: D31/D32, FD-73, CE-D29..42, CALC-AC12..20 and relevant API/offline/history/release coverage.

## Current frozen contract and approved delta
The prior documents defined semantics but did not assign the complete concrete tuple. Doc13 section20 contradicted Doc01/Doc09 on the aggregationMethod literal. The approved bounded delta is: `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`.
Frozen formulaCode values remain exactly the table in the accompanying machine-readable contract and Doc13 section22.1. This is not an implementation IADR.

## Reason and alternatives
Resolve the reported metadata contradiction and missing release identifiers. Rejected: guessed version names, blanket allowed-string lists, current-handler fallback and synthetic Production snapshots. No formula, precision or business-rule alternative is approved.

## Impact analysis
- Business/UI/lifecycle: unchanged; no task, CAM-008 or milestone completion implied.
- Architecture: bind the approved tuple to real pure Kotlin handlers.
- Data/API: engineVersion is string2.0; schema/normalization/formula/rule versions are integers1. Existing physical TEXT projections require lossless explicit mapping, not schema or historical-data conversion.
- Offline: actual runtime compatibility remains mandatory before activation.
- Calculation: exact same F01-F10 mathematics, normalization, precision, rounding and tolerance.
- Evidence/reporting/retention: preserve actual frozen history; no automatic relabeling.
- Security/privacy/operations: no auth changes, secrets or external mutation; version support must be demonstrated by tests/release evidence.

## RVM impact
Add FD-73 and executable calculation version/fingerprint/history contract coverage; retain incomplete integration and gate status.

## Test impact
Exact typed tuple, real F01-F10 dispatch, fingerprint/stale detection, unsupported tuple rejection, historical reproduction, baseline consistency and client/server parity.

## Migration and compatibility impact
No Supabase mutation or SQL/Room schema change is authorized by this implementation. Unknown or synthetic historical labels are not aliases for this tuple. Unsupported versions fail GEOM_VERSION_UNSUPPORTED. Existing historical provenance remains immutable.

## Release-gate impact
CAM-008, complete CALC integration and all GATE-M5 terminal branches remain required. This approval is not completion evidence.

## Approved delta
Only the exact tuple/type/aggregation clarification above is approved. All unaffected frozen rules remain authoritative.

## Implementation verification
See engineering/task-evidence/CR-CALC-VERSION-CONTRACT-001.md for commands/results, baseline hashes/registry/RVM verification, executable handler/fingerprint/history tests and explicit remaining integration limitations.
