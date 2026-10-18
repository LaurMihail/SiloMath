# SiloMath V6 — Android/Kotlin + UI/UX Best-Practice Audit

**Audit date:** 2026-10-17  
**Baseline:** SiloMath V6 APPROVED / FROZEN  
**Corpus:** Documents 01–21 from `21_UI_DESIGN_SYSTEM_FINAL.zip`  
**Visual benchmark:** supplied Stitch overview screenshot; screen-level detail is insufficient for pixel/content parity claims  
**External reference baseline:** current Android Developers / AndroidX guidance retrieved during audit

---

## 1. Executive Summary

### Verdict

**B. V6 IS FUNDAMENTALLY SOUND BUT REQUIRES LIMITED BASELINE MODERNIZATION.**

SiloMath V6 is architecturally strong. Its layered architecture, strict module ownership, local-first source-of-truth model, atomic business-mutation + SyncOperation rule, durable WorkManager-based synchronization, typed API/error model, Room migration discipline, security/retention controls, adaptive intent, accessibility baseline, and test traceability are materially aligned with modern Android engineering practice.

The audit did **not** identify a reason to redesign the domain model, offline-first architecture, 19-module topology, calculation engine boundary, authorization model, Room/API boundaries, or the core synchronization semantics.

The material modernization need is concentrated in the Android presentation/platform contract:

1. Navigation baseline is frozen to **Navigation Compose**, while current Android guidance strongly recommends **Navigation 3** for new single-activity applications. Navigation Compose remains supported, so this is modernization rather than a critical defect.
2. **Predictive Back** is not specified or tested, despite modern Android target behavior making supported back APIs increasingly important.
3. **Edge-to-edge / WindowInsets / IME inset handling** are not explicitly part of the frozen UI contract, although edge-to-edge is enforced for current target SDK levels.
4. Lifecycle-aware Compose state collection behavior is not explicit.
5. The one-off UI event wording can be read as endorsing transient event channels, while current Android architecture guidance favors state-driven handling.
6. Baseline Profiles / Macrobenchmark / R8-based performance-release engineering are not explicitly frozen into the quality/production contract.
7. Adaptive rules are good but expressed partly in older width/device terminology rather than a current window-adaptive/posture model.
8. The Stitch quality floor requested by Product is not actually frozen as an acceptance requirement in Doc06/18/21.
9. Location maps/geospatial UI are **not currently supported by the data/API/UI baseline**. Adding them is a legitimate modernization candidate, but requires a controlled Baseline Change because Location has no latitude/longitude/geospatial contract and LOC-01 has an exhaustive list-only control contract.
10. Doc20 contains an explicit stale contradiction saying no Doc21 is introduced, although Doc02 freezes a 21-document set and Doc21 exists.

### Audit coverage

- Documents audited: **21/21**
- Source lines inspected/scanned: **12,365**
- Markdown headings/sections indexed: **871**
- Bullet/invariant statements indexed: **2,607**
- Full-screen contracts: **36**
- Sheet contracts: **7**
- Major UI contracts: **43**

### Confirmed findings

| Severity | Count |
|---|---:|
| Critical | 0 |
| High | 2 |
| Medium | 7 |
| Low | 5 |
| Info / modernization-only | 3 |
| **Total confirmed / actionable** | **17** |

### Baseline-change disposition

- **Required Baseline Change:** 1 editorial/governance correction (Doc20 contradiction)
- **Strongly Recommended Baseline Changes:** predictive back, edge-to-edge/insets, navigation technology modernization decision, lifecycle/state contract clarification, Stitch visual-quality floor
- **Optional Baseline Modernization:** maps/geospatial product capability
- **Implementation-only choices:** type-safe routes (if Navigation Compose retained), `collectAsStateWithLifecycle` concrete API, current adaptive library primitives, exact Flow operators, specific DI bindings, benchmark implementation details

---

## 2. Coverage Ledger

