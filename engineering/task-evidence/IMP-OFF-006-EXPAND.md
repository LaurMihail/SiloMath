# IMP-OFF-006 — IADR-0002 EXPAND prerequisite

Status: EXPAND IMPLEMENTED / LOCAL VERIFIED. Staged import, authorization/compatibility and atomic READY activation are not claimed complete.

Authority: user acceptance of IADR-0002; 01 section 18; 02 D29/D30/FD-69; 09 canonical aggregates; 10 explicit geometry/decimal/UUID/revision schema and section 18; 12 sections 13–16.

Recorded IADR-0002 ACCEPTED. Room schema 26 adds 28 explicit canonical `v6_` tables (353 fields) in the same SiloMathDatabase. Legacy schema, identities, REAL values, generic JSON and pending outboxes are retained without implicit canonical migration. Canonical rows have UUID keys, explicit parameter columns, decimal TEXT, deferred relationship FKs, tenant composite FKs where ownership is explicit, version/revision uniqueness and immutable/validated-content guards. Only typed canonical projections may receive V6 package imports. No legacy rename/drop or destructive fallback is introduced.

Validation actually executed:
- `engineering/validate-adr-process.ps1`: PASS, two registered implementation ADRs; no frozen changes.
- `.\gradlew.bat :core:database:compileDebugKotlin`: PASS, generated schema 26.
- `.\gradlew.bat :core:database:compileDebugAndroidTestKotlin`: PASS (pre-existing OrganizationMembershipDaoTest type-inference warning).
- `.\gradlew.bat :core:database:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.CanonicalOfflineExpand25To26Test'`: PASS on Pixel_8/API37. Populated schema-25 fixture preserves exact legacy Double, legacy parameter JSON and pending outbox; all new canonical tables remain empty, contain no REAL column, pass FK checks and reject immutable-family rewrite.
- `git diff --check`: PASS.

The pre-existing `.idea/deploymentTargetSelector.xml` change is outside this task and excluded. No external environment changes or push. Existing blocker report is retained with its explicit resolution, not deleted.