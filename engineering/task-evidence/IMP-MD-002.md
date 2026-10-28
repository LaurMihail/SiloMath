# IMP-MD-002 — Location UI

Status: GREEN / DONE. Date: 2026-10-27. Commit: the atomic commit containing this evidence.

## Authority and satisfied predecessors

V6 APPROVED/FROZEN baseline refreeze ccfa3a0 (FD-74); Docs01/02; Doc06 LOC-01, LOC01-01..06, P1-SH-01 / P1SH01-01..10 and canonical LOC-02 target; Doc09 Location/history/lifecycle; Docs10/11 existing Location payload; Doc12 local-first durable mutation; Doc17 SEC-D146..149; Doc21 tokens/adaptive/accessibility. MD-001 accepted at checkpoint91227af; UI-007 committed4afe034; UI-008 committedd8c1295. Dependency correction retains the original graph; no execution-order exception or PREIMPLEMENTED-as-satisfied inference. Product Owner explicitly approved inclusion of P1-SH-01 in this task.

## Implemented scope

- LOC-01 Production Locations tab uses the existing Room-backed LocationRepository plus actual authenticated active-Organization Admin/Standard context. A single local filtered state feeds both List and Map. Name/Code trim/case-insensitive search; Active default / Inactive / All; List default / Map; stable IDs and saved filter/view/selection state. No legacy projection fallback, fake Production data or default coordinates.
- Markers derive only from canonical valid coordinate pairs in that result. Rows without positions remain in List. Marker and row selection route to the existing canonical LOC-02 identity; the bounded LOC-02 renderer resolves real local Location/Storage Unit reads and checks the parent scope. It does not complete Storage Unit mutation/details tasks or introduce Location Details.
- Admin Add/Edit now reaches the real P1-SH-01 sheet through typed Navigation3. Root authorizer verifies role/parent and rechecks session after suspended lookup; ViewModel independently verifies Admin again before mutation. Standard has no mutation controls and cannot invoke the editor via a forged destination.
- P1-SH-01 has the exact approved Name, Code, Address, City, Country, Notes, Active/Inactive and optional Map Position controls, Save and Cancel. It trims Name, uppercases/normalizes Code, checks limits/requiredness/Organization uniqueness, enforces pair/ranges and atomic coordinate clear. Manual map pin selection and typed coordinates work without GPS or basemap. Known historical references lock Code; the repository repeats local historical/uniqueness/Country guards inside its existing transaction. No master/snapshot deletion or reinterpretation. Inactivation changes current master lifecycle and never rewrites or invalidates the frozen Campaign context (Doc09 section8).
- FD-74 catalogue is fully packaged locally (249 assigned codes in this source snapshot, not a new frozen catalogueVersion). Persisted Country is the exact selected uppercase alpha-2; labels are presentation only. Search filters selector options and cannot become a Country write. Null is preserved as the approved optional value. New non-catalogue values fail closed in form and repository. Existing non-conforming Country, including empty historical strings, is preserved verbatim when unchanged; explicit selection/clear is a new user action. No historical audit/migration or implicit name-to-code conversion.
- Save calls the existing MD-001 durable Location + SyncOperation transaction; success is emitted only after commit. Creation allocates a stable UUID per draft/retry. Edit retains identity, base entityVersion and creation metadata. Local event time is provisional; request payload does not synthesize server timestamps. Failure retains the draft and exposes a sanitized error. Cancel/back do not save uncommitted edits.
- MapLibre Native OpenGL13.7.1 is an isolated presentation renderer (IADR-0004). App supplies optional environment raster/attribution configuration; no public/demo provider is hardcoded. Dedicated allowlisted HTTPS GET client strips business/auth headers and body, disables redirects/SDK logging and only permits numeric configured tile paths and static provider query. No auth client, token, names, Evidence or business payload goes to provider. Local GeoJSON stays in the renderer. Cache is bounded/disposable/non-authoritative; tile availability cannot establish READY/offline eligibility.
- Production merged manifest has no fine/coarse/background location permission. No GPS/location component, geocoding or route guidance. Provider/network/HTTP failure only affects basemap presentation; Show list remains available and authoritative.
- Doc21 theme/components/tokens, current-bounds compact vs coordinated medium/expanded layout, IME avoidance, scroll access at 200% font/short window, labeled 48dp marker alternatives and 48dp native hit boxes. Status has text; errors are explicit. Root owns canonical back/shell; no string-route NavHost added.

## Executed verification

Exact final Gradle commands:

`.\gradlew.bat :feature:masterdata:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=com.silomath.feature.masterdata.location' :feature:masterdata:testDebugUnitTest :core:domain:testDebugUnitTest --tests com.silomath.core.domain.model.CountryCatalogTest --tests com.silomath.core.domain.model.LocationCoordinateContractTest :app:testProductionDebugUnitTest --tests com.silomath.app.navigation.ProductionDestinationAuthorizerTest :app:compileProductionDebugKotlin`

`.\gradlew.bat :app:connectedProductionDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.data.repository.LocationUiPersistenceTest' :app:compileProductionDebugKotlin`

Both BUILD SUCCESSFUL after final Kotlin changes. Latest XML evidence: 21 feature JVM (17 Location +4 existing Organization regressions), 6 domain (Country1 + coordinates5), 6 Production-authorizer JVM, 11 feature Compose/native and 5 Production Room integration =49 PASS; zero failures/errors/skips. Gradle may reuse unchanged tasks; per-suite timestamps, testcase names and report hashes are retained in IMP-MD-002-validation.json.

