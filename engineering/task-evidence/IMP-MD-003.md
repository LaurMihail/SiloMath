# IMP-MD-003 — Storage Unit Repository / API

Status: GREEN / DONE. Date: 2026-10-28. Commit: the single atomic local commit containing this evidence. No push or live Supabase mutation.

## Authority and dependency boundary

Authority consulted in requested order: V6 APPROVED/FROZEN 01_PRODUCT_BASELINE; 02_DECISION_REGISTER (including FD-74, refreeze ccfa3a0); Docs03/09/10/11/12; Docs07/08 ownership; audited graph/evidence; current implementation. Doc09 section4.5 defines StorageUnit and mandatory metadata; Doc03 BR-MD-001/002 fixes hierarchy/lifecycle/history; Doc10 fixes UUID/FK/tenant/version/audit and section18 forward-safe staged migration; Doc11 fixes the four Storage Unit endpoints, common mutation headers and sparse PATCH semantics; Doc12 sections6/7/10/12/20 fixes durable journal, dependencies, reconciliation and retry. IADR-0002 governs reuse of existing typed canonical Room tables, explicit provenance and retained legacy structures. Existing IADR-0003 Production encryption is unchanged. No new baseline, status, business field, endpoint, version identifier, Room version/schema export or architectural execution-order exception.

MD-001 is accepted DONE. MD-002 is DONE at d74ccc77bb464e075842f1801cb6430b51f21593. The audited graph keeps MD-003 after MD-001 + MD-002. Original workbook and dependency edges are unchanged. MD-004 is not started.

## Audit gaps and implemented scope

- Domain StorageUnit was already substantially canonical; retained immutable UUID/Location identity, Name/Code/Description bounds, ACTIVE/INACTIVE and version metadata. Closed the missing trimmed-Name invariant. Business Code remains separate from UUID identity, uppercase and unique per Location.
- Replaced this repository's numeric legacy StorageDao write path with existing typed v6_storage_units plus a scoped canonical DAO. Existing v6 composite Organization/Location FK and UUID primary key are reused; there is no new Room schema migration. Legacy tables, numeric IDs, remoteId columns and existing data are retained physically but do not supply operational fallback or fabricated canonical facts.
- Room is the read authority. A single authorized, revocation-aware Flow exposes only the active authorized Organization/Location; it remains observable even before the first parent/child appears. Admin saves commit business row + SyncOperation + required parent dependency atomically. Standard reads and cannot write. New UUID is preserved; CREATE base version is null; PATCH retains authoritative base version. No network participates in local save. Local actor/event timestamps are provisional facts; API requests do not assert server timestamps or creation metadata.
- Parent Organization/Location may be established from complete authorized V6 API responses. The narrow local-parent bridge requires an already verified canonical Organization, a captured unresolved canonical LOCATION CREATE journal in that Organization, exact UUID/actor match and complete metadata. It never upgrades incomplete legacy rows by guessing. Parent bridge, child and outbox share the same rollback boundary. Retained Location projection is not deleted.
- Explicit refresh fetches all cursor pages and validates identity/provenance/metadata. Clean newer rows are reconciled transactionally; dirty rows are never overwritten or generically merged. A newer server version over dirty data creates durable OPEN SyncConflict with attempted and authoritative contexts. Missing list rows do not authorize deletion. Suspended responses are discarded after authorization changes.
- Added canonical transfer model, Retrofit DTOs/API and authenticated remote source. Frozen routes: GET/POST /api/v1/organizations/{organizationId}/locations/{locationId}/storage-units; GET/PATCH /api/v1/organizations/{organizationId}/storage-units/{storageUnitId}. POST does not invent a client entity-ID field. Sparse PATCH preserves absent fields and explicit null clears Description. No DELETE/reassignment endpoint.
- Offline creation uses the existing frozen POST /api/v1/sync/batch contract, carrying stable entityId and the canonical CREATE parent in payload. This owner handles STORAGE_UNIT only; other entity owners return an explicit retryable unavailable result rather than an invented implementation. It does not claim completion of the global sync scheduler/pull/worker backlog or infer READY.
- Owner synchronization is serialized by the singleton repository run lock. FIFO is strict per entity, dependencies must be ACKNOWLEDGED, operation/key/base identities are retained on replay, and capturing actor cannot silently change. IN_FLIGHT/attempt metadata is persisted before dispatch; interrupted IN_FLIGHT is replay-safe. ACK plus authoritative read reconciliation is atomic. Lost ACK/read failure retains the same operation. Conflicts/permanent errors block later same-entity work and are never purged or silently rebased. Retry scheduling uses the existing frozen 2/5/15/30/60/300-second capped/jitter policy; Retry-After is preserved and takes precedence. 401 refresh remains the existing one-retry executor; 403/422 cannot become a blind network retry loop. This entity-owner adapter does not change any other auth flow.
- Ktor routes isolate guards by endpoint and HTTP method: ACTIVE bearer + authorized Organization membership, Admin mutations, Admin/Standard reads. JDBC rechecks active User/Organization/Admin membership in the transaction and verifies device binding for batch. SQL is parameterized. Tenant/parent scope, field normalization, limits, UUIDs and optimistic base versions fail closed. Request cannot override server/immutable/legacy fields.
- Server mutation, authoritative UTC timestamps/actors/version, ACK SyncOperation, dependency edges and append-only before/after AuditEvent commit together or roll back. Version trigger remains the sole increment authority. Concurrent same-base updates yield exactly one success. Idempotency uses transactional locking and a canonical fingerprint (actor/tenant/parent/entity/operation/base/fields/event/device/dependencies), returning the original semantic response on exact replay and 409 on mismatch.
- CLI-generated versioned migration 20261028081616_v6_storage_unit_contract_guards.sql adds validated composite parent scope/field checks and guards immutable UUID/parent/creation provenance plus historically referenced Code (snapshot, CampaignScope and Stocktaking lineage). Existing historical FK RESTRICT behavior is preserved. No data normalization/backfill, business reinterpretation, destructive cascade, legacy contraction or live DDL execution.

