export type CalculationError =
  | "GEOM_MEASUREMENT_COUNT"
  | "GEOM_INVALID_ULLAGE"
  | "GEOM_MIXED_DATUM"
  | "GEOM_MIXED_PROFILE_VERSION"
  | "GEOM_UNSUPPORTED_MEASUREMENT_TYPE"
  | "GEOM_FILL_OUT_OF_RANGE"
  | "GEOM_INVALID_PARAMETER"
  | "GEOM_OUTLET_GE_DIAMETER"
  | "GEOM_AREA_PROVENANCE_REQUIRED"
  | "GEOM_VARIABLE_SECTION_NOT_SUPPORTED"
  | "GEOM_DENSITY_REQUIRED"
  | "GEOM_INVALID_DENSITY"
  | "GEOM_STALE_RESULT"
  | "GEOM_CALCULATION_MISMATCH"
  | "GEOM_VERSION_UNSUPPORTED"

export class CalculationFailure extends Error {
  constructor(readonly code: CalculationError) {
    super(code)
  }
}

type Family =
  | "CYL_TRUNC_CONE_V1"
  | "CYL_FLAT_V1"
  | "SQUARE_FLAT_V1"
  | "RECT_FLAT_V1"
  | "SQUARE_PYRAMID_HOPPER_V1"
  | "RECT_PYRAMID_HOPPER_V1"
  | "REG_HEX_FLAT_V1"
  | "STAR_VERIFIED_AREA_V1"
  | "POCKET_VERIFIED_AREA_V1"
  | "PERIMETER_VERIFIED_AREA_V1"

export interface FrozenCalculationInput {
  frozenContextId: string
  geometryFamilyId: string
  geometryProfileVersionId: string
  geometryFamily: Family
  formulaCode: string
  formulaVersion: number
  inputSchemaVersion: number
  engineVersion: string
  normalizationVersion: number
  surfaceModelVersion: string
  aggregationMethod: string
  aggregationRuleVersion: number
  geometryOrigin: string
  datumReference: string
  measurementReferenceHeightM: string
  maximumFillHeightM: string
  fixedParameters: Record<string, string>
  approvedAreaProvenance: string | null
  hasConstantCrossSection: boolean | null
  readings: Array<{
    sequenceNumber: number
    geometryProfileVersionId: string
    datumReference: string
    geometryOrigin: string
    measurementReferenceHeightM: string
    rawValue: string
    rawUnit: string
    measurementType: string
  }>
  density: {
    value: string
    unit: string
  } | null
}

type Rat = { n: bigint; d: bigint }
type Constant = "ONE" | "PI" | "SQRT_THREE"
type Exact = { coefficient: Rat; constant: Constant }

function fail(code: CalculationError): never {
  throw new CalculationFailure(code)
}

function requireCalc(condition: boolean, code: CalculationError): void {
  if (!condition) fail(code)
}

function absBigInt(x: bigint): bigint {
  return x < 0n ? -x : x
}

function gcd(a: bigint, b: bigint): bigint {
  a = absBigInt(a)
  b = absBigInt(b)
  while (b !== 0n) {
    const t = a % b
    a = b
    b = t
  }
  return a
}

function rat(n: bigint, d: bigint = 1n): Rat {
  if (d === 0n) throw new Error("division by zero")
  const sign = d < 0n ? -1n : 1n
  const g = gcd(n, d)
  return { n: (n / g) * sign, d: (d / g) * sign }
}

function parseDecimal(value: string): Rat {
  const text = value.trim()
  if (!/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(text)) {
    fail("GEOM_INVALID_PARAMETER")
  }

  const negative = text.startsWith("-")
  const unsigned = text.replace(/^[+-]/, "")
  const [wholeRaw, fracRaw = ""] = unsigned.split(".")
  const whole = wholeRaw === "" ? "0" : wholeRaw
  const digits = `${whole}${fracRaw}`.replace(/^0+(?=\d)/, "") || "0"
  const numerator = BigInt(digits) * (negative ? -1n : 1n)
  return rat(numerator, 10n ** BigInt(fracRaw.length))
}

function add(a: Rat, b: Rat): Rat {
  return rat(a.n * b.d + b.n * a.d, a.d * b.d)
}