Native API37 emulator tests cover actual SDK startup without provider, real touch-to-coordinate selection, accessible stable-ID marker action, List availability on missing basemap, coordinated expanded bounds, 200% font in short compact bounds, all list states, locally available Country selection emitting RO, protected Code, historical Country warning, atomic Clear Map Position, Cancel and denied mutation state. Controlled expanded bounds are not a claim of manual tablet/foldable-device QA.

Real in-memory Room integration tests cover business row + outbox atomic success, rollback if outbox insert fails, CREATE nullable baseEntityVersion, absence of fabricated server timestamps, Country fail-closed/new writes vs unchanged historical data, canonical reads without insufficient legacy fallback, uniqueness, historical-code guard and foreign_key_check. This is an explicit test-only in-memory configuration; the Production encrypted factory is unchanged. No migration SQL, Room version/schema exports or encryption implementation changed.

`engineering/validate-module-dependencies.ps1`: PASS19/19 modules,65 project dependencies,0 violations. `engineering/validate-adr-process.ps1`: PASS4 IADRs. Production merged-manifest XML parsed: location permissions absent; allowBackup=false preserved. Catalogue SHA-256 matches its packaged SOURCE.json; .gitattributes pins its resources to LF. `git diff --check`: PASS, repeated with cached check at commit gate.

Initial new-test fixture failures were corrected narrowly: MockK any() generated an invalid UUID value-class argument (replaced by exact typed fixture ID); Compose progress matcher required the current API; nested-scroll test had to scroll its containing results pane first; Romania matched both the search text and option (qualified the option with RO). A PowerShell Gradle property needed quoting; class-list filtering was replaced by the package filter to actually run both UI classes. No test removed/weakened, no frozen behavior changed; final full relevant suites are green.

## Scope limits / remaining owners

No live-provider availability or deployment-service/cache-coverage certification; basemap is intentionally best-effort. No live backend/Supabase test or mutation, push, auth-flow rewrite or historical Country migration. The historical-reference query is a local guard; this task does not claim a new server API or globally complete history download. Existing server authority/sync conflict contracts from MD-001 remain unchanged. UI-009 Stitch visual-floor comparison, Storage Unit mutation/details, gates M3/M5/M6, AUTH-010 and snapshotSchemaVersion remain owned by their existing tasks/boundaries; no completion is inferred for them. No GATE becomes Done through this UI commit.

Preexisting IDE changes and the generated .kotlin compiler-session artifact are excluded. No local.properties, credentials, temporary/build outputs or external configuration belong to this commit. Original audited workbook and official baseline files remain unchanged.

## Exact atomic commit scope

- `.gitattributes`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/silomath/core/data/repository/LocationUiPersistenceTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/silomath/app/MainActivity.kt`
- `app/src/main/java/com/silomath/app/navigation/MapPresentationModule.kt`
- `app/src/main/java/com/silomath/app/navigation/ProductionDestinationAuthorizer.kt`
- `app/src/main/java/com/silomath/core/data/repository/OfflineLocationRepository.kt`
- `app/src/test/java/com/silomath/app/navigation/ProductionDestinationAuthorizerTest.kt`
- `core/database/src/main/java/com/silomath/core/database/dao/LocationDao.kt`
- `core/designsystem/src/main/java/com/silomath/core/designsystem/components/AppHeader.kt`
- `core/domain/src/main/java/com/silomath/core/domain/model/CountryCatalog.kt`
- `core/domain/src/main/java/com/silomath/core/domain/repository/LocationRepository.kt`
- `core/domain/src/main/resources/country/LICENSE-pycountry.txt`
- `core/domain/src/main/resources/country/SOURCE.json`
- `core/domain/src/main/resources/country/iso3166-1.tsv`
- `core/domain/src/test/java/com/silomath/core/domain/model/CountryCatalogTest.kt`
- `engineering/adr-register.json`
- `engineering/adr/IADR-0004-isolated-facility-map-presentation.md`
- `engineering/reconciliation/2026-10-27-UI-foundation-dependency-correction/MD002-completion.json`
- `engineering/reconciliation/2026-10-27-UI-foundation-dependency-correction/README.md`
- `engineering/task-evidence/IMP-MD-002-validation.json`
- `engineering/task-evidence/IMP-MD-002.md`
- `feature/masterdata/build.gradle.kts`
- `feature/masterdata/src/androidTest/java/com/silomath/feature/masterdata/location/LocationEditorSheetTest.kt`
- `feature/masterdata/src/androidTest/java/com/silomath/feature/masterdata/location/LocationsScreenTest.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/LocationDraft.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/LocationEditorSheet.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/LocationEditorViewModel.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/LocationUiState.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/LocationUnitsScreen.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/LocationUnitsViewModel.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/LocationsScreen.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/LocationsViewModel.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/map/FacilityMapRuntime.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/map/MapProviderConfig.kt`
- `feature/masterdata/src/main/java/com/silomath/feature/masterdata/location/map/NativeFacilityMap.kt`
- `feature/masterdata/src/test/java/com/silomath/feature/masterdata/location/LocationEditorViewModelTest.kt`
- `feature/masterdata/src/test/java/com/silomath/feature/masterdata/location/LocationsViewModelTest.kt`
- `feature/masterdata/src/test/java/com/silomath/feature/masterdata/location/MapPrivacyTest.kt`
- `gradle/libs.versions.toml`
