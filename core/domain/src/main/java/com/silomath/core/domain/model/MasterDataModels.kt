package com.silomath.core.domain.model

import com.silomath.core.model.ClientId
import com.silomath.core.model.GeometryProfileId
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.StorageUnitId
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

data class Location(
    val locationId: LocationId,
    val organizationId: OrganizationId,
    val name: String,
    val code: String,
    val address: String?,
    val city: String?,
    val country: String?,
    val notes: String?,
    val lifecycleStatus: MasterStatus,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null
) {
    init {
        require(name.isNotBlank()) { "Location name must not be blank" }
        require(name == name.trim()) { "Location name must be trimmed" }
        require(name.length <= 150) { "Location name must be <= 150 characters" }
        require(code.isNotBlank()) { "Location code must not be blank" }
        require(code == code.trim().uppercase(Locale.ROOT)) { "Location code must be trimmed uppercase" }
        require(code.length <= 50) { "Location code must be <= 50 characters" }
        require(address == null || address.length <= 250) { "Location address must be <= 250 characters" }
        require(city == null || city.length <= 100) { "Location city must be <= 100 characters" }
        require(notes == null || notes.length <= 500) { "Location notes must be <= 500 characters" }
        require(entityVersion >= 1L) { "Location entityVersion must be >= 1" }
        require((latitude == null) == (longitude == null)) {
            "Location latitude and longitude must both be present or both be absent"
        }
        require(latitude == null || latitude >= MIN_LATITUDE) {
            "Location latitude must be >= -90"
        }
        require(latitude == null || latitude <= MAX_LATITUDE) {
            "Location latitude must be <= 90"
        }
        require(longitude == null || longitude >= MIN_LONGITUDE) {
            "Location longitude must be >= -180"
        }
        require(longitude == null || longitude <= MAX_LONGITUDE) {
            "Location longitude must be <= 180"
        }
    }

    companion object {
        private val MIN_LATITUDE = BigDecimal("-90")
        private val MAX_LATITUDE = BigDecimal("90")
        private val MIN_LONGITUDE = BigDecimal("-180")
        private val MAX_LONGITUDE = BigDecimal("180")
    }
}

data class StorageUnit(
    val storageUnitId: StorageUnitId,
    val locationId: LocationId,
    val name: String,
    val code: String,
    val description: String?,
    val lifecycleStatus: MasterStatus,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long
) {
    init {
        require(name.isNotBlank()) { "StorageUnit name must not be blank" }
        require(name == name.trim()) { "StorageUnit name must be trimmed" }
        require(name.length <= 150) { "StorageUnit name must be <= 150 characters" }
        require(code.isNotBlank()) { "StorageUnit code must not be blank" }
        require(code == code.trim().uppercase(Locale.ROOT)) { "StorageUnit code must be trimmed uppercase" }
        require(code.length <= 50) { "StorageUnit code must be <= 50 characters" }
        require(description == null || description.length <= 500) { "StorageUnit description must be <= 500 characters" }
        require(entityVersion >= 1L) { "StorageUnit entityVersion must be >= 1" }
    }
}

data class StorageSpace(
    val storageSpaceId: StorageSpaceId,
    val storageUnitId: StorageUnitId,
    val code: String,
    val description: String?,
    val geometryProfileId: GeometryProfileId,
    val lifecycleStatus: MasterStatus,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long
) {
    init {
        require(code.isNotBlank()) { "StorageSpace code must not be blank" }
        require(code == code.trim().uppercase(Locale.ROOT)) { "StorageSpace code must be trimmed uppercase" }
        require(entityVersion >= 1L) { "StorageSpace entityVersion must be >= 1" }
    }
}

data class Client(
    val clientId: ClientId,
    val organizationId: OrganizationId,
    val name: String,
    val code: String,
    val registrationTaxId: String?,
    val notes: String?,
    val lifecycleStatus: MasterStatus,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long
) {
    init {
        require(name.isNotBlank()) { "Client name must not be blank" }
        require(name.length <= 150) { "Client name must be <= 150 characters" }
        require(code.isNotBlank()) { "Client code must not be blank" }
        require(code == code.trim().uppercase(Locale.ROOT)) { "Client code must be trimmed uppercase" }
        require(code.length <= 50) { "Client code must be <= 50 characters" }
        require(registrationTaxId == null || registrationTaxId.length <= 50) { "Client registrationTaxId must be <= 50 characters" }
        require(notes == null || notes.length <= 500) { "Client notes must be <= 500 characters" }
        require(entityVersion >= 1L) { "Client entityVersion must be >= 1" }
    }
}
