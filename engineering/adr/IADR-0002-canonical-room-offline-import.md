# IADR-0002 — Canonical Room Offline Import with Preserved Legacy Data

## Metadata

- ID: IADR-0002
- Status: ACCEPTED
- Owner: SiloMath
- Decision date: 2026-10-27
- Supersedes: None
- Superseded by: None

## Context

The legacy Room geometry, measurement and stocktaking projections cannot represent the frozen V6 revision, exact decimal and reproducibility contracts. OFF-006 must not discard those facts or synthesize missing legacy provenance. The user explicitly approved EXPAND -> MIGRATE/RECONCILE -> CONTRACT, with no contraction in OFF-006.

## Frozen baseline references

01_PRODUCT_BASELINE section 18; 02_DECISION_REGISTER D29/D30/FD-69; 07_ARCHITECTURE AR-NFR-02/04/09/10; 08_MODULE_ARCHITECTURE; 09_DATA_MODEL; 10_DATABASE_SCHEMA sections 3/6/7/18; 12_OFFLINE_SYNC_SPECIFICATION sections 13–16/OS-D21–29; 17_SECURITY_AUDIT_PRIVACY; 18_TEST_STRATEGY_ACCEPTANCE.

## Decision

Use the existing SiloMathDatabase. EXPAND adds explicit typed canonical V6 tables using local `v6_` physical names to avoid collisions with retained legacy tables. Canonical UUIDs, entity versions, revision relationships, explicit geometry parameters and decimal TEXT are stored without routing through legacy entities or Double. These are transitional local physical names; server/API identities and semantics do not change.

Legacy tables, IDs, data, pending outbox and adapters are retained unchanged in this phase. No legacy value is implicitly promoted into canonical authority, including generic JSON geometry, REAL decimals or absent historical versions. MIGRATE/RECONCILE accepts only authoritative verified V6 input and explicitly resolved provenance; unresolved legacy work is retained and cannot be silently merged or overwritten.

A verified encrypted package is durable staging. Its decoded/validated import plan does not mutate operational data. Final canonical row reconciliation, current-manifest switch and READY transition must share one Room transaction, after compatibility and current user/organization/campaign/device authorization. Partial, corrupt, unauthorized, incompatible or conflicting imports never replace the current package. No DataStore offline-capability flag supplies authority.

No CONTRACT operation, legacy table rename/drop or destructive fallback is permitted in OFF-006. Production encryption and backup protections remain mandatory before enabling operational READY; this IADR does not relax them. Migration/import mechanics are owned by core:database/core:sync; auth consumes derived verified context.

## Alternatives considered

### Expand the legacy operational types in place

Not selected for this phase: legacy integer references, required root-based projections, Double values and generic JSON structures would remain intertwined with canonical revision writes and require wider concurrent adapter changes. They cannot supply missing provenance.

### Independent database or opaque business JSON cache

Rejected: it would create another operational authority or avoid the explicit frozen physical representation. The canonical schema remains in the same Room database with explicit fields.

## Consequences

### Positive

- Non-destructive forward migration and an explicit authority boundary.
- Exact canonical data remains independent of incomplete legacy representations.
- Interrupted import/refresh can recover from encrypted staging without partial activation.

### Negative / trade-offs

- Temporary storage duplication and explicit adapter reconciliation are required.
- Existing legacy readers are not silently redirected; canonical read/write adapters must be migrated deliberately.
- CONTRACT requires a later separately reviewed task after provenance, unresolved work and consumer migration have been verified.

## Verification impact

Test populated schema-25 forward migration, retained IDs/values/outbox, canonical UUID/FK/tenant/uniqueness constraints, exact decimal storage, immutable versions/revisions, staging isolation and rollback. Verify compatibility/authorization plus atomic activation and failed-refresh preservation before READY. Continue AUTH-010 only after its real prerequisites pass.

## Baseline impact check

Changes frozen V6 baseline: NO

## Notes

Accepted by explicit user approval in this session. This decision authorizes the migration architecture, not fabricated historical values or premature offline eligibility.