function sub(a: Rat, b: Rat): Rat {
  return rat(a.n * b.d - b.n * a.d, a.d * b.d)
}

function mul(a: Rat, b: Rat): Rat {
  return rat(a.n * b.n, a.d * b.d)
}

function div(a: Rat, b: Rat): Rat {
  return rat(a.n * b.d, a.d * b.n)
}

function pow(a: Rat, exponent: number): Rat {
  return rat(a.n ** BigInt(exponent), a.d ** BigInt(exponent))
}

function cmp(a: Rat, b: Rat): number {
  const v = a.n * b.d - b.n * a.d
  return v < 0n ? -1 : v > 0n ? 1 : 0
}

const ZERO = rat(0n)
const TWO = rat(2n)
const THREE = rat(3n)

function roundHalfUp(value: Rat, scale: number): string {
  const factor = 10n ** BigInt(scale)
  const scaled = value.n * factor
  const q = scaled / value.d
  const rem = absBigInt(scaled % value.d)
  const den = absBigInt(value.d)

  let rounded = q
  if (rem * 2n >= den) {
    rounded += scaled >= 0n ? 1n : -1n
  }

  const negative = rounded < 0n
  const digits = absBigInt(rounded).toString().padStart(scale + 1, "0")
  const head = scale === 0 ? digits : digits.slice(0, -scale)
  const tail = scale === 0 ? "" : `.${digits.slice(-scale)}`
  return `${negative ? "-" : ""}${head}${tail}`
}

function decimalPlain(value: string): string {
  const r = parseDecimal(value)
  if (r.n === 0n) return "0"

  let denominator = r.d
  let twos = 0
  let fives = 0
  while (denominator % 2n === 0n) {
    denominator /= 2n
    twos++
  }
  while (denominator % 5n === 0n) {
    denominator /= 5n
    fives++
  }
  if (denominator !== 1n) throw new Error("non-terminating canonical decimal")

  const scale = Math.max(twos, fives)
  let numerator = r.n
  numerator *= 2n ** BigInt(scale - twos)
  numerator *= 5n ** BigInt(scale - fives)

  const negative = numerator < 0n
  const digits = absBigInt(numerator).toString().padStart(scale + 1, "0")
  let result = scale === 0
    ? digits
    : `${digits.slice(0, -scale)}.${digits.slice(-scale)}`

  if (result.includes(".")) { result = result.replace(/0+$/, "").replace(/\.$/, "") }
  return `${negative ? "-" : ""}${result || "0"}`
}

function integerSqrt(n: bigint): bigint {
  if (n < 0n) throw new Error("negative sqrt")
  if (n < 2n) return n

  let x = 1n << BigInt(Math.ceil(n.toString(2).length / 2))
  while (true) {
    const next = (x + n / x) >> 1n
    if (next >= x) return x
    x = next
  }
}

function atanBounds(q: bigint, scale: bigint): [bigint, bigint] {
  const square = q * q
  let power = q
  let n = 0n
  let lower = 0n
  let upper = 0n

  while (true) {
    const floor = scale / (power * (2n * n + 1n))
    if (floor === 0n) {
      if (n % 2n === 0n) upper += 1n
      else lower -= 1n
      return [lower, upper]
    }

    if (n % 2n === 0n) {
      lower += floor
      upper += floor + 1n
    } else {
      lower -= floor + 1n
      upper -= floor
    }

    power *= square
    n++
  }
}

function constantBounds(
  constant: Constant,
  digits: number,
): [Rat, Rat] {
  const scale = 10n ** BigInt(digits)

  if (constant === "ONE") {
    return [rat(scale, scale), rat(scale, scale)]
  }

  if (constant === "SQRT_THREE") {
    const root = integerSqrt(3n * scale * scale)
    return [rat(root, scale), rat(root + 1n, scale)]
  }

  const a = atanBounds(5n, scale)
  const b = atanBounds(239n, scale)

  return [
    rat(a[0] * 16n - b[1] * 4n, scale),
    rat(a[1] * 16n - b[0] * 4n, scale),
  ]
}

