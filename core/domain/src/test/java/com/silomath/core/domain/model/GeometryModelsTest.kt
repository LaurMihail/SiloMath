package com.silomath.core.domain.model

import com.silomath.core.model.CanonicalDecimal
import com.silomath.core.model.GeometryFamilyId
import com.silomath.core.model.GeometryProfileId
import com.silomath.core.model.GeometryProfileVersionId
import com.silomath.core.model.OrganizationId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GeometryModelsTest {

    private val organizationId =
        OrganizationId("11111111-1111-4111-8111-111111111111")

    private val geometryFamilyId =
        GeometryFamilyId("22222222-2222-4222-8222-222222222222")

    private val geometryProfileId =
        GeometryProfileId("33333333-3333-4333-8333-333333333333")

    private val geometryProfileVersionId =
        GeometryProfileVersionId("44444444-4444-4444-8444-444444444444")

    private val instant =
        Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `geometry family registry contains exactly frozen F01 to F10`() {
        val families = GeometryFamilyCode.entries

        assertEquals(10, families.size)
        assertEquals(
            listOf(
                "F01", "F02", "F03", "F04", "F05",
                "F06", "F07", "F08", "F09", "F10"
            ),
            families.map { it.sequenceCode }
        )
    }

    @Test
    fun `geometry family accepts platform governed metadata`() {
        val family = GeometryFamily(
            geometryFamilyId = geometryFamilyId,
            familyCode = GeometryFamilyCode.CYL_FLAT_V1,
            displayName = "Cylindrical flat bottom",
            formulaVersion = "V1",
            parameterContract = "D,H"
        )

        assertEquals(
            GeometryFamilyCode.CYL_FLAT_V1,
            family.familyCode
        )
    }

    @Test
    fun `geometry family rejects blank or untrimmed metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            GeometryFamily(
                geometryFamilyId = geometryFamilyId,
                familyCode = GeometryFamilyCode.CYL_FLAT_V1,
                displayName = " ",
                formulaVersion = "V1",
                parameterContract = "D,H"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            GeometryFamily(
                geometryFamilyId = geometryFamilyId,
                familyCode = GeometryFamilyCode.CYL_FLAT_V1,
                displayName = " Cylindrical flat bottom ",
                formulaVersion = "V1",
                parameterContract = "D,H"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            GeometryFamily(
                geometryFamilyId = geometryFamilyId,
                familyCode = GeometryFamilyCode.CYL_FLAT_V1,
                displayName = "Cylindrical flat bottom",
                formulaVersion = " ",
                parameterContract = "D,H"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            GeometryFamily(
                geometryFamilyId = geometryFamilyId,
                familyCode = GeometryFamilyCode.CYL_FLAT_V1,
                displayName = "Cylindrical flat bottom",
                formulaVersion = "V1",
                parameterContract = " "
            )
        }
    }

    @Test
    fun `geometry profile accepts canonical logical master`() {
        val profile = geometryProfile()

        assertEquals("PROFILE1", profile.code)
        assertEquals(
            geometryProfileVersionId,
            profile.currentVersionId
        )
    }

    @Test
    fun `geometry profile rejects non canonical code`() {
        assertThrows(IllegalArgumentException::class.java) {
            geometryProfile(code = " profile1 ")
        }
    }

    @Test
    fun `geometry profile rejects blank or overlong name`() {
        assertThrows(IllegalArgumentException::class.java) {
            geometryProfile(name = "   ")
        }

        assertThrows(IllegalArgumentException::class.java) {
            geometryProfile(name = "A".repeat(151))
        }
    }

    @Test
    fun `geometry profile rejects zero entity version`() {
        assertThrows(IllegalArgumentException::class.java) {
            geometryProfile(entityVersion = 0L)
        }
    }

    @Test
    fun `formula family rejects verified area semantics`() {
        assertThrows(IllegalArgumentException::class.java) {
            formulaVersion(
                verifiedAreaM2 = decimal("100.000000")
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            formulaVersion(
                areaProvenance =
                    AreaProvenance("Certified drawing A-101")
            )
        }
    }

    @Test
    fun `verified area family requires positive area and provenance`() {
        assertThrows(IllegalArgumentException::class.java) {
            verifiedAreaVersion(verifiedAreaM2 = null)
        }

        assertThrows(IllegalArgumentException::class.java) {
            verifiedAreaVersion(areaProvenance = null)
        }

        assertThrows(IllegalArgumentException::class.java) {
            verifiedAreaVersion(
                verifiedAreaM2 = decimal("0.000000")
            )
        }
    }

    @Test
    fun `verified area family accepts frozen area semantics`() {
        val version = verifiedAreaVersion()

        assertEquals(
            decimal("100.000000"),
            version.verifiedAreaM2
        )
        assertEquals(
            "Certified drawing A-101",
            version.areaProvenance?.value
        )
    }

    @Test
    fun `formula family keeps verified area fields absent`() {
        val version = formulaVersion()

        assertNull(version.verifiedAreaM2)
        assertNull(version.areaProvenance)
    }

    @Test
    fun `geometry profile version rejects invalid common values`() {
        assertThrows(IllegalArgumentException::class.java) {
            formulaVersion(versionNumber = 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            formulaVersion(
                measurementReferenceHeightM =
                    decimal("0.000000")
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            formulaVersion(
                maximumFillHeightM =
                    decimal("0.000000")
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            formulaVersion(
                maxCapacityM3 =
                    decimal("0.000000")
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            formulaVersion(schemaVersion = " ")
        }
    }

    @Test
    fun `geometry origin and provenance reject blank or untrimmed text`() {
        assertThrows(IllegalArgumentException::class.java) {
            GeometryOrigin(" ")
        }

        assertThrows(IllegalArgumentException::class.java) {
            GeometryOrigin(" bottom ")
        }

        assertThrows(IllegalArgumentException::class.java) {
            AreaProvenance(" ")
        }

        assertThrows(IllegalArgumentException::class.java) {
            AreaProvenance(" drawing ")
        }
    }

    @Test
    fun `geometry family id requires canonical uuid`() {
        assertThrows(IllegalArgumentException::class.java) {
            GeometryFamilyId("not-a-uuid")
        }

        assertThrows(IllegalArgumentException::class.java) {
            GeometryFamilyId(
                "22222222-2222-4222-8222-22222222222A"
            )
        }
    }

    private fun geometryProfile(
        name: String = "Standard Silo",
        code: String = "PROFILE1",
        entityVersion: Long = 1L
    ) = GeometryProfile(
        geometryProfileId = geometryProfileId,
        organizationId = organizationId,
        name = name,
        code = code,
        geometryFamilyId = geometryFamilyId,
        lifecycleStatus = MasterStatus.ACTIVE,
        currentVersionId = geometryProfileVersionId,
        createdAt = instant,
        createdBy = "actor",
        updatedAt = instant,
        updatedBy = "actor",
        entityVersion = entityVersion
    )

    private fun formulaVersion(
        versionNumber: Int = 1,
        verifiedAreaM2: CanonicalDecimal? = null,
        areaProvenance: AreaProvenance? = null,
        measurementReferenceHeightM: CanonicalDecimal =
            decimal("21.000000"),
        maximumFillHeightM: CanonicalDecimal =
            decimal("20.000000"),
        maxCapacityM3: CanonicalDecimal =
            decimal("1000.000000"),
        schemaVersion: String = "1"
    ) = GeometryProfileVersion(
        geometryProfileVersionId = geometryProfileVersionId,
        geometryProfileId = geometryProfileId,
        versionNumber = versionNumber,
        geometryFamilyId = geometryFamilyId,
        dimensions = CylFlatDimensions(
            diameterM = decimal("10.000000"),
            heightM = decimal("20.000000")
        ),
        verifiedAreaM2 = verifiedAreaM2,
        areaProvenance = areaProvenance,
        measurementReferenceHeightM =
            measurementReferenceHeightM,
        maximumFillHeightM = maximumFillHeightM,
        geometryOrigin = GeometryOrigin("FLAT_BOTTOM"),
        maxCapacityM3 = maxCapacityM3,
        createdAt = instant,
        createdBy = "actor",
        schemaVersion = schemaVersion
    )

    private fun verifiedAreaVersion(
        verifiedAreaM2: CanonicalDecimal? =
            decimal("100.000000"),
        areaProvenance: AreaProvenance? =
            AreaProvenance("Certified drawing A-101")
    ) = GeometryProfileVersion(
        geometryProfileVersionId = geometryProfileVersionId,
        geometryProfileId = geometryProfileId,
        versionNumber = 1,
        geometryFamilyId = geometryFamilyId,
        dimensions = StarVerifiedAreaDimensions(
            heightM = decimal("20.000000")
        ),
        verifiedAreaM2 = verifiedAreaM2,
        areaProvenance = areaProvenance,
        measurementReferenceHeightM =
            decimal("21.000000"),
        maximumFillHeightM =
            decimal("20.000000"),
        geometryOrigin = GeometryOrigin("SECTION_BOTTOM"),
        maxCapacityM3 =
            decimal("2000.000000"),
        createdAt = instant,
        createdBy = "actor",
        schemaVersion = "1"
    )

    private fun decimal(value: String): CanonicalDecimal =
        CanonicalDecimal.from(value)
}
