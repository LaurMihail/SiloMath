package com.silomath.backend.calculation

import com.silomath.calculation.*
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/** Real backend verifier + same pure Kotlin client engine, against independent frozen vectors/hash. */
class CalculationParityTest {
    private fun d(value: String)=BigDecimal(value)
    private val verifier=AuthoritativeCalculationVerifier()
    private fun input(parameters: GeometryParameters=CylFlat(d("10"),d("20")),fill: BigDecimal=d("15")): CanonicalCalculationInput {
        val profileId=UUID.fromString("33333333-3333-4333-8333-333333333333")
        val max=FrozenGeometryKernel.maximumFillHeight(parameters)
        val profile=FrozenNumericalProfile(profileId,parameters,"BOTTOM","test-frozen-datum",max,max)
        return CanonicalCalculationInput(UUID.fromString("11111111-1111-4111-8111-111111111111"),
            UUID.fromString("22222222-2222-4222-8222-222222222222"),profile,
            listOf(UllageReading(1,profileId,profile.datumReference,profile.geometryOrigin,max,max-fill,"m","ULLAGE")),
            ActualBulkDensity(d("76.4"),"kg/hl"),ApprovedCalculationContract.versions,parameters.family.name)
    }
    private fun fails(code: CalculationError,action: ()->Unit) { assertEquals(code,assertThrows(CalculationFailure::class.java) { action() }.code) }
    @Test fun backendMatchesClientCanonicalHashAndNormativeVolume() {
        val source=input();val client=ProductionCalculationRegistry.calculate(source)
        val root=Path.of(System.getProperty("silomath.repoRoot"))
        val reference=Json.parseToJsonElement(Files.readString(root.resolve("calculation-engine/src/test/resources/approved-calculation-fingerprint-reference.json"))).jsonObject
        assertEquals(reference.getValue("sha256").jsonPrimitive.content,client.normalizedInputFingerprint)
        val server=verifier.verify(source,client.normalizedInputFingerprint,client.numerical.productVolumeM3.toPersistenceDecimal())
        assertEquals("1178.097245",server.numerical.productVolumeM3.toPersistenceDecimal().toPlainString())
        assertEquals(client.normalizedInputFingerprint,server.normalizedInputFingerprint)
        assertEquals(client.numerical.physicalQuantityKg.toPersistenceDecimal(),server.numerical.physicalQuantityKg.toPersistenceDecimal())
    }
    @Test fun allTenFrozenFamiliesReproduceOnClientAndServer() {
        val cases=listOf(
            Triple(CylTruncCone(d("18"),d("15"),d("6"),d("0.45")),d("17"),"3321.138601"),
            Triple(CylFlat(d("10"),d("20")),d("15"),"1178.097245"),
            Triple(SquareFlat(d("6"),d("20")),d("18"),"648.000000"),
            Triple(RectFlat(d("8"),d("6"),d("24")),d("20"),"960.000000"),
            Triple(SquarePyramid(d("6"),d("14"),d("6")),d("3"),"9.000000"),
            Triple(RectPyramid(d("8"),d("6"),d("12"),d("6")),d("9"),"240.000000"),
            Triple(RegularHex(d("4"),d("20")),d("12"),"498.830633"),
            Triple(VerifiedArea(GeometryFamily.STAR_VERIFIED_AREA_V1,d("30"),d("20"),"approved-test-drawing",true),d("16"),"480.000000"),
            Triple(VerifiedArea(GeometryFamily.POCKET_VERIFIED_AREA_V1,d("25"),d("18"),"approved-test-survey",true),d("14"),"350.000000"),
            Triple(VerifiedArea(GeometryFamily.PERIMETER_VERIFIED_AREA_V1,d("42.5"),d("22"),"approved-test-as-built",true),d("19.75"),"839.375000")
        )
        for((parameters,height,expected) in cases) {
            val source=input(parameters,height);val client=ProductionCalculationRegistry.calculate(source)
            val server=verifier.verify(source,client.normalizedInputFingerprint,client.numerical.productVolumeM3.toPersistenceDecimal())
            assertEquals(expected,server.numerical.productVolumeM3.toPersistenceDecimal().toPlainString())
            assertEquals(client.normalizedInputFingerprint,server.normalizedInputFingerprint)
        }
    }
    @Test fun verificationToleranceAcceptsOnlyTheFrozenInclusiveBoundary() {
        val source=input();val client=ProductionCalculationRegistry.calculate(source);val volume=client.numerical.productVolumeM3.toPersistenceDecimal()
        for(delta in listOf(d("-0.001"),d("0"),d("0.001"))) verifier.verify(source,client.normalizedInputFingerprint,volume+delta)
        for(delta in listOf(d("-0.001001"),d("0.001001"))) fails(CalculationError.GEOM_CALCULATION_MISMATCH) {
            verifier.verify(source,client.normalizedInputFingerprint,volume+delta)
        }
        fails(CalculationError.GEOM_INVALID_PARAMETER) { verifier.verify(source,client.normalizedInputFingerprint,d("-0.000001")) }
    }
    @Test fun staleOrUnregisteredVersionCannotBeAcceptedThroughToleranceOrFallback() {
        val source=input();val client=ProductionCalculationRegistry.calculate(source);val volume=client.numerical.productVolumeM3.toPersistenceDecimal()
        fails(CalculationError.GEOM_STALE_RESULT) { verifier.verify(source,"0".repeat(64),volume) }
        fails(CalculationError.GEOM_VERSION_UNSUPPORTED) { verifier.verify(source.copy(versions=source.versions.copy(engineVersion="unregistered-history")),client.normalizedInputFingerprint,volume) }
        fails(CalculationError.GEOM_INVALID_ULLAGE) { verifier.verify(source.copy(readings=source.readings.map { it.copy(rawValue=d("-1")) }),client.normalizedInputFingerprint,volume) }
        assertEquals(client.normalizedInputFingerprint,ProductionCalculationRegistry.reproduce(client.frozenInput).normalizedInputFingerprint)
    }
}