| Doc | Name | Lines | Headings | Coverage | Material finding summary |
|---:|---|---:|---:|---|---|
| 01 | PRODUCT_BASELINE | 831 | 41 | FULLY AUDITED | Navigation technology baseline should be reconsidered; otherwise strong |
| 02 | DECISION_REGISTER | 214 | 16 | FULLY AUDITED | 21-doc authority is correct; cross-conflicts with stale Doc20 text |
| 03 | BUSINESS_RULES | 224 | 12 | FULLY AUDITED | No material Android best-practice defect |
| 04 | USER_ROLES_PERMISSIONS | 243 | 19 | FULLY AUDITED | Strong server/client authorization separation |
| 05 | WORKFLOWS_STATE_MACHINES | 315 | 38 | FULLY AUDITED | No material Android defect; strong explicit state semantics |
| 06 | UI_SCREEN_SPECIFICATION_AND_NAVIGATION | 1691 | 124 | FULLY AUDITED | Needs predictive-back/edge-to-edge/window-adaptive clarification; maps require CR |
| 07 | ARCHITECTURE | 830 | 74 | FULLY AUDITED | Main modernization findings: nav, UI events, lifecycle collection, performance tooling |
| 08 | MODULE_ARCHITECTURE | 655 | 54 | FULLY AUDITED | 19-module topology valid; modern nav ownership can fit without new module |
| 09 | DATA_MODEL | 1167 | 65 | FULLY AUDITED | Strong model; geospatial data absent, so map capability requires CR |
| 10 | DATABASE_SCHEMA | 345 | 28 | FULLY AUDITED | Strong physical/Room separation and migration discipline |
| 11 | API_SPECIFICATION | 521 | 51 | FULLY AUDITED | Strong API contract; geospatial representation absent |
| 12 | OFFLINE_SYNC_SPECIFICATION | 645 | 37 | FULLY AUDITED | Stronger than generic Android guidance; one stale editorial sync/pull note |
| 13 | CALCULATION_ENGINE_SPECIFICATION | 507 | 35 | FULLY AUDITED | Excellent pure-Kotlin boundary; no material defect |
| 14 | COMMODITY_SPECIFICATION | 408 | 34 | FULLY AUDITED | No Android-specific material defect |
| 15 | EVIDENCE_STORAGE_SPECIFICATION | 429 | 36 | FULLY AUDITED | Strong bounded media pipeline and integrity semantics |
| 16 | REPORTING_SPECIFICATION | 518 | 37 | FULLY AUDITED | No material defect; charts remain presentation-only where justified |
| 17 | SECURITY_AUDIT_PRIVACY | 535 | 32 | FULLY AUDITED | Strong Keystore/backup/redaction/encryption boundaries |
| 18 | TEST_STRATEGY_ACCEPTANCE | 579 | 28 | FULLY AUDITED | Strong; should add predictive-back, edge-to-edge, macrobenchmark/baseline-profile gates |
| 19 | RETENTION_POLICY | 503 | 29 | FULLY AUDITED | Strong; no Android-specific material defect |
| 20 | PRODUCTION_OPERATIONS | 708 | 38 | FULLY AUDITED | Explicit Doc21 contradiction; performance/release tooling should be modernized |
| 21 | UI_DESIGN_SYSTEM_FINAL | 497 | 43 | FULLY AUDITED | Strong visual foundation; needs edge-to-edge/dynamic-color policy/Stitch floor clarification |

---

## 3. Android/Kotlin Compliance Scorecard

| Domain | Rating | Audit conclusion |
|---|---|---|
| Architecture | COMPLIANT | Strong layered boundaries and SoC |
| Kotlin | COMPLIANT | Coroutines/Flow/structured concurrency/pure engine well defined |
| Compose | PARTIAL | Good UDF/ViewModel rules; lifecycle collection and transient-event semantics need clarification |
| Navigation | PARTIAL | Solid semantic graph, but frozen Navigation Compose is behind current preferred Navigation 3 stack; predictive back missing |
| Modularization | COMPLIANT | 19-module graph is defensible and appropriately constrained |
| DI | COMPLIANT | Hilt ownership is clear and testable |
| Room | COMPLIANT | Strong migrations, transactions, constraints, local source-of-truth |
| Offline-first | COMPLIANT | Enterprise-grade and stronger than generic guidance |
| Sync | COMPLIANT | Durable, idempotent, ordered, conflict-explicit |
| Background work | COMPLIANT | WorkManager use is appropriate; Android 16 long-running quota nuance is implementation guidance |
| Network/API | COMPLIANT | Versioned API boundary, typed errors, idempotency, optimistic concurrency |
| Security | COMPLIANT | Strong Keystore, encryption, backup exclusion, redaction, tenant isolation |
| Privacy | COMPLIANT | Strong minimization/retention/logging controls |
| Adaptive UI | PARTIAL | Correct intent; terminology/primitives should be modernized around window adaptation/posture |
| Accessibility | COMPLIANT | 48dp targets, semantics, TalkBack, non-color state, scaling |
| Performance | PARTIAL | Runtime rules good; Baseline Profiles/Macrobenchmark/R8 release engineering absent |
| Testing | PARTIAL | Excellent breadth; missing modern nav/e2e/performance-specific acceptance |
| Gradle/build | PARTIAL | Good governance, but performance/shrinker release expectations not explicit |
| CI/CD | COMPLIANT/PARTIAL | Strong release gating; add performance tooling gates |
| Operations | COMPLIANT | Very strong operational/recovery model; one stale Doc21 contradiction |

---

## 4. Visual / UX Scorecard

| Dimension | Rating | Note |
|---|---|---|
| Visual hierarchy | COMPLIANT | Doc21 defines hierarchy, tokens, density and restrained B2B language |
| Information density | COMPLIANT | Explicitly favors scanability and efficient rows |
| Design-system maturity | COMPLIANT | Strong token/component foundation |
| Stitch parity | PARTIAL / NOT FULLY VERIFIABLE | Only zoomed-out overview supplied; individual screens are not readable enough for exact parity |
| Adaptive behavior | PARTIAL | Good compact/medium/expanded intent; update to current window-adaptive APIs/terminology |
| Navigation UX | PARTIAL | Semantic IA is strong; platform interaction contract needs predictive back/current nav stack |
| Forms | COMPLIANT | Units, validation, read-only distinction, input rules strong |
| Data visualization | PARTIAL | Allowed only with approved purpose; no rich visualization contract beyond reports/dashboard needs |
| Maps/geospatial readiness | NON-COMPLIANT FOR MAP FEATURE | No geo data contract/API/UI controls; map is not presently implementable canonically |
| Offline UX | COMPLIANT | Strong explicit local/pending/error/conflict distinctions |
| Sync UX | COMPLIANT | Clear non-color and non-false-success requirements |
| Empty/loading/error states | COMPLIANT | Defined globally and per-screen |
| Tablet experience | PARTIAL | Supported, but modern adaptive primitives/posture not explicit |
| Accessibility | COMPLIANT | Strong baseline |
| Enterprise polish | COMPLIANT AS SPEC / MUST VERIFY IN IMPLEMENTATION | Doc21 is substantially better than generic Material defaults |

