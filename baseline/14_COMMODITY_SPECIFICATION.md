# SiloMath V6 — 14_COMMODITY_SPECIFICATION

**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-27 - CR-CALC-VERSION-CONTRACT-001 approved calculation-version clarification

## 1. Purpose and authority

This document is the normative SiloMath V6 Commodity contract. It consolidates the frozen Product Baseline, Business Rules, UI, Data Model, Database/API/Offline contracts, Calculation Engine and the explicitly approved CO-D01–CO-D32 decisions.

Commodity governance is platform-level. Organization roles consume governed definitions but do not administer them.

## 2. Scope

- Canonical 24-item Commodity Catalog and stable identity rules.
- Commodity versus immutable CommodityVersion semantics.
- Lifecycle, current-version publication and historical behavior.
- Reference Test Weight, Moisture and Impurities reference semantics.
- Actual Bulk Density boundary with Stocktaking and Calculation Engine.
- Offline package/catalog behavior and synchronization integrity.
- Snapshot/report/history preservation.
- UI/API/data-layer implementation constraints, validation and acceptance tests.

Out of scope: reconciliation, accounting inventory, tenant-defined commodities, arbitrary commodity formulas, automatic moisture/impurity/shrink/packing corrections, quality grading engines, pricing and forecasting.

## 3. Normative V6 Commodity Catalog

SiloMath V6 contains exactly the following 24 platform-governed Commodity identities. There is no generic `Other`, `Unknown` or `Empty` Commodity.

| # | Category | Canonical platform code | Canonical display name |
|---:|---|---|---|
| 1 | Cereal | `COMMON_WHEAT` | Common Wheat |
| 2 | Cereal | `DURUM_WHEAT` | Durum Wheat |
| 3 | Cereal | `SPELT` | Spelt |
| 4 | Cereal | `CORN_MAIZE` | Corn/Maize |
| 5 | Cereal | `BARLEY` | Barley |
| 6 | Cereal | `MALTING_BARLEY` | Malting Barley |
| 7 | Cereal | `OATS` | Oats |
| 8 | Cereal | `RYE` | Rye |
| 9 | Cereal | `TRITICALE` | Triticale |
| 10 | Cereal | `SORGHUM` | Sorghum |
| 11 | Cereal | `MILLET` | Millet |
| 12 | Cereal | `RICE` | Rice |
| 13 | Pseudocereal | `BUCKWHEAT` | Buckwheat |
| 14 | Oilseed | `SUNFLOWER_SEED` | Sunflower Seed |
| 15 | Oilseed | `RAPESEED_CANOLA` | Rapeseed/Canola |
| 16 | Oilseed | `SOYBEAN` | Soybean |
| 17 | Oilseed | `MUSTARD_SEED` | Mustard Seed |
| 18 | Oilseed | `LINSEED_FLAXSEED` | Linseed/Flaxseed |
| 19 | Pulse | `FIELD_PEAS` | Field Peas |
| 20 | Pulse | `FIELD_FABA_BEANS` | Field/Faba Beans |
| 21 | Pulse | `LUPINS` | Lupins |
| 22 | Pulse | `DRY_BEANS` | Dry Beans |
| 23 | Pulse | `CHICKPEAS` | Chickpeas |
| 24 | Pulse | `LENTILS` | Lentils |

The codes above are the canonical V6 machine/business identifiers for the governed seed catalog. They are normalized uppercase identifiers, globally unique and never repurposed for another semantic Commodity. Human-facing names are versioned display data.

## 4. Domain model

### 4.1 Commodity

| Field | Required | Mutability | Contract |
|---|---|---|---|
| commodityId | Yes | Stable | Platform UUID; logical identity across all versions. |
| code | Yes | Stable | Globally unique canonical platform code; trim + uppercase; never reused. |
| currentVersionId | Yes while supported | Platform controlled | Points to exactly one immutable current CommodityVersion used for new operations. |
| lifecycleStatus | Yes | Platform controlled | Exactly ACTIVE or INACTIVE. |
| createdAt / governance provenance | Yes | Platform controlled | Traceable creation/governance metadata. |

### 4.2 CommodityVersion

