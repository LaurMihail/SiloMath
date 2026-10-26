package com.silomath.backend.location

import java.math.BigDecimal
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class LocationResponse(
    val id: String,
    val organizationId: String,
    val name: String,
    val code: String,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val notes: String? = null,
    val lifecycleStatus: String,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String,
    val entityVersion: Long,
    val latitude: String? = null,
    val longitude: String? = null
)

@Serializable
data class LocationPage(
    val items: List<LocationResponse>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)

@Serializable
data class LocationCreateRequest(
    val name: String,
    val code: String,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val notes: String? = null,
    val lifecycleStatus: String,
    val latitude: String? = null,
    val longitude: String? = null,
    val idempotencyKey: String,
    val clientOperationId: String,
    val occurredAtUtc: String? = null
)

data class CanonicalCoordinates(
    val latitude: BigDecimal?,
    val longitude: BigDecimal?
)

object LocationContractValidator {
    private const val NAME_MAX_LENGTH = 150
    private const val CODE_MAX_LENGTH = 50
    private const val ADDRESS_MAX_LENGTH = 250
    private const val CITY_MAX_LENGTH = 100
    private const val NOTES_MAX_LENGTH = 500

    private val minLatitude = BigDecimal("-90")
    private val maxLatitude = BigDecimal("90")
    private val minLongitude = BigDecimal("-180")
    private val maxLongitude = BigDecimal("180")

    fun canonicalizeCreate(
        request: LocationCreateRequest
    ): LocationCreateRequest =
        request.copy(
            name =
                canonicalizeName(
                    request.name
                ),
            code =
                canonicalizeCode(
                    request.code
                ),
            address =
                validateOptionalText(
                    field = "address",
                    value = request.address,
                    maxLength =
                        ADDRESS_MAX_LENGTH
                ),
            city =
                validateOptionalText(
                    field = "city",
                    value = request.city,
                    maxLength =
                        CITY_MAX_LENGTH
                ),
            notes =
                validateOptionalText(
                    field = "notes",
                    value = request.notes,
                    maxLength =
                        NOTES_MAX_LENGTH
                ),
            lifecycleStatus =
                validateLifecycleStatus(
                    request.lifecycleStatus
                )
        )

    fun canonicalizeName(
        value: String
    ): String {
        val canonical =
            value.trim()

        require(canonical.isNotEmpty()) {
            "name is required"
        }

        require(
            canonical.length <=
                NAME_MAX_LENGTH
        ) {
            "name must be at most " +
                "$NAME_MAX_LENGTH characters"
        }

        return canonical
    }

    fun canonicalizeCode(
        value: String
    ): String {
        val canonical =
            value
                .trim()
                .uppercase(Locale.ROOT)

        require(canonical.isNotEmpty()) {
            "code is required"
        }

        require(
            canonical.length <=
                CODE_MAX_LENGTH
        ) {
            "code must be at most " +
                "$CODE_MAX_LENGTH characters"
        }

        return canonical
    }

    fun validateAddress(
        value: String
    ): String =
        validateText(
            field = "address",
            value = value,
            maxLength =
                ADDRESS_MAX_LENGTH
        )

    fun validateCity(
        value: String
    ): String =
        validateText(
            field = "city",
            value = value,
            maxLength =
                CITY_MAX_LENGTH
        )

    fun validateNotes(
        value: String
    ): String =
        validateText(
            field = "notes",
            value = value,
            maxLength =
                NOTES_MAX_LENGTH
        )

    fun validateLifecycleStatus(
        value: String
    ): String {
        require(
            value == "ACTIVE" ||
                value == "INACTIVE"
        ) {
            "lifecycleStatus must be ACTIVE or INACTIVE"
        }

        return value
    }

    private fun validateOptionalText(
        field: String,
        value: String?,
        maxLength: Int
    ): String? =
        value?.let {
            validateText(
                field = field,
                value = it,
                maxLength = maxLength
            )
        }

    private fun validateText(
        field: String,
        value: String,
        maxLength: Int
    ): String {
        require(value.length <= maxLength) {
            "$field must be at most " +
                "$maxLength characters"
        }

        return value
    }

    fun parseCoordinates(
        latitude: String?,
        longitude: String?
    ): CanonicalCoordinates {
        require((latitude == null) == (longitude == null)) {
            "latitude and longitude must both be present or both be absent"
        }

        if (latitude == null) {
            return CanonicalCoordinates(
                latitude = null,
                longitude = null
            )
        }

        val parsedLatitude =
            latitude.toBigDecimalOrNull()
                ?: throw IllegalArgumentException(
                    "latitude must be a decimal"
                )

        val parsedLongitude =
            requireNotNull(longitude)
                .toBigDecimalOrNull()
                ?: throw IllegalArgumentException(
                    "longitude must be a decimal"
                )

        require(
            parsedLatitude >= minLatitude &&
                parsedLatitude <= maxLatitude
        ) {
            "latitude must be between -90 and 90"
        }

        require(
            parsedLongitude >= minLongitude &&
                parsedLongitude <= maxLongitude
        ) {
            "longitude must be between -180 and 180"
        }

        return CanonicalCoordinates(
            latitude = parsedLatitude,
            longitude = parsedLongitude
        )
    }
}