function requireNumeric206(value: string): string {
  const unsigned = value.startsWith("-") ? value.slice(1) : value
  const digits = unsigned.replace(".", "").replace(/^0+/, "")
  const precision = digits.length === 0 ? 1 : digits.length
  requireCalc(precision <= 20, "GEOM_INVALID_PARAMETER")
  return value
}

function persistenceDecimal(value: Exact): string {
  if (
    value.constant === "ONE" ||
    value.coefficient.n === 0n
  ) {
    return requireNumeric206(roundHalfUp(value.coefficient, 6))
  }

  for (let digits = 32; digits <= 16384; digits *= 2) {
    const [lower, upper] = constantBounds(value.constant, digits)
    const a = roundHalfUp(mul(value.coefficient, lower), 6)
    const b = roundHalfUp(mul(value.coefficient, upper), 6)
    if (a === b) return requireNumeric206(a)
  }

  throw new Error("Persistence rounding could not be certified")
}

function exact(coefficient: Rat, constant: Constant = "ONE"): Exact {
  return { coefficient, constant }
}

function maximumFillHeight(input: FrozenCalculationInput): Rat {
  const p = input.fixedParameters
  switch (input.geometryFamily) {
    case "CYL_TRUNC_CONE_V1":
      return add(parseDecimal(p.hopperHeightM), parseDecimal(p.cylinderHeightM))
    case "CYL_FLAT_V1":
    case "SQUARE_FLAT_V1":
    case "RECT_FLAT_V1":
    case "REG_HEX_FLAT_V1":
    case "STAR_VERIFIED_AREA_V1":
    case "POCKET_VERIFIED_AREA_V1":
    case "PERIMETER_VERIFIED_AREA_V1":
      return parseDecimal(p.heightM)
    case "SQUARE_PYRAMID_HOPPER_V1":
    case "RECT_PYRAMID_HOPPER_V1":
      return add(parseDecimal(p.hopperHeightM), parseDecimal(p.bodyHeightM))
  }
}

function positive(...values: Rat[]): void {
  requireCalc(values.every((v) => cmp(v, ZERO) > 0), "GEOM_INVALID_PARAMETER")
}

function nonnegative(value: Rat): void {
  requireCalc(cmp(value, ZERO) >= 0, "GEOM_INVALID_PARAMETER")
}

function validateGeometry(input: FrozenCalculationInput): void {
  const p = input.fixedParameters

  switch (input.geometryFamily) {
    case "CYL_TRUNC_CONE_V1": {
      const diameter = parseDecimal(p.diameterM)
      const hopper = parseDecimal(p.hopperHeightM)
      const cylinder = parseDecimal(p.cylinderHeightM)
      const outlet = parseDecimal(p.outletDiameterM)
      positive(diameter, hopper)
      nonnegative(cylinder)
      nonnegative(outlet)
      requireCalc(cmp(outlet, diameter) < 0, "GEOM_OUTLET_GE_DIAMETER")
      return
    }

    case "CYL_FLAT_V1":
      positive(parseDecimal(p.diameterM), parseDecimal(p.heightM))
      return

    case "SQUARE_FLAT_V1":
      positive(parseDecimal(p.sideM), parseDecimal(p.heightM))
      return

    case "RECT_FLAT_V1":
      positive(
        parseDecimal(p.lengthM),
        parseDecimal(p.widthM),
        parseDecimal(p.heightM),
      )
      return

    case "SQUARE_PYRAMID_HOPPER_V1":
      positive(parseDecimal(p.sideM), parseDecimal(p.hopperHeightM))
      nonnegative(parseDecimal(p.bodyHeightM))
      return

    case "RECT_PYRAMID_HOPPER_V1":
      positive(
        parseDecimal(p.lengthM),
        parseDecimal(p.widthM),
        parseDecimal(p.hopperHeightM),
      )
      nonnegative(parseDecimal(p.bodyHeightM))
      return

    case "REG_HEX_FLAT_V1":
      positive(parseDecimal(p.sideM), parseDecimal(p.heightM))
      return

    case "STAR_VERIFIED_AREA_V1":
    case "POCKET_VERIFIED_AREA_V1":
    case "PERIMETER_VERIFIED_AREA_V1":
      positive(parseDecimal(p.areaM2), parseDecimal(p.heightM))
      requireCalc(
        !!input.approvedAreaProvenance?.trim(),
        "GEOM_AREA_PROVENANCE_REQUIRED",
      )
      requireCalc(
        input.hasConstantCrossSection === true,
        "GEOM_VARIABLE_SECTION_NOT_SUPPORTED",
      )
      return
  }
}

