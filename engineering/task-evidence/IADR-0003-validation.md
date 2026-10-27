# IADR-0003 validation - 2026-10-27

Current status: **PASS under the explicitly revised commit gate**. Executable authentic tests PASS; all approved reconstructed behavior tests PASS; SQLCipher/backup/Production/diff PASS. Original historical MigrationTestHelper cases depending on missing schemas 9-14 remain **BLOCKED - authentic historical Room fixture unavailable**, documented validation debt. The historical suite is NOT fully PASS. No schemas 9-14 were fabricated and no historical tests were changed. OFF-006 may resume after the atomic security commit.

The earlier verification/remediation records below are retained as history. The final revised-gate results at the end supersede earlier blocked commit-gate statements.

## Implemented scope
Modern net.zetetic:sqlcipher-android 4.19.1; unconditional encrypted application Room factory; random installation secret wrapped with Android Keystore AES-GCM; no-backup authenticated envelope; preserving corruption handling; verified plaintext export with exact full-table fingerprints and atomic promotion/recovery; protected-store backup exclusions. IADR-0003 is recorded as ACCEPTED, not a Baseline Change. Test-only SiloMathDbTest constructor arguments were mechanically aligned with current entities to compile the requested APK backup tests. Unrelated .idea changes are retained and excluded from task scope.

## Commands and results
- `./gradlew.bat :core:database:dependencyInsight --configuration debugCompileClasspath --dependency sqlcipher-android`: PASS, 4.19.1.
- `./gradlew.bat :core:database:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.protection.EncryptedRoomDatabaseTest'`: PASS, 10 tests, zero failures after the targeted staging-file creation correction. Tests cover real Keystore/fresh installation, exact populated legacy+v6+both outboxes, correct/wrong/missing/corrupt key, ordinary SQLite rejection, interruption recovery and tampered candidate rejection. Emulator page size: 16384 bytes (`adb shell getconf PAGESIZE`).
- `./gradlew.bat :app:connectedDevelopmentDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.security.ProtectedBackupPolicyTest' :app:compileProductionDebugKotlin :core:database:testDebugUnitTest`: PASS after test fixture constructor alignment. Backup tests: 2, zero failures. Production compilation passed; database unit-test task is NO-SOURCE, not an executed unit-test suite.
- Room migrations were invoked as `./gradlew.bat :core:database:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.<Class>"`. Individually verified PASS: CanonicalRoomMigration15To19Test (4), CanonicalRoomMigration15To20Test (4), CanonicalRoomMigration19To20Test (1), CanonicalRoomMigration20To21Test (1), CanonicalRoomMigration21To22Test (1), OfflinePackageMigration24To25Test (1), CanonicalOfflineExpand25To26Test (1). The earlier comma-separated filter ran only the first class and is not evidence for other classes.
- Same command with LocationMigrationTest: FAIL, 2 tests / 1 failure. `migration14To15_migratesLocationsCorrectly` cannot create its source database: missing `com.silomath.core.database.SiloMathDatabase/14.json`. Failure occurs before migration code executes. The other FK test passes.
- Remaining planned classes DeviceInstallationMigrationTest, OrganizationMembershipMigrationTest, OrganizationMigrationTest, UserMigrationAndEntityTest, Wp01TransactionAndMigrationTest were NOT run after this blocker (the loop stops on failure).
- `git diff --check`: PASS. No push, Supabase changes, destructive Git operations or secret/build-output staging.

## Blocker and required input
The repository exports schemas 15 through 26. Schema 14 is absent from tracked files, local Git history for its exact path and the available read-only current/older repository archives. Do not invent a historical fixture, delete the test or weaken its assertions. Restore an authentic exported Room schema 14 from an authoritative source, or explicitly approve a separate test-fixture remediation approach. This is an existing validation dependency, not evidence that encryption lost rows. IADR-0003 cannot be marked fully PASS or atomically committed under the all-green requirement yet.