| Field | Required | Mutability | Contract |
|---|---|---|---|
| commodityVersionId | Yes | Immutable | Stable UUID of the immutable definition version. |
| commodityId | Yes | Immutable | Parent Commodity. |
| versionNumber | Yes | Immutable | Positive monotonic integer, unique within Commodity. |
| displayName | Yes | Immutable | Human-facing name frozen for this version. |
| category | Yes | Immutable | Governed category matching the V6 catalog classification. |
| referenceTestWeight | Optional | Immutable | Reference-only numeric value with explicit governed unit. |
| moistureReference | Optional | Immutable | Platform reference metadata; no automatic quantity correction. |
| impuritiesReference | Optional | Immutable | Platform reference metadata; no automatic quantity correction. |
| definitionMetadata | As governed | Immutable | Only keys/types admitted by the platform-controlled schema registry. |
| createdAt / createdBy | Yes | Immutable | Version creation provenance. |

CommodityVersion has no independent Active/Inactive state. Availability for a new selection is determined by the parent Commodity lifecycle and `currentVersionId`.

## 5. Lifecycle and version publication

Commodity lifecycle is exactly `ACTIVE` or `INACTIVE`.

- ACTIVE: current version may be offered for new authorized stocktaking selection.
- INACTIVE: cannot be selected for a new operation, but all historical/pinned references remain resolvable and readable.
- Reactivation reuses the same Commodity identity; it does not fabricate a new Commodity solely because availability changed.
- Publishing a new CommodityVersion and moving `currentVersionId` is one atomic platform-governance operation.
- Prior versions remain immutable and retained according to the historical/retention contracts.
- A governed display-name/reference-data change that must remain historically reproducible creates a new CommodityVersion.

## 6. Governance and authorization

Commodity and CommodityVersion are global platform-governed master data, not Organization-scoped customer master data.

| Capability | Platform governance authority | Organization Admin | Standard |
|---|---|---|---|
| Browse authorized catalog | Yes | Yes | Yes |
| View Commodity details/version/reference data | Yes | Yes | Yes |
| Create Commodity | Yes | No | No |
| Edit/rename governed definition | Yes via new version | No | No |
| Activate/deactivate Commodity | Yes | No | No |
| Publish current CommodityVersion | Yes | No | No |
| Delete historical referenced Commodity/version | No destructive historical delete | No | No |
| Create tenant-specific/custom/free-text Commodity | Not a V6 capability | No | No |

Organization Admin status never implies platform Commodity-governance authority.

## 7. Reference parameters versus operational Actual Bulk Density

The V6 Commodity reference set consists of three governed reference concepts:

1. **Reference Test Weight**
2. **Moisture**
3. **Impurities**

They are reference/master data on CommodityVersion when defined. They do not become hidden numerical correction rules.

**Actual Bulk Density is different.** It is a StocktakingRevision operational input captured for the actual physical inventory observation. It is not the fourth Commodity reference parameter and is not automatically copied from Reference Test Weight.

## 8. Reference Test Weight

Reference Test Weight is optional versioned reference information. When present, its value and governed unit are explicit and frozen with the CommodityVersion.

Rules:

- It may be displayed as reference/plausibility context.
- It never supplies a missing Actual Bulk Density automatically.
- It never silently overwrites an operator-entered Actual Bulk Density.
- It is not itself authoritative Physical Quantity input.
- Changing a historically relevant reference value is performed by publishing a new CommodityVersion.

## 9. Moisture and Impurities

Moisture and Impurities are optional governed reference metadata when defined for a CommodityVersion. Where represented numerically, their schema, unit and semantic label are platform-controlled metadata rather than customer-defined arbitrary fields.

V6 explicitly does **not** apply automatic:

- moisture correction
- impurity correction
- shrink factor
- packing factor
- commodity-derived mass correction
- quality-grade adjustment

Their presence cannot change `PhysicalQuantityKg` unless a future explicitly approved calculation contract introduces such behavior.

## 10. definitionMetadata controlled schema

`definitionMetadata` is not a customer-editable JSON extension point. The platform may carry additional reference attributes only through a controlled key/type registry.

A governed metadata key definition includes at minimum: stable key, semantic label, value type, unit/enum domain where relevant, nullability, version applicability and whether it is historical-reference material. Unknown customer-provided keys are rejected.

V6 reference metadata cannot contain executable expressions, scripts, formula text or tenant-specific calculation logic.

## 11. Stocktaking selection contract

For a non-empty StocktakingRevision:

- Commodity selection is mandatory.
- The selected value is the exact `commodityVersionId`; `commodityId` alone is insufficient for historical integrity.
- New selection offers only authorized ACTIVE Commodity identities and their current version.
- Changing Commodity in a working revision revalidates dependent quantity context and invalidates a stale CalculationResult where calculation-relevant inputs/context changed.
- Validated revisions are immutable; a change requires the frozen Reset Validation/new-revision workflow.
- Completed Campaign stocktaking remains immutable.

