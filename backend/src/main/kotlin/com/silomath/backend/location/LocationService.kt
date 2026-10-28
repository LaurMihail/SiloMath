package com.silomath.backend.location

data class LocationListQuery(
    val organizationId: String,
    val cursor: String? = null,
    val search: String? = null,
    val status: String? = null,
    val pageSize: Int = 50
)

interface LocationReadService {
    fun list(
        query: LocationListQuery
    ): LocationPage

    fun get(
        organizationId: String,
        locationId: String
    ): LocationResponse?
}
