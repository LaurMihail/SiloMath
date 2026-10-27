# SiloMath V6 — 13_CALCULATION_ENGINE_SPECIFICATION

**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-27 - CR-CALC-VERSION-CONTRACT-001 approved calculation-version clarification

## 1. Purpose and authority

This document is the normative V6 contract for numerical stocktaking calculation. It consolidates the frozen product/business/data/database/API/offline contracts and the explicitly approved CE-D01–CE-D42 decisions. It defines what Android and server implementations must calculate, validate, version, persist, verify and reproduce. UI presentation is not numerical authority.

Normative precedence for this document is the frozen V6 documentation set and approved CE-D01–CE-D42. Earlier SiloMatch calculation material is traceability input only where consistent with V6; it cannot override this contract.

## 2. Scope

- Geometry Engine for exactly ten V6 Geometry Families F01–F10.
- Measurement normalization, datum validation and MEAN_ULLAGE_V1 aggregation.
- LEVEL_BASED_V1 effective fill-height model.
- Maximum Capacity and current Product Volume.
- Actual Bulk Density normalization and Physical Quantity.
- Deterministic precision, rounding, fingerprinting and versioning.
- Offline/server parity, validation errors, golden vectors and historical reproducibility.
- Mark Empty interaction with the calculation domain.

Out of scope: arbitrary 3D surfaces, point clouds, calibration curves, CAD/mesh volume, eccentric/wedge hoppers, non-apex pyramidal frustums for F05/F06, automatic moisture/impurity corrections, packing factors, reconciliation, tenant-authored executable formulas.

## 3. Canonical calculation pipeline

For a non-empty StocktakingRevision:

```text
Measurements
  → Unit Normalization
  → Compatibility / Datum Validation
  → MEAN_ULLAGE_V1 Aggregation
  → Effective Fill Height
  → Geometry Engine V_family(h)
  → Product Volume [m³]
  → Actual Bulk Density Normalization [kg/m³]
  → Quantity Engine
  → Physical Quantity [kg]
```

Canonical equations:

```text
u_eff = (u1 + ... + un) / n,  1 ≤ n ≤ 5
h = measurementReferenceHeightM - u_eff
V = V_family(h)
M = V × ρ
```

The surface model identifier is `LEVEL_BASED_V1`; aggregation is `MEAN_ULLAGE_V1`. No reading is automatically classified as an outlier or discarded.

## 4. Measurement and datum contract

`ULLAGE` is the V6 numerical field measurement: vertical distance downward from the frozen measurement reference point to the product surface. `geometryOrigin` is the family-specific h=0 plane. `measurementReferenceHeightM` is the vertical elevation of the physical measurement point above that origin.

Rules:

- Exactly 1–5 readings for a non-empty numerical calculation.
- Every reading is normalized to metres before aggregation.
- Every reading must be ≥0 and ≤ `measurementReferenceHeightM`.
- All readings in one result use the same GeometryProfileVersion, datum/reference, measurement type and compatible unit semantics.
- One invalid reading blocks the result; no partial subset calculation is allowed.
- `h` must satisfy `0 ≤ h ≤ maximumFillHeightM`.
- `measurementReferenceHeightM` may exceed `maximumFillHeightM` when the physical reference point lies above the maximum product surface.
- Geometry is read-only during stocktaking; an apparent geometry mismatch is reported as an Issue, not corrected by changing geometry.

## 5. GeometryProfileVersion calculation contract

Every calculation-capable immutable GeometryProfileVersion carries the controlled family identity, formula/schema versions, family parameters in canonical SI units, `measurementReferenceHeightM`, `maximumFillHeightM`, `geometryOrigin`, calculated `maxCapacityM3`, and provenance where the family requires it.

`maximumFillHeightM` must equal the full geometric fill height defined by the family parameter schema. `maxCapacityM3` is always derived as `V_family(maximumFillHeightM)` and is never manually entered.

## 6. Geometry Family registry