---

## 5. Stitch Benchmark Inventory

The supplied Stitch overview demonstrates a large, coherent set of mobile screens/states and supports treating Stitch as a **minimum visual-density and visual-maturity reference**. The overview is too zoomed out to reliably read individual fields, exact states, typography, spacing, component behavior or per-screen navigation. Therefore:

- macro-level benchmark: **usable**;
- screen-by-screen parity: **INSUFFICIENT REFERENCE** for most screens;
- business semantics: **must never be inferred from Stitch**;
- final parity audit requires screen exports or readable screenshots.

Observed benchmark characteristics at overview level:

- consistent navy/white enterprise visual language;
- dense operational cards/lists/forms;
- repeated status/summary treatments;
- coherent mobile flow families;
- multiple long-form operational screens;
- modal/sheet-like interactions;
- strong visual consistency across many screens.

---

## 6. Screen-by-Screen Parity Matrix

Because the only supplied Stitch image is a zoomed-out board overview, the audit cannot honestly classify all 43 components as BELOW / AT / ABOVE at screen level. Every screen remains `INSUFFICIENT REFERENCE` until readable exports are supplied.

The baseline itself is capable of producing a visual system at or above the overview benchmark, **provided Doc21 is implemented rather than the current default Material template code**.

For Locations specifically, the baseline currently defines LOC-01 as search + lifecycle filter + lazy list + Admin add action. There is no map control in its exhaustive field contract and no geospatial data in the Location model. Therefore a modern map/list enhancement is not merely styling.

---

## 7. Doc21 Component Parity Matrix

| Area | Doc21 baseline | Assessment |
|---|---|---|
| Palette | Frozen slate/white + cobalt semantic system | Strong; suitable for industrial B2B |
| Typography | Inter, frozen scale | Strong; explicit identity beyond default Material |
| Spacing/grid | 4/8dp system | Strong |
| Shape/elevation | Restrained, low decoration | Strong |
| Navigation | Canonical 5 destinations, rail for wider widths | Strong IA; current platform primitive should be modernized |
| Cards/lists | Dense operational rows preferred | Strong |
| Status/sync | Text + semantic visual treatment | Strong |
| Forms | Explicit units/focus/error/read-only | Strong |
| Empty/error/offline | Explicit | Strong |
| Accessibility | 48dp, TalkBack, contrast, scaling | Strong |
| Adaptive | 600/840 width bands | Sound intent; terminology/API mapping should be modernized |
| Edge-to-edge | Not specified | Gap |
| Predictive back | Not specified | Gap |
| Dynamic color | Not explicitly allowed/disabled | Ambiguity; fixed frozen palette suggests it should not override canonical tokens |
| Maps | No shared map pattern | Conditional gap only if map capability is approved |
| Charts | Mentioned as possible purposeful visualization, no shared chart contract | Fine unless richer analytics are adopted |

---

# 8. Document-by-Document Audit

## 01_PRODUCT_BASELINE

**Strengths:** correct native Kotlin/Compose foundation; UDF; Hilt; Room; Retrofit/OkHttp; Coroutines/Flow; WorkManager; DataStore; strict local-first behavior.  
**Finding:** `Navigation Compose` is frozen as technology. Current Android architecture guidance now strongly recommends Navigation 3 for new single-activity applications. Navigation Compose is not deprecated, so this is not a hard incompatibility.  
**Result:** material modernization candidate, not a platform blocker.

## 02_DECISION_REGISTER

**Strengths:** authority/traceability model is unusually strong. FD-55/57 correctly freeze the 21-document set and Doc21 authority.  
**Finding:** conflicts with stale wording in Doc20 saying no Doc21 is introduced.  
**Result:** Doc02 is correct; Doc20 needs correction.

## 03_BUSINESS_RULES

No material Android/Kotlin best-practice defect found. Business rules are appropriately implementation-neutral and test-traceable.

## 04_USER_ROLES_PERMISSIONS

No material defect found. Authorization dimensions, offline authorization and mandatory server enforcement are technically sound and stronger than UI-only authorization anti-patterns.

## 05_WORKFLOWS_STATE_MACHINES

No material defect found. Explicit separation of domain states, operational classifications and sync/evidence states prevents common client-state modeling errors.

## 06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION

**Strengths:** exhaustive screen/field/control contracts, stable-ID navigation, preserved context, permission/offline/adaptive rules, accessibility requirements.  
**Findings:** predictive back and edge-to-edge behavior are not specified; adaptive rules do not explicitly reference current window-adaptive/posture semantics; Locations are list-only by contract.  
**Result:** limited baseline modernization recommended.

## 07_ARCHITECTURE

**Strengths:** excellent layering, local source-of-truth, transactional outbox, structured concurrency, pure calculation, explicit architecture seams.  
**Findings:**
- Navigation Compose technology choice is now behind the current preferred Navigation 3 stack.
- `One-off navigation/UI effects are separated from durable business state` is ambiguous and can encourage event-channel patterns that current Android guidance discourages.
- lifecycle-aware UI collection behavior is not explicit.
- performance section omits Baseline Profiles, Macrobenchmark and release shrinker/R8 expectations.
- edge-to-edge and predictive back are absent.

## 08_MODULE_ARCHITECTURE

No material topology defect found. The 19-module graph is pragmatic and sufficiently granular. `core:navigation` can own Navigation 3/type-safe destination contracts without adding a new production module.  
**Modernization:** navigation internals can change while preserving module ownership and IA.

## 09_DATA_MODEL

No material Android modeling defect found. Stable UUIDs, metadata/versioning, aggregates and historical invariants are strong.  
**Map impact:** Location contains name/code/address/city/country but no latitude/longitude/geometry/geocoding metadata. A canonical location map therefore needs a baseline data-model extension.

## 10_DATABASE_SCHEMA

No material defect found. Strong mapping separation, schema constraints, transaction boundaries, migration policy, non-destructive historical behavior and Room working-set boundary.

## 11_API_SPECIFICATION

No material defect found. Strong `/api/v1`, typed errors, lossless decimals, idempotency, optimistic concurrency, cursor pagination and stable IDs.  
**Map impact:** no canonical geospatial fields/endpoints are defined; adopting map coordinates requires API contract extension or an explicit client-only geocoding policy (the latter has privacy/offline consistency tradeoffs).

## 12_OFFLINE_SYNC_SPECIFICATION

No material architectural defect found. This is stronger than generic Android offline-first guidance.  
**Editorial finding:** it still says `/sync/pull` must be added to Doc11 during final QA, but Doc11 already contains it. Remove/update stale finalization wording.  
**WorkManager note:** Android 16 quota behavior for genuinely long-running workers should be handled in implementation/operations; current evidence files are capped at 3 MB, so the frozen WorkManager choice is not presently unsound.

## 13_CALCULATION_ENGINE_SPECIFICATION

No material defect found. Pure Kotlin, deterministic decimal pipeline, immutable versions and golden vectors are exemplary boundaries.

## 14_COMMODITY_SPECIFICATION

No material Android best-practice defect found.

## 15_EVIDENCE_STORAGE_SPECIFICATION

No material defect found. Canonical normalization, size cap, hashing, app-private/encrypted storage, durable metadata/binary ordering and non-public object references are strong.

## 16_REPORTING_SPECIFICATION

No material defect found. Reporting semantics are correctly derived from authoritative snapshots and prohibit misleading incomplete final exports. Richer charts may be added only as presentation of already-approved data, unless they introduce new analytics semantics.

## 17_SECURITY_AUDIT_PRIVACY

No material defect found. Keystore-backed credentials, encrypted local business data/evidence, Android backup exclusions, secret redaction, tenant isolation and secure environment separation align strongly with Android security principles.

## 18_TEST_STRATEGY_ACCEPTANCE

**Strengths:** excellent coverage of migrations, process death, airplane mode, sync ordering/idempotency, security, adaptive UI and accessibility.  
**Findings:** add explicit acceptance for predictive back; edge-to-edge/inset/IME safety; performance benchmarking using Macrobenchmark/Baseline Profiles where supported; release shrinker configuration verification. Visual regression should become stronger if Stitch parity is frozen as a product visual floor.

## 19_RETENTION_POLICY

No material Android defect found. Retention, purge dependency safety and backup re-disposition behavior are unusually thorough.

## 20_PRODUCTION_OPERATIONS

**Confirmed cross-document contradiction:** section 36 says `no Doc21 is introduced`, but Doc02 FD-55/FD-57 establishes a 21-document baseline and Doc21 is frozen. This must be corrected.  
**Modernization:** Android production performance should include explicit profile/benchmark/release shrinker evidence.

## 21_UI_DESIGN_SYSTEM_FINAL

**Strengths:** strong non-generic enterprise identity; explicit Inter typography; restrained surfaces; semantic state; accessibility; adaptive layout; reusable semantic components; anti-drift rules.  
**Findings:** no edge-to-edge/insets contract; no predictive-back visual/interactivity contract; dynamic-color behavior is not explicit; width categories use device-like wording (`Compact phone`) instead of purely window-adaptive semantics; Stitch is referenced as generation input but not frozen as a measurable minimum-quality acceptance floor.  
**Map note:** adding a map requires shared design patterns and accessibility alternatives only after product/data CR approval.

---

# 9. Master Findings Register

