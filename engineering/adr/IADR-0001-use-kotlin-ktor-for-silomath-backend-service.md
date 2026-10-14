# IADR-0001 - Use Kotlin/Ktor for the SiloMath Backend Service

## Metadata

- ID: IADR-0001
- Status: ACCEPTED
- Owner: SiloMath
- Decision date: 2026-10-14
- Supersedes: None
- Superseded by: None

## Context

The frozen SiloMath V6 architecture requires Android-to-server communication
through a versioned HTTPS/REST backend boundary.

The canonical API specification defines `/api/v1` as the external backend
contract.

The repository already contains PostgreSQL migrations, Supabase-managed
infrastructure and some Supabase Edge Functions, but it does not contain a
standalone canonical backend service that owns the `/api/v1` boundary.

Android also still contains transitional direct Supabase/PostgREST access for
some business operations.

IMP-REP-002 requires creation of the backend repository foundation without
prematurely implementing the later API, authentication, master-data,
campaign, stocktaking, synchronization, reporting or production-operation
tasks.

The frozen V6 baseline specifies the architectural boundary and behavior but
does not prescribe a concrete JVM backend framework.

A repository implementation decision is therefore required for the backend
runtime and framework.

## Frozen baseline references

- 01_PRODUCT_BASELINE
- 02_DECISION_REGISTER
- 07_ARCHITECTURE
- 08_MODULE_ARCHITECTURE
- 09_DATA_MODEL
- 10_DATABASE_SCHEMA
- 11_API_SPECIFICATION
- 12_OFFLINE_SYNC_SPECIFICATION
- 17_SECURITY_AUDIT_PRIVACY
- 18_TEST_STRATEGY_ACCEPTANCE
- 20_PRODUCTION_OPERATIONS
- AR-NFR-06
- AR-NFR-12
- FD-52

## Decision

SiloMath will implement its canonical backend service using Kotlin on the JVM
with Ktor as the HTTP application framework.

The backend service will be repository-resident under the top-level
`backend/` directory and will remain separate from the frozen Android Gradle
module graph.

The backend service owns the canonical external `/api/v1` HTTP boundary.

Supabase may continue to provide compatible infrastructure capabilities,
including PostgreSQL, identity infrastructure and private object storage,
but provider-native REST, RPC or Edge Function URLs are not the canonical
SiloMath business API contract.

Android business operations must migrate incrementally toward the canonical
SiloMath `/api/v1` backend as the corresponding backlog tasks are
implemented.

This IADR does not authorize implementation of business endpoints outside
their dependency-aware backlog tasks.

The initial IMP-REP-002 backend foundation may establish only the
infrastructure required to prove that the backend repository and canonical
API boundary exist and are executable.

Framework-specific code must not redefine frozen request/response schemas,
authorization semantics, workflow behavior, data ownership, offline
behavior, calculation behavior, retention rules, security rules or release
gates.

## Alternatives considered

### Alternative A - Supabase Edge Functions as the canonical API surface

Supabase Edge Functions could host server-side business handlers.

This option was not selected as the canonical backend service foundation
because the provider-native function surface is not itself the frozen
SiloMath `/api/v1` contract and would require additional routing/gateway
mechanics to expose the canonical boundary consistently.

Edge Functions may still be used internally where a later implementation
decision and the frozen contract permit it.

### Alternative B - Direct Supabase/PostgREST from Android

The existing repository contains direct Android access to Supabase Auth and
PostgREST for several transitional flows.

This option was rejected as the target architecture because it bypasses the
canonical versioned backend boundary required by the frozen V6 architecture
and API specification.

Existing direct-provider paths are transitional implementation debt and must
be retired through their proper dependency-aware backlog tasks rather than
removed unsafely in IMP-REP-002.

### Alternative C - Spring Boot backend

Spring Boot can implement the required REST and PostgreSQL architecture.

It was not selected because Ktor provides the required HTTP/server
capabilities with a smaller framework surface while keeping the backend in
the same Kotlin language ecosystem already used by SiloMath.

This is an implementation trade-off only. The frozen V6 external behavior
would remain identical under either framework.

## Consequences

### Positive

- Kotlin remains the implementation language across Android and backend.
- The canonical `/api/v1` boundary becomes independently executable and
  testable.
- Backend implementation remains independent of the Android module graph.
- Existing PostgreSQL infrastructure can be retained where compatible.
- Existing Supabase infrastructure can be retained behind the canonical
  backend boundary where compatible.
- Later API tasks gain an explicit server-side location for authentication,
  authorization, validation, idempotency, concurrency, pagination, sync and
  business endpoints.
- API contract and integration testing can execute against an explicit
  backend service.

### Negative / trade-offs

- The repository gains an additional independently built JVM application.
- Backend dependency/version management must be maintained separately from
  the frozen Android module graph.
- Existing direct Supabase Android paths require incremental migration.
- Deployment and environment configuration for the backend must be completed
  under the appropriate later M1 and production-operations tasks.
- Ktor-specific implementation knowledge becomes part of repository
  maintenance.

## Verification impact

IMP-REP-002 verification must prove at minimum that:

- the `backend/` project builds independently;
- the backend process can start;
- the canonical `/api/v1` routing boundary exists;
- foundation endpoints used solely for service verification do not expose
  business data or secrets;
- configuration is externalized rather than embedding Production secrets;
- tests execute independently of Production services;
- the Android 19-module graph remains unchanged;
- existing governance validators remain PASS;
- the PostgreSQL V6 migration contract remains PASS.

Later tasks remain responsible for contract tests covering authentication,
authorization, standard error envelopes, correlation IDs, idempotency,
entity-version concurrency, pagination, sync and all business endpoints.

RVM mappings should be added only when concrete frozen requirements obtain
implementation and test evidence. This IADR does not invent verification
evidence for requirements that are not yet implemented.

## Baseline impact check

Changes frozen V6 baseline: NO

This decision selects an implementation framework for an already-frozen
backend boundary.

It does not alter the SiloMath V6 product contract.

If a future implementation proposal requires changing any frozen behavior,
endpoint contract, architecture boundary, workflow, status, data invariant,
offline rule, security rule or release rule, that change must use the
Baseline Change / Change Request process instead of this IADR.

## Notes

This IADR is the repository implementation decision supporting
IMP-REP-002 - Initialize Backend Repository.

The Android frozen module graph remains unchanged.

The backend is a separate repository-level application directory, not a new
Android production module.