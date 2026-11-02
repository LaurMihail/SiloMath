import {
  calculateAuthoritative,
  verifyAuthoritativeCalculation,
  CalculationFailure,
  type FrozenCalculationInput,
} from "./authoritative-calculation.ts"

function addDecimal(a:string,b:string):string {
  function parts(v:string) {
    const neg=v.startsWith("-")
    const s=v.replace(/^[+-]/,"")
    const [w,f=""]=s.split(".")
    return { neg, w:w||"0", f }
  }
  const A=parts(a), B=parts(b)
  const scale=Math.max(A.f.length,B.f.length)
  const ai=BigInt((A.w+A.f.padEnd(scale,"0"))||"0")*(A.neg?-1n:1n)
  const bi=BigInt((B.w+B.f.padEnd(scale,"0"))||"0")*(B.neg?-1n:1n)
  const sum=ai+bi
  const neg=sum<0n
  const digits=(sum<0n?-sum:sum).toString().padStart(scale+1,"0")
  return scale===0
    ? `${neg?"-":""}${digits}`
    : `${neg?"-":""}${digits.slice(0,-scale)}.${digits.slice(-scale)}`
}

function input(
  geometryFamily: FrozenCalculationInput["geometryFamily"],
  fixedParameters: Record<string,string>,
  fillHeightM: string,
  approvedAreaProvenance: string | null = null,
  hasConstantCrossSection: boolean | null = null,
): FrozenCalculationInput {
  const profileId="33333333-3333-4333-8333-333333333333"

  let max:string
  switch (geometryFamily) {
    case "CYL_TRUNC_CONE_V1":
      max=addDecimal(fixedParameters.hopperHeightM,fixedParameters.cylinderHeightM)
      break
    case "SQUARE_PYRAMID_HOPPER_V1":
    case "RECT_PYRAMID_HOPPER_V1":
      max=addDecimal(fixedParameters.hopperHeightM,fixedParameters.bodyHeightM)
      break
    default:
      max=fixedParameters.heightM
  }

  const ullage=addDecimal(max,`-${fillHeightM}`)

  return {
    frozenContextId:"11111111-1111-4111-8111-111111111111",
    geometryFamilyId:"22222222-2222-4222-8222-222222222222",
    geometryProfileVersionId:profileId,
    geometryFamily,
    formulaCode:geometryFamily,
    formulaVersion:1,
    inputSchemaVersion:1,
    engineVersion:"2.0",
    normalizationVersion:1,
    surfaceModelVersion:"LEVEL_BASED_V1",
    aggregationMethod:"MEAN_ULLAGE",
    aggregationRuleVersion:1,
    geometryOrigin:"BOTTOM",
    datumReference:"test-frozen-datum",
    measurementReferenceHeightM:max,
    maximumFillHeightM:max,
    fixedParameters,
    approvedAreaProvenance,
    hasConstantCrossSection,
    readings:[{
      sequenceNumber:1,
      geometryProfileVersionId:profileId,
      datumReference:"test-frozen-datum",
      geometryOrigin:"BOTTOM",
      measurementReferenceHeightM:max,
      rawValue:ullage,
      rawUnit:"m",
      measurementType:"ULLAGE",
    }],
    density:{value:"76.4",unit:"kg/hl"},
  }
}

const cases = [
  [input("CYL_TRUNC_CONE_V1",{diameterM:"18",cylinderHeightM:"15",hopperHeightM:"6",outletDiameterM:"0.45"},"17"),"3321.138601"],
  [input("CYL_FLAT_V1",{diameterM:"10",heightM:"20"},"15"),"1178.097245"],
  [input("SQUARE_FLAT_V1",{sideM:"6",heightM:"20"},"18"),"648.000000"],
  [input("RECT_FLAT_V1",{lengthM:"8",widthM:"6",heightM:"24"},"20"),"960.000000"],
  [input("SQUARE_PYRAMID_HOPPER_V1",{sideM:"6",bodyHeightM:"14",hopperHeightM:"6"},"3"),"9.000000"],
  [input("RECT_PYRAMID_HOPPER_V1",{lengthM:"8",widthM:"6",bodyHeightM:"12",hopperHeightM:"6"},"9"),"240.000000"],
  [input("REG_HEX_FLAT_V1",{sideM:"4",heightM:"20"},"12"),"498.830633"],
  [input("STAR_VERIFIED_AREA_V1",{areaM2:"30",heightM:"20"},"16","approved-test-drawing",true),"480.000000"],
  [input("POCKET_VERIFIED_AREA_V1",{areaM2:"25",heightM:"18"},"14","approved-test-survey",true),"350.000000"],
  [input("PERIMETER_VERIFIED_AREA_V1",{areaM2:"42.5",heightM:"22"},"19.75","approved-test-as-built",true),"839.375000"],
] as const