function volume(input: FrozenCalculationInput, h: Rat): Exact {
  validateGeometry(input)

  const maximum = maximumFillHeight(input)
  requireCalc(
    cmp(h, ZERO) >= 0 && cmp(h, maximum) <= 0,
    "GEOM_FILL_OUT_OF_RANGE",
  )

  const p = input.fixedParameters

  switch (input.geometryFamily) {
    case "CYL_TRUNC_CONE_V1": {
      const radius = div(parseDecimal(p.diameterM), TWO)
      const outlet = div(parseDecimal(p.outletDiameterM), TWO)
      const hop = parseDecimal(p.hopperHeightM)

      const hopperVolume = div(
        mul(
          hop,
          add(add(pow(radius, 2), mul(radius, outlet)), pow(outlet, 2)),
        ),
        THREE,
      )

      const coefficient = cmp(h, hop) <= 0
        ? (() => {
          const atHeight = add(
            outlet,
            div(mul(sub(radius, outlet), h), hop),
          )
          return div(
            mul(
              h,
              add(
                add(pow(outlet, 2), mul(outlet, atHeight)),
                pow(atHeight, 2),
              ),
            ),
            THREE,
          )
        })()
        : add(hopperVolume, mul(pow(radius, 2), sub(h, hop)))

      return exact(coefficient, "PI")
    }

    case "CYL_FLAT_V1":
      return exact(
        mul(pow(div(parseDecimal(p.diameterM), TWO), 2), h),
        "PI",
      )

    case "SQUARE_FLAT_V1":
      return exact(mul(pow(parseDecimal(p.sideM), 2), h))

    case "RECT_FLAT_V1":
      return exact(
        mul(mul(parseDecimal(p.lengthM), parseDecimal(p.widthM)), h),
      )

    case "SQUARE_PYRAMID_HOPPER_V1": {
      const area = pow(parseDecimal(p.sideM), 2)
      const hop = parseDecimal(p.hopperHeightM)
      return exact(
        cmp(h, hop) <= 0
          ? div(mul(area, pow(h, 3)), mul(THREE, pow(hop, 2)))
          : add(div(mul(area, hop), THREE), mul(area, sub(h, hop))),
      )
    }

    case "RECT_PYRAMID_HOPPER_V1": {
      const area = mul(
        parseDecimal(p.lengthM),
        parseDecimal(p.widthM),
      )
      const hop = parseDecimal(p.hopperHeightM)
      return exact(
        cmp(h, hop) <= 0
          ? div(mul(area, pow(h, 3)), mul(THREE, pow(hop, 2)))
          : add(div(mul(area, hop), THREE), mul(area, sub(h, hop))),
      )
    }

    case "REG_HEX_FLAT_V1":
      return exact(
        div(
          mul(mul(THREE, pow(parseDecimal(p.sideM), 2)), h),
          TWO,
        ),
        "SQRT_THREE",
      )

    case "STAR_VERIFIED_AREA_V1":
    case "POCKET_VERIFIED_AREA_V1":
    case "PERIMETER_VERIFIED_AREA_V1":
      return exact(mul(parseDecimal(p.areaM2), h))
  }
}