| Code | Canonical identifier | Fixed profile inputs | Blocking parameter rules | maximumFillHeightM | V(h) strategy |
|---|---|---|---|---|---|
| F01 | CYL_TRUNC_CONE_V1 | D,Hcyl,Hhop,d | D>0; Hcyl≥0; Hhop>0; 0≤d<D | Hhop+Hcyl | Piecewise frustum then cylinder |
| F02 | CYL_FLAT_V1 | D,H | D>0; H>0 | H | π(D/2)²h |
| F03 | SQUARE_FLAT_V1 | a,H | a>0; H>0 | H | a²h |
| F04 | RECT_FLAT_V1 | L,W,H | L,W,H>0 | H | LWh |
| F05 | SQUARE_PYRAMID_HOPPER_V1 | a,Hbody,Hhop | a>0; Hbody≥0; Hhop>0 | Hhop+Hbody | Piecewise square pyramid then square prism |
| F06 | RECT_PYRAMID_HOPPER_V1 | L,W,Hbody,Hhop | L,W>0; Hbody≥0; Hhop>0 | Hhop+Hbody | Piecewise rectangular pyramid then rectangular prism |
| F07 | REG_HEX_FLAT_V1 | s,H | s>0; H>0 | H | [(3√3/2)s²]h |
| F08 | STAR_VERIFIED_AREA_V1 | A,H,provenance | A>0; H>0; provenance required | H | Ah |
| F09 | POCKET_VERIFIED_AREA_V1 | A,H,provenance | A>0; H>0; provenance required | H | Ah |
| F10 | PERIMETER_VERIFIED_AREA_V1 | A,H,provenance | A>0; H>0; provenance required | H | Ah |

All dimensions are internal usable dimensions. Formula definitions are platform-governed controlled application data. Organization users cannot submit formula text or redefine a Geometry Family.

## 7. F01 — CYL_TRUNC_CONE_V1

Parameters: `D` internal diameter; `Hcyl` cylindrical body height; `Hhop` truncated-cone hopper height; `d` outlet diameter. Constraints: `D>0`, `Hcyl≥0`, `Hhop>0`, `0≤d<D`.

```text
R = D / 2
r = d / 2
Vhopper = (π × Hhop / 3) × (R² + Rr + r²)
Vcylinder = πR²Hcyl
Vmax = Vhopper + Vcylinder

For 0 ≤ h ≤ Hhop:
  radius(h) = r + (R-r) × (h/Hhop)
  V(h) = (πh/3) × [r² + r×radius(h) + radius(h)²]

For Hhop < h ≤ Hhop+Hcyl:
  V(h) = Vhopper + πR²(h-Hhop)
```

Boundary invariants: `V(0)=0`, both branches agree at `h=Hhop`, and `V(Hhop+Hcyl)=Vmax`. `d=0` is mathematically a true cone but remains the same explicit versioned family contract.

## 8. F02 — CYL_FLAT_V1

Parameters: `D>0`, `H>0`; `R=D/2`.

```text
Vmax = πR²H
V(h) = πR²h,  0 ≤ h ≤ H
```

## 9. F03 — SQUARE_FLAT_V1

Parameters: internal side `a>0`, height `H>0`.

```text
Vmax = a²H
V(h) = a²h,  0 ≤ h ≤ H
```

## 10. F04 — RECT_FLAT_V1

Parameters: internal length `L>0`, width `W>0`, height `H>0`.

```text
Vmax = LWH
V(h) = LWh,  0 ≤ h ≤ H
```

## 11. F05 — SQUARE_PYRAMID_HOPPER_V1

V6 interpretation: square vertical prism above an inverted square pyramidal hopper terminating at an apex. Parameters: `a>0`, `Hbody≥0`, `Hhop>0`. Origin is the hopper apex.

```text
Vhopper = a²Hhop / 3
Vmax = Vhopper + a²Hbody

For 0 ≤ h ≤ Hhop:
  a(h) = a × h/Hhop
  V(h) = a²h³ / (3Hhop²)

For Hhop < h ≤ Hhop+Hbody:
  V(h) = Vhopper + a²(h-Hhop)
```

The family is apex-only in V6. A non-zero outlet, square pyramidal frustum or other hopper interpretation is not inferred and requires a new explicitly versioned contract.

## 12. F06 — RECT_PYRAMID_HOPPER_V1

V6 interpretation: rectangular vertical prism above an inverted rectangular pyramidal hopper terminating at an apex. Parameters: `L>0`, `W>0`, `Hbody≥0`, `Hhop>0`. Origin is the hopper apex.

```text
Vhopper = LWHhop / 3
Vmax = Vhopper + LWHbody

For 0 ≤ h ≤ Hhop:
  L(h) = L × h/Hhop
  W(h) = W × h/Hhop
  V(h) = LWh³ / (3Hhop²)

For Hhop < h ≤ Hhop+Hbody:
  V(h) = Vhopper + LW(h-Hhop)
```

The family is apex-only in V6. Rectangular frustum, eccentric and wedge variants require a new explicitly versioned contract.

## 13. F07 — REG_HEX_FLAT_V1