For Mark Empty, Commodity is not required and the application must not fabricate an Empty/Unknown/Other Commodity.

## 12. Actual Bulk Density contract

Actual Bulk Density is the operational quantity input for non-empty stocktaking.

```text
density > 0
supported entered units: kg/m³, kg/hl
canonical unit: kg/m³
1 kg/hl = 10 kg/m³
PhysicalQuantityKg = ProductVolumeM3 × ActualBulkDensityKgM3
```

For reproducibility, the working/validated calculation context retains the entered density value, entered unit and normalized density used by the Calculation Engine, together with the calculation/version/fingerprint metadata frozen by Doc13.

Reference Test Weight can support plausibility UX but cannot satisfy the mandatory Actual Bulk Density requirement.

## 13. Calculation Engine boundary

The Geometry Engine is Commodity-independent and produces Product Volume. Commodity selection does not change geometry volume. The Quantity Engine consumes Product Volume plus Actual Bulk Density.

Commodity identity/reference values do not enter the numerical fingerprint unless an explicitly approved future engine version makes a governed Commodity attribute a numerical input. V6 does not do so.

## 14. Campaign and snapshot behavior

Campaign launch preserves Commodity compatibility/context required for deterministic field operation. A campaign is not retroactively rewritten when a Commodity receives a new current version or becomes INACTIVE.

A StocktakingRevision pins the exact CommodityVersion actually selected for the non-empty record. Campaign/final snapshot structures preserve the stable IDs/version IDs and frozen human-readable/versioned context required by their existing V6 contracts.

## 15. Offline working set

Commodity data is included only as required by the authorized Campaign-scoped Offline Working Set.

- The device receives the governed identities/current versions needed for authorized field selection plus any pinned versions needed by campaign/stocktaking history.
- Offline possession is read/use authority only; it never grants Commodity governance rights.
- A later catalog refresh may add a new current version or lifecycle change without rewriting an already pinned version.
- An INACTIVE Commodity/version that is historically referenced remains locally resolvable where required by the working set/history.
- Offline sync never converts free text into a Commodity identity.
- Catalog/version payload integrity follows the frozen package/manifest/sync contracts in Doc12.

## 16. API contract alignment

Doc11 exposes read-only Commodity resources to organization users: catalog list, Commodity detail and CommodityVersion detail. Customer mutation endpoints for Commodity do not exist.

API representations must distinguish stable Commodity identity from immutable CommodityVersion. A non-empty stocktaking mutation carries the exact `commodityVersionId` required by the working revision. Server validation rejects unknown, unauthorized, non-current-for-new-selection or otherwise invalid Commodity selections according to operation context, while preserving legitimate pinned historical references.

## 17. Persistence contract alignment

The authoritative database preserves Commodity identity separately from CommodityVersion. `currentVersionId` is a controlled pointer; immutable versions are never updated to simulate a new definition. Historical foreign keys use the exact version where required.

Room may cache governed catalog/version rows required by the Campaign working set. Cached rows remain reference data and are not customer-editable local master records.

## 18. UI contract — COM-01 Commodity Catalog

COM-01 is read-only for Admin and Standard.

- Displays the 24 governed Commodity definitions.
- Search is case-insensitive by Commodity display name.
- Lifecycle filter is Active / Inactive / All; default Active.
- Lifecycle is explicit and not communicated by color alone.
- No Add/Edit/Delete/Clone/custom Commodity affordance.
- Offline/local state may display authorized cached catalog data.

## 19. UI contract — COM-02 Commodity Details

COM-02 displays read-only identity/category, version, lifecycle and governed reference information. It must state clearly that Actual Bulk Density is captured operationally and that Reference Test Weight is not an automatic fallback.

Reference values absent from a CommodityVersion are shown as unavailable/not defined rather than fabricated defaults.

## 20. Search, sort and filtering

Catalog search is case-insensitive on version display name. Lifecycle filter values are `ACTIVE`, `INACTIVE`, `ALL`. Stable deterministic ordering uses display name plus Commodity UUID/code tie-breaker where needed. Search/filtering never changes lifecycle or current-version state.

## 21. Historical integrity

