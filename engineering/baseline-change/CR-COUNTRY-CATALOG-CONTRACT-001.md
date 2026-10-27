# Change Request: CR-COUNTRY-CATALOG-CONTRACT-001

## Governance metadata
- Title: Canonical ISO 3166-1 Country catalogue and alpha-2 contract
- Status: APPROVED
- Baseline: SILOMATH_V6
- Changes frozen V6 baseline: YES (approved Baseline Change / Clarification)
- Owner: SiloMath Product Owner
- Requested by: Product Owner
- Created date: 2026-10-27

## Approval
- Decision: APPROVED by explicit user instruction in this conversation.
- Decision date: 2026-10-27
- Approved by: Product Owner.
- Approval source: current user message explicitly approving the complete Country contract as FROZEN V6. No separate attachment or external approval is fabricated.
- Superseded by: None.

## Affected frozen baseline
Material changes/cross-references: Docs01/02/03/06/09/10/11/12/16/18/19/20. All21 official files receive the same controlled refreeze revision marker; the remaining9 bodies are unchanged. Next available existing IDs: FD-74, BR-MD-014, TA-D233. The existing CR namespace is reused; no new identifier scheme.

## Current frozen contract and approved delta
Previously Doc06 required a Country selector with supported values, while Doc09 called Country a code/value without identifying its catalogue/encoding. The approved canonical catalogue is the complete ISO 3166-1 list. Persisted and API Country values are uppercase ISO 3166-1 alpha-2 codes. Examples: Romania=RO; Germany=DE; United States=US. Human-readable country names are presentation labels, not the canonical stored/transmitted value.

Selectors do not permit free text for new Country values. New values must be members of the catalogue and are validated fail-closed. Selector options are available locally/cached for local-first/offline-first use. Existing optional/null field semantics remain unchanged. No catalogue revision/version identifier, polling interval or subset is invented.

Already persisted non-conforming values are not automatically changed, uppercased, mapped from names, truncated, cleared or reinterpreted. They require a separate audit before any migration. This approval contains no data migration, DDL execution, backfill or authorization to mutate external environments.

## Reason and alternatives
Resolve an explicitly confirmed baseline gap without inventing Production values under an implementation task. Not approved: arbitrary country subsets, free-text new values, country-name persistence as canonical value, network-only selectors or implicit legacy normalization. This is an approved product-contract clarification, not an IADR.

## Impact analysis
- Business/UI: shared Country semantics for Organization and Location, including ORG02-05 and P1SH01-05. No new screen, role, lifecycle or permission.
- Data/API: existing country field names remain; new values use canonical alpha-2 uppercase and catalogue membership. Display labels are not substituted into API/storage.
- Offline/architecture: complete options local/cached under existing module ownership. No new Country entity/module/endpoint, package-manifest item or OFFLINE_READY gate is mandated.
- Database/migration: preserve historical non-conforming storage; do not infer a narrowing DDL or implicit backfill from the new-value contract.
- Calculation: F01-F10, exact approved version tuple and mathematical semantics remain unchanged. snapshotSchemaVersion remains UNRESOLVED and is not inputSchemaVersion.
- Reporting/retention: preserve actual stored historical values and immutable snapshots; no automatic country reinterpretation.
- Security/operations: existing tenant/role/TLS/backup/retention rules unchanged; catalogue availability and aligned validation require implementation/release evidence later.

## RVM impact
Register FD-74, BR-MD-014 and TA-D233 through regenerated source/verification registries. Runtime acceptance remains unverified until implemented and tested; documentation checks are not UI/API implementation PASS.

## Test impact
Planned runtime coverage: RO/DE/US code/label separation; complete catalogue membership; no free-text selector; non-catalog/noncanonical new values rejected; offline/local options; unchanged optionality; preservation of non-conforming historical data before separate audit/migration. Current refreeze runs document/registry/hash/reference and mathematical-regression consistency gates only.

## Migration and compatibility impact
No historical audit has been performed by this refreeze. No Room schema version change, SQL migration, data conversion, Supabase mutation or external-provider configuration is authorized/executed here.

## Release-gate impact
This approval does not close P1-SH-01, ORG-02, MD-002 or any milestone. Runtime Country implementation/verification remains required. Existing AUTH-010/READY/M3 and snapshot-schema boundaries are untouched.

## Approved delta
Only the Country catalogue/encoding/label/selector/new-write validation/local-availability/historical-preservation contract above. No implied ISO catalogue data snapshot, label alias policy or historical migration is approved.

## Implementation verification
See engineering/task-evidence/CR-COUNTRY-CATALOG-CONTRACT-001.md and the new COUNTRY refreeze manifest/validation evidence. Prior refreeze manifests/evidence remain unchanged.
