# SiloMath V6 - 18_TEST_STRATEGY_ACCEPTANCE

**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-17 — Android/UX implementation-closure refreeze  

## 1. Purpose and authority

This document is the normative V6 verification and acceptance contract. It converts frozen Documents 01–17 plus Document 21 visual authority into a risk-based, traceable test strategy and exact Production release gates. It does not create or reinterpret product behavior.

## 2. Quality objectives
- Prove business correctness and deterministic calculation results.
- Prove offline durability, synchronization idempotency and explicit conflict behavior.
- Prove tenant isolation, authorization and security controls.
- Prove immutable historical behavior and atomic Campaign Close.
- Prove report integrity and cross-report reconciliation.
- Prove usable adaptive Android behavior across the supported form factors.
- Produce reproducible release evidence tied to the exact candidate build.

## 3. Test strategy model
- Doc18 is the V6 contract for test strategy, verification, acceptance and release quality gates.
- Tests verify frozen Documents 01-17 and do not redefine business behavior.
- A test-versus-contract conflict is resolved by fixing test/implementation or escalating the inconsistency, never by silently changing the contract.
- Testing is risk-based and layered, with highest automated density on calculation, stocktaking, sync, close, authorization, persistence and immutable history.
- V6 uses a test pyramid: many unit/domain tests, then repository/database/API contract tests, integration tests, and fewer E2E/UI tests.
- Manual testing complements automation for exploratory UX, device behavior and UAT; it does not replace automated regression for critical rules.
- Tests define reproducible preconditions, fixtures/data, action, expected result and isolated cleanup/state.
- Deterministic fixtures are mandatory where results are contractual; random/fuzz testing may supplement but not replace them.
- Unit tests cover pure domain rules, validation, normalization, calculations, state transitions and semantically contractual adapters.
- Database tests cover Room DAOs/transactions/migrations and PostgreSQL schema constraints/transactions.
- Repository tests cover local-first reads, atomic local write plus outbox, mapping and error propagation.
- API contract tests cover request/response schema, error envelope, authentication, authorization, idempotency, entityVersion and pagination.
- Integration tests cover Android persistence/repository/sync and backend database/API workflows.
- UI tests cover navigation, permissions, state rendering, validation surfaces, accessibility and adaptive behavior.
- End-to-end tests cover canonical journeys from Sign In through Completed Campaign and reporting.
- SEC-T01-SEC-T40 from Doc17 are a mandatory verification set.
- UAT validates usability and business acceptance on a release candidate without redefining frozen requirements.

## 4. Test pyramid and ownership

| Layer | Primary scope | Expected automation |
|---|---|---|
| Unit/domain | Pure rules, validation, calculation, state transitions | High |
| Database/repository | Room/PostgreSQL constraints, transactions, migrations, local-first/outbox | High |
| API contract | Schema, authz, idempotency, concurrency, pagination/errors | High |
| Integration | Android local/sync and backend API/database workflows | High on critical paths |
| UI | Navigation, states, permissions, adaptive/accessibility | Critical paths automated |
| E2E/UAT | Canonical business journeys and business acceptance | Focused; release-required |


## 5. Calculation Engine verification
- All F01-F10 geometry families have canonical golden vectors.
- Each family tests nominal values, valid boundaries, invalid boundaries, applicable zero/empty paths and representative decimal precision.
- Golden expected results are frozen independently of production code; tests never derive expected values by calling the same production formula.
- MEAN_ULLAGE_V1, LEVEL_BASED_V1, effective fill height, h<0 and h>maximumFillHeight are tested; invalid height is never clamped.
- Measurement cardinality 1-5 is tested and >5 is rejected.
- Canonical unit normalization is tested.
- No intermediate rounding is verified; persistence is scale 6 HALF_UP.
- Quantity tests verify M = V * ActualBulkDensity, density >0 and Mark Empty exact zero.
- Reference Test Weight never becomes fallback density.
- Moisture/impurities never automatically change Physical Quantity.
- Validated/final historical calculations are not recalculated after current Geometry/Commodity changes.
- Any Calculation Engine/formula change requires full golden regression before release.

## 6. Database and migration verification
- Every Production Room schema migration has automated migration testing from every relevant supported version to target.
- PostgreSQL migrations are tested against representative populated data, not only empty databases.
- Migration testing explicitly verifies no destructive data loss.
- Doc10 constraints, uniqueness, FKs and checks have negative tests as well as happy paths.
- Transaction tests cover Save & Launch, Reset Validation, Close, local business write plus outbox and other frozen atomic operations.
- Campaign Code allocation has concurrency testing for atomic uniqueness.
- Tenant-scope constraints have cross-Organization negative tests.
- Immutable versions/snapshots/events have mutation, repoint and delete rejection tests.

## 7. Offline and sync verification
- Doc12 mandatory tests are release-required regression tests.
- Local save is tested with network unavailable and survives process restart.
- Business mutation plus SyncOperation is verified as one local transaction.
- Timeout/5xx/restart retry preserves operation/idempotency identity and never duplicates the business action.
- Same-entity FIFO and dependency ordering have deterministic tests.
- Overlapping multi-device edits create explicit conflict without auto-merge or force overwrite.
- 401, 403, 429 Retry-After, transient 5xx and permanent domain errors have distinct tests.
- Package download tests staging, verify, atomic switch and preservation of the prior valid package on failure.
- Manifest/hash corruption blocks package use.
- Known revocation/Completed state blocks incompatible new mutations without deleting recoverable unsynced data.
- Evidence metadata-to-binary dependency-to-verification/final ACK is tested end-to-end.
- Airplane-mode regression is mandatory for the canonical assigned-Campaign stocktaking flow.
- Restart with IN_FLIGHT and no ACK retries the same operation identity.

## 8. Campaign, Stocktaking and Close verification
- Canonical Campaign E2E is Create -> Scope/Team -> Save & Launch -> Active -> field work -> Review -> Close -> Completed -> Final Snapshot/report.
- Save & Launch tests guards, atomic CampaignSnapshot creation, frozen Scope/version context and ACTIVE state.
- Failed Save & Launch cannot create a partially launched Campaign or incomplete snapshot.
- Campaign Scope is immutable after launch and negative mutation tests are required.
- Team Assignment remains Admin-only editable while Active; launch team snapshot is never rewritten.
- Removed team members lose new Stocktaking mutation rights; later unauthorized offline sync is rejected.
- Exactly one StocktakingRecord per Campaign x Storage Space is verified under retry/concurrency.
- Non-empty validation covers Measurements, CommodityVersion, Actual Bulk Density, Ownership and Client when Third-Party.
- Owned does not require Client; Third-Party without Client is rejected.
- Mark Empty produces Validated, isEmpty=true, Volume=0 and Quantity=0 without fabricated Commodity/Density/Ownership/Client.
- Validate does not automatically resolve Open Issues.
- Reset Validation is Admin-only and Active-only, creates revision N+1 WORKING and preserves the prior validated revision immutable.
- Reset Validation on Completed Campaign is rejected.
- Issue OPEN->RESOLVED and RESOLVED->OPEN before Close are tested with append-only status/resolution history.
- Close revalidates all guards server-side; client readiness is insufficient.
- Every Close Guard has at least one individual negative test.
- Successful Close creates exactly one FinalInventorySnapshot and exactly one entry per scoped Space atomically.
- Failed Close leaves Campaign Active and creates zero partial Final Snapshot.
- Completed Campaign mutation attempts are rejected through normal client/API paths.
- Master-data changes after Close never alter Final Snapshot/history.

