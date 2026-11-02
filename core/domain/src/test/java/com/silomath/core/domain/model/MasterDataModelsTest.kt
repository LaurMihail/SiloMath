package com.silomath.core.domain.model

import com.silomath.core.model.ClientId
import com.silomath.core.model.GeometryProfileId
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.StorageUnitId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MasterDataModelsTest {

    private val organizationId =
        OrganizationId("11111111-1111-4111-8111-111111111111")

    private val locationId =
        LocationId("22222222-2222-4222-8222-222222222222")

    private val storageUnitId =
        StorageUnitId("33333333-3333-4333-8333-333333333333")

    private val storageSpaceId =
        StorageSpaceId("44444444-4444-4444-8444-444444444444")

    private val geometryProfileId =
        GeometryProfileId("55555555-5555-4555-8555-555555555555")

    private val clientId =
        ClientId("66666666-6666-4666-8666-666666666666")

    private val instant =
        Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun canonicalMasterDataAcceptsValidValues() {
        val location = location()
        val storageUnit = storageUnit()
        val storageSpace = storageSpace()
        val client = client()

        assertEquals("LOC1", location.code)
        assertEquals("UNIT1", storageUnit.code)
        assertEquals("SPACE1", storageSpace.code)
        assertEquals("CLIENT1", client.code)
    }

    @Test
    fun locationRejectsNonCanonicalCode() {
        assertThrows(IllegalArgumentException::class.java) {
            location(code = " loc1 ")
        }
    }

    @Test
    fun locationRejectsBlankOrOverlongName() {
        assertThrows(IllegalArgumentException::class.java) {
            location(name = "   ")
        }

        assertThrows(IllegalArgumentException::class.java) {
            location(name = "A".repeat(151))
        }
    }

    @Test
    fun storageUnitRejectsNonCanonicalCodeAndOverlongDescription() {
        assertThrows(IllegalArgumentException::class.java) {
            storageUnit(code = "unit1")
        }

        assertThrows(IllegalArgumentException::class.java) {
            storageUnit(description = "D".repeat(501))
        }
    }

    @Test
    fun storageSpaceRejectsBlankOrNonCanonicalCode() {
        assertThrows(IllegalArgumentException::class.java) {
            storageSpace(code = "")
        }

        assertThrows(IllegalArgumentException::class.java) {
            storageSpace(code = "space1")
        }
    }

    @Test
    fun clientRejectsNonCanonicalCodeAndOverlongFields() {
        assertThrows(IllegalArgumentException::class.java) {
            client(code = " client1 ")
        }

        assertThrows(IllegalArgumentException::class.java) {
            client(registrationTaxId = "T".repeat(51))
        }

        assertThrows(IllegalArgumentException::class.java) {
            client(notes = "N".repeat(501))
        }
    }

    @Test
    fun allMasterDataRejectZeroEntityVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            location(entityVersion = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            storageUnit(entityVersion = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            storageSpace(entityVersion = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            client(entityVersion = 0L)
        }
    }

    private fun location(
        name: String = "Location",
        code: String = "LOC1",
        entityVersion: Long = 1L
    ) = Location(
        locationId = locationId,
        organizationId = organizationId,
        name = name,
        code = code,
        address = null,
        city = null,
        country = null,
        notes = null,
        lifecycleStatus = MasterStatus.ACTIVE,
        createdAt = instant,
        createdBy = "actor",
        updatedAt = instant,
        updatedBy = "actor",
        entityVersion = entityVersion
    )

    private fun storageUnit(
        name: String = "Storage Unit",
        code: String = "UNIT1",
        description: String? = null,
        entityVersion: Long = 1L
    ) = StorageUnit(
        storageUnitId = storageUnitId,
        locationId = locationId,
        name = name,
        code = code,
        description = description,
        lifecycleStatus = MasterStatus.ACTIVE,
        createdAt = instant,
        createdBy = "actor",
        updatedAt = instant,
        updatedBy = "actor",
        entityVersion = entityVersion
    )

    private fun storageSpace(
        code: String = "SPACE1",
        entityVersion: Long = 1L
    ) = StorageSpace(
        storageSpaceId = storageSpaceId,
        storageUnitId = storageUnitId,
        code = code,
        description = null,
        geometryProfileId = geometryProfileId,
        lifecycleStatus = MasterStatus.ACTIVE,
        createdAt = instant,
        createdBy = "actor",
        updatedAt = instant,
        updatedBy = "actor",
        entityVersion = entityVersion
    )

    private fun client(
        name: String = "Client",
        code: String = "CLIENT1",
        registrationTaxId: String? = null,
        notes: String? = null,
        entityVersion: Long = 1L
    ) = Client(
        clientId = clientId,
        organizationId = organizationId,
        name = name,
        code = code,
        registrationTaxId = registrationTaxId,
        notes = notes,
        lifecycleStatus = MasterStatus.ACTIVE,
        createdAt = instant,
        createdBy = "actor",
        updatedAt = instant,
        updatedBy = "actor",
        entityVersion = entityVersion
    )
}
