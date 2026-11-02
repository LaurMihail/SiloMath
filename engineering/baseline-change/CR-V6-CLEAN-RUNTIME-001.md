# Change Request: CR-V6-CLEAN-RUNTIME-001

## Governance metadata

- CR ID: CR-V6-CLEAN-RUNTIME-001
- Title: Eliminate legacy runtime and backwards-compatibility support
- Status: APPROVED
- Baseline: SILOMATH_V6
- Changes frozen V6 baseline: YES
- Owner: Product Owner
- Requested by: Product Owner
- Created date: 2026-11-02

## Approval

- Decision: APPROVED
- Decision date: 2026-11-02
- Approved / rejected by: Product Owner (explicit user approval)
- Superseded by:

## Affected frozen baseline

### Documents

- 01_PRODUCT_BASELINE
- 02_DECISION_REGISTER
- 07_ARCHITECTURE
- 08_MODULE_ARCHITECTURE
- 09_DATA_MODEL
- 10_DATABASE_SCHEMA
- 12_OFFLINE_SYNC_SPECIFICATION
- 18_TEST_STRATEGY_ACCEPTANCE
- 20_PRODUCTION_OPERATIONS

## Current frozen contract

The current V6 baseline requires forward-safe Android/Room schema evolution, supported migration paths, migration regression coverage and transitional compatibility mechanisms where needed to preserve existing local installations and data.

## Requested delta

SiloMath Android supports only the current approved V6 runtime and current local schema.

Because there are no Production users and no Production local datasets requiring preservation, compatibility with historical Android/Room schemas is no longer required.

Legacy Room entities, tables, DAOs, repositories, Long-to-UUID shims, legacy_* fields, fallback logic, historical Room migration paths and tests whose sole purpose is upgrading unsupported historical Android installations may be removed.

The application must create the current canonical V6 local schema directly on a fresh installation.

## Reason

No Production users exist. Preserving compatibility with unused historical Android schemas adds runtime complexity, duplicate data models and defect risk without protecting real user data.

## Alternatives considered

Retaining historical Android migration support was rejected because there are no Production installations or local datasets requiring it.

## Impact analysis

### Business rules / lifecycle / workflows

No approved business rule, lifecycle or workflow semantic changes.

### UI / navigation / design

No UI or navigation behavior changes.

### Architecture / module boundaries

Remove transitional legacy Android runtime paths and retain only the current V6 module/runtime architecture.

### Data model / database

Android Room becomes current-schema-only. Legacy Android tables and compatibility columns may be removed.

This approval does not authorize removal of PostgreSQL migrations required for the current Supabase database history, replay, provenance or current Production schema.

### API / backend contract

No change to the Supabase canonical backend contract approved by FD-76.

### Offline / synchronization

Offline-first/local-first behavior remains mandatory for the current V6 schema. Historical Android schema compatibility is no longer required.

### Calculation engine

No mathematical or calculation contract change.

### Evidence / reporting

No evidence or reporting semantic change.

### Security / privacy

No security or privacy relaxation.

### Retention / production operations

No historical Android upgrade guarantee is required before first Production release.

## RVM impact

Remove or invalidate verification entries whose only purpose is Android historical-schema upgrade compatibility. Preserve all current V6 functional, offline, persistence and synchronization verification.

## Test impact

Remove historical Room migration regression tests that exist only for unsupported pre-V6/current-schema installations.

Retain tests proving creation, persistence, offline operation and synchronization of the current canonical V6 schema.

## Migration and compatibility impact

No backwards compatibility is required for historical Android/Room installations.

Fresh installation of the current application is the supported deployment model until Production users exist.

PostgreSQL/Supabase migration history remains outside this approved removal unless separately audited and approved.

## Release-gate impact

The Room historical migration regression gate may be replaced with a current-schema creation and persistence gate.

All other applicable V6 release gates remain authoritative.

## Approved delta

Approved: SiloMath Android is current-V6-only and has no requirement to preserve or upgrade historical Android/Room schemas or legacy local runtime models.

Legacy Android/Room compatibility code and tests may be removed when they are not required by current V6 functionality.

This approval does not authorize deletion of PostgreSQL/Supabase migration history required by the current canonical backend.

## Implementation verification

- Baseline documentation updated: PASS
- Requirement registry updated: PASS
- RVM updated: PASS
- Affected tests updated: PASS
- Applicable validators PASS: PASS
- CI PASS: PASS - GitHub Actions run 41445308308
- Implementation commit / PR: c5e2b257b02bfa55a4152715add6bd0a53bd72c5; follow-up CI fixes e4fc7480d535ecee7db306f8857be98102eceb2c and 4197294d054fe82f7d718a43af4fe78133df5aec
- Release evidence: PASS - Governance Validators, Android Build and Static Quality, Current V6 Room Schema Validation, and PostgreSQL Migration Validation all SUCCESS in GitHub Actions run 41445308308

## Authority guard

This Change Request is APPROVED and authorizes only the explicitly stated delta.

All unaffected requirements in Documents 01–21 remain authoritative.