## Boundaries
Native behavior was tested on the available API37 x86_64 16KB emulator; other API/ABI devices were not exercised. No automatic destructive recovery/purge exists: unavailable keys retain files and emit sanitized recovery-required failures. Controlled re-authentication/re-download remains subject to recoverable unsynced-data preservation and the frozen offline flow.


## Approved separate fixture remediation - 2026-10-27

The user approved a separate reconstructed behavior test, not an invented authentic export or a weakening of historical tests. Added only `core/database/src/androidTest/java/com/silomath/core/database/LocationMigrationReconstructedFixtureTest.kt` plus this evidence update in this remediation. No baseline, migration SQL or original LocationMigrationTest changes. No `14.json` was created.

- authentic Room 14 fixture: **MISSING** from repository, local history and available archives;
- historical MigrationTestHelper 14-to-15 test: **BLOCKED**, unchanged, never reported PASS;
- reconstructed migration behavior test: **PASS**, 1 test; explicitly marked `reconstructed / non-authoritative historical test fixture`.

The controlled in-memory pre-15 fixture contains only the migration's needed organizations, locations and storage_units structures. It checks exact legacy row snapshots (including whitespace, revision values, nulls and legacy type strings), valid/malformed/null location UUID sources, valid/null canonical organization sources, independent mapping of those two IDs, all new canonical metadata NULL without a source, retained child rows, FK declaration and post-migration enforcement, and a clean foreign_key_check. Valid/nonexistent canonical organization identity is modeled as UUID/NULL, matching the source contract; this does not certify the whole historical Room schema or arbitrary corrupted source schemas.

### Commands executed after adding the test

1. `.\gradlew.bat :core:database:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.LocationMigrationReconstructedFixtureTest'`: PASS (1).
2. Each of the following classes was executed separately with `.\gradlew.bat :core:database:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.<Class>"`; all five commands ran, despite recorded failures:

| Class | Tests | Passed | Failed | Exact missing input |
|---|---:|---:|---:|---|
| DeviceInstallationMigrationTest | 1 | 0 | 1 | SiloMathDatabase/11.json |
| OrganizationMembershipMigrationTest | 1 | 0 | 1 | SiloMathDatabase/13.json |
| OrganizationMigrationTest | 4 | 1 | 3 | SiloMathDatabase/12.json |
| UserMigrationAndEntityTest | 1 | 0 | 1 | SiloMathDatabase/10.json |
| Wp01TransactionAndMigrationTest | 3 | 2 | 1 | SiloMathDatabase/9.json |

All seven failures are FileNotFoundException from MigrationTestHelper loading the source schema, before migration SQL execution. No test was deleted, skipped, weakened or marked PASS to bypass them. They are distinct from the approved Room-14 limitation; no reconstructed fixtures for migrations 9-14 have been authorized or fabricated.

3. `.\gradlew.bat :core:database:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.protection.EncryptedRoomDatabaseTest'`: PASS again (10 tests / 0 failures), covering security, conversion and recovery after the new test was added.
4. `.\gradlew.bat :app:connectedDevelopmentDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.security.ProtectedBackupPolicyTest' :app:compileProductionDebugKotlin :core:database:testDebugUnitTest`: PASS again; 2 backup tests / 0 failures; Production compilation up-to-date, database unit-test task remains NO-SOURCE.
5. `git diff --check`: PASS after the final evidence update. Original LocationMigrationTest is unchanged. No secrets, temporary files or build outputs staged; existing .idea changes untouched.

### Commit gate and next decision

The conditional all-green commit gate is NOT satisfied because seven additional historical tests cannot execute without schemas 9-13. No commit or push; no OFF-006 resumption. Required: authentic fixtures for those earlier migrations, or explicit approval to extend the same separate, non-authoritative reconstructed behavior-test strategy to those missing fixture dependencies while retaining all historical tests as BLOCKED. The approved Room-14 exception remains explicitly limited: **authentic Room 14 fixture unavailable**.