function assertEquals<T>(actual:T,expected:T,message:string):void {
  if (actual!==expected) {
    throw new Error(`${message}: expected ${expected}, got ${actual}`)
  }
}

async function assertCalculationFailure(
  operation:()=>Promise<unknown>,
  expectedCode:CalculationFailure["code"],
  message:string,
):Promise<void> {
  try {
    await operation()
  } catch (error) {
    if (error instanceof CalculationFailure && error.code===expectedCode) {
      return
    }
    throw error
  }
  throw new Error(message)
}

for (const [source,expected] of cases) {
  Deno.test(`authoritative calculation ${source.geometryFamily} uses exact V6 arithmetic`,async()=>{
    const result=await calculateAuthoritative(source)
    assertEquals(
      result.productVolumeM3,
      expected,
      `${source.geometryFamily} product volume`,
    )
    assertEquals(
      result.productVolumeM3.split(".")[1]?.length,
      6,
      `${source.geometryFamily} persistence scale`,
    )
  })
}

const base=cases[1][0]
const frozenFingerprint="72e43e3c77aac0a8d58e7cab5f785e3c464f228244944637abac13d05501e60c"

Deno.test("authoritative fingerprint is stable for the frozen normalized input",async()=>{
  const authoritative=await calculateAuthoritative(base)
  assertEquals(
    authoritative.inputFingerprint,
    frozenFingerprint,
    "authoritative input fingerprint",
  )
})

Deno.test("verification accepts the inclusive ±0.001 m3 tolerance",async()=>{
  const authoritative=await calculateAuthoritative(base)
  for (const clientVolume of ["1178.096245","1178.097245","1178.098245"]) {
    await verifyAuthoritativeCalculation(
      base,
      authoritative.inputFingerprint,
      clientVolume,
    )
  }
})

Deno.test("verification rejects values outside the ±0.001 m3 tolerance",async()=>{
  const authoritative=await calculateAuthoritative(base)
  for (const clientVolume of ["1178.096244","1178.098246"]) {
    await assertCalculationFailure(
      ()=>verifyAuthoritativeCalculation(
        base,
        authoritative.inputFingerprint,
        clientVolume,
      ),
      "GEOM_CALCULATION_MISMATCH",
      `out-of-tolerance value ${clientVolume} was accepted`,
    )
  }
})

Deno.test("verification rejects a stale normalized fingerprint",async()=>{
  const authoritative=await calculateAuthoritative(base)
  await assertCalculationFailure(
    ()=>verifyAuthoritativeCalculation(
      base,
      "0".repeat(64),
      authoritative.productVolumeM3,
    ),
    "GEOM_STALE_RESULT",
    "stale fingerprint was accepted",
  )
})

Deno.test("unknown geometry family has no latest-handler fallback",async()=>{
  const invalidFamily = {
    ...base,
    geometryFamily:"UNKNOWN_FAMILY_V1",
    formulaCode:"UNKNOWN_FAMILY_V1",
  } as unknown as FrozenCalculationInput
  await assertCalculationFailure(
    ()=>calculateAuthoritative(invalidFamily),
    "GEOM_VERSION_UNSUPPORTED",
    "unknown geometry family was accepted",
  )
})

Deno.test("NUMERIC(20,6) overflow is rejected",async()=>{
  const overflowInput=input(
    "SQUARE_FLAT_V1",
    {sideM:"100000000",heightM:"100000000"},
    "0",
  )
  await assertCalculationFailure(
    ()=>calculateAuthoritative(overflowInput),
    "GEOM_INVALID_PARAMETER",
    "NUMERIC(20,6) overflow was accepted",
  )
})

Deno.test("only the frozen calculation version tuple is accepted",async()=>{
  const unsupportedTuples:FrozenCalculationInput[]=[
    {...base,engineVersion:"2.1"},
    {...base,inputSchemaVersion:2},
    {...base,normalizationVersion:2},
    {...base,surfaceModelVersion:"OTHER_SURFACE_MODEL"},
    {...base,aggregationMethod:"OTHER_AGGREGATION"},
    {...base,aggregationRuleVersion:2},
  ]
  for (const unsupported of unsupportedTuples) {
    await assertCalculationFailure(
      ()=>calculateAuthoritative(unsupported),
      "GEOM_VERSION_UNSUPPORTED",
      "unsupported calculation version tuple was accepted",
    )
  }
})
