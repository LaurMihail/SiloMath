package com.silomath.calculation

import java.math.BigDecimal
import java.util.UUID
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import com.silomath.calculation.CalculationError.*

class MeasurementAndPrecisionTest {
    private fun d(value: String)=BigDecimal(value)
    private val id=UUID.fromString("11111111-1111-4111-8111-111111111111")
    private val profile=FrozenNumericalProfile(id,RectFlat(d("3"),d("1"),d("1")),"BOTTOM","test-frozen-datum",d("1"),d("1"))
    private fun readings(vararg values: String)=values.mapIndexed { i,v ->
        UllageReading(i+1,id,profile.datumReference,profile.geometryOrigin,profile.measurementReferenceHeightM,d(v),"m","ULLAGE")
    }
    private fun fails(code: CalculationError,action: ()->Unit) { assertEquals(code,assertThrows(CalculationFailure::class.java) { action() }.code) }

    @Test fun numericalPipelineConsumesCanonicalInputsWithoutSnapshotBuilderOrVersionClaims() {
        val result=NumericalKernel.evaluate(profile,readings("0","0","1"),ActualBulkDensity(d("76.4"),"kg/hl"))
        assertEquals("2.000000",result.productVolumeM3.toPersistenceDecimal().toPlainString())
        assertEquals("1528.000000",result.physicalQuantityKg.toPersistenceDecimal().toPlainString())
        assertEquals("3.000000",result.maxCapacityM3.toPersistenceDecimal().toPlainString())
        assertEquals(0,d("764").compareTo(result.normalizedDensityKgM3))
        fails(GEOM_DENSITY_REQUIRED) { NumericalKernel.evaluate(profile,readings("0"),null) }
    }
    @Test fun repeatingMeanAndFillRemainExactUntilGeometryPersistence() {
        val source=readings("0","0","1")
        val mean=MeasurementKernel.meanUllage(profile,source)
        assertEquals("0.333333",mean.toPersistenceDecimal().toPlainString())
        val fill=MeasurementKernel.effectiveFillHeight(profile,source)
        assertEquals("0.666667",fill.toPersistenceDecimal().toPlainString())
        // Premature scale-six fill would produce 2.000001; real rational fill yields exactly 2.
        assertEquals("2.000000",FrozenGeometryKernel.volume(profile.parameters,fill.coefficient).toPersistenceDecimal().toPlainString())
    }
    @Test fun officialMeanVectorAndReferenceAboveFillMaximum() {
        val p=profile.copy(parameters=SquareFlat(d("1"),d("16")),measurementReferenceHeightM=d("18"),maximumFillHeightM=d("16"))
        val r=readings("3","4","5").map { it.copy(measurementReferenceHeightM=d("18")) }
        assertEquals("4.000000",MeasurementKernel.meanUllage(p,r).toPersistenceDecimal().toPlainString())
        assertEquals("14.000000",MeasurementKernel.effectiveFillHeight(p,r).toPersistenceDecimal().toPlainString())
    }
    @Test fun everyReadingIsValidatedAndNoSubsetOrClampIsAllowed() {
        fails(GEOM_MEASUREMENT_COUNT) { MeasurementKernel.normalize(emptyList(),profile) }
        fails(GEOM_MEASUREMENT_COUNT) { MeasurementKernel.normalize(readings("0","0","0","0","0","0"),profile) }
        fails(GEOM_INVALID_ULLAGE) { MeasurementKernel.normalize(readings("0","-0.1","0"),profile) }
        fails(GEOM_INVALID_ULLAGE) { MeasurementKernel.normalize(readings("0","1.000001"),profile) }
        val highRef=profile.copy(measurementReferenceHeightM=d("2"))
        fails(GEOM_FILL_OUT_OF_RANGE) { MeasurementKernel.effectiveFillHeight(highRef,readings("0").map { it.copy(measurementReferenceHeightM=d("2")) }) }
    }
    @Test fun datumProfileMeasurementTypeAndSequenceAreExplicit() {
        val r=readings("0","1")
        fails(GEOM_MIXED_DATUM) { MeasurementKernel.normalize(r.mapIndexed { i,x -> if(i==0) x.copy(datumReference="other") else x },profile) }
        fails(GEOM_MIXED_DATUM) { MeasurementKernel.normalize(r.map { it.copy(geometryOrigin="OTHER") },profile) }
        fails(GEOM_MIXED_DATUM) { MeasurementKernel.normalize(r.map { it.copy(measurementReferenceHeightM=d("2")) },profile) }
        fails(GEOM_MIXED_PROFILE_VERSION) { MeasurementKernel.normalize(r.map { it.copy(geometryProfileVersionId=UUID.randomUUID()) },profile) }
        fails(GEOM_UNSUPPORTED_MEASUREMENT_TYPE) { MeasurementKernel.normalize(r.map { it.copy(measurementType="DEPTH") },profile) }
        fails(GEOM_INVALID_PARAMETER) { MeasurementKernel.normalize(r.map { it.copy(rawUnit="unspecified") },profile) }
        fails(GEOM_INVALID_PARAMETER) { MeasurementKernel.normalize(r.map { it.copy(sequenceNumber=1) },profile) }
        fails(GEOM_INVALID_PARAMETER) { MeasurementKernel.normalize(r.map { it.copy(sequenceNumber=6) },profile) }
        // Doc10 sequence 1..5 is unique, not required to be contiguous after removal.
        assertEquals(listOf(d("0"),d("1")),MeasurementKernel.normalize(listOf(r[1].copy(sequenceNumber=5),r[0]),profile))
    }
    @Test fun geometryInvalidParametersAndVerifiedAreaGuards() {
        fails(GEOM_INVALID_PARAMETER) { FrozenGeometryKernel.maximumCapacity(CylFlat(d("0"),d("1"))) }
        fails(GEOM_INVALID_PARAMETER) { FrozenGeometryKernel.maximumCapacity(CylTruncCone(d("2"),d("-1"),d("1"),d("0"))) }
        fails(GEOM_OUTLET_GE_DIAMETER) { FrozenGeometryKernel.maximumCapacity(CylTruncCone(d("2"),d("1"),d("1"),d("2"))) }
        for(family in listOf(GeometryFamily.STAR_VERIFIED_AREA_V1,GeometryFamily.POCKET_VERIFIED_AREA_V1,GeometryFamily.PERIMETER_VERIFIED_AREA_V1)) {
            fails(GEOM_AREA_PROVENANCE_REQUIRED) { FrozenGeometryKernel.maximumCapacity(VerifiedArea(family,d("1"),d("1"),null,true)) }
            fails(GEOM_VARIABLE_SECTION_NOT_SUPPORTED) { FrozenGeometryKernel.maximumCapacity(VerifiedArea(family,d("1"),d("1"),"approved-test-source",false)) }
        }
    }
    @Test fun hopperBoundariesAreContinuousAndApexAndZeroBodyAreAllowed() {
        val cases=listOf<GeometryParameters>(CylTruncCone(d("18"),d("15"),d("6"),d("0.45")),SquarePyramid(d("6"),d("14"),d("6")),RectPyramid(d("8"),d("6"),d("12"),d("6")))
        for(p in cases) {
            val before=FrozenGeometryKernel.volume(p,d("5.999999999"))
            val at=FrozenGeometryKernel.volume(p,d("6"))
            val after=FrozenGeometryKernel.volume(p,d("6.000000001"))
            assertTrue(before.coefficient <= at.coefficient && at.coefficient <= after.coefficient)
            val innerBefore=FrozenGeometryKernel.volume(p,d("5.9999999999"))
            val innerAfter=FrozenGeometryKernel.volume(p,d("6.0000000001"))
            // Convergence is checked on exact coefficients, not coincidental six-place rounded buckets.
            assertTrue(innerAfter.coefficient-innerBefore.coefficient <
                (after.coefficient-before.coefficient)/Rational.integer(2))
        }
        assertEquals("3.141593",FrozenGeometryKernel.maximumCapacity(CylTruncCone(d("2"),d("0"),d("3"),d("0"))).toPersistenceDecimal().toPlainString())
        assertEquals("72.000000",FrozenGeometryKernel.maximumCapacity(SquarePyramid(d("6"),d("0"),d("6"))).toPersistenceDecimal().toPlainString())
    }
    @Test fun quantityUsesFullUnroundedVolumeAndEquivalentDensityUnits() {
        val volume=FrozenGeometryKernel.volume(CylTruncCone(d("18"),d("15"),d("6"),d("0.45")),d("17"))
        val first=QuantityKernel.physicalQuantity(volume,ActualBulkDensity(d("76.4"),"kg/hl"))
        val second=QuantityKernel.physicalQuantity(volume,ActualBulkDensity(d("764"),"kg/m\u00b3"))
        assertEquals("2537349.890960",first.toPersistenceDecimal().toPlainString())
        assertEquals(first.toPersistenceDecimal(),second.toPersistenceDecimal())
        assertEquals("2537349.891164",volume.toPersistenceDecimal().multiply(d("764")).toPlainString())
        fails(GEOM_DENSITY_REQUIRED) { QuantityKernel.physicalQuantity(volume,null) }
        fails(GEOM_INVALID_DENSITY) { QuantityKernel.physicalQuantity(volume,ActualBulkDensity(d("0"),"kg/hl")) }
        fails(GEOM_INVALID_DENSITY) { QuantityKernel.physicalQuantity(volume,ActualBulkDensity(d("1"),"unknown")) }
        fails(GEOM_INVALID_PARAMETER) { QuantityKernel.physicalQuantity(ExactValue.fromCanonicalDecimal(d("-1")),ActualBulkDensity(d("1"),"kg/hl")) }
    }
    @Test fun persistenceRoundingOnlyIsHalfUpAndLocaleIndependent() {
        val previous=Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ro-RO"))
            assertEquals("1.234568",ExactValue.fromCanonicalDecimal(d("1.2345675")).toPersistenceDecimal().toPlainString())
            assertEquals("-1.234568",ExactValue.fromCanonicalDecimal(d("-1.2345675")).toPersistenceDecimal().toPlainString())
            repeat(100) { assertEquals("3.141593",FrozenGeometryKernel.volume(CylFlat(d("2"),d("1")),d("1")).toPersistenceDecimal().toPlainString()) }
            assertThrows(IllegalArgumentException::class.java) { ExactValue.fromCanonicalDecimal(d("100000000000000")).toPersistenceDecimal() }
        } finally { Locale.setDefault(previous) }
    }
}