| ID | Document(s) | Category | Severity | Finding | Baseline change? |
|---|---|---|---|---|---|
| F-001 | 20 vs 02/21 | CROSS-DOC-CONFLICT | Medium | Doc20 says no Doc21 is introduced; baseline actually has 21 docs | YES, required editorial correction |
| F-002 | 01/07/08 | BP-GAP / MODERNIZATION | Medium | Navigation Compose frozen while Android now strongly recommends Navigation 3 for new single-activity apps | YES if technology baseline changes |
| F-003 | 06/07/18/21 | BP-GAP | High | Predictive Back absent | YES, strongly recommended |
| F-004 | 06/07/18/21 | BP-GAP | High | Edge-to-edge / WindowInsets / IME-inset behavior not frozen | YES, strongly recommended |
| F-005 | 07/08/18 | UNDER-SPECIFIED | Medium | Lifecycle-aware Compose state collection not explicit | Prefer baseline behavioral clarification; concrete API is implementation choice |
| F-006 | 07 | AMBIGUOUS | Medium | One-off UI effects wording can encourage event channels instead of state-driven handling | YES, clarification recommended |
| F-007 | 07/18/20 | PERFORMANCE-GAP | Medium | No Baseline Profiles / Macrobenchmark acceptance | YES for quality contract; implementation specifics remain open |
| F-008 | 20/07 | PERFORMANCE-GAP | Medium | R8/shrinker release behavior not explicit | YES/clarify release contract |
| F-009 | 07/21/06 | ADAPTIVE-GAP | Low | Width/device terminology is older than current window-adaptive/posture model | YES, limited wording modernization |
| F-010 | 21/18/06 | VISUAL-GAP | Medium | Stitch is not frozen as a measurable minimum visual quality floor | YES if Product wants this as mandatory requirement |
| F-011 | 09/10/11/12/06/21 | MODERNIZATION-OPPORTUNITY | Info | Maps/geospatial capability cannot be implemented canonically with current data/UI contract | YES if adopted |
| F-012 | 21 | AMBIGUOUS | Low | Dynamic color policy unspecified despite frozen palette | YES, limited visual clarification |
| F-013 | 01/07/08 | IMPLEMENTATION-CHOICE | Low | Type-safe destinations/routes not mandated | NO unless exact technology is frozen differently |
| F-014 | 12/20 | IMPLEMENTATION-CHOICE | Low | Android 16 long-running Worker quotas not discussed | NO; implementation/ops note unless workload changes |
| F-015 | 12/11 | CROSS-DOC-GAP (stale text) | Low | Doc12 still instructs adding `/sync/pull`, already present in Doc11 | YES, editorial cleanup only |
| F-016 | 18/21 | TEST-GAP | Info | Visual regression is optional; should be stronger if Stitch floor becomes frozen | Conditional on F-010 |
| F-017 | 06/21 | ACCESSIBILITY-GAP (conditional) | Info | Map/chart accessible alternatives are not defined | Conditional on maps/richer charts approval |

---

# 10. Cross-Document Consistency Matrix — Material Results

| Cross-check | Result |
|---|---|
| 01 ↔ 02 | Consistent |
| 02 ↔ 20 ↔ 21 | **Conflict:** stale `no Doc21` statement in Doc20 |
| 03 ↔ 05 ↔ 09 ↔ 10 ↔ 11 ↔ 12 | Consistent on lifecycle/authority/sync semantics |
| 04 ↔ 05 ↔ 06 ↔ 11 ↔ 17 | Consistent on roles/team/state/server enforcement |
| 06 ↔ 07 ↔ 08 ↔ 21 | Consistent on 43 UI components, 5 top-level destinations, stable-ID navigation; platform interaction modernizations missing consistently |
| 09 ↔ 10 ↔ 11 | Consistent on stable IDs/versioning/physical/API meaning |
| 10 ↔ 12 ↔ 19 | Consistent on durable outbox/cleanup/retention safeguards |
| 11 ↔ 12 | Semantically consistent; stale Doc12 editorial instruction about `/sync/pull` |
| 12 ↔ 17 ↔ 18 ↔ 20 | Strong consistency on durability/security/testing/operations |
| 13 ↔ 03 ↔ 09 ↔ 18 | Strong calculation consistency |
| 15 ↔ 09 ↔ 10 ↔ 11 ↔ 12 ↔ 17 ↔ 19 | Strong Evidence lifecycle/integrity consistency |
| 16 ↔ 09 ↔ 11 ↔ 18 | Strong report authority/integrity consistency |
| 18 ↔ all | Broad traceability strong; add modern Android platform UX/performance gates |
| 21 ↔ 06 ↔ 07 ↔ 08 ↔ 18 | Semantically consistent; modern visual/platform behavior gaps are shared omissions, not contradictions |

---

# 11. Deprecated / Outdated Practices Register

No frozen API was found that is clearly deprecated and unusable.

`Navigation Compose` is **not deprecated**, but it is no longer the current strongest recommendation for a new application architecture as of the audit date; Android's architecture recommendations point new multi-screen single-activity apps to Navigation 3. Treat this as modernization, not deprecation.

The 600/840dp adaptive bands are not inherently invalid. The issue is the frozen wording and lack of current window-adaptive/posture semantics, not the breakpoint values themselves.

---

# 12. Missing Modern Practices Register