## 9. API and authorization verification
- All Doc11 endpoints have contract tests for method/path, required headers, request schema, success schema and documented errors.
- Protected endpoints have negative unauthenticated-access tests.
- Role-sensitive endpoints explicitly test Admin versus Standard.
- Tenant resources test cross-Organization access using a valid UUID from another tenant.
- Campaign Stocktaking mutations test Campaign Team authorization separately from Role.
- Entity-version mutations test current-version success and stale-version conflict.
- Idempotent mutations replay same key and same payload without duplicate business action.
- Reusing an Idempotency-Key with different payload is rejected according to the API contract.
- API error envelope is contract-tested for stable machine code, correlation context and field errors where defined.
- Cursor pagination tests first/middle/final pages, opaque cursor and no duplicate/missing rows in a stable dataset.
- Unsupported/invalid filters, sort fields and malformed identifiers have negative tests.
- Immutable resources have direct API mutation rejection tests.
- Client-supplied organizationId, role or userId can never grant authority.
- Authentication contract tests include Sign In, refresh rotation, Sign Out, Disabled User and password-reset lifecycle.

## 10. Evidence verification
- PHOTO and NOTE have distinct unit/domain/integration tests.
- PHOTO tests verify EXIF orientation correction, canonical JPEG, long edge <=2048 px and no upscale.
- Target <=1.5 MB and hard rejection >3 MB are tested.
- SHA-256 is verified over exact canonical JPEG bytes.
- GPS/unnecessary EXIF and device identifiers are absent from canonical Evidence metadata.
- PHOTO quota is max 5 per StocktakingRevision and max 5 per Issue; the sixth is rejected.
- NOTE does not consume PHOTO quota and validates trimmed non-empty text and maximum length.
- Local Evidence success appears only after durable private file, metadata and outbox commit.
- Failure between file creation, metadata and outbox cannot produce false Saved state.
- Evidence transitions LOCAL_PENDING -> UPLOADING -> SYNCED and SYNC_ERROR have transition/retry tests.
- Retry preserves Evidence identity/hash and never creates duplicate official Evidence.
- Server MIME/size/hash mismatch blocks finalization.
- Validated Evidence mutation/removal is rejected.
- Working Evidence removal/cleanup respects audit, retention and reference safety.
- Production verification covers encrypted local PHOTO and private authorized remote retrieval.

## 11. Reporting, History, Dossier and Audit verification
- All nine canonical report types have dataset-level regression tests.
- Completed official reports read only FinalInventorySnapshot and never recalculate history.
- Active reporting is explicitly non-final in UI/export tests.
- UI/PDF/CSV for one request reconcile semantically to the same authoritative dataset.
- Physical Inventory totals are the reference totals for cross-report reconciliation.
- Inventory by Location/Commodity/Ownership totals reconcile to the same Physical Inventory selection.
- Capacity tests row utilization and aggregate sum(Volume)/sum(Capacity)*100, never mean row percentages.
- Worksheet tests frozen calculation trace and forbids historical recalculation.
- History tests 2..N Completed Campaigns and COMPARABLE, SCOPE_ADDED, SCOPE_REMOVED and CONTEXT_CHANGED.
- History previous quantity zero produces frozen N/A percentage semantics.
- Dossier tests frozen content and PDF-only dossier export.
- Dossier never grants Standard access to Admin-only Audit Trail content.
- Audit Trail is Admin-only, read-only and tests filtering/detail/export.
- Incomplete/partial offline report sources cannot be exported as final/complete reports.
- Report integrity/reconciliation failure blocks official export where Doc16 requires it.
- PDF and CSV have semantic regression tests; file existence alone is insufficient.

## 12. UI, navigation, adaptive and accessibility verification
- All 43 major UI components have acceptance traceability; critical components have automated UI coverage.
- Jetpack Navigation 3 type-safe destinations test forward navigation, Back/context restoration, process recreation and deep-link guards.
- Predictive Back is verified for root/top-level, nested destinations, overlays/sheets, and approved unsaved-edit guards on supported API levels.
- Edge-to-edge tests verify system bars, cutouts, gesture insets and IME do not obscure required actions, validation messages or critical values.
- Lifecycle tests verify inactive destinations do not keep unnecessary UI state collection active and critical operation outcomes survive required recreation.
- STK-05 -> STK-03 restores Storage Unit and relevant list/scroll context.
- Bottom navigation is exactly Dashboard / Inventory Campaigns / Stocktaking / Locations / More and is absent from authentication.
- Navigation/deep-link tests cannot bypass authentication, Organization scope, role, Campaign access or entity state.
- Critical UI is verified on phone portrait, phone landscape, tablet portrait and tablet landscape.
- Adaptive testing requires equivalent business capability, permissions and critical information without clipping/overlap, not pixel identity.
- Large-text tests verify primary actions, labels and critical values without clipping.
- Accessibility tests verify semantics/labels, focus order, touch targets and content descriptions for interactive/non-text controls.
- Critical status is never communicated by color alone.
- Field validation is associated with the relevant control/action.
- Read-only versus editable state is tested visually/semantically on critical workflows.
- Large STK-03 datasets verify efficient/lazy rendering and usability without mandatory network page fetch.
- UI tests verify canonical English terminology/enums for business-critical labels/statuses.
- Screenshot/golden visual-regression tests are mandatory for frozen design-system primitives and representative critical screens; they supplement and never replace semantic/navigation/accessibility tests. Approved Stitch references are the minimum visual-quality floor, not a source of business semantics.
- LOC-01 verifies List/Map query/filter equivalence, marker selection, no-position Locations remaining available in List, basemap-unavailable fallback, and accessible List alternatives.

## 13. Security verification
- SEC-T01-SEC-T40 are mandatory release regression tests.
- SEC-AC01-SEC-AC25 are mandatory security release gates.
- Security testing includes client manipulation/API-level testing; UI-only permission tests are insufficient.
- Production artifacts are scanned/tested for secrets and debug trust/logging configuration.
- TLS/cleartext tests run against Production-equivalent network configuration.
- At-rest verification covers encrypted Room and local PHOTO Evidence.
- Logs/diagnostic packages are inspected for forbidden secrets/payloads.
- Sign-In/reset throttling and token lifetime/rotation/replay are mandatory security integration tests.
- Cross-tenant authorization regression is release-blocking.
- Any failed mandatory security gate blocks Production release.

