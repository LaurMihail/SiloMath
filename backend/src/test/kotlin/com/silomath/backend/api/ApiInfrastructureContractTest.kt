package com.silomath.backend.api

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ApiInfrastructureContractTest {

    @Test
    fun `entity version guard rejects stale base version`() {
        assertEquals(
            EntityVersionCheckResult.Match,
            checkEntityVersion(
                baseEntityVersion = 7,
                authoritativeEntityVersion = 7
            )
        )
        assertEquals(
            EntityVersionCheckResult.Conflict(7, 8),
            checkEntityVersion(
                baseEntityVersion = 7,
                authoritativeEntityVersion = 8
            )
        )
    }

    @Test
    fun `cursor round trips sort value and deterministic tie breaker`() {
        val codec = CursorCodec()
        val encoded = codec.encode(
            sortValue = "2026-10-25T12:00:00Z",
            tieBreakerId = "11111111-1111-1111-1111-111111111111"
        )

        assertEquals(
            "2026-10-25T12:00:00Z" to
                "11111111-1111-1111-1111-111111111111",
            codec.decode(encoded)
        )
        assertNull(codec.decode("not-a-valid-cursor"))
    }

    @Test
    fun `page limit remains server controlled`() {
        val policy = PageLimitPolicy(
            defaultLimit = 25,
            maxLimit = 100
        )

        assertEquals(25, policy.normalize(null))
        assertEquals(50, policy.normalize(50))
        assertEquals(100, policy.normalize(500))
    }

    @Test
    fun `governed decimal round trips without Double`() {
        val json = Json
        val original = DecimalEnvelope(
            BigDecimal("1234567890.12345678901234567890")
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<DecimalEnvelope>(encoded)

        assertEquals(original.value, decoded.value)
        assertEquals(
            "{\"value\":\"1234567890.12345678901234567890\"}",
            encoded
        )
    }

    @Serializable
    private data class DecimalEnvelope(
        @Serializable(with = BigDecimalAsPlainStringSerializer::class)
        val value: BigDecimal
    )
}
