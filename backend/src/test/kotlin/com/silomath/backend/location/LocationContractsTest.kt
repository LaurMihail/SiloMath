package com.silomath.backend.location

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LocationContractsTest {

    @Test
    fun `coordinates may both be absent`() {
        val coordinates =
            LocationContractValidator.parseCoordinates(
                latitude = null,
                longitude = null
            )

        assertNull(coordinates.latitude)
        assertNull(coordinates.longitude)
    }

    @Test
    fun `coordinates preserve decimal precision`() {
        val coordinates =
            LocationContractValidator.parseCoordinates(
                latitude = "45.123456",
                longitude = "27.987654"
            )

        assertEquals(
            BigDecimal("45.123456"),
            coordinates.latitude
        )
        assertEquals(
            BigDecimal("27.987654"),
            coordinates.longitude
        )
    }

    @Test
    fun `boundary coordinates are accepted`() {
        val coordinates =
            LocationContractValidator.parseCoordinates(
                latitude = "-90.000000",
                longitude = "180.000000"
            )

        assertEquals(
            BigDecimal("-90.000000"),
            coordinates.latitude
        )
        assertEquals(
            BigDecimal("180.000000"),
            coordinates.longitude
        )
    }

    @Test
    fun `partial coordinate pair is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            LocationContractValidator.parseCoordinates(
                latitude = "45.000000",
                longitude = null
            )
        }
    }

    @Test
    fun `out of range latitude is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            LocationContractValidator.parseCoordinates(
                latitude = "90.000001",
                longitude = "20.000000"
            )
        }
    }

    @Test
    fun `out of range longitude is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            LocationContractValidator.parseCoordinates(
                latitude = "45.000000",
                longitude = "-180.000001"
            )
        }
    }
}