Parameters: internal regular-hexagon side length `s>0`, height `H>0`. V6 deliberately uses side length as the primary cross-section dimension so across-flats/apothem/diameter interpretations cannot be mixed within the same schema version.

```text
Ahex = (3√3 / 2) × s²
Vmax = Ahex × H
V(h) = Ahex × h,  0 ≤ h ≤ H
```

F07 is flat-bottom constant-section. A hexagonal hopper/composite form requires another controlled family/formula version.

## 14. F08–F10 — verified constant-area families

F08 `STAR_VERIFIED_AREA_V1`, F09 `POCKET_VERIFIED_AREA_V1`, and F10 `PERIMETER_VERIFIED_AREA_V1` retain distinct family identities but share one mathematical V6 strategy.

Parameters: verified horizontal cross-sectional area `A>0`, height `H>0`, and mandatory approved area provenance.

```text
Vmax = A × H
V(h) = A × h,  0 ≤ h ≤ H
```

Accepted provenance is a traceable approved source such as a certified drawing, engineering calculation, surveyed/as-built value or equivalent approved reference. These families are invalid where the actual horizontal cross-section varies with height. V6 does not invent arm counts, radii, neighboring-cell geometry or another universal parametric formula.

## 15. Maximum Capacity

`maxCapacityM3` is a deterministic property of GeometryProfileVersion:

```text
maxCapacityM3 = V_family(maximumFillHeightM)
```

Changing any calculation-affecting dimension, datum or family interpretation creates a new immutable GeometryProfileVersion. Existing launched Campaigns and historical results remain pinned to the frozen version.

## 16. Quantity Engine

For non-empty stocktaking, Actual Bulk Density is mandatory and must be positive. Supported V6 density input units are `kg/m³` and `kg/hl`.

```text
ρkg/m3 = entered density, when unit = kg/m³
ρkg/m3 = entered density × 10, when unit = kg/hl
Mkg = ProductVolumeM3 × ρkg/m3
```

Reference Test Weight may support reference/plausibility UX but never fills a missing Actual Bulk Density automatically. Moisture and Impurities do not correct quantity in V6. Tonnes are a display/export adapter (`t = kg/1000`); canonical calculated quantity remains kg.

## 17. Mark Empty

Mark Empty is a workflow action, not a degenerate non-empty formula call. A successful Mark Empty revision is Validated with `isEmpty=true`, `ProductVolumeM3=0` and `PhysicalQuantityKg=0`. Measurements, Commodity, Actual Bulk Density, Ownership and Client are not required for that path. Notes/evidence may remain under their frozen workflow rules.

## 18. Numeric precision and rounding

- All formulas execute after normalization to SI.
- Intermediate processing uses at least 15 significant decimal digits.
- No intermediate rounding.
- π and √3 use deterministic documented mathematical implementations; rounded literals such as 3.14 or 1.732 are prohibited.
- Persistence values governed by this contract use scale 6 with `ROUND_HALF_UP`; PostgreSQL uses `NUMERIC(20,6)` where applicable.
- Display formatting, including two-decimal geometry presentation in UI, never feeds back into calculation.

## 19. Validation and failure semantics

| Error code | Blocking condition |
|---|---|
| `GEOM_MEASUREMENT_COUNT` | Reading count outside 1..5. |
| `GEOM_INVALID_ULLAGE` | Negative ullage or ullage above measurement reference height. |
| `GEOM_MIXED_DATUM` | Readings do not share the same frozen datum/reference. |
| `GEOM_MIXED_PROFILE_VERSION` | Readings do not share the same GeometryProfileVersion. |
| `GEOM_UNSUPPORTED_MEASUREMENT_TYPE` | Measurement type is not V6 ULLAGE. |
| `GEOM_FILL_OUT_OF_RANGE` | Derived fill height is outside [0, maximumFillHeightM]. |
| `GEOM_INVALID_PARAMETER` | A family parameter violates its domain. |
| `GEOM_OUTLET_GE_DIAMETER` | F01 outlet diameter d is not strictly less than D. |
| `GEOM_AREA_PROVENANCE_REQUIRED` | F08–F10 verified area lacks approved provenance. |
| `GEOM_VARIABLE_SECTION_NOT_SUPPORTED` | Verified-area family has height-varying cross-section. |
| `GEOM_DENSITY_REQUIRED` | Non-empty quantity calculation has no Actual Bulk Density. |
| `GEOM_INVALID_DENSITY` | Actual Bulk Density is ≤0 or unit unsupported. |
| `GEOM_STALE_RESULT` | Stored CalculationResult fingerprint no longer matches current calculation inputs. |
| `GEOM_CALCULATION_MISMATCH` | Client/server Product Volume delta exceeds 0.001 m³ for identical normalized fingerprint. |
| `GEOM_VERSION_UNSUPPORTED` | Required frozen formula/schema/engine semantics cannot be reproduced by the executing build. |

