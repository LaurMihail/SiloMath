# Change Request: CR-GEOMETRY-DISPLAY-NAMES-001

## Governance metadata

- CR ID: CR-GEOMETRY-DISPLAY-NAMES-001
- Title: Canonical Geometry Family display names
- Status: APPROVED
- Baseline: SILOMATH_V6
- Changes frozen V6 baseline: YES
- Owner: Laur
- Requested by: Laur
- Created date: 2026-10-31

## Approval

- Decision: APPROVED
- Decision date: 2026-10-31
- Approved / rejected by: Laur
- Superseded by: None

Only an APPROVED Change Request authorizes the explicitly approved frozen
baseline delta.

## Affected frozen baseline

### Documents

- 01_PRODUCT_BASELINE
- 02_DECISION_REGISTER
- 09_DATA_MODEL
- 13_CALCULATION_ENGINE_SPECIFICATION
- 18_TEST_STRATEGY_ACCEPTANCE

### Requirement / Decision IDs

- GeometryFamily frozen V6 contract in 09_DATA_MODEL §4.8.
- Geometry Family registry in 13_CALCULATION_ENGINE_SPECIFICATION §6.
- Approved Calculation Version Contract in 13_CALCULATION_ENGINE_SPECIFICATION §22.1 remains unchanged except for the added canonical display-name mapping.
- A new Decision Register entry is required for this approved clarification.

Where the affected baseline text has no formal Requirement / Decision ID,
identify the exact document and section instead.

## Current frozen contract

SiloMath V6 defines exactly ten platform-governed Geometry Families F01-F10.

09_DATA_MODEL §4.8 requires GeometryFamily to contain:
- geometryFamilyId;
- familyCode;
- displayName;
- formulaVersion;
- parameterContract;
- lifecycle/support state.

The frozen familyCode values and F01-F10 mapping are already defined by the
calculation baseline. The displayName attribute is required and
platform-governed, but the frozen baseline does not assign the ten exact
canonical display-name values.

The existing numerical, parameter, versioning and historical reproducibility
contracts remain authoritative.

## Requested delta

Freeze exactly one canonical English displayName for each of the ten existing
V6 Geometry Families:

| Registry code | Canonical familyCode / formulaCode | Canonical displayName |
|---|---|---|
| F01 | CYL_TRUNC_CONE_V1 | Cylindrical Silo with Truncated Cone Hopper |
| F02 | CYL_FLAT_V1 | Cylindrical Silo with Flat Bottom |
| F03 | SQUARE_FLAT_V1 | Square Silo with Flat Bottom |
| F04 | RECT_FLAT_V1 | Rectangular Silo with Flat Bottom |
| F05 | SQUARE_PYRAMID_HOPPER_V1 | Square Silo with Pyramidal Hopper |
| F06 | RECT_PYRAMID_HOPPER_V1 | Rectangular Silo with Pyramidal Hopper |
| F07 | REG_HEX_FLAT_V1 | Regular Hexagonal Silo with Flat Bottom |
| F08 | STAR_VERIFIED_AREA_V1 | Star-Shaped Storage with Verified Area |
| F09 | POCKET_VERIFIED_AREA_V1 | Pocket Storage with Verified Area |
| F10 | PERIMETER_VERIFIED_AREA_V1 | Perimeter-Based Storage with Verified Area |

This delta adds display data only. It does not create, remove, rename or
reinterpret a Geometry Family identifier, formulaCode, formulaVersion,
parameterContract or numerical rule.

## Reason

The frozen data model requires GeometryFamily.displayName but did not freeze
the concrete values. Production canonical master-data cannot be seeded without
inventing platform-governed values. Freezing the ten display names closes that
baseline gap while preserving all existing numerical semantics.

## Alternatives considered

1. Use familyCode as the UI/display value.
   - Rejected because familyCode is a machine/business identifier and the
     frozen data model separately requires displayName.

2. Reuse test/fixture labels such as "Cylinder".
   - Rejected because repository fixtures are non-authoritative and incomplete.

3. Leave the production seed blocked.
   - Rejected because the ten Geometry Families are required canonical
     platform reference data.

## Impact analysis

### Business rules / lifecycle / workflows

- No lifecycle, workflow or status change.
- The set of supported Geometry Families remains exactly F01-F10.

### UI / navigation / design