function normalizedReadings(input: FrozenCalculationInput): Rat[] {
  const readings = [...input.readings].sort(
    (a, b) => a.sequenceNumber - b.sequenceNumber,
  )

  requireCalc(
    readings.length >= 1 && readings.length <= 5,
    "GEOM_MEASUREMENT_COUNT",
  )

  requireCalc(
    readings.every((r) => r.sequenceNumber >= 1 && r.sequenceNumber <= 5) &&
      new Set(readings.map((r) => r.sequenceNumber)).size === readings.length,
    "GEOM_INVALID_PARAMETER",
  )

  requireCalc(
    input.geometryOrigin.trim() !== "" &&
      input.datumReference.trim() !== "",
    "GEOM_INVALID_PARAMETER",
  )

  const referenceHeight = parseDecimal(input.measurementReferenceHeightM)
  requireCalc(cmp(referenceHeight, ZERO) > 0, "GEOM_INVALID_PARAMETER")

  requireCalc(
    cmp(
      parseDecimal(input.maximumFillHeightM),
      maximumFillHeight(input),
    ) === 0,
    "GEOM_INVALID_PARAMETER",
  )

  return readings.map((reading) => {
    requireCalc(
      reading.measurementType === "ULLAGE",
      "GEOM_UNSUPPORTED_MEASUREMENT_TYPE",
    )
    requireCalc(
      reading.geometryProfileVersionId === input.geometryProfileVersionId,
      "GEOM_MIXED_PROFILE_VERSION",
    )
    requireCalc(
      reading.datumReference === input.datumReference &&
        reading.geometryOrigin === input.geometryOrigin &&
        cmp(
          parseDecimal(reading.measurementReferenceHeightM),
          referenceHeight,
        ) === 0,
      "GEOM_MIXED_DATUM",
    )
    requireCalc(reading.rawUnit === "m", "GEOM_INVALID_PARAMETER")

    const raw = parseDecimal(reading.rawValue)
    requireCalc(
      cmp(raw, ZERO) >= 0 && cmp(raw, referenceHeight) <= 0,
      "GEOM_INVALID_ULLAGE",
    )
    return raw
  })
}

function normalizedDensity(input: FrozenCalculationInput): Rat {
  requireCalc(input.density !== null, "GEOM_DENSITY_REQUIRED")
  const density = input.density!
  const value = parseDecimal(density.value)

  requireCalc(cmp(value, ZERO) > 0, "GEOM_INVALID_DENSITY")

  if (density.unit === "kg/m³") return value
  if (density.unit === "kg/hl") return mul(value, rat(10n))

  fail("GEOM_INVALID_DENSITY")
}

function requireSupported(input: FrozenCalculationInput): void {
  const supportedFamilies = new Set<string>([
    "CYL_TRUNC_CONE_V1",
    "CYL_FLAT_V1",
    "SQUARE_FLAT_V1",
    "RECT_FLAT_V1",
    "SQUARE_PYRAMID_HOPPER_V1",
    "RECT_PYRAMID_HOPPER_V1",
    "REG_HEX_FLAT_V1",
    "STAR_VERIFIED_AREA_V1",
    "POCKET_VERIFIED_AREA_V1",
    "PERIMETER_VERIFIED_AREA_V1",
  ])

  requireCalc(
    supportedFamilies.has(String(input.geometryFamily)),
    "GEOM_VERSION_UNSUPPORTED",
  )
  requireCalc(input.formulaVersion === 1, "GEOM_VERSION_UNSUPPORTED")
  requireCalc(input.inputSchemaVersion === 1, "GEOM_VERSION_UNSUPPORTED")
  requireCalc(input.engineVersion === "2.0", "GEOM_VERSION_UNSUPPORTED")
  requireCalc(input.normalizationVersion === 1, "GEOM_VERSION_UNSUPPORTED")
  requireCalc(
    input.surfaceModelVersion === "LEVEL_BASED_V1",
    "GEOM_VERSION_UNSUPPORTED",
  )
  requireCalc(
    input.aggregationMethod === "MEAN_ULLAGE",
    "GEOM_VERSION_UNSUPPORTED",
  )
  requireCalc(input.aggregationRuleVersion === 1, "GEOM_VERSION_UNSUPPORTED")
  requireCalc(
    input.formulaCode === input.geometryFamily,
    "GEOM_VERSION_UNSUPPORTED",
  )
}

function stableJson(value: unknown): string {
  if (value === null || typeof value !== "object") {
    return JSON.stringify(value)
  }

  if (Array.isArray(value)) {
    return `[${value.map(stableJson).join(",")}]`
  }

  const objectValue = value as Record<string, unknown>
  return `{${Object.keys(objectValue).sort().map(
    (key) => `${JSON.stringify(key)}:${stableJson(objectValue[key])}`,
  ).join(",")}}`
}

