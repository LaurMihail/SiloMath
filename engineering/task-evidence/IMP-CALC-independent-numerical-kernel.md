# Independent Doc13 numerical kernel - execution-order exception

Status: IMPLEMENTED / LOCAL VERIFIED numerical subkernel; version-certified Production engine and end-to-end CALC tasks remain PARTIAL / NOT COMPLETE. User explicitly approved implementing pure Kotlin independently of CAM-008. CAM-008 is NOT PASS. GATE-M5 is NOT Done. OFF-006 remains PASS independently.

## Authority and implementation

01_PRODUCT_BASELINE section 12; 02_DECISION_REGISTER D31/D32; Doc13 CE-D01..42, sections 3..19 and 25..28; canonical numeric/domain contracts from Docs09/10. No new engine/formula/schema/normalization identifier has been assigned and no test-only version string is used as Production metadata.

Files: calculation-engine/build.gradle.kts; calculation-engine/src/main/kotlin/com/silomath/calculation/{CalculationContract,ExactValue,FrozenGeometryKernel,MeasurementKernel,NumericalKernel}.kt; calculation-engine/src/test/kotlin/com/silomath/calculation/{GeometryGoldenTest,MeasurementAndPrecisionTest}.kt.

- Canonical numerical DTOs use UUID, explicit family-specific SI parameter types, BigDecimal source inputs, frozen datum/profile identity and ordered ULLAGE readings. No CampaignRepository, Android, Room, Retrofit, legacy model or mutable master-data lookup.
- Real mathematical dispatch implements exactly the documented F01-F10 family contracts, including F01 conical frustum, F05/F06 apex-only hoppers, F07 side length and F08-F10 approved provenance/constant-section checks. This is mathematical family dispatch, NOT a certified historical formulaVersion registry.
- Canonical-metre normalization validates every reading/profile/datum/type, 1-5 readings and unique sequence 1-5. No unsupported raw-unit conversion is guessed; non-SI adapters and their supported unit registry are outside this subkernel.
- All readings contribute to exact mean; effective height is checked without clamping. Reference height can exceed maximum fill height. Density accepts the documented kg/m3 (superscript-three canonical spelling) and kg/hl, with exact x10 normalization; no Reference Test Weight fallback.
- NumericalKernel composes normalization, exact mean/fill, capacity/current volume and density/quantity. It returns numerical values only, never a persisted/certified CalculationResult, validated revision, compatibility/authorization assertion or READY state.

## Deterministic precision implementation

Reduced BigInteger rational coefficients preserve all decimal source values and non-terminating means/frustum ratios exactly. Volumes retain an exact symbolic factor ONE, PI or SQRT_THREE. No Double, Float, MathContext, finite rounded pi literal or intermediate scale rounding is used.

At explicit toPersistenceDecimal only: rational values round HALF_UP to scale6. Irrational values use rigorous rational enclosures: Machin pi=16*atan(1/5)-4*atan(1/239), alternating-series remainder bounds and integer-floor error bounds; sqrt3 uses integer Newton lower/upper bounds. Precision increases until both enclosure endpoints certify the same scale6 result. NUMERIC(20,6) boundary overflow rejects rather than clamps. Integer sqrt does not rely on Android API33 BigInteger.sqrt().

This is an internal non-semantic numerical implementation choice, not a Baseline Change or a newly assigned release/version contract.

## Verification actually executed

