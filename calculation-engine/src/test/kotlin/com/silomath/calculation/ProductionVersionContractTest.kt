package com.silomath.calculation

import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import com.silomath.calculation.CalculationError.*

class ProductionVersionContractTest {
    private fun d(value: String)=BigDecimal(value)
    private fun id(n: String)=UUID.fromString(n)
    private fun input(): CanonicalCalculationInput {
        val profile=FrozenNumericalProfile(id("33333333-3333-4333-8333-333333333333"),CylFlat(d("10"),d("20")),
            "BOTTOM","test-frozen-datum",d("20"),d("20"))
        return CanonicalCalculationInput(id("11111111-1111-4111-8111-111111111111"),id("22222222-2222-4222-8222-222222222222"),profile,
            listOf(UllageReading(1,profile.geometryProfileVersionId,profile.datumReference,profile.geometryOrigin,d("20"),d("5"),"m","ULLAGE")),
            ActualBulkDensity(d("76.4"),"kg/hl"),ApprovedCalculationContract.versions,"CYL_FLAT_V1")
    }
    private fun fails(code: CalculationError,action: ()->Unit) { assertEquals(code,assertThrows(CalculationFailure::class.java) { action() }.code) }

    @Test fun exactTypedTupleMatchesApprovedBaselineArtifact() {
        val root=Path.of(System.getProperty("silomath.repoRoot"))
        val approved=Json.parseToJsonElement(Files.readString(root.resolve("engineering/baseline-change/CR-CALC-VERSION-CONTRACT-001.json"))).jsonObject
        val v=ApprovedCalculationContract.versions
        assertTrue(approved.getValue("engineVersion").jsonPrimitive.isString)
        assertEquals(approved.getValue("engineVersion").jsonPrimitive.content,v.engineVersion)
        for((key,value) in mapOf("formulaVersion" to v.formulaVersion,"inputSchemaVersion" to v.inputSchemaVersion,
            "normalizationVersion" to v.normalizationVersion,"aggregationRuleVersion" to v.aggregationRuleVersion)) {
            assertFalse(approved.getValue(key).jsonPrimitive.isString)
            assertEquals(approved.getValue(key).jsonPrimitive.int,value)
        }
        assertEquals("MEAN_ULLAGE",v.aggregationMethod)
        assertEquals("MEAN_ULLAGE_V1",ApprovedCalculationContract.combinedSemanticIdentifier)
        assertEquals("LEVEL_BASED_V1",v.surfaceModelVersion)
        assertEquals(approved.getValue("formulaCodes").jsonObject.values.map { it.jsonPrimitive.content }.toSet(),
            ProductionCalculationRegistry.registeredKeys().map { it.formulaCode }.toSet())
    }
    @Test fun approvedTupleDispatchesEveryFamilyToActualGoldenHandler() {
        assertEquals(10,ProductionCalculationRegistry.registeredKeys().size)
        for(vector in GeometryGoldenTest.vectors()) {
            val p=vector[1] as GeometryParameters;val h=vector[2] as BigDecimal
            val max=FrozenGeometryKernel.maximumFillHeight(p)
            val base=input();val profile=base.profile.copy(parameters=p,measurementReferenceHeightM=max,maximumFillHeightM=max)
            val source=base.copy(profile=profile,formulaCode=p.family.name,readings=base.readings.map { it.copy(rawValue=max-h,measurementReferenceHeightM=max) })
            val result=ProductionCalculationRegistry.calculate(source)
            assertEquals(vector[4],result.numerical.productVolumeM3.toPersistenceDecimal().toPlainString())
            assertEquals(vector[3],result.numerical.maxCapacityM3.toPersistenceDecimal().toPlainString())
            assertTrue(result.normalizedInputFingerprint.matches(Regex("[0-9a-f]{64}")))
        }
    }
    @Test fun everyUnregisteredTupleFieldAndMismatchedFormulaFailsClosed() {
        val source=input();val v=source.versions
        val unsupported=listOf(v.copy(engineVersion="unknown"),v.copy(formulaVersion=2),v.copy(inputSchemaVersion=2),
            v.copy(normalizationVersion=2),v.copy(aggregationRuleVersion=2),v.copy(aggregationMethod="MEAN_ULLAGE_V1"),v.copy(surfaceModelVersion="unknown"))
        for(version in unsupported) {
            val changed=source.copy(versions=version)
            assertFalse(ProductionCalculationRegistry.supports(CalculationHandlerKey(changed.profile.parameters.family,changed.formulaCode,version)))
            fails(GEOM_VERSION_UNSUPPORTED) { ProductionCalculationRegistry.calculate(changed) }
            fails(GEOM_VERSION_UNSUPPORTED) { CalculationFingerprint.sha256(changed) }
        }
        fails(GEOM_VERSION_UNSUPPORTED) { ProductionCalculationRegistry.calculate(source.copy(formulaCode="SQUARE_FLAT_V1")) }
        fails(GEOM_VERSION_UNSUPPORTED) { ProductionCalculationRegistry.calculate(source.copy(formulaCode="unknown")) }
    }
    @Test fun fingerprintMatchesIndependentCanonicalUtf8Reference() {
        val resource=javaClass.getResourceAsStream("/approved-calculation-fingerprint-reference.json")!!.bufferedReader().use { it.readText() }
        val reference=Json.parseToJsonElement(resource).jsonObject
        assertEquals(reference.getValue("canonicalMaterial").jsonPrimitive.content,CalculationFingerprint.canonicalMaterial(input()))
        assertEquals(reference.getValue("sha256").jsonPrimitive.content,CalculationFingerprint.sha256(input()))
        val json=Json.parseToJsonElement(CalculationFingerprint.canonicalMaterial(input())).jsonObject
        assertFalse(json.getValue("inputSchemaVersion").jsonPrimitive.isString)
        assertEquals(JsonNull,json.getValue("approvedAreaProvenance"))
        assertEquals(json.keys.sorted(),json.keys.toList())
    }
    @Test fun equivalentDecimalScalesAndLocaleDoNotChangeFingerprint() {
        val source=input();val expected=CalculationFingerprint.sha256(source)
        val previous=Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            val equivalent=source.copy(profile=source.profile.copy(parameters=CylFlat(d("10.000000"),d("20.0"))),
                density=ActualBulkDensity(d("76.400000"),"kg/hl"),readings=source.readings.map { it.copy(rawValue=d("5.000000")) })
            assertEquals(expected,CalculationFingerprint.sha256(equivalent))
        } finally { Locale.setDefault(previous) }
    }
    @Test fun everyCalculationAffectingInputChangesFingerprintAndStalesResult() {
        val source=input();val stored=ProductionCalculationRegistry.calculate(source)
        val changedProfile=source.profile.copy(parameters=CylFlat(d("12"),d("20")))
        val newProfileId=UUID.randomUUID()
        val candidates=listOf(source.copy(frozenContextId=UUID.randomUUID()),source.copy(geometryFamilyId=UUID.randomUUID()),
            source.copy(profile=changedProfile),source.copy(density=ActualBulkDensity(d("77"),"kg/hl")),
            source.copy(readings=source.readings.map { it.copy(rawValue=d("6")) }),
            source.copy(profile=source.profile.copy(datumReference="other"),readings=source.readings.map { it.copy(datumReference="other") }),
            source.copy(profile=source.profile.copy(geometryOrigin="other"),readings=source.readings.map { it.copy(geometryOrigin="other") }),
            source.copy(profile=source.profile.copy(measurementReferenceHeightM=d("21")),readings=source.readings.map { it.copy(measurementReferenceHeightM=d("21")) }),
            source.copy(profile=source.profile.copy(geometryProfileVersionId=newProfileId),readings=source.readings.map { it.copy(geometryProfileVersionId=newProfileId) }),
            source.copy(density=ActualBulkDensity(d("764"),"kg/m\u00b3")))
        for(candidate in candidates) {
            assertNotEquals(stored.normalizedInputFingerprint,CalculationFingerprint.sha256(candidate))
            assertTrue(StaleCalculationDetection.isStale(stored,candidate))
            fails(GEOM_STALE_RESULT) { StaleCalculationDetection.requireFresh(stored,candidate) }
        }
        StaleCalculationDetection.requireFresh(stored,source)
        assertFalse(StaleCalculationDetection.isStale(stored,source))
    }
    @Test fun orderedReadingsIncludeSequenceAndIgnoreCollectionIterationOrder() {
        val source=input();val a=source.readings.single().copy(rawValue=d("4"));val b=a.copy(sequenceNumber=2,rawValue=d("6"))
        val first=source.copy(readings=listOf(a,b));val reordered=first.copy(readings=listOf(b,a))
        assertEquals(CalculationFingerprint.sha256(first),CalculationFingerprint.sha256(reordered))
        val reassigned=first.copy(readings=listOf(a.copy(sequenceNumber=2),b.copy(sequenceNumber=1)))
        assertNotEquals(CalculationFingerprint.sha256(first),CalculationFingerprint.sha256(reassigned))
    }
    @Test fun historicalReproductionUsesFrozenInputsAndNeverFallsBack() {
        val source=input();val stored=ProductionCalculationRegistry.calculate(source)
        val originalVolume=stored.numerical.productVolumeM3.toPersistenceDecimal()
        ProductionCalculationRegistry.calculate(source.copy(profile=source.profile.copy(parameters=CylFlat(d("12"),d("20")))))
        val reproduced=ProductionCalculationRegistry.reproduce(stored.frozenInput)
        assertEquals(stored.normalizedInputFingerprint,reproduced.normalizedInputFingerprint)
        assertEquals(originalVolume,reproduced.numerical.productVolumeM3.toPersistenceDecimal())
        val unsupported=source.copy(versions=source.versions.copy(engineVersion="unregistered-history"))
        fails(GEOM_VERSION_UNSUPPORTED) { ProductionCalculationRegistry.reproduce(unsupported) }
        fails(GEOM_VERSION_UNSUPPORTED) { StaleCalculationDetection.requireFresh(stored,unsupported) }
        assertTrue(StaleCalculationDetection.isStale(stored,unsupported))
    }
    @Test fun capturedHistoryCannotBeChangedThroughCallerListOrRegistryKeySet() {
        val original=input();val mutable=original.readings.toMutableList();val source=original.copy(readings=mutable)
        val stored=ProductionCalculationRegistry.calculate(source);mutable.clear()
        assertEquals(1,stored.frozenInput.readings.size)
        assertThrows(UnsupportedOperationException::class.java) { (stored.frozenInput.readings as MutableList).clear() }
        val keys=ProductionCalculationRegistry.registeredKeys().toMutableSet();keys.clear()
        assertEquals(10,ProductionCalculationRegistry.registeredKeys().size)
    }
}
