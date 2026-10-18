# SiloMath V6 — Baseline Modernization Decision Pack

**Date:** 2026-10-17  
**Status:** DECISION PACK — no baseline change applied yet  
**Authority:** SiloMath V6 APPROVED/FROZEN remains authoritative until a Baseline Change is explicitly approved and re-frozen.  
**Source audit:** `SiloMath_V6_Android_UX_Best_Practice_Audit_2026-10-17.md`

---

## 1. Purpose

This pack converts the completed 21/21 documentation audit into explicit decisions. It does **not** modify the frozen baseline. It separates:

1. changes that should be approved as Baseline Change;
2. clarifications that should be frozen into the documentation;
3. implementation decisions that should remain outside the product baseline;
4. optional product modernization, especially Maps/Geo.

The objective is to avoid rebuilding `:core:navigation`, `:core:designsystem` and feature UI against a platform contract that may immediately change.

---

# 2. Executive Recommendation

## Recommended decision set

| ID | Decision | Recommendation | Priority | Continue implementation before decision? |
|---|---|---|---|---|
| CR-GOV01 | Remove stale Doc20 `no Doc21` contradiction | **APPROVE** | P0 | No reason to delay editorial correction |
| CR-GOV02 | Remove stale Doc12 future `/sync/pull` wording | **APPROVE** | P0 | Safe editorial correction |
| CR-A01 | Modern Android Navigation contract | **APPROVE IN PRINCIPLE — target Navigation 3, validate dependency maturity first** | P0 | **Do not build canonical navigation first** |
| CR-A02 | Predictive Back contract | **APPROVE** | P0 | No |
| CR-A03 | Edge-to-edge / Insets / IME contract | **APPROVE** | P0 | No |
| CR-A04 | Lifecycle-aware UI state observation | **APPROVE** | P0 | No broad UI migration first |
| CR-A05 | State-driven UI event semantics | **APPROVE** | P0 | No broad UI migration first |
| CR-A07 | Adaptive window terminology modernization | **APPROVE** | P1 | Prefer before DS/navigation implementation |
| CR-UI01 | Dynamic color policy | **APPROVE — canonical SiloMath semantic tokens must not be overridden** | P1 | Prefer before design-system implementation |
| CR-UX01 | Stitch minimum visual quality floor | **APPROVE** | P0/P1 | **Yes, before rebuilding screens** |
| CR-A06 | Performance release engineering | **APPROVE** | P2 | Does not block early domain work |
| CR-UX02 | Location Map / Geo model | **DEFER AS SEPARATE PRODUCT CR, but proceed to detailed design proposal** | Separate | Do not silently implement |

### Overall recommendation

Proceed with a **limited V6 Baseline Modernization** before large-scale UI/navigation migration. Do **not** reopen domain rules, offline semantics, the 19-module topology, calculation rules, campaign lifecycle, authorization model or existing data invariants except where a specifically approved CR requires it.

---

# 3. Decision Package A — Android Platform Modernization

## CR-A01 — Modern Android Navigation Contract

### Recommendation
**APPROVE IN PRINCIPLE.** Use a current Jetpack navigation contract and target **Navigation 3** unless a focused compatibility check finds a blocking reason to retain Navigation Compose.

### Current baseline wording
Examples currently frozen:

- Doc01: `Navigation Compose;`
- Doc07: `Navigation | Navigation Compose; single-activity application.`
- Doc07 ADR-010: `Single-activity Navigation Compose with five canonical operational top-level destinations.`
- Doc08: `root Navigation Compose graph assembly`.

### Problem
The product semantics are valid, but the baseline unnecessarily freezes a particular navigation implementation technology. This makes platform modernization a Baseline Change even when product behavior does not change.

### Proposed baseline rule
Replace technology-specific wording with a semantic contract such as:

> SiloMath uses the current supported Jetpack navigation stack for a single-activity Compose application. The implementation shall use type-safe destination contracts where supported. The navigation implementation must preserve the five canonical operational top-level destinations, Auth outside the operational shell, stable-ID arguments, authorization guards, state restoration, deep-link safety, adaptive navigation behavior and testability. Navigation technology changes that preserve this contract are implementation decisions unless they alter frozen product behavior.

For the first implementation under the revised baseline, record an IADR selecting Navigation 3 after dependency/version validation.

### Product impact
None intended.

### Architecture impact
`01, 02, 06, 07, 08, 18, 21` direct review/change. `05, 17, 20` consistency review.

### Risk of change
Library maturity/version migration risk.