## 14. Performance and reliability
- V6 uses scenario-based performance acceptance; Product documentation does not invent unsupported business SLAs. Engineering performance thresholds are version-controlled test configuration and may evolve through evidence/IADR without Product/Baseline approval when user-visible behavior remains compliant.
- Release builds verify R8 full mode, Baseline/Startup Profile generation for representative critical user journeys where supported, and Macrobenchmark startup/interaction regression evidence.
- Main-thread blocking disk/network I/O in production critical paths is a release failure.
- STK-03 is tested with representative large Storage Unit data for smooth/lazy navigation without memory failure.
- Campaign offline package is tested at representative production scale for download/verify/local navigation.
- Sync tests the 100-operation batch boundary and sustained backlog processing without duplication/data loss.
- Reports test representative large Completed Campaign generation/view/export without timeout, corruption or inconsistent totals.
- Evidence tests maximum-size canonical PHOTO and multiple queued uploads without corruption/duplicates.
- Reliability soak testing covers repeated local writes, sync retries and app restarts.
- Process-death tests are mandatory for critical local-first mutation paths.
- Network transition tests cover online->offline, offline->online and unstable/intermittent connectivity.
- Performance regression is compared with the approved release baseline; material degradation in critical field workflows is investigated before release.

## 15. Test environments and test data
- Automated unit/domain tests run locally/CI without Production dependency.
- Integration/API/E2E use isolated test environments or ephemeral dependencies where practical.
- Staging is Production-like for schema, API/security configuration, object-storage behavior and migration/release verification.
- Automated tests never run destructive scenarios against Production business data.
- Production validation is limited to approved non-destructive smoke/health checks.
- Test fixtures use synthetic data by default.
- Production personal/business data is not copied to Dev/Test without an explicitly authorized sanitized process.
- Canonical fixtures include all 10 geometry families, 24 governed commodities, Owned/Third-Party, Client, Empty, Issues, Evidence, Active/Completed and conflict states.
- Time-sensitive tests use controllable clocks/fixed UTC instants.
- Concurrency tests use deterministic coordination where possible rather than fragile timing sleeps.

## 16. Defect severity
- S0 Blocker includes data loss/corruption, wrong official inventory quantity/snapshot, security/tenant breach, destructive migration, inability to complete core workflow or immutable-history mutation.
- S1 Critical includes major frozen-rule break, unusable critical offline workflow, repeatable duplicate mutation, Close/report integrity failure or serious authorization defect without confirmed breach.
- S2 Major substantially impairs an important feature but has a safe workaround and does not corrupt authoritative data.
- S3 Minor is a limited non-critical UX/presentation defect without business correctness, security or data-integrity impact.
- Any open S0 or S1 blocks Production release.
- S2 may be accepted only through explicit release waiver with owner, impact, workaround and follow-up.
- S3 does not automatically block release but remains tracked.
- Severity is determined by impact/risk, not ease of repair.

| Severity | Release meaning |
|---|---|
| S0 Blocker | Production blocked; data/security/core-workflow/immutable-history risk. |
| S1 Critical | Production blocked; major frozen-rule/offline/authorization/integrity failure. |
| S2 Major | Only explicit documented waiver may permit release. |
| S3 Minor | Tracked; does not automatically block release. |

## 17. Coverage policy
- V6 does not use a global numeric code-coverage percentage as a release gate.
- An arbitrary percentage can over-reward trivial code while critical rules remain untested.
- Release gating is based on requirements/risk coverage, not raw line coverage.
- 100% of frozen critical business rules/release gates identified in the traceability matrix require test evidence.
- F01-F10, Close Guards, critical Stocktaking transitions, offline durability/idempotency/conflict, tenant authorization, migration safety and mandatory security gates require explicit automated coverage.
- Line/branch coverage may be collected as engineering diagnostic/trend data but does not independently decide release.
- New code implementing a frozen critical rule without an associated test cannot be considered Done.

## 18. Requirements Verification Matrix
- V6 maintains a Requirements Verification Matrix (RVM).
- RVM links Requirement/Decision ID -> component/module -> test ID(s) -> test level -> automated/manual -> result/evidence.
- Verifiable frozen BR/FC/IC/DM/DB/OS/CE/CO/EV/RP/SEC decisions are mapped in the RVM.
- A critical requirement without test mapping is a release gap even when the overall suite is green.
- A test failure must identify the affected contractual requirement.
- Obsolete tests for changed/removed requirements are explicitly updated/removed and never retained as false authority.
- Release evidence records the exact build and relevant schema/API versions plus mandatory-suite results.

## 19. User Acceptance Testing
- UAT runs on a release candidate in Production-like Staging.
- UAT uses synthetic/approved test Organizations and Campaigns, not implicit Production data.
- Minimum UAT personas are Admin and Standard.
- Admin UAT covers relevant master setup, Campaign launch, team/scope, review/close, reports and administration.
- Standard UAT covers assigned Campaign, field Stocktaking, offline operation, Issue/Evidence and authorized reports.
- UAT includes at least one offline->reconnect->sync scenario.
- UAT includes Third-Party+Client, Owned, Mark Empty and Issue flow.
- UAT includes Completed Campaign plus official reporting/history.
- UAT sign-off confirms frozen workflow acceptance and cannot waive security/data-integrity blockers.
- UAT defects use S0-S3 and the same release gates.

## 20. Production release gates
- Production-candidate build/compile/static checks pass.
- Required unit/domain suite passes.
- F01-F10 Calculation golden regression passes.
- Database schema/constraint/migration suite passes.
- API contract plus authorization/tenant-isolation suite passes.
- Offline/sync mandatory regression suite passes.
- Stocktaking/Campaign/Close critical workflow suite passes.
- Evidence integrity/storage suite passes.
- Reporting/History/Dossier/Audit mandatory suite passes.
- SEC-T01-SEC-T40 pass and SEC-AC01-SEC-AC25 are satisfied.
- Critical UI/navigation/predictive-back/edge-to-edge/adaptive/accessibility/Location-map suite passes.
- Required visual-regression/Stitch-floor checks pass for frozen components and representative critical screens.
- Required release-performance checks pass: R8 full mode, Baseline/Startup Profiles and Macrobenchmark evidence for configured critical journeys.
- Production migration rehearsal against representative populated data passes.
- Staging smoke/E2E canonical journey passes.
- No open S0/S1 defects.
- Every accepted S2 has an explicit documented waiver.
- RVM has no unmapped/unverified critical frozen requirement.
- UAT sign-off passes for the release candidate.
- Production artifact security/configuration checks pass.
- No unresolved known data-integrity/reconciliation/migration defect exists.
- Release evidence/report is archived for the exact candidate build.

## 21. Failure and flaky-test policy
- A mandatory test failure is real until a demonstrated cause exists.
- Rerun may aid diagnosis but a later green rerun does not automatically erase the initial failure.
- A flaky mandatory test is fixed or formally quarantined with owner/reason; quarantine cannot hide a critical-rule verification gap.
- A critical requirement without another valid verification cannot be quarantined for Production release.
- Tests avoid arbitrary sleeps when deterministic synchronization/clock control is available.
- CI retains sufficient sanitized failure artifacts/logs for reproduction without exposing secrets.

## 22. Mandatory verification suite catalog

The suite catalog defines mandatory verification domains without inventing placeholder test cases. Executable repository test IDs must be concrete and must map through the RVM to the exact frozen requirement(s) they verify.

