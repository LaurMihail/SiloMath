# Change Request: CR-API-STORAGE-SPACE-BATCH-001

## Governance metadata
- Title: Atomic all-or-nothing Storage Space batch-create contract
- Status: APPROVED
- Baseline: SILOMATH_V6
- Changes frozen V6 baseline: YES (approved Baseline Change / Clarification)
- Owner: SiloMath Product Owner
- Requested by: Product Owner
- Created date: 2026-10-28

## Approval
- Decision: APPROVED by explicit Product Owner instruction during IMP-MD-005.
- Decision date: 2026-10-28
- Approved by: Product Owner.
- Approval provenance: `engineering/task-evidence/IMP-MD-005.md`, Authority and clarification section, records the explicit approval and the implemented/tested contract.
- Superseded by: None.

## Affected frozen baseline
Material changes/cross-references: Docs02/11/12/18. Doc01 remains overall authority and all 21 official files receive the same controlled refreeze revision marker. Docs03/05/07/09/10/17/20 were inspected: their existing general atomicity, authorization, transaction, data, security and operations rules remain sufficient and no redundant semantic text is added.

New frozen decision identifier: FD-75. New acceptance identifier: TA-D234. No existing identifier is renumbered or reinterpreted.

## Current frozen contract and approved delta
Doc11 already defines `POST /api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces:batch-create`, but its transaction semantics were not explicit enough to distinguish atomic execution from partial success.

The approved contract is:

- one `storage-spaces:batch-create` request is one logical mutation intent;
- validate the complete batch before commit;
- any validation, uniqueness, hierarchy, authorization, Organization scope or other mandatory invariant failure fails the complete batch;
- on failure, zero Storage Space rows and zero partial business, audit, journal, idempotency or outbox/SyncOperation effects from that request are committed;
- successful execution commits the complete batch atomically;
- exact replay with the same idempotency key and same canonical payload is safe and does not create duplicate effects;
- reuse of the same idempotency key with a different payload is an idempotency conflict;
- partial-success semantics are prohibited.

Where the batch is represented locally for supported offline/local-first work, the complete local business mutation and its durable outbox/SyncOperation effects share one Room transaction. A failed batch leaves no partial local business state or partial outbox.

No other V6 behavior changes.

## Reason and alternatives
This formalizes the Product Owner clarification already approved and implemented during IMP-MD-005. It removes an ambiguity in the existing endpoint contract without changing the endpoint, DTO identity model, authorization model, hierarchy, optimistic concurrency, lifecycle or ordinary single-create semantics.

Not approved: per-item partial success, best-effort commits, silent omission of failed elements, a new endpoint/version, weakening authorization, or generating independent idempotency identities for partial effects.

## Impact analysis
- Product/workflow: one batch request remains one logical intent; no new role, state, screen or workflow.
- API: additive clarification within existing `/api/v1`; no route or DTO break.
- Authorization/scope: existing Admin-only mutation and Organization/parent validation remain mandatory for every element.
- Data/database: no new entity, field, table, migration or compatibility mapping.
- Offline/sync: no parallel sync system; supported local representation must preserve one atomic business/outbox boundary.
- Idempotency/concurrency: existing key and optimistic-concurrency rules are preserved; exact replay is safe and key/payload mismatch conflicts.
- Audit/history: failed batches emit no partial audit/journal effects; successful batches retain the existing audit contract.
- Security/privacy/retention/operations: no semantic change beyond fail-closed atomic application of existing controls.
- Compatibility: backward-compatible clarification of an existing v1 endpoint. No live data migration, DDL, backfill or Supabase mutation is authorized.

## Requirement and RVM impact
FD-75 and TA-D234 are new frozen source IDs. Regenerate the requirement registry through the existing controlled builder. Map only actual existing verification: the IMP-MD-005 PostgreSQL/JDBC batch-create and rollback tests, with evidence `engineering/task-evidence/IMP-MD-005.md` and implementation commit `d9aaaff6a9d1ebe1c2782a91cefa8cbe342e8630`. Do not infer unrelated verification.

## Test impact
Verification must cover:
- complete success;
- invalid/duplicate/wrong-parent/wrong-scope/unauthorized element causes complete rollback;
- zero partial business/audit/journal/outbox effects;
- exact-key replay without duplicates;
- same-key/different-payload conflict;
- transaction fault rollback.

Existing IMP-MD-005 evidence records 6/6 real PostgreSQL/JDBC tests, including atomic/idempotent batch-create and injected audit-failure rollback.

## Baseline application
Apply the exact delta to FD-75 in Doc02, the endpoint and atomic transaction contract in Doc11, the local/outbox invariant in Doc12, and TA-D234 in Doc18. Refreeze all 21 documents through the established shared revision marker and a new manifest/validation record. Preserve all earlier manifests, CRs, IADRs and historical evidence unchanged.