- Validated StocktakingRevision preserves exact CommodityVersion identity.
- FinalInventorySnapshot preserves Commodity identity/version and frozen display/reference context required for independent historical reporting.
- Inventory History and reports use snapshot/frozen version data, not today's current Commodity label/reference values.
- A rename, new version, deactivation or reactivation cannot alter a completed historical quantity or label provenance.
- Historical Physical Quantity is never recalculated from current Commodity reference data.

## 22. Validation and failure semantics

| Condition | Required behavior |
|---|---|
| Non-empty validation has no CommodityVersion | Block validation. |
| Mark Empty has no Commodity | Valid; do not fabricate one. |
| Selected Commodity is INACTIVE for a new selection | Reject new selection; historical pinned use remains readable. |
| Selected version is not the governed current version for a new selection | Reject new selection unless operation is explicitly using a valid previously pinned historical/campaign context. |
| Actual Bulk Density missing for non-empty validation | Block validation. |
| Actual Bulk Density ≤ 0 | Block validation. |
| Unsupported density unit | Block validation. |
| Reference Test Weight present but Actual Bulk Density missing | Do not autofill; block non-empty validation. |
| Unknown/custom/free-text Commodity | Reject. |
| Organization user attempts Commodity mutation | Reject authorization. |
| Arbitrary definitionMetadata key/type | Reject unless platform registry admits it. |
| Stale calculation after calculation-relevant working input change | Invalidate/recalculate before validation. |

## 23. Audit and provenance

Platform Commodity governance changes must be attributable through platform operational controls. Application audit/history must preserve the version identity used by stocktaking and final snapshot. Organization audit must not pretend that an organization Admin authored a platform Commodity definition.

Where a reference-data change is relevant to historical interpretation, the old immutable CommodityVersion remains the provenance source.

## 24. Security and tenant boundary

- Commodity definitions are global platform reference data, but access still follows authenticated API/session rules.
- `X-Organization-Id` or route context never grants platform governance rights.
- Organization roles cannot mutate global Commodity state.
- Offline package authorization remains Campaign/device scoped.
- Commodity metadata must not contain secrets, credentials or executable customer content.

## 25. Reporting contract

Reports group/filter by frozen Commodity identity/version context as required by the report specification. Completed-campaign reports derive from immutable FinalInventorySnapshot/read models and must not substitute current CommodityVersion labels/reference values when that would rewrite historical meaning.

The Inventory by Commodity and Inventory History outputs therefore remain stable after catalog version changes.

## 26. Migration and seed-data rules

- The V6 platform seed contains exactly the 24 catalog identities in Section 3.
- Seed operations are idempotent by stable code/identity mapping.
- A seed/update never repurposes an existing code for another Commodity.
- Reference-data corrections that affect historical interpretation publish a new CommodityVersion rather than mutating the old row.
- Removal from new use is represented by INACTIVE, not destructive deletion of referenced history.
- Database/Room migrations preserve pinned commodityVersionId references.

## 27. Required test matrix

| Test | Scenario | Expected result |
|---|---|---|
| COM-T01 | Catalog seed/count | Exactly 24 identities; all normative names present once. |
| COM-T02 | Duplicate code seed | Reject; Commodity.code globally unique. |
| COM-T03 | Code normalization | Trim + uppercase yields canonical code. |
| COM-T04 | Publish version | New versionNumber > prior; currentVersionId switches atomically; prior remains immutable. |
| COM-T05 | Rename | Creates new CommodityVersion; commodityId/code remain stable. |
| COM-T06 | Deactivate Commodity | No new selection; historical references remain readable. |
| COM-T07 | Reactivate Commodity | Current governed version becomes selectable again; identity unchanged. |
| COM-T08 | Admin mutation attempt | Rejected; organization Admin has no Commodity governance right. |
| COM-T09 | Standard mutation attempt | Rejected. |
| COM-T10 | Non-empty without CommodityVersion | Validation blocked. |
| COM-T11 | Mark Empty without Commodity | Allowed under frozen Mark Empty workflow. |
| COM-T12 | Density = 0 | Validation blocked. |
| COM-T13 | Density < 0 | Validation blocked. |
| COM-T14 | 76.4 kg/hl | Normalizes to 764 kg/m³. |
| COM-T15 | 764 kg/m³ | Normalizes to 764 kg/m³. |
| COM-T16 | Reference Test Weight exists; Actual Density missing | Do not autofill; non-empty validation remains blocked. |
| COM-T17 | Moisture changes in new version | No automatic Physical Quantity correction. |
| COM-T18 | Impurities change in new version | No automatic Physical Quantity correction. |
| COM-T19 | Working revision Commodity changes | Dependent calculation context becomes stale/revalidated. |
| COM-T20 | Validated historical record after current rename | Displays frozen historical version data, not rewritten current label. |
| COM-T21 | Historical record after deactivation | Remains readable/reportable. |
| COM-T22 | Offline package | Contains required authorized current/pinned Commodity versions; no mutation capability. |
| COM-T23 | Old offline package references frozen version | Version remains resolvable for historical/pinned operation. |
| COM-T24 | Free-text Other attempt | Rejected; no generic Other Commodity. |
| COM-T25 | Custom tenant Commodity attempt | Rejected. |
| COM-T26 | Arbitrary definitionMetadata key | Rejected unless key/type is in platform-controlled schema. |
| COM-T27 | Final snapshot close | Freezes commodityId, commodityVersionId and required human-readable/versioned context. |
| COM-T28 | Historical report after new CommodityVersion | Uses snapshot/frozen version data. |
| COM-T29 | Current catalog search | Case-insensitive display-name search returns governed records only. |
| COM-T30 | Lifecycle filter | Active/Inactive/All works without changing lifecycle state. |

