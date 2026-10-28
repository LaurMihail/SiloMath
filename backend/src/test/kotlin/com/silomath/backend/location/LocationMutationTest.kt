package com.silomath.backend.location

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class LocationMutationTest {

    @Test
    fun `omitted nullable field remains omitted`() {
        val intent =
            LocationPatchParser.parse(
                buildJsonObject {
                    put(
                        "name",
                        JsonPrimitive("New Name")
                    )
                }
            )

        assertIs<PatchValue.Omitted>(
            intent.address
        )
    }

    @Test
    fun `explicit null clears nullable field`() {
        val intent =
            LocationPatchParser.parse(
                buildJsonObject {
                    put(
                        "address",
                        JsonNull
                    )
                }
            )

        assertIs<PatchValue.Clear>(
            intent.address
        )
    }

    @Test
    fun `explicit null cannot clear required field`() {
        assertFailsWith<IllegalArgumentException> {
            LocationPatchParser.parse(
                buildJsonObject {
                    put(
                        "name",
                        JsonNull
                    )
                }
            )
        }
    }

    @Test
    fun `coordinates must be patched together`() {
        assertFailsWith<IllegalArgumentException> {
            LocationPatchParser.parse(
                buildJsonObject {
                    put(
                        "latitude",
                        JsonPrimitive("45.000000")
                    )
                }
            )
        }
    }

    @Test
    fun `coordinates may be cleared atomically`() {
        val intent =
            LocationPatchParser.parse(
                buildJsonObject {
                    put(
                        "latitude",
                        JsonNull
                    )
                    put(
                        "longitude",
                        JsonNull
                    )
                }
            )

        assertIs<PatchValue.Clear>(
            intent.latitude
        )
        assertIs<PatchValue.Clear>(
            intent.longitude
        )
    }

    @Test
    fun `coordinate set preserves exact decimal`() {
        val intent =
            LocationPatchParser.parse(
                buildJsonObject {
                    put(
                        "latitude",
                        JsonPrimitive("45.123456")
                    )
                    put(
                        "longitude",
                        JsonPrimitive("27.987654")
                    )
                }
            )

        val latitude =
            assertIs<PatchValue.Set<BigDecimal>>(
                intent.latitude
            )

        assertEquals(
            BigDecimal("45.123456"),
            latitude.value
        )
    }

    @Test
    fun `fingerprint is independent of json property order`() {
        val first =
            buildJsonObject {
                put(
                    "name",
                    JsonPrimitive("A")
                )
                put(
                    "city",
                    JsonPrimitive("B")
                )
            }

        val second =
            buildJsonObject {
                put(
                    "city",
                    JsonPrimitive("B")
                )
                put(
                    "name",
                    JsonPrimitive("A")
                )
            }

        assertEquals(
            LocationMutationFingerprint.patch(
                organizationId =
                    "11111111-1111-1111-1111-111111111111",
                locationId =
                    "22222222-2222-2222-2222-222222222222",
                baseEntityVersion = 3L,
                json = first
            ),
            LocationMutationFingerprint.patch(
                organizationId =
                    "11111111-1111-1111-1111-111111111111",
                locationId =
                    "22222222-2222-2222-2222-222222222222",
                baseEntityVersion = 3L,
                json = second
            )
        )
    }
}
