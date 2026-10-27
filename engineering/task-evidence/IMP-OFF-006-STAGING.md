# IMP-OFF-006 — Verified canonical staged import

Status: canonical staging IMPLEMENTED / LOCAL VERIFIED. Package remains VERIFYING; operational READY/activation and AUTH-010 are not completed by this task.

Authority: accepted IADR-0002; 01 section 18; 02 D29/D30/FD-69/FD-72; 09 canonical aggregates; 10 explicit physical types/relationships and forward migration; 11 package transport; 12 sections 13–16; 17 offline authorization/security.

Added a closed canonical schema/relation validator, separate typed canonical read DAO, and an in-memory import plan backed by the already durable encrypted package. Planning validates required frozen fields, canonical UUIDs, explicit foreign/tenant relationships, exact decimal TEXT without rounding or Double conversion, positive versions, canonical states, historical actor coverage, snapshot scope, current actor membership and installation ownership/revocation. Missing Commodity snapshot references are rejected even where server/EXPAND compatibility columns remain nullable. No operational row, current-manifest pointer or READY state is written by planning.

The backend now includes the authorized DeviceInstallation and completes the closed reference graph (including pinned/current versions and historical actor identities). Every fetched row is checked against the campaign/tenant boundary before publication. Incomplete nullable server legacy-expansion rows cannot be represented as complete frozen V6 packages. Transport format 2 explicitly marks the complete canonical package shape; unsupported older artifacts remain ineligible rather than being silently interpreted.

The real cross-runtime test exposed numeric normalization during generic JsonElement serialization: 10.000000 became 10.0 after collection hashes were computed. The encoder now builds the explicit envelope with literal-preserving JSON text and never routes numeric business values through Double; regression assertions verify scale and collection hashes. The subsequent fixture lifecycle failure was fixed in test-only data (ACTIVE rather than the test helper's placeholder); validation was not weakened.

Final verification after code changes:
- Disposable PostgreSQL 18 localhost cluster, loaded only canonical additive test schema; no existing database or Supabase was used.
- With SILOMATH_OFFLINE_TEST_DATABASE_URL and SILOMATH_OFFLINE_TEST_FIXTURE_OUT pointing to that isolated test fixture: `.\gradlew.bat -p backend test --tests 'com.silomath.backend.offline.*'` — PASS: 17 tests, zero failures/skips.
- `.\gradlew.bat :core:sync:testDebugUnitTest --tests com.silomath.core.sync.offline.OfflineImportPlannerTest --tests com.silomath.core.sync.offline.OfflinePackageIntegrityTest --tests com.silomath.core.sync.offline.OfflinePackageDownloadTest --tests com.silomath.core.sync.offline.CanonicalPackageContractTest :app:compileDevelopmentDebugKotlin` — PASS: 23 tests, zero failures/skips, app compilation PASS. This consumes a real backend-generated synthetic package and proves backend-to-client exact-decimal, hash, context and canonical mapping compatibility.
- Populated Room 25→26 migration was separately verified in commit 62c7aa0; no legacy schema/data contraction occurs here.
- `git diff --check`: PASS.

Next real gate: Production Room protection (backlog IMP-SEC-005). Existing DatabaseModule still opens the default unencrypted SQLite database; the catalog and accepted IADRs contain no database-encryption provider. IADR-0002 explicitly does not waive that security requirement. A security-provider/forward conversion decision is needed before enabling Production canonical population and READY. OFF-007/008 and AUTH-010 remain pending. No false compatibility/authorization/protection certificate or DataStore flag was used.

Pre-existing/background IDE configuration changes are excluded. No push or external deployment.