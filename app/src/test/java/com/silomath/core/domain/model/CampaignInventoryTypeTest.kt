package com.silomath.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CampaignInventoryTypeTest {

    @Test
    fun `exactly three canonical inventory types exist`() {
        assertEquals(
            listOf("ANNUAL", "MONTHLY", "CONTROL"),
            CampaignInventoryType.entries.map { it.name }
        )
    }
}