- UI may display the approved canonical displayName for the corresponding
  Geometry Family.
- No navigation or screen behavior change is introduced.

### Architecture / module boundaries

- No architecture or module-boundary change.

### Data model / database

- silomath.geometry_families.name is populated with the approved displayName.
- familyCode semantics remain the canonical semantic identifiers
  CYL_TRUNC_CONE_V1 through PERIMETER_VERIFIED_AREA_V1.
- No new table or column is introduced by this CR.

### API / backend contract

- Existing GeometryFamily representations may return the approved displayName.
- No API operation or mutation authority changes.

### Offline / synchronization

- Offline canonical GeometryFamily data carries the approved displayName.
- No offline conflict or synchronization policy changes.

### Calculation engine

- No numerical behavior changes.
- formulaCode, formulaVersion, parameter contracts, dispatch and fingerprints
  are unchanged.

### Evidence / reporting

- Historical/snapshot display values may preserve the approved name when a
  canonical family is captured.
- No reporting calculation changes.

### Security / privacy

- No security or privacy change.

### Retention / production operations

- No retention-policy change.
- Production seed/release validation must assert the approved ten-name mapping.

## RVM impact

Add or update RVM coverage to verify:
- exactly ten canonical Geometry Families exist;
- each canonical familyCode maps to the approved displayName;
- no display-name change modifies formulaCode, formulaVersion or numerical
  behavior;
- offline/package representations preserve the approved displayName where the
  GeometryFamily contract includes it.

## Test impact

Required:
- database seed assertion for all ten familyCode/displayName mappings;
- domain/catalog mapping test where applicable;
- offline package/read test for canonical GeometryFamily displayName;
- regression test proving calculation dispatch remains keyed by the frozen
  formulaCode and formulaVersion, not by displayName.

Existing non-authoritative fixture labels remain test-only and must not be
treated as production canonical values.

## Migration and compatibility impact

The production canonical GeometryFamily seed may populate the approved names.

No historical production row currently requires conversion as part of the
approved seed implementation. If future environments contain authoritative
rows, they must be assessed explicitly rather than silently overwritten.

No client compatibility break is authorized.

## Release-gate impact

Before Production:
- baseline documents and Decision Register updated;
- canonical seed migration passes;
- ten familyCode/displayName assertions pass;
- calculation-engine regression remains GREEN;
- offline/master-data tests remain GREEN;
- CI and migration gates remain GREEN.

## Approved delta

APPROVED on 2026-10-31:

The following GeometryFamily.displayName values are frozen for SiloMath V6:

- F01 / CYL_TRUNC_CONE_V1 = "Cylindrical Silo with Truncated Cone Hopper"
- F02 / CYL_FLAT_V1 = "Cylindrical Silo with Flat Bottom"
- F03 / SQUARE_FLAT_V1 = "Square Silo with Flat Bottom"
- F04 / RECT_FLAT_V1 = "Rectangular Silo with Flat Bottom"
- F05 / SQUARE_PYRAMID_HOPPER_V1 = "Square Silo with Pyramidal Hopper"
- F06 / RECT_PYRAMID_HOPPER_V1 = "Rectangular Silo with Pyramidal Hopper"
- F07 / REG_HEX_FLAT_V1 = "Regular Hexagonal Silo with Flat Bottom"
- F08 / STAR_VERIFIED_AREA_V1 = "Star-Shaped Storage with Verified Area"
- F09 / POCKET_VERIFIED_AREA_V1 = "Pocket Storage with Verified Area"
- F10 / PERIMETER_VERIFIED_AREA_V1 = "Perimeter-Based Storage with Verified Area"

No other Geometry Family semantic, identifier, parameter, formula, version,
lifecycle or workflow change is approved by this CR.

## Implementation verification

Complete when the approved change is applied.

- Baseline documentation updated: PENDING
- Requirement registry updated: PENDING
- RVM updated: PENDING
- Affected tests updated: PENDING
- Applicable validators PASS: PENDING
- CI PASS: PENDING
- Implementation commit / PR: PENDING
- Release evidence: PENDING

## Authority guard

This Change Request MUST NOT be treated as authority while its status is
PROPOSED or REJECTED.

Only the explicitly APPROVED delta may alter the frozen V6 contract.

All unaffected requirements in Documents 01–21 remain authoritative.