1. Predictive Back behavioral contract and tests.
2. Edge-to-edge + safe insets + IME/cutout interaction contract.
3. Explicit lifecycle-aware UI state observation behavior.
4. Current Navigation 3 decision or documented rationale for retaining Navigation Compose.
5. Baseline Profiles/Macrobenchmark release-performance evidence.
6. Explicit R8/shrinker release configuration expectations.
7. Current window-adaptive/posture terminology/API guidance.
8. Dynamic-color policy for a frozen semantic brand palette.
9. Stitch visual-quality floor if Product wants it to be mandatory rather than aspirational.

---

# 13. Visual Gap Register

| Gap | Current state | Required decision |
|---|---|---|
| Stitch visual floor | Stitch referenced for generation, not measurable acceptance | Freeze parity criterion in Doc21/18 if mandatory |
| Edge-to-edge polish | Not specified | Add design/inset contract |
| Back gesture continuity | Not specified | Add predictive-back contract |
| Map/spatial Locations | Unsupported | Product/data/API/offline CR |
| Dynamic color | Unspecified | Explicitly disable, constrain, or define compatible behavior |
| Current adaptive primitives | Semantics allow them | Keep as implementation decision unless exact behavior is frozen |

---

# 14. Modern UX Opportunity Register

| ID | Area | Pattern | New data/API? | Offline/privacy impact | Baseline change? | Recommendation |
|---|---|---|---|---|---|---|
| UX-NAV-001 | App navigation | Navigation 3 + adaptive scene/navigation model | No business data | Low | Yes to technology baseline | Strongly recommended |
| UX-ADP-001 | Tablet/large window | Current window-adaptive list-detail/supporting panes | No | None | Mostly no; wording clarification | Recommended |
| UX-MAP-001 | Locations | List + map overview | **Yes: coordinates/geospatial contract** | Map cache/provider/privacy considerations | **Yes** | Valuable candidate |
| UX-MAP-002 | Storage hierarchy | Selected Location map context + hierarchy | Likely yes | Same | Yes | Candidate after UX-MAP-001 |
| UX-VIZ-001 | Dashboard | Purposeful KPI/trend visualizations derived from approved data | Maybe no | Minimal | Depends on exact semantics | Candidate |
| UX-HIS-001 | History | Timeline/visual comparison of already-approved history data | Usually no | Minimal | Likely presentation-only if no new semantics | Candidate |
| UX-EVD-001 | Evidence | More polished thumbnail/detail presentation | No new business semantics | Local encrypted media handling | Usually no | Recommended presentation modernization |

---

# 15. Map / Geo Feasibility Assessment

## Current data sufficiency

**Insufficient for a canonical map.** `Location` contains `address`, `city`, `country`, but no latitude, longitude, geometry, confidence/source, geocoding status or map-specific metadata. `StorageUnit` and `StorageSpace` likewise have no geographic coordinate contract.

## Recommended product shape if approved

Prefer a conservative **Location-level map**, not GPS tracking:

- optional authoritative `latitude` / `longitude` on Location;
- optional geocoding provenance/source and last-confirmed timestamp only if operationally needed;
- LOC-01 can become **list + map** without adding a new top-level destination;
- tapping a marker selects the existing Location record and preserves the existing Location → Storage Unit → Storage Space hierarchy;
- no turn-by-turn navigation, live tracking, background location permission or sensor workflow unless separately approved.

## Required document impact

| Document | Impact |
|---|---|
| 03 Business Rules | Define whether geo is optional, who edits it, validation/authority |
| 06 UI | Add map/list controls to LOC-01 and Add/Edit Location if applicable |
| 09 Data Model | Add coordinate/provenance fields |
| 10 DB Schema | Physical types/checks/indexes if needed |
| 11 API | DTO/mutation/read fields |
| 12 Offline | Working-set inclusion/caching semantics; map tile strategy must not be confused with business data |
| 17 Security/Privacy | Geodata minimization, logging/telemetry restrictions |
| 18 Tests | Coordinate validation, list/map sync, accessibility, offline fallback |
| 19 Retention | Review if provenance/geocoding metadata has separate lifecycle |
| 21 Design System | Marker/selection/map/list/accessibility patterns |

## Offline map implications

Do **not** automatically require full offline map tiles. Business records must remain fully browsable offline even if basemap imagery/tiles are unavailable. The list/hierarchy remains authoritative; map rendering must degrade safely without blocking field workflows.

## Privacy

Location coordinates describe business facilities, not necessarily user location. Do not request Android location permission simply to display stored facility coordinates. If later adding "my location", background tracking or route guidance, that is a separate privacy/product change.

## Recommendation

**STRONGLY WORTH EXPLORING as an optional Baseline Change**, because it materially improves modern Location UX without changing inventory semantics. Keep it Location-centric and avoid GPS tracking scope creep.

---

# 16. Implementation Choice Register

The following should remain implementation choices unless the baseline specifically needs behavioral language:

- exact use of `collectAsStateWithLifecycle` after freezing the requirement that observation be lifecycle-aware;
- type-safe route classes/objects if Navigation Compose remains;
- Navigation 3 scene-strategy classes and exact library artifacts if Navigation 3 is adopted;
- exact `WindowAdaptiveInfo`/window-size helper usage;
- exact Flow operators and `stateIn` policy;
- `@Binds` vs `@Provides` where behavior/ownership is unchanged;
- benchmark scenario implementation details;
- exact map provider if map capability is approved, provided provider choice preserves privacy/offline/security/product contracts.

---

# 17. Baseline Change Candidates

## CR-A01 — Modern Android Navigation Contract

**Classification:** STRONGLY RECOMMENDED BASELINE CHANGE  
**Affected:** 01, 02, 06, 07, 08, 18, 21  
**Proposal:** replace the frozen implementation technology wording `Navigation Compose` with a current Jetpack navigation contract, selecting Navigation 3 as the target unless an explicit compatibility reason is documented for retaining Navigation Compose. Preserve all five canonical top-level destinations, stable-ID arguments, auth/authorization guards and 43-component product inventory.  
**Risk of no change:** starts the controlled rebuild on a navigation stack that is supported but no longer the strongest current recommendation.  
**Risk of change:** migration complexity and dependency/version maturity.  
**Product behavior impact:** none intended.

## CR-A02 — Predictive Back and Back-State Continuity

**Classification:** STRONGLY RECOMMENDED BASELINE CHANGE  
**Affected:** 06, 07, 18, 21  
**Proposal:** require supported predictive-back APIs/behavior, preserving current context restoration and avoiding custom root interception that defeats system behavior. Add regression tests.

## CR-A03 — Edge-to-Edge / Insets / IME Contract

**Classification:** STRONGLY RECOMMENDED BASELINE CHANGE  
**Affected:** 06, 07, 18, 21  
**Proposal:** require edge-to-edge-safe layouts, WindowInsets/cutout/gesture/IME handling, no critical action behind system bars, and regression on supported API/form-factor matrix.

## CR-A04 — Lifecycle-Aware UI State Observation

**Classification:** STRONGLY RECOMMENDED CLARIFICATION  
**Affected:** 07, 08, 18  
**Proposal:** state that Compose observes ViewModel state only while lifecycle-appropriate and does not keep unnecessary upstream collection active when UI is not active. Keep exact API as implementation detail.

## CR-A05 — State-Driven UI Event Semantics

**Classification:** STRONGLY RECOMMENDED CLARIFICATION  
**Affected:** 07, optionally 06/18  
**Proposal:** replace ambiguous "one-off navigation/UI effects" wording with state-driven handling consistent with current Android architecture recommendations. Navigation actions that are pure UI logic remain in UI/navigation layer; business outcomes are represented in durable UiState/domain state.

## CR-A06 — Android Performance Release Engineering

**Classification:** STRONGLY RECOMMENDED BASELINE MODERNIZATION  
**Affected:** 07, 18, 20  
**Proposal:** add representative startup/jank Macrobenchmark evidence, Baseline Profile generation/verification where supported, and release R8/shrinker correctness tests while preserving reflection/serialization requirements.

## CR-A07 — Adaptive Window Terminology Modernization

**Classification:** OPTIONAL/LIMITED BASELINE MODERNIZATION  
**Affected:** 06, 07, 21  
**Proposal:** define Compact/Medium/Expanded as **window** contexts, not device categories; account for resize/multi-window/posture; keep current canonical destinations and no desktop-only product divergence.

## CR-UX01 — Stitch Minimum Visual Quality Floor

**Classification:** STRONGLY RECOMMENDED **IF Product intends Stitch parity to be mandatory**  
**Affected:** 21, 18, possibly 06/02  
**Proposal:** freeze that final production screens must be no less mature than approved Stitch reference screens in hierarchy, density, component polish and state coverage, while business semantics always come from Docs01–20. Introduce representative reference-screen and visual-regression acceptance evidence.

## CR-UX02 — Location Map / Geospatial Model

**Classification:** OPTIONAL BASELINE MODERNIZATION  
**Affected:** 03, 06, 09, 10, 11, 12, 17, 18, 19 review, 21  
**Proposal:** add optional facility coordinates and a list+map Location experience without new top-level destination, GPS tracking or turn-by-turn scope.

## CR-GOV01 — Remove Doc20 `no Doc21` Contradiction

**Classification:** REQUIRED BASELINE CORRECTION  
**Affected:** 20 only, plus consistency verification with 02/21  
**Proposal:** replace stale finalization sentence with the current 21-document governance model.

## CR-GOV02 — Remove Stale `/sync/pull` Finalization Instruction

**Classification:** LOW-RISK BASELINE EDITORIAL CORRECTION  
**Affected:** 12, verify 11  
**Proposal:** state that Doc11 already contains GET `/api/v1/sync/pull`; remove future-action wording.

## CR-UI01 — Dynamic Color Policy

**Classification:** LIMITED VISUAL BASELINE CLARIFICATION  
**Affected:** 21  
**Proposal:** explicitly state whether Android dynamic color is disabled, constrained, or mapped. Given frozen semantic colors and industrial brand consistency, recommended default is that system dynamic color does not override canonical SiloMath semantic tokens.

---

# 18. Document Change Impact Matrix