async function sha256(text: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(text),
  )

  return Array.from(new Uint8Array(digest))
    .map((x) => x.toString(16).padStart(2, "0"))
    .join("")
}

function canonicalDimensions(
  input: FrozenCalculationInput,
): Record<string, string> {
  const p = input.fixedParameters

  switch (input.geometryFamily) {
    case "CYL_TRUNC_CONE_V1":
      return {
        diameterM: decimalPlain(p.diameterM),
        cylinderHeightM: decimalPlain(p.cylinderHeightM),
        hopperHeightM: decimalPlain(p.hopperHeightM),
        outletDiameterM: decimalPlain(p.outletDiameterM),
      }
    case "CYL_FLAT_V1":
      return {
        diameterM: decimalPlain(p.diameterM),
        heightM: decimalPlain(p.heightM),
      }
    case "SQUARE_FLAT_V1":
      return {
        sideM: decimalPlain(p.sideM),
        heightM: decimalPlain(p.heightM),
      }
    case "RECT_FLAT_V1":
      return {
        lengthM: decimalPlain(p.lengthM),
        widthM: decimalPlain(p.widthM),
        heightM: decimalPlain(p.heightM),
      }
    case "SQUARE_PYRAMID_HOPPER_V1":
      return {
        sideM: decimalPlain(p.sideM),
        bodyHeightM: decimalPlain(p.bodyHeightM),
        hopperHeightM: decimalPlain(p.hopperHeightM),
      }
    case "RECT_PYRAMID_HOPPER_V1":
      return {
        lengthM: decimalPlain(p.lengthM),
        widthM: decimalPlain(p.widthM),
        bodyHeightM: decimalPlain(p.bodyHeightM),
        hopperHeightM: decimalPlain(p.hopperHeightM),
      }
    case "REG_HEX_FLAT_V1":
      return {
        sideM: decimalPlain(p.sideM),
        heightM: decimalPlain(p.heightM),
      }
    case "STAR_VERIFIED_AREA_V1":
    case "POCKET_VERIFIED_AREA_V1":
    case "PERIMETER_VERIFIED_AREA_V1":
      return {
        areaM2: decimalPlain(p.areaM2),
        heightM: decimalPlain(p.heightM),
      }
  }
}

async function fingerprint(
  input: FrozenCalculationInput,
  readings: Rat[],
  densityKgM3: Rat,
): Promise<string> {
  const density = input.density!
  const ordered = [...input.readings]
    .sort((a, b) => a.sequenceNumber - b.sequenceNumber)
    .map((reading, index) => ({
      sequenceNumber: reading.sequenceNumber,
      ullageM: decimalPlain(ratToTerminating(readings[index])),
    }))

  const material = {
    frozenContextId: input.frozenContextId,
    geometryFamilyId: input.geometryFamilyId,
    geometryProfileVersionId: input.geometryProfileVersionId,
    geometryFamily: input.geometryFamily,
    formulaCode: input.formulaCode,
    formulaVersion: input.formulaVersion,
    inputSchemaVersion: input.inputSchemaVersion,
    engineVersion: input.engineVersion,
    normalizationVersion: input.normalizationVersion,
    surfaceModelVersion: input.surfaceModelVersion,
    aggregationMethod: input.aggregationMethod,
    aggregationRuleVersion: input.aggregationRuleVersion,
    measurementType: "ULLAGE",
    geometryOrigin: input.geometryOrigin,
    datumReference: input.datumReference,
    measurementReferenceHeightM: decimalPlain(input.measurementReferenceHeightM),
    maximumFillHeightM: decimalPlain(input.maximumFillHeightM),
    fixedParameters: canonicalDimensions(input),
    approvedAreaProvenance:
      input.geometryFamily.endsWith("_VERIFIED_AREA_V1")
        ? input.approvedAreaProvenance
        : null,
    hasConstantCrossSection:
      input.geometryFamily.endsWith("_VERIFIED_AREA_V1")
        ? input.hasConstantCrossSection
        : null,
    orderedNormalizedReadings: ordered,
    enteredDensityValue: decimalPlain(density.value),
    enteredDensityUnit: density.unit,
    normalizedDensityKgM3: decimalPlain(ratToTerminating(densityKgM3)),
  }
  return await sha256(stableJson(material))
}

