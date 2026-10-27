package com.silomath.calculation

import java.math.BigDecimal
import java.util.UUID

/** Canonical numerical domain contracts only. No repository, platform or mutable master-data access. */
enum class GeometryFamily {
    CYL_TRUNC_CONE_V1, CYL_FLAT_V1, SQUARE_FLAT_V1, RECT_FLAT_V1,
    SQUARE_PYRAMID_HOPPER_V1, RECT_PYRAMID_HOPPER_V1, REG_HEX_FLAT_V1,
    STAR_VERIFIED_AREA_V1, POCKET_VERIFIED_AREA_V1, PERIMETER_VERIFIED_AREA_V1
}

enum class CalculationError {
    GEOM_MEASUREMENT_COUNT, GEOM_INVALID_ULLAGE, GEOM_MIXED_DATUM,
    GEOM_MIXED_PROFILE_VERSION, GEOM_UNSUPPORTED_MEASUREMENT_TYPE,
    GEOM_FILL_OUT_OF_RANGE, GEOM_INVALID_PARAMETER, GEOM_OUTLET_GE_DIAMETER,
    GEOM_AREA_PROVENANCE_REQUIRED, GEOM_VARIABLE_SECTION_NOT_SUPPORTED,
    GEOM_DENSITY_REQUIRED, GEOM_INVALID_DENSITY, GEOM_STALE_RESULT,
    GEOM_CALCULATION_MISMATCH, GEOM_VERSION_UNSUPPORTED
}
class CalculationFailure(val code: CalculationError) : IllegalArgumentException(code.name)
internal fun checkCalculation(condition: Boolean, code: CalculationError) { if (!condition) throw CalculationFailure(code) }

sealed interface GeometryParameters { val family: GeometryFamily }
data class CylTruncCone(val diameterM: BigDecimal, val cylinderHeightM: BigDecimal,
    val hopperHeightM: BigDecimal, val outletDiameterM: BigDecimal) : GeometryParameters {
    override val family = GeometryFamily.CYL_TRUNC_CONE_V1
}
data class CylFlat(val diameterM: BigDecimal, val heightM: BigDecimal) : GeometryParameters {
    override val family = GeometryFamily.CYL_FLAT_V1
}
data class SquareFlat(val sideM: BigDecimal, val heightM: BigDecimal) : GeometryParameters {
    override val family = GeometryFamily.SQUARE_FLAT_V1
}
data class RectFlat(val lengthM: BigDecimal, val widthM: BigDecimal, val heightM: BigDecimal) : GeometryParameters {
    override val family = GeometryFamily.RECT_FLAT_V1
}
data class SquarePyramid(val sideM: BigDecimal, val bodyHeightM: BigDecimal, val hopperHeightM: BigDecimal) : GeometryParameters {
    override val family = GeometryFamily.SQUARE_PYRAMID_HOPPER_V1
}
data class RectPyramid(val lengthM: BigDecimal, val widthM: BigDecimal, val bodyHeightM: BigDecimal,
    val hopperHeightM: BigDecimal) : GeometryParameters { override val family = GeometryFamily.RECT_PYRAMID_HOPPER_V1 }
data class RegularHex(val sideM: BigDecimal, val heightM: BigDecimal) : GeometryParameters {
    override val family = GeometryFamily.REG_HEX_FLAT_V1
}
/** Provenance is an already-approved source reference, not an engine assertion of engineering approval. */
data class VerifiedArea(val familyCode: GeometryFamily, val areaM2: BigDecimal, val heightM: BigDecimal,
    val approvedProvenanceReference: String?, val hasConstantCrossSection: Boolean) : GeometryParameters {
    override val family = familyCode
}

data class FrozenNumericalProfile(val geometryProfileVersionId: UUID, val parameters: GeometryParameters,
    val geometryOrigin: String, val datumReference: String, val measurementReferenceHeightM: BigDecimal,
    val maximumFillHeightM: BigDecimal)
data class UllageReading(val sequenceNumber: Int, val geometryProfileVersionId: UUID,
    val datumReference: String, val geometryOrigin: String, val measurementReferenceHeightM: BigDecimal,
    val rawValue: BigDecimal, val rawUnit: String, val measurementType: String)
data class ActualBulkDensity(val value: BigDecimal, val unit: String)

/** No defaults or invented release identifiers. Construction is NOT a compatibility certificate. */
data class PinnedCalculationVersions(val formulaVersion: String, val inputSchemaVersion: String,
    val engineVersion: String, val normalizationVersion: String, val surfaceModelVersion: String,
    val aggregationMethod: String, val aggregationRuleVersion: Int)
data class CanonicalCalculationInput(val frozenContextId: UUID, val geometryFamilyId: UUID,
    val profile: FrozenNumericalProfile, val readings: List<UllageReading>,
    val density: ActualBulkDensity?, val versions: PinnedCalculationVersions)

/** Numerical kernels do not issue version-certified CalculationResults or workflow validation. */
data class NumericalResult(val effectiveUllageM: ExactValue, val effectiveFillHeightM: ExactValue,
    val maxCapacityM3: ExactValue, val productVolumeM3: ExactValue,
    val normalizedDensityKgM3: BigDecimal, val physicalQuantityKg: ExactValue)
