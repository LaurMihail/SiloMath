package com.silomath.calculation

import java.math.BigDecimal
import com.silomath.calculation.CalculationError.*

/** Doc13 F01-F10 mathematical contracts. This is not a Production release/version registry. */
object FrozenGeometryKernel {
    fun maximumFillHeight(parameters: GeometryParameters): BigDecimal {
        validate(parameters)
        return when(parameters) {
            is CylTruncCone -> parameters.hopperHeightM + parameters.cylinderHeightM
            is CylFlat -> parameters.heightM
            is SquareFlat -> parameters.heightM
            is RectFlat -> parameters.heightM
            is SquarePyramid -> parameters.hopperHeightM + parameters.bodyHeightM
            is RectPyramid -> parameters.hopperHeightM + parameters.bodyHeightM
            is RegularHex -> parameters.heightM
            is VerifiedArea -> parameters.heightM
        }
    }
    fun maximumCapacity(parameters: GeometryParameters) = volume(parameters, maximumFillHeight(parameters))
    fun volume(parameters: GeometryParameters, fillHeightM: BigDecimal): ExactValue = volume(parameters, Rational.decimal(fillHeightM))
    internal fun volume(p: GeometryParameters, h: Rational): ExactValue {
        val maximum = Rational.decimal(maximumFillHeight(p))
        checkCalculation(h >= Rational.ZERO && h <= maximum, GEOM_FILL_OUT_OF_RANGE)
        fun r(value: BigDecimal) = Rational.decimal(value)
        val two = Rational.integer(2); val three = Rational.integer(3)
        val (coefficient, constant) = when(p) {
            is CylTruncCone -> {
                val radius = r(p.diameterM) / two; val outlet = r(p.outletDiameterM) / two; val hop = r(p.hopperHeightM)
                val hopperVolume = hop * (radius.pow(2) + radius * outlet + outlet.pow(2)) / three
                val v = if (h <= hop) {
                    val atHeight = outlet + (radius - outlet) * h / hop
                    h * (outlet.pow(2) + outlet * atHeight + atHeight.pow(2)) / three
                } else hopperVolume + radius.pow(2) * (h - hop)
                v to ExactConstant.PI
            }
            is CylFlat -> (r(p.diameterM) / two).pow(2) * h to ExactConstant.PI
            is SquareFlat -> r(p.sideM).pow(2) * h to ExactConstant.ONE
            is RectFlat -> r(p.lengthM) * r(p.widthM) * h to ExactConstant.ONE
            is SquarePyramid -> {
                val area = r(p.sideM).pow(2); val hop = r(p.hopperHeightM)
                (if (h <= hop) area * h.pow(3) / (three * hop.pow(2)) else area * hop / three + area * (h - hop)) to ExactConstant.ONE
            }
            is RectPyramid -> {
                val area = r(p.lengthM) * r(p.widthM); val hop = r(p.hopperHeightM)
                (if (h <= hop) area * h.pow(3) / (three * hop.pow(2)) else area * hop / three + area * (h - hop)) to ExactConstant.ONE
            }
            is RegularHex -> three * r(p.sideM).pow(2) * h / two to ExactConstant.SQRT_THREE
            is VerifiedArea -> r(p.areaM2) * h to ExactConstant.ONE
        }
        return ExactValue(coefficient, constant)
    }
    private fun validate(p: GeometryParameters) {
        fun positive(vararg values: BigDecimal) = checkCalculation(values.all { it.signum() > 0 }, GEOM_INVALID_PARAMETER)
        fun nonnegative(value: BigDecimal) = checkCalculation(value.signum() >= 0, GEOM_INVALID_PARAMETER)
        when(p) {
            is CylTruncCone -> {
                positive(p.diameterM,p.hopperHeightM);nonnegative(p.cylinderHeightM);nonnegative(p.outletDiameterM)
                checkCalculation(p.outletDiameterM < p.diameterM,GEOM_OUTLET_GE_DIAMETER)
            }
            is CylFlat -> positive(p.diameterM,p.heightM)
            is SquareFlat -> positive(p.sideM,p.heightM)
            is RectFlat -> positive(p.lengthM,p.widthM,p.heightM)
            is SquarePyramid -> { positive(p.sideM,p.hopperHeightM);nonnegative(p.bodyHeightM) }
            is RectPyramid -> { positive(p.lengthM,p.widthM,p.hopperHeightM);nonnegative(p.bodyHeightM) }
            is RegularHex -> positive(p.sideM,p.heightM)
            is VerifiedArea -> {
                checkCalculation(p.family in setOf(GeometryFamily.STAR_VERIFIED_AREA_V1,GeometryFamily.POCKET_VERIFIED_AREA_V1,GeometryFamily.PERIMETER_VERIFIED_AREA_V1),GEOM_INVALID_PARAMETER)
                positive(p.areaM2,p.heightM)
                checkCalculation(!p.approvedProvenanceReference.isNullOrBlank(),GEOM_AREA_PROVENANCE_REQUIRED)
                checkCalculation(p.hasConstantCrossSection,GEOM_VARIABLE_SECTION_NOT_SUPPORTED)
            }
        }
    }
}
