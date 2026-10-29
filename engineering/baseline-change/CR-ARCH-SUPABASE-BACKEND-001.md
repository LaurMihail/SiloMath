# Change Request: CR-ARCH-SUPABASE-BACKEND-001

## Governance metadata

- CR ID: CR-ARCH-SUPABASE-BACKEND-001
- Title: Adopt Supabase as the sole canonical core backend platform
- Status: APPROVED
- Baseline: SILOMATH_V6
- Changes frozen V6 baseline: YES
- Owner: Product Owner
- Requested by: Product Owner
- Created date: 2026-10-29

## Approval

- Decision: APPROVED
- Decision date: 2026-10-29
- Approved / rejected by: Product Owner (explicit user approval)
- Superseded by: N/A

Only the explicitly approved delta below alters the frozen V6 baseline.

## Affected frozen baseline

### Documents

- 01_PRODUCT_BASELINE
- 02_DECISION_REGISTER
- 07_ARCHITECTURE
- 11_API_SPECIFICATION
- 12_OFFLINE_SYNC_SPECIFICATION
- 17_SECURITY_AUDIT_PRIVACY
- 18_TEST_STRATEGY_ACCEPTANCE
- 20_PRODUCTION_OPERATIONS

### Requirement / Decision IDs

- FD-41 REST and server authorization
- FD-42 Idempotent synchronized mutations
- FD-43 Atomic Close API
- FD-44 Server-generated Campaign Code
- FD-45 Offline package API
- FD-46 Evidence transfer
- FD-69 Implementation closure
- FD-71 Environment endpoint provisioning
- FD-75 Atomic Storage Space batch-create
- New decision: FD-76 Supabase canonical backend platform

The existing decisions above are preserved semantically unless this CR explicitly changes deployment topology or platform ownership.

## Current frozen contract

SiloMath V6 requires a REST-based backend boundary with PostgreSQL as enterprise-authoritative data storage, authoritative server-side authorization for protected operations, atomic and idempotent critical mutations, controlled offline synchronization, object storage for binary Evidence, and Development/Staging/Production operational controls. The current implementation has introduced a separate Kotlin/Ktor application backend and external compute/deployment service, but the frozen baseline does not require Render or Ktor by vendor name.

## Requested delta

Approve the following architecture contract:

> Supabase is the sole canonical backend platform for SiloMath application runtime. PostgreSQL is the authoritative enterprise datastore. All server-authoritative business operations are implemented using Supabase PostgreSQL functions and/or Supabase Edge Functions. Android may access Supabase services directly only where explicitly permitted by the API/security contract and RLS. Critical mutations remain server-authoritative, atomic and idempotent. No separate application backend or external compute service is required for core SiloMath business operations. External auxiliary services may be used only for non-core supporting capabilities explicitly approved by the baseline and must not become authoritative for core business state.

Additional constraints:

1. The frozen `/api/v1` semantic contract remains authoritative for ordinary REST resources and commands where Document 11 specifies it. Its implementation may be hosted by Supabase Edge Functions and/or delegated to transactional PostgreSQL functions without weakening HTTP/API semantics.
2. Direct Android-to-Supabase access is opt-in by contract, never assumed. Every direct path must be explicitly allowed, authenticated as applicable, tenant-scoped, RLS-protected, and must not bypass server-authoritative business guards.
3. PostgreSQL functions/RPC may implement data-centric atomic commands. Edge Functions may implement API orchestration, authentication mediation, validation, storage coordination and integrations. The choice between the two is an implementation detail only when all frozen semantics are preserved.
4. Supabase Auth is the canonical identity/authentication platform. Session restoration and authorization behavior remain governed by Documents 04, 11 and 17.
5. Supabase Storage is the canonical remote object-storage platform for SiloMath Evidence unless a future approved Baseline Change states otherwise. Evidence security, hashing, authorization and retention semantics remain unchanged.
6. Supabase Realtime may be used for non-authoritative change notification/refresh acceleration. Realtime delivery is never the source of truth and must not replace durable sync/idempotency/version rules.
7. Android remains offline-first/local-first. Room/SQLCipher remains the durable local operational read/write store for supported field operations and the durable outbox/SyncOperation model remains authoritative for queued offline work.
8. The Calculation Engine contract is unchanged by this CR.
9. Core SiloMath runtime business logic must not depend on a separate Render/Ktor service, AWS Lambda, Azure Functions, Cloud Run or equivalent external compute service.
10. GitHub/GitHub Actions, Google Play and other explicitly approved non-core build, distribution, observability or integration services are not made authoritative for core business state by this decision.

