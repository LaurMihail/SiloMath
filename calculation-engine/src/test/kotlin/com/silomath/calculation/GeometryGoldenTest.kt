package com.silomath.calculation

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Exact expected values from Doc13 CE-G01..CE-G10; no synthetic version metadata. */
@RunWith(Parameterized::class)
class GeometryGoldenTest(private val vector: String, private val parameters: GeometryParameters,
    private val fill: BigDecimal, private val maximum: String, private val current: String) {
    @Test fun goldenMaximum() { assertEquals(maximum,FrozenGeometryKernel.maximumCapacity(parameters).toPersistenceDecimal().toPlainString()) }
    @Test fun goldenCurrent() { assertEquals(current,FrozenGeometryKernel.volume(parameters,fill).toPersistenceDecimal().toPlainString()) }
    @Test fun zeroAndFullBoundaries() {
        assertEquals("0.000000",FrozenGeometryKernel.volume(parameters,BigDecimal.ZERO).toPersistenceDecimal().toPlainString())
        assertEquals(maximum,FrozenGeometryKernel.volume(parameters,FrozenGeometryKernel.maximumFillHeight(parameters)).toPersistenceDecimal().toPlainString())
    }
    @Test fun monotonicWithoutIntermediateRoundingAndRejectOutOfRange() {
        val max=FrozenGeometryKernel.maximumFillHeight(parameters)
        var previous=FrozenGeometryKernel.volume(parameters,BigDecimal.ZERO)
        for(i in 1..100) {
            val height=max.multiply(BigDecimal(i)).movePointLeft(2)
            val next=FrozenGeometryKernel.volume(parameters,height)
            assertEquals(previous.constant,next.constant)
            assertTrue(next.coefficient >= previous.coefficient)
            previous=next
        }
        assertEquals(CalculationError.GEOM_FILL_OUT_OF_RANGE,
            assertThrows(CalculationFailure::class.java) { FrozenGeometryKernel.volume(parameters,BigDecimal("-0.000001")) }.code)
        assertEquals(CalculationError.GEOM_FILL_OUT_OF_RANGE,
            assertThrows(CalculationFailure::class.java) { FrozenGeometryKernel.volume(parameters,max+BigDecimal("0.000001")) }.code)
    }
    companion object {
        private fun d(value: String)=BigDecimal(value)
        @JvmStatic @Parameterized.Parameters(name="{0}") fun vectors(): List<Array<Any>> = listOf(
            arrayOf("CE-G01",CylTruncCone(d("18"),d("15"),d("6"),d("0.45")),d("17"),"4339.014620","3321.138601"),
            arrayOf("CE-G02",CylFlat(d("10"),d("20")),d("15"),"1570.796327","1178.097245"),
            arrayOf("CE-G03",SquareFlat(d("6"),d("20")),d("18"),"720.000000","648.000000"),
            arrayOf("CE-G04",RectFlat(d("8"),d("6"),d("24")),d("20"),"1152.000000","960.000000"),
            arrayOf("CE-G05",SquarePyramid(d("6"),d("14"),d("6")),d("3"),"576.000000","9.000000"),
            arrayOf("CE-G06",RectPyramid(d("8"),d("6"),d("12"),d("6")),d("9"),"672.000000","240.000000"),
            arrayOf("CE-G07",RegularHex(d("4"),d("20")),d("12"),"831.384388","498.830633"),
            arrayOf("CE-G08",VerifiedArea(GeometryFamily.STAR_VERIFIED_AREA_V1,d("30"),d("20"),"approved-test-drawing",true),d("16"),"600.000000","480.000000"),
            arrayOf("CE-G09",VerifiedArea(GeometryFamily.POCKET_VERIFIED_AREA_V1,d("25"),d("18"),"approved-test-survey",true),d("14"),"450.000000","350.000000"),
            arrayOf("CE-G10",VerifiedArea(GeometryFamily.PERIMETER_VERIFIED_AREA_V1,d("42.5"),d("22"),"approved-test-as-built",true),d("19.75"),"935.000000","839.375000")
        )
    }
}
