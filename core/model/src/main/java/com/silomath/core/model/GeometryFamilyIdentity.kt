package com.silomath.core.model

import java.util.UUID
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class GeometryFamilyId(val value: String) {
    init {
        require(value.isNotBlank()) {
            "GeometryFamilyId must not be blank"
        }

        val parsed = try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException(
                "GeometryFamilyId must be a valid UUID: $value"
            )
        }

        require(parsed.toString() == value) {
            "GeometryFamilyId must use canonical lowercase UUID representation: $value"
        }
    }

    override fun toString(): String = value
}