## 28. Acceptance / release gates

| Gate | Acceptance criterion |
|---|---|
| COM-AC01 | Catalog contains exactly the 24 normative V6 identities and no generic Other/Unknown/Empty item. |
| COM-AC02 | Every Commodity has stable UUID identity, stable globally unique canonical code, lifecycle and currentVersionId. |
| COM-AC03 | Every CommodityVersion is immutable, belongs to exactly one Commodity and has a positive monotonic versionNumber. |
| COM-AC04 | Admin and Standard can read authorized catalog data but cannot mutate platform Commodity definitions. |
| COM-AC05 | ACTIVE current versions are selectable for new non-empty stocktaking; INACTIVE identities remain historical/readable only. |
| COM-AC06 | Non-empty validation pins exact commodityVersionId; Mark Empty requires no Commodity. |
| COM-AC07 | Actual Bulk Density is captured operationally and is never silently populated from Reference Test Weight. |
| COM-AC08 | kg/m³ and kg/hl normalize deterministically according to the frozen Calculation Engine contract. |
| COM-AC09 | Reference Test Weight, Moisture and Impurities are reference-only and never silently change Physical Quantity. |
| COM-AC10 | Commodity/version changes that affect reproducibility create immutable new versions rather than rewriting history. |
| COM-AC11 | Campaign/offline packages contain only required authorized Commodity data and preserve pinned historical versions. |
| COM-AC12 | Offline catalog possession never grants create/edit/delete rights. |
| COM-AC13 | Final snapshots preserve Commodity identity/version plus frozen display/reference context required by reporting. |
| COM-AC14 | Reports/history never join current Commodity data in a way that rewrites historical labels/meaning. |
| COM-AC15 | definitionMetadata is schema-controlled and cannot become arbitrary customer business logic. |
| COM-AC16 | No custom/free-text Commodity path exists in V6 stocktaking. |
| COM-AC17 | Search is case-insensitive by Commodity display name; lifecycle filtering supports Active/Inactive/All as frozen UI contract. |
| COM-AC18 | COM-01/COM-02 remain read-only and make Actual Bulk Density versus Reference Test Weight semantics unambiguous. |
| COM-AC19 | Commodity selection/change correctly invalidates stale quantity calculation context in a working revision. |
| COM-AC20 | No unresolved Commodity governance, lifecycle, versioning, calculation, offline or historical-integrity decision remains for V6 implementation. |

## 29. Approved CO-D01–CO-D32 decision register