| Suite | Domain | Governing decisions | Required coverage |
|---|---|---|---|
| CAL | Calculation Engine | TA-D18-D29 | F01-F10 golden vectors; aggregation; boundaries; units; precision; historical reproducibility. |
| DB | Database / migrations | TA-D30-D37 | Room/PostgreSQL migrations, constraints, atomic transactions, concurrency, tenant and immutability negatives. |
| SYN | Offline / sync | TA-D38-D50 | Durable local save, outbox atomicity, retry/idempotency, dependencies, conflicts, package integrity, restart. |
| STK | Campaign / Stocktaking / Close | TA-D51-D70 | Launch atomicity, scope/team, validation/empty/reset/issues, all Close Guards, Final Snapshot immutability. |
| API | API / authorization | TA-D71-D84 | Endpoint contracts, authn/authz, tenant isolation, team rules, entityVersion, idempotency, pagination/errors. |
| EVD | Evidence | TA-D85-D99 | JPEG normalization, limits/hash/privacy, quota, durable save, sync, immutability and encrypted/private storage. |
| RPT | Reporting / History / Dossier / Audit | TA-D100-D115 | Nine reports, frozen-source semantics, reconciliation, history states, dossier, audit and offline completeness. |
| UI | UI / navigation / accessibility | TA-D116-D130 + TA-D219-D226 | 43-component traceability, Navigation 3/context/predictive back, edge-to-edge/insets/IME, window-adaptive classes, Location List/Map, visual regression/Stitch floor, large text and accessibility. |
| SEC | Security | TA-D131-D140 + SEC-T01-T40 + SEC-AC01-AC25 | Mandatory security regression and release gates, including tenant isolation, credentials, TLS, encryption and abuse controls. |
| PERF | Performance / reliability | TA-D141-D150 | Representative-scale field lists, package, sync backlog, reports, Evidence, process death, network transitions and soak. |
| UAT | User Acceptance | TA-D183-D192 | Admin and Standard canonical release-candidate journeys including offline/reconnect and official reporting/history. |

## 23. Release gate checklist

| Gate | Requirement |
|---|---|
| REL-G01 | Production-candidate build/compile/static checks pass. |
| REL-G02 | Required unit/domain suite passes. |
| REL-G03 | F01-F10 Calculation golden regression passes. |
| REL-G04 | Database schema/constraint/migration suite passes. |
| REL-G05 | API contract plus authorization/tenant-isolation suite passes. |
| REL-G06 | Offline/sync mandatory regression suite passes. |
| REL-G07 | Stocktaking/Campaign/Close critical workflow suite passes. |
| REL-G08 | Evidence integrity/storage suite passes. |
| REL-G09 | Reporting/History/Dossier/Audit mandatory suite passes. |
| REL-G10 | SEC-T01-SEC-T40 pass and SEC-AC01-SEC-AC25 are satisfied. |
| REL-G11 | Critical UI/navigation/predictive-back/edge-to-edge/adaptive/accessibility/Location-map suite passes. |
| REL-G17 | Required frozen-component/representative-screen visual regression and Stitch visual-floor evidence passes. |
| REL-G18 | R8 full mode plus required Baseline/Startup Profile and Macrobenchmark release-performance evidence passes. |
| REL-G12 | Production migration rehearsal against representative populated data passes. |
| REL-G13 | Staging smoke/E2E canonical journey passes. |
| REL-G14 | No open S0/S1 defects. |
| REL-G15 | Every accepted S2 has an explicit documented waiver. |
| REL-G16 | RVM has no unmapped/unverified critical frozen requirement. |
| REL-G17 | UAT sign-off passes for the release candidate. |
| REL-G18 | Production artifact security/configuration checks pass. |
| REL-G19 | No unresolved known data-integrity/reconciliation/migration defect exists. |
| REL-G20 | Release evidence/report is archived for the exact candidate build. |

## 24. Requirements Verification Matrix schema

| Field | Required meaning |
|---|---|
| Requirement/Decision ID | Frozen source identifier, e.g. BR/FC/IC/DM/DB/OS/CE/CO/EV/RP/SEC. |
| Source document/version | Exact frozen document and version. |
| Component/module | Implementation area responsible for behavior. |
| Test ID(s) | Executable/manual verification references. |
| Test level | Unit, DB, repository, API, integration, UI, E2E, security, UAT. |
| Automation | Automated or manual with rationale. |
| Environment | Local/CI/integration/Staging/approved Production smoke. |
| Result | Pass/Fail/Blocked/Not Run. |
| Evidence | CI run, artifact, log, screenshot, report or signed UAT record. |
| Build/version | Exact candidate build and relevant schema/API version. |

## 25. TA-D01-TA-D232 approved decision register

