package com.silomath.backend.api

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Page<T>(
    val items: List<T>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)

@Serializable
internal data class CursorToken(
    val sortValue: String,
    val tieBreakerId: String
)

class CursorCodec {
    private val json = Json { encodeDefaults = true }

    fun encode(
        sortValue: String,
        tieBreakerId: String
    ): String {
        val payload = json.encodeToString(
            CursorToken(sortValue, tieBreakerId)
        )
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(cursor: String): Pair<String, String>? =
        runCatching {
            val payload = String(
                Base64.getUrlDecoder().decode(cursor),
                StandardCharsets.UTF_8
            )
            val token = json.decodeFromString<CursorToken>(payload)
            token.sortValue to token.tieBreakerId
        }.getOrNull()
}

/** Server-controlled operational page-size policy; values are supplied by runtime configuration. */
data class PageLimitPolicy(
    val defaultLimit: Int,
    val maxLimit: Int
) {
    init {
        require(defaultLimit > 0)
        require(maxLimit >= defaultLimit)
    }

    fun normalize(requestedLimit: Int?): Int =
        requestedLimit
            ?.takeIf { it > 0 }
            ?.coerceAtMost(maxLimit)
            ?: defaultLimit
}