### Risk of no change
Canonical rebuild begins on a supported but less future-facing navigation foundation and technology wording remains over-specified.

### Decision
**APPROVE**, subject to a short technical compatibility check before implementation.

---

## CR-A02 — Predictive Back and Back-State Continuity

### Recommendation
**APPROVE.**

### Current baseline
No explicit predictive-back contract in Doc06/07/18/21.

### Proposed rule

> All supported Android back behavior shall use supported system/Jetpack back APIs and integrate with predictive back where available. Back navigation must preserve the canonical navigation graph, state restoration and authorization constraints. Custom interception must not suppress platform back behavior unless required by an explicit frozen workflow guard.

### Required acceptance
- predictive-back behavior for supported API levels;
- root/top-level destination behavior;
- nested destination behavior;
- dialogs/sheets;
- unsaved form confirmation only where an approved workflow requires it;
- process recreation/back stack restoration.

### Decision
**APPROVE.**

---

## CR-A03 — Edge-to-Edge / WindowInsets / IME Contract

### Recommendation
**APPROVE.**

### Current baseline
Doc21 specifies visual components and adaptive layouts but does not freeze edge-to-edge/inset/IME safety.

### Proposed rule

> Production Compose screens are edge-to-edge safe. Content and interactive controls must correctly handle system bars, display cutouts, gesture insets and IME insets. No required control, validation message or primary action may become obscured by system UI or the soft keyboard.

### Required acceptance
Phone/tablet portrait/landscape + keyboard-open cases + gesture navigation.

### Decision
**APPROVE.**

---

## CR-A04 — Lifecycle-Aware UI State Observation

### Recommendation
**APPROVE as baseline clarification; keep concrete API as implementation detail.**

### Proposed rule

> Compose UI observes ViewModel state only while lifecycle-appropriate and must not keep unnecessary upstream collection active when the destination is not active. The exact AndroidX API is an implementation decision.

### Explicit non-baseline implementation recommendation
Use `collectAsStateWithLifecycle` where appropriate in the current AndroidX implementation.

### Decision
**APPROVE.**

---

## CR-A05 — State-Driven UI Event Semantics

### Recommendation
**APPROVE.**

### Current wording
Doc07 currently says:

> One-off navigation/UI effects are separated from durable business state.

### Problem
This can be interpreted as endorsing transient event channels for important outcomes.

### Proposed wording

> Durable business outcomes and user-visible operation results are represented in durable domain/UI state. Pure UI navigation commands and ephemeral presentation actions may remain in the UI/navigation layer, but must not become an alternative authority for business state. Critical success/failure state must survive lifecycle recreation as required by the workflow.

### Decision
**APPROVE.**

---

## CR-A06 — Android Performance Release Engineering

### Recommendation
**APPROVE, P2.**

### Proposed baseline additions
Docs07/18/20 should require measurable release evidence for:

- representative startup/jank Macrobenchmark scenarios;
- Baseline Profile generation/verification where supported and beneficial;
- release R8/shrinker enabled and verified;
- serialization/reflection rules preserved;
- no main-thread blocking I/O in production paths;
- critical operational flows remain within defined performance budgets.

Exact benchmark classes, thresholds and profile-generation implementation remain repository implementation decisions unless later frozen by release policy.

### Decision
**APPROVE.**

---

## CR-A07 — Adaptive Window Terminology Modernization

### Recommendation
**APPROVE.**

### Current baseline
Doc07/21 describe:

- Compact
- Medium
- Expanded

and Doc21 labels Compact as `Compact phone`.

### Proposed rule
Use **window context**, not device identity:

> Compact, Medium and Expanded describe the current available application window, not a permanent device class. Layout adaptation must remain correct under resize, split-screen/multi-window and posture changes. The same frozen product/navigation semantics apply in all window contexts.

Current Material 3 Adaptive primitives can be implementation choices.

### Decision
**APPROVE.**

---

# 4. Decision Package B — UI/UX and Visual Quality

## CR-UX01 — Stitch Minimum Visual Quality Floor

### Recommendation
**APPROVE.** Product intent has now been explicitly stated: Stitch is the minimum visual benchmark, not the ceiling.

### Proposed frozen rule

> Approved SiloMath Stitch references are the minimum visual-quality benchmark for production UI. Final Android screens must be no less mature in information hierarchy, usable density, component refinement, state coverage and enterprise polish than the corresponding approved reference. Stitch does not define business semantics; Documents 01–20 remain authoritative for behavior and data. Visual parity must not introduce unapproved fields, actions, statuses, workflows or permissions.