Calculation is integrity-first: missing/ambiguous dimensional interpretation blocks the operation. No automatic clamp, guessed dimension, hidden fallback or silent formula substitution is permitted.

## 20. CalculationResult reproducibility record

For a non-empty numerical result, persistence/snapshotting must preserve enough data to reproduce and audit the exact result. At minimum:

- StocktakingRevision / Campaign snapshot context and GeometryProfileVersion ID
- Geometry Family identifier; formulaCode and formulaVersion
- inputSchemaVersion; engineVersion; normalizationVersion
- surfaceModelVersion=`LEVEL_BASED_V1`
- aggregationMethod=`MEAN_ULLAGE` and aggregationRuleVersion=`1`; combined semantic identifier `MEAN_ULLAGE_V1`
- geometry origin, measurementReferenceHeightM and maximumFillHeightM
- all family parameters used, in canonical units
- ordered normalized readings and their sequence numbers
- effectiveUllageM and effectiveFillHeightM
- maxCapacityM3 and currentProductVolumeM3
- entered Actual Bulk Density value/unit and normalizedDensityKgM3
- physicalQuantityKg
- normalizedInputFingerprint
- creation/validation provenance required by the frozen data/audit contracts

Validated CalculationResult content is immutable. A new working revision creates a new calculation lineage; it does not overwrite the validated historical result.

## 21. Normalized input fingerprint

The fingerprint is SHA-256 over a canonical deterministic serialization of calculation-affecting normalized values and version identifiers. The serialization must be locale-independent, stable in field ordering, explicit about nullability/units, and must use canonical decimal representations rather than display-formatted strings.

Minimum fingerprint material:

```text
campaignSnapshotSpaceId / frozen calculation context
geometryProfileVersionId
geometryFamilyId + formulaCode + formulaVersion
inputSchemaVersion + engineVersion + normalizationVersion
surfaceModelVersion + aggregationMethod + aggregationRuleVersion
measurementReferenceHeightM + maximumFillHeightM + geometryOrigin
all normalized fixed geometry inputs
ordered normalized readings
entered density value/unit + normalizedDensityKgM3
```

Any calculation-relevant change invalidates the prior fingerprint/result. Commodity identity, Ownership and Client are business snapshot data but do not enter the numerical fingerprint unless a future version explicitly makes them alter a numerical input.

## 22. Version semantics

| Version | Changes when | Does not change merely for |
|---|---|---|
| `geometryProfileVersion` | dimensions, datum or calculation-affecting profile configuration changes | stock measurement changes |
| `formulaVersion` | mathematical formula or branch semantics change | refactor with numerically identical contract |
| `inputSchemaVersion` | required/meaningful formula inputs or their interpretation change | UI label/layout change |
| `engineVersion` | software behavior relevant to numerical reproducibility/certification changes | pure UI change |
| `aggregationRuleVersion` | measurement aggregation semantics change | different reading values under same rule |
| `normalizationVersion` | canonical conversion/normalization semantics change | display-unit preference only |
| `surfaceModelVersion` | surface interpretation changes | ordinary input value changes |

Historical results identify the exact versions used. New versions never silently reinterpret old results.

### 22.1 Approved Calculation Version Contract

CR-CALC-VERSION-CONTRACT-001 / FD-73 is APPROVED. The canonical tuple is:

| Field | Canonical type | Approved value |
|---|---|---|
| engineVersion | String | "2.0" |
| inputSchemaVersion | Integer | 1 |
| normalizationVersion | Integer | 1 |
| formulaVersion | Integer | 1 for F01-F10 |
| surfaceModelVersion | Identifier | LEVEL_BASED_V1 |
| aggregationMethod | Enum | MEAN_ULLAGE |
| aggregationRuleVersion | Integer | 1 |
| combined semantic identifier | Identifier | MEAN_ULLAGE_V1 |

| Family | Frozen formulaCode (unchanged) | formulaVersion |
|---|---|---|
| F01 | CYL_TRUNC_CONE_V1 | 1 |
| F02 | CYL_FLAT_V1 | 1 |
| F03 | SQUARE_FLAT_V1 | 1 |
| F04 | RECT_FLAT_V1 | 1 |
| F05 | SQUARE_PYRAMID_HOPPER_V1 | 1 |
| F06 | RECT_PYRAMID_HOPPER_V1 | 1 |
| F07 | REG_HEX_FLAT_V1 | 1 |
| F08 | STAR_VERIFIED_AREA_V1 | 1 |
| F09 | POCKET_VERIFIED_AREA_V1 | 1 |
| F10 | PERIMETER_VERIFIED_AREA_V1 | 1 |

