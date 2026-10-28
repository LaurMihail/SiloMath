# Documentation reconciliation and MD-005 baseline clarification refreeze

Status: GREEN / DONE. Date: 2026-10-28.

## Scope and authority

Task: reconcile current SiloMath documentation and formalize the Product Owner-approved IMP-MD-005 Storage Space batch-create clarification. Authority order: V6 APPROVED/FROZEN Doc01, Doc02, affected specialist documents, approved CRs, IADRs, implementation/evidence.

No product semantic was reopened. The only frozen delta is the already-approved atomic all-or-nothing batch-create contract recorded in `engineering/task-evidence/IMP-MD-005.md`.

## Formalized decision

- Change Request: `CR-API-STORAGE-SPACE-BATCH-001`, APPROVED.
- Frozen decision: `FD-75`.
- Acceptance requirement: `TA-D234`.
- Refreeze date: 2026-10-28.
- Manifest: `governance_evidence/SiloMath_V6_FROZEN_MANIFEST_2026-10-28_BATCH.json`.
- Validation evidence: `governance_evidence/SiloMath_V6_Refreeze_Validation_2026-10-28_BATCH.json`.
- Refreeze record: `governance_evidence/SiloMath_V6_Baseline_Refreeze_Record_2026-10-28_BATCH.md`.

One `storage-spaces:batch-create` request is one logical mutation intent. Complete validation precedes commit; any mandatory failure rolls back all rows and business/audit/journal/idempotency/outbox effects. Exact-key/same-payload replay is safe; same-key/different-payload reuse conflicts. Partial success is prohibited.

## Baseline application

All 21 baseline documents received the established shared 2026-10-28 revision marker. Material text changed only in:

- Doc02: FD-75 and approved-contract cross-reference.
- Doc11: removed stale `Candidate` metadata and made the existing `/api/v1` endpoint explicitly atomic all-or-nothing.
- Doc12: local business/outbox atomicity and retry/ACK no-partial-success invariant.
- Doc18: TA-D234 verification contract.

Docs03/05/07/09/10/17/20 were inspected and required no redundant semantic addition. Existing general authorization, transaction, data, security and operations rules remain unchanged.

## Governance and repository documentation

- `AGENTS.md`: current V6 authority chain, CR/refreeze rule, Navigation 3, backend/Room authority split and Doc12 invariants.
- `README.md`: current repository orientation without stale registry counts or CI-PENDING claims.
- Baseline change register: four approved CR records; the prior three were not modified semantically.
- Baseline registry: current 2026-10-28 manifest/validation references and new hashes.
- Existing historical manifests/refreeze records, CRs, IADRs and task evidence remain untouched.

## Requirement registry and RVM

The new decision introduces two actual source IDs: FD-75 and TA-D234. The controlled builders regenerated:

- requirement registry: 2207 rows / 1845 unique source IDs;
- RVM: 1845 rows / 21 mapped / 3 verified / 1821 unmapped.

Only the committed IMP-MD-005 tests `batchCreateIsAtomicAndIdempotent` and `auditFailureRollsBackBusinessAndIdempotencyRecord` are mapped to FD-75/TA-D234. Evidence: `engineering/task-evidence/IMP-MD-005.md`; implementation commit: `d9aaaff6a9d1ebe1c2782a91cefa8cbe342e8630`. No test was rerun and no verification was invented during this documentation task.

The registry builder/validators were extended mechanically from FD-74/TA-D233 to FD-75/TA-D234.

## IMP-MD-001 evidence decision

No dedicated evidence file was created. Repository provenance provides downstream acceptance references and a checkpoint prefix, but no sufficient exact executed commands/results from IMP-MD-001. A dedicated record cannot be reconstructed safely without fabricating evidence. This absence does not block the refreeze.

## Files changed

- `AGENTS.md`, `README.md`.
- `baseline/01_PRODUCT_BASELINE.md` through `baseline/21_UI_DESIGN_SYSTEM_FINAL.md`.
- `engineering/baseline-change/CR-API-STORAGE-SPACE-BATCH-001.md` and `.json`.
- `engineering/baseline-change-register.json`, `engineering/baseline-registry.json`.
- `engineering/build-requirement-registry.ps1`, `engineering/validate-baseline-registry.ps1`, `engineering/validate-requirement-registry.ps1`.
- `engineering/requirement-id-registry.json` and `.csv`.
- `engineering/rvm-mappings.json`, `engineering/rvm.json`, `engineering/rvm.csv`.
- the three new 2026-10-28 governance-evidence artifacts listed above.
- this task evidence.

IDE state, logs, temporary files and unrelated changes are excluded.

## Validators executed

Controlled builders:

- `build-requirement-registry.ps1`: PASS after extending the frozen FD range to FD-75.
- `build-rvm.ps1`: PASS.

Applicable validators:

- baseline registry: PASS, 21/21.
- baseline change process: PASS, 4 registered approved CRs.
- ADR/IADR process: PASS, 17 frozen ADRs reserved and 4 IADRs registered.
- requirement registry: PASS, 2207 rows / 1845 unique IDs.
- RVM: PASS, 1845 total / 21 mapped / 3 verified / 1821 unmapped.
- PostgreSQL V6 contract: PASS, 68 migrations / 38 V6 / 0 violations.

Module validation is not applicable: no module inventory, dependency or implementation source changed.

Final diff checks and the final post-evidence validator pass are recorded before commit.
