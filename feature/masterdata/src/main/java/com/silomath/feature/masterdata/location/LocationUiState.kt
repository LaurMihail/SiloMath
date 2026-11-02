package com.silomath.feature.masterdata.location

import com.silomath.core.domain.model.Location
import com.silomath.core.domain.model.MasterStatus
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import java.util.Locale

enum class LocationView { List, Map }
enum class LifecycleFilter { Active, Inactive, All }

data class LocationUiState(
    val organizationId: OrganizationId? = null,
    val locations: List<Location> = emptyList(),
    val unfilteredCount: Int = 0,
    val query: String = "",
    val lifecycle: LifecycleFilter = LifecycleFilter.Active,
    val view: LocationView = LocationView.List,
    val selectedId: LocationId? = null,
    val isAdmin: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val navigationError: String? = null
) {
    val positionedLocations get() = locations.filter { it.latitude != null && it.longitude != null }
}

fun filteredLocations(locations: List<Location>, organizationId: OrganizationId,
    query: String, lifecycle: LifecycleFilter): List<Location> {
    val search=query.trim().lowercase(Locale.ROOT)
    return locations.filter { location ->
        location.organizationId == organizationId &&
            (lifecycle == LifecycleFilter.All || location.lifecycleStatus ==
                if (lifecycle == LifecycleFilter.Active) MasterStatus.ACTIVE else MasterStatus.INACTIVE) &&
            (search.isEmpty() || location.name.lowercase(Locale.ROOT).contains(search) ||
                location.code.lowercase(Locale.ROOT).contains(search))
    }.sortedWith(compareBy<Location> { it.name.lowercase(Locale.ROOT) }.thenBy { it.code }.thenBy { it.locationId.value })
}
