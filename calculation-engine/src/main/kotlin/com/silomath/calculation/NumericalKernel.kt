package com.silomath.calculation

/**
 * Standalone numerical pipeline for canonical frozen inputs. Does not issue a version-certified
 * CalculationResult, persist state, validate a revision, or assert CampaignSnapshot compatibility.
 * Production binding requires the separately approved release/handler version contract.
 */
object NumericalKernel {
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
