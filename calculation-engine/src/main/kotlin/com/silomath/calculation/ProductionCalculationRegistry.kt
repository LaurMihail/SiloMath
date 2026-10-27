package com.silomath.calculation

import java.util.Collections

/** CR-CALC-VERSION-CONTRACT-001 / FD-73. These identifiers are user-approved, not inferred. */
object ApprovedCalculationContract {
    val versions = PinnedCalculationVersions(
        formulaVersion=1, inputSchemaVersion=1, engineVersion="2.0", normalizationVersion=1,
        surfaceModelVersion="LEVEL_BASED_V1", aggregationMethod="MEAN_ULLAGE", aggregationRuleVersion=1
    )
    const val combinedSemanticIdentifier = "MEAN_ULLAGE_V1"
}
data class CalculationHandlerKey(val family: GeometryFamily, val formulaCode: String, val versions: PinnedCalculationVersions)

/** Retains the frozen numerical input, not a mutable master-data lookup or a validated workflow assertion. */
class VersionedCalculationResult internal constructor(
    val frozenInput: CanonicalCalculationInput,
    val numerical: NumericalResult,
    val normalizedInputFingerprint: String
)

/** Exact tuple -> executable handler. No wildcard, coercion, alias or latest-version fallback. */
object ProductionCalculationRegistry {
    private class Doc13Version2Handler(private val family: GeometryFamily) {
        fun execute(input: CanonicalCalculationInput): NumericalResult {
            checkCalculation(input.profile.parameters.family == family,CalculationError.GEOM_VERSION_UNSUPPORTED)
            return NumericalKernel.evaluate(input.profile,input.readings,input.density)
        }
    }
    private val handlers: Map<CalculationHandlerKey,Doc13Version2Handler> = GeometryFamily.entries.associate { family ->
        CalculationHandlerKey(family,family.name,ApprovedCalculationContract.versions) to Doc13Version2Handler(family)
    }
    fun registeredKeys(): Set<CalculationHandlerKey> = handlers.keys.toSet()
    fun supports(key: CalculationHandlerKey): Boolean = handlers.containsKey(key)
    internal fun requireSupported(input: CanonicalCalculationInput) {
        resolve(input)
    }
    private fun resolve(input: CanonicalCalculationInput): Doc13Version2Handler =
        handlers[CalculationHandlerKey(input.profile.parameters.family,input.formulaCode,input.versions)]
            ?: throw CalculationFailure(CalculationError.GEOM_VERSION_UNSUPPORTED)

    fun calculate(input: CanonicalCalculationInput): VersionedCalculationResult {
        val captured = input.copy(readings=Collections.unmodifiableList(input.readings.toList()))
        val handler = resolve(captured)
        val numerical = handler.execute(captured)
        return VersionedCalculationResult(captured,numerical,CalculationFingerprint.sha256(captured))
    }
    /** Reproduction resolves the stored tuple and stored inputs; it never substitutes current master data. */
    fun reproduce(frozenInput: CanonicalCalculationInput): VersionedCalculationResult = calculate(frozenInput)
}