function ratToTerminating(value: Rat): string {
  let denominator = value.d
  let twos = 0
  let fives = 0
  while (denominator % 2n === 0n) {
    denominator /= 2n
    twos++
  }
  while (denominator % 5n === 0n) {
    denominator /= 5n
    fives++
  }
  if (denominator !== 1n) throw new Error("non-terminating decimal")

  const scale = Math.max(twos, fives)
  let numerator = value.n
  numerator *= 2n ** BigInt(scale - twos)
  numerator *= 5n ** BigInt(scale - fives)

  const negative = numerator < 0n
  const digits = absBigInt(numerator).toString().padStart(scale + 1, "0")

  if (scale === 0) return `${negative ? "-" : ""}${digits}`

  return `${negative ? "-" : ""}${digits.slice(0, -scale)}.${digits.slice(-scale)}`
}

export interface AuthoritativeCalculation {
  inputFingerprint: string
  effectiveUllageM: string
  effectiveFillHeightM: string
  maxCapacityM3: string
  productVolumeM3: string
  normalizedDensityKgM3: string
  physicalQuantityKg: string
}

export function calculateGeometryCapacity(input: FrozenCalculationInput): string {
  requireSupported(input)
  validateGeometry(input)
  const maximum = maximumFillHeight(input)
  requireCalc(cmp(maximum, parseDecimal(input.maximumFillHeightM)) === 0, "GEOM_INVALID_PARAMETER")
  requireCalc(cmp(parseDecimal(input.measurementReferenceHeightM), maximum) >= 0, "GEOM_INVALID_PARAMETER")
  requireCalc(Boolean(input.geometryOrigin.trim()), "GEOM_INVALID_PARAMETER")
  return persistenceDecimal(volume(input, maximum))
}

export async function calculateAuthoritative(
  input: FrozenCalculationInput,
): Promise<AuthoritativeCalculation> {
  requireSupported(input)

  const readings = normalizedReadings(input)
  const densityKgM3 = normalizedDensity(input)

  const total = readings.reduce((sum, v) => add(sum, v), ZERO)
  const mean = div(total, rat(BigInt(readings.length)))

  const fill = sub(parseDecimal(input.measurementReferenceHeightM), mean)
  requireCalc(
    cmp(fill, ZERO) >= 0 &&
      cmp(fill, parseDecimal(input.maximumFillHeightM)) <= 0,
    "GEOM_FILL_OUT_OF_RANGE",
  )

  const capacity = volume(input, maximumFillHeight(input))
  const productVolume = volume(input, fill)

  return {
    inputFingerprint: await fingerprint(input, readings, densityKgM3),
    effectiveUllageM: persistenceDecimal(exact(mean)),
    effectiveFillHeightM: persistenceDecimal(exact(fill)),
    maxCapacityM3: persistenceDecimal(capacity),
    productVolumeM3: persistenceDecimal(productVolume),
    normalizedDensityKgM3: persistenceDecimal(exact(densityKgM3)),
    physicalQuantityKg: persistenceDecimal(
      exact(mul(productVolume.coefficient, densityKgM3), productVolume.constant),
    ),
  }
}

export async function verifyAuthoritativeCalculation(
  input: FrozenCalculationInput,
  clientFingerprint: string,
  clientProductVolumeM3: string,
): Promise<AuthoritativeCalculation> {
  const calculated = await calculateAuthoritative(input)

  if (clientFingerprint !== calculated.inputFingerprint) {
    fail("GEOM_STALE_RESULT")
  }

  const clientVolume = parseDecimal(clientProductVolumeM3)
  requireCalc(cmp(clientVolume, ZERO) >= 0, "GEOM_INVALID_PARAMETER")

  const official = parseDecimal(calculated.productVolumeM3)
  const delta = sub(official, clientVolume)
  const absoluteDelta = rat(absBigInt(delta.n), delta.d)

  if (cmp(absoluteDelta, parseDecimal("0.001")) > 0) {
    fail("GEOM_CALCULATION_MISMATCH")
  }

  return calculated
}
