package com.silomath.core.data.network.dto

import java.math.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class LocationPageDto(
    val items: List<LocationResponseDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)

@Serializable
data class LocationResponseDto(
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
    @Serializable(
        with = NullableBigDecimalJsonSerializer::class
    )
    val latitude: BigDecimal? = null,
    @Serializable(
        with = NullableBigDecimalJsonSerializer::class
    )
    val longitude: BigDecimal? = null
)

@Serializable
data class LocationCreateRequestDto(
    val name: String,
    val code: String,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val notes: String? = null,
    val lifecycleStatus: String,
    @Serializable(
        with = NullableBigDecimalJsonSerializer::class
    )
    val latitude: BigDecimal? = null,
    @Serializable(
        with = NullableBigDecimalJsonSerializer::class
    )
    val longitude: BigDecimal? = null,
    val idempotencyKey: String,
    val clientOperationId: String,
    val occurredAtUtc: String? = null
)