The combined identifier is not the aggregationMethod field value. Production resolution binds the complete approved tuple and formulaCode to actual implemented handlers. Unknown tuples produce GEOM_VERSION_UNSUPPORTED; there is no latest-handler fallback or alias from historical placeholder strings. Keep pinned inputs and actual historical identifiers; no existing snapshot/result is silently relabeled. Integer wire/domain metadata may have a documented lossless decimal-text storage representation where an existing physical version column is TEXT; this does not authorize SQL schema mutation or historical conversion.

This approval clarifies identifiers and representation only: formulas, units, datum, aggregation arithmetic, precision, persistence rounding and verification tolerance are unchanged. Approval does not complete CAM-008, CALC tasks or GATE-M5.

## 23. Offline calculation and server verification

Android calculates immediately from the frozen Campaign/GeometryProfileVersion working set using the same V6 formula and normalization semantics as the server. Sync does not make the server blindly trust a client result: the authoritative side can recalculate/verify from the frozen normalized inputs and versions.

For identical normalized fingerprint:

- absolute Product Volume delta `≤ 0.001 m³`: verification may accept the server-authoritative normalized result and record verification metadata;
- delta `> 0.001 m³`: raise `GEOM_CALCULATION_MISMATCH`; never silently overwrite validated local evidence/history.

The 0.001 m³ threshold is a cross-engine verification tolerance only. It never legalizes an invalid dimension, fill height or other domain input.

A mobile/server release must remain compatible with frozen versions it is allowed to process. If the required frozen semantics cannot be reproduced, calculation/validation blocks with `GEOM_VERSION_UNSUPPORTED` rather than substituting the current formula.

## 24. Historical reproducibility

Historical calculations are never recalculated from current Geometry Profile, Commodity reference data or other mutable master data. Reproduction uses the frozen calculation inputs and pinned version identifiers. Formula handlers needed for retained history must remain reproducible for the applicable retention period, or an equivalent verified reproduction mechanism must exist.

FinalInventorySnapshot stores the frozen values required by its V6 contract; reporting reads the immutable historical result/snapshot and must not re-run today's engine against old measurements.

## 25. Domain architecture contract

The Calculation Engine is domain-layer logic independent of Compose. UI/ViewModels collect/display state and invoke use cases; they do not implement geometry formulas. Family formulas resolve through a controlled handler registry keyed by frozen family/formula versions. Android and server implementations share the same normative test vectors and version semantics.

Recommended conceptual interfaces:

```text
GeometryCalculationInput
  frozenProfileVersion
  orderedMeasurements[1..5]
  measurementType
  aggregation/surface/normalization versions

GeometryCalculationResult
  effectiveUllageM
  effectiveFillHeightM
  maxCapacityM3
  currentProductVolumeM3
  formula/schema/engine versions
  normalizedInputFingerprint

QuantityCalculationInput
  currentProductVolumeM3
  actualBulkDensityValue
  actualBulkDensityUnit

QuantityCalculationResult
  normalizedDensityKgM3
  physicalQuantityKg
```

## 26. Golden vectors — all ten families

Expected values below are canonical SI values rounded only at the persistence/test assertion boundary to six decimals.

| Test | Family | Inputs | Expected Vmax m³ | Expected V(h) m³ |
|---|---|---|---:|---:|
| CE-G01 | F01 | D=18; Hcyl=15; Hhop=6; d=0.45; h=17 | 4339.014620 | 3321.138601 |
| CE-G02 | F02 | D=10; H=20; h=15 | 1570.796327 | 1178.097245 |
| CE-G03 | F03 | a=6; H=20; h=18 | 720.000000 | 648.000000 |
| CE-G04 | F04 | L=8; W=6; H=24; h=20 | 1152.000000 | 960.000000 |
| CE-G05 | F05 | a=6; Hbody=14; Hhop=6; h=3 | 576.000000 | 9.000000 |
| CE-G06 | F06 | L=8; W=6; Hbody=12; Hhop=6; h=9 | 672.000000 | 240.000000 |
| CE-G07 | F07 | s=4; H=20; h=12 | 831.384388 | 498.830633 |
| CE-G08 | F08 | A=30; H=20; h=16 | 600.000000 | 480.000000 |
| CE-G09 | F09 | A=25; H=18; h=14 | 450.000000 | 350.000000 |
| CE-G10 | F10 | A=42.5; H=22; h=19.75 | 935.000000 | 839.375000 |