## Executed final verification

All following commands ran in C:\DEVELOPER\NativeAndroid\SiloMath. Gradle reused unchanged tasks where applicable. Exact per-suite counts and report SHA-256 are recorded in IMP-MD-003-validation.json; no raw build output or database data is committed.

```powershell
.\gradlew.bat :core:domain:testDebugUnitTest --tests '*StorageUnitContractTest' --tests '*MasterDataModelsTest' :app:testProductionDebugUnitTest --tests '*StorageUnitTransportTest' :app:compileProductionDebugKotlin
.\gradlew.bat :app:connectedProductionDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.data.repository.StorageUnitRepositoryTest' :app:compileProductionDebugKotlin
.\gradlew.bat -p backend :compileKotlin :test --tests 'com.silomath.backend.storageunit.*' '-Pmd003JdbcUrl=jdbc:postgresql://127.0.0.1:55483/silomath_md003_release2?user=md003_test'
.\engineering\test-storage-unit-postgres.ps1 -DatabaseName silomath_md003_finalgate
```

Results: domain12 (StorageUnitContract5 + existing MasterData7), real Room repository13 on Pixel_8 API37 emulator, real HTTP/Retrofit transport4, backend12 (contract5 + real PostgreSQL/JDBC/Ktor7): **41 PASS, 0 failures/errors/skips**. Production compile and backend compile PASS after final applicable source changes.

