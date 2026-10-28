package com.silomath.calculation

import java.math.BigDecimal
import java.security.MessageDigest
import kotlinx.serialization.json.*

/** Canonical JSON for the approved schema1: sorted fields, explicit nulls, decimal TEXT, UTF-8 SHA-256. */
object CalculationFingerprint {
    fun sha256(input: CanonicalCalculationInput): String = MessageDigest.getInstance("SHA-256")
        .digest(canonicalMaterial(input).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun canonicalMaterial(input: CanonicalCalculationInput): String {
        ProductionCalculationRegistry.requireSupported(input)
        val readings = MeasurementKernel.normalize(input.readings,input.profile)
        val normalizedDensity = QuantityKernel.normalizedDensity(input.density)
        val density = requireNotNull(input.density)
        val version = input.versions
        val parameters = input.profile.parameters
        val material = buildJsonObject {
            put("frozenContextId",input.frozenContextId.toString())
            put("geometryFamilyId",input.geometryFamilyId.toString())
            put("geometryProfileVersionId",input.profile.geometryProfileVersionId.toString())
            put("geometryFamily",parameters.family.name)
            put("formulaCode",input.formulaCode)
            put("formulaVersion",version.formulaVersion)
            put("inputSchemaVersion",version.inputSchemaVersion)
            put("engineVersion",version.engineVersion)
            put("normalizationVersion",version.normalizationVersion)
            put("surfaceModelVersion",version.surfaceModelVersion)
            put("aggregationMethod",version.aggregationMethod)
            put("aggregationRuleVersion",version.aggregationRuleVersion)
            put("measurementType","ULLAGE")
            put("geometryOrigin",input.profile.geometryOrigin)
            put("datumReference",input.profile.datumReference)
            put("measurementReferenceHeightM",decimal(input.profile.measurementReferenceHeightM))
            put("maximumFillHeightM",decimal(input.profile.maximumFillHeightM))
            put("fixedParameters",dimensions(parameters))
            put("approvedAreaProvenance",if(parameters is VerifiedArea) JsonPrimitive(parameters.approvedProvenanceReference) else JsonNull)
            put("hasConstantCrossSection",if(parameters is VerifiedArea) JsonPrimitive(parameters.hasConstantCrossSection) else JsonNull)
            put("orderedNormalizedReadings",buildJsonArray {
                input.readings.sortedBy { it.sequenceNumber }.zip(readings).forEach { (reading,normalized) ->
                    add(buildJsonObject { put("sequenceNumber",reading.sequenceNumber);put("ullageM",decimal(normalized)) })
                }
            })
            put("enteredDensityValue",decimal(density.value))
            put("enteredDensityUnit",density.unit)
            put("normalizedDensityKgM3",decimal(normalizedDensity))
        }
        return sorted(material).toString()
    }
    private fun decimal(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
    private fun dimensions(p: GeometryParameters): JsonObject {
        val values: Map<String,BigDecimal> = when(p) {
            is CylTruncCone -> mapOf("diameterM" to p.diameterM,"cylinderHeightM" to p.cylinderHeightM,"hopperHeightM" to p.hopperHeightM,"outletDiameterM" to p.outletDiameterM)
            is CylFlat -> mapOf("diameterM" to p.diameterM,"heightM" to p.heightM)
            is SquareFlat -> mapOf("sideM" to p.sideM,"heightM" to p.heightM)
            is RectFlat -> mapOf("lengthM" to p.lengthM,"widthM" to p.widthM,"heightM" to p.heightM)
            is SquarePyramid -> mapOf("sideM" to p.sideM,"bodyHeightM" to p.bodyHeightM,"hopperHeightM" to p.hopperHeightM)
            is RectPyramid -> mapOf("lengthM" to p.lengthM,"widthM" to p.widthM,"bodyHeightM" to p.bodyHeightM,"hopperHeightM" to p.hopperHeightM)
            is RegularHex -> mapOf("sideM" to p.sideM,"heightM" to p.heightM)
            is VerifiedArea -> mapOf("areaM2" to p.areaM2,"heightM" to p.heightM)
        }
        return JsonObject(values.mapValues { JsonPrimitive(decimal(it.value)) })
    }
    private fun sorted(value: JsonElement): JsonElement = when(value) {
        is JsonObject -> JsonObject(value.toSortedMap().mapValues { sorted(it.value) })
        is JsonArray -> JsonArray(value.map(::sorted))
        else -> value
    }
}
object StaleCalculationDetection {
    fun isStale(stored: VersionedCalculationResult,currentInput: CanonicalCalculationInput): Boolean =
        try { stored.normalizedInputFingerprint != CalculationFingerprint.sha256(currentInput) }
        catch(_: CalculationFailure) { true }
    fun requireFresh(stored: VersionedCalculationResult,currentInput: CanonicalCalculationInput) {
        // Unsupported versions keep their specific failure; they cannot become fresh through fallback.
        val actual = CalculationFingerprint.sha256(currentInput)
        checkCalculation(actual == stored.normalizedInputFingerprint,CalculationError.GEOM_STALE_RESULT)
    }
}
