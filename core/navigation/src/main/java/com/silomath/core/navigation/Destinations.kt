package com.silomath.core.navigation

import androidx.navigation3.runtime.NavKey
import com.silomath.core.model.*
import kotlinx.serialization.Serializable

@Serializable
enum class ShellSection(val componentId: String, val title: String) {
    Dashboard("DSH-01", "Dashboard"), Campaigns("CAM-01", "Inventory Campaigns"),
    Stocktaking("STK-01", "Stocktaking"), Locations("LOC-01", "Locations"), More("MOR-01", "More")
}

/** Keys hold identity only, never credentials, mutable models or calculation versions. */
@Serializable
sealed interface Destination : NavKey

@Serializable data object SignIn : Destination
@Serializable data object ResetPassword : Destination
@Serializable data object SelectOrganization : Destination

@Serializable
sealed interface OrganizationDestination : Destination { val organizationId: OrganizationId }

@Serializable data class ShellDestination(override val organizationId: OrganizationId,
    val section: ShellSection) : OrganizationDestination
@Serializable data class LocationUnits(override val organizationId: OrganizationId,
    val locationId: LocationId) : OrganizationDestination
@Serializable data class StorageUnitDetails(override val organizationId: OrganizationId,
    val locationId: LocationId, val storageUnitId: StorageUnitId) : OrganizationDestination
@Serializable data class StorageSpaceDetails(override val organizationId: OrganizationId,
    val locationId: LocationId, val storageUnitId: StorageUnitId,
    val storageSpaceId: StorageSpaceId) : OrganizationDestination
@Serializable data class LocationEditor(override val organizationId: OrganizationId,
    val locationId: LocationId? = null) : OrganizationDestination

@Serializable data class StocktakingLocation(override val organizationId: OrganizationId,
    val campaignId: CampaignId, val locationId: LocationId) : OrganizationDestination
@Serializable data class StocktakingUnit(override val organizationId: OrganizationId,
    val campaignId: CampaignId, val locationId: LocationId,
    val storageUnitId: StorageUnitId) : OrganizationDestination
@Serializable data class StocktakingSpace(override val organizationId: OrganizationId,
    val campaignId: CampaignId, val locationId: LocationId, val storageUnitId: StorageUnitId,
    val storageSpaceId: StorageSpaceId) : OrganizationDestination

fun OrganizationDestination.shellSection(): ShellSection = when(this) {
    is ShellDestination -> section
    is LocationUnits, is StorageUnitDetails, is StorageSpaceDetails, is LocationEditor -> ShellSection.Locations
    is StocktakingLocation, is StocktakingUnit, is StocktakingSpace -> ShellSection.Stocktaking
}

/** Canonical parent identities; hierarchy membership still requires a domain authorizer. */
fun OrganizationDestination.parent(): OrganizationDestination? = when(this) {
    is ShellDestination -> null
    is LocationUnits, is LocationEditor -> ShellDestination(organizationId,ShellSection.Locations)
    is StorageUnitDetails -> LocationUnits(organizationId,locationId)
    is StorageSpaceDetails -> StorageUnitDetails(organizationId,locationId,storageUnitId)
    is StocktakingLocation -> ShellDestination(organizationId,ShellSection.Stocktaking)
    is StocktakingUnit -> StocktakingLocation(organizationId,campaignId,locationId)
    is StocktakingSpace -> StocktakingUnit(organizationId,campaignId,locationId,storageUnitId)
}

fun OrganizationDestination.parentChain(): List<OrganizationDestination> =
    generateSequence(this) { it.parent() }.toList().asReversed()