### 26.1 Aggregation vector

`readings = [3,4,5] m` → `effectiveUllage=4 m`. With `referenceHeight=18 m`, `h=14 m`.

### 26.2 Quantity vector

Using CE-G01 current Product Volume `3321.138601 m³` and Actual Bulk Density `76.4 kg/hl`: normalized density is `764 kg/m³`; expected Physical Quantity is `2537349.890960 kg`.

### 26.3 Boundary and failure vectors

| Test | Input / condition | Expected |
|---|---|---|
| CE-B01 | 0 measurements | GEOM_MEASUREMENT_COUNT |
| CE-B02 | 6 measurements | GEOM_MEASUREMENT_COUNT |
| CE-B03 | ullage < 0 | GEOM_INVALID_ULLAGE |
| CE-B04 | ullage > reference height | GEOM_INVALID_ULLAGE |
| CE-B05 | F01 d = D | GEOM_OUTLET_GE_DIAMETER |
| CE-B06 | F08–F10 area without provenance | GEOM_AREA_PROVENANCE_REQUIRED |
| CE-B07 | measurement changes after calculation | prior result becomes STALE / GEOM_STALE_RESULT |
| CE-B08 | derived h < 0 or h > maximumFillHeight | GEOM_FILL_OUT_OF_RANGE |
| CE-B09 | offline/server volume delta > 0.001 m³ | GEOM_CALCULATION_MISMATCH |
| CE-B10 | Actual Bulk Density = 0 | GEOM_INVALID_DENSITY |
| CE-B11 | mixed datum in readings | GEOM_MIXED_DATUM |
| CE-B12 | mixed profile version in readings | GEOM_MIXED_PROFILE_VERSION |
| CE-B13 | F05 h=Hhop | continuous at hopper/body boundary |
| CE-B14 | F06 h=Hhop | continuous at hopper/body boundary |
| CE-B15 | F01 h=Hhop | continuous at frustum/cylinder boundary |
| CE-B16 | h=0 for every family | V(h)=0 |
| CE-B17 | h=maximumFillHeight for every family | V(h)=Vmax |
| CE-B18 | kg/hl density conversion | normalized density=value×10 |
| CE-B19 | Mark Empty | V=0; M=0; no non-empty density/measurement requirement |
| CE-B20 | unsupported historical formula/engine version | GEOM_VERSION_UNSUPPORTED; never silently recalculate with current semantics |

## 27. Required unit-test properties

- For every family: V(0)=0 and V(maximumFillHeightM)=Vmax.
- For every valid family/profile: V(h) is non-decreasing over its valid domain.
- F01, F05 and F06 piecewise branches are continuous at the hopper/body boundary.
- Equivalent supported density units normalize to the same kg/m³ and produce the same quantity.
- Equivalent supported length units, when admitted by adapters, normalize to the same canonical geometry inputs.
- Changing any fingerprint input makes the previous CalculationResult stale.
- Repeated execution with identical canonical inputs and versions produces the same persistence-boundary outputs.
- Mark Empty always yields exact zero volume/quantity without invoking non-empty density requirements.
- Validated historical results remain unchanged after newer GeometryProfileVersion/formula/engine releases.

## 28. Acceptance / release gates

| Gate | Acceptance criterion |
|---|---|
| CALC-AC01 | Exactly F01–F10 resolve to controlled V6 handlers; no tenant executable formulas. |
| CALC-AC02 | All family parameter schemas, constraints, Vmax and V(h) branches are explicit and deterministic. |
| CALC-AC03 | ULLAGE, datum semantics, 1–5 readings and MEAN_ULLAGE_V1 are enforced identically offline/server. |
| CALC-AC04 | No invalid reading is silently discarded and no invalid fill is silently clamped. |
| CALC-AC05 | Max Capacity is derived from the immutable GeometryProfileVersion. |
| CALC-AC06 | F05/F06 apex-only semantics and F07 side-length semantics are unambiguous. |
| CALC-AC07 | F08–F10 require positive verified area + provenance and reject variable cross-section use. |
| CALC-AC08 | Geometry Engine outputs m³ only; Quantity Engine uses Actual Bulk Density and outputs canonical kg. |
| CALC-AC09 | Reference Test Weight, Moisture and Impurities never silently alter Physical Quantity. |
| CALC-AC10 | Canonical units/conversions are explicit and equivalent supported units yield identical normalized inputs. |
| CALC-AC11 | At least 15 significant intermediate digits, no intermediate rounding, scale-6 HALF_UP persistence. |
| CALC-AC12 | Golden vectors cover all ten families plus branch boundaries and quantity conversion. |
| CALC-AC13 | Client/server Product Volume parity uses ≤0.001 m³ verification tolerance only, not input clamping. |
| CALC-AC14 | CalculationResult contains sufficient frozen inputs, outputs and version metadata for reproduction. |
| CALC-AC15 | SHA-256 normalized input fingerprint changes for every calculation-relevant semantic/input change. |
| CALC-AC16 | Validated/final historical calculations are immutable and never recalculated from current master data. |
| CALC-AC17 | Mark Empty is represented as a workflow zero result and cannot be confused with a non-empty numerical calculation. |
| CALC-AC18 | Calculation engine remains domain-layer logic independently unit-testable from Compose/UI. |
| CALC-AC19 | Offline calculation and authoritative server verification use the same formula/version semantics. |
| CALC-AC20 | No unresolved mathematical, datum, precision, versioning or reproducibility decision remains for V6 implementation. |

