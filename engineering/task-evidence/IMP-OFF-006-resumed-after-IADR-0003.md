# IMP-OFF-006 - final staged import verification

Classification: ALREADY_IMPLEMENTED / PASS for **Implement Staged Package Import**. Evaluated strictly at the approved task boundary. Calculation runtime compatibility is NOT a prerequisite for staging and does not block OFF-006. The previous uncommitted report incorrectly conflated staging with activation; this finalized report corrects that scope error.

## Applicable authority and dependencies

01_PRODUCT_BASELINE / 02_DECISION_REGISTER local-first and pinned provenance; Doc10 explicit canonical representations; Doc12 sections 13-15 staging isolation; accepted IADR-0002 canonical EXPAND and IADR-0003 encryption. OFF-001 and OFF-005 are implemented/verified; canonical schema EXPAND and encrypted durable staging are present. No baseline change.

## Verified implementation

- OfflinePackageDownloadRunner persists per-version recovery metadata and encrypted durable package staging, leading to VERIFYING.
- OfflinePackageIntegrityVerifier validates cryptographic hashes, counts, closed required inventory and manifest/user/organization/campaign/device/package identity.
- OfflineImportPlanner decodes all required canonical fields into CanonicalOfflinePlan: exact decimal TEXT, source versions/timestamps/actors, explicit closed FK/tenant relationships, pinned profile/version references and frozen snapshot scope.
- Missing fields, actors, pinned references or valid provenance are rejected. No import via incomplete legacy models, no guessing or legacy reinterpretation.
- The verified encrypted package is durable staging; the validated canonical plan is isolated in memory. No canonical operational-row reconciliation or current-manifest switch occurs at this stage. Those writes belong to the later atomic activation/population transaction.
- Worker records verifiedAt while retaining VERIFYING. No READY/OFFLINE_READY flag is written and the active package is untouched.

Implementation references: core/sync/.../offline/StagedOfflineImport.kt, OfflinePackageIntegrityVerifier.kt, OfflinePackageDownloadRunner.kt, OfflinePackageDownloadWorker.kt; core/database/.../offline/CanonicalOfflinePlan.kt and CanonicalOfflineRelations.kt. Previously committed implementation remains unchanged by closure.

## Validation at closure

With SILOMATH_OFFLINE_TEST_FIXTURE_OUT referencing the retained actual backend-generated disposable test contract.json, the scoped command is:

`.\gradlew.bat :core:sync:testDebugUnitTest --rerun --tests com.silomath.core.sync.offline.OfflineImportPlannerTest --tests com.silomath.core.sync.offline.OfflinePackageIntegrityTest --tests com.silomath.core.sync.offline.OfflinePackageDownloadTest --tests com.silomath.core.sync.offline.CanonicalPackageContractTest :app:compileDevelopmentDebugKotlin`

Result recorded after execution: planner 8 + integrity 7 + download 7 + backend-client contract 1 = 23 PASS, zero failures/errors/skips. App compilation PASS (up-to-date). The initial same scoped command without --rerun also returned BUILD SUCCESSFUL with tasks up-to-date; it is not reported as a fresh test execution. `git diff --check`: PASS.

## Explicit next boundary

OFF-006 is PASS; it neither activates a package nor certifies calculation compatibility. OFF-007 atomic activation, OFF-009 failed refresh preservation and OFF-010 real package compatibility remain pending. Per the user correction, restore execution to GATE-M5 and its audited CALC chain before READY. AUTH-010 remains blocked until a valid authorized compatible integrity-verified working set is atomically READY. No bypass or package activation was introduced.

Authentic historical Room fixtures 9-14 remain documented missing validation debt accepted under IADR-0003; this is unrelated to staged import completion. No push or Supabase change. Existing .idea changes excluded from the atomic OFF-006 documentation closure commit.