| CR | Direct change | Dependent review |
|---|---|---|
| CR-A01 Navigation | 01,02,06,07,08,18,21 | 05,17,20 |
| CR-A02 Predictive Back | 06,07,18,21 | 08 |
| CR-A03 Edge-to-edge | 06,07,18,21 | 08,20 |
| CR-A04 Lifecycle collection | 07,08,18 | 06 |
| CR-A05 UI event semantics | 07 | 06,18 |
| CR-A06 Performance engineering | 07,18,20 | 08 |
| CR-A07 Adaptive wording | 06,07,21 | 18 |
| CR-UX01 Stitch floor | 21,18 | 02,06 |
| CR-UX02 Maps | 03,06,09,10,11,12,17,18,21 | 01,02,04,05,19,20 |
| CR-GOV01 Doc21 contradiction | 20 | 02,21 |
| CR-GOV02 sync/pull stale text | 12 | 11 |
| CR-UI01 Dynamic color | 21 | 18 |

---

# 19. Prioritized Remediation Plan

### P0 — before broad UI/module migration

1. Correct Doc20 `no Doc21` contradiction.
2. Decide Navigation 3 vs retaining Navigation Compose; do not migrate screens into a navigation foundation whose target is undecided.
3. Freeze predictive-back and edge-to-edge/inset behavior.
4. Clarify lifecycle-aware state collection and state-driven transient UI handling.
5. If Stitch parity is a mandatory Product requirement, freeze it now in Doc21/18 before rebuilding screens.

### P1 — before feature UI implementation

6. Modernize adaptive wording/primitives.
7. Add Baseline Profile/Macrobenchmark/R8 performance release requirements.
8. Clarify dynamic-color policy.
9. Decide whether Location Maps are V6 modernization scope. If yes, approve CR-UX02 before implementing LOC/master-data UI.

### P2 — before release

10. Add predictive-back, edge-to-edge, adaptive, accessibility and performance regression evidence.
11. Add representative Stitch parity/visual regression evidence if CR-UX01 is approved.
12. Validate map offline fallback/accessibility/privacy if maps are approved.

### P3 — optional modernization

13. Richer KPI/trend visualizations where they only present already-approved data.
14. History timeline and richer evidence presentation without new business semantics.

---

# 20. Final Verdict

**B — V6 IS FUNDAMENTALLY SOUND BUT REQUIRES LIMITED BASELINE MODERNIZATION.**

The baseline is not architecturally obsolete. The core business/domain/data/offline/security contracts are unusually strong and should be preserved. Replacing them with a generic Android sample architecture would be a regression.

The necessary corrections are concentrated at the edge between the frozen product and the 2026 Android platform/UI stack: current navigation, predictive back, edge-to-edge/insets, lifecycle-aware Compose observation, performance release engineering and measurable visual quality.

The Location map request is technically valuable but is **not present in V6**. It should be handled as a controlled product/data/UI Baseline Change rather than silently added during design implementation.

---

# 21. Post-Audit Recommendation

### Documents that should be modified before continuing broad UI migration

- **20** — mandatory governance contradiction correction.
- **06, 07, 18, 21** — predictive back + edge-to-edge/insets.
- **01, 07, 08** (and related 02/06/18/21 references) — navigation technology decision.
- **07/08/18** — lifecycle-aware state observation/state-driven event clarification.
- **07/18/20** — Android performance release engineering.
- **21** — dynamic-color policy; Stitch floor if explicitly approved.

### Documents requiring change only if Maps are approved

- 03, 06, 09, 10, 11, 12, 17, 18, 21; review 19 and 20.

### Documents that should remain materially frozen

- 03–05 business/authorization/workflow semantics, except geo additions if approved;
- 09–16 existing non-geo domain/data/calculation/evidence/reporting semantics;
- 17 security baseline except geodata privacy additions if approved;
- 19 retention baseline unless new geo metadata creates a separate retention concern.

### Implementation may continue?

**Foundation/domain/backend work that does not depend on the findings may continue.**  
**Broad UI/navigation migration should pause until P0 baseline decisions are approved**, because otherwise we risk rebuilding `core:navigation`, feature navigation, screen scaffolds and visual acceptance twice.

---

# Anti-Omission Check

- [x] 01–21 source files present and audited
- [x] Android architecture
- [x] Kotlin
- [x] Compose
- [x] Navigation
- [x] Modularization
- [x] DI
- [x] Room
- [x] Offline-first
- [x] Sync
- [x] WorkManager/background
- [x] Network/API
- [x] Auth/Security/Privacy
- [x] Adaptive UI
- [x] Material/design system
- [x] Accessibility
- [x] Performance
- [x] Testing
- [x] Gradle/build/release
- [x] CI/CD/operations
- [x] Stitch macro-level benchmark
- [ ] Full screen-by-screen Stitch parity — **cannot be truthfully completed from the supplied zoomed-out board image; readable screen exports are required**
- [x] Maps/geospatial feasibility
- [x] Cross-document consistency
- [x] Baseline changes separated from implementation choices

**Audit completeness qualification:** documentation audit is complete 21/21. Stitch screen-level parity is explicitly incomplete due to insufficient visual resolution, as required by the anti-hallucination rules.
