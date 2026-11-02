package com.silomath.calculation

import java.math.BigDecimal
import com.silomath.calculation.CalculationError.*

/** Canonical SI boundary. Additional display/input units require their documented adapter, not guessed conversions. */
object MeasurementKernel {
    fun normalize(readings: List<UllageReading>, profile: FrozenNumericalProfile): List<BigDecimal> {
        checkCalculation(readings.size in 1..5,GEOM_MEASUREMENT_COUNT)
        checkCalculation(readings.all { it.sequenceNumber in 1..5 } && readings.map { it.sequenceNumber }.distinct().size == readings.size,GEOM_INVALID_PARAMETER)
        checkCalculation(profile.geometryOrigin.isNotBlank() && profile.datumReference.isNotBlank(),GEOM_INVALID_PARAMETER)
        checkCalculation(profile.measurementReferenceHeightM.signum() > 0,GEOM_INVALID_PARAMETER)
        checkCalculation(profile.maximumFillHeightM.compareTo(FrozenGeometryKernel.maximumFillHeight(profile.parameters)) == 0,GEOM_INVALID_PARAMETER)
        return readings.sortedBy { it.sequenceNumber }.map { reading ->
            checkCalculation(reading.measurementType == "ULLAGE",GEOM_UNSUPPORTED_MEASUREMENT_TYPE)
            checkCalculation(reading.geometryProfileVersionId == profile.geometryProfileVersionId,GEOM_MIXED_PROFILE_VERSION)
            checkCalculation(reading.datumReference == profile.datumReference && reading.geometryOrigin == profile.geometryOrigin &&
                reading.measurementReferenceHeightM.compareTo(profile.measurementReferenceHeightM) == 0,GEOM_MIXED_DATUM)
            checkCalculation(reading.rawUnit == "m",GEOM_INVALID_PARAMETER)
            checkCalculation(reading.rawValue.signum() >= 0 && reading.rawValue <= profile.measurementReferenceHeightM,GEOM_INVALID_ULLAGE)
            reading.rawValue
        }
    }
    fun meanUllage(profile: FrozenNumericalProfile,readings: List<UllageReading>): ExactValue {
        val values = normalize(readings,profile)
        return ExactValue(values.fold(Rational.ZERO) { sum,value -> sum + Rational.decimal(value) } / Rational.integer(values.size.toLong()))
    }
    fun effectiveFillHeight(profile: FrozenNumericalProfile,readings: List<UllageReading>): ExactValue {
        val mean = meanUllage(profile,readings)
        val fill = Rational.decimal(profile.measurementReferenceHeightM) - mean.coefficient
        checkCalculation(fill >= Rational.ZERO && fill <= Rational.decimal(profile.maximumFillHeightM),GEOM_FILL_OUT_OF_RANGE)
        return ExactValue(fill)
    }
}
object QuantityKernel {
    fun normalizedDensity(density: ActualBulkDensity?): BigDecimal {
        checkCalculation(density != null,GEOM_DENSITY_REQUIRED)
        val actual = requireNotNull(density)
        checkCalculation(actual.value.signum() > 0,GEOM_INVALID_DENSITY)
        return when(actual.unit) {
            "kg/m\u00b3" -> actual.value
            "kg/hl" -> actual.value * BigDecimal.TEN
            else -> throw CalculationFailure(GEOM_INVALID_DENSITY)
        }
    }
    fun physicalQuantity(volumeM3: ExactValue,density: ActualBulkDensity?): ExactValue {
        checkCalculation(volumeM3.coefficient >= Rational.ZERO,GEOM_INVALID_PARAMETER)
        return volumeM3.times(normalizedDensity(density))
    }
}