| ID | Area | Frozen V6 outcome |
|---|---|---|
| **TA-D001** | Strategy and governance | Doc18 is the V6 contract for test strategy, verification, acceptance and release quality gates. |
| **TA-D002** | Strategy and governance | Tests verify frozen Documents 01-17 and do not redefine business behavior. |
| **TA-D003** | Strategy and governance | A test-versus-contract conflict is resolved by fixing test/implementation or escalating the inconsistency, never by silently changing the contract. |
| **TA-D004** | Strategy and governance | Testing is risk-based and layered, with highest automated density on calculation, stocktaking, sync, close, authorization, persistence and immutable history. |
| **TA-D005** | Strategy and governance | V6 uses a test pyramid: many unit/domain tests, then repository/database/API contract tests, integration tests, and fewer E2E/UI tests. |
| **TA-D006** | Strategy and governance | Manual testing complements automation for exploratory UX, device behavior and UAT; it does not replace automated regression for critical rules. |
| **TA-D007** | Strategy and governance | Tests define reproducible preconditions, fixtures/data, action, expected result and isolated cleanup/state. |
| **TA-D008** | Strategy and governance | Deterministic fixtures are mandatory where results are contractual; random/fuzz testing may supplement but not replace them. |
| **TA-D009** | Test levels | Unit tests cover pure domain rules, validation, normalization, calculations, state transitions and semantically contractual adapters. |
| **TA-D010** | Test levels | Database tests cover Room DAOs/transactions/migrations and PostgreSQL schema constraints/transactions. |
| **TA-D011** | Test levels | Repository tests cover local-first reads, atomic local write plus outbox, mapping and error propagation. |
| **TA-D012** | Test levels | API contract tests cover request/response schema, error envelope, authentication, authorization, idempotency, entityVersion and pagination. |
| **TA-D013** | Test levels | Integration tests cover Android persistence/repository/sync and backend database/API workflows. |
| **TA-D014** | Test levels | UI tests cover navigation, permissions, state rendering, validation surfaces, accessibility and adaptive behavior. |
| **TA-D015** | Test levels | End-to-end tests cover canonical journeys from Sign In through Completed Campaign and reporting. |
| **TA-D016** | Test levels | SEC-T01-SEC-T40 from Doc17 are a mandatory verification set. |
| **TA-D017** | Test levels | UAT validates usability and business acceptance on a release candidate without redefining frozen requirements. |
| **TA-D018** | Calculation Engine | All F01-F10 geometry families have canonical golden vectors. |
| **TA-D019** | Calculation Engine | Each family tests nominal values, valid boundaries, invalid boundaries, applicable zero/empty paths and representative decimal precision. |
| **TA-D020** | Calculation Engine | Golden expected results are frozen independently of production code; tests never derive expected values by calling the same production formula. |
| **TA-D021** | Calculation Engine | MEAN_ULLAGE_V1, LEVEL_BASED_V1, effective fill height, h<0 and h>maximumFillHeight are tested; invalid height is never clamped. |
| **TA-D022** | Calculation Engine | Measurement cardinality 1-5 is tested and >5 is rejected. |
| **TA-D023** | Calculation Engine | Canonical unit normalization is tested. |
| **TA-D024** | Calculation Engine | No intermediate rounding is verified; persistence is scale 6 HALF_UP. |
| **TA-D025** | Calculation Engine | Quantity tests verify M = V * ActualBulkDensity, density >0 and Mark Empty exact zero. |
| **TA-D026** | Calculation Engine | Reference Test Weight never becomes fallback density. |
| **TA-D027** | Calculation Engine | Moisture/impurities never automatically change Physical Quantity. |
| **TA-D028** | Calculation Engine | Validated/final historical calculations are not recalculated after current Geometry/Commodity changes. |
| **TA-D029** | Calculation Engine | Any Calculation Engine/formula change requires full golden regression before release. |
| **TA-D030** | Database and migrations | Every Production Room schema migration has automated migration testing from every relevant supported version to target. |
| **TA-D031** | Database and migrations | PostgreSQL migrations are tested against representative populated data, not only empty databases. |
| **TA-D032** | Database and migrations | Migration testing explicitly verifies no destructive data loss. |
| **TA-D033** | Database and migrations | Doc10 constraints, uniqueness, FKs and checks have negative tests as well as happy paths. |
| **TA-D034** | Database and migrations | Transaction tests cover Save & Launch, Reset Validation, Close, local business write plus outbox and other frozen atomic operations. |
| **TA-D035** | Database and migrations | Campaign Code allocation has concurrency testing for atomic uniqueness. |
| **TA-D036** | Database and migrations | Tenant-scope constraints have cross-Organization negative tests. |
| **TA-D037** | Database and migrations | Immutable versions/snapshots/events have mutation, repoint and delete rejection tests. |
| **TA-D038** | Offline and sync | Doc12 mandatory tests are release-required regression tests. |
| **TA-D039** | Offline and sync | Local save is tested with network unavailable and survives process restart. |
| **TA-D040** | Offline and sync | Business mutation plus SyncOperation is verified as one local transaction. |
| **TA-D041** | Offline and sync | Timeout/5xx/restart retry preserves operation/idempotency identity and never duplicates the business action. |
| **TA-D042** | Offline and sync | Same-entity FIFO and dependency ordering have deterministic tests. |
| **TA-D043** | Offline and sync | Overlapping multi-device edits create explicit conflict without auto-merge or force overwrite. |
| **TA-D044** | Offline and sync | 401, 403, 429 Retry-After, transient 5xx and permanent domain errors have distinct tests. |
| **TA-D045** | Offline and sync | Package download tests staging, verify, atomic switch and preservation of the prior valid package on failure. |
| **TA-D046** | Offline and sync | Manifest/hash corruption blocks package use. |
| **TA-D047** | Offline and sync | Known revocation/Completed state blocks incompatible new mutations without deleting recoverable unsynced data. |
| **TA-D048** | Offline and sync | Evidence metadata-to-binary dependency-to-verification/final ACK is tested end-to-end. |
| **TA-D049** | Offline and sync | Airplane-mode regression is mandatory for the canonical assigned-Campaign stocktaking flow. |
| **TA-D050** | Offline and sync | Restart with IN_FLIGHT and no ACK retries the same operation identity. |
| **TA-D051** | Campaign, Stocktaking and Close | Canonical Campaign E2E is Create -> Scope/Team -> Save & Launch -> Active -> field work -> Review -> Close -> Completed -> Final Snapshot/report. |
| **TA-D052** | Campaign, Stocktaking and Close | Save & Launch tests guards, atomic CampaignSnapshot creation, frozen Scope/version context and ACTIVE state. |
| **TA-D053** | Campaign, Stocktaking and Close | Failed Save & Launch cannot create a partially launched Campaign or incomplete snapshot. |
| **TA-D054** | Campaign, Stocktaking and Close | Campaign Scope is immutable after launch and negative mutation tests are required. |
| **TA-D055** | Campaign, Stocktaking and Close | Team Assignment remains Admin-only editable while Active; launch team snapshot is never rewritten. |
| **TA-D056** | Campaign, Stocktaking and Close | Removed team members lose new Stocktaking mutation rights; later unauthorized offline sync is rejected. |
| **TA-D057** | Campaign, Stocktaking and Close | Exactly one StocktakingRecord per Campaign x Storage Space is verified under retry/concurrency. |
| **TA-D058** | Campaign, Stocktaking and Close | Non-empty validation covers Measurements, CommodityVersion, Actual Bulk Density, Ownership and Client when Third-Party. |
| **TA-D059** | Campaign, Stocktaking and Close | Owned does not require Client; Third-Party without Client is rejected. |
| **TA-D060** | Campaign, Stocktaking and Close | Mark Empty produces Validated, isEmpty=true, Volume=0 and Quantity=0 without fabricated Commodity/Density/Ownership/Client. |
| **TA-D061** | Campaign, Stocktaking and Close | Validate does not automatically resolve Open Issues. |
| **TA-D062** | Campaign, Stocktaking and Close | Reset Validation is Admin-only and Active-only, creates revision N+1 WORKING and preserves the prior validated revision immutable. |
| **TA-D063** | Campaign, Stocktaking and Close | Reset Validation on Completed Campaign is rejected. |
| **TA-D064** | Campaign, Stocktaking and Close | Issue OPEN->RESOLVED and RESOLVED->OPEN before Close are tested with append-only status/resolution history. |
| **TA-D065** | Campaign, Stocktaking and Close | Close revalidates all guards server-side; client readiness is insufficient. |
| **TA-D066** | Campaign, Stocktaking and Close | Every Close Guard has at least one individual negative test. |
| **TA-D067** | Campaign, Stocktaking and Close | Successful Close creates exactly one FinalInventorySnapshot and exactly one entry per scoped Space atomically. |
| **TA-D068** | Campaign, Stocktaking and Close | Failed Close leaves Campaign Active and creates zero partial Final Snapshot. |
| **TA-D069** | Campaign, Stocktaking and Close | Completed Campaign mutation attempts are rejected through normal client/API paths. |
| **TA-D070** | Campaign, Stocktaking and Close | Master-data changes after Close never alter Final Snapshot/history. |
| **TA-D071** | API and authorization contract testing | All Doc11 endpoints have contract tests for method/path, required headers, request schema, success schema and documented errors. |
| **TA-D072** | API and authorization contract testing | Protected endpoints have negative unauthenticated-access tests. |
| **TA-D073** | API and authorization contract testing | Role-sensitive endpoints explicitly test Admin versus Standard. |
| **TA-D074** | API and authorization contract testing | Tenant resources test cross-Organization access using a valid UUID from another tenant. |
| **TA-D075** | API and authorization contract testing | Campaign Stocktaking mutations test Campaign Team authorization separately from Role. |
| **TA-D076** | API and authorization contract testing | Entity-version mutations test current-version success and stale-version conflict. |
| **TA-D077** | API and authorization contract testing | Idempotent mutations replay same key and same payload without duplicate business action. |
| **TA-D078** | API and authorization contract testing | Reusing an Idempotency-Key with different payload is rejected according to the API contract. |
| **TA-D079** | API and authorization contract testing | API error envelope is contract-tested for stable machine code, correlation context and field errors where defined. |
| **TA-D080** | API and authorization contract testing | Cursor pagination tests first/middle/final pages, opaque cursor and no duplicate/missing rows in a stable dataset. |
| **TA-D081** | API and authorization contract testing | Unsupported/invalid filters, sort fields and malformed identifiers have negative tests. |
| **TA-D082** | API and authorization contract testing | Immutable resources have direct API mutation rejection tests. |
| **TA-D083** | API and authorization contract testing | Client-supplied organizationId, role or userId can never grant authority. |
| **TA-D084** | API and authorization contract testing | Authentication contract tests include Sign In, refresh rotation, Sign Out, Disabled User and password-reset lifecycle. |
| **TA-D085** | Evidence | PHOTO and NOTE have distinct unit/domain/integration tests. |
| **TA-D086** | Evidence | PHOTO tests verify EXIF orientation correction, canonical JPEG, long edge <=2048 px and no upscale. |
| **TA-D087** | Evidence | Target <=1.5 MB and hard rejection >3 MB are tested. |
| **TA-D088** | Evidence | SHA-256 is verified over exact canonical JPEG bytes. |
| **TA-D089** | Evidence | GPS/unnecessary EXIF and device identifiers are absent from canonical Evidence metadata. |
| **TA-D090** | Evidence | PHOTO quota is max 5 per StocktakingRevision and max 5 per Issue; the sixth is rejected. |
| **TA-D091** | Evidence | NOTE does not consume PHOTO quota and validates trimmed non-empty text and maximum length. |
| **TA-D092** | Evidence | Local Evidence success appears only after durable private file, metadata and outbox commit. |
| **TA-D093** | Evidence | Failure between file creation, metadata and outbox cannot produce false Saved state. |
| **TA-D094** | Evidence | Evidence transitions LOCAL_PENDING -> UPLOADING -> SYNCED and SYNC_ERROR have transition/retry tests. |
| **TA-D095** | Evidence | Retry preserves Evidence identity/hash and never creates duplicate official Evidence. |
| **TA-D096** | Evidence | Server MIME/size/hash mismatch blocks finalization. |
| **TA-D097** | Evidence | Validated Evidence mutation/removal is rejected. |
| **TA-D098** | Evidence | Working Evidence removal/cleanup respects audit, retention and reference safety. |
| **TA-D099** | Evidence | Production verification covers encrypted local PHOTO and private authorized remote retrieval. |
| **TA-D100** | Reports, History, Dossier and Audit | All nine canonical report types have dataset-level regression tests. |
| **TA-D101** | Reports, History, Dossier and Audit | Completed official reports read only FinalInventorySnapshot and never recalculate history. |
| **TA-D102** | Reports, History, Dossier and Audit | Active reporting is explicitly non-final in UI/export tests. |
| **TA-D103** | Reports, History, Dossier and Audit | UI/PDF/CSV for one request reconcile semantically to the same authoritative dataset. |
| **TA-D104** | Reports, History, Dossier and Audit | Physical Inventory totals are the reference totals for cross-report reconciliation. |
| **TA-D105** | Reports, History, Dossier and Audit | Inventory by Location/Commodity/Ownership totals reconcile to the same Physical Inventory selection. |
| **TA-D106** | Reports, History, Dossier and Audit | Capacity tests row utilization and aggregate sum(Volume)/sum(Capacity)*100, never mean row percentages. |
| **TA-D107** | Reports, History, Dossier and Audit | Worksheet tests frozen calculation trace and forbids historical recalculation. |
| **TA-D108** | Reports, History, Dossier and Audit | History tests 2..N Completed Campaigns and COMPARABLE, SCOPE_ADDED, SCOPE_REMOVED and CONTEXT_CHANGED. |
| **TA-D109** | Reports, History, Dossier and Audit | History previous quantity zero produces frozen N/A percentage semantics. |
| **TA-D110** | Reports, History, Dossier and Audit | Dossier tests frozen content and PDF-only dossier export. |
| **TA-D111** | Reports, History, Dossier and Audit | Dossier never grants Standard access to Admin-only Audit Trail content. |
| **TA-D112** | Reports, History, Dossier and Audit | Audit Trail is Admin-only, read-only and tests filtering/detail/export. |
| **TA-D113** | Reports, History, Dossier and Audit | Incomplete/partial offline report sources cannot be exported as final/complete reports. |
| **TA-D114** | Reports, History, Dossier and Audit | Report integrity/reconciliation failure blocks official export where Doc16 requires it. |
| **TA-D115** | Reports, History, Dossier and Audit | PDF and CSV have semantic regression tests; file existence alone is insufficient. |
| **TA-D116** | UI, navigation, adaptive and accessibility | All 43 major UI components have acceptance traceability; critical components have automated UI coverage. |
| **TA-D117** | UI, navigation, adaptive and accessibility | Navigation 3 type-safe destinations test forward navigation, Back/context restoration, process recreation and deep-link guards. |
| **TA-D118** | UI, navigation, adaptive and accessibility | STK-05 -> STK-03 restores Storage Unit and relevant list/scroll context. |
| **TA-D119** | UI, navigation, adaptive and accessibility | Bottom navigation is exactly Dashboard / Inventory Campaigns / Stocktaking / Locations / More and is absent from authentication. |
| **TA-D120** | UI, navigation, adaptive and accessibility | Navigation/deep-link tests cannot bypass authentication, Organization scope, role, Campaign access or entity state. |
| **TA-D121** | UI, navigation, adaptive and accessibility | Critical UI is verified across Compact/Medium/Expanded windows, portrait/landscape, resize/multi-window contexts, and supported posture changes using representative devices/emulators. |
| **TA-D122** | UI, navigation, adaptive and accessibility | Adaptive testing requires equivalent business capability, permissions and critical information without clipping/overlap, not pixel identity. |
| **TA-D123** | UI, navigation, adaptive and accessibility | Large-text tests verify primary actions, labels and critical values without clipping. |
| **TA-D124** | UI, navigation, adaptive and accessibility | Accessibility tests verify semantics/labels, focus order, touch targets and content descriptions for interactive/non-text controls. |
| **TA-D125** | UI, navigation, adaptive and accessibility | Critical status is never communicated by color alone. |
| **TA-D126** | UI, navigation, adaptive and accessibility | Field validation is associated with the relevant control/action. |
| **TA-D127** | UI, navigation, adaptive and accessibility | Read-only versus editable state is tested visually/semantically on critical workflows. |
| **TA-D128** | UI, navigation, adaptive and accessibility | Large STK-03 datasets verify efficient/lazy rendering and usability without mandatory network page fetch. |
| **TA-D129** | UI, navigation, adaptive and accessibility | UI tests verify canonical English terminology/enums for business-critical labels/statuses. |
| **TA-D130** | UI, navigation, adaptive and accessibility | Screenshot/golden tests supplement but never replace semantic/navigation/accessibility tests; frozen design-system primitives and representative critical screens require visual-regression evidence. |
| **TA-D131** | Security verification | SEC-T01-SEC-T40 are mandatory release regression tests. |
| **TA-D132** | Security verification | SEC-AC01-SEC-AC25 are mandatory security release gates. |
| **TA-D133** | Security verification | Security testing includes client manipulation/API-level testing; UI-only permission tests are insufficient. |
| **TA-D134** | Security verification | Production artifacts are scanned/tested for secrets and debug trust/logging configuration. |
| **TA-D135** | Security verification | TLS/cleartext tests run against Production-equivalent network configuration. |
| **TA-D136** | Security verification | At-rest verification covers encrypted Room and local PHOTO Evidence. |
| **TA-D137** | Security verification | Logs/diagnostic packages are inspected for forbidden secrets/payloads. |
| **TA-D138** | Security verification | Sign-In/reset throttling and token lifetime/rotation/replay are mandatory security integration tests. |
| **TA-D139** | Security verification | Cross-tenant authorization regression is release-blocking. |
| **TA-D140** | Security verification | Any failed mandatory security gate blocks Production release. |
| **TA-D141** | Performance and reliability | V6 uses scenario-based performance acceptance; engineering thresholds are version-controlled test configuration and may evolve without Product/Baseline approval when frozen behavior is unchanged. |
| **TA-D142** | Performance and reliability | STK-03 is tested with representative large Storage Unit data for smooth/lazy navigation without memory failure. |
| **TA-D143** | Performance and reliability | Campaign offline package is tested at representative production scale for download/verify/local navigation. |
| **TA-D144** | Performance and reliability | Sync tests the 100-operation batch boundary and sustained backlog processing without duplication/data loss. |
| **TA-D145** | Performance and reliability | Reports test representative large Completed Campaign generation/view/export without timeout, corruption or inconsistent totals. |
| **TA-D146** | Performance and reliability | Evidence tests maximum-size canonical PHOTO and multiple queued uploads without corruption/duplicates. |
| **TA-D147** | Performance and reliability | Reliability soak testing covers repeated local writes, sync retries and app restarts. |
| **TA-D148** | Performance and reliability | Process-death tests are mandatory for critical local-first mutation paths. |
| **TA-D149** | Performance and reliability | Network transition tests cover online->offline, offline->online and unstable/intermittent connectivity. |
| **TA-D150** | Performance and reliability | Performance regression is compared with the approved release baseline; material degradation in critical field workflows is investigated before release. |
| **TA-D151** | Test environments and data | Automated unit/domain tests run locally/CI without Production dependency. |
| **TA-D152** | Test environments and data | Integration/API/E2E use isolated test environments or ephemeral dependencies where practical. |
| **TA-D153** | Test environments and data | Staging is Production-like for schema, API/security configuration, object-storage behavior and migration/release verification. |
| **TA-D154** | Test environments and data | Automated tests never run destructive scenarios against Production business data. |
| **TA-D155** | Test environments and data | Production validation is limited to approved non-destructive smoke/health checks. |
| **TA-D156** | Test environments and data | Test fixtures use synthetic data by default. |
| **TA-D157** | Test environments and data | Production personal/business data is not copied to Dev/Test without an explicitly authorized sanitized process. |
| **TA-D158** | Test environments and data | Canonical fixtures include all 10 geometry families, 24 governed commodities, Owned/Third-Party, Client, Empty, Issues, Evidence, Active/Completed and conflict states. |
| **TA-D159** | Test environments and data | Time-sensitive tests use controllable clocks/fixed UTC instants. |
| **TA-D160** | Test environments and data | Concurrency tests use deterministic coordination where possible rather than fragile timing sleeps. |
| **TA-D161** | Defect severity | S0 Blocker includes data loss/corruption, wrong official inventory quantity/snapshot, security/tenant breach, destructive migration, inability to complete core workflow or immutable-history mutation. |
| **TA-D162** | Defect severity | S1 Critical includes major frozen-rule break, unusable critical offline workflow, repeatable duplicate mutation, Close/report integrity failure or serious authorization defect without confirmed breach. |
| **TA-D163** | Defect severity | S2 Major substantially impairs an important feature but has a safe workaround and does not corrupt authoritative data. |
| **TA-D164** | Defect severity | S3 Minor is a limited non-critical UX/presentation defect without business correctness, security or data-integrity impact. |
| **TA-D165** | Defect severity | Any open S0 or S1 blocks Production release. |
| **TA-D166** | Defect severity | S2 may be accepted only through explicit release waiver with owner, impact, workaround and follow-up. |
| **TA-D167** | Defect severity | S3 does not automatically block release but remains tracked. |
| **TA-D168** | Defect severity | Severity is determined by impact/risk, not ease of repair. |
| **TA-D169** | Coverage policy | V6 does not use a global numeric code-coverage percentage as a release gate. |
| **TA-D170** | Coverage policy | An arbitrary percentage can over-reward trivial code while critical rules remain untested. |
| **TA-D171** | Coverage policy | Release gating is based on requirements/risk coverage, not raw line coverage. |
| **TA-D172** | Coverage policy | 100% of frozen critical business rules/release gates identified in the traceability matrix require test evidence. |
| **TA-D173** | Coverage policy | F01-F10, Close Guards, critical Stocktaking transitions, offline durability/idempotency/conflict, tenant authorization, migration safety and mandatory security gates require explicit automated coverage. |
| **TA-D174** | Coverage policy | Line/branch coverage may be collected as engineering diagnostic/trend data but does not independently decide release. |
| **TA-D175** | Coverage policy | New code implementing a frozen critical rule without an associated test cannot be considered Done. |
| **TA-D176** | Traceability | V6 maintains a Requirements Verification Matrix (RVM). |
| **TA-D177** | Traceability | RVM links Requirement/Decision ID -> component/module -> test ID(s) -> test level -> automated/manual -> result/evidence. |
| **TA-D178** | Traceability | Verifiable frozen BR/FC/IC/DM/DB/OS/CE/CO/EV/RP/SEC decisions are mapped in the RVM. |
| **TA-D179** | Traceability | A critical requirement without test mapping is a release gap even when the overall suite is green. |
| **TA-D180** | Traceability | A test failure must identify the affected contractual requirement. |
| **TA-D181** | Traceability | Obsolete tests for changed/removed requirements are explicitly updated/removed and never retained as false authority. |
| **TA-D182** | Traceability | Release evidence records the exact build and relevant schema/API versions plus mandatory-suite results. |
| **TA-D183** | User Acceptance Testing | UAT runs on a release candidate in Production-like Staging. |
| **TA-D184** | User Acceptance Testing | UAT uses synthetic/approved test Organizations and Campaigns, not implicit Production data. |
| **TA-D185** | User Acceptance Testing | Minimum UAT personas are Admin and Standard. |
| **TA-D186** | User Acceptance Testing | Admin UAT covers relevant master setup, Campaign launch, team/scope, review/close, reports and administration. |
| **TA-D187** | User Acceptance Testing | Standard UAT covers assigned Campaign, field Stocktaking, offline operation, Issue/Evidence and authorized reports. |
| **TA-D188** | User Acceptance Testing | UAT includes at least one offline->reconnect->sync scenario. |
| **TA-D189** | User Acceptance Testing | UAT includes Third-Party+Client, Owned, Mark Empty and Issue flow. |
| **TA-D190** | User Acceptance Testing | UAT includes Completed Campaign plus official reporting/history. |
| **TA-D191** | User Acceptance Testing | UAT sign-off confirms frozen workflow acceptance and cannot waive security/data-integrity blockers. |
| **TA-D192** | User Acceptance Testing | UAT defects use S0-S3 and the same release gates. |
| **TA-D193** | Exact release gates | Production-candidate build/compile/static checks pass. |
| **TA-D194** | Exact release gates | Required unit/domain suite passes. |
| **TA-D195** | Exact release gates | F01-F10 Calculation golden regression passes. |
| **TA-D196** | Exact release gates | Database schema/constraint/migration suite passes. |
| **TA-D197** | Exact release gates | API contract plus authorization/tenant-isolation suite passes. |
| **TA-D198** | Exact release gates | Offline/sync mandatory regression suite passes. |
| **TA-D199** | Exact release gates | Stocktaking/Campaign/Close critical workflow suite passes. |
| **TA-D200** | Exact release gates | Evidence integrity/storage suite passes. |
| **TA-D201** | Exact release gates | Reporting/History/Dossier/Audit mandatory suite passes. |
| **TA-D202** | Exact release gates | SEC-T01-SEC-T40 pass and SEC-AC01-SEC-AC25 are satisfied. |
| **TA-D203** | Exact release gates | Critical UI/navigation/predictive-back/edge-to-edge/adaptive/accessibility/Location-map suite passes. |
| **TA-D204** | Exact release gates | Production migration rehearsal against representative populated data passes. |
| **TA-D205** | Exact release gates | Staging smoke/E2E canonical journey passes. |
| **TA-D206** | Exact release gates | No open S0/S1 defects. |
| **TA-D207** | Exact release gates | Every accepted S2 has an explicit documented waiver. |
| **TA-D208** | Exact release gates | RVM has no unmapped/unverified critical frozen requirement. |
| **TA-D209** | Exact release gates | UAT sign-off passes for the release candidate. |
| **TA-D210** | Exact release gates | Production artifact security/configuration checks pass. |
| **TA-D211** | Exact release gates | No unresolved known data-integrity/reconciliation/migration defect exists. |
| **TA-D212** | Exact release gates | Release evidence/report is archived for the exact candidate build. |
| **TA-D213** | Failure and flaky-test policy | A mandatory test failure is real until a demonstrated cause exists. |
| **TA-D214** | Failure and flaky-test policy | Rerun may aid diagnosis but a later green rerun does not automatically erase the initial failure. |
| **TA-D215** | Failure and flaky-test policy | A flaky mandatory test is fixed or formally quarantined with owner/reason; quarantine cannot hide a critical-rule verification gap. |
| **TA-D216** | Failure and flaky-test policy | A critical requirement without another valid verification cannot be quarantined for Production release. |
| **TA-D217** | Failure and flaky-test policy | Tests avoid arbitrary sleeps when deterministic synchronization/clock control is available. |
| **TA-D218** | Failure and flaky-test policy | CI retains sufficient sanitized failure artifacts/logs for reproduction without exposing secrets. |
| **TA-D219** | Android platform UI | Predictive Back passes root/top-level, nested, overlay/sheet and approved unsaved-edit guard scenarios on supported API levels. |
| **TA-D220** | Android platform UI | Edge-to-edge/system-bar/cutout/gesture/IME tests prove required actions, messages and critical values remain reachable/visible. |
| **TA-D221** | Android platform UI | Lifecycle-aware state collection is verified; inactive destinations do not keep unnecessary UI-bound collection active and durable outcomes survive required recreation. |
| **TA-D222** | Location map | LOC-01 List and Map use the same authorized filtered Location dataset and marker selection preserves canonical LOC-02 identity/navigation. |
| **TA-D223** | Location map | Locations without coordinates remain available in List; invalid/partial coordinates are rejected; no device-location permission is requested. |
| **TA-D224** | Location map | Basemap/network failure does not block stored Location list/hierarchy/coordinates and does not create false business sync failure. |
| **TA-D225** | Visual quality | Approved Stitch references are the minimum visual-quality floor for representative screens; screenshot/golden regression is required for frozen design-system primitives and critical representative screens. |
| **TA-D226** | Accessibility | Map functionality always has an equivalent accessible List path; critical chart/map meaning cannot be map/color-only. |
| **TA-D227** | Release performance | Production release verifies R8 full-mode shrinking/optimization and required keep/serialization rules. |
| **TA-D228** | Release performance | Baseline/Startup Profiles cover configured representative critical user journeys where supported and are regenerated/verified for release. |
| **TA-D229** | Release performance | Macrobenchmark verifies configured startup and interaction/jank scenarios; regression thresholds live in version-controlled engineering configuration and do not require Product approval. |
| **TA-D230** | Implementation closure | A missing Product/Baseline approval is never a valid reason to defer an ordinary non-semantic implementation decision already permitted by Documents 01–21; IADR records material technical choices. |
| **TA-D231** | Identity API provenance | `UserMembershipResponse` mapping proves User and Membership metadata/entityVersion remain distinct; canonical User persistence never fabricates User metadata from Membership metadata. |
| **TA-D232** | Environment provisioning | Development/Staging/Production base URLs/provider endpoints are supplied through controlled environment configuration, are absent as hard-coded Product semantics, and preserve the frozen `/api/v1`/security contract without Product approval. |

## 26. Cross-document handoff
- Doc19 RETENTION_POLICY must define retention/purge of test evidence, CI artifacts, UAT records, operational telemetry and other retained verification material without weakening frozen business/audit retention.
- Doc20 PRODUCTION_OPERATIONS must implement the release pipeline, Production-like Staging, secret-safe CI/CD, migration rehearsal, backup/restore verification, monitoring and non-destructive Production smoke checks required here.
- Any future product-contract change must update the RVM and affected tests before the changed behavior can pass a Production release gate.

## 27. Freeze statement

This document is the canonical APPROVED / FROZEN SiloMath V6 Test Strategy & Acceptance contract after the 2026-10-17 implementation-closure refreeze, defined by TA-D01-TA-D232, including layered risk-based testing, critical-rule traceability, mandatory Calculation/Sync/Security regression, adaptive/accessibility verification, defect severity, UAT, exact release gates and the policy of no arbitrary global code-coverage percentage as a release gate.