## Reason

SiloMath is a native Android multi-user application requiring centralized enterprise data, synchronization between users, strict server authority for protected mutations, and reliable offline-first field operation. Supabase already provides the authoritative PostgreSQL datastore plus Auth, RLS, Edge Functions, Storage and Realtime. A separate application backend adds an additional deployment, secret, network/JDBC, health, monitoring and failure domain without being required by the frozen product semantics.

The approved topology reduces operational complexity while preserving the stronger frozen guarantees: PostgreSQL authority, server-side authorization, atomicity, idempotency, versioning, tenant isolation, offline durability and auditable critical commands.

## Alternatives considered

### Keep Kotlin/Ktor on Render

Preserves a single-language Kotlin server/client ecosystem, but adds a separate runtime, JDBC connectivity, provider-specific deployment/health management, secret rotation and another failure domain. Rejected as the canonical target architecture because those costs are not required for current core SiloMath requirements.

### Android directly accesses Supabase tables/RPC for all operations

Rejected. It would make it too easy to bypass the canonical API/security boundary and server-authoritative guards. Direct access remains allowed only for explicitly approved paths protected by RLS and the API/security contract.

### Hybrid permanent topology: Supabase plus separate backend service

Rejected as the default target because it preserves the additional operational burden. A future external core compute dependency requires a separate approved Baseline Change.

## Impact analysis

### Business rules / lifecycle / workflows

- No approved business lifecycle, status, role, Close guard, Campaign rule, Stocktaking rule or Evidence rule changes.
- Critical business commands remain server-authoritative.

### UI / navigation / design

- No screen, navigation or visual-design change.

### Architecture / module boundaries

- Remove a separate application-backend runtime from the target architecture.
- Supabase becomes the sole canonical core backend platform.
- Android repository/network boundaries remain; implementation adapters target permitted Supabase endpoints/services.
- Existing Ktor backend code becomes transitional/superseded implementation and must not be treated as target architecture after refreeze.

### Data model / database

- PostgreSQL remains authoritative; no schema semantics are weakened.
- RLS, constraints and transactional PostgreSQL functions become first-class enforcement mechanisms.

### API / backend contract

- `/api/v1` semantic contracts remain frozen where specified.
- Supabase Edge Functions and PostgreSQL functions/RPC implement server-authoritative commands.
- Direct Android access requires explicit contract permission and RLS protection.

### Offline / synchronization

- Offline-first/local-first remains frozen.
- Room/SQLCipher and durable SyncOperation/outbox semantics remain.
- Synchronization targets Supabase-backed canonical operations; retries retain idempotency/version rules.

### Calculation engine

- No change.

### Evidence / reporting

- Supabase Storage becomes canonical remote object storage for Evidence.
- Final Snapshot/reporting authority remains PostgreSQL data governed by existing contracts.

### Security / privacy

- Supabase Auth is canonical identity platform.
- RLS is mandatory defense-in-depth for direct client-accessible data paths.
- Service-role/secrets remain server-only and must never ship in APKs, ordinary logs or client configuration.
- Edge Functions/PostgreSQL functions must enforce the same server-authoritative authorization and tenant boundaries as the superseded separate backend topology.

### Retention / production operations

- Development, Staging and Production remain distinct controlled environments.
- Supabase deployment/migration/function/storage/auth configuration is promoted through the frozen release process.
- Provider health, backup/PITR, restore, monitoring and incident evidence must satisfy Document 20.
- Separate Render/Ktor deployment/health/JDBC operational gates become obsolete after migration closure.