## Final revised gate after explicit approval for schemas 9-13

Added `HistoricalMigrationReconstructedBehaviorTest`: five individually named reconstructed / non-authoritative historical test fixture cases for the REAL MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13 and MIGRATION_13_14 objects. No copies of migration SQL are executed by the tests. Minimal in-memory source tables contain only columns read by those migrations, necessary FK parent/child keys and observed legacy-row sentinels from the original tests. The approved separate 14-to-15 case remains in LocationMigrationReconstructedFixtureTest.

All cases verify retained rows/identities, prescribed NULL/default mappings, exact literal retention where migration SQL retains fields and foreign_key_check. Additional assertions exercise installation-user RESTRICT, membership parent RESTRICT, retained location-child CASCADE definitions, uniqueness/UUID guards, pending payload preservation and unknown legacy role remaining unchanged with NULL canonical role. Existing organization trim/UUID/revision mappings are tested as existing SQL behavior, not invented business normalization. No authentic full-schema certification is claimed.

Final command `.\gradlew.bat :core:database:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.HistoricalMigrationReconstructedBehaviorTest'`: **5 PASS**, no failures/errors/skips.

The following cases were each invoked separately with `.\gradlew.bat :core:database:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.<Case>"`. Method suffixes select only executable existing methods and do not modify/disable historical fixture tests:

| Executed case | PASS count |
|---|---:|
| CanonicalOfflineExpand25To26Test | 1 |
| CanonicalRoomMigration15To19Test | 4 |
| CanonicalRoomMigration15To20Test | 4 |
| CanonicalRoomMigration19To20Test | 1 |
| CanonicalRoomMigration20To21Test | 1 |
| CanonicalRoomMigration21To22Test | 1 |
| LocationMigrationReconstructedFixtureTest | 1 |
| LocationMigrationTest#productionDatabase_enforcesForeignKeysByDefault | 1 |
| OfflinePackageMigration24To25Test | 1 |
| OrganizationMigrationTest#freshDb13_enforcesPhysicalConstraintsParityWithoutTestCreatedTriggers | 1 |
| protection.EncryptedRoomDatabaseTest | 10 |
| Wp01TransactionAndMigrationTest#transaction_businessMutationAndSyncOperation_persistTogetherAtomically | 1 |
| Wp01TransactionAndMigrationTest#transaction_forcedOutboxFailure_rollsBackBusinessMutation | 1 |

The 7 complete migration classes with available authentic fixtures account for 13 PASS; the 4 executable methods from mixed historical classes account for 4 PASS. Together with 6 reconstructed cases, 10 SQLCipher tests and 2 packaged backup tests: **35 executable tests PASS**. This does not include or silently pass the historical cases that cannot load authentic schemas 9-14.

Final `.\gradlew.bat :app:connectedDevelopmentDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.security.ProtectedBackupPolicyTest' :app:compileProductionDebugKotlin :core:database:testDebugUnitTest`: PASS; 2 backup tests, Production compilation up-to-date; unit-test task NO-SOURCE. `engineering/validate-adr-process.ps1`: PASS (3 IADRs). `git diff --check`: PASS. Every final native test/build follows the last code change. Existing historical test files have empty git diffs. No baseline/Supabase/external environment change, no push, and no .idea/build/temporary/secret files included in the security commit.

### Explicit retained validation limitation

Authentic fixtures 9.json-14.json: MISSING. Historical MigrationTestHelper cases for 9-to-10, 10-to-11, 11-to-12, 12-to-13, 13-to-14, 14-to-15: **BLOCKED - authentic historical Room fixture unavailable**. Their previous FileNotFoundException results remain in the historical record and are not relabeled PASS. Reconstructed behavior coverage does not validate the complete original Room schemas. The user explicitly accepted this documented debt as non-blocking for encryption and OFF-006.
