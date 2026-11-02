package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.silomath.core.database.entity.canonical.*

/** Canonical reads are deliberately separate from legacy operational projections. */
@Dao
interface CanonicalOfflineDao {
    @Query("SELECT * FROM v6_calculation_results WHERE calculation_result_id = :id LIMIT 1")
    suspend fun getCalculationResults(id: String): V6CalculationResultsRow?

    @Query("SELECT * FROM v6_campaign_scopes WHERE campaign_scope_id = :id LIMIT 1")
    suspend fun getCampaignScopes(id: String): V6CampaignScopesRow?

    @Query("SELECT * FROM v6_campaign_snapshot_commodity_items WHERE snapshot_commodity_item_id = :id LIMIT 1")
    suspend fun getCampaignSnapshotCommodityItems(id: String): V6CampaignSnapshotCommodityItemsRow?

    @Query("SELECT * FROM v6_campaign_snapshot_scope_items WHERE snapshot_scope_item_id = :id LIMIT 1")
    suspend fun getCampaignSnapshotScopeItems(id: String): V6CampaignSnapshotScopeItemsRow?

    @Query("SELECT * FROM v6_campaign_snapshot_team_items WHERE snapshot_team_item_id = :id LIMIT 1")
    suspend fun getCampaignSnapshotTeamItems(id: String): V6CampaignSnapshotTeamItemsRow?

    @Query("SELECT * FROM v6_campaign_snapshots WHERE campaign_snapshot_id = :id LIMIT 1")
    suspend fun getCampaignSnapshots(id: String): V6CampaignSnapshotsRow?

    @Query("SELECT * FROM v6_campaign_team_assignments WHERE assignment_id = :id LIMIT 1")
    suspend fun getCampaignTeamAssignments(id: String): V6CampaignTeamAssignmentsRow?

    @Query("SELECT * FROM v6_clients WHERE client_id = :id LIMIT 1")
    suspend fun getClients(id: String): V6ClientsRow?

    @Query("SELECT * FROM v6_commodities WHERE commodity_id = :id LIMIT 1")
    suspend fun getCommodities(id: String): V6CommoditiesRow?

    @Query("SELECT * FROM v6_commodity_versions WHERE commodity_version_id = :id LIMIT 1")
    suspend fun getCommodityVersions(id: String): V6CommodityVersionsRow?

    @Query("SELECT * FROM v6_device_installations WHERE device_installation_id = :id LIMIT 1")
    suspend fun getDeviceInstallations(id: String): V6DeviceInstallationsRow?

    @Query("SELECT * FROM v6_evidence WHERE evidence_id = :id LIMIT 1")
    suspend fun getEvidence(id: String): V6EvidenceRow?

    @Query("SELECT * FROM v6_geometry_families WHERE geometry_family_id = :id LIMIT 1")
    suspend fun getGeometryFamilies(id: String): V6GeometryFamiliesRow?

    @Query("SELECT * FROM v6_geometry_profile_versions WHERE geometry_profile_version_id = :id LIMIT 1")
    suspend fun getGeometryProfileVersions(id: String): V6GeometryProfileVersionsRow?

    @Query("SELECT * FROM v6_geometry_profiles WHERE geometry_profile_id = :id LIMIT 1")
    suspend fun getGeometryProfiles(id: String): V6GeometryProfilesRow?

    @Query("SELECT * FROM v6_inventory_campaigns WHERE campaign_id = :id LIMIT 1")
    suspend fun getInventoryCampaigns(id: String): V6InventoryCampaignsRow?

    @Query("SELECT * FROM v6_issue_resolutions WHERE issue_resolution_id = :id LIMIT 1")
    suspend fun getIssueResolutions(id: String): V6IssueResolutionsRow?

    @Query("SELECT * FROM v6_issue_status_events WHERE issue_status_event_id = :id LIMIT 1")
    suspend fun getIssueStatusEvents(id: String): V6IssueStatusEventsRow?

    @Query("SELECT * FROM v6_issues WHERE issue_id = :id LIMIT 1")
    suspend fun getIssues(id: String): V6IssuesRow?

    @Query("SELECT * FROM v6_locations WHERE location_id = :id LIMIT 1")
    suspend fun getLocations(id: String): V6LocationsRow?

    @Query("SELECT * FROM v6_measurements WHERE measurement_id = :id LIMIT 1")
    suspend fun getMeasurements(id: String): V6MeasurementsRow?

    @Query("SELECT * FROM v6_organization_memberships WHERE membership_id = :id LIMIT 1")
    suspend fun getOrganizationMemberships(id: String): V6OrganizationMembershipsRow?

    @Query("SELECT * FROM v6_organizations WHERE organization_id = :id LIMIT 1")
    suspend fun getOrganizations(id: String): V6OrganizationsRow?

    @Query("SELECT * FROM v6_stocktaking_records WHERE stocktaking_record_id = :id LIMIT 1")
    suspend fun getStocktakingRecords(id: String): V6StocktakingRecordsRow?

    @Query("SELECT * FROM v6_stocktaking_revisions WHERE stocktaking_revision_id = :id LIMIT 1")
    suspend fun getStocktakingRevisions(id: String): V6StocktakingRevisionsRow?

    @Query("SELECT * FROM v6_storage_spaces WHERE storage_space_id = :id LIMIT 1")
    suspend fun getStorageSpaces(id: String): V6StorageSpacesRow?

    @Query("SELECT * FROM v6_storage_units WHERE storage_unit_id = :id LIMIT 1")
    suspend fun getStorageUnits(id: String): V6StorageUnitsRow?

    @Query("SELECT * FROM v6_users WHERE user_id = :id LIMIT 1")
    suspend fun getUsers(id: String): V6UsersRow?

}
