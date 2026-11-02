package com.silomath.core.common

import java.util.UUID

object UuidUtils {
    fun generateUuid(): String {
        return UUID.randomUUID().toString().lowercase()
    }

    fun isValidUuid(uuidString: String?): Boolean {
        val value = uuidString?.trim()?.takeIf { it.isNotEmpty() } ?: return false

        return try {
            UUID.fromString(value)
                .toString()
                .equals(value, ignoreCase = true)
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
