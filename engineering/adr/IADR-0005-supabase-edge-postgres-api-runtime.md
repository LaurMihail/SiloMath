# IADR-0005 — Use Supabase Edge Functions and PostgreSQL Functions for the V6 API Runtime

## Metadata

- ID: IADR-0005
- Status: ACCEPTED
- Owner: SiloMath
- Decision date: 2026-10-29
- Changes frozen V6 baseline: NO
- Superseded by: None

## Context

CR-ARCH-SUPABASE-BACKEND-001 / FD-76 approved Supabase as the sole canonical backend platform for SiloMath application runtime. It supersedes the separate Kotlin/Ktor application backend and Render as canonical core runtime while preserving the frozen `/api/v1`, authorization, atomicity, idempotency, entity-version, offline-first and historical-integrity contracts.

The implementation still requires a concrete technical boundary between Android, HTTP orchestration and authoritative database operations. The frozen baseline permits Supabase Edge Functions and PostgreSQL functions to host that boundary without changing product semantics.

## Frozen baseline references

- 01_PRODUCT_BASELINE
- 02_DECISION_REGISTER — FD-76
- 07_ARCHITECTURE — CR-ARCH-SUPABASE-BACKEND-001
- 11_API_SPECIFICATION
- 12_OFFLINE_SYNC_SPECIFICATION
- 15_EVIDENCE_STORAGE_SPECIFICATION
- 17_SECURITY_AUDIT_PRIVACY
- 18_TEST_STRATEGY_ACCEPTANCE
- 20_PRODUCTION_OPERATIONS

## Decision

1. Supabase Auth is the canonical identity and session authority.
2. Supabase Edge Functions host the versioned `/api/v1` HTTP boundary where HTTP orchestration is required. The `api-v1` function is the canonical HTTP adapter, not an independent business datastore.
3. PostgreSQL in the private canonical `silomath` schema is the authoritative enterprise datastore. Critical mutations, authorization-sensitive commands, atomic multi-entity operations, idempotency and entity-version enforcement are implemented by server-authoritative PostgreSQL functions and/or Edge Function orchestration exactly as permitted by FD-76 and Documents 10–12 and 17.
4. Supabase Storage is the canonical remote Evidence object-storage platform. Storage authorization and object metadata must follow Documents 15 and 17.
5. Android does not use direct PostgREST table access as a general business API. Direct Supabase access is permitted only where Documents 11 and 17 explicitly authorize the path and the required authorization/RLS controls are enforced.
6. No standalone Ktor backend or Render service is part of the current canonical runtime. Supabase is the sole canonical backend platform for core application runtime.
7. Android remains offline-first/local-first. Room and the durable outbox retain the frozen operational behavior; Supabase does not replace local field operation.

This decision selects implementation topology only. It does not add, remove or reinterpret any frozen product behavior, endpoint semantics, lifecycle status, business rule or offline/sync invariant.

## Alternatives considered

- **Separate Kotlin/Ktor service on Render:** rejected because FD-76 explicitly supersedes a separate application backend as canonical core runtime.
- **Direct Android PostgREST business-table access:** rejected as a blanket API replacement because Documents 11 and 17 permit direct access only for explicitly authorized paths and do not waive server-authoritative mutation, tenant authorization, idempotency or atomicity.
- **Edge Functions without PostgreSQL transactional functions for critical mutations:** rejected where it would split or weaken database atomicity, concurrency, idempotency or authoritative invariants.

## Verification impact

- Verify `/api/v1` route and DTO parity against Document 11.
- Verify PostgreSQL authorization, tenant isolation, atomicity, idempotency, entity-version and immutability contracts.
- Verify offline manifest/package and sync batch/status/pull behavior against Document 12, including retry and conflict semantics.
- Verify Android Auth/session, network/repository wiring and offline-first behavior.
- Verify only the current canonical Supabase runtime against the approved API, security, offline/sync and operational contracts.

## Consequences

- Supabase Auth, PostgreSQL, Edge Functions and Storage form the canonical backend platform.
- `/api/v1` contract parity and PostgreSQL transaction/security tests are required for the current canonical Supabase runtime.
- Superseded standalone backend implementation artifacts are not part of the current canonical repository state.
- Supabase migrations remain forward-only and historical migration files/evidence are preserved.
- This ADR defines implementation topology without changing the approved V6 baseline.