| Contract / traceability | Executed evidence |
|---|---|
| Doc09 section4.5; BR-MD-001; Doc10 UUID/tenant lineage | UUID domain rejection and independence; Room canonical PK/no numeric fields; per-Location Code uniqueness; cross-parent/cross-tenant rejection; PostgreSQL composite-FK rejection with both Organizations existing. |
| BR-MD-002; Doc09 history | Active/Inactive domain, local and server roundtrip; immutable parent/creation metadata; actual PostgreSQL referenced Unit delete/Code change rejected; snapshot Code retained during rename/inactivation. |
| Doc11 authorization/API | Standard GET200 and POST403; missing credential401; cross-tenant403; canonical GET/POST/PATCH response mapping; normalization422; immutable/legacy fields422; server version409; exact headers/paths/null/absent handling; unrelated auth route isolation. |
| Doc10 local-first atomicity; Doc12 journal | Real Room successful business+outbox commit and forced outbox-insert rollback; explicit parent bridge success and bridge+business rollback; canonical foreign_key_check; no remote call during save. |
| Doc12 reconciliation/state | Durable IN_FLIGHT before fake remote executes; attempt count/recovery; exact-key lost-ACK retry; due-time enforcement; dependency block/ACK release; unchanged stale base on FIFO conflict; durable OPEN conflict on newer refresh; actor/session changes prevent unauthorized application. |
| Doc10/11 server authority | Real JDBC create/update, metadata/timestamps, null semantics, concurrent optimistic update, original-response idempotent replay and mismatch, authoritative journal/audit counts, forced audit failure rolls back Unit + SyncOperation. |
| Doc10 section18 | Fresh standalone PostgreSQL18 replay of all67 retained migrations, populated retained legacy graph and controlled populated canonical historical graph; official002/003/004 plus scoped012 assertions PASS. New StorageUnit constraints validated; old Campaign FK remains intentionally NOT VALID. |

Additional final gates, all PASS:

```powershell
.\engineering\validate-module-dependencies.ps1
.\engineering\validate-postgres-v6-contract.ps1
.\engineering\validate-baseline-registry.ps1
.\engineering\validate-baseline-change-process.ps1
.\engineering\validate-adr-process.ps1
.\engineering\validate-requirement-registry.ps1
.\engineering\validate-rvm.ps1
git diff --check
git diff --cached --check
```

Module validator:19/19 modules,65 project dependencies,0 violations. PostgreSQL static validator:67 migrations,37 V6,0 violations. Baseline registry21/21, approved CR registry3, IADR registry4, requirement registry2202 rows/1843 unique IDs, RVM governance PASS. RVM's global coverage is not relabeled VERIFIED by this entity-specific evidence. Existing IADR-0002 already determines the canonical-versus-legacy storage boundary; existing authentication and retry infrastructure is reused, so no new infrastructure-provider/architecture choice or IADR is introduced here.

## Accepted legacy limitation — not an MD-003 gate

Legacy fixture001 deliberately creates public.organizations UUID11111111-1111-1111-1111-111111111111 and launches its Campaign through the historical RPC. Migration20261015001000 preserves organization_id in canonical inventory_campaigns; the incomplete legacy Organization is not promoted to canonical authority. Migration20261025001000 / IMP-DB-003 explicitly introduces fk_inventory_campaigns_organization_id NOT VALID with ON DELETE RESTRICT so existing unresolved history is retained, while new references remain enforced. Provenance for canonical Code/actors/metadata is insufficient; nothing is fabricated.

The earlier extra global VALIDATE-every-FK check failed on this known orphan and was initially treated as a blocker. The read-only audit and Product Owner explicitly accepted it as staged legacy debt; full history reconciliation is not a predecessor or gate of MD-003. **PostgreSQL validator PASS does not mean all legacy rows are reconciled.** Official PK/FK assertions deliberately do not require convalidated=true globally. The scoped012 test proves new/canonical StorageUnit integrity and preserves this staged Campaign limitation; it does not weaken or modify any existing official assertion. No Campaign/Organization backfill, forced global FK validation or deletion was performed. Tracking owner: DB-003 constraint/reconciliation follow-up, outside this task.

## Verification limits and outside scope

