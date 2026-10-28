# IMP-MD-005 — Implement Storage Space Repository / API

Status: GREEN / DONE. Date: 2026-10-28.

## Authority and clarification

Authority consulted for the implemented contract: V6 APPROVED/FROZEN 01_PRODUCT_BASELINE, 02_DECISION_REGISTER, 03_BUSINESS_RULES, 04_USER_ROLES_PERMISSIONS, 07_ARCHITECTURE, 08_MODULE_ARCHITECTURE, 09_DATA_MODEL, 10_DATABASE_SCHEMA, 11_API_SPECIFICATION, 12_OFFLINE_SYNC_SPECIFICATION, 17_SECURITY_AUDIT_PRIVACY and 18_TEST_STRATEGY_ACCEPTANCE. The canonical hierarchy remains Organization → Location → Storage Unit → Storage Space. Canonical UUID identity, immutable canonical parent, Admin-only mutations, Standard read access, tenant isolation, Active/Inactive lifecycle, optimistic concurrency and retained historical references are enforced.

Product Owner approved the following Baseline Clarification / Change Request during IMP-MD-005: Storage Space batch-create is one logical intent and is atomic all-or-nothing. Every item is validated before commit. Any validation, uniqueness, hierarchy, authorization or invariant failure rolls back all rows and side effects. Exact-key replay is safe; reuse with a different payload is an idempotency conflict. No other frozen rule or baseline file changed.

The canonical read-only prerequisite from commit 979b15a096c013ebc5b931b363619eef8b8c9940 is reused. No numeric legacy fallback, fabricated legacy mapping or ambiguous-row promotion was introduced.

## Implemented scope

- Domain repository boundary exposes canonical Unit-scoped observation, details, save, refresh, history check and synchronization behavior.
- Room remains the local read authority. Canonical UUID PK/parent/tenant lineage, entityVersion and unique Unit×Code index are represented in schema 27. The 26→27 auto-migration is additive. Local save validates Organization→Location→Unit→Space and Geometry Profile scope, fixed parent, uniqueness, lifecycle, version and historical-reference rules.
- Local business row and durable SyncOperation/outbox commit in the same Room transaction. Failed outbox insertion rolls back the business row. Offline work retains stable operation/idempotency identity, same-entity ordering, dependencies, retry metadata, explicit conflicts and ACK reconciliation.
- Retrofit DTO/API/source and application wiring provide list/get/create/patch, atomic batch-create and existing sync-batch integration under /api/v1.
- Ktor routes require bearer authentication and Organization authorization. Admin may mutate; Admin and Standard may read. JDBC revalidates active Organization/User/Admin membership, hierarchy, parent/profile tenant scope, UUIDs, optimistic base version and idempotency.
- Single server mutations commit Storage Space, acknowledged journal and audit together. Atomic batch-create validates the full payload, then commits all Storage Spaces, audit effects and one logical idempotency journal record in one PostgreSQL transaction. Duplicate items, invalid items or injected audit failure leave no partial rows or journal.
- PostgreSQL migration 20261028150000_v6_storage_space_contract_guards.sql adds canonical composite lineage/FK checks, Unit×Code constraints, immutable identity/parent/creation guards and historical-reference protection with non-destructive RESTRICT behavior.
- The create-path eager fallback bug was corrected in JdbcStorageSpaceService: geometryProfileId now uses an explicit present-field branch, so CREATE no longer evaluates the PATCH fallback failure.

## Geometry fixture blocker and correction

Doc10 and migrations 20261018001000, 20261018003000 and 20261025001000 were audited. The hardened schema has immediate RESTRICT FKs from geometry_profiles.current_version_id to geometry_profile_versions.geometry_profile_version_id and from geometry_profile_versions.geometry_profile_id to geometry_profiles.geometry_profile_id. NOT VALID still enforces new writes.

The initial JDBC fixture attempted to insert a new Profile/Version pair after hardening and failed in setup. The corrected StorageSpacePostgresTest reuses the controlled, already-valid Profile/Version graph loaded by supabase/tests/migration/011_md003_historical_reference_fixture.sql before FK hardening. Setup verifies the matching Profile/current Version/Organization relation, restores the fixture membership to Admin, and creates a unique canonical Location/Storage Unit for each test. It does not modify Geometry schema, relax constraints, insert null pointers, or invent Geometry/calculation behavior. Campaign history data uses canonical code and required launch metadata. Fault injection uses the per-test Unit UUID to remain collision-free.

## Verification

Executed test results: 27 PASS, 0 failures, 0 errors, 0 skipped.

- Domain Storage Space contract: 5 PASS.
- Real Room/repository Storage Space suite: 7 PASS.
- Backend contract/route suite: 9 PASS.
- Real PostgreSQL/JDBC StorageSpacePostgresTest: 6/6 PASS. Covers CRUD projection, version increment/stale conflict, idempotent replay and mismatch, duplicate code, wrong parent/profile/tenant, Standard denial, historical immutability/lifecycle, transaction rollback, and atomic/idempotent batch-create.
- PostgreSQL standalone replay: 68 migrations PASS; official migration and tenant-isolation assertions PASS.
- Android Production compile: PASS at the accepted checkpoint; no Android production source changed after that PASS.
- Final backend compile: PASS after the last backend correction.

Commands executed for the final continuation:

```powershell
$env:JAVA_TOOL_OPTIONS='-Dsilomath.md005.jdbc=jdbc:postgresql://127.0.0.1:55483/silomath_md003_md005_finalgate3?user=md003_test'; .\gradlew.bat -p backend :test --tests StorageSpacePostgresTest --rerun-tasks
.\gradlew.bat -p backend :compileKotlin
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\engineering\validate-module-dependencies.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\engineering\validate-postgres-v6-contract.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\engineering\validate-baseline-registry.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\engineering\validate-baseline-change-process.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\engineering\validate-adr-process.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\engineering\validate-requirement-registry.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\engineering\validate-rvm.ps1
```

Final validators: module graph PASS (19/19 modules, 65 project dependencies, 0 violations); PostgreSQL contract PASS (68 migrations, 38 V6, 0 violations); baseline registry PASS (21/21); baseline change process PASS (3 registered approved requests); ADR/IADR process PASS (17 reserved frozen ADRs, 4 registered IADRs); requirement registry PASS (2202 rows, 1843 unique source IDs); RVM governance PASS (1843 IDs, controlled mapping, no invented evidence).

## Legacy and task boundaries

Legacy numeric tables and unreconciled rows remain retained and are neither deleted nor treated as canonical authority. No dual-write, legacy contraction, destructive history rewrite, live Supabase operation, dependency upgrade or calculation-engine change was made. PostgreSQL tests use only the disposable loopback cluster and dedicated replay database.

IMP-MD-006 remains separate: no complete Storage Space UI, P1-SH-03 implementation or UI workflow is included. Global sync ownership outside the existing Storage Space integration and legacy reconciliation remain outside this task.

## Atomic commit scope

The eventual commit contains only MD-005 domain, Room schema/DAO/repository, network contract/source/wiring, backend route/service/sync dispatch, PostgreSQL migration, targeted tests, generated Room schema 27 and this evidence. IDE state, logs, temporary/disposable PostgreSQL data and agent artifacts are excluded.
