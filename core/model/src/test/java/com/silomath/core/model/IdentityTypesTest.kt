package com.silomath.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IdentityTypesTest {

    private val canonicalUuid = "123e4567-e89b-12d3-a456-426614174000"

    @Test
    fun canonicalUuidIsAcceptedBySharedIdentityTypes() {
        assertEquals(canonicalUuid, UserId(canonicalUuid).value)
        assertEquals(canonicalUuid, OrganizationId(canonicalUuid).value)
        assertEquals(canonicalUuid, OrganizationMembershipId(canonicalUuid).value)
        assertEquals(canonicalUuid, DeviceInstallationId(canonicalUuid).value)
        assertEquals(canonicalUuid, UserId(canonicalUuid).toString())
    }

    @Test
    fun uppercaseUuidIsRejectedAsNonCanonical() {
        assertThrows(IllegalArgumentException::class.java) {
            UserId(canonicalUuid.uppercase())
        }
    }

    @Test
    fun invalidUuidIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            OrganizationId("not-a-uuid")
        }
    }

    @Test
    fun blankUuidIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceInstallationId("   ")
        }
    }

    @Test
    fun commodityIdRequiresCanonicalStableUuid() {
        assertEquals(canonicalUuid, CommodityId(canonicalUuid).value)
        assertThrows(IllegalArgumentException::class.java) {
            CommodityId("WHEAT")
        }
    }

    @Test
    fun serializationUsesCanonicalUuidString() {
        val id = UserId(canonicalUuid)
        val encoded = Json.encodeToString(UserId.serializer(), id)
        assertEquals("\"$canonicalUuid\"", encoded)
        assertEquals(id, Json.decodeFromString(UserId.serializer(), encoded))
    }
}