## 29. Approved CE-D01–CE-D42 decision register

| ID | Frozen V6 outcome |
|---|---|
| **CE-D01** | V6 engine has two numerical components: Geometry Engine outputs m³; Quantity Engine outputs kg. |
| **CE-D02** | Canonical pipeline: Measurements → Normalization → Aggregation → Effective Fill Height → Geometry Engine → Product Volume → Quantity Engine → Physical Quantity. |
| **CE-D03** | The only V6 numerical measurement type is ULLAGE: vertical distance down from the frozen datum/reference point to the product surface. |
| **CE-D04** | Surface model is LEVEL_BASED_V1; it does not reconstruct an arbitrary irregular 3D surface. |
| **CE-D05** | Aggregation is MEAN_ULLAGE_V1 over all 1–5 valid compatible readings; no automatic outlier rejection. |
| **CE-D06** | Effective fill height h = measurementReferenceHeightM − effectiveUllageM; h=0 is the family geometry origin. |
| **CE-D07** | Valid fill domain is 0 ≤ h ≤ maximumFillHeightM; invalid values block calculation; no silent clamp. |
| **CE-D08** | For non-empty stocktaking V = V_family(h), M = V × ρ, with ρ = Actual Bulk Density. |
| **CE-D09** | Moisture and Impurities do not automatically correct Physical Quantity in V6. |
| **CE-D10** | Mark Empty bypasses normal non-empty calculation and produces V=0 and M=0. |
| **CE-D11** | F01 CYL_TRUNC_CONE_V1 uses the approved cylinder + conical-frustum piecewise capacity/fill contract. |
| **CE-D12** | F02 CYL_FLAT_V1 is a constant-section circular cylinder with parameters D,H. |
| **CE-D13** | F03 SQUARE_FLAT_V1 is a constant-section square prism with parameters a,H. |
| **CE-D14** | F04 RECT_FLAT_V1 is a constant-section rectangular prism with parameters L,W,H. |
| **CE-D15** | F05 SQUARE_PYRAMID_HOPPER_V1 is a square vertical prism over an inverted square pyramidal hopper terminating at an apex; parameters a,Hbody,Hhop. |
| **CE-D16** | F05 is apex-only in V6; a square pyramidal frustum/non-zero outlet requires a new versioned contract. |
| **CE-D17** | F06 RECT_PYRAMID_HOPPER_V1 is a rectangular vertical prism over an inverted rectangular pyramidal hopper terminating at an apex; parameters L,W,Hbody,Hhop. |
| **CE-D18** | F06 is apex-only in V6; frustum/eccentric/wedge variants require a new versioned contract. |
| **CE-D19** | F07 REG_HEX_FLAT_V1 uses internal regular-hexagon side length s as its primary cross-section parameter, plus H. |
| **CE-D20** | F07 area A=(3√3/2)s², Vmax=A·H, V(h)=A·h. |
| **CE-D21** | F07 is flat-bottom constant-section; hexagonal hopper/composite variants require a new versioned contract. |
| **CE-D22** | F08/F09/F10 retain distinct family identities but share the V6 verified-area handler Vmax=A·H and V(h)=A·h. |
| **CE-D23** | F08–F10 require A>0, H>0 and approved area provenance. |
| **CE-D24** | F08–F10 are invalid when the real horizontal cross-section varies with height; no invented parametric substitute is allowed. |
| **CE-D25** | Every stocktaking-capable GeometryProfileVersion has positive measurementReferenceHeightM and maximumFillHeightM; maximumFillHeightM matches the family's full geometric fill height. |
| **CE-D26** | measurementReferenceHeightM may exceed maximumFillHeightM when the physical measurement point lies above maximum fill. |
| **CE-D27** | Every ullage is non-negative and not above measurementReferenceHeightM; all aggregated readings share profile version, datum, measurement type and compatible units; one invalid reading blocks calculation. |
| **CE-D28** | Canonical units are m, m², m³, kg/m³ and kg; 1 kg/hl = 10 kg/m³. |
| **CE-D29** | Intermediate calculation uses at least 15 significant decimal digits and no intermediate rounding. |
| **CE-D30** | Persistence boundary is scale 6 ROUND_HALF_UP; PostgreSQL uses NUMERIC(20,6) where the numeric contract applies. |
| **CE-D31** | π and √3 use deterministic documented mathematical implementations; rounded constants such as 3.14/1.732 are forbidden; golden vectors govern parity. |
| **CE-D32** | No tolerance makes invalid geometry valid; client/server verification accepts absolute Product Volume delta ≤0.001 m³ for identical normalized input fingerprint, otherwise GEOM_CALCULATION_MISMATCH. |
| **CE-D33** | Actual Bulk Density must be >0 and V6 accepts kg/m³ or kg/hl, normalized to kg/m³. |
| **CE-D34** | Reference Test Weight may support information/plausibility only and never automatically replaces missing Actual Bulk Density. |
| **CE-D35** | Physical Quantity Mkg=Vm3×ρkg/m3; tonnes are display/export conversion only; canonical quantity remains kg. |
| **CE-D36** | maxCapacityM3 = V_family(maximumFillHeightM) and is never manually entered. |
| **CE-D37** | Max Capacity belongs to GeometryProfileVersion; calculation-affecting profile changes create a new immutable version. |
| **CE-D38** | CalculationResult preserves all calculation-relevant version IDs, datum, normalized ordered readings, geometry inputs, derived values, density inputs, outputs and normalized input fingerprint. |
| **CE-D39** | Fingerprint is SHA-256 over canonical deterministic serialization of calculation-affecting normalized inputs and version identifiers; locale/UI strings are excluded. |
| **CE-D40** | Fingerprint changes whenever numerical result or its interpretation can change; non-numerical Commodity/Client/Ownership identity is excluded unless it changes a numerical input. |
| **CE-D41** | Version semantics are explicit for geometryProfileVersion, formulaVersion, inputSchemaVersion, engineVersion, aggregationRuleVersion, normalizationVersion and surfaceModelVersion. |
| **CE-D42** | Validated/final historical results are never recalculated from current master data; reproduction uses frozen inputs and pinned compatible versions/handlers. |

