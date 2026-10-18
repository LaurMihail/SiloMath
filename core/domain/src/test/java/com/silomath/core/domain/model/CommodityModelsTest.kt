package com.silomath.core.domain.model

import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CommodityModelsTest {

    private val commodityId = CommodityId("11111111-1111-4111-8111-111111111111")
    private val versionId = CommodityVersionId("22222222-2222-4222-8222-222222222222")
    private val otherVersionId = CommodityVersionId("33333333-3333-4333-8333-333333333333")
    private val instant = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun v6CatalogContainsExactlyTheFrozen24Identities() {
        assertEquals(24, V6CommodityCatalog.definitions.size)
        assertEquals(24, V6CommodityCatalog.definitions.map { it.code }.distinct().size)
        assertEquals(24, V6CommodityCatalog.definitions.map { it.displayName }.distinct().size)
        assertNull(V6CommodityCatalog.byCode("OTHER"))
        assertNull(V6CommodityCatalog.byCode("UNKNOWN"))
        assertNull(V6CommodityCatalog.byCode("EMPTY"))
    }

    @Test
    fun catalogUsesExactNormativeBoundaryEntriesAndLookupIsCanonicalized() {
        assertEquals("Common Wheat", V6CommodityCatalog.definitions.first().displayName)
        assertEquals("LENTILS", V6CommodityCatalog.definitions.last().code)
        assertNotNull(V6CommodityCatalog.byCode(" common_wheat "))
        assertEquals(
            CommodityCategory.PSEUDOCEREAL,
            V6CommodityCatalog.byCode("BUCKWHEAT")!!.category
        )
    }

    @Test
    fun commodityRequiresCanonicalCodeAndGovernanceProvenance() {
        assertThrows(IllegalArgumentException::class.java) {
            commodity(code = " common_wheat ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            commodity(governanceActor = "   ")
        }
    }

    @Test
    fun onlyActiveCurrentVersionIsSelectableForNewOperation() {
        val active = commodity()
        val inactive = commodity(lifecycleStatus = CommodityLifecycleStatus.INACTIVE)

        assertTrue(active.isSelectableForNewOperation(versionId))
        assertFalse(active.isSelectableForNewOperation(otherVersionId))
        assertFalse(inactive.isSelectableForNewOperation(versionId))
    }

    @Test
    fun commodityVersionRequiresPositiveVersionAndFrozenDisplayNameShape() {
        assertThrows(IllegalArgumentException::class.java) {
            commodityVersion(versionNumber = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            commodityVersion(displayName = " Common Wheat ")
        }
    }

    private fun commodity(
        code: String = "COMMON_WHEAT",
        lifecycleStatus: CommodityLifecycleStatus = CommodityLifecycleStatus.ACTIVE,
        governanceActor: String = "platform-governance"
    ) = Commodity(
        commodityId = commodityId,
        code = code,
        currentVersionId = versionId,
        lifecycleStatus = lifecycleStatus,
        createdAt = instant,
        governanceActor = governanceActor
    )

    private fun commodityVersion(
        versionNumber: Long = 1L,
        displayName: String = "Common Wheat"
    ) = CommodityVersion(
        commodityVersionId = versionId,
        commodityId = commodityId,
        versionNumber = versionNumber,
        displayName = displayName,
        category = CommodityCategory.CEREAL,
        referenceTestWeight = null,
        moistureReference = null,
        impuritiesReference = null,
        definitionMetadata = emptyMap(),
        createdAt = instant,
        createdBy = "platform-governance"
    )
}
