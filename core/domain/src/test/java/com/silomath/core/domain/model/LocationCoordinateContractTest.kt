package com.silomath.core.domain.model

import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LocationCoordinateContractTest {

    @Test
    fun `coordinate pair accepts exact boundary values`() {
        val location = location(
            latitude = BigDecimal("-90.000000"),
            longitude = BigDecimal("180.000000")
        )

        assertEquals(BigDecimal("-90.000000"), location.latitude)
        assertEquals(BigDecimal("180.000000"), location.longitude)
    }

    @Test
    fun `coordinate pair may be absent`() {
        val location = location(
            latitude = null,
            longitude = null
        )

        assertEquals(null, location.latitude)
        assertEquals(null, location.longitude)
    }

    @Test
    fun `partial coordinate pair is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            location(
                latitude = BigDecimal("45.000000"),
                longitude = null
            )
        }
    }

    @Test
    fun `out of range latitude is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            location(
                latitude = BigDecimal("90.000001"),
                longitude = BigDecimal("20.000000")
            )
        }
    }

    @Test
    fun `out of range longitude is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            location(
                latitude = BigDecimal("45.000000"),
                longitude = BigDecimal("-180.000001")
            )
        }
    }

    private fun location(
        latitude: BigDecimal?,
        longitude: BigDecimal?
    ): Location =
        Location(
            locationId = LocationId("11111111-1111-1111-1111-111111111111"),
            organizationId = OrganizationId("22222222-2222-2222-2222-222222222222"),
            name = "Braila",
            code = "BR",
            address = null,
            city = "Braila",
            country = "RO",
            notes = null,
            lifecycleStatus = MasterStatus.ACTIVE,
            createdAt = Instant.EPOCH,
            createdBy = "33333333-3333-3333-3333-333333333333",
            updatedAt = Instant.EPOCH,
            updatedBy = "33333333-3333-3333-3333-333333333333",
            entityVersion = 1L,
            latitude = latitude,
            longitude = longitude
        )
}