## 30. Cross-document handoff

- Doc14 Commodity Specification must preserve Actual Bulk Density as the operational quantity input and must not introduce an automatic Reference Test Weight fallback or moisture/impurity correction.
- Doc16 Reporting Specification must report frozen Product Volume/Physical Quantity and must not recompute historical results with the current engine.
- Doc17 Security/Audit/Privacy must treat formula/version/fingerprint and validation provenance as integrity-sensitive audit context.
- Doc18 Test Strategy/Acceptance must include all golden/boundary/property tests in this document, offline/server parity, stale-result detection and historical reproduction tests.
- Doc19 Retention must preserve the data/version capability required to reproduce retained historical calculations.
- Doc20 Production Operations must prevent incompatible engine releases from processing frozen versions they cannot reproduce.

## 31. Freeze statement

This document is **APPROVED / FROZEN V6** and freezes the SiloMath V6 Calculation Engine contract. Any later semantic change to F01–F10 parameter meaning, formulas, datum semantics, aggregation, surface model, numerical precision, quantity semantics, fingerprint composition or version/reproduction behavior requires an explicit approved V6 Baseline Change and corresponding cross-document consistency update. Non-semantic implementation choices that preserve this contract require no Product/Baseline approval; architecturally material choices may be recorded through the implementation ADR process.

## Approved calculation-version cross-reference

CR-CALC-VERSION-CONTRACT-001 / FD-73: `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`. Formula codes are the unchanged F01-F10 identifiers in Doc13 sections6/22.1. Domain/API version numbers are integers except engineVersion, which is the exact string "2.0". Resolve only actual implemented handlers for the full pinned tuple; unsupported versions return GEOM_VERSION_UNSUPPORTED without fallback. Preserve historical metadata verbatim; no automatic reassignment to this initial approved tuple. This clarification changes no mathematics and is not evidence of completed integration or release gates.
