package com.silomath.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CampaignInventoryTypeTest {

    @Test
    fun `exactly three allowed canonical inventory types exist`() {
        assertEquals(3, CampaignInventoryType.entries.size)
    }

    @Test
    fun `each domain value maps to exact backend wire string`() {
        assertEquals("Annual", CampaignInventoryType.ANNUAL.wireValue)
        assertEquals("Monthly", CampaignInventoryType.MONTHLY.wireValue)
        assertEquals("Control", CampaignInventoryType.CONTROL.wireValue)
    }

    @Test
    fun `fromWireValue converts valid strings to enum and returns null for invalid string`() {
        assertEquals(CampaignInventoryType.ANNUAL, CampaignInventoryType.fromWireValue("Annual"))
        assertEquals(CampaignInventoryType.MONTHLY, CampaignInventoryType.fromWireValue("Monthly"))
        assertEquals(CampaignInventoryType.CONTROL, CampaignInventoryType.fromWireValue("Control"))
        assertNull(CampaignInventoryType.fromWireValue("Handover / Takeover"))
        assertNull(CampaignInventoryType.fromWireValue("Invalid"))
        assertNull(CampaignInventoryType.fromWireValue("annual"))
    }
}