## RVM impact

- Register FD-76 and map architecture/API/security/operations verification to Supabase-only core runtime evidence.
- Preserve existing verification requirements for FD-41 through FD-46 and FD-75; change only their implementation evidence/provider topology.
- Add verification that no critical mutation bypasses approved server-authoritative Supabase paths.
- Add verification that no core business state depends on a separate external compute service.

## Test impact

Required additions/updates:

- Edge Function/API contract tests for canonical `/api/v1` commands.
- PostgreSQL function/RPC transaction and authorization tests for critical mutations.
- RLS tests for every direct Android-accessible path.
- Auth/session tests using Supabase Auth while preserving frozen behavior.
- Offline sync/idempotency/version tests against Supabase canonical operations.
- Evidence Storage authorization/hash tests against Supabase Storage.
- environment/deployment smoke tests for Development/Staging/Production Supabase contexts.
- regression proof that Close, Save & Launch, batch-create and other critical mutations remain atomic and server-authoritative.

Tests that prove only Ktor/Render/JDBC-specific mechanics cease to be baseline acceptance evidence after migration closure, although they may remain temporarily as transition tests until the old backend is removed.

## Migration and compatibility impact

- Migrate Android remote adapters from the separate Ktor service to approved Supabase endpoints/services incrementally.
- Implement/verify Edge Functions and PostgreSQL functions before removing equivalent Ktor paths.
- Preserve `/api/v1` payload/error/idempotency/version semantics for supported clients.
- Remove Render/Ktor runtime only after Supabase Staging passes equivalent contract, security, sync and smoke gates.
- No destructive PostgreSQL reinterpretation is authorized by this CR.
- Existing queued offline operations must remain replay-compatible or be migrated explicitly and verifiably before cutover.

## Release-gate impact

Before architecture migration is DONE:

1. Supabase Development configuration and tests pass.
2. Supabase Staging is provisioned and isolated.
3. PostgreSQL migrations and functions validate.
4. Edge Function/API contract tests pass.
5. RLS/authorization tests pass.
6. Offline sync/idempotency/version regression passes.
7. Critical atomic command regression passes.
8. Evidence Storage security/integrity regression passes.
9. Staging smoke test passes.
10. Only after equivalent Supabase coverage is green may the separate backend be decommissioned.

Production promotion continues to follow Build → Automated Tests → Migration Validation → Staging → Production → Smoke Test.

## Approved delta

APPROVED on 2026-10-29 by Product Owner:

- Supabase is the sole canonical backend platform for SiloMath application runtime.
- PostgreSQL remains the authoritative enterprise datastore.
- Server-authoritative business operations use Supabase PostgreSQL functions and/or Supabase Edge Functions.
- Android direct access is allowed only where explicitly permitted and RLS/API/security protected.
- Critical mutations remain server-authoritative, atomic and idempotent.
- No separate application backend/external compute service is required or canonical for core SiloMath business operations.
- External auxiliary services are permitted only for non-core supporting capabilities and cannot become authoritative for core business state without an approved Baseline Change.
- All unaffected V6 frozen requirements remain authoritative.

## Implementation verification

- Baseline documentation updated: PENDING IN THIS CHANGESET
- Requirement registry updated: PENDING REGENERATION/VALIDATION
- RVM updated: PENDING REGENERATION/MAPPING
- Affected tests updated: PENDING IMPLEMENTATION MIGRATION
- Applicable validators PASS: PENDING CI
- CI PASS: PENDING CI
- Implementation commit / PR: PENDING
- Release evidence: PENDING SUPABASE STAGING MIGRATION

## Authority guard

This CR is APPROVED and authorizes only the bounded architecture delta stated above. It does not authorize changes to product statuses, business workflows, roles, calculation semantics, offline durability guarantees, historical integrity, security weakening, or API behavior outside the explicit platform/topology delta.

All unaffected requirements in Documents 01–21 remain authoritative.