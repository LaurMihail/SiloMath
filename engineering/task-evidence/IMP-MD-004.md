# IMP-MD-004 — Storage Unit UI

Historical audit status: BLOCKED_TECHNICAL — missing architectural read prerequisite. Date: 2026-10-28.
Checkpoint: MD-003 DONE at 6db82d7f419af6d2908ed579ab99768d8daec85d. No MD-004 implementation or commit.

## Authority and audit

V6 APPROVED/FROZEN Docs01/02; Doc03 BR-AUTHZ-001/BR-MD-001; Doc04 Master Data authorization; Doc06 LOC-02, STU-02, P1-SH-02 exhaustive controls; Doc21 visual/adaptive/accessibility authority; Docs07/08 repository/module boundaries; Doc17 tenant protection; canonical MD-003 repository and current StorageSpaceRepository implementation. Frozen authority supersedes the old v1.0 label in AGENTS.md. Baseline and dependency graph are unchanged.

LOC-02 already has authorized Location/Unit binding and local search/lifecycle presentation, but its Add/Edit and STU-02 navigation remain fail-closed pending this task. MD-003 provides canonical Unit UUID/parent/metadata/local mutation+outbox behavior. These implemented parts must be reused, not reimplemented.

Doc06 STU02-05 requires the child StorageSpace read model, always visible, parent fixed; STU-02 displays actual authorized cached children and distinguishes content/empty/error. STU02-07 and STS-02/P1-SH-03 remain later Storage Space feature boundaries; no fabricated action or empty state is authorized.

## Exact blocker

app/src/main/java/com/silomath/core/data/repository/OfflineStorageSpaceRepository.kt observeStorageSpaces calls StorageDao.getStorageUnitByStorageUnitId, then getStorageSpacesByUnit(storageUnit.id). That lookup requires a numeric legacy parent and returns emptyList when it is absent. MD-003 deliberately writes/reads v6_storage_units and never fabricates a legacy numeric mirror. Therefore the current reader cannot certify completeness or tenant/parent scoping for the canonical hierarchy. Empty results from that missing legacy parent must not be shown as verified absence of children.

The feature may not call DAOs directly (Doc08), copy incomplete legacy projections, fabricate Production data, or repair Storage Space Repository/API under MD-004 (explicit task scope exclusion). A baseline error/empty visual state is not authorization to substitute a permanently unimplemented read source and claim this mandatory contract DONE.

## Dependency correction needed

The audited graph currently has MD-004 <- MD-001 + MD-003, MD-005 <- MD-001 + MD-004, MD-006 <- MD-001 + MD-005. Full completion of STU-02 additionally needs a canonical authorized local Storage Space read projection. Simply adding the whole MD-005 as a predecessor would create a cycle.

Recommended decision: explicitly approve a dependency correction that extracts only that minimal read prerequisite from MD-005 before MD-004. It must read existing canonical UUID rows through a domain repository/read contract, validate Organization -> Location -> Unit -> Space lineage and react to authorization changes, without Storage Space mutation/API/UI implementation. Keep the remaining MD-005/006 scope and audited edges/history intact. This proposal is not executed or treated as an execution-order exception.

## Verification and scope

Read-only audit of baseline, existing contracts/code and dependency TSV; git status --short before/after audit. No build/test command run: no implementation changed. No existing tests weakened. Only this blocker evidence file is added. Existing IDE changes are untouched. No changes to MD-003, baseline, graph, Supabase or other repositories; no commit/push. Do not mark MD-004 GREEN/DONE until the prerequisite decision and all requested implementation/test/build gates are satisfied.

## Approved correction / current status

2026-10-28: Product Owner explicitly approved the extracted read-only prerequisite. It is now GREEN in the separate atomic commit containing IMP-MD-004-read-prerequisite.md; see reconciliation/2026-10-28-MD004-storage-space-read-prerequisite/README.md. Original blocker and proposal above are retained as audit history. Current MD-004 status: IN PROGRESS, UI not yet accepted. MD-005 status is unchanged and is not declared Done or partially complete. Resume MD-004 after the prerequisite commit.

## UI implementation attempt / current validation blocker — 2026-10-28

Current status: PARTIAL / BLOCKED_TECHNICAL (Android emulator framework unavailable). The preceding IN PROGRESS statement is retained as history. Read prerequisite is separately GREEN/committed979b15a096c013ebc5b931b363619eef8b8c9940. No MD-004 UI commit; remaining MD-005 is not started or declared partially complete.

Implemented locally, uncommitted: LOC-02 canonical scoped list with Location context, local Name/Code search and Active/Inactive/All filters, Admin Add/Edit and typed navigation; STU-02 Unit Name/Code/Description/status, parent context, canonical read-only child list with genuine empty vs unavailable state, compact single list/expanded panes; P1-SH-02 fixed parent, approved fields only, domain validation, normalization, stable draft UUID, unchanged edit identity/base/creation metadata, historical Code read-only, actor/context rechecks, sanitized save failure/draft retention and success only after repository commit. Core Navigation3 StorageUnitEditor key and app renderer/authorizer preserve role/hierarchy checks, serialized UUID context and existing back-stack behavior. Existing MD-003 historical-reference query is exposed as a read contract for the UI; no Storage Space mutation/API/Geometry/Capacity work.

Later-task boundaries: STS-02 renderer remains fail-closed. Admin Add Storage Space is shown disabled; P1-SH-03 is not implemented or navigable. Standard sees no mutation controls. No fake child collection or legacy fallback. No baseline, PostgreSQL, Supabase or dependency-version change.

Executed verification (do not infer full acceptance):

1. `.\gradlew.bat :app:compileProductionDebugKotlin` — PASS after initial UI implementation.
2. `.\gradlew.bat :feature:masterdata:testDebugUnitTest --tests '*StorageUnitViewModelsTest' :core:navigation:testDebugUnitTest :app:testProductionDebugUnitTest --tests '*ProductionDestinationAuthorizerTest'` — initial new test failed only because MockK any() tried to construct an invalid inline LocationId; one targeted correction used exact loc fixture.
3. `.\gradlew.bat :feature:masterdata:testDebugUnitTest --tests '*StorageUnitViewModelsTest' :core:navigation:testDebugUnitTest :app:testProductionDebugUnitTest --tests '*ProductionDestinationAuthorizerTest' :feature:masterdata:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=com.silomath.feature.masterdata.storageunit'` — BUILD SUCCESSFUL; 8 new ViewModel tests, NavigationContractTest and 7 Production authorizer tests passed; all9 initial Compose tests passed on Pixel_8 API37. Covers search/lifecycle/Admin/Standard/scope, loading/error/real empty, list/detail/editor, fixed parent, Save/Cancel, read-only historical Code, semantics, 200% font/short window and controlled expanded bounds.
4. After adding actor-change isolation, UI screenshot capture and the new real editor->Room integration tests, `.\gradlew.bat :feature:masterdata:testDebugUnitTest --tests '*StorageUnitViewModelsTest' :feature:masterdata:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=com.silomath.feature.masterdata.storageunit' :app:compileProductionDebugKotlin` — JVM8 and Production compile PASS, but AndroidTestRunner failed BEFORE instrumentation: `Can't find service: activity`, `Can't find service: package`, APK install failed. Gradle reported BUILD SUCCESSFUL despite device failure; this is explicitly NOT accepted as final Compose PASS. Initial Compose9 is not a final-source acceptance claim.
5. New StorageUnitEditorPersistenceTest (2 real VM->MD-003 Room commit/rollback tests) is authored but NOT EXECUTED. Existing MD-003 Room regression rerun is also pending. No final visual screenshots reviewed.

Module validator PASS19/19,65 project dependencies,0 violations. Baseline registry21/21, approved CR process3, IADR process4, requirement registry2202/1843 and RVM validators PASS. git diff --check PASS. No new architectural provider/choice outside existing IADR-0002/0003, UI-007/008; no IADR/baseline change.

Device diagnosis: adb devices reports emulator-5554 device; sys.boot_completed=1 but `service check package` reports not found. One targeted `adb -s emulator-5554 reboot` was attempted; command remains waiting and subsequent package service check still fails. AGENTS.md Error Handling requires stopping after the same issue persists following one targeted correction. No further speculative device repair, emulator data deletion or source workaround performed. Restore a functioning Android framework/test emulator, then rerun final Compose, real editor/Room regressions, Production compile and final validators/diff before any MD-004 commit. This is a technical test-environment blocker, not a new product decision or a scope change.

Working tree at the interrupted checkpoint: UI implementation/tests/evidence preserved uncommitted. Existing IDE changes remained untouched/excluded. No push or live Supabase.

## Final recovery and closure — 2026-10-28

The emulator was cold-booted and recovered with Android `package` and `activity` services available. `pm path android` succeeded, `am get-current-user` returned `0`, and the target was API 37 / x86_64. The MD-004 source and test files remained byte-for-byte unchanged during emulator recovery; no affected gate required a source correction.

Final verified results:

- 9 Storage Unit Compose tests: PASS.
- 2 real editor → repository → Room/outbox tests: PASS.
- 13 Storage Unit repository regression tests: PASS.
- 4 canonical Storage Space reader tests: PASS.
- 8 Storage Unit ViewModel tests: PASS.
- 13 Navigation tests: PASS.
- 7 Production destination authorization tests: PASS.
- Production compile: PASS.
- Module validator: PASS.
- Applicable governance validators: PASS.
- `git diff --check`: PASS.

Accepted scope is limited to LOC-02, STU-02 and P1-SH-02 plus their required repository read contract, Navigation3 destination/authorization wiring, tests and this evidence. STS-02 remains fail-closed; P1-SH-03, Storage Space mutation/API/geometry/capacity work and the remainder of IMP-MD-005 are not implemented or started. The separately committed canonical read-only prerequisite remains GREEN at `979b15a096c013ebc5b931b363619eef8b8c9940` and does not change IMP-MD-005 status. No baseline, dependency, live Supabase or production data was changed.

Final status: **IMP-MD-004 GREEN / DONE**.
