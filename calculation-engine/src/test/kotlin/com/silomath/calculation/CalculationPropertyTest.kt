package com.silomath.calculation

import java.math.BigDecimal
import java.util.Random
import java.util.UUID
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class CalculationPropertyTest {
    private fun d(value: String)=BigDecimal(value)
    @Test fun explicitEmptyNumericalBranchNeverCallsNonEmptyRequirements() {
        val empty=NumericalKernel.quantitiesForExplicitMarkEmpty()
        assertEquals("0.000000",empty.productVolumeM3.toPlainString())
        assertEquals("0.000000",empty.physicalQuantityKg.toPlainString())
        // No optional dummy profile, density, reading, version or workflow-Validated field exists on this branch.
    }
    @Test fun deterministicSeededHeightsStayWithinAllFamilyCapacityBounds() {
        val random=Random(13002026L)
        for(vector in GeometryGoldenTest.vectors()) {
            val p=vector[1] as GeometryParameters;val max=FrozenGeometryKernel.maximumFillHeight(p)
            val capacity=FrozenGeometryKernel.maximumCapacity(p)
            val heights=List(100) { max.multiply(BigDecimal(random.nextInt(10001))).movePointLeft(4) }.sorted()
            var previous=FrozenGeometryKernel.volume(p,BigDecimal.ZERO)
            for(height in heights) {
                val next=FrozenGeometryKernel.volume(p,height)
                assertEquals(capacity.constant,next.constant)
                assertTrue(next.coefficient >= Rational.ZERO)
                assertTrue(next.coefficient >= previous.coefficient && next.coefficient <= capacity.coefficient)
                previous=next
            }
        }
    }
    @Test fun identicalRepeatedReadingsHaveExactMeanForEveryAllowedCount() {
        val id=UUID.fromString("11111111-1111-4111-8111-111111111111")
        val p=FrozenNumericalProfile(id,SquareFlat(d("2"),d("20")),"BOTTOM","datum",d("25"),d("20"))
        for(count in 1..5) {
            val readings=(1..count).map { UllageReading(it,id,"datum","BOTTOM",d("25"),d("7.123456"),"m","ULLAGE") }
            assertEquals("7.123456",MeasurementKernel.meanUllage(p,readings).toPersistenceDecimal().toPlainString())
            assertEquals("17.876544",MeasurementKernel.effectiveFillHeight(p,readings).toPersistenceDecimal().toPlainString())
        }
    }
    @Test fun invalidParametersAreRejectedForEveryFamilyWithoutRepairOrClamp() {
        val invalid=listOf<GeometryParameters>(CylTruncCone(d("1"),d("0"),d("0"),d("0")),
            CylFlat(d("1"),d("0")),SquareFlat(d("0"),d("1")),RectFlat(d("1"),d("-1"),d("1")),
            SquarePyramid(d("1"),d("-1"),d("1")),RectPyramid(d("1"),d("0"),d("1"),d("1")),
            RegularHex(d("0"),d("1")),
            VerifiedArea(GeometryFamily.STAR_VERIFIED_AREA_V1,d("0"),d("1"),"approved-test-source",true),
            VerifiedArea(GeometryFamily.POCKET_VERIFIED_AREA_V1,d("1"),d("0"),"approved-test-source",true),
            VerifiedArea(GeometryFamily.PERIMETER_VERIFIED_AREA_V1,d("-1"),d("1"),"approved-test-source",true))
        for(p in invalid) assertEquals(CalculationError.GEOM_INVALID_PARAMETER,
            assertThrows(CalculationFailure::class.java) { FrozenGeometryKernel.maximumCapacity(p) }.code)
    }
    @Test fun canonicalFingerprintEscapesTextWithoutLocaleOrDelimiterCollisions() {
        val id=UUID.fromString("11111111-1111-4111-8111-111111111111")
        val datum="reference\"\n\u03a9"
        val profile=FrozenNumericalProfile(id,SquareFlat(d("2"),d("20")),"BOTTOM",datum,d("20"),d("20"))
        val input=CanonicalCalculationInput(id,id,profile,listOf(UllageReading(1,id,datum,"BOTTOM",d("20"),d("5"),"m","ULLAGE")),
            ActualBulkDensity(d("764"),"kg/m\u00b3"),ApprovedCalculationContract.versions,"SQUARE_FLAT_V1")
        val material=CalculationFingerprint.canonicalMaterial(input)
        assertEquals(datum,Json.parseToJsonElement(material).jsonObject.getValue("datumReference").jsonPrimitive.content)
        val changed=input.copy(profile=profile.copy(datumReference="reference"),readings=input.readings.map { it.copy(datumReference="reference") })
        assertNotEquals(CalculationFingerprint.sha256(input),CalculationFingerprint.sha256(changed))
    }
}