- No live Supabase/backend calls, deployment or Production credentials. Tests use a dedicated PostgreSQL18 cluster on127.0.0.1:55483 with test-only trust credentials, newly created databases and parameterized JDBC. The harness never resets/drops/deletes a database. The isolated cluster is stopped after verification; retained test files/data remain outside the repository.
- Standalone infrastructure shim000 is test-only, not an authentic GoTrue/auth schema or historical Production export. Fixture011 is explicitly controlled/non-authoritative; MD003_TEST_ONLY_* snapshot/geometry labels are not Production version identifiers, not CAM-008/calculation certification. snapshotSchemaVersion remains UNRESOLVED and is not equated with inputSchemaVersion.
- Room tests use explicit in-memory configuration; Production SQLCipher/Keystore/backup factory is unchanged. Existing authentic historical Room9–14 fixture limitations are not relabeled PASS. No Room migration/schema version changed.
- Global sync orchestration, scheduling/triggers, other entity handlers, global pull checkpoints, conflict-resolution UI, package activation/READY/AUTH-010 and legacy reconciliation/CONTRACT remain their own backlog scope. Supplied client/device metadata must be real at integration; tests use clearly test-only values. No fabricated Production build/device/version metadata.
- No LOC-02, STU-02, P1-SH-02, other master-data UI, Country selector changes or MD-004 implementation. Existing legacy downstream adapters are not silently rewritten or supplied fabricated numeric mirrors.
- Initial new-test failures were fixed narrowly: UUID imports; removal of MockK from the new Android fixture because its native agent cannot load with16KB emulator pages; endpoint/method isolation for role guards; exact Room conflict-column names in the new test; root-qualified backend :test to avoid applying the filter to calculation-engine. The earlier pgcrypto test shim issue and global legacy-FK over-validation were separately identified. No historical test was deleted/weakened, dependency upgraded, baseline changed or unrelated feature repaired.

## Exact atomic commit scope

Only the following task source/test/migration/harness/evidence files belong to the commit. Existing .idea/compiler.xml, deploymentTargetSelector.xml, gradle.xml, misc.xml and untracked kotlinc.xml are left intact and excluded. No build outputs, temporary scripts, test database files, local configuration or secrets are staged.

- `app/src/androidTest/java/com/silomath/core/data/repository/StorageUnitRepositoryTest.kt`
- `app/src/main/java/com/silomath/core/data/repository/OfflineStorageUnitRepository.kt`
- `app/src/main/java/com/silomath/core/network/di/RestApiModule.kt`
- `app/src/test/java/com/silomath/core/data/repository/StorageUnitTransportTest.kt`
- `backend/build.gradle.kts`
- `backend/src/main/kotlin/com/silomath/backend/Application.kt`
- `backend/src/main/kotlin/com/silomath/backend/routes/StorageUnitRoutes.kt`
- `backend/src/main/kotlin/com/silomath/backend/storageunit/JdbcStorageUnitService.kt`
- `backend/src/main/kotlin/com/silomath/backend/storageunit/StorageUnitContracts.kt`
- `backend/src/test/kotlin/com/silomath/backend/storageunit/StorageUnitContractsTest.kt`
- `backend/src/test/kotlin/com/silomath/backend/storageunit/StorageUnitPostgresTest.kt`
- `core/database/src/main/java/com/silomath/core/database/SiloMathDatabase.kt`
- `core/database/src/main/java/com/silomath/core/database/dao/StorageUnitCanonicalDao.kt`
- `core/domain/src/main/java/com/silomath/core/domain/model/MasterDataModels.kt`
- `core/domain/src/main/java/com/silomath/core/domain/repository/StorageUnitRepository.kt`
- `core/domain/src/test/java/com/silomath/core/domain/model/StorageUnitContractTest.kt`
- `core/model/src/main/java/com/silomath/core/model/storage/StorageUnitTransfer.kt`
- `core/network/src/main/java/com/silomath/core/data/network/api/StorageUnitApi.kt`
- `core/network/src/main/java/com/silomath/core/data/network/dto/StorageUnitDtos.kt`
- `core/network/src/main/java/com/silomath/core/data/network/source/StorageUnitRemoteSource.kt`
- `engineering/task-evidence/IMP-MD-003-validation.json`
- `engineering/task-evidence/IMP-MD-003.md`
- `engineering/test-storage-unit-postgres.ps1`
- `supabase/migrations/20261028081616_v6_storage_unit_contract_guards.sql`
- `supabase/tests/migration/000_standalone_postgres_infrastructure.sql`
- `supabase/tests/migration/011_md003_historical_reference_fixture.sql`
- `supabase/tests/migration/012_md003_canonical_integrity_assertions.sql`
