package com.silomath.core.domain.model

enum class CampaignInventoryType(val wireValue: String) {
    ANNUAL("Annual"),
    MONTHLY("Monthly"),
    CONTROL("Control");

    companion object {
        fun fromWireValue(value: String): CampaignInventoryType? {
            return entries.find { it.wireValue == value }
        }
    }
}

data class SaveAndLaunchTeamMember(
    val userId: String,
    val roleAtAssignment: String
)

data class SaveAndLaunchCampaignInput(
    val organizationId: String,
    val name: String,
    val inventoryType: CampaignInventoryType,
    val startDate: String,
    val plannedEndDate: String,
    val scopeStorageSpaceIds: List<String>,
    val team: List<SaveAndLaunchTeamMember>,
    val idempotencyKey: String
)

data class SaveAndLaunchCampaignResult(
    val campaignId: String,
    val launchSnapshotId: String
)