- `.\gradlew.bat :calculation-engine:compileKotlin`: initially exposed the existing empty module's Java17/Kotlin24 target mismatch; targeted fix explicitly sets Kotlin JVM17 to match existing Java17, without SDK/Kotlin/AGP upgrades. Final compile PASS.
- `.\gradlew.bat :calculation-engine:test :calculation-engine:build`: initial 48-test run exposed an encoding-corrupted SI literal and a test that incorrectly demanded identical rounded buckets across a continuous boundary. One targeted correction per cause: escaped canonical SI literal; exact-coefficient convergence assertion instead of rounded-bucket equality. Corrected run PASS.
- Final after adding the composed canonical numerical entry point: `.\gradlew.bat :calculation-engine:test :calculation-engine:build :app:compileProductionDebugKotlin`: **49 PASS**, zero failures/errors/skips; module build PASS; Production Kotlin compilation PASS. Production compilation is NOT end-to-end calculation integration or server parity.
- GeometryGoldenTest: 40 tests (Doc13 CE-G01..10 maxima/current, zero/full boundaries, exact-coefficient monotonicity/range rejection).
- MeasurementAndPrecisionTest: 9 tests (canonical pipeline without snapshot builder, exact repeating mean, official aggregation/reference vector, invalid readings, mixed datum/profile/type/sequence, geometry/provenance guards, hopper continuity/apex/zero-body, density/quantity vector and no intermediate rounding, HALF_UP/locale/persistence overflow).
- Official quantity vector uses FULL unrounded CE-G01 volume: 2537349.890960 kg, distinguishing it from 2537349.891164 kg produced by prematurely rounding volume.
- `git diff --check`: PASS. Main imports checked: only JDK and this module; tests add existing catalog JUnit dependency, no version upgrade.

## Task boundary accounting

| Scope | Current state |
|---|---|
| CALC-001 canonical numerical API/entry point | Standalone numerical API implemented/tested; version-certified API and CampaignSnapshot integration incomplete |
| CALC-002 normalization | Canonical SI path implemented/tested; no unsupported adapter unit set invented |
| CALC-003/004 mean/fill | Exact numerical behavior implemented/tested |
| CALC-005..014 F01..F10 | All mathematical kernels/goldens implemented/tested; historical version binding unassigned |
| CALC-015/016/017 density/quantity/precision | Numerical policy implemented/tested; persistence adapters not migrated by this task |
| CALC-018/019 fingerprint/stale detection | NOT IMPLEMENTED; metadata/version boundary unresolved |
| CALC-020 golden suite | All ten numerical golden vectors PASS; not entire milestone closure |
| CALC-021 boundary/property suite | Standalone numerical properties PASS; full version/workflow acceptance remains incomplete |
| CALC-022 client/server parity | NOT IMPLEMENTED / NOT TESTED; Kotlin unit results are not a server integration test |
| CALC-023 historical compatibility | BLOCKED at approved release/handler identifiers; no allowed-string-list substitute |
| CAM-008 / CampaignSnapshot integration | Still incomplete; required before real end-to-end compatibility |
| GATE-M5 | OPEN; all terminal branches and real integration are not green |

## Exact boundary requiring clarification/approved contract

1. Official-document contradiction at the persisted aggregationMethod representation:
   - 01_PRODUCT_BASELINE section12: aggregationMethod=MEAN_ULLAGE, aggregationRuleVersion=1, combined rule identifier MEAN_ULLAGE_V1.
   - 09_DATA_MODEL CalculationResult: MEAN_ULLAGE enum; ruleVersion1; combined identifier MEAN_ULLAGE_V1.
   - 13_CALCULATION_ENGINE_SPECIFICATION section20: explicitly writes aggregationMethod=MEAN_ULLAGE_V1.
   These cannot both be the same literal field value. No serializer, stored value or compatibility rule has been chosen by assumption. Numerical mean semantics themselves are consistent.
2. Concrete approved Production values/mapping for engineVersion, formulaVersion per family, inputSchemaVersion and normalizationVersion are not established in the inspected official documents/current approved contracts. AggregationRuleVersion=1 and surfaceModelVersion=LEVEL_BASED_V1 ARE documented; they are not invented or treated as missing. Versioned family codes do not automatically define all these separate version fields.

Required: clarify the contradictory aggregationMethod field wording through the official authority process, and provide/approve the concrete implementation release/handler version contract with provenance. No version name is proposed as if approved. The metadata/fingerprint/version-certified registry boundary is stopped. No fake compatibility, synthetic CampaignSnapshot in Production or substitution for legacy history. No push, Supabase mutation, baseline file edit, READY activation or AUTH-010 bypass.
