package com.silomath.backend.security

import kotlin.test.Test
import kotlin.test.assertEquals

class SecurityContractSmokeTest {

    @Test
    fun `organization role registry remains exactly admin and standard`() {
        assertEquals(
            setOf("ADMIN", "STANDARD"),
            OrganizationRole.entries.map { it.name }.toSet()
        )
    }

    @Test
    fun `entity state requirements introduce no new business states`() {
        assertEquals(
            setOf(
                "CAMPAIGN_ACTIVE",
                "CAMPAIGN_COMPLETED",
                "STOCKTAKING_WORKING_MUTABLE",
                "EVIDENCE_WORKING_MUTABLE",
                "ISSUE_REOPENABLE"
            ),
            EntityStateRequirement.entries.map { it.name }.toSet()
        )
    }
}
