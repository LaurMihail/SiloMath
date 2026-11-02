package com.silomath.core.database.source

/**
 * Infrastructure-facing reference used while the legacy Long identity
 * is being migrated to the canonical UUID organization identity.
 *
 * This is not a Room entity.
 *
 * Removal target:
 * Campaign CONTRACT stage after legacy Long identities are removed.
 */
data class CampaignLocalOrganizationRef(
    val legacyId: Long,
    val organizationId: String?
)
