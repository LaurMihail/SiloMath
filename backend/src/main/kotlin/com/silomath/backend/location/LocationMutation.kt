package com.silomath.backend.location

import java.math.BigDecimal
import java.security.MessageDigest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface PatchValue<out T> {
    data object Omitted : PatchValue<Nothing>
    data object Clear : PatchValue<Nothing>
    data class Set<T>(
        val value: T
    ) : PatchValue<T>
}

data class LocationPatchIntent(
    val name: PatchValue<String>,
    val code: PatchValue<String>,
    val address: PatchValue<String>,
    val city: PatchValue<String>,
    val country: PatchValue<String>,
    val notes: PatchValue<String>,
    val lifecycleStatus: PatchValue<String>,
    val latitude: PatchValue<BigDecimal>,
    val longitude: PatchValue<BigDecimal>
) {
    init {
        val latitudeOmitted =
            latitude is PatchValue.Omitted
        val longitudeOmitted =
            longitude is PatchValue.Omitted

        require(latitudeOmitted == longitudeOmitted) {
            "latitude and longitude must be patched together"
        }

        val latitudeClear =
            latitude is PatchValue.Clear
        val longitudeClear =
            longitude is PatchValue.Clear

        require(latitudeClear == longitudeClear) {
            "latitude and longitude must be cleared together"
        }

        if (
            latitude is PatchValue.Set &&
            longitude is PatchValue.Set
        ) {
            LocationContractValidator.parseCoordinates(
                latitude =
                    latitude.value.toPlainString(),
                longitude =
                    longitude.value.toPlainString()
            )
        }
    }
}

private fun <T, R> PatchValue<T>.mapSet(
    transform: (T) -> R
): PatchValue<R> =
    when (this) {
        PatchValue.Omitted ->
            PatchValue.Omitted

        PatchValue.Clear ->
            PatchValue.Clear

        is PatchValue.Set ->
            PatchValue.Set(
                transform(value)
            )
    }

object LocationPatchParser {

    private val allowedProperties =
        setOf(
            "name",
            "code",
            "address",
            "city",
            "country",
            "notes",
            "lifecycleStatus",
            "latitude",
            "longitude",
            "baseEntityVersion",
            "idempotencyKey",
            "clientOperationId",
            "occurredAtUtc"
        )

    fun parse(
        json: JsonObject
    ): LocationPatchIntent {
        val unknown =
            json.keys - allowedProperties

        require(unknown.isEmpty()) {
            "Unsupported Location PATCH properties: " +
                unknown.sorted().joinToString(", ")
        }

        return LocationPatchIntent(
            name =
                json.patchString(
                    key = "name",
                    clearAllowed = false
                ).mapSet(
                    LocationContractValidator::
                        canonicalizeName
                ),
            code =
                json.patchString(
                    key = "code",
                    clearAllowed = false
                ).mapSet(
                    LocationContractValidator::
                        canonicalizeCode
                ),
            address =
                json.patchString(
                    key = "address",
                    clearAllowed = true
                ).mapSet(
                    LocationContractValidator::
                        validateAddress
                ),
            city =
                json.patchString(
                    key = "city",
                    clearAllowed = true
                ).mapSet(
                    LocationContractValidator::
                        validateCity
                ),
            country =
                json.patchString(
                    key = "country",
                    clearAllowed = true
                ),
            notes =
                json.patchString(
                    key = "notes",
                    clearAllowed = true
                ).mapSet(
                    LocationContractValidator::
                        validateNotes
                ),
            lifecycleStatus =
                json.patchString(
                    key = "lifecycleStatus",
                    clearAllowed = false
                ).mapSet(
                    LocationContractValidator::
                        validateLifecycleStatus
                ),
            latitude =
                json.patchDecimal(
                    key = "latitude"
                ),
            longitude =
                json.patchDecimal(
                    key = "longitude"
                )
        )
    }

    private fun JsonObject.patchString(
        key: String,
        clearAllowed: Boolean
    ): PatchValue<String> {
        if (!containsKey(key)) {
            return PatchValue.Omitted
        }

        val element =
            getValue(key)

        if (element is JsonNull) {
            require(clearAllowed) {
                "$key cannot be cleared"
            }
            return PatchValue.Clear
        }

        val primitive =
            element as? JsonPrimitive
                ?: throw IllegalArgumentException(
                    "$key must be a JSON string"
                )

        require(primitive.isString) {
            "$key must be a JSON string"
        }

        return PatchValue.Set(
            primitive.content
        )
    }

    private fun JsonObject.patchDecimal(
        key: String
    ): PatchValue<BigDecimal> {
        if (!containsKey(key)) {
            return PatchValue.Omitted
        }

        val element =
            getValue(key)

        if (element is JsonNull) {
            return PatchValue.Clear
        }

        val primitive =
            element as? JsonPrimitive
                ?: throw IllegalArgumentException(
                    "$key must be a decimal JSON primitive"
                )

        val value =
            primitive.content
                .toBigDecimalOrNull()
                ?: throw IllegalArgumentException(
                    "$key must be a decimal"
                )

        return PatchValue.Set(value)
    }
}

object LocationMutationFingerprint {

    fun create(
        organizationId: String,
        request: LocationCreateRequest
    ): String =
        sha256(
            listOf(
                "LOCATION_CREATE",
                organizationId,
                request.name,
                request.code,
                request.address.orEmpty(),
                request.city.orEmpty(),
                request.country.orEmpty(),
                request.notes.orEmpty(),
                request.lifecycleStatus,
                request.latitude.orEmpty(),
                request.longitude.orEmpty(),
                request.clientOperationId,
                request.occurredAtUtc.orEmpty()
            ).joinToString("\u001F")
        )

    fun patch(
        organizationId: String,
        locationId: String,
        baseEntityVersion: Long,
        json: JsonObject
    ): String =
        sha256(
            listOf(
                "LOCATION_PATCH",
                organizationId,
                locationId,
                baseEntityVersion.toString(),
                canonicalJsonObject(json)
            ).joinToString("\u001F")
        )

    private fun canonicalJsonObject(
        json: JsonObject
    ): String =
        json.entries
            .sortedBy { it.key }
            .joinToString(
                prefix = "{",
                postfix = "}",
                separator = ","
            ) { (key, value) ->
                "$key=${canonicalJsonElement(value)}"
            }

    private fun canonicalJsonElement(
        element: JsonElement
    ): String =
        when (element) {
            JsonNull ->
                "null"

            is JsonPrimitive ->
                if (element.isString) {
                    "\"${element.content}\""
                } else {
                    element.content
                }

            is JsonObject ->
                canonicalJsonObject(element)

            else ->
                element.toString()
        }

    private fun sha256(
        value: String
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") {
                "%02x".format(it)
            }
}
