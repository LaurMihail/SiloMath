package com.silomath.calculation

/**
 * Standalone numerical pipeline for canonical frozen inputs. Does not issue a version-certified
 * CalculationResult, persist state, validate a revision, or assert CampaignSnapshot compatibility.
 * Production binding requires the separately approved release/handler version contract.
 */
object NumericalKernel {
    /** Explicit workflow-zero numerical branch, never a non-empty result or an authorization/validation grant. */
    fun quantitiesForExplicitMarkEmpty(): EmptyNumericalQuantities = EmptyNumericalQuantities

    fun evaluate(profile: FrozenNumericalProfile, readings: List<UllageReading>, density: ActualBulkDensity?): NumericalResult {
        val capturedReadings = readings.toList()
        val mean = MeasurementKernel.meanUllage(profile,capturedReadings)
        val fill = MeasurementKernel.effectiveFillHeight(profile,capturedReadings)
        val capacity = FrozenGeometryKernel.maximumCapacity(profile.parameters)
        val volume = FrozenGeometryKernel.volume(profile.parameters,fill.coefficient)
        val normalizedDensity = QuantityKernel.normalizedDensity(density)
        return NumericalResult(mean,fill,capacity,volume,normalizedDensity,volume.times(normalizedDensity))
    }
}

/** No invented readings, geometry, density, version provenance or Validated flag. Workflow owns those checks. */
object EmptyNumericalQuantities {
    val productVolumeM3: java.math.BigDecimal = java.math.BigDecimal.ZERO.setScale(6)
    val physicalQuantityKg: java.math.BigDecimal = java.math.BigDecimal.ZERO.setScale(6)
}