| ID | Frozen V6 outcome |
|---|---|
| **CO-D01** | The canonical V6 catalog contains exactly 24 platform-governed Commodity identities; the nominal catalog is normative in this document. |
| **CO-D02** | Commodity is stable logical identity; CommodityVersion is its immutable versioned definition. |
| **CO-D03** | commodityId is a stable UUID and does not change between versions. |
| **CO-D04** | Commodity.code is a stable globally unique machine/business code and is never reused for another Commodity. |
| **CO-D05** | Commodity codes use TRIM + UPPERCASE canonicalization; semantic identity changes are not performed by repurposing a code. |
| **CO-D06** | displayName is versioned in CommodityVersion; a governed rename preserves Commodity identity and creates a new version. |
| **CO-D07** | versionNumber is a positive monotonically increasing integer unique within a Commodity. |
| **CO-D08** | An available Commodity has exactly one currentVersionId for new operations. |
| **CO-D09** | Publishing a new current version is an atomic platform operation; the previous version remains immutable. |
| **CO-D10** | Organization users, including Admin, have read-only Commodity governance: no Add/Edit/Delete/Clone/custom Commodity. |
| **CO-D11** | Commodity is global platform master data, not organization-scoped. |
| **CO-D12** | Commodity identity lifecycle/support state is ACTIVE/INACTIVE; INACTIVE remains historically readable but is not selectable for new operations. |
| **CO-D13** | CommodityVersion has no independent Active/Inactive lifecycle; new-operation availability derives from Commodity lifecycle plus currentVersionId. |
| **CO-D14** | Deactivation never rewrites CampaignSnapshot, StocktakingRevision, FinalInventorySnapshot or historical reports. |
| **CO-D15** | Non-empty Stocktaking persists the exact commodityVersionId, not only commodityId. |
| **CO-D16** | Mark Empty requires no Commodity and never fabricates EMPTY/UNKNOWN/OTHER Commodity values. |
| **CO-D17** | Changing Commodity in a working revision invalidates dependent context and the current CalculationResult when quantity inputs require revalidation/recalculation. |
| **CO-D18** | Actual Bulk Density is operational StocktakingRevision input, not an implicit canonical CommodityVersion property. |
| **CO-D19** | Actual Bulk Density must be >0 for non-empty validation. |
| **CO-D20** | Accepted density input units are kg/m³ and kg/hl; canonical normalization is kg/m³ and 1 kg/hl = 10 kg/m³. |
| **CO-D21** | Entered density value, entered unit and normalized value are retained sufficiently for audit/reproducibility. |
| **CO-D22** | Reference Test Weight belongs to CommodityVersion as optional reference information and has an explicit value/unit. |
| **CO-D23** | Reference Test Weight is informational/plausibility-only and never automatically supplies or replaces Actual Bulk Density. |
| **CO-D24** | Moisture and Impurities are platform-governed reference metadata when defined; they are not automatic V6 quantity-formula inputs. |
| **CO-D25** | V6 applies no automatic moisture, impurity, shrink, packing or other commodity-derived correction to Physical Quantity. |
| **CO-D26** | definitionMetadata is governed by a platform-controlled schema/registry; customer-editable arbitrary JSON is prohibited. |
| **CO-D27** | A reference-data change that must remain historically reproducible creates a new CommodityVersion; existing versions are not mutated. |
| **CO-D28** | Campaign/offline working sets transport only authorized Commodity identity/version/catalog data needed for selection/use; offline possession grants no governance rights. |
| **CO-D29** | A frozen historical version remains displayable after later Commodity deactivation; new selection uses only currently authorized/current catalog data. |
| **CO-D30** | FinalInventorySnapshot preserves stable Commodity IDs/version IDs and frozen human-readable/versioned values needed without current-master joins. |
| **CO-D31** | Reports and Inventory History use frozen historical Commodity/version metadata; current renames/versions never rewrite history. |
| **CO-D32** | V6 has no tenant-specific Commodity, semantic custom alias, free-text Commodity, custom density formula or executable Commodity rule without explicit contract change. |

## 30. Cross-document handoff

- Doc15 Evidence Storage Specification must not introduce Commodity-governance semantics.
- Doc16 Reporting Specification must use frozen Commodity/version snapshot values and never recompute historical quantity from current Commodity reference data.
- Doc17 Security/Audit/Privacy must preserve the platform-versus-organization governance boundary and version provenance.
- Doc18 Test Strategy/Acceptance must include the catalog, versioning, density-boundary, offline and historical-integrity tests defined here.
- Doc19 Retention Policy must retain CommodityVersion data needed by retained validated/final history.
- Doc20 Production Operations must treat the 24-item seed catalog and CommodityVersion publication as controlled platform operations with rollback/history safety.

## 31. Freeze statement

This document is **APPROVED / FROZEN V6** and freezes the SiloMath V6 Commodity contract: the 24-item catalog, identity/code governance, lifecycle, immutable versioning, reference-data semantics, Actual Bulk Density boundary, offline behavior and historical preservation. Any semantic change requires an explicit approved V6 Baseline Change and cross-document consistency update. Non-semantic implementation choices that preserve this contract require no Product/Baseline approval.