### Add acceptance evidence
Doc18 should require:

- representative reference-screen comparison before broad screen production;
- screenshot/visual-regression evidence for frozen design-system components and representative critical screens;
- explicit loading/empty/error/offline/sync states where applicable;
- phone/tablet window-context verification;
- accessibility verification.

### Important limitation
The existing zoomed-out Stitch board proves overall maturity/density but is insufficient for screen-by-screen parity. Readable individual screen exports are still needed before exact parity acceptance can be frozen screen-by-screen.

### Decision
**APPROVE.**

---

## CR-UI01 — Dynamic Color Policy

### Recommendation
**APPROVE: system dynamic color must NOT override canonical SiloMath semantic tokens.**

### Rationale
SiloMath uses semantic colors for operational/status meaning and an industrial enterprise identity. Uncontrolled system-derived color palettes could alter visual semantics and cross-device consistency.

### Proposed rule

> Android dynamic color must not replace or reinterpret canonical SiloMath semantic color tokens. Platform-derived color may be used only in explicitly approved non-semantic decorative surfaces and only when contrast/accessibility and brand consistency remain intact. Default production behavior uses the canonical Doc21 token system.

### Decision
**APPROVE.**

---

# 5. Decision Package C — Maps / Geospatial Locations

## CR-UX02 — Location Map / Geospatial Model

### Recommendation
**DEFER approval as a separate product capability CR, but develop the proposal now.**

Maps are desirable and compatible with the target product quality, but they are not currently supported canonically by the frozen data/UI/API contract.

### Current baseline
`Location` has address/city/country-style information but no canonical latitude/longitude/geospatial fields. LOC-01 is a list/search/filter contract and does not contain map controls.

### Recommended scope if approved
Keep the capability conservative:

- optional coordinates on `Location`;
- Location overview can use **List / Map** presentation;
- selected marker opens/selects the existing Location record;
- optional clustering where data volume warrants;
- map/list synchronization;
- Storage Units and Storage Spaces remain children of Location and do not require independent GPS coordinates unless later justified;
- no background GPS tracking;
- no continuous user-location tracking;
- no turn-by-turn navigation;
- no new top-level destination;
- no dependence on basemap availability for core field work.

### Offline rule
The authoritative Location list/hierarchy and business data remain usable offline even when map tiles are unavailable. Map rendering must degrade safely and must never block stocktaking or master-data access.

### Likely new Location fields
Only after product approval, consider:

- `latitude: Decimal?`
- `longitude: Decimal?`
- optional coordinate source/provenance if operationally required;
- optional `geoUpdatedAt` only if operationally required.

Do **not** add geocoding metadata merely because a mapping SDK can provide it.

### Documents requiring direct modification if approved
`03, 06, 09, 10, 11, 12, 17, 18, 21`

### Documents requiring dependent review
`01, 02, 04, 05, 19, 20`

### Decision
**DEFER as independent CR.** Do not mix with Android modernization CRs. Prepare a product/data/security/offline design first.

---

# 6. Governance Corrections

## CR-GOV01 — Remove Doc20 `no Doc21` contradiction

### Recommendation
**APPROVE immediately.**

### Current Doc20 wording

> After Doc20 is frozen, the next activity is the final consistency/finalization pass across Documents 01-20; no Doc21 is introduced.

### Conflict
Doc02 defines the authoritative 21-document baseline and Doc21 exists and is frozen.

### Proposed wording

> The authoritative SiloMath V6 baseline consists of Documents 01–21. After Doc21 is frozen, finalization consists of cross-document consistency verification, traceability closure and controlled Baseline Change governance; no additional product-baseline document is introduced without an approved Baseline Change.

### Decision
**APPROVE.**

---

## CR-GOV02 — Remove stale `/sync/pull` finalization instruction

### Recommendation
**APPROVE immediately.**

### Current Doc12 issue
Doc12 still states that `GET /api/v1/sync/pull` must be added to Doc11 during a future finalization pass, while the current baseline already contains that endpoint contract.

### Proposed wording

> `GET /api/v1/sync/pull` is part of the frozen V6 API contract defined in Doc11. Doc12 relies on that endpoint and must not describe its addition as future work.

### Decision
**APPROVE.**

---

# 7. What must remain unchanged

The audit does **not** justify reopening these frozen foundations:

- exact 19-module production topology;
- `:app` as composition root;
- Feature → Core allowed / Core → Feature prohibited;
- repository/domain/data boundaries;
- Room as client read source where defined;
- offline-first/local-first field-operation model;
- atomic `business mutation + SyncOperation` rule;
- canonical Campaign lifecycle;
- canonical user/master-data lifecycles;
- authorization model;
- calculation-engine purity;
- `/api/v1` backend boundary;
- evidence integrity model;
- report inventory;
- historical immutability and retention semantics.

Any later attempt to alter these remains a separate Baseline Change.

---

# 8. Implementation-Decision Register — no baseline change required

After the above baseline rules are approved, the following should normally remain implementation decisions:

| Topic | Recommended implementation direction |
|---|---|
| Compose Flow collection | `collectAsStateWithLifecycle` where appropriate |
| UI state | immutable/sealed UiState where useful |
| Route representation | type-safe route objects/classes supported by chosen navigation stack |
| Flow operators | select per use case; do not freeze specific operators |
| Hilt bindings | constructor injection + Binds/Provides based on implementation needs |
| Adaptive primitives | current Material 3 Adaptive APIs while preserving frozen behavior |
| Benchmark implementation | Macrobenchmark scenarios and Baseline Profile mechanics defined in repository engineering artifacts |
| Map provider | choose only after CR-UX02 approval and privacy/offline/provider review |
| Design-system implementation | Compose implementation may evolve while preserving frozen Doc21 tokens/contracts |

---

# 9. Document Impact Summary

| Decision | Direct document changes | Dependent review |
|---|---|---|
| CR-A01 Navigation | 01,02,06,07,08,18,21 | 05,17,20 |
| CR-A02 Predictive Back | 06,07,18,21 | 08 |
| CR-A03 Edge-to-edge | 06,07,18,21 | 08,20 |
| CR-A04 Lifecycle observation | 07,08,18 | 06 |
| CR-A05 UI event semantics | 07 | 06,18 |
| CR-A06 Performance engineering | 07,18,20 | 08 |
| CR-A07 Adaptive wording | 06,07,21 | 18 |
| CR-UX01 Stitch floor | 21,18 | 02,06 |
| CR-UI01 Dynamic color | 21 | 18 |
| CR-UX02 Maps | 03,06,09,10,11,12,17,18,21 | 01,02,04,05,19,20 |
| CR-GOV01 Doc21 contradiction | 20 | 02,21 |
| CR-GOV02 sync/pull wording | 12 | 11 |

---

# 10. Approval Order

## P0 — approve before canonical UI/navigation rebuild

1. CR-GOV01 — Doc20/Doc21 correction
2. CR-GOV02 — `/sync/pull` editorial correction
3. CR-A01 — navigation contract / Navigation 3 direction
4. CR-A02 — predictive back
5. CR-A03 — edge-to-edge/insets/IME
6. CR-A04 — lifecycle-aware observation
7. CR-A05 — state-driven UI event semantics
8. CR-UX01 — Stitch visual floor

## P1 — approve before Design System / adaptive UI freeze

9. CR-A07 — window-adaptive terminology
10. CR-UI01 — dynamic color policy

## P2 — approve before production candidate/release gates

11. CR-A06 — performance release engineering

## Separate product track

12. CR-UX02 — Maps/Geo

---

# 11. Recommended Execution Sequence After Approval

```text
Approve decision pack
        ↓
Create controlled Baseline Change record(s)
        ↓
Edit only affected Docs 01–21
        ↓
Cross-document consistency validation
        ↓
Update Decision Register / requirements / RVM mappings
        ↓
Re-freeze baseline
        ↓
Realign backlog to re-frozen baseline
        ↓
Implement canonical :core:designsystem
        ↓
Implement canonical :core:navigation
        ↓
Operational shell
        ↓
Migrate :feature:auth
        ↓
Migrate :feature:dashboard
        ↓
Continue feature migration by backlog/dependency order
```

Maps/Geo proceeds only after separate CR-UX02 approval and must not block the non-map baseline modernization unless Product chooses it as a release requirement.

---

# 12. Decision Requested from Product Owner

Recommended approval set:

```text
APPROVE:
CR-GOV01
CR-GOV02
CR-A01
CR-A02
CR-A03
CR-A04
CR-A05
CR-A06
CR-A07
CR-UX01
CR-UI01

DEFER / SEPARATE PRODUCT CR:
CR-UX02 Maps / Geo
```

If this set is approved, the next activity is **not implementation code**. The next activity is to prepare and apply the controlled Baseline Change to the affected official documents, then run a full cross-document consistency pass and re-freeze V6 before resuming canonical UI/navigation migration.